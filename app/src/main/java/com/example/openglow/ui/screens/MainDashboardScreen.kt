package com.example.openglow.ui.screens // ⚠️ 패키지명 확인

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.util.UUID

import com.example.openglow.ui.components.*
import com.example.openglow.ui.theme.BackgroundGray

// 💡 캘린더와 대시보드가 함께 사용할 통합 데이터 규격
enum class PriorityType(val label: String, val color: Color) {
    URGENT("긴급", com.example.openglow.ui.theme.UrgentRed),
    NORMAL("일반", com.example.openglow.ui.theme.PointBlue)
}

data class EventData(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val type: PriorityType,
    val deadlineTime: LocalDateTime,
    val location: String
)

@Composable
fun MainDashboardScreen() {
    var selectedTabIndex by remember { mutableStateOf(2) }

    // 💡 모든 데이터는 오직 이 하나의 리스트로만 관리합니다.
    var eventList by remember { mutableStateOf(emptyList<EventData>()) }

    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("openglow_storage", Context.MODE_PRIVATE) }

    // 앱 실행 시 통합 데이터 로드
    LaunchedEffect(Unit) {
        val savedEvents = sharedPrefs.getString("unified_events_data", "") ?: ""
        if (savedEvents.isNotBlank()) {
            try {
                eventList = savedEvents.split("&&").map {
                    val parts = it.split("||")
                    EventData(parts[0], parts[1], PriorityType.valueOf(parts[2]), LocalDateTime.parse(parts[3]), parts[4])
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    // 데이터 변경 시 자동 저장
    LaunchedEffect(eventList) {
        if (eventList.isNotEmpty()) {
            val encoded = eventList.joinToString("&&") { "${it.id}||${it.title}||${it.type.name}||${it.deadlineTime}||${it.location}" }
            sharedPrefs.edit().putString("unified_events_data", encoded).apply()
        } else {
            sharedPrefs.edit().remove("unified_events_data").apply()
        }
    }

    // 🌟 1분마다 마감 시간이 지난 일정을 캘린더 전체에서 자동 삭제
    LaunchedEffect(Unit) {
        while (true) {
            delay(60000)
            val now = LocalDateTime.now()
            eventList = eventList.filter { it.deadlineTime.isAfter(now) }
        }
    }

    // 🌟 대시보드에 보여줄 마감 임박 상위 5개 일정 계산
    val top5PriorityTasks = remember(eventList) {
        eventList.sortedBy { it.deadlineTime }.take(5)
    }

    Scaffold(
        bottomBar = { BottomNavigationBar(selectedIndex = selectedTabIndex, onTabSelected = { selectedTabIndex = it }) },
        containerColor = BackgroundGray
    ) { padding ->
        when (selectedTabIndex) {
            0 -> {
                Box(modifier = Modifier.padding(padding)) {
                    NoteScreen()
                }
            }
            1 -> {
                Box(modifier = Modifier.padding(padding)) {
                    CalendarScreen(eventList = eventList, onEventListChange = { eventList = it })
                }
            }
            2 -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)
                ) {
                    HeaderSection()
                    Spacer(Modifier.height(20.dp))
                    AiSummaryCard()
                    Spacer(Modifier.height(24.dp))

                    // 💡 대시보드 우선순위 액션에 상위 5개 리스트와 데이터 제어권 전달
                    PrioritySection(
                        priorityList = top5PriorityTasks,
                        onEventListChange = { eventList = it },
                        fullEventList = eventList
                    )
                }
            }
            else -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("준비 중인 화면입니다.", color = Color.Gray, fontSize = 16.sp)
                }
            }
        }
    }
}