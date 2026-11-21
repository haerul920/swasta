package id.swasana.core.util

import androidx.annotation.StringRes
import id.swasana.R
import id.swasana.domain.model.AirQualityLevel
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

val IndonesianLocale: Locale = Locale.forLanguageTag("id-ID")

object Formatters {
  private val timeFormat = DateTimeFormatter.ofPattern("HH.mm", IndonesianLocale)
  private val longDate = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", IndonesianLocale)
  private val shortDate = DateTimeFormatter.ofPattern("d MMM yyyy", IndonesianLocale)
  private val dayMonth = DateTimeFormatter.ofPattern("d MMM", IndonesianLocale)

  /** Contoh: Rp1.250.000 */
  fun rupiah(amount: Long): String =
    "Rp" + NumberFormat.getIntegerInstance(IndonesianLocale).format(amount)

  /** Contoh: Rp35rb untuk pilihan harga cepat. */
  fun rupiahShort(amount: Long): String =
    if (amount >= 1000 && amount % 1000 == 0L) "Rp${amount / 1000}rb" else rupiah(amount)

  fun number(value: Long): String = NumberFormat.getIntegerInstance(IndonesianLocale).format(value)

  fun decimal(value: Double, digits: Int = 1): String =
    NumberFormat.getNumberInstance(IndonesianLocale)
      .apply {
        maximumFractionDigits = digits
        minimumFractionDigits = 0
      }
      .format(value)

  fun time(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
    timeFormat.format(instant.atZone(zone))

  fun minutesOfDay(minute: Int): String = "%02d.%02d".format(minute / 60, minute % 60)

  fun longDate(date: LocalDate): String = longDate.format(date)

  fun shortDate(date: LocalDate): String = shortDate.format(date)

  fun shortDate(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
    shortDate.format(instant.atZone(zone))

  fun dayMonth(date: LocalDate): String = dayMonth.format(date)

  /** Jarak ramah-baca: 450 m atau 1,2 km. */
  fun distance(meters: Double): String =
    if (meters < 1000) "${(Math.round(meters / 10.0) * 10).toInt()} m"
    else "${decimal(meters / 1000.0)} km"
}

@StringRes
fun levelNameRes(level: AirQualityLevel): Int =
  when (level) {
    AirQualityLevel.GOOD -> R.string.aqi_level_good
    AirQualityLevel.MODERATE -> R.string.aqi_level_moderate
    AirQualityLevel.UNHEALTHY_SENSITIVE -> R.string.aqi_level_unhealthy_sensitive
    AirQualityLevel.UNHEALTHY -> R.string.aqi_level_unhealthy
    AirQualityLevel.VERY_UNHEALTHY -> R.string.aqi_level_very_unhealthy
    AirQualityLevel.HAZARDOUS -> R.string.aqi_level_hazardous
  }

@StringRes
fun levelAdviceRes(level: AirQualityLevel): Int =
  when (level) {
    AirQualityLevel.GOOD -> R.string.aqi_advice_good
    AirQualityLevel.MODERATE -> R.string.aqi_advice_moderate
    AirQualityLevel.UNHEALTHY_SENSITIVE -> R.string.aqi_advice_unhealthy_sensitive
    AirQualityLevel.UNHEALTHY -> R.string.aqi_advice_unhealthy
    AirQualityLevel.VERY_UNHEALTHY -> R.string.aqi_advice_very_unhealthy
    AirQualityLevel.HAZARDOUS -> R.string.aqi_advice_hazardous
  }
