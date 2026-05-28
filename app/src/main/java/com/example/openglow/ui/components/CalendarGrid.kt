package com.example.openglow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.openglow.ui.theme.PointBlue
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CalendarGrid(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val firstDayOfMonth = currentMonth.atDay(1)
    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7
    val emptyCells = firstDayOfWeek

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(emptyCells) {
            Box(modifier = Modifier.aspectRatio(1f))
        }

        items(daysInMonth) { day ->
            val date = currentMonth.atDay(day + 1)
            val isSelected = date == selectedDate
            val isToday = date == LocalDate.now()

            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) PointBlue else Color.Transparent)
                    .clickable { onDateSelected(date) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (day + 1).toString(),
                    color = when {
                        isSelected -> Color.White
                        date.dayOfWeek.value == 7 -> Color.Red
                        else -> Color.Black
                    },
                    fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
                )
                if (isToday && !isSelected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 4.dp)
                            .size(4.dp)
                            .background(PointBlue, CircleShape)
                    )
                }
            }
        }
    }
}