package com.example.data.repositories

import android.content.Context
import com.example.data.local.AirQualityDao
import com.example.data.local.HealthJournalDao
import com.example.data.local.SwasanaDatabase
import com.example.data.local.SwasanaSeedData
import com.example.data.local.toDomain
import com.example.data.local.toEntity
import com.example.domain.entities.AirQualityReading
import com.example.domain.entities.AppNotification
import com.example.domain.entities.DailyMission
import com.example.domain.entities.HealthJournal
import com.example.domain.entities.QuitProgress
import com.example.domain.entities.SmokeFreeZone
import com.example.domain.entities.SmokeReport
import com.example.domain.entities.User
import com.example.domain.enums.ReportStatus
import com.example.domain.interfaces.IAirQualityRepository
import com.example.domain.interfaces.IJournalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class RoomAirQualityRepository(
    private val dao: AirQualityDao
) : IAirQualityRepository {
    override suspend fun save(reading: AirQualityReading) {
        dao.insert(reading.toEntity())
    }

    override suspend fun findLatest(location: String): AirQualityReading? {
        return dao.findLatestByLocation(location)?.toDomain()
    }

    override fun getHistoryFlow(): Flow<List<AirQualityReading>> {
        return dao.getAllReadings().map { list -> list.map { it.toDomain() } }
    }
}

class RoomJournalRepository(
    private val dao: HealthJournalDao
) : IJournalRepository {
    override suspend fun save(journal: HealthJournal) {
        dao.insert(journal.toEntity())
    }

    override fun findByUserFlow(userId: String): Flow<List<HealthJournal>> {
        return dao.getByUser(userId).map { list -> list.map { it.toDomain() } }
    }
}

class SwasanaLocalStore(
    context: Context,
    val db: SwasanaDatabase
) {
    private val prefs = context.getSharedPreferences("swasana_session_prefs", Context.MODE_PRIVATE)

    val usersFlow: Flow<List<User>> = db.userDao().getAllUsers().map { l -> l.map { it.toDomain() } }
    val zonesFlow: Flow<List<SmokeFreeZone>> = db.smokeZoneDao().getAllZones().map { l -> l.map { it.toDomain() } }
    val reportsFlow: Flow<List<SmokeReport>> = db.smokeReportDao().getAllReports().map { l -> l.map { it.toDomain() } }
    val missionsFlow: Flow<List<DailyMission>> = db.dailyMissionDao().getAllMissions().map { l -> l.map { it.toDomain() } }
    val notificationsFlow: Flow<List<AppNotification>> = db.notificationDao().getAll().map { l -> l.map { it.toDomain() } }
    val allJournalsFlow: Flow<List<HealthJournal>> = db.healthJournalDao().getAll().map { l -> l.map { it.toDomain() } }

    fun observeQuitProgress(userId: String): Flow<QuitProgress?> =
        db.quitProgressDao().observeByUser(userId).map { it?.toDomain() }

    fun getSavedUserId(): String? = prefs.getString("active_user_id", null)

    fun saveActiveUserId(userId: String?) {
        prefs.edit().apply {
            if (userId == null) remove("active_user_id") else putString("active_user_id", userId)
        }.apply()
    }

    fun getSelectedCity(): String = prefs.getString("selected_city", "Makassar") ?: "Makassar"

    fun saveSelectedCity(city: String) {
        prefs.edit().putString("selected_city", city).apply()
    }

    suspend fun ensureSeeded() {
        val existingUsers = db.userDao().getAllUsers().first()
        if (existingUsers.isEmpty()) {
            seedAllDemoData()
        }
    }

    suspend fun seedAllDemoData() {
        db.userDao().clearAll()
        db.airQualityDao().clearAll()
        db.smokeZoneDao().clearAll()
        db.smokeReportDao().clearAll()
        db.quitProgressDao().clearAll()
        db.dailyMissionDao().clearAll()
        db.healthJournalDao().clearAll()
        db.notificationDao().clearAll()

        val now = System.currentTimeMillis()
        db.userDao().upsertAll(SwasanaSeedData.demoUsers().map { it.toEntity() })
        db.airQualityDao().insertAll(
            SwasanaSeedData.indonesianCityReadings().mapIndexed { idx, r ->
                r.toEntity(now - idx * 60_000L)
            }
        )
        db.smokeZoneDao().upsertAll(SwasanaSeedData.seedSmokeFreeZones().map { it.toEntity() })
        db.smokeReportDao().upsertAll(
            SwasanaSeedData.seedSmokeReports().mapIndexed { idx, r ->
                r.toEntity(now - idx * 3_600_000L)
            }
        )
        db.quitProgressDao().upsert(SwasanaSeedData.seedQuitProgress(SwasanaSeedData.DEMO_USER_ID).toEntity())
        db.quitProgressDao().upsert(SwasanaSeedData.seedQuitProgress(SwasanaSeedData.DEMO_ADMIN_ID).toEntity())
        db.dailyMissionDao().upsertAll(SwasanaSeedData.seedDailyMissions().map { it.toEntity() })

        val seedJournals = SwasanaSeedData.seedHealthJournals(SwasanaSeedData.DEMO_USER_ID)
        db.healthJournalDao().insertAll(
            seedJournals.mapIndexed { idx, j ->
                j.toEntity(now - (seedJournals.size - 1 - idx) * 86_400_000L)
            }
        )
        // Also seed journals for Admin demo account so charts look rich in both roles
        val adminJournals = SwasanaSeedData.seedHealthJournals(SwasanaSeedData.DEMO_ADMIN_ID)
            .map { it.copy(id = "adm_${it.id}") }
        db.healthJournalDao().insertAll(
            adminJournals.mapIndexed { idx, j ->
                j.toEntity(now - (adminJournals.size - 1 - idx) * 86_400_000L)
            }
        )

        db.notificationDao().insertAll(
            SwasanaSeedData.seedNotifications(SwasanaSeedData.DEMO_USER_ID).mapIndexed { idx, n ->
                n.toEntity(now - idx * 1_800_000L)
            }
        )
        saveSelectedCity("Makassar")
    }

    suspend fun upsertUser(user: User) = db.userDao().upsert(user.toEntity())
    suspend fun getUserByEmail(email: String): User? = db.userDao().getByEmail(email)?.toDomain()
    suspend fun getUserById(id: String): User? = db.userDao().getById(id)?.toDomain()

    suspend fun upsertZone(zone: SmokeFreeZone) = db.smokeZoneDao().upsert(zone.toEntity())
    suspend fun deleteZone(id: String) = db.smokeZoneDao().deleteById(id)

    suspend fun upsertReport(report: SmokeReport) = db.smokeReportDao().upsert(report.toEntity())
    suspend fun updateReportStatus(id: String, status: ReportStatus) =
        db.smokeReportDao().updateStatus(id, status.name)

    suspend fun upsertQuitProgress(progress: QuitProgress) = db.quitProgressDao().upsert(progress.toEntity())
    suspend fun toggleMission(id: String, completed: Boolean) = db.dailyMissionDao().setCompleted(id, completed)

    suspend fun insertNotification(notification: AppNotification) =
        db.notificationDao().insert(notification.toEntity())

    suspend fun markNotificationRead(id: String) = db.notificationDao().markRead(id)
    suspend fun markAllNotificationsRead() = db.notificationDao().markAllRead()
}
