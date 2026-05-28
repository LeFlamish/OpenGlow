package com.example.openglow.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.openglow.ui.theme.CardWhite
import com.example.openglow.ui.theme.PointBlue

@Composable
fun RecommendationSection() {
    Column {
        Text("추천 액션", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RecommendItem(Icons.Default.Translate, "번역", Modifier.weight(1f))
            RecommendItem(Icons.Default.Map, "지도", Modifier.weight(1f))
            RecommendItem(Icons.Default.Group, "팀원 공유", Modifier.weight(1f))
        }
    }
}

@Composable
private fun RecommendItem(icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(100.dp),
        color = CardWhite,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.LightGray)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = PointBlue)
            Spacer(Modifier.height(8.dp))
            Text(label, fontSize = 12.sp)
        }
    }
}