package id.swasana.data.remote.openweather

import id.swasana.BuildConfig
import id.swasana.domain.aqi.AqiCalculator
import id.swasana.domain.model.AirQualityForecastPoint
import id.swasana.domain.model.AirQualityProviderType
import id.swasana.domain.model.AirQualityReading
import id.swasana.domain.model.GeoPoint
import id.swasana.domain.model.PollutionHotspot
import id.swasana.domain.repository.AirQualityProvider
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

@Serializable
data class OwComponents(
  @SerialName("pm2_5") val pm25: Double? = null,
  val pm10: Double? = null,
  val o3: Double? = null,
)

@Serializable data class OwMain(val aqi: Int = 0)

@Serializable
data class OwItem(val dt: Long, val main: OwMain = OwMain(), val components: OwComponents = OwComponents())

@Serializable data class OwResponse(val list: List<OwItem> = emptyList())

/** https://openweathermap.org/api/air-pollution */
interface OpenWeatherApi {
  @GET("data/2.5/air_pollution")
  suspend fun current(
    @Query("lat") lat: Double,
    @Query("lon") lon: Double,
    @Query("appid") key: String,
  ): OwResponse

  @GET("data/2.5/air_pollution/forecast")
  suspend fun forecast(
    @Query("lat") lat: Double,
    @Query("lon") lon: Double,
    @Query("appid") key: String,
  ): OwResponse
}

/**
 * Sumber cadangan dan prakiraan. Indeks bawaan OpenWeather berskala 1–5, sehingga AQI dihitung
 * ulang dari konsentrasi PM2.5 dengan breakpoint EPA.
 */
@Singleton
class OpenWeatherAirQualityProvider @Inject constructor(private val api: OpenWeatherApi) :
  AirQualityProvider {

  private val key: String = BuildConfig.OPENWEATHER_API_KEY

  override val type = AirQualityProviderType.OPENWEATHER
  override val displayName = "OpenWeather"
  override val isConfigured: Boolean get() = key.isNotBlank()

  override suspend fun current(point: GeoPoint): Result<AirQualityReading> = runCatching {
    check(isConfigured) { "API key OpenWeather belum diisi" }
    val item =
      api.current(point.lat, point.lng, key).list.firstOrNull()
        ?: error("OpenWeather: data kosong")
    toReading(item, point)
  }

  override suspend fun forecast(point: GeoPoint, hours: Int): Result<List<AirQualityForecastPoint>> =
    runCatching {
      check(isConfigured) { "API key OpenWeather belum diisi" }
      val now = Instant.now().epochSecond
      api
        .forecast(point.lat, point.lng, key)
        .list
        .filter { it.dt >= now - 3600 }
        .take(hours)
        .map {
          val aqi = it.components.pm25?.let(AqiCalculator::aqiFromPm25) ?: 0
          AirQualityForecastPoint(Instant.ofEpochSecond(it.dt), aqi, AqiCalculator.levelFor(aqi))
        }
    }

  override suspend fun hotspots(point: GeoPoint, radiusKm: Double): Result<List<PollutionHotspot>> =
    Result.success(emptyList())

  internal fun toReading(item: OwItem, point: GeoPoint): AirQualityReading {
    val pm25 = item.components.pm25
    val aqi = pm25?.let(AqiCalculator::aqiFromPm25) ?: error("OpenWeather: PM2.5 tidak tersedia")
    return AirQualityReading(
      location = point.name,
      lat = point.lat,
      lng = point.lng,
      aqi = aqi,
      level = AqiCalculator.levelFor(aqi),
      pm25 = pm25,
      pm10 = item.components.pm10,
      o3 = item.components.o3,
      source = displayName,
      recordedAt = Instant.ofEpochSecond(item.dt),
    )
  }
}
