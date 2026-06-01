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

    fun isModelReady(model: AppModelInfo): Boolean {
        val directory = getModelDirectory(model)
        return model.artifacts.all { artifact ->
            val file = File(directory, artifact.fileName)
            file.exists() && file.length() > 0L && hasConfiguredChecksum(artifact) && file.sha256() == artifact.sha256
        }
    }

    fun getModelStateFlow(modelId: String): StateFlow<ModelDownloadState> {
        val model = ModelRegistry.byId(modelId)
        val initial = if (model != null && isModelReady(model)) {
            ModelDownloadState.Ready(model.id, getModelDirectory(model).absolutePath)
        } else {
            ModelDownloadState.NotDownloaded
        }
        return stateFlows.getOrPut(modelId) { MutableStateFlow(initial) }
    }

    suspend fun downloadModel(model: AppModelInfo): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val state = mutableState(model.id)
            state.value = ModelDownloadState.Checking
            cancelledModelId = null

            validateRegistry(model)
            ensureStorageAvailable(model)

            val directory = getModelDirectory(model)
            if (directory.exists()) directory.deleteRecursively()
            directory.mkdirs()

            try {
                model.artifacts.forEach { artifact ->
                    if (cancelledModelId == model.id) error("Download cancelled")
                    downloadArtifact(model, artifact, directory, state)
                }

                state.value = ModelDownloadState.Verifying(model.id)
                val verified = model.artifacts.all { artifact ->
                    File(directory, artifact.fileName).sha256() == artifact.sha256
                }
                if (!verified) error("SHA-256 verification failed")

                state.value = ModelDownloadState.Ready(model.id, directory.absolutePath)
                Log.i(TAG, "Model ready: id=${model.id}, dir=${directory.absolutePath}")
                Unit
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
            mutableState(model.id).value = ModelDownloadState.NotDownloaded
        }
    }

    fun cancelDownload(modelId: String) {
        cancelledModelId = modelId
        mutableState(modelId).value = ModelDownloadState.Failed(modelId, "Download cancelled")
    }

    fun getLocalLlmModelFile(): File? {
        val model = ModelRegistry.recommendedLocalLlm
        if (!isModelReady(model)) return null
        return File(getModelDirectory(model), "model.litertlm").takeIf { it.exists() }
    }

    fun getKcElectraModelDirectory(): File? {
        val model = ModelRegistry.recommendedKcElectra
        if (!isModelReady(model)) return null
        return getModelDirectory(model).takeIf { it.exists() }
    }

    private fun mutableState(modelId: String): MutableStateFlow<ModelDownloadState> {
        return stateFlows.getOrPut(modelId) { MutableStateFlow(ModelDownloadState.NotDownloaded) }
    }

    private fun validateRegistry(model: AppModelInfo) {
        model.artifacts.forEach { artifact ->
            if (artifact.downloadUrl.startsWith("TODO")) {
                error("Download URL is not configured for ${artifact.fileName}")
            }
            if (!hasConfiguredChecksum(artifact)) {
                error("SHA-256 is not configured for ${artifact.fileName}")
            }
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
                    if (cancelledModelId == model.id) throw IOException("Download cancelled")
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

        val hash = tmpFile.sha256()
        if (hash != artifact.sha256) {
            throw IOException("SHA-256 mismatch for ${artifact.fileName}")
        }
        if (!tmpFile.renameTo(targetFile)) {
            throw IOException("Failed to move ${artifact.fileName}")
        }
    }

    private fun hasConfiguredChecksum(artifact: ModelArtifactInfo): Boolean {
        return artifact.sha256.length == 64 && !artifact.sha256.startsWith("TODO")
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

    private companion object {
        private const val TAG = "ModelDownloadManager"
    }
}
