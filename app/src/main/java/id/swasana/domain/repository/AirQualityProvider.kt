package id.swasana.domain.repository

import id.swasana.domain.model.AirQualityForecastPoint
import id.swasana.domain.model.AirQualityProviderType
import id.swasana.domain.model.AirQualityReading
import id.swasana.domain.model.GeoPoint
import id.swasana.domain.model.PollutionHotspot

/** Sumber data kualitas udara. Implementasi: WAQI (utama) dan OpenWeather (cadangan/prakiraan). */
interface AirQualityProvider {
  val type: AirQualityProviderType

  /** Nama sumber yang ditampilkan di layar. */
  val displayName: String

  /** false bila token/API key belum diisi. */
  val isConfigured: Boolean

  suspend fun current(point: GeoPoint): Result<AirQualityReading>

  suspend fun forecast(point: GeoPoint, hours: Int): Result<List<AirQualityForecastPoint>>

  suspend fun hotspots(point: GeoPoint, radiusKm: Double): Result<List<PollutionHotspot>>
}
