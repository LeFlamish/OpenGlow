package com.example.openglow.ui.screens // ⚠️ 패키지명 확인

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.openglow.ui.components.*
import com.example.openglow.ui.theme.BackgroundGray
import com.example.openglow.ui.theme.PointBlue
import java.time.LocalDateTime

@Composable
fun NoteScreen() {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(NoteType.PERSON) }
    var contactList by remember { mutableStateOf(emptyList<ContactEntity>()) }

    var viewingContactId by remember { mutableStateOf<String?>(null) }

    var showAddContactDialog by remember { mutableStateOf(false) }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var showEditContactDialog by remember { mutableStateOf(false) }

    // 🌟 상세 대화 내역 수정 팝업 제어용 상태 추가
    var selectedNoteForEdit by remember { mutableStateOf<ConversationNote?>(null) }

    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("openglow_storage", Context.MODE_PRIVATE) }

    LaunchedEffect(Unit) {
        val savedData = sharedPrefs.getString("contacts_directory_data", "") ?: ""
        if (savedData.isNotBlank()) {
            try {
                contactList = savedData.split("&&").map { contactStr ->
                    val cParts = contactStr.split("||")
                    val notesStr = if (cParts.size > 5) cParts[5] else ""
                    val notes = if (notesStr.isNotBlank()) {
                        notesStr.split("@@").map { nStr ->
                            val nParts = nStr.split("~~")
                            ConversationNote(nParts[0], nParts[1], LocalDateTime.parse(nParts[2]))
                        }
                    } else emptyList()
                    ContactEntity(cParts[0], cParts[1], NoteType.valueOf(cParts[2]), cParts[3].toBoolean(), notes, LocalDateTime.parse(cParts[4]))
                }
            } catch (e: Exception) {
                sharedPrefs.edit().remove("contacts_directory_data").apply()
            }
        }
    }

    LaunchedEffect(contactList) {
        if (contactList.isNotEmpty()) {
            val encoded = contactList.joinToString("&&") { c ->
                val notesStr = c.notes.joinToString("@@") { n -> "${n.id}~~${n.summary}~~${n.createdAt}" }
                "${c.id}||${c.name}||${c.type.name}||${c.isFavorite}||${c.updatedAt}||$notesStr"
            }
            sharedPrefs.edit().putString("contacts_directory_data", encoded).apply()
        } else {
            sharedPrefs.edit().remove("contacts_directory_data").apply()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(BackgroundGray)) {

        if (viewingContactId == null) {
            val displayContacts = remember(contactList, selectedTab, searchQuery) {
                contactList
                    .filter { it.name.contains(searchQuery, ignoreCase = true) }
                    .filter { when (selectedTab) { NoteType.FAVORITE -> it.isFavorite; else -> it.type == selectedTab } }
                    .sortedWith(compareByDescending<ContactEntity> { it.isFavorite }.thenByDescending { it.updatedAt })
            }

            Column(modifier = Modifier.fillMaxSize()) {
                OutlinedTextField(
                    value = searchQuery, onValueChange = { searchQuery = it }, modifier = Modifier.fillMaxWidth().padding(16.dp), placeholder = { Text("인물/그룹 이름 검색") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "검색") }, shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White, focusedBorderColor = PointBlue, unfocusedBorderColor = Color.Transparent)
                )

                TabRow(
                    selectedTabIndex = selectedTab.ordinal, containerColor = BackgroundGray, contentColor = PointBlue,
                    indicator = { tabPositions -> TabRowDefaults.Indicator(modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]), color = PointBlue, height = 3.dp) }
                ) {
                    NoteType.entries.forEach { type ->
                        Tab(selected = selectedTab == type, onClick = { selectedTab = type }, text = { Text(type.label, fontWeight = FontWeight.Bold, fontSize = 15.sp) }, selectedContentColor = PointBlue, unselectedContentColor = Color.Gray)
                    }
                }

                if (displayContacts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(if (selectedTab == NoteType.FAVORITE) "즐겨찾기된 항목이 없습니다." else "등록된 연락처가 없습니다.", color = Color.Gray) }
                } else {
                    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
                        items(displayContacts, key = { it.id }) { contact ->
                            ContactCard(
                                contact = contact,
                                onClick = { viewingContactId = contact.id },
                                onFavoriteToggle = {
                                    contactList = contactList.map { if (it.id == contact.id) it.copy(isFavorite = !it.isFavorite, updatedAt = LocalDateTime.now()) else it }
                                }
                            )
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick = { showAddContactDialog = true }, containerColor = PointBlue, contentColor = Color.White, shape = CircleShape, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 16.dp)
            ) { Icon(Icons.Default.Add, contentDescription = "연락처 추가") }

            if (showAddContactDialog) {
                AddContactDialog(
                    currentTabType = selectedTab, onDismiss = { showAddContactDialog = false },
                    onSave = { name, type ->
                        contactList = contactList + ContactEntity(name = name, type = type)
                        showAddContactDialog = false
                    }
                )
            }
        } else {
            val contact = contactList.find { it.id == viewingContactId }
            if (contact == null) {
                viewingContactId = null
            } else {
                val sortedNotes = contact.notes.sortedByDescending { it.createdAt }

                Column(modifier = Modifier.fillMaxSize()) {
                    Surface(color = Color.White, shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewingContactId = null }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.ArrowBack, contentDescription = "뒤로 가기") }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = contact.name, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.weight(1f))

                            IconButton(onClick = { showEditContactDialog = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "수정", tint = Color.Gray)
                            }
                            IconButton(onClick = { contactList = contactList.map { if (it.id == contact.id) it.copy(isFavorite = !it.isFavorite) else it } }) {
                                Icon(imageVector = if (contact.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline, contentDescription = "즐겨찾기", tint = if (contact.isFavorite) Color(0xFFFFC107) else Color.LightGray)
                            }
                        }
                    }

                    if (sortedNotes.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("등록된 대화 내역이 없습니다.", color = Color.Gray) }
                    } else {
                        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
                            items(sortedNotes, key = { it.id }) { note ->
                                // 🌟 대화 내역 카드에 클릭 이벤트 연동
                                ConversationNoteCard(
                                    note = note,
                                    onClick = { selectedNoteForEdit = note }
                                )
                            }
                        }
                    }
                }

                FloatingActionButton(
                    onClick = { showAddNoteDialog = true }, containerColor = PointBlue, contentColor = Color.White, shape = CircleShape, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 16.dp)
                ) { Icon(Icons.Default.Add, contentDescription = "대화 내역 추가") }

                if (showAddNoteDialog) {
                    AddConversationDialog(
                        onDismiss = { showAddNoteDialog = false },
                        onSave = { summary ->
                            val newNote = ConversationNote(summary = summary)
                            contactList = contactList.map {
                                if (it.id == contact.id) it.copy(notes = it.notes + newNote, updatedAt = LocalDateTime.now()) else it
                            }
                            showAddNoteDialog = false
                        }
                    )
                }

                if (showEditContactDialog) {
                    EditDeleteContactDialog(
                        contact = contact,
                        onDismiss = { showEditContactDialog = false },
                        onUpdate = { newName, newType ->
                            contactList = contactList.map {
                                if (it.id == contact.id) it.copy(name = newName, type = newType, updatedAt = LocalDateTime.now()) else it
                            }
                            showEditContactDialog = false
                        },
                        onDelete = {
                            contactList = contactList.filter { it.id != contact.id }
                            showEditContactDialog = false
                            viewingContactId = null
                        }
                    )
                }

                // 🌟 신규 연동: 대화 내역 자체 수정/삭제 팝업창 띄우기
                selectedNoteForEdit?.let { targetNote ->
                    EditDeleteConversationDialog(
                        note = targetNote,
                        onDismiss = { selectedNoteForEdit = null },
                        onUpdate = { newSummary ->
                            contactList = contactList.map { c ->
                                if (c.id == contact.id) {
                                    val updatedNotes = c.notes.map { n ->
                                        if (n.id == targetNote.id) n.copy(summary = newSummary) else n
                                    }
                                    c.copy(notes = updatedNotes, updatedAt = LocalDateTime.now()) // 폴더 업데이트 시각도 갱신!
                                } else c
                            }
                            selectedNoteForEdit = null
                        },
                        onDelete = {
                            contactList = contactList.map { c ->
                                if (c.id == contact.id) {
                                    val updatedNotes = c.notes.filter { n -> n.id != targetNote.id }
                                    c.copy(notes = updatedNotes, updatedAt = LocalDateTime.now())
                                } else c
                            }
                            selectedNoteForEdit = null
                        }
                    )
                }
            }
        }
    }
}