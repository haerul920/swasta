package id.swasana.domain.aqi

import id.swasana.domain.model.AirQualityLevel
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Perhitungan US AQI dari konsentrasi polutan memakai breakpoint EPA.
 * Breakpoint PM2.5 mengikuti revisi EPA yang berlaku sejak Mei 2024.
 */
object AqiCalculator {

  private data class Breakpoint(val cLow: Double, val cHigh: Double, val iLow: Int, val iHigh: Int)

  // PM2.5 (µg/m³, rata-rata 24 jam), EPA Mei 2024.
  private val pm25Table =
    listOf(
      Breakpoint(0.0, 9.0, 0, 50),
      Breakpoint(9.1, 35.4, 51, 100),
      Breakpoint(35.5, 55.4, 101, 150),
      Breakpoint(55.5, 125.4, 151, 200),
      Breakpoint(125.5, 225.4, 201, 300),
      Breakpoint(225.5, 325.4, 301, 400),
      Breakpoint(325.5, 500.4, 401, 500),
    )

  // PM10 (µg/m³, rata-rata 24 jam).
  private val pm10Table =
    listOf(
      Breakpoint(0.0, 54.0, 0, 50),
      Breakpoint(55.0, 154.0, 51, 100),
      Breakpoint(155.0, 254.0, 101, 150),
      Breakpoint(255.0, 354.0, 151, 200),
      Breakpoint(355.0, 424.0, 201, 300),
      Breakpoint(425.0, 504.0, 301, 400),
      Breakpoint(505.0, 604.0, 401, 500),
    )

  // O3 (ppb, rata-rata 8 jam). Di atas 200 ppb EPA memakai rata-rata 1 jam; di sini dibatasi.
  private val o3Table =
    listOf(
      Breakpoint(0.0, 54.0, 0, 50),
      Breakpoint(55.0, 70.0, 51, 100),
      Breakpoint(71.0, 85.0, 101, 150),
      Breakpoint(86.0, 105.0, 151, 200),
      Breakpoint(106.0, 200.0, 201, 300),
    )

  /** Faktor konversi O3: 1 ppb ≈ 1,96 µg/m³ pada 25 °C dan 1 atm. */
  private const val O3_UGM3_PER_PPB = 1.96

  const val MAX_AQI = 500

  /** US AQI dari konsentrasi PM2.5 (µg/m³). Nilai dipotong 1 desimal sesuai aturan EPA. */
  fun aqiFromPm25(concentration: Double): Int =
    interpolate(pm25Table, floor(concentration * 10) / 10)

  fun aqiFromPm10(concentration: Double): Int = interpolate(pm10Table, floor(concentration))

  /** Sub-indeks O3 dari konsentrasi dalam µg/m³ (dikonversi ke ppb). */
  fun aqiFromO3(microgramsPerM3: Double): Int =
    interpolate(o3Table, floor(microgramsPerM3 / O3_UGM3_PER_PPB))

  /** Kebalikan [aqiFromPm25]: perkiraan konsentrasi dari sub-indeks (dipakai untuk data WAQI). */
  fun pm25FromAqi(aqi: Int): Double = invert(pm25Table, aqi)

  fun pm10FromAqi(aqi: Int): Double = invert(pm10Table, aqi)

  fun o3FromAqi(aqi: Int): Double = invert(o3Table, aqi) * O3_UGM3_PER_PPB

  /** Kategori US AQI. */
  fun levelFor(aqi: Int): AirQualityLevel =
    when {
      aqi <= 50 -> AirQualityLevel.GOOD
      aqi <= 100 -> AirQualityLevel.MODERATE
      aqi <= 150 -> AirQualityLevel.UNHEALTHY_SENSITIVE
      aqi <= 200 -> AirQualityLevel.UNHEALTHY
      aqi <= 300 -> AirQualityLevel.VERY_UNHEALTHY
      else -> AirQualityLevel.HAZARDOUS
    }

  /** Kategori berdasarkan konsentrasi PM2.5 langsung (rentang µg/m³ dari EPA 2024). */
  fun levelForPm25(concentration: Double): AirQualityLevel = levelFor(aqiFromPm25(concentration))

  private fun interpolate(table: List<Breakpoint>, c: Double): Int {
    if (c.isNaN() || c < 0) return 0
    val last = table.last()
    if (c >= last.cHigh) return last.iHigh.coerceAtMost(MAX_AQI)
    val bp = table.firstOrNull { c >= it.cLow && c <= it.cHigh }
      // Celah antar breakpoint akibat pembulatan: pakai breakpoint berikutnya.
      ?: table.first { c < it.cLow }
    val value =
      (bp.iHigh - bp.iLow).toDouble() / (bp.cHigh - bp.cLow) * (c - bp.cLow) + bp.iLow
    return value.roundToInt().coerceIn(0, MAX_AQI)
  }

  private fun invert(table: List<Breakpoint>, aqi: Int): Double {
    if (aqi <= 0) return 0.0
    val bp = table.firstOrNull { aqi in it.iLow..it.iHigh } ?: return table.last().cHigh
    val c = (aqi - bp.iLow).toDouble() * (bp.cHigh - bp.cLow) / (bp.iHigh - bp.iLow) + bp.cLow
    return (c * 10).roundToInt() / 10.0
  }
}
