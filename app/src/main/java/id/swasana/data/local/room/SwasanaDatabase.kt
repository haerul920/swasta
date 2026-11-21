package id.swasana.data.local.room

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface AirQualityDao {
  @Insert suspend fun insert(entity: AirQualityReadingEntity): Long

  @Query("SELECT * FROM air_quality_reading ORDER BY recordedAt DESC LIMIT 1")
  fun observeLatest(): Flow<AirQualityReadingEntity?>

  @Query("SELECT * FROM air_quality_reading ORDER BY recordedAt DESC LIMIT 1")
  suspend fun latest(): AirQualityReadingEntity?

  @Query("SELECT * FROM air_quality_reading WHERE recordedAt >= :since ORDER BY recordedAt ASC")
  fun observeSince(since: Long): Flow<List<AirQualityReadingEntity>>

  @Query("DELETE FROM air_quality_reading WHERE recordedAt < :before")
  suspend fun pruneBefore(before: Long)

  @Query("DELETE FROM air_quality_reading") suspend fun deleteAll()
}

@Dao
interface JournalDao {
  @Query("SELECT * FROM journal_entry WHERE dateEpochDay >= :sinceDay ORDER BY dateEpochDay ASC")
  fun observeSince(sinceDay: Long): Flow<List<JournalEntryEntity>>

  @Query("SELECT * FROM journal_entry ORDER BY dateEpochDay ASC")
  fun observeAll(): Flow<List<JournalEntryEntity>>

  @Query("SELECT * FROM journal_entry WHERE dateEpochDay = :day LIMIT 1")
  suspend fun byDay(day: Long): JournalEntryEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(entity: JournalEntryEntity)

  @Query("DELETE FROM journal_entry") suspend fun deleteAll()
}

@Dao
interface CravingDao {
  @Query("SELECT * FROM craving_event ORDER BY timestamp DESC")
  fun observeAll(): Flow<List<CravingEventEntity>>

  @Query("SELECT COUNT(*) FROM craving_event WHERE overcome = 1")
  fun observeOvercomeCount(): Flow<Int>

  @Insert suspend fun insert(entity: CravingEventEntity)

  @Query("DELETE FROM craving_event") suspend fun deleteAll()
}

@Dao
interface SmokeDao {
  @Query("SELECT * FROM smoke_free_zone WHERE verified = 1 ORDER BY name ASC")
  fun observeVerifiedZones(): Flow<List<SmokeFreeZoneEntity>>

  @Query("SELECT * FROM smoke_free_zone WHERE verified = 0 ORDER BY name ASC")
  fun observeProposedZones(): Flow<List<SmokeFreeZoneEntity>>

  @Query("SELECT COUNT(*) FROM smoke_free_zone") suspend fun zoneCount(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertZone(zone: SmokeFreeZoneEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertZones(zones: List<SmokeFreeZoneEntity>)

  @Query("DELETE FROM smoke_free_zone WHERE id = :id") suspend fun deleteZone(id: String)

  @Query("UPDATE smoke_free_zone SET verified = 1 WHERE id = :id")
  suspend fun verifyZone(id: String)

  @Query("SELECT * FROM smoke_report WHERE status = :status ORDER BY reportedAt DESC")
  fun observeReports(status: String): Flow<List<SmokeReportEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertReport(report: SmokeReportEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertReports(reports: List<SmokeReportEntity>)

  @Query("UPDATE smoke_report SET status = :status WHERE id = :id")
  suspend fun setReportStatus(id: String, status: String)

  @Query("DELETE FROM smoke_report WHERE reporterId = :reporterId")
  suspend fun deleteReportsBy(reporterId: String)

  @Query("SELECT COUNT(*) FROM smoke_report") suspend fun reportCount(): Int
}

@Dao
interface NotificationDao {
  @Query("SELECT * FROM app_notification ORDER BY createdAt DESC LIMIT 50")
  fun observeAll(): Flow<List<NotificationEntity>>

  @Query("SELECT COUNT(*) FROM app_notification WHERE isRead = 0")
  fun observeUnreadCount(): Flow<Int>

  @Insert suspend fun insert(entity: NotificationEntity)

  @Query("UPDATE app_notification SET isRead = 1 WHERE isRead = 0") suspend fun markAllRead()

  @Query("DELETE FROM app_notification") suspend fun deleteAll()
}

@Database(
  entities =
    [
      AirQualityReadingEntity::class,
      JournalEntryEntity::class,
      CravingEventEntity::class,
      SmokeFreeZoneEntity::class,
      SmokeReportEntity::class,
      NotificationEntity::class,
    ],
  version = 1,
  exportSchema = true,
)
abstract class SwasanaDatabase : RoomDatabase() {
  abstract fun airQualityDao(): AirQualityDao

  abstract fun journalDao(): JournalDao

  abstract fun cravingDao(): CravingDao

  abstract fun smokeDao(): SmokeDao

  abstract fun notificationDao(): NotificationDao
}
