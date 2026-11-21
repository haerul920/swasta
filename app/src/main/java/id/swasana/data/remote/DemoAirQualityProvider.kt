package id.swasana.data.remote

import id.swasana.domain.aqi.AqiCalculator
import id.swasana.domain.model.AirQualityForecastPoint
import id.swasana.domain.model.AirQualityProviderType
import id.swasana.domain.model.AirQualityReading
import id.swasana.domain.model.GeoPoint
import id.swasana.domain.model.PollutionHotspot
import id.swasana.domain.repository.AirQualityProvider
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data contoh yang dipakai HANYA bila token WAQI maupun API key OpenWeather belum diisi,
 * agar aplikasi tetap bisa dicoba saat pengembangan. Sumbernya selalu diberi label "Data contoh".
 */
@Singleton
class DemoAirQualityProvider @Inject constructor() : AirQualityProvider {
  override val type = AirQualityProviderType.WAQI
  override val displayName = DISPLAY_NAME
  override val isConfigured = true

  override suspend fun current(point: GeoPoint): Result<AirQualityReading> {
    // PM2.5 tetap agar tampilan konsisten: sekitar AQI 128 (tidak sehat bagi kelompok sensitif).
    val pm25 = 47.0
    val aqi = AqiCalculator.aqiFromPm25(pm25)
    return Result.success(
      AirQualityReading(
        location = point.name,
        lat = point.lat,
        lng = point.lng,
        aqi = aqi,
        level = AqiCalculator.levelFor(aqi),
        pm25 = pm25,
        pm10 = 89.0,
        o3 = 52.0,
        source = DISPLAY_NAME,
        recordedAt = Instant.now(),
      )
    )
  }

  override suspend fun forecast(point: GeoPoint, hours: Int): Result<List<AirQualityForecastPoint>> {
    val base = Instant.now().truncatedTo(ChronoUnit.HOURS)
    val pattern = listOf(128, 135, 142, 151, 146, 136, 125, 118, 110, 104, 98, 92)
    return Result.success(
      (0 until hours).map { i ->
        val v = pattern[i % pattern.size]
        AirQualityForecastPoint(base.plus((i + 1).toLong(), ChronoUnit.HOURS), v, AqiCalculator.levelFor(v))
      }
    )
  }

  override suspend fun hotspots(point: GeoPoint, radiusKm: Double): Result<List<PollutionHotspot>> =
    Result.success(
      listOf(
        PollutionHotspot("demo-1", "Simpang jalan utama (contoh)", point.lat + 0.0042, point.lng + 0.0071, 156, AqiCalculator.levelFor(156), Instant.now()),
        PollutionHotspot("demo-2", "Kawasan pergudangan (contoh)", point.lat - 0.0061, point.lng - 0.0049, 138, AqiCalculator.levelFor(138), Instant.now()),
      )
    )

  companion object {
    const val DISPLAY_NAME = "Data contoh (belum ada API key)"
  }
}
