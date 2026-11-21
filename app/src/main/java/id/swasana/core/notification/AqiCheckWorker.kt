package id.swasana.core.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import id.swasana.R
import id.swasana.core.util.levelNameRes
import id.swasana.domain.aqi.displayAqi
import id.swasana.domain.repository.AirQualityRepository
import id.swasana.domain.repository.NotificationRepository
import id.swasana.domain.repository.SettingsRepository
import id.swasana.domain.usecase.AlertService
import java.time.Clock
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

/**
 * Pemeriksaan AQI berkala (60 menit, hemat baterai). Membandingkan AQI dengan ambang profil,
 * lalu mengirim notifikasi lokal. Lokasi yang dipakai adalah lokasi terakhir yang tersimpan di
 * perangkat, sehingga tidak perlu izin lokasi latar belakang.
 */
@HiltWorker
class AqiCheckWorker
@AssistedInject
constructor(
  @Assisted context: Context,
  @Assisted params: WorkerParameters,
  private val settingsRepository: SettingsRepository,
  private val airQuality: AirQualityRepository,
  private val notifications: NotificationRepository,
  private val helper: NotificationHelper,
  private val alertService: AlertService,
  private val clock: Clock,
) : CoroutineWorker(context, params) {

  override suspend fun doWork(): Result {
    val settings = settingsRepository.settings.first()
    if (!settings.alertsEnabled) return Result.success()
    val point =
      (if (settings.useManualLocation) settings.manualLocation else null)
        ?: settings.lastLocation
        ?: return Result.success()

    val reading =
      airQuality.refresh(point, force = true).getOrElse {
        return if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.success()
      }

    val display = reading.displayAqi(settings.aqiScale)
    val evaluation = alertService.evaluate(display, settings, LocalTime.now(clock))
    if (evaluation.newLastNotifiedLevel != settings.lastNotifiedLevel) {
      settingsRepository.update { it.copy(lastNotifiedLevel = evaluation.newLastNotifiedLevel) }
    }
    if (evaluation.shouldNotify) {
      val ctx = applicationContext
      val title = ctx.getString(R.string.notif_air_alert_title, display.value)
      val text = ctx.getString(R.string.notif_air_alert_text, ctx.getString(levelNameRes(display.level)))
      helper.showAirAlert(title, text)
      notifications.add("$title. $text", clock.instant())
    }
    return Result.success()
  }

  companion object {
    private const val WORK_NAME = "aqi_check"
    private const val MAX_ATTEMPTS = 3

    fun schedule(context: Context) {
      val request =
        PeriodicWorkRequestBuilder<AqiCheckWorker>(60, TimeUnit.MINUTES)
          .setConstraints(
            Constraints.Builder()
              .setRequiredNetworkType(NetworkType.CONNECTED)
              .setRequiresBatteryNotLow(true)
              .build()
          )
          .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
          .build()
      WorkManager.getInstance(context)
        .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun cancel(context: Context) {
      WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
  }
}
