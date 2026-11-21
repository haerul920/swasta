package id.swasana.domain.aqi

import id.swasana.domain.model.AirQualityLevel
import id.swasana.domain.model.AirQualityReading
import id.swasana.domain.model.AqiScale
import id.swasana.domain.model.DisplayAqi
import kotlin.math.roundToInt

/**
 * Indeks Standar Pencemar Udara (ISPU) menurut Permen LHK No. 14 Tahun 2020.
 *
 * Kategori: Baik 0–50, Sedang 51–100, Tidak sehat 101–200, Sangat tidak sehat 201–300,
 * Berbahaya > 300. ISPU dihitung per parameter dan diambil nilai tertinggi.
 */
object IspuCalculator {

  private data class Segment(val cLow: Double, val cHigh: Double, val iLow: Int, val iHigh: Int)

  // PM2.5 (µg/m³, 24 jam)
  private val pm25 =
    listOf(
      Segment(0.0, 15.5, 0, 50),
      Segment(15.6, 55.4, 51, 100),
      Segment(55.5, 150.4, 101, 200),
      Segment(150.5, 250.4, 201, 300),
      Segment(250.5, 500.0, 301, 500),
    )

  // PM10 (µg/m³, 24 jam)
  private val pm10 =
    listOf(
      Segment(0.0, 50.0, 0, 50),
      Segment(51.0, 150.0, 51, 100),
      Segment(151.0, 350.0, 101, 200),
      Segment(351.0, 420.0, 201, 300),
      Segment(421.0, 500.0, 301, 500),
    )

  // O3 (µg/m³, 8 jam)
  private val o3 =
    listOf(
      Segment(0.0, 120.0, 0, 50),
      Segment(121.0, 235.0, 51, 100),
      Segment(236.0, 400.0, 101, 200),
      Segment(401.0, 800.0, 201, 300),
      Segment(801.0, 1000.0, 301, 500),
    )

  fun levelFor(ispu: Int): AirQualityLevel =
    when {
      ispu <= 50 -> AirQualityLevel.GOOD
      ispu <= 100 -> AirQualityLevel.MODERATE
      ispu <= 200 -> AirQualityLevel.UNHEALTHY
      ispu <= 300 -> AirQualityLevel.VERY_UNHEALTHY
      else -> AirQualityLevel.HAZARDOUS
    }

  /** ISPU = nilai sub-indeks tertinggi dari parameter yang tersedia; null bila tidak ada data. */
  fun fromConcentrations(pm25Value: Double?, pm10Value: Double?, o3Value: Double?): Int? {
    val parts =
      listOfNotNull(
        pm25Value?.let { interpolate(pm25, it) },
        pm10Value?.let { interpolate(pm10, it) },
        o3Value?.let { interpolate(o3, it) },
      )
    return parts.maxOrNull()
  }

  /** ISPU sebuah pembacaan; bila konsentrasi kosong, dikonversi mendekati dari US AQI. */
  fun fromReading(reading: AirQualityReading): Int =
    fromConcentrations(reading.pm25, reading.pm10, reading.o3) ?: approximateFromUsAqi(reading.aqi)

  private fun approximateFromUsAqi(usAqi: Int): Int =
    interpolate(pm25, AqiCalculator.pm25FromAqi(usAqi))

  private fun interpolate(table: List<Segment>, c: Double): Int {
    if (c.isNaN() || c <= 0) return 0
    val last = table.last()
    if (c >= last.cHigh) return last.iHigh
    val seg = table.firstOrNull { c >= it.cLow && c <= it.cHigh } ?: table.first { c < it.cLow }
    val v = (seg.iHigh - seg.iLow).toDouble() / (seg.cHigh - seg.cLow) * (c - seg.cLow) + seg.iLow
    return v.roundToInt()
  }
}

/** Menghasilkan nilai dan kategori sesuai skala pilihan pengguna. */
fun AirQualityReading.displayAqi(scale: AqiScale): DisplayAqi =
  when (scale) {
    AqiScale.US_AQI -> DisplayAqi(aqi, level, scale)
    AqiScale.ISPU -> {
      val ispu = IspuCalculator.fromReading(this)
      DisplayAqi(ispu, IspuCalculator.levelFor(ispu), scale)
    }
  }

fun displayAqiOf(usAqi: Int, scale: AqiScale): DisplayAqi =
  when (scale) {
    AqiScale.US_AQI -> DisplayAqi(usAqi, AqiCalculator.levelFor(usAqi), scale)
    AqiScale.ISPU -> {
      val ispu = IspuCalculator.fromConcentrations(AqiCalculator.pm25FromAqi(usAqi), null, null) ?: 0
      DisplayAqi(ispu, IspuCalculator.levelFor(ispu), scale)
    }
  }
