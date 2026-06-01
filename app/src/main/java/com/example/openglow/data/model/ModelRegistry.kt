package com.example.openglow.data.model

enum class ModelKind {
    LOCAL_LLM,
    TEXT_CLASSIFIER,
}

data class ModelArtifactInfo(
    val fileName: String,
    val downloadUrl: String,
    val sha256: String,
    val sizeBytes: Long,
)

data class AppModelInfo(
    val id: String,
    val kind: ModelKind,
    val displayName: String,
    val description: String,
    val recommended: Boolean,
    val artifacts: List<ModelArtifactInfo>,
    val targetDirectoryName: String,
)

object ModelRegistry {
    val recommendedLocalLlm = AppModelInfo(
        id = "qwen2_5_1_5b_litertlm",
        kind = ModelKind.LOCAL_LLM,
        displayName = "Qwen2.5 1.5B Local LLM",
        description = "한국어 알림 요약 및 노트 정리용 로컬 LLM",
        recommended = true,
        targetDirectoryName = "local_llm",
        artifacts = listOf(
            ModelArtifactInfo(
                fileName = "model.litertlm",
                downloadUrl = "TODO_MODEL_DOWNLOAD_URL",
                sha256 = "TODO_SHA256",
                sizeBytes = 1_600_000_000L,
            ),
        ),
    )

    val recommendedKcElectra = AppModelInfo(
        id = "kcelectra_classifier",
        kind = ModelKind.TEXT_CLASSIFIER,
        displayName = "KcELECTRA Korean Notification Classifier",
        description = "중요도, 업무 여부, 회의/프로젝트 여부 분류용 한국어 분류 모델",
        recommended = true,
        targetDirectoryName = "kcelectra",
        artifacts = listOf(
            ModelArtifactInfo(
                fileName = "model.onnx",
                downloadUrl = "TODO_KCELECTRA_ONNX_URL",
                sha256 = "TODO_SHA256",
                sizeBytes = 300_000_000L,
            ),
            ModelArtifactInfo(
                fileName = "vocab.txt",
                downloadUrl = "TODO_KCELECTRA_VOCAB_URL",
                sha256 = "TODO_SHA256",
                sizeBytes = 1_000_000L,
            ),
            ModelArtifactInfo(
                fileName = "tokenizer_config.json",
                downloadUrl = "TODO_KCELECTRA_TOKENIZER_CONFIG_URL",
                sha256 = "TODO_SHA256",
                sizeBytes = 100_000L,
            ),
            ModelArtifactInfo(
                fileName = "label_map.json",
                downloadUrl = "TODO_KCELECTRA_LABEL_MAP_URL",
                sha256 = "TODO_SHA256",
                sizeBytes = 10_000L,
            ),
        ),
    )

    val allModels = listOf(recommendedLocalLlm, recommendedKcElectra)

    fun byId(modelId: String): AppModelInfo? = allModels.firstOrNull { it.id == modelId }
}
