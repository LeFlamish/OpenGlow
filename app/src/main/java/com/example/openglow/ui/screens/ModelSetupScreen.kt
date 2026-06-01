package com.example.openglow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.openglow.data.model.AppModelInfo
import com.example.openglow.data.model.ModelDownloadState
import com.example.openglow.data.model.ModelRegistry
import com.example.openglow.ui.theme.CardWhite

@Composable
fun ModelSetupScreen(
    viewModel: ModelSetupViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ModelStatusCard(title = "Gemini API") {
            Text("API key configured: ${uiState.geminiApiKeyConfigured}")
            Text("Gemini 키가 있으면 모델 다운로드 전에도 즉시 알림 분석이 가능합니다.")
        }

        ModelCard(
            model = ModelRegistry.recommendedLocalLlm,
            state = uiState.localLlmState,
            path = uiState.localLlmPath.orEmpty(),
            helperText = "로컬 AI 모델을 다운로드하면 Gemini 한도 초과 시 기기 안에서 알림을 분석할 수 있습니다. Wi-Fi 환경을 권장합니다.",
            onDownload = viewModel::downloadLocalLlm,
            onCancel = viewModel::cancelLocalLlm,
            onDelete = viewModel::deleteLocalLlm,
        )

        ModelCard(
            model = ModelRegistry.recommendedKcElectra,
            state = uiState.kcElectraState,
            path = uiState.kcElectraPath.orEmpty(),
            helperText = "KcELECTRA 분류 모델을 다운로드하면 중요도와 업무 관련 여부를 더 빠르게 판단할 수 있습니다. 원본 Hugging Face PyTorch 모델이 아니라 Android용 ONNX/TFLite 변환 모델을 받습니다.",
            onDownload = viewModel::downloadKcElectra,
            onCancel = viewModel::cancelKcElectra,
            onDelete = viewModel::deleteKcElectra,
        )

        ModelStatusCard(title = "현재 fallback 상태") {
            Text("Current classifier: ${uiState.currentClassifier}")
            Text("Current LLM fallback: ${uiState.currentLlmFallback}")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = viewModel::testGemini) {
                    Text("Gemini 테스트")
                }
                OutlinedButton(onClick = viewModel::testLocalLlm) {
                    Text("Local 테스트")
                }
            }
            OutlinedButton(onClick = viewModel::testClassifier) {
                Text("KcELECTRA 분류 테스트")
            }
            uiState.testMessage?.let {
                Text(it)
                OutlinedButton(onClick = viewModel::clearTestMessage) {
                    Text("메시지 지우기")
                }
            }
        }
    }
}

@Composable
private fun ModelCard(
    model: AppModelInfo,
    state: ModelDownloadState,
    path: String,
    helperText: String,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
) {
    ModelStatusCard(title = model.displayName) {
        Text(model.description)
        Text(helperText)
        Text("예상 용량: ${model.artifacts.sumOf { it.sizeBytes }.formatBytes()}")
        Text("저장 경로: $path", maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("필요 파일: ${model.artifacts.joinToString { it.fileName }}")
        Text("상태: ${state.label()}")
        if (state is ModelDownloadState.Downloading) {
            LinearProgressIndicator(
                progress = { state.progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
            Text("${state.currentFileName}: ${state.downloadedBytes.formatBytes()} / ${state.totalBytes.formatBytes()}")
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onDownload) {
                Text("다운로드")
            }
            OutlinedButton(onClick = onCancel) {
                Text("취소")
            }
            OutlinedButton(onClick = onDelete) {
                Text("삭제")
            }
        }
    }
}

@Composable
private fun ModelStatusCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(1.dp),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

private fun ModelDownloadState.label(): String = when (this) {
    ModelDownloadState.NotDownloaded -> "Not downloaded"
    ModelDownloadState.Checking -> "Checking"
    is ModelDownloadState.Downloading -> "Downloading"
    is ModelDownloadState.Verifying -> "Verifying"
    is ModelDownloadState.Ready -> "Ready"
    is ModelDownloadState.Failed -> "Failed: $message"
}

private fun Long.formatBytes(): String {
    if (this <= 0L) return "unknown"
    val mb = this / 1024.0 / 1024.0
    return if (mb >= 1024) {
        "%.1f GB".format(mb / 1024.0)
    } else {
        "%.1f MB".format(mb)
    }
}
