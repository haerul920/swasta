package id.swasana

import id.swasana.domain.aqi.AqiCalculator
import id.swasana.domain.aqi.IspuCalculator
import id.swasana.domain.model.AirQualityLevel
import id.swasana.domain.model.AppSettings
import id.swasana.domain.model.AqiScale
import id.swasana.domain.model.DisplayAqi
import id.swasana.domain.model.MilestoneState
import id.swasana.domain.model.ProductType
import id.swasana.domain.model.QuitProgress
import id.swasana.domain.model.RecoveryMilestone
import id.swasana.domain.model.UserProfile
import id.swasana.domain.usecase.ActivityStatus
import id.swasana.domain.usecase.ActivityType
import id.swasana.domain.usecase.AlertService
import id.swasana.domain.usecase.RecommendationService
import id.swasana.domain.usecase.StatsUtil
import java.time.Instant
import java.time.LocalTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AqiCalculatorTest {
  @Test
  fun batasKategoriPm25Epa2024() {
    assertEquals(0, AqiCalculator.aqiFromPm25(0.0))
    assertEquals(50, AqiCalculator.aqiFromPm25(9.0))
    assertEquals(51, AqiCalculator.aqiFromPm25(9.1))
    assertEquals(100, AqiCalculator.aqiFromPm25(35.4))
    assertEquals(101, AqiCalculator.aqiFromPm25(35.5))
    assertEquals(150, AqiCalculator.aqiFromPm25(55.4))
    assertEquals(151, AqiCalculator.aqiFromPm25(55.5))
    assertEquals(200, AqiCalculator.aqiFromPm25(125.4))
    assertEquals(201, AqiCalculator.aqiFromPm25(125.5))
    assertEquals(301, AqiCalculator.aqiFromPm25(225.5))
  }

  @Test
  fun interpolasiLinear() {
    // 22.25 dipotong 22.2; (49/26.3)*(22.2-9.1)+51 = 75.4
    assertEquals(75, AqiCalculator.aqiFromPm25(22.25))
  }

  @Test
  fun kategoriUsAqi() {
    assertEquals(AirQualityLevel.GOOD, AqiCalculator.levelFor(50))
    assertEquals(AirQualityLevel.MODERATE, AqiCalculator.levelFor(100))
    assertEquals(AirQualityLevel.UNHEALTHY_SENSITIVE, AqiCalculator.levelFor(150))
    assertEquals(AirQualityLevel.UNHEALTHY, AqiCalculator.levelFor(200))
    assertEquals(AirQualityLevel.VERY_UNHEALTHY, AqiCalculator.levelFor(300))
    assertEquals(AirQualityLevel.HAZARDOUS, AqiCalculator.levelFor(301))
  }

  @Test
  fun kebalikanKonsentrasiKembaliKeIndeks() {
    for (aqi in listOf(25, 75, 128, 180, 250)) {
      val back = AqiCalculator.aqiFromPm25(AqiCalculator.pm25FromAqi(aqi))
      assertEquals(aqi.toDouble(), back.toDouble(), 1.0)
    }
  }

  @Test
  fun ispu() {
    assertEquals(AirQualityLevel.GOOD, IspuCalculator.levelFor(50))
    assertEquals(AirQualityLevel.UNHEALTHY, IspuCalculator.levelFor(101))
    assertEquals(AirQualityLevel.UNHEALTHY, IspuCalculator.levelFor(200))
    assertEquals(AirQualityLevel.VERY_UNHEALTHY, IspuCalculator.levelFor(201))
    assertEquals(AirQualityLevel.HAZARDOUS, IspuCalculator.levelFor(301))
    assertEquals(50, IspuCalculator.fromConcentrations(15.5, null, null))
    assertEquals(100, IspuCalculator.fromConcentrations(55.4, null, null))
    assertNull(IspuCalculator.fromConcentrations(null, null, null))
  }
}

class QuitProgressTest {
  private val start = Instant.parse("2026-10-29T00:00:00Z")
  private val progress = QuitProgress("u", start, ProductType.CIGARETTE, 12, 35_000, 16)

  @Test
  fun hariDanUangHemat() {
    val now = start.plusSeconds(23 * 24 * 3600L + 4 * 3600)
    assertEquals(23, progress.smokeFreeDays(now))
    assertEquals(23L * 12, progress.cigarettesAvoided(now))
    assertEquals(Math.round(23 * 12 * 35_000 / 16.0), progress.moneySaved(now))
  }

  @Test
  fun tahapPemulihan() {
    val now = start.plusSeconds(23 * 24 * 3600L)
    val s = progress.statuses(now)
    assertEquals(MilestoneState.REACHED, s.first { it.milestone == RecoveryMilestone.WEEKS_2_12 }.state)
    assertEquals(MilestoneState.IN_PROGRESS, s.first { it.milestone == RecoveryMilestone.MONTHS_1_9 }.state)
    assertEquals(MilestoneState.UPCOMING, s.first { it.milestone == RecoveryMilestone.YEAR_1 }.state)
    assertEquals(RecoveryMilestone.MONTHS_1_9, progress.nextMilestone(now)!!.milestone)
  }

  @Test
  fun waktuSebelumMulaiTidakNegatif() {
    assertEquals(0, progress.smokeFreeDays(start.minusSeconds(100)))
  }
}

class StatsTest {
  @Test
  fun pearson() {
    assertEquals(1.0, StatsUtil.pearson(listOf(1.0, 2.0, 3.0), listOf(2.0, 4.0, 6.0))!!, 1e-9)
    assertEquals(-1.0, StatsUtil.pearson(listOf(1.0, 2.0, 3.0), listOf(6.0, 4.0, 2.0))!!, 1e-9)
    assertNull(StatsUtil.pearson(listOf(1.0, 1.0, 1.0), listOf(1.0, 2.0, 3.0)))
  }
}

class AlertServiceTest {
  private val service = AlertService()
  private val settings = AppSettings(profiles = setOf(UserProfile.NAPAS))
  private val noon = LocalTime.NOON

  private fun aqi(v: Int, l: AirQualityLevel) = DisplayAqi(v, l, AqiScale.US_AQI)

  @Test
  fun ambangBawaanSesuaiProfil() {
    assertEquals(100, settings.effectiveThreshold)
    assertEquals(150, AppSettings(profiles = setOf(UserProfile.UMUM)).effectiveThreshold)
  }

  @Test
  fun notifikasiHanyaSaatNaikKategori() {
    val first = service.evaluate(aqi(128, AirQualityLevel.UNHEALTHY_SENSITIVE), settings, noon)
    assertTrue(first.shouldNotify)
    val after = settings.copy(lastNotifiedLevel = first.newLastNotifiedLevel)
    assertFalse(service.evaluate(aqi(130, AirQualityLevel.UNHEALTHY_SENSITIVE), after, noon).shouldNotify)
    assertTrue(service.evaluate(aqi(170, AirQualityLevel.UNHEALTHY), after, noon).shouldNotify)
  }

  @Test
  fun resetSaatUdaraMembaik() {
    val r = service.evaluate(aqi(60, AirQualityLevel.MODERATE), settings.copy(lastNotifiedLevel = 2), noon)
    assertFalse(r.shouldNotify)
    assertEquals(-1, r.newLastNotifiedLevel)
  }

  @Test
  fun jamTenangMelewatiTengahMalam() {
    assertTrue(AlertService.isQuietHour(LocalTime.of(23, 0), 22 * 60, 6 * 60))
    assertTrue(AlertService.isQuietHour(LocalTime.of(5, 59), 22 * 60, 6 * 60))
    assertFalse(AlertService.isQuietHour(LocalTime.of(6, 0), 22 * 60, 6 * 60))
    val r = service.evaluate(aqi(128, AirQualityLevel.UNHEALTHY_SENSITIVE), settings, LocalTime.of(23, 0))
    assertFalse(r.shouldNotify)
  }

  @Test
  fun notifikasiDimatikan() {
    val off = settings.copy(alertsEnabled = false)
    assertFalse(service.evaluate(aqi(200, AirQualityLevel.UNHEALTHY), off, noon).shouldNotify)
  }
}

class RecommendationServiceTest {
  private val service = RecommendationService()
  private val display = DisplayAqi(128, AirQualityLevel.UNHEALTHY_SENSITIVE, AqiScale.US_AQI)

  @Test
  fun profilRentanLebihKetat() {
    val vulnerable = service.activities(display, setOf(UserProfile.NAPAS))
    assertEquals(ActivityStatus.POSTPONE, vulnerable.first { it.activity == ActivityType.OUTDOOR_EXERCISE }.status)
    assertEquals(ActivityStatus.WEAR_MASK, vulnerable.first { it.activity == ActivityType.WALK }.status)
    val general = service.activities(display, setOf(UserProfile.UMUM))
    assertEquals(ActivityStatus.WEAR_MASK, general.first { it.activity == ActivityType.OUTDOOR_EXERCISE }.status)
  }

  @Test
  fun saranMengikutiProfil() {
    val recs = service.recommendations(display, setOf(UserProfile.PEROKOK, UserProfile.NAPAS), 100)
    assertEquals(3, recs.size)
    assertNotNull(recs.firstOrNull { it.tag == "I" })
  }
}
