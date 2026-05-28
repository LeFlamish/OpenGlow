package com.example.openglow.ui.components // ⚠️ 패키지명 확인

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.openglow.ui.theme.CardWhite
import com.example.openglow.ui.theme.PointBlue
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

enum class NoteType(val label: String) {
    PERSON("개인"),
    GROUP("그룹"),
    FAVORITE("즐겨찾기")
}

data class ConversationNote(
    val id: String = UUID.randomUUID().toString(),
    val summary: String,
    val createdAt: LocalDateTime = LocalDateTime.now()
)

data class ContactEntity(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: NoteType,
    val isFavorite: Boolean = false,
    val notes: List<ConversationNote> = emptyList(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

@Composable
fun ContactCard(
    contact: ContactEntity,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(1.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = contact.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "대화 내역 ${contact.notes.size}개", color = Color.Gray, fontSize = 13.sp)
            }
            IconButton(onClick = onFavoriteToggle) {
                Icon(
                    imageVector = if (contact.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                    contentDescription = "즐겨찾기",
                    tint = if (contact.isFavorite) Color(0xFFFFC107) else Color.LightGray
                )
            }
        }
    }
}

// 🌟 수정: 대화 내역 카드에 클릭(onClick) 기능을 추가했습니다.
@Composable
fun ConversationNoteCard(note: ConversationNote, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() } // 클릭 가능하게 변경
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = note.summary, fontSize = 15.sp, color = Color.DarkGray)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = note.createdAt.format(DateTimeFormatter.ofPattern("yyyy.MM.dd a h:mm")),
                color = Color.Gray, fontSize = 11.sp, modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
fun AddContactDialog(
    currentTabType: NoteType,
    onDismiss: () -> Unit,
    onSave: (name: String, type: NoteType) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(if (currentTabType == NoteType.FAVORITE) NoteType.PERSON else currentTabType) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("새 연락처 추가", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TabRow(selectedTabIndex = if (selectedType == NoteType.PERSON) 0 else 1, containerColor = Color.Transparent, indicator = {}, divider = {}) {
                    listOf(NoteType.PERSON, NoteType.GROUP).forEach { type ->
                        val isSelected = selectedType == type
                        FilterChip(
                            selected = isSelected, onClick = { selectedType = type }, label = { Text(type.label, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center) },
                            modifier = Modifier.padding(horizontal = 4.dp), colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PointBlue.copy(alpha = 0.2f), selectedLabelColor = PointBlue)
                        )
                    }
                }
                OutlinedTextField(value = name, onValueChange = { name = it.replace(Regex("[|&@~]"), "") }, label = { Text("인물 또는 그룹 이름") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소", color = Color.Gray) } },
        confirmButton = { Button(onClick = { onSave(name, selectedType) }, enabled = name.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = PointBlue)) { Text("추가") } },
        shape = RoundedCornerShape(16.dp), containerColor = Color.White
    )
}

@Composable
fun AddConversationDialog(
    onDismiss: () -> Unit,
    onSave: (summary: String) -> Unit
) {
    var summary by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("대화 내역 추가", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(value = summary, onValueChange = { summary = it.replace(Regex("[|&@~]"), "") }, label = { Text("내용 요약") }, modifier = Modifier.fillMaxWidth().height(120.dp), maxLines = 5)
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소", color = Color.Gray) } },
        confirmButton = { Button(onClick = { onSave(summary) }, enabled = summary.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = PointBlue)) { Text("저장") } },
        shape = RoundedCornerShape(16.dp), containerColor = Color.White
    )
}

@Composable
fun EditDeleteContactDialog(
    contact: ContactEntity,
    onDismiss: () -> Unit,
    onUpdate: (name: String, type: NoteType) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(contact.name) }
    var selectedType by remember { mutableStateOf(contact.type) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("연락처 수정 및 삭제", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TabRow(selectedTabIndex = if (selectedType == NoteType.PERSON) 0 else 1, containerColor = Color.Transparent, indicator = {}, divider = {}) {
                    listOf(NoteType.PERSON, NoteType.GROUP).forEach { type ->
                        val isSelected = selectedType == type
                        FilterChip(
                            selected = isSelected, onClick = { selectedType = type }, label = { Text(type.label, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center) },
                            modifier = Modifier.padding(horizontal = 4.dp), colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PointBlue.copy(alpha = 0.2f), selectedLabelColor = PointBlue)
                        )
                    }
                }
                OutlinedTextField(value = name, onValueChange = { name = it.replace(Regex("[|&@~]"), "") }, label = { Text("인물 또는 그룹 이름") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
        },
        dismissButton = {
            TextButton(onClick = onDelete) { Text("삭제", color = Color.Red, fontWeight = FontWeight.Bold) }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismiss) { Text("취소", color = Color.Gray) }
                Button(onClick = { onUpdate(name, selectedType) }, enabled = name.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = PointBlue)) { Text("수정 완료") }
            }
        },
        shape = RoundedCornerShape(16.dp), containerColor = Color.White
    )
}

// 🌟 신규 추가: 대화 내역 자체를 수정하거나 삭제하는 다이얼로그
@Composable
fun EditDeleteConversationDialog(
    note: ConversationNote,
    onDismiss: () -> Unit,
    onUpdate: (summary: String) -> Unit,
    onDelete: () -> Unit
) {
    var summary by remember { mutableStateOf(note.summary) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("대화 내역 수정 및 삭제", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(value = summary, onValueChange = { summary = it.replace(Regex("[|&@~]"), "") }, label = { Text("내용 요약") }, modifier = Modifier.fillMaxWidth().height(120.dp), maxLines = 5)
        },
        dismissButton = {
            TextButton(onClick = onDelete) { Text("삭제", color = Color.Red, fontWeight = FontWeight.Bold) }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismiss) { Text("취소", color = Color.Gray) }
                Button(onClick = { onUpdate(summary) }, enabled = summary.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = PointBlue)) { Text("수정 완료") }
            }
        },
        shape = RoundedCornerShape(16.dp), containerColor = Color.White
    )
}