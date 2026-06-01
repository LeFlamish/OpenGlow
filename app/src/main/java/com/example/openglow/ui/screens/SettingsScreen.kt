package com.example.openglow.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.openglow.BuildConfig
import com.example.openglow.ui.theme.BackgroundGray
import com.example.openglow.ui.theme.CardWhite

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.exportMessage) {
        uiState.exportMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    androidx.compose.material3.Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundGray,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundGray)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "설정",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            SettingsCard(title = "민감한 알림 분석") {
                Text(
                    text = "알림 원문 전체는 로그에 남기지 않고, 분석 기록에는 120자 미리보기와 해시만 저장합니다.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            SettingsCard(title = "피드백 기반 개인화 데이터") {
                Text(
                    text = "사용자가 수정한 중요도, 업무 관련 여부, 개인/그룹 판단을 JSONL로 내보냅니다. 앱 안에서 직접 학습하지 않고, PC나 서버에서 LoRA/adapter 학습에 사용할 수 있는 데이터만 생성합니다.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isExporting,
                        onClick = { viewModel.exportTrainingData(includeTextPreview = false) },
                    ) {
                        Text("보호 모드 export")
                    }
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isExporting,
                        onClick = { viewModel.exportTrainingData(includeTextPreview = true) },
                    ) {
                        Text("미리보기 포함")
                    }
                }
            }

            SettingsCard(title = "로컬 모델") {
                Text(
                    text = "ENABLE_LOCAL_LLM=${BuildConfig.ENABLE_LOCAL_LLM}, 모델 경로 설정=${BuildConfig.LOCAL_LLM_MODEL_PATH.isNotBlank()}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "모델 파일은 Git에 포함하지 않습니다. 생성된 LoRA/adapter 또는 교체 모델은 설정된 경로에 배치한 뒤 LocalLlmClient 런타임에 연결하는 구조입니다.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(
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
