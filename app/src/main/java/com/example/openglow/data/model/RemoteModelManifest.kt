package com.example.openglow.data.model

data class RemoteModelManifest(
    val models: List<RemoteModelInfo>? = null,
)

data class RemoteModelInfo(
    val id: String? = null,
    val version: String? = null,
    val kind: String? = null,
    val targetDirectoryName: String? = null,
    val displayName: String? = null,
    val description: String? = null,
    val artifacts: List<RemoteModelArtifact>? = null,
)

data class RemoteModelArtifact(
    val fileName: String? = null,
    val url: String? = null,
    val downloadUrl: String? = null,
    val sha256: String? = null,
    val sizeBytes: Long? = null,
)

fun RemoteModelInfo.toAppModelInfo(fallback: AppModelInfo): AppModelInfo? {
    val modelId = id?.trim().orEmpty()
    val modelKind = kind
        ?.trim()
        ?.let { rawKind -> runCatching { ModelKind.valueOf(rawKind) }.getOrNull() }
        ?: return null
    val directoryName = targetDirectoryName?.trim().takeUnless { it.isNullOrBlank() }
        ?: fallback.targetDirectoryName
    val remoteArtifacts = artifacts.orEmpty().mapNotNull { artifact ->
        val fileName = artifact.fileName?.trim().takeUnless { it.isNullOrBlank() } ?: return@mapNotNull null
        ModelArtifactInfo(
            fileName = fileName,
            downloadUrl = artifact.url?.trim().takeUnless { it.isNullOrBlank() }
                ?: artifact.downloadUrl?.trim().orEmpty(),
            sha256 = artifact.sha256?.trim()?.lowercase().orEmpty(),
            sizeBytes = artifact.sizeBytes ?: 0L,
        )
    }

    return fallback.copy(
        id = modelId.ifBlank { fallback.id },
        kind = modelKind,
        displayName = displayName?.trim().takeUnless { it.isNullOrBlank() } ?: fallback.displayName,
        description = description?.trim().takeUnless { it.isNullOrBlank() } ?: fallback.description,
        artifacts = remoteArtifacts,
        targetDirectoryName = directoryName,
    )
}
