package com.example.openglow.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.openglow.ui.components.AiSummaryCard
import com.example.openglow.ui.components.BottomNavigationBar
import com.example.openglow.ui.components.HeaderSection
import com.example.openglow.ui.components.PrioritySection
import com.example.openglow.ui.theme.BackgroundGray
import java.time.LocalDateTime
import java.util.UUID
import kotlinx.coroutines.delay

enum class PriorityType(val label: String, val color: Color) {
    URGENT("긴급", com.example.openglow.ui.theme.UrgentRed),
    NORMAL("일반", com.example.openglow.ui.theme.PointBlue),
}

data class EventData(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val type: PriorityType,
    val deadlineTime: LocalDateTime,
    val location: String,
)

@Composable
fun MainDashboardScreen(
    viewModel: MainDashboardViewModel = hiltViewModel(),
) {
    var selectedTabIndex by remember { mutableStateOf(2) }
    val aiSummaryState by viewModel.uiState.collectAsState()
    var eventList by remember { mutableStateOf(emptyList<EventData>()) }

    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("openglow_storage", Context.MODE_PRIVATE) }

    LaunchedEffect(Unit) {
        val savedEvents = sharedPrefs.getString("unified_events_data", "") ?: ""
        if (savedEvents.isNotBlank()) {
            runCatching {
                eventList = savedEvents.split("&&").map {
                    val parts = it.split("||")
                    EventData(
                        id = parts[0],
                        title = parts[1],
                        type = PriorityType.valueOf(parts[2]),
                        deadlineTime = LocalDateTime.parse(parts[3]),
                        location = parts[4],
                    )
                }
            }
        }
    }

    LaunchedEffect(eventList) {
        if (eventList.isNotEmpty()) {
            val encoded = eventList.joinToString("&&") {
                "${it.id}||${it.title}||${it.type.name}||${it.deadlineTime}||${it.location}"
            }
            sharedPrefs.edit().putString("unified_events_data", encoded).apply()
        } else {
            sharedPrefs.edit().remove("unified_events_data").apply()
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            val now = LocalDateTime.now()
            eventList = eventList.filter { it.deadlineTime.isAfter(now) }
        }
    }

    val top5PriorityTasks = remember(eventList) {
        eventList.sortedBy { it.deadlineTime }.take(5)
    }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                selectedIndex = selectedTabIndex,
                onTabSelected = { selectedTabIndex = it },
            )
        },
        containerColor = BackgroundGray,
    ) { padding ->
        when (selectedTabIndex) {
            0 -> {
                Box(modifier = Modifier.padding(padding)) {
                    NoteScreen()
                }
            }
            1 -> {
                Box(modifier = Modifier.padding(padding)) {
                    CalendarScreen(
                        eventList = eventList,
                        onEventListChange = { eventList = it },
                    )
                }
            }
            2 -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                ) {
                    HeaderSection()
                    Spacer(Modifier.height(20.dp))
                    val latestSummary = aiSummaryState.latestSummary
                    AiSummaryCard(
                        senderName = aiSummaryState.sender?.displayName,
                        summaryText = latestSummary?.summarizedText,
                        importance = latestSummary?.importance,
                        isWorkRelated = latestSummary?.isWorkRelated,
                        notificationCount = latestSummary?.notificationCount ?: 0,
                    )
                    Spacer(Modifier.height(24.dp))
                    PrioritySection(
                        priorityList = top5PriorityTasks,
                        onEventListChange = { eventList = it },
                        fullEventList = eventList,
                    )
                }
            }
            3 -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("AI 실험 화면은 준비 중입니다.", color = Color.Gray, fontSize = 16.sp)
                }
            }
            4 -> {
                Box(modifier = Modifier.padding(padding)) {
                    SettingsScreen()
                }
            }
        }
    }
}
