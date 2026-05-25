package com.example.openglow.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.openglow.data.entity.*
import com.example.openglow.data.local.dao.*

@Database(
    entities = [
        SenderEntity::class,
        SenderIdentifierEntity::class,
        NotificationEntity::class,
        SummaryEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun senderDao(): SenderDao
    abstract fun notificationDao(): NotificationDao
    abstract fun summaryDao(): SummaryDao
}
