package id.swasana.domain.usecase

import id.swasana.domain.model.GeoPoint
import id.swasana.domain.model.PollutionHotspot
import id.swasana.domain.model.ReportStatus
import id.swasana.domain.model.ReportType
import id.swasana.domain.model.SmokeFreeZone
import id.swasana.domain.model.SmokeReport
import id.swasana.domain.repository.SmokeRepository
import java.time.Clock
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.flow.Flow

enum class MapFilter {
  ALL,
  SMOKE_FREE,
  HIGH_POLLUTION,
  CITIZEN_REPORT,
}

enum class MapPinType {
  SMOKE_FREE,
  HIGH_POLLUTION,
  CITIZEN_REPORT,
}

/** Pin gabungan untuk peta. [subtitleKey] dan [meta] dipetakan ke teks di lapisan UI. */
data class MapPin(
  val id: String,
  val type: MapPinType,
  val title: String,
  val description: String,
  val lat: Double,
  val lng: Double,
  val distanceMeters: Double?,
  val verified: Boolean,
  val aqi: Int? = null,
  val reportedAt: Instant? = null,
  val reportType: ReportType? = null,
)

class SmokeZoneService
@Inject
constructor(private val repository: SmokeRepository, private val clock: Clock) {

  fun observeVerifiedZones(): Flow<List<SmokeFreeZone>> = repository.observeVerifiedZones()

  fun observeProposedZones(): Flow<List<SmokeFreeZone>> = repository.observeProposedZones()

  fun observeReports(status: ReportStatus): Flow<List<SmokeReport>> =
    repository.observeReports(status)

  suspend fun submitReport(
    reporterId: String,
    type: ReportType,
    point: GeoPoint,
    description: String,
    photoUri: String?,
  ): Result<Unit> =
    repository.submitReport(
      SmokeReport(
        id = UUID.randomUUID().toString(),
        type = type,
        lat = point.lat,
        lng = point.lng,
        description = description.trim(),
        photoUrl = null,
        reportedAt = clock.instant(),
        status = ReportStatus.PENDING,
        reporterId = reporterId,
        address = point.name,
      ),
      photoUri,
    )

  suspend fun proposeZone(
    proposerId: String,
    name: String,
    point: GeoPoint,
    description: String,
    category: id.swasana.domain.model.ZoneCategory,
  ): Result<Unit> =
    repository.proposeZone(
      SmokeFreeZone(
        id = UUID.randomUUID().toString(),
        name = name.trim(),
        address = point.name,
        lat = point.lat,
        lng = point.lng,
        description = description.trim(),
        category = category,
        verified = false,
        proposerId = proposerId,
      )
    )

  suspend fun setReportStatus(id: String, status: ReportStatus) =
    repository.setReportStatus(id, status)

  suspend fun verifyZone(id: String) = repository.verifyZone(id)

  suspend fun upsertZone(zone: SmokeFreeZone) = repository.upsertZone(zone)

  suspend fun deleteZone(id: String) = repository.deleteZone(id)

  suspend fun deleteReportsBy(reporterId: String) = repository.deleteReportsBy(reporterId)

  /** Gabungkan zona, titik polusi, dan laporan warga menjadi pin, lalu terapkan filter dan pencarian. */
  fun buildPins(
    center: GeoPoint?,
    zones: List<SmokeFreeZone>,
    hotspots: List<PollutionHotspot>,
    reports: List<SmokeReport>,
    filter: MapFilter,
    query: String,
  ): List<MapPin> {
    val pins = mutableListOf<MapPin>()
    if (filter == MapFilter.ALL || filter == MapFilter.SMOKE_FREE) {
      zones.forEach {
        pins +=
          MapPin(
            id = "zone:${it.id}",
            type = MapPinType.SMOKE_FREE,
            title = it.name,
            description = it.description,
            lat = it.lat,
            lng = it.lng,
            distanceMeters = center?.let { c -> distanceMeters(c.lat, c.lng, it.lat, it.lng) },
            verified = it.verified,
          )
      }
    }
    if (filter == MapFilter.ALL || filter == MapFilter.HIGH_POLLUTION) {
      hotspots.forEach {
        pins +=
          MapPin(
            id = "hot:${it.id}",
            type = MapPinType.HIGH_POLLUTION,
            title = it.name,
            description = "",
            lat = it.lat,
            lng = it.lng,
            distanceMeters = center?.let { c -> distanceMeters(c.lat, c.lng, it.lat, it.lng) },
            verified = true,
            aqi = it.aqi,
            reportedAt = it.updatedAt,
          )
      }
    }
    if (filter == MapFilter.ALL || filter == MapFilter.CITIZEN_REPORT) {
      reports.forEach {
        pins +=
          MapPin(
            id = "rep:${it.id}",
            type = MapPinType.CITIZEN_REPORT,
            title = it.address.ifBlank { "" },
            description = it.description,
            lat = it.lat,
            lng = it.lng,
            distanceMeters = center?.let { c -> distanceMeters(c.lat, c.lng, it.lat, it.lng) },
            verified = it.status == ReportStatus.VERIFIED,
            reportedAt = it.reportedAt,
            reportType = it.type,
          )
      }
    }
    val q = query.trim()
    val filtered =
      if (q.isEmpty()) pins
      else pins.filter { it.title.contains(q, true) || it.description.contains(q, true) }
    return filtered.sortedBy { it.distanceMeters ?: Double.MAX_VALUE }
  }

  companion object {
    private const val EARTH_RADIUS_M = 6_371_000.0

    /** Jarak haversine dalam meter. */
    fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
      val dLat = Math.toRadians(lat2 - lat1)
      val dLng = Math.toRadians(lng2 - lng1)
      val a =
        sin(dLat / 2).pow(2) +
          cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
      return 2 * EARTH_RADIUS_M * asin(sqrt(a))
    }
  }
}
