package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getByEmail(email: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(users: List<UserEntity>)

    @Query("DELETE FROM users")
    suspend fun clearAll()
}

@Dao
interface AirQualityDao {
    @Query("SELECT * FROM air_quality_readings ORDER BY createdEpochMillis DESC")
    fun getAllReadings(): Flow<List<AirQualityReadingEntity>>

    @Query("SELECT * FROM air_quality_readings WHERE LOWER(location) = LOWER(:location) ORDER BY createdEpochMillis DESC LIMIT 1")
    suspend fun findLatestByLocation(location: String): AirQualityReadingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reading: AirQualityReadingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(readings: List<AirQualityReadingEntity>)

    @Query("DELETE FROM air_quality_readings")
    suspend fun clearAll()
}

@Dao
interface SmokeZoneDao {
    @Query("SELECT * FROM smoke_free_zones ORDER BY rating DESC")
    fun getAllZones(): Flow<List<SmokeFreeZoneEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(zone: SmokeFreeZoneEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(zones: List<SmokeFreeZoneEntity>)

    @Query("DELETE FROM smoke_free_zones WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM smoke_free_zones")
    suspend fun clearAll()
}

@Dao
interface SmokeReportDao {
    @Query("SELECT * FROM smoke_reports ORDER BY createdEpochMillis DESC")
    fun getAllReports(): Flow<List<SmokeReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(report: SmokeReportEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(reports: List<SmokeReportEntity>)

    @Query("UPDATE smoke_reports SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("DELETE FROM smoke_reports")
    suspend fun clearAll()
}

@Dao
interface QuitProgressDao {
    @Query("SELECT * FROM quit_progress WHERE userId = :userId LIMIT 1")
    fun observeByUser(userId: String): Flow<QuitProgressEntity?>

    @Query("SELECT * FROM quit_progress WHERE userId = :userId LIMIT 1")
    suspend fun getByUser(userId: String): QuitProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: QuitProgressEntity)

    @Query("DELETE FROM quit_progress")
    suspend fun clearAll()
}

@Dao
interface DailyMissionDao {
    @Query("SELECT * FROM daily_missions")
    fun getAllMissions(): Flow<List<DailyMissionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(missions: List<DailyMissionEntity>)

    @Query("UPDATE daily_missions SET completed = :completed WHERE id = :id")
    suspend fun setCompleted(id: String, completed: Boolean)

    @Query("DELETE FROM daily_missions")
    suspend fun clearAll()
}

@Dao
interface HealthJournalDao {
    @Query("SELECT * FROM health_journals WHERE userId = :userId ORDER BY createdEpochMillis DESC")
    fun getByUser(userId: String): Flow<List<HealthJournalEntity>>

    @Query("SELECT * FROM health_journals ORDER BY createdEpochMillis DESC")
    fun getAll(): Flow<List<HealthJournalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(journal: HealthJournalEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(journals: List<HealthJournalEntity>)

    @Query("DELETE FROM health_journals")
    suspend fun clearAll()
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY createdEpochMillis DESC")
    fun getAll(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notifications: List<NotificationEntity>)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markRead(id: String)

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllRead()

    @Query("DELETE FROM notifications")
    suspend fun clearAll()
}
