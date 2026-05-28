package com.example.openglow.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.openglow.ui.theme.BackgroundGray
import com.example.openglow.ui.theme.CardWhite
import com.example.openglow.ui.theme.PointBlue

@Composable
fun AiSummaryCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("AI가 정리한 오늘의 핵심", fontWeight = FontWeight.Bold)
                Text("자세히 보기 >", fontSize = 12.sp, color = Color.Gray)
            }
            Spacer(Modifier.height(12.dp))
            SummaryItem("1", "오후 4시 팀 프로젝트 회의")
            SummaryItem("2", "발표 자료 오늘 6시까지 수정 필요")
            SummaryItem("3", "회의 장소가 IT대학으로 변경됨")

            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {}, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = BackgroundGray), border = BorderStroke(1.dp, Color.LightGray)) {
                    Text("전체 계획 보기", color = Color.Black)
                }
                Button(onClick = {}, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = PointBlue)) {
                    Icon(Icons.Default.CalendarToday, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("캘린더에 추가")
                }
            }
        }
    }
}

@Composable
private fun SummaryItem(num: String, text: String) {
    Row(Modifier.padding(vertical = 4.dp)) {
        Box(Modifier.size(20.dp).background(PointBlue.copy(alpha = 0.1f), CircleShape), Alignment.Center) {
            Text(num, color = PointBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 14.sp)
    }
}