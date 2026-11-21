package id.swasana.data.local.room

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import id.swasana.domain.aqi.AqiCalculator
import id.swasana.domain.model.AirQualityReading
import id.swasana.domain.model.AppNotification
import id.swasana.domain.model.CravingEvent
import id.swasana.domain.model.CravingTrigger
import id.swasana.domain.model.HealthJournalEntry
import id.swasana.domain.model.ReportStatus
import id.swasana.domain.model.ReportType
import id.swasana.domain.model.SmokeFreeZone
import id.swasana.domain.model.SmokeReport
import id.swasana.domain.model.SymptomEntry
import id.swasana.domain.model.SymptomType
import id.swasana.domain.model.ZoneCategory
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "air_quality_reading", indices = [Index("recordedAt")])
data class AirQualityReadingEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val location: String,
  val lat: Double,
  val lng: Double,
  val aqi: Int,
  val pm25: Double?,
  val pm10: Double?,
  val o3: Double?,
  val source: String,
  val recordedAt: Long,
)

fun AirQualityReadingEntity.toDomain() =
  AirQualityReading(
    id = id,
    location = location,
    lat = lat,
    lng = lng,
    aqi = aqi,
    level = AqiCalculator.levelFor(aqi),
    pm25 = pm25,
    pm10 = pm10,
    o3 = o3,
    source = source,
    recordedAt = Instant.ofEpochMilli(recordedAt),
  )

fun AirQualityReading.toEntity() =
  AirQualityReadingEntity(
    id = id,
    location = location,
    lat = lat,
    lng = lng,
    aqi = aqi,
    pm25 = pm25,
    pm10 = pm10,
    o3 = o3,
    source = source,
    recordedAt = recordedAt.toEpochMilli(),
  )

@Entity(tableName = "journal_entry", indices = [Index(value = ["dateEpochDay"], unique = true)])
data class JournalEntryEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val dateEpochDay: Long,
  /** Daftar [SymptomType] dipisah koma. */
  val symptoms: String,
  val severity: Int,
  val inhalerUses: Int,
  val cigarettesToday: Int?,
  val aqiAtEntry: Int?,
  val locationName: String?,
  val lat: Double?,
  val lng: Double?,
  val notes: String,
)

fun JournalEntryEntity.toDomain() =
  HealthJournalEntry(
    id = id,
    date = LocalDate.ofEpochDay(dateEpochDay),
    symptoms =
      symptoms
        .split(",")
        .filter { it.isNotBlank() }
        .mapNotNull { name -> runCatching { SymptomType.valueOf(name) }.getOrNull() }
        .map { SymptomEntry(it, severity) },
    severity = severity,
    inhalerUses = inhalerUses,
    cigarettesToday = cigarettesToday,
    aqiAtEntry = aqiAtEntry,
    locationName = locationName,
    lat = lat,
    lng = lng,
    notes = notes,
  )

fun HealthJournalEntry.toEntity() =
  JournalEntryEntity(
    id = id,
    dateEpochDay = date.toEpochDay(),
    symptoms = symptoms.joinToString(",") { it.type.name },
    severity = severity,
    inhalerUses = inhalerUses,
    cigarettesToday = cigarettesToday,
    aqiAtEntry = aqiAtEntry,
    locationName = locationName,
    lat = lat,
    lng = lng,
    notes = notes,
  )

@Entity(tableName = "craving_event")
data class CravingEventEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val timestamp: Long,
  val intensity: Int,
  val trigger: String?,
  val overcome: Boolean,
)

fun CravingEventEntity.toDomain() =
  CravingEvent(
    id = id,
    timestamp = Instant.ofEpochMilli(timestamp),
    intensity = intensity,
    trigger = trigger?.let { runCatching { CravingTrigger.valueOf(it) }.getOrNull() },
    overcome = overcome,
  )

fun CravingEvent.toEntity() =
  CravingEventEntity(
    id = id,
    timestamp = timestamp.toEpochMilli(),
    intensity = intensity,
    trigger = trigger?.name,
    overcome = overcome,
  )

@Entity(tableName = "smoke_free_zone")
data class SmokeFreeZoneEntity(
  @PrimaryKey val id: String,
  val name: String,
  val address: String,
  val lat: Double,
  val lng: Double,
  val description: String,
  val category: String,
  val verified: Boolean,
  val proposerId: String?,
)

fun SmokeFreeZoneEntity.toDomain() =
  SmokeFreeZone(
    id = id,
    name = name,
    address = address,
    lat = lat,
    lng = lng,
    description = description,
    category = runCatching { ZoneCategory.valueOf(category) }.getOrDefault(ZoneCategory.OTHER),
    verified = verified,
    proposerId = proposerId,
  )

fun SmokeFreeZone.toEntity() =
  SmokeFreeZoneEntity(
    id = id,
    name = name,
    address = address,
    lat = lat,
    lng = lng,
    description = description,
    category = category.name,
    verified = verified,
    proposerId = proposerId,
  )

@Entity(tableName = "smoke_report", indices = [Index("status"), Index("reporterId")])
data class SmokeReportEntity(
  @PrimaryKey val id: String,
  val type: String,
  val lat: Double,
  val lng: Double,
  val description: String,
  val photoUrl: String?,
  val reportedAt: Long,
  val status: String,
  val reporterId: String,
  val address: String,
)

fun SmokeReportEntity.toDomain() =
  SmokeReport(
    id = id,
    type = runCatching { ReportType.valueOf(type) }.getOrDefault(ReportType.CIGARETTE_SMOKE),
    lat = lat,
    lng = lng,
    description = description,
    photoUrl = photoUrl,
    reportedAt = Instant.ofEpochMilli(reportedAt),
    status = runCatching { ReportStatus.valueOf(status) }.getOrDefault(ReportStatus.PENDING),
    reporterId = reporterId,
    address = address,
  )

fun SmokeReport.toEntity() =
  SmokeReportEntity(
    id = id,
    type = type.name,
    lat = lat,
    lng = lng,
    description = description,
    photoUrl = photoUrl,
    reportedAt = reportedAt.toEpochMilli(),
    status = status.name,
    reporterId = reporterId,
    address = address,
  )

@Entity(tableName = "app_notification")
data class NotificationEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val message: String,
  val createdAt: Long,
  val isRead: Boolean,
)

fun NotificationEntity.toDomain() =
  AppNotification(id, message, Instant.ofEpochMilli(createdAt), isRead)
