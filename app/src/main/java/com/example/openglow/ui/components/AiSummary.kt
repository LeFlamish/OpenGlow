package com.example.openglow.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.openglow.ui.theme.CardWhite
import com.example.openglow.ui.theme.PointBlue
import com.example.openglow.ui.theme.SoftGray
import com.example.openglow.ui.theme.UrgentRed

@Composable
fun AiSummaryCard(
    senderName: String?,
    summaryText: String?,
    importance: String?,
    isWorkRelated: Boolean?,
    notificationCount: Int,
) {
    val hasSummary = !summaryText.isNullOrBlank()
    val importanceUi = importanceUi(importance)

    Card(
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("AI가 정리한 최신 알림", fontWeight = FontWeight.Bold)
                senderName?.let {
                    Text(it, fontSize = 12.sp, color = SoftGray)
                }
            }
            Spacer(Modifier.height(12.dp))

            if (hasSummary) {
                Text(
                    text = summaryText.orEmpty(),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = Color.Black,
                )

                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SummaryChip(
                        text = "중요도 ${importanceUi.label}",
                        color = importanceUi.color,
                    )
                    SummaryChip(
                        text = if (isWorkRelated == true) "업무 관련" else "업무 외",
                        color = if (isWorkRelated == true) PointBlue else SoftGray,
                    )
                }

                if (notificationCount > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "${notificationCount}개 알림 분석",
                        fontSize = 12.sp,
                        color = SoftGray,
                    )
                }
            } else {
                Text(
                    text = "아직 분석된 알림이 없습니다.",
                    fontSize = 14.sp,
                    color = SoftGray,
                )
            }
        }
    }
}

@Composable
private fun SummaryChip(text: String, color: Color) {
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

private data class ImportanceUi(
    val label: String,
    val color: Color,
)

private fun importanceUi(importance: String?): ImportanceUi = when (importance?.uppercase()) {
    "LOW" -> ImportanceUi("낮음", SoftGray)
    "HIGH" -> ImportanceUi("높음", Color(0xFFF59E0B))
    "URGENT" -> ImportanceUi("긴급", UrgentRed)
    else -> ImportanceUi("보통", PointBlue)
}
