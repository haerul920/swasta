package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.entities.AirQualityReading
import com.example.domain.entities.AppNotification
import com.example.domain.entities.DailyMission
import com.example.domain.entities.HealthJournal
import com.example.domain.entities.QuitProgress
import com.example.domain.entities.SmokeFreeZone
import com.example.domain.entities.SmokeReport
import com.example.domain.entities.SymptomEntry
import com.example.domain.entities.User
import com.example.domain.enums.AirQualityLevel
import com.example.domain.enums.HealthProfileGroup
import com.example.domain.enums.NotificationType
import com.example.domain.enums.ReportStatus
import com.example.domain.enums.SmokeSeverity
import com.example.domain.enums.SymptomType
import com.example.domain.enums.UserRole
import com.example.domain.enums.ZoneCategory
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val passwordHash: String,
    val role: String,
    val healthGroup: String,
    val alertThresholdAqi: Int
)

@Entity(tableName = "air_quality_readings")
data class AirQualityReadingEntity(
    @PrimaryKey val id: String,
    val location: String,
    val latitude: Double,
    val longitude: Double,
    val aqi: Int,
    val pm25: Double,
    val pm10: Double,
    val level: String,
    val recordedAt: String,
    val createdEpochMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "smoke_free_zones")
data class SmokeFreeZoneEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val description: String,
    val rating: Double,
    val verifiedByAdmin: Boolean
)

@Entity(tableName = "smoke_reports")
data class SmokeReportEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val reporterName: String,
    val location: String,
    val latitude: Double,
    val longitude: Double,
    val description: String,
    val severity: String,
    val reportedAt: String,
    val status: String,
    val createdEpochMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "quit_progress")
data class QuitProgressEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val startDateEpochMillis: Long,
    val dailyCigaretteCost: Double,
    val cigarettesPerDay: Int,
    val addictionType: String,
    val targetGoalName: String,
    val targetGoalPrice: Double
)

@Entity(tableName = "daily_missions")
data class DailyMissionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val category: String,
    val xpPoints: Int,
    val completed: Boolean,
    val date: String
)

@Entity(tableName = "health_journals")
data class HealthJournalEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val date: String,
    val aqiSnapshot: Int,
    val notes: String,
    val symptomsJson: String,
    val createdEpochMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val message: String,
    val type: String,
    val createdAt: String,
    val isRead: Boolean,
    val createdEpochMillis: Long = System.currentTimeMillis()
)

// Mappers
fun UserEntity.toDomain() = User(
    id = id,
    name = name,
    email = email,
    passwordHash = passwordHash,
    role = runCatching { UserRole.valueOf(role) }.getOrDefault(UserRole.USER),
    healthGroup = runCatching { HealthProfileGroup.valueOf(healthGroup) }.getOrDefault(HealthProfileGroup.GENERAL),
    alertThresholdAqi = alertThresholdAqi
)

fun User.toEntity() = UserEntity(
    id = id,
    name = name,
    email = email,
    passwordHash = passwordHash,
    role = role.name,
    healthGroup = healthGroup.name,
    alertThresholdAqi = alertThresholdAqi
)

fun AirQualityReadingEntity.toDomain() = AirQualityReading(
    id = id,
    location = location,
    latitude = latitude,
    longitude = longitude,
    aqi = aqi,
    pm25 = pm25,
    pm10 = pm10,
    level = runCatching { AirQualityLevel.valueOf(level) }.getOrDefault(AirQualityLevel.fromAqi(aqi)),
    recordedAt = recordedAt
)

fun AirQualityReading.toEntity(epoch: Long = System.currentTimeMillis()) = AirQualityReadingEntity(
    id = id,
    location = location,
    latitude = latitude,
    longitude = longitude,
    aqi = aqi,
    pm25 = pm25,
    pm10 = pm10,
    level = level.name,
    recordedAt = recordedAt,
    createdEpochMillis = epoch
)

fun SmokeFreeZoneEntity.toDomain() = SmokeFreeZone(
    id = id,
    name = name,
    category = runCatching { ZoneCategory.valueOf(category) }.getOrDefault(ZoneCategory.TAMAN),
    address = address,
    latitude = latitude,
    longitude = longitude,
    description = description,
    rating = rating,
    verifiedByAdmin = verifiedByAdmin
)

fun SmokeFreeZone.toEntity() = SmokeFreeZoneEntity(
    id = id,
    name = name,
    category = category.name,
    address = address,
    latitude = latitude,
    longitude = longitude,
    description = description,
    rating = rating,
    verifiedByAdmin = verifiedByAdmin
)

fun SmokeReportEntity.toDomain() = SmokeReport(
    id = id,
    userId = userId,
    reporterName = reporterName,
    location = location,
    latitude = latitude,
    longitude = longitude,
    description = description,
    severity = runCatching { SmokeSeverity.valueOf(severity) }.getOrDefault(SmokeSeverity.SEDANG),
    reportedAt = reportedAt,
    status = runCatching { ReportStatus.valueOf(status) }.getOrDefault(ReportStatus.PENDING)
)

fun SmokeReport.toEntity(epoch: Long = System.currentTimeMillis()) = SmokeReportEntity(
    id = id,
    userId = userId,
    reporterName = reporterName,
    location = location,
    latitude = latitude,
    longitude = longitude,
    description = description,
    severity = severity.name,
    reportedAt = reportedAt,
    status = status.name,
    createdEpochMillis = epoch
)

fun QuitProgressEntity.toDomain() = QuitProgress(
    id = id,
    userId = userId,
    startDateEpochMillis = startDateEpochMillis,
    dailyCigaretteCost = dailyCigaretteCost,
    cigarettesPerDay = cigarettesPerDay,
    addictionType = addictionType,
    targetGoalName = targetGoalName,
    targetGoalPrice = targetGoalPrice
)

fun QuitProgress.toEntity() = QuitProgressEntity(
    id = id,
    userId = userId,
    startDateEpochMillis = startDateEpochMillis,
    dailyCigaretteCost = dailyCigaretteCost,
    cigarettesPerDay = cigarettesPerDay,
    addictionType = addictionType,
    targetGoalName = targetGoalName,
    targetGoalPrice = targetGoalPrice
)

fun DailyMissionEntity.toDomain() = DailyMission(
    id = id,
    title = title,
    description = description,
    category = category,
    xpPoints = xpPoints,
    completed = completed,
    date = date
)

fun DailyMission.toEntity() = DailyMissionEntity(
    id = id,
    title = title,
    description = description,
    category = category,
    xpPoints = xpPoints,
    completed = completed,
    date = date
)

fun HealthJournalEntity.toDomain(): HealthJournal {
    val list = mutableListOf<SymptomEntry>()
    runCatching {
        val arr = JSONArray(symptomsJson)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                SymptomEntry(
                    id = obj.optString("id"),
                    type = runCatching { SymptomType.valueOf(obj.optString("type")) }
                        .getOrDefault(SymptomType.COUGH),
                    severity = obj.optInt("severity", 1).coerceIn(1, 5),
                    notes = obj.optString("notes")
                )
            )
        }
    }
    return HealthJournal(
        id = id,
        userId = userId,
        date = date,
        aqiSnapshot = aqiSnapshot,
        notes = notes,
        symptoms = list
    )
}

fun HealthJournal.toEntity(epoch: Long = System.currentTimeMillis()): HealthJournalEntity {
    val arr = JSONArray()
    symptoms.forEach { s ->
        val obj = JSONObject()
        obj.put("id", s.id)
        obj.put("type", s.type.name)
        obj.put("severity", s.severity)
        obj.put("notes", s.notes)
        arr.put(obj)
    }
    return HealthJournalEntity(
        id = id,
        userId = userId,
        date = date,
        aqiSnapshot = aqiSnapshot,
        notes = notes,
        symptomsJson = arr.toString(),
        createdEpochMillis = epoch
    )
}

fun NotificationEntity.toDomain() = AppNotification(
    id = id,
    userId = userId,
    title = title,
    message = message,
    type = runCatching { NotificationType.valueOf(type) }.getOrDefault(NotificationType.AQI_ALERT),
    createdAt = createdAt,
    isRead = isRead
)

fun AppNotification.toEntity(epoch: Long = System.currentTimeMillis()) = NotificationEntity(
    id = id,
    userId = userId,
    title = title,
    message = message,
    type = type.name,
    createdAt = createdAt,
    isRead = isRead,
    createdEpochMillis = epoch
)
