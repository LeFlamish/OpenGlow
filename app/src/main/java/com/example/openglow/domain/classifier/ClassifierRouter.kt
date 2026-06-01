package com.example.openglow.domain.classifier

import android.util.Log
import javax.inject.Inject

class ClassifierRouter @Inject constructor(
    private val kcElectraClassifierClient: KcElectraClassifierClient,
    private val ruleBasedTextClassifier: RuleBasedTextClassifier,
) {
    suspend fun classify(text: String): ClassificationHint {
        return if (kcElectraClassifierClient.isAvailable()) {
            runCatching {
                kcElectraClassifierClient.classify(text)
            }.getOrElse {
                Log.w(TAG, "KcELECTRA fallback reason: ${it.message}")
                ruleBasedTextClassifier.classify(text)
            }
        } else {
            ruleBasedTextClassifier.classify(text)
        }
    }

    suspend fun currentClassifierName(): String {
        return if (kcElectraClassifierClient.isAvailable()) "KcELECTRA" else "RuleBased"
    }

    private companion object {
        private const val TAG = "ClassifierRouter"
    }
}
