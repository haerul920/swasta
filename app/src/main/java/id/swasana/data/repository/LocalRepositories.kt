package id.swasana.data.repository

import id.swasana.data.local.room.CravingDao
import id.swasana.data.local.room.CravingEventEntity
import id.swasana.data.local.room.JournalDao
import id.swasana.data.local.room.NotificationDao
import id.swasana.data.local.room.NotificationEntity
import id.swasana.data.local.room.toDomain
import id.swasana.data.local.room.toEntity
import id.swasana.domain.model.AppNotification
import id.swasana.domain.model.CravingEvent
import id.swasana.domain.model.HealthJournalEntry
import id.swasana.domain.repository.CravingRepository
import id.swasana.domain.repository.JournalRepository
import id.swasana.domain.repository.NotificationRepository
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class JournalRepositoryImpl @Inject constructor(private val dao: JournalDao) : JournalRepository {
  override fun observeEntries(since: LocalDate): Flow<List<HealthJournalEntry>> =
    dao.observeSince(since.toEpochDay()).map { l -> l.map { it.toDomain() } }

  override fun observeAll(): Flow<List<HealthJournalEntry>> =
    dao.observeAll().map { l -> l.map { it.toDomain() } }

  override suspend fun entryFor(date: LocalDate): HealthJournalEntry? =
    dao.byDay(date.toEpochDay())?.toDomain()

  override suspend fun save(entry: HealthJournalEntry) {
    // Satu entri per tanggal: gunakan id yang sudah ada agar REPLACE menimpa entri hari yang sama.
    val existing = dao.byDay(entry.date.toEpochDay())
    dao.upsert(entry.copy(id = existing?.id ?: entry.id).toEntity())
  }

  override suspend fun deleteAll() = dao.deleteAll()
}

@Singleton
class CravingRepositoryImpl @Inject constructor(private val dao: CravingDao) : CravingRepository {
  override fun observeAll(): Flow<List<CravingEvent>> =
    dao.observeAll().map { l -> l.map(CravingEventEntity::toDomain) }

  override fun observeOvercomeCount(): Flow<Int> = dao.observeOvercomeCount()

  override suspend fun add(event: CravingEvent) = dao.insert(event.toEntity())

  override suspend fun deleteAll() = dao.deleteAll()
}

@Singleton
class NotificationRepositoryImpl @Inject constructor(private val dao: NotificationDao) :
  NotificationRepository {
  override fun observeAll(): Flow<List<AppNotification>> =
    dao.observeAll().map { l -> l.map(NotificationEntity::toDomain) }

  override fun observeUnreadCount(): Flow<Int> = dao.observeUnreadCount()

  override suspend fun add(message: String, createdAt: Instant) =
    dao.insert(NotificationEntity(message = message, createdAt = createdAt.toEpochMilli(), isRead = false))

  override suspend fun markAllRead() = dao.markAllRead()

  override suspend fun deleteAll() = dao.deleteAll()
}
