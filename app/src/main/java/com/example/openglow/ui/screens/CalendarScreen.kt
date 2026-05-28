package com.example.openglow.ui.screens // ⚠️ 패키지명 확인

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.openglow.ui.components.*
import com.example.openglow.ui.theme.BackgroundGray
import com.example.openglow.ui.theme.CardWhite
import com.example.openglow.ui.theme.PointBlue
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun CalendarScreen(
    eventList: List<EventData>,
    onEventListChange: (List<EventData>) -> Unit
) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedEventForEdit by remember { mutableStateOf<EventData?>(null) }

    val filteredEvents = eventList.filter { it.deadlineTime.toLocalDate() == selectedDate }

    Box(modifier = Modifier.fillMaxSize().background(BackgroundGray)) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            CalendarHeader(currentMonth = currentMonth, onPreviousMonth = { currentMonth = currentMonth.minusMonths(1) }, onNextMonth = { currentMonth = currentMonth.plusMonths(1) })
            Spacer(modifier = Modifier.height(16.dp))
            Card(colors = CardDefaults.cardColors(containerColor = CardWhite), elevation = CardDefaults.cardElevation(2.dp), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    DaysOfWeekHeader()
                    Spacer(modifier = Modifier.height(8.dp))
                    CalendarGrid(currentMonth = currentMonth, selectedDate = selectedDate, onDateSelected = { selectedDate = it })
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "${selectedDate.monthValue}월 ${selectedDate.dayOfMonth}일의 일정 (${filteredEvents.size}개)", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 12.dp))

            if (filteredEvents.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("등록된 일정이 없습니다.", color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                    items(filteredEvents, key = { it.id }) { event ->
                        val timeFormatter = DateTimeFormatter.ofPattern("a h시 mm분")

                        // 🌟 [요구사항 반영]: 제목에서 태그 글자를 제거하고 오직 본래 제목(event.title)만 출력합니다.
                        // 🌟 또한 event.type.color를 토스하여 긴급일 때 빨간색 선이 나오도록 구현했습니다.
                        EventCard(
                            time = event.deadlineTime.format(timeFormatter),
                            title = event.title,
                            location = event.location,
                            color = event.type.color,
                            onEventClick = { selectedEventForEdit = event }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true }, containerColor = PointBlue, contentColor = Color.White, shape = CircleShape, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 16.dp)
        ) { Icon(Icons.Default.Add, contentDescription = "일정 추가") }

        if (showAddDialog) {
            AddEventDialog(
                initialDate = selectedDate.atTime(12, 0),
                onDismiss = { showAddDialog = false },
                onSave = { title, type, deadlineTime, location ->
                    val newEvent = EventData(title = title, type = type, deadlineTime = deadlineTime, location = location)
                    onEventListChange(eventList + newEvent)
                    showAddDialog = false
                }
            )
        }

        selectedEventForEdit?.let { targetEvent ->
            EditDeleteEventDialog(
                event = targetEvent,
                onDismiss = { selectedEventForEdit = null },
                onUpdate = { newTitle, newType, newDeadlineTime, newLocation ->
                    onEventListChange(eventList.map { if (it.id == targetEvent.id) it.copy(title = newTitle, type = newType, deadlineTime = newDeadlineTime, location = newLocation) else it })
                    selectedEventForEdit = null
                },
                onDelete = {
                    onEventListChange(eventList.filter { it.id != targetEvent.id })
                    selectedEventForEdit = null
                }
            )
        }
    }
}