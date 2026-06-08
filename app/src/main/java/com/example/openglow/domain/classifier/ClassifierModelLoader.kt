package com.example.openglow.domain.classifier

import com.example.openglow.data.model.ModelDownloadManager
import java.io.File
import javax.inject.Inject

data class ClassifierModelFiles(
    val modelFile: File,
    val vocabFile: File,
    val tokenizerConfigFile: File,
    val labelMapFile: File,
)

class ClassifierModelLoader @Inject constructor(
    private val modelDownloadManager: ModelDownloadManager,
) {
    fun loadFiles(): ClassifierModelFiles? {
        val directory = modelDownloadManager.getKcElectraModelDirectory() ?: return null
        val model = File(directory, "model.tflite")
            .takeIf { it.exists() && it.length() > 0L }
            ?: return null

        val vocab = File(directory, "vocab.txt").takeIf { it.exists() && it.length() > 0L } ?: return null
        val tokenizerConfig = File(directory, "tokenizer_config.json")
            .takeIf { it.exists() && it.length() > 0L }
            ?: return null
        val labelMap = File(directory, "label_map.json").takeIf { it.exists() && it.length() > 0L } ?: return null

        return ClassifierModelFiles(
            modelFile = model,
            vocabFile = vocab,
            tokenizerConfigFile = tokenizerConfig,
            labelMapFile = labelMap,
        )
    }
}
