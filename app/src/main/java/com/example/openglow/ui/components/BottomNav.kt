package com.example.openglow.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.openglow.ui.theme.PointBlue

data class BottomNavItem(val label: String, val icon: ImageVector)

@Composable
fun BottomNavigationBar(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
) {
    val navItems = listOf(
        BottomNavItem("노트", Icons.Default.Description),
        BottomNavItem("캘린더", Icons.Default.CalendarToday),
        BottomNavItem("홈", Icons.Default.Home),
        BottomNavItem("AI", Icons.Default.AutoAwesome),
        BottomNavItem("설정", Icons.Default.Settings),
    )

    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp,
    ) {
        navItems.forEachIndexed { index, item ->
            val isSelected = selectedIndex == index
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(index) },
                icon = {
                    val iconSize = if (item.label == "홈" || item.label == "AI") 28.dp else 24.dp
                    androidx.compose.material3.Icon(
                        item.icon,
                        contentDescription = item.label,
                        modifier = Modifier.size(iconSize),
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 11.sp,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PointBlue,
                    selectedTextColor = PointBlue,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = PointBlue.copy(alpha = 0.1f),
                ),
            )
        }
    }
}
