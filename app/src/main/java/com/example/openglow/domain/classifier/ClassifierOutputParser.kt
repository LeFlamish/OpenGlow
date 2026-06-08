package com.example.openglow.domain.classifier

import org.json.JSONObject

class ClassifierOutputParser(
    labelMapJson: String,
) {
    private val labelMap = runCatching { JSONObject(labelMapJson) }.getOrNull()

    fun isLabelMapReadable(): Boolean {
        return labelMap != null
    }
}
