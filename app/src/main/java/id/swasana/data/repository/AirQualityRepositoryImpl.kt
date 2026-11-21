package id.swasana.data.repository

import id.swasana.data.local.room.AirQualityDao
import id.swasana.data.local.room.toDomain
import id.swasana.data.local.room.toEntity
import id.swasana.data.remote.DemoAirQualityProvider
import id.swasana.data.remote.openweather.OpenWeatherAirQualityProvider
import id.swasana.data.remote.waqi.WaqiAirQualityProvider
import id.swasana.domain.model.AirQualityForecastPoint
import id.swasana.domain.model.AirQualityProviderType
import id.swasana.domain.model.AirQualityReading
import id.swasana.domain.model.GeoPoint
import id.swasana.domain.model.PollutionHotspot
import id.swasana.domain.repository.AirQualityProvider
import id.swasana.domain.repository.AirQualityRepository
import id.swasana.domain.repository.SettingsRepository
import id.swasana.domain.usecase.SmokeZoneService
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Singleton
class AirQualityRepositoryImpl
@Inject
constructor(
  private val waqi: WaqiAirQualityProvider,
  private val openWeather: OpenWeatherAirQualityProvider,
  private val demo: DemoAirQualityProvider,
  private val dao: AirQualityDao,
  private val settings: SettingsRepository,
  private val clock: Clock,
) : AirQualityRepository {

  override fun observeLatest(): Flow<AirQualityReading?> = dao.observeLatest().map { it?.toDomain() }

  override fun observeSince(since: Instant): Flow<List<AirQualityReading>> =
    dao.observeSince(since.toEpochMilli()).map { list -> list.map { it.toDomain() } }

  /** Urutan sumber: pilihan pengguna, lalu cadangan; bila keduanya belum dikonfigurasi, data contoh. */
  private suspend fun orderedProviders(): List<AirQualityProvider> {
    val preferred = settings.settings.first().provider
    val primary = if (preferred == AirQualityProviderType.WAQI) waqi else openWeather
    val secondary = if (primary === waqi) openWeather else waqi
    val configured = listOf(primary, secondary).filter { it.isConfigured }
    return configured.ifEmpty { listOf(demo) }
  }

  override suspend fun refresh(point: GeoPoint, force: Boolean): Result<AirQualityReading> {
    val cached = dao.latest()?.toDomain()
    val now = clock.instant()
    if (!force && cached != null && isFresh(cached, point, now)) return Result.success(cached)

    var lastError: Throwable? = null
    for (provider in orderedProviders()) {
      val result = provider.current(point)
      result.onSuccess { reading ->
        val fixed = reading.copy(recordedAt = reading.recordedAt.coerceAtMost(now))
        val id = dao.insert(fixed.toEntity())
        dao.pruneBefore(now.minus(Duration.ofDays(RETENTION_DAYS)).toEpochMilli())
        return Result.success(fixed.copy(id = id))
      }
      lastError = result.exceptionOrNull()
    }
    // Semua sumber gagal: pakai cache terakhir bila lokasinya masih berdekatan.
    if (cached != null && isNear(cached, point)) return Result.success(cached)
    return Result.failure(lastError ?: IllegalStateException("Data kualitas udara tidak tersedia"))
  }

  override suspend fun forecast(point: GeoPoint, hours: Int): Result<List<AirQualityForecastPoint>> {
    var lastError: Throwable? = null
    // Prakiraan per jam dari OpenWeather lebih dulu, lalu sumber lain.
    val providers =
      buildList {
        if (openWeather.isConfigured) add(openWeather)
        if (waqi.isConfigured) add(waqi)
        if (isEmpty()) add(demo)
      }
    for (p in providers) {
      val r = p.forecast(point, hours)
      if (r.isSuccess) return r
      lastError = r.exceptionOrNull()
    }
    return Result.failure(lastError ?: IllegalStateException("Prakiraan tidak tersedia"))
  }

  override suspend fun hotspots(point: GeoPoint, radiusKm: Double): List<PollutionHotspot> {
    for (provider in orderedProviders()) {
      val r = provider.hotspots(point, radiusKm)
      val list = r.getOrNull()
      if (!list.isNullOrEmpty()) return list
    }
    return emptyList()
  }

  private fun isFresh(r: AirQualityReading, p: GeoPoint, now: Instant): Boolean =
    Duration.between(r.recordedAt, now) < Duration.ofMinutes(FRESH_MINUTES) && isNear(r, p)

  private fun isNear(r: AirQualityReading, p: GeoPoint): Boolean =
    SmokeZoneService.distanceMeters(r.lat, r.lng, p.lat, p.lng) < NEAR_METERS

  private companion object {
    const val FRESH_MINUTES = 20L
    const val NEAR_METERS = 3_000.0
    const val RETENTION_DAYS = 60L
  }
}
