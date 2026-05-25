package com.example.openglow.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.openglow.data.local.AppDatabase
import com.example.openglow.data.local.dao.NotificationDao
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
}
