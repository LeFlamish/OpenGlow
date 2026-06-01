package com.example.openglow.data.localai

import android.util.Log
import com.example.openglow.BuildConfig
import com.example.openglow.data.model.ModelDownloadManager
import com.example.openglow.domain.llm.AnalysisPromptBuilder
import com.example.openglow.domain.llm.NoteUpdateInput
import java.io.File
import javax.inject.Inject

class LiteRtLocalLlmClient @Inject constructor(
    private val modelDownloadManager: ModelDownloadManager,
) : LocalLlmClient {
    @Volatile private var cachedModelFile: File? = null

    override suspend fun isAvailable(): Boolean {
        val modelFile = resolveModelFile()
        val available = BuildConfig.LOCAL_LLM_ENABLED && modelFile != null
        Log.i(
            TAG,
            "Local LLM availability: enabled=${BuildConfig.LOCAL_LLM_ENABLED}, " +
                "backend=${BuildConfig.LOCAL_LLM_BACKEND}, available=$available",
        )
        return available
    }

    override suspend fun analyze(input: NoteUpdateInput): Result<String> {
        val modelFile = resolveModelFile()
            ?: return Result.failure(IllegalStateException("Local LLM model is not available"))

        val prompt = AnalysisPromptBuilder.buildLocalPrompt(input)
        Log.i(TAG, "Local LLM request prepared: backend=${BuildConfig.LOCAL_LLM_BACKEND}, modelBytes=${modelFile.length()}")

        // Stable integration point for LiteRT-LM.
        // The engine must be initialized off the UI thread. Prefer GPU backend, then CPU fallback.
        // Until the LiteRT runtime binding is wired, return failure so LlmRouter falls through to rules.
        return Result.failure(
            UnsupportedOperationException(
                "LiteRT-LM runtime is not wired yet; promptLength=${prompt.length}",
            ),
        )
    }

    fun clearEngineCache() {
        cachedModelFile = null
    }

    private fun resolveModelFile(): File? {
        val debugFile = BuildConfig.DEBUG_LOCAL_LLM_MODEL_PATH
            .takeIf { BuildConfig.DEBUG && it.isNotBlank() }
            ?.let(::File)
            ?.takeIf { it.exists() && it.isFile }
        if (debugFile != null) {
            cachedModelFile = debugFile
            return debugFile
        }

        val downloaded = modelDownloadManager.getLocalLlmModelFile()
        cachedModelFile = downloaded
        return downloaded
    }

    private companion object {
        private const val TAG = "LocalLlmClient"
    }
}
