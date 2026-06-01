package com.example.openglow.domain.classifier

import android.util.Log
import javax.inject.Inject

class KcElectraClassifierClient @Inject constructor(
    private val modelLoader: ClassifierModelLoader,
    private val ruleBasedTextClassifier: RuleBasedTextClassifier,
) : TextClassifierClient {
    override suspend fun isAvailable(): Boolean {
        val files = modelLoader.loadFiles()
        val available = files != null
        Log.i(TAG, "KcELECTRA availability: $available")
        return available
    }

    override suspend fun classify(text: String): ClassificationHint {
        val files = modelLoader.loadFiles()
            ?: throw IllegalStateException("KcELECTRA model files are not ready")

        val ruleHint = ruleBasedTextClassifier.classify(text)
        return runCatching {
            val tokenizer = KoreanTokenizer(files.vocabFile)
            val tokenized = tokenizer.tokenize(text.limitForClassifier())
            val outputParser = ClassifierOutputParser(files.labelMapFile.readText(Charsets.UTF_8))

            // Integration point for ONNX Runtime Mobile or TFLite inference.
            // The converted Android model should consume input_ids, attention_mask, token_type_ids.
            outputParser.parsePrototypeFallback(tokenized, ruleHint)
        }.getOrElse {
            Log.w(TAG, "KcELECTRA inference failed: ${it.message}")
            throw it
        }
    }

    private fun String.limitForClassifier(): String {
        if (length <= 900) return this
        return take(450) + "\n" + takeLast(450)
    }

    private companion object {
        private const val TAG = "KcElectraClassifier"
    }
}
