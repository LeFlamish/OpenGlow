package com.example.openglow.domain.classifier

import android.util.Log
import javax.inject.Inject

class KcElectraClassifierClient @Inject constructor(
    private val modelLoader: ClassifierModelLoader,
) : TextClassifierClient {
    override suspend fun isAvailable(): Boolean {
        val filesReady = modelLoader.loadFiles() != null
        Log.i(TAG, "KcELECTRA availability: filesReady=$filesReady, runtimeConfigured=false")
        return false
    }

    override suspend fun classify(text: String): ClassificationHint {
        throw IllegalStateException(
            "KcELECTRA는 아직 설정되지 않았습니다. 현재는 RuleBased 분류기를 사용합니다.",
        )
    }

    private companion object {
        private const val TAG = "KcElectraClassifier"
    }
}
