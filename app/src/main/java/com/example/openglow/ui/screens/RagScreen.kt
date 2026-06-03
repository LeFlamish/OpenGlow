package com.example.openglow.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.openglow.ui.theme.BackgroundGray

@Composable
fun RagScreen(viewModel: RagViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("노트 기반 RAG", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("저장된 노트 요약을 동기화한 뒤 질문합니다.", style = MaterialTheme.typography.bodyMedium)
            }
            OutlinedButton(
                onClick = viewModel::testServerConnection,
                enabled = !state.isTestingConnection,
            ) {
                Text(if (state.isTestingConnection) "확인 중" else "서버 연결 확인")
            }
        }

        RagCard(title = "동기화 상태") {
            if (state.isLoadingStatus) {
                CircularProgressIndicator()
            } else {
                Text("전체 노트: ${state.totalCount}개")
                Text("동기화 완료: ${state.uploadedCount}개")
                Text("동기화 필요: ${state.notUploadedCount}개")
            }
            state.syncMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            Button(
                onClick = viewModel::syncNotifications,
                enabled = !state.isSyncing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White,
                    )
                    Text("  동기화 중입니다...")
                } else {
                    Text("노트 데이터 동기화")
                }
            }
        }

        RagCard(title = "질문") {
            OutlinedTextField(
                value = state.question,
                onValueChange = viewModel::updateQuestion,
                placeholder = { Text("예: 최근 중요한 일정과 해야 할 일을 알려줘") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
            )
            Button(
                onClick = viewModel::askQuestion,
                enabled = !state.isAsking,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.isAsking) "답변을 생성하는 중입니다..." else "질문하기")
            }
        }

        RagCard(title = "답변") {
            Text(state.answer ?: "아직 질문이 없습니다.")
        }

        state.errorMessage?.let { error ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun RagCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}
