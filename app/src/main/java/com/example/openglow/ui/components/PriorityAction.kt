package com.example.openglow.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.openglow.ui.screens.EventData
import com.example.openglow.ui.theme.CardWhite
import com.example.openglow.ui.theme.PointBlue
import java.time.format.DateTimeFormatter

@Composable
fun PrioritySection(
    priorityList: List<EventData>, // 상위 5개 일정 받아옴
    fullEventList: List<EventData>, // 수정을 위한 전체 캘린더 리스트
    onEventListChange: (List<EventData>) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedTaskForEdit by remember { mutableStateOf<EventData?>(null) }

    Column {
        Text("우선순위 액션", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(12.dp))

        if (priorityList.isEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = CardWhite), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                    Text("마감에 임박한 일정이 없습니다.", color = Color.Gray, fontSize = 14.sp)
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                priorityList.forEach { task ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardWhite),
                        elevation = CardDefaults.cardElevation(2.dp),
                        modifier = Modifier.fillMaxWidth().clickable { selectedTaskForEdit = task }
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(color = task.type.color, shape = RoundedCornerShape(4.dp)) {
                                    Text(text = task.type.label, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(text = task.title, fontWeight = FontWeight.Bold)
                            }
                            val formatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 a h시 mm분")
                            Text(text = "마감: ${task.deadlineTime.format(formatter)}", color = task.type.color, fontSize = 12.sp, modifier = Modifier.padding(start = 40.dp, top = 4.dp))
                            if (task.location.isNotBlank()) {
                                Text(text = "장소: ${task.location}", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(start = 40.dp, top = 2.dp))
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { showAddDialog = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = PointBlue.copy(alpha = 0.1f))
        ) { Text("+ 계획 추가하기", color = PointBlue, fontWeight = FontWeight.Bold) }
    }

    if (showAddDialog) {
        AddEventDialog(
            onDismiss = { showAddDialog = false },
            onSave = { title, type, deadlineTime, location ->
                val newEvent = EventData(title = title, type = type, deadlineTime = deadlineTime, location = location)
                onEventListChange(fullEventList + newEvent)
                showAddDialog = false
            }
        )
    }

    selectedTaskForEdit?.let { targetTask ->
        EditDeleteEventDialog(
            event = targetTask,
            onDismiss = { selectedTaskForEdit = null },
            onUpdate = { newTitle, newType, newDeadlineTime, newLocation ->
                onEventListChange(fullEventList.map {
                    if (it.id == targetTask.id) it.copy(title = newTitle, type = newType, deadlineTime = newDeadlineTime, location = newLocation) else it
                })
                selectedTaskForEdit = null
            },
            onDelete = {
                onEventListChange(fullEventList.filter { it.id != targetTask.id })
                selectedTaskForEdit = null
            }
        )
    }
}