package com.example.openglow.data.model

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

sealed class ModelDownloadState {
    object NotDownloaded : ModelDownloadState()
    object Checking : ModelDownloadState()
    data class NotConfigured(val modelId: String, val message: String) : ModelDownloadState()
    data class Downloading(
        val modelId: String,
        val currentFileName: String,
        val progress: Float,
        val downloadedBytes: Long,
        val totalBytes: Long,
    ) : ModelDownloadState()
    data class Verifying(val modelId: String) : ModelDownloadState()
    data class Ready(val modelId: String, val directoryPath: String) : ModelDownloadState()
    data class Failed(val modelId: String, val message: String) : ModelDownloadState()
    data class Cancelled(val modelId: String) : ModelDownloadState()
}

@Singleton
class ModelDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
) {
    private val stateFlows = mutableMapOf<String, MutableStateFlow<ModelDownloadState>>()
    @Volatile private var cancelledModelId: String? = null

    fun getModelDirectory(model: AppModelInfo): File {
        return File(File(context.filesDir, "models"), model.targetDirectoryName)
    }

    fun getModelStateFlow(modelId: String): StateFlow<ModelDownloadState> {
        val model = ModelRegistry.byId(modelId)
        val initial = model?.let(::currentStateFor) ?: ModelDownloadState.NotDownloaded
        return stateFlows.getOrPut(modelId) { MutableStateFlow(initial) }
    }

    fun refreshModelState(model: AppModelInfo) {
        mutableState(model.id).value = currentStateFor(model)
    }

    fun isModelReady(model: AppModelInfo): Boolean {
        val directory = getModelDirectory(model)
        return model.artifacts.isNotEmpty() && model.artifacts.all { artifact ->
            val file = File(directory, artifact.fileName)
            file.exists() &&
                file.length() > 0L &&
                hasConfiguredChecksum(artifact) &&
                file.sha256() == artifact.sha256.lowercase()
        }
    }

    suspend fun downloadModel(model: AppModelInfo): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val state = mutableState(model.id)
            state.value = ModelDownloadState.Checking
            cancelledModelId = null

            configurationIssue(model)?.let { message ->
                state.value = ModelDownloadState.NotConfigured(model.id, message)
                throw IllegalStateException(message)
            }
            ensureStorageAvailable(model)

            val directory = getModelDirectory(model)
            if (directory.exists()) directory.deleteRecursively()
            directory.mkdirs()

            try {
                model.artifacts.forEach { artifact ->
                    throwIfCancelled(model.id)
                    downloadArtifact(model, artifact, directory, state)
                }

                state.value = ModelDownloadState.Verifying(model.id)
                val verified = model.artifacts.all { artifact ->
                    File(directory, artifact.fileName).sha256() == artifact.sha256.lowercase()
                }
                if (!verified) error("SHA-256 verification failed")

                state.value = ModelDownloadState.Ready(model.id, directory.absolutePath)
                Log.i(TAG, "Model ready: id=${model.id}, dir=${directory.absolutePath}")
                Unit
            } catch (e: ModelDownloadCancelledException) {
                directory.deleteRecursively()
                state.value = ModelDownloadState.Cancelled(model.id)
                throw e
            } catch (e: Throwable) {
                directory.deleteRecursively()
                state.value = ModelDownloadState.Failed(model.id, e.message ?: "Download failed")
                throw e
            }
        }
    }

    suspend fun deleteModel(model: AppModelInfo): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            getModelDirectory(model).deleteRecursively()
            mutableState(model.id).value = currentStateFor(model)
        }
    }

    fun cancelDownload(modelId: String) {
        cancelledModelId = modelId
        mutableState(modelId).value = ModelDownloadState.Cancelled(modelId)
    }

    fun getLocalLlmModelFile(): File? {
        return File(File(context.filesDir, "models/local_llm"), "model.litertlm")
            .takeIf { it.exists() && it.isFile && it.length() > 0L }
    }

    fun getKcElectraModelDirectory(): File? {
        val directory = File(context.filesDir, "models/kcelectra")
        val requiredFiles = listOf(
            "model.tflite",
            "vocab.txt",
            "tokenizer_config.json",
            "label_map.json",
        )
        return directory.takeIf { dir ->
            dir.exists() && requiredFiles.all { name ->
                File(dir, name).let { it.exists() && it.isFile && it.length() > 0L }
            }
        }
    }

    fun configurationIssue(model: AppModelInfo): String? {
        if (model.artifacts.isEmpty()) {
            return notConfiguredMessage(model)
        }
        val hasUnconfiguredArtifact = model.artifacts.any { artifact ->
            artifact.downloadUrl.isBlank() ||
                artifact.downloadUrl.startsWith("TODO", ignoreCase = true) ||
                !hasConfiguredChecksum(artifact)
        }
        return if (hasUnconfiguredArtifact) notConfiguredMessage(model) else null
    }

    private fun currentStateFor(model: AppModelInfo): ModelDownloadState {
        return when {
            isModelReady(model) -> ModelDownloadState.Ready(model.id, getModelDirectory(model).absolutePath)
            configurationIssue(model) != null -> ModelDownloadState.NotConfigured(model.id, configurationIssue(model).orEmpty())
            else -> ModelDownloadState.NotDownloaded
        }
    }

    private fun mutableState(modelId: String): MutableStateFlow<ModelDownloadState> {
        return stateFlows.getOrPut(modelId) { MutableStateFlow(ModelDownloadState.NotDownloaded) }
    }

    private fun notConfiguredMessage(model: AppModelInfo): String {
        return when (model.kind) {
            ModelKind.LOCAL_LLM ->
                "Local LLM 모델이 아직 설정되지 않았습니다. MODEL_MANIFEST_URL의 model.litertlm URL과 SHA-256을 확인해 주세요."
            ModelKind.TEXT_CLASSIFIER ->
                "KcELECTRA 모델이 아직 설정되지 않았습니다. OpenGlow 분류 태스크에 맞게 fine-tuning된 model.tflite, vocab.txt, tokenizer_config.json, label_map.json이 필요합니다."
        }
    }

    private fun ensureStorageAvailable(model: AppModelInfo) {
        val requiredBytes = model.artifacts.sumOf { it.sizeBytes }
        val usableBytes = context.filesDir.usableSpace
        if (requiredBytes > 0 && usableBytes < requiredBytes) {
            error("Not enough storage for ${model.displayName}")
        }
    }

    private fun downloadArtifact(
        model: AppModelInfo,
        artifact: ModelArtifactInfo,
        directory: File,
        state: MutableStateFlow<ModelDownloadState>,
    ) {
        val request = Request.Builder().url(artifact.downloadUrl).build()
        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IOException("HTTP ${response.code} while downloading ${artifact.fileName}")
        }
        val body = response.body ?: throw IOException("Empty response for ${artifact.fileName}")
        val totalBytes = body.contentLength().takeIf { it > 0 } ?: artifact.sizeBytes
        val tmpFile = File(directory, "${artifact.fileName}.tmp")
        val targetFile = File(directory, artifact.fileName)

        body.byteStream().use { input ->
            tmpFile.outputStream().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var downloaded = 0L
                while (true) {
                    throwIfCancelled(model.id)
                    val read = input.read(buffer)
                    if (read == -1) break
                    output.write(buffer, 0, read)
                    downloaded += read
                    state.value = ModelDownloadState.Downloading(
                        modelId = model.id,
                        currentFileName = artifact.fileName,
                        progress = if (totalBytes > 0) downloaded.toFloat() / totalBytes else 0f,
                        downloadedBytes = downloaded,
                        totalBytes = totalBytes,
                    )
                }
            }
        }

        val expectedHash = artifact.sha256.lowercase()
        val hash = tmpFile.sha256()
        if (hash != expectedHash) {
            tmpFile.delete()
            throw IOException("SHA-256 mismatch for ${artifact.fileName}")
        }
        if (targetFile.exists()) targetFile.delete()
        if (!tmpFile.renameTo(targetFile)) {
            tmpFile.copyTo(targetFile, overwrite = true)
            tmpFile.delete()
        }
    }

    private fun throwIfCancelled(modelId: String) {
        if (cancelledModelId == modelId) {
            throw ModelDownloadCancelledException()
        }
    }

    private fun hasConfiguredChecksum(artifact: ModelArtifactInfo): Boolean {
        return artifact.sha256.length == SHA256_LENGTH && !artifact.sha256.startsWith("TODO", ignoreCase = true)
    }

    private fun File.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString(separator = "") { "%02x".format(it) }
    }

    private class ModelDownloadCancelledException : IOException("Download cancelled")

    private companion object {
        private const val TAG = "ModelDownloadManager"
        private const val SHA256_LENGTH = 64
    }
}
