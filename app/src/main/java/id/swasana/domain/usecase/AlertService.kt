package id.swasana.domain.usecase

import id.swasana.domain.model.AppSettings
import id.swasana.domain.model.DisplayAqi
import java.time.LocalTime
import javax.inject.Inject

data class AlertEvaluation(
  val shouldNotify: Boolean,
  /** Nilai baru untuk [AppSettings.lastNotifiedLevel]. */
  val newLastNotifiedLevel: Int,
)

/**
 * Menentukan kapan peringatan dikirim.
 *
 * Aturan anti-spam:
 *  - hanya saat nilai melewati ambang,
 *  - maksimal 1 notifikasi per kenaikan kategori,
 *  - tidak mengirim pada jam tenang (ditunda sampai pemeriksaan berikutnya di luar jam tenang),
 *  - setelah udara kembali di bawah ambang, penghitung direset.
 */
class AlertService @Inject constructor() {

  fun isAboveThreshold(display: DisplayAqi, settings: AppSettings): Boolean =
    display.value > settings.effectiveThreshold

  fun evaluate(display: DisplayAqi, settings: AppSettings, now: LocalTime): AlertEvaluation {
    if (!settings.alertsEnabled) {
      return AlertEvaluation(false, settings.lastNotifiedLevel)
    }
    if (!isAboveThreshold(display, settings)) {
      return AlertEvaluation(false, -1)
    }
    val level = display.level.ordinal
    if (level <= settings.lastNotifiedLevel) {
      return AlertEvaluation(false, settings.lastNotifiedLevel)
    }
    if (isQuietHour(now, settings.quietStartMinute, settings.quietEndMinute)) {
      return AlertEvaluation(false, settings.lastNotifiedLevel)
    }
    return AlertEvaluation(true, level)
  }

  companion object {
    /** Jam tenang bisa melewati tengah malam (mis. 22.00 sampai 06.00). */
    fun isQuietHour(now: LocalTime, startMinute: Int, endMinute: Int): Boolean {
      if (startMinute == endMinute) return false
      val minute = now.hour * 60 + now.minute
      return if (startMinute < endMinute) {
        minute in startMinute until endMinute
      } else {
        minute >= startMinute || minute < endMinute
      }
    }
  }
}
