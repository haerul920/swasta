package id.swasana.domain.usecase

import id.swasana.domain.aqi.displayAqi
import id.swasana.domain.model.AirQualityForecastPoint
import id.swasana.domain.model.AirQualityReading
import id.swasana.domain.model.AqiScale
import id.swasana.domain.model.DisplayAqi
import id.swasana.domain.model.GeoPoint
import id.swasana.domain.model.PollutionHotspot
import id.swasana.domain.repository.AirQualityRepository
import id.swasana.domain.repository.LocationRepository
import id.swasana.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/** Tidak ada lokasi yang bisa dipakai (izin ditolak dan belum memilih lokasi manual). */
class NoLocationException : Exception("Lokasi tidak tersedia")

class AirQualityService
@Inject
constructor(
  private val repository: AirQualityRepository,
  private val locations: LocationRepository,
  private val settings: SettingsRepository,
) {

  fun observeLatest(): Flow<AirQualityReading?> = repository.observeLatest()

  /** Lokasi aktif yang dipakai aplikasi, dan menyimpannya sebagai lokasi terakhir di perangkat. */
  suspend fun activeLocation(): GeoPoint? {
    val point = locations.resolveLocation() ?: settings.settings.first().lastLocation
    if (point != null) settings.update { it.copy(lastLocation = point) }
    return point
  }

  suspend fun refresh(force: Boolean = false): Result<AirQualityReading> {
    val point = activeLocation() ?: return Result.failure(NoLocationException())
    return repository.refresh(point, force)
  }

  suspend fun forecast(hours: Int = 12): Result<List<AirQualityForecastPoint>> {
    val point = activeLocation() ?: return Result.failure(NoLocationException())
    return repository.forecast(point, hours)
  }

  suspend fun hotspots(point: GeoPoint): List<PollutionHotspot> = repository.hotspots(point)

  fun display(reading: AirQualityReading, scale: AqiScale): DisplayAqi = reading.displayAqi(scale)
}
