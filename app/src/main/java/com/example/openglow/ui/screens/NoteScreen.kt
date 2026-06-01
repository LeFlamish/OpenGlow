package com.example.openglow.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.openglow.ui.theme.BackgroundGray
import com.example.openglow.ui.theme.CardWhite
import com.example.openglow.ui.theme.PointBlue
import com.example.openglow.ui.theme.SoftGray
import com.example.openglow.ui.theme.UrgentRed

@Composable
fun NoteScreen(
    viewModel: NoteViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedNote by remember { mutableStateOf<NoteUiModel?>(null) }
    var feedbackNote by remember { mutableStateOf<NoteUiModel?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.feedbackMessage) {
        uiState.feedbackMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFeedbackMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundGray,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundGray)
                .padding(padding)
                .padding(16.dp),
        ) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = viewModel::updateQuery,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("노트 검색") },
            )

            Spacer(modifier = Modifier.height(12.dp))

            ScopeFilterRow(
                selectedFilter = uiState.selectedFilter,
                onFilterSelected = viewModel::selectFilter,
            )

            Spacer(modifier = Modifier.height(8.dp))

            ImportanceFilterRow(
                selectedFilter = uiState.selectedImportance,
                onFilterSelected = viewModel::selectImportance,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("업무 관련만 보기", color = Color.DarkGray, fontSize = 14.sp)
                Switch(checked = uiState.workOnly, onCheckedChange = viewModel::setWorkOnly)
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.notes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("아직 정리된 노트가 없습니다.", color = SoftGray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(uiState.notes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            onClick = { selectedNote = note },
                        )
                    }
                }
            }
        }
    }

    selectedNote?.let { note ->
        NoteDetailDialog(
            note = note,
            onDismiss = { selectedNote = null },
            onFeedbackClick = {
                selectedNote = null
                feedbackNote = note
            },
        )
    }

    feedbackNote?.let { note ->
        FeedbackDialog(
            note = note,
            onDismiss = { feedbackNote = null },
            onSubmit = { importance, isWorkRelated, scope, comment ->
                viewModel.submitFeedback(
                    noteId = note.id,
                    correctedImportance = importance,
                    correctedIsWorkRelated = isWorkRelated,
                    correctedSenderScope = scope,
                    userComment = comment,
                )
                feedbackNote = null
            },
        )
    }
}

@Composable
private fun ScopeFilterRow(
    selectedFilter: NoteFilter,
    onFilterSelected: (NoteFilter) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        NoteFilter.entries.forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PointBlue.copy(alpha = 0.14f),
                    selectedLabelColor = PointBlue,
                ),
            )
        }
    }
}

@Composable
private fun ImportanceFilterRow(
    selectedFilter: ImportanceFilter,
    onFilterSelected: (ImportanceFilter) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ImportanceFilter.entries.forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = importanceColor(filter.name).copy(alpha = 0.14f),
                    selectedLabelColor = importanceColor(filter.name),
                ),
            )
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun NoteCard(
    note: NoteUiModel,
    onClick: () -> Unit,
) {
    val importanceColor = importanceColor(note.aggregateImportance)

    Card(
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(1.dp),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = note.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${note.senderDisplayName} · ${note.updatedAtText}",
                        color = SoftGray,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                AssistChip(
                    onClick = {},
                    label = { Text(scopeLabel(note.senderScope)) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = PointBlue.copy(alpha = 0.10f),
                        labelColor = PointBlue,
                    ),
                    border = null,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = note.latestOneLineSummary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = Color.DarkGray,
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = note.finalSummary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = Color.DarkGray,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                NoteChip(text = note.platform, color = SoftGray)
                NoteChip(text = "중요도 ${importanceLabel(note.aggregateImportance)}", color = importanceColor)
                NoteChip(
                    text = if (note.aggregateIsWorkRelated) "업무 관련" else "업무 외",
                    color = if (note.aggregateIsWorkRelated) PointBlue else SoftGray,
                )
                if (note.latestMeetingDetected) {
                    NoteChip(text = "회의", color = PointBlue)
                }
                if (note.latestProjectDetected) {
                    NoteChip(text = "프로젝트", color = PointBlue)
                }
                if (!note.latestDeadlineText.isNullOrBlank()) {
                    NoteChip(text = note.latestDeadlineText, color = UrgentRed)
                }
                if (note.calendarCandidate) {
                    NoteChip(text = "캘린더 후보", color = UrgentRed)
                }
                NoteChip(text = modelSourceLabel(note.modelSource), color = SoftGray)
            }

            if (note.actionItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    note.actionItems.take(3).forEach { item ->
                        Text(
                            text = "- $item",
                            color = Color.DarkGray,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "알림 ${note.notificationCount}개 누적 · 신뢰도 ${(note.confidence * 100).toInt()}%",
                color = SoftGray,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun NoteChip(text: String, color: Color) {
    AssistChip(
        onClick = {},
        label = { Text(text, fontSize = 12.sp) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = color.copy(alpha = 0.10f),
            labelColor = color,
        ),
        border = null,
    )
}

@Composable
private fun NoteDetailDialog(
    note: NoteUiModel,
    onDismiss: () -> Unit,
    onFeedbackClick: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("닫기")
            }
        },
        dismissButton = {
            TextButton(onClick = onFeedbackClick) {
                Text("판단 수정하기")
            }
        },
        title = {
            Text(
                text = note.title,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        text = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(440.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("최종 정리본", fontWeight = FontWeight.Bold)
                    Text(
                        text = note.finalSummary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    )

                    if (note.retainedFacts.isNotEmpty()) {
                        Text("보존된 핵심 사실", fontWeight = FontWeight.Bold)
                        note.retainedFacts.forEach {
                            Text("- $it", fontSize = 13.sp, lineHeight = 18.sp)
                        }
                    }

                    if (note.actionItems.isNotEmpty()) {
                        Text("최근 할 일", fontWeight = FontWeight.Bold)
                        note.actionItems.forEach {
                            Text("- $it", fontSize = 13.sp, lineHeight = 18.sp)
                        }
                    }
                }
            }
        },
        shape = RoundedCornerShape(8.dp),
        containerColor = Color.White,
    )
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun FeedbackDialog(
    note: NoteUiModel,
    onDismiss: () -> Unit,
    onSubmit: (String?, Boolean?, String?, String?) -> Unit,
) {
    var selectedImportance by remember(note.id) { mutableStateOf(note.aggregateImportance) }
    var selectedWorkRelated by remember(note.id) { mutableStateOf(note.aggregateIsWorkRelated) }
    var selectedScope by remember(note.id) { mutableStateOf(note.senderScope) }
    var comment by remember(note.id) { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("이 판단이 맞나요?", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(note.latestOneLineSummary, fontSize = 14.sp, lineHeight = 20.sp)

                Text("중요도", fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("LOW", "NORMAL", "HIGH", "URGENT").forEach { importance ->
                        FilterChip(
                            selected = selectedImportance == importance,
                            onClick = { selectedImportance = importance },
                            label = { Text(importanceLabel(importance)) },
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("업무 관련")
                    Switch(checked = selectedWorkRelated, onCheckedChange = { selectedWorkRelated = it })
                }

                Text("개인/그룹", fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("INDIVIDUAL", "GROUP", "UNKNOWN").forEach { scope ->
                        FilterChip(
                            selected = selectedScope == scope,
                            onClick = { selectedScope = scope },
                            label = { Text(scopeLabel(scope)) },
                        )
                    }
                }

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    label = { Text("간단 코멘트") },
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmit(
                        selectedImportance,
                        selectedWorkRelated,
                        selectedScope,
                        comment,
                    )
                },
            ) {
                Text("저장")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        },
        shape = RoundedCornerShape(8.dp),
        containerColor = Color.White,
    )
}

private fun scopeLabel(scope: String): String = when (scope) {
    "GROUP" -> "그룹"
    "UNKNOWN" -> "불확실"
    else -> "개인"
}

private fun modelSourceLabel(source: String): String = when (source) {
    "GEMINI" -> "Gemini"
    "LOCAL_LLM" -> "Local"
    else -> "Rule"
}

private fun importanceLabel(importance: String): String = when (importance) {
    "LOW" -> "낮음"
    "HIGH" -> "높음"
    "URGENT" -> "긴급"
    else -> "보통"
}

private fun importanceColor(importance: String): Color = when (importance) {
    "LOW" -> SoftGray
    "HIGH" -> Color(0xFFF59E0B)
    "URGENT" -> UrgentRed
    else -> PointBlue
}
