package com.example.openglow.data.localai

import android.util.Log
import com.example.openglow.BuildConfig
import java.io.File
import javax.inject.Inject

class ConfigurableLocalLlmClient @Inject constructor() : LocalLlmClient {
    override suspend fun isAvailable(): Boolean {
        val enabled = BuildConfig.ENABLE_LOCAL_LLM
        val path = BuildConfig.LOCAL_LLM_MODEL_PATH
        val available = enabled && path.isNotBlank() && File(path).exists()
        Log.i(TAG, "Local LLM availability: enabled=$enabled, hasPath=${path.isNotBlank()}, available=$available")
        return available
    }

    override suspend fun analyze(prompt: String): Result<String> {
        if (!isAvailable()) {
            return Result.failure(IllegalStateException("Local LLM model is not configured"))
        }

        // This is the stable integration point for MediaPipe LLM Inference or LiteRT-LM.
        // Model bytes are intentionally not bundled in Git; if runtime loading is not wired,
        // the router continues to the rule fallback without crashing the app.
        return Result.failure(UnsupportedOperationException("Local LLM runtime is not wired yet"))
    }

    private companion object {
        private const val TAG = "LocalLlmClient"
    }
}
