package com.example.openglow.domain.classifier

import org.json.JSONObject

class ClassifierOutputParser(
    labelMapJson: String,
) {
    private val labelMap = runCatching { JSONObject(labelMapJson) }.getOrNull()

    fun parsePrototypeFallback(
        tokenizedText: TokenizedText,
        ruleHint: ClassificationHint,
    ): ClassificationHint {
        // Real ONNX/TFLite output shapes should be parsed here once the converted model is published.
        // The label_map.json contract lets the parser support either multi-head or single-logits models.
        val modelName = if (labelMap != null && tokenizedText.inputIds.isNotEmpty()) {
            "KcELECTRA"
        } else {
            "KcELECTRA-unconfigured"
        }
        return ruleHint.copy(
            confidence = (ruleHint.confidence + 0.08f).coerceAtMost(0.95f),
            modelName = modelName,
        )
    }
}
