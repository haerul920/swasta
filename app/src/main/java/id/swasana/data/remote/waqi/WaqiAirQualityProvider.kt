package id.swasana.data.remote.waqi

import id.swasana.BuildConfig
import id.swasana.domain.aqi.AqiCalculator
import id.swasana.domain.model.AirQualityForecastPoint
import id.swasana.domain.model.AirQualityProviderType
import id.swasana.domain.model.AirQualityReading
import id.swasana.domain.model.GeoPoint
import id.swasana.domain.model.PollutionHotspot
import id.swasana.domain.repository.AirQualityProvider
import java.time.Instant
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** https://aqicn.org/json-api/doc/ */
interface WaqiApi {
  @GET("feed/geo:{lat};{lon}/")
  suspend fun feed(
    @Path("lat") lat: Double,
    @Path("lon") lon: Double,
    @Query("token") token: String,
  ): ResponseBody

  @GET("map/bounds/")
  suspend fun bounds(@Query("latlng") latlng: String, @Query("token") token: String): ResponseBody
}

class WaqiException(message: String) : Exception(message)

/**
 * Sumber utama. Catatan: nilai `iaqi.*` dari WAQI adalah sub-indeks AQI, bukan konsentrasi.
 * Konsentrasi (µg/m³) diperkirakan dengan membalik breakpoint EPA ([AqiCalculator.pm25FromAqi]).
 */
@Singleton
class WaqiAirQualityProvider
@Inject
constructor(private val api: WaqiApi, private val json: Json) : AirQualityProvider {

  private val token: String = BuildConfig.WAQI_TOKEN

  override val type = AirQualityProviderType.WAQI
  override val displayName = "WAQI (aqicn.org)"
  override val isConfigured: Boolean get() = token.isNotBlank()

  override suspend fun current(point: GeoPoint): Result<AirQualityReading> = runCatching {
    check(isConfigured) { "Token WAQI belum diisi" }
    val body = withContext(Dispatchers.IO) { api.feed(point.lat, point.lng, token).string() }
    WaqiParser.parseFeed(json.parseToJsonElement(body), point, displayName)
  }

  override suspend fun forecast(point: GeoPoint, hours: Int): Result<List<AirQualityForecastPoint>> =
    Result.failure(UnsupportedOperationException("WAQI tidak menyediakan prakiraan per jam"))

  override suspend fun hotspots(point: GeoPoint, radiusKm: Double): Result<List<PollutionHotspot>> =
    runCatching {
      check(isConfigured) { "Token WAQI belum diisi" }
      val dLat = radiusKm / 111.0
      val dLng = radiusKm / (111.0 * Math.cos(Math.toRadians(point.lat)).coerceAtLeast(0.2))
      val latlng =
        "${point.lat - dLat},${point.lng - dLng},${point.lat + dLat},${point.lng + dLng}"
      val body = withContext(Dispatchers.IO) { api.bounds(latlng, token).string() }
      WaqiParser.parseBounds(json.parseToJsonElement(body))
    }
}

object WaqiParser {

  fun parseFeed(root: JsonElement, point: GeoPoint, source: String): AirQualityReading {
    val obj = root.jsonObject
    val status = obj["status"]?.jsonPrimitive?.content
    if (status != "ok") {
      val reason = obj["data"]?.let { (it as? JsonPrimitive)?.content } ?: "status=$status"
      throw WaqiException("WAQI: $reason")
    }
    val data = obj["data"]!!.jsonObject
    val aqi =
      data["aqi"]?.jsonPrimitive?.let { it.intOrNull ?: it.content.toIntOrNull() }
        ?: throw WaqiException("WAQI: nilai AQI belum tersedia")
    val iaqi = data["iaqi"]?.jsonObject
    fun sub(key: String): Int? = iaqi?.get(key)?.jsonObject?.get("v")?.jsonPrimitive?.doubleOrNull?.toInt()

    val pm25 = sub("pm25")?.let { AqiCalculator.pm25FromAqi(it) }
    val pm10 = sub("pm10")?.let { AqiCalculator.pm10FromAqi(it) }
    val o3 = sub("o3")?.let { AqiCalculator.o3FromAqi(it) }

    val city = data["city"]?.jsonObject
    val geo = city?.get("geo")?.jsonArray
    val lat = geo?.getOrNull(0)?.jsonPrimitive?.doubleOrNull ?: point.lat
    val lng = geo?.getOrNull(1)?.jsonPrimitive?.doubleOrNull ?: point.lng
    val stationName = city?.get("name")?.jsonPrimitive?.content
    val time =
      data["time"]?.jsonObject?.get("iso")?.jsonPrimitive?.content?.let {
        runCatching { OffsetDateTime.parse(it).toInstant() }.getOrNull()
      } ?: Instant.now()

    return AirQualityReading(
      location = point.name.ifBlank { stationName.orEmpty() },
      lat = point.lat.takeIf { point.name.isNotBlank() } ?: lat,
      lng = point.lng.takeIf { point.name.isNotBlank() } ?: lng,
      aqi = aqi,
      level = AqiCalculator.levelFor(aqi),
      pm25 = pm25,
      pm10 = pm10,
      o3 = o3,
      source = source + if (!stationName.isNullOrBlank()) " · $stationName" else "",
      recordedAt = time,
    )
  }

  fun parseBounds(root: JsonElement, minAqi: Int = 101, limit: Int = 12): List<PollutionHotspot> {
    val obj = root.jsonObject
    if (obj["status"]?.jsonPrimitive?.content != "ok") return emptyList()
    val arr = (obj["data"] as? kotlinx.serialization.json.JsonArray) ?: return emptyList()
    return arr
      .mapNotNull { el ->
        val o = el as? JsonObject ?: return@mapNotNull null
        val aqi = o["aqi"]?.jsonPrimitive?.content?.toIntOrNull() ?: return@mapNotNull null
        val lat = o["lat"]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
        val lng = o["lon"]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
        val station = o["station"]?.jsonObject
        val name = station?.get("name")?.jsonPrimitive?.content ?: "Stasiun ${o["uid"]?.jsonPrimitive?.content}"
        val time =
          station?.get("time")?.jsonPrimitive?.content?.let {
            runCatching { OffsetDateTime.parse(it).toInstant() }.getOrNull()
          }
        PollutionHotspot(
          id = o["uid"]?.jsonPrimitive?.content ?: "$lat,$lng",
          name = name,
          lat = lat,
          lng = lng,
          aqi = aqi,
          level = AqiCalculator.levelFor(aqi),
          updatedAt = time,
        )
      }
      .filter { it.aqi >= minAqi }
      .sortedByDescending { it.aqi }
      .take(limit)
  }
}
