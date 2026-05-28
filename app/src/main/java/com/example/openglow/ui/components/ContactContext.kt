package com.example.openglow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.openglow.ui.theme.CardWhite

@Composable
fun ContactContextSection() {
    Card(colors = CardDefaults.cardColors(containerColor = CardWhite)) {
        Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).background(Color.LightGray, CircleShape))
            Spacer(Modifier.width(12.dp))
            Column {
                Text("정지욱 팀원", fontWeight = FontWeight.Bold)
                Text("최근 협업: 발표 자료 수정", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}