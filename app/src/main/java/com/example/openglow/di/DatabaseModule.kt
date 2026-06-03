package com.example.openglow.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
            .addMigrations(MIGRATION_5_6)
            // Development only: production builds must use an explicit Room migration.
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

    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE notes ADD COLUMN ragUploadedAt INTEGER")
            db.execSQL("ALTER TABLE notes ADD COLUMN ragRemoteDocumentId TEXT")
            db.execSQL("ALTER TABLE notes ADD COLUMN ragLastUploadedNoteUpdatedAt INTEGER")
        }
    }
}
