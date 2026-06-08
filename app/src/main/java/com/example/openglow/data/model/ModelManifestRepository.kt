package com.example.openglow.data.model

import android.util.Log
import com.example.openglow.BuildConfig
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

@Singleton
class ModelManifestRepository @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson,
) {
    @Volatile private var cachedManifest: RemoteModelManifest? = null

    suspend fun getLocalLlmModel(): AppModelInfo {
        return resolveModel(
            fallback = ModelRegistry.recommendedLocalLlm,
            kind = ModelKind.LOCAL_LLM,
            fallbackIds = setOf(ModelRegistry.LOCAL_LLM_ID, "qwen2_5_1_5b_litertlm"),
        )
    }

    suspend fun getKcElectraModel(): AppModelInfo {
        return resolveModel(
            fallback = ModelRegistry.recommendedKcElectra,
            kind = ModelKind.TEXT_CLASSIFIER,
            fallbackIds = setOf(ModelRegistry.KC_ELECTRA_ID),
        )
    }

    suspend fun resolveModel(
        fallback: AppModelInfo,
        kind: ModelKind = fallback.kind,
        fallbackIds: Set<String> = setOf(fallback.id),
    ): AppModelInfo {
        val manifest = loadManifestOrNull() ?: return fallback
        val remote = manifest.models
            .orEmpty()
            .firstNotNullOfOrNull { candidate ->
                val mapped = candidate.toAppModelInfo(fallback) ?: return@firstNotNullOfOrNull null
                val idMatches = mapped.id == fallback.id || mapped.id in fallbackIds
                val kindMatches = mapped.kind == kind
                mapped.takeIf { kindMatches && (idMatches || kind == ModelKind.LOCAL_LLM) }
            }
            ?: return fallback

        return remote.copy(id = fallback.id, recommended = fallback.recommended)
    }

    suspend fun refreshModelStates(modelDownloadManager: ModelDownloadManager) {
        modelDownloadManager.refreshModelState(getLocalLlmModel())
        modelDownloadManager.refreshModelState(getKcElectraModel())
    }

    private suspend fun loadManifestOrNull(): RemoteModelManifest? = withContext(Dispatchers.IO) {
        if (BuildConfig.MODEL_MANIFEST_URL.isBlank()) {
            return@withContext null
        }
        cachedManifest?.let { return@withContext it }

        runCatching {
            val request = Request.Builder()
                .url(BuildConfig.MODEL_MANIFEST_URL)
                .build()
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                error("HTTP ${response.code} while loading model manifest")
            }
            val body = response.body?.string().orEmpty()
            gson.fromJson(body, RemoteModelManifest::class.java)
        }.onSuccess {
            cachedManifest = it
            Log.i(TAG, "Model manifest loaded: url=${BuildConfig.MODEL_MANIFEST_URL}, models=${it.models.orEmpty().size}")
        }.onFailure {
            Log.w(TAG, "Model manifest load failed: ${it.message}")
        }.getOrNull()
    }

    private companion object {
        private const val TAG = "ModelManifestRepository"
    }
}
