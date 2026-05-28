package com.example.openglow.ui.components // ⚠️ 패키지명 확인

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.openglow.ui.theme.CardWhite
import com.example.openglow.ui.theme.PointBlue

@Composable
fun EventCard(
    time: String,
    title: String,
    location: String,
    color: Color = PointBlue, // 🌟 긴급/일반 색상을 동적으로 받기 위해 추가 (기본값 파란색)
    onEventClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp)
            .clickable { onEventClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .background(color, RoundedCornerShape(2.dp)) // 🌟 넘겨받은 색상(빨간색 또는 파란색)으로 선을 그려줍니다.
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "$time • $location", color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}