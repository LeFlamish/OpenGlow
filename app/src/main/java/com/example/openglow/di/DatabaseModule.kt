package com.example.openglow.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.openglow.data.local.AppDatabase
import com.example.openglow.data.local.dao.AnalysisLogDao
import com.example.openglow.data.local.dao.CalendarSuggestionDao
import com.example.openglow.data.local.dao.FeedbackDao
import com.example.openglow.data.local.dao.NotificationDao
import com.example.openglow.data.local.dao.NoteDao
import com.example.openglow.data.local.dao.PersonalizationRuleDao
import com.example.openglow.data.local.dao.SenderDao
import com.example.openglow.data.local.dao.SummaryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "openglow_database"
        )
            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideSenderDao(database: AppDatabase): SenderDao {
        return database.senderDao()
    }

    @Provides
    fun provideNotificationDao(database: AppDatabase): NotificationDao {
        return database.notificationDao()
    }

    @Provides
    fun provideSummaryDao(database: AppDatabase): SummaryDao {
        return database.summaryDao()
    }

    @Provides
    fun provideNoteDao(database: AppDatabase): NoteDao {
        return database.noteDao()
    }

    @Provides
    fun provideAnalysisLogDao(database: AppDatabase): AnalysisLogDao {
        return database.analysisLogDao()
    }

    @Provides
    fun provideFeedbackDao(database: AppDatabase): FeedbackDao {
        return database.feedbackDao()
    }

    @Provides
    fun providePersonalizationRuleDao(database: AppDatabase): PersonalizationRuleDao {
        return database.personalizationRuleDao()
    }

    @Provides
    fun provideCalendarSuggestionDao(database: AppDatabase): CalendarSuggestionDao {
        return database.calendarSuggestionDao()
    }
}
