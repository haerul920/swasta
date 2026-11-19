package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserEntity::class,
        AirQualityReadingEntity::class,
        SmokeFreeZoneEntity::class,
        SmokeReportEntity::class,
        QuitProgressEntity::class,
        DailyMissionEntity::class,
        HealthJournalEntity::class,
        NotificationEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SwasanaDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun airQualityDao(): AirQualityDao
    abstract fun smokeZoneDao(): SmokeZoneDao
    abstract fun smokeReportDao(): SmokeReportDao
    abstract fun quitProgressDao(): QuitProgressDao
    abstract fun dailyMissionDao(): DailyMissionDao
    abstract fun healthJournalDao(): HealthJournalDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile
        private var INSTANCE: SwasanaDatabase? = null

        fun getInstance(context: Context): SwasanaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SwasanaDatabase::class.java,
                    "swasana_local_db"
                ).fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
