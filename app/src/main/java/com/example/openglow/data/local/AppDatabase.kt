package com.example.openglow.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.openglow.data.entity.AnalysisLogEntity
import com.example.openglow.data.entity.CalendarSuggestionEntity
import com.example.openglow.data.entity.FeedbackEntity
import com.example.openglow.data.entity.NoteEntity
import com.example.openglow.data.entity.NotificationEntity
import com.example.openglow.data.entity.PersonalizationRuleEntity
import com.example.openglow.data.entity.SenderEntity
import com.example.openglow.data.entity.SenderIdentifierEntity
import com.example.openglow.data.entity.SummaryEntity
import com.example.openglow.data.local.dao.AnalysisLogDao
import com.example.openglow.data.local.dao.CalendarSuggestionDao
import com.example.openglow.data.local.dao.FeedbackDao
import com.example.openglow.data.local.dao.NoteDao
import com.example.openglow.data.local.dao.NotificationDao
import com.example.openglow.data.local.dao.PersonalizationRuleDao
import com.example.openglow.data.local.dao.SenderDao
import com.example.openglow.data.local.dao.SummaryDao

@Database(
    entities = [
        SenderEntity::class,
        SenderIdentifierEntity::class,
        NotificationEntity::class,
        SummaryEntity::class,
        NoteEntity::class,
        AnalysisLogEntity::class,
        FeedbackEntity::class,
        PersonalizationRuleEntity::class,
        CalendarSuggestionEntity::class,
    ],
    version = 6,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun senderDao(): SenderDao
    abstract fun notificationDao(): NotificationDao
    abstract fun summaryDao(): SummaryDao
    abstract fun noteDao(): NoteDao
    abstract fun analysisLogDao(): AnalysisLogDao
    abstract fun feedbackDao(): FeedbackDao
    abstract fun personalizationRuleDao(): PersonalizationRuleDao
    abstract fun calendarSuggestionDao(): CalendarSuggestionDao
}
