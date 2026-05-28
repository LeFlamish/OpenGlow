package com.example.openglow.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.openglow.ui.screens.EventData
import com.example.openglow.ui.screens.PriorityType
import com.example.openglow.ui.theme.PointBlue
import com.example.openglow.ui.theme.UrgentRed
import java.time.LocalDateTime

@Composable
fun AddEventDialog(
    initialDate: LocalDateTime = LocalDateTime.now(),
    onDismiss: () -> Unit,
    onSave: (title: String, type: PriorityType, deadlineTime: LocalDateTime, location: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(PriorityType.URGENT) }
    var selectedDateTime by remember { mutableStateOf(initialDate) }

    val context = LocalContext.current
    val datePickerDialog = remember {
        DatePickerDialog(context, { _, year, month, dayOfMonth -> selectedDateTime = selectedDateTime.withYear(year).withMonth(month + 1).withDayOfMonth(dayOfMonth) }, selectedDateTime.year, selectedDateTime.monthValue - 1, selectedDateTime.dayOfMonth)
    }
    val timePickerDialog = remember {
        TimePickerDialog(context, { _, hourOfDay, minute -> selectedDateTime = selectedDateTime.withHour(hourOfDay).withMinute(minute).withSecond(0) }, selectedDateTime.hour, selectedDateTime.minute, false)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("새 일정 추가", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PriorityType.entries.forEach { type ->
                        val isSelected = selectedType == type
                        Surface(shape = RoundedCornerShape(8.dp), color = if (isSelected) type.color.copy(alpha = 0.1f) else Color.Transparent, border = BorderStroke(1.dp, if (isSelected) type.color else Color.LightGray), modifier = Modifier.weight(1f).clickable { selectedType = type }) {
                            Text(text = type.label, color = if (isSelected) type.color else Color.Gray, modifier = Modifier.padding(vertical = 10.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("일정 제목") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("장소") }, modifier = Modifier.fillMaxWidth())

                Text("마감 시각 설정", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { datePickerDialog.show() }, modifier = Modifier.weight(1.1f), shape = RoundedCornerShape(8.dp)) {
                        Text("${selectedDateTime.year}년 ${selectedDateTime.monthValue}월 ${selectedDateTime.dayOfMonth}일", fontSize = 11.sp)
                    }
                    OutlinedButton(onClick = { timePickerDialog.show() }, modifier = Modifier.weight(0.9f), shape = RoundedCornerShape(8.dp)) {
                        val amPm = if (selectedDateTime.hour < 12) "오전" else "오후"
                        val displayHour = if (selectedDateTime.hour % 12 == 0) 12 else selectedDateTime.hour % 12
                        Text("$amPm ${displayHour}시 ${"%02d".format(selectedDateTime.minute)}분", fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { if (title.isNotBlank()) onSave(title, selectedType, selectedDateTime, location) }, enabled = title.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = PointBlue)) { Text("저장") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소", color = Color.Gray) } },
        shape = RoundedCornerShape(16.dp), containerColor = Color.White
    )
}

@Composable
fun EditDeleteEventDialog(
    event: EventData,
    onDismiss: () -> Unit,
    onUpdate: (title: String, type: PriorityType, deadlineTime: LocalDateTime, location: String) -> Unit,
    onDelete: () -> Unit
) {
    var title by remember { mutableStateOf(event.title) }
    var location by remember { mutableStateOf(event.location) }
    var selectedType by remember { mutableStateOf(event.type) }
    var selectedDateTime by remember { mutableStateOf(event.deadlineTime) }

    val context = LocalContext.current
    val datePickerDialog = remember {
        DatePickerDialog(context, { _, year, month, dayOfMonth -> selectedDateTime = selectedDateTime.withYear(year).withMonth(month + 1).withDayOfMonth(dayOfMonth) }, selectedDateTime.year, selectedDateTime.monthValue - 1, selectedDateTime.dayOfMonth)
    }
    val timePickerDialog = remember {
        TimePickerDialog(context, { _, hourOfDay, minute -> selectedDateTime = selectedDateTime.withHour(hourOfDay).withMinute(minute).withSecond(0) }, selectedDateTime.hour, selectedDateTime.minute, false)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("일정 수정 및 삭제", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PriorityType.entries.forEach { type ->
                        val isSelected = selectedType == type
                        Surface(shape = RoundedCornerShape(8.dp), color = if (isSelected) type.color.copy(alpha = 0.1f) else Color.Transparent, border = BorderStroke(1.dp, if (isSelected) type.color else Color.LightGray), modifier = Modifier.weight(1f).clickable { selectedType = type }) {
                            Text(text = type.label, color = if (isSelected) type.color else Color.Gray, modifier = Modifier.padding(vertical = 10.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("일정 제목") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("장소") }, modifier = Modifier.fillMaxWidth())

                Text("마감 시각 변경", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { datePickerDialog.show() }, modifier = Modifier.weight(1.1f), shape = RoundedCornerShape(8.dp)) {
                        Text("${selectedDateTime.year}년 ${selectedDateTime.monthValue}월 ${selectedDateTime.dayOfMonth}일", fontSize = 11.sp)
                    }
                    OutlinedButton(onClick = { timePickerDialog.show() }, modifier = Modifier.weight(0.9f), shape = RoundedCornerShape(8.dp)) {
                        val amPm = if (selectedDateTime.hour < 12) "오전" else "오후"
                        val displayHour = if (selectedDateTime.hour % 12 == 0) 12 else selectedDateTime.hour % 12
                        Text("$amPm ${displayHour}시 ${"%02d".format(selectedDateTime.minute)}분", fontSize = 11.sp)
                    }
                }
            }
        },
        dismissButton = { TextButton(onClick = onDelete) { Text("삭제", color = UrgentRed, fontWeight = FontWeight.Bold) } },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismiss) { Text("취소", color = Color.Gray) }
                Button(onClick = { if (title.isNotBlank()) onUpdate(title, selectedType, selectedDateTime, location) }, colors = ButtonDefaults.buttonColors(containerColor = PointBlue), enabled = title.isNotBlank()) {
                    Text("수정 완료")
                }
            }
        },
        shape = RoundedCornerShape(16.dp), containerColor = Color.White
    )
}