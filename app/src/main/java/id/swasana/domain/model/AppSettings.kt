package id.swasana.domain.model

import java.time.Instant

/** Data kebiasaan merokok/vape yang diisi pengguna profil PEROKOK. */
data class HabitSettings(
  val productType: ProductType = ProductType.CIGARETTE,
  val dailyCount: Int = 12,
  val packPrice: Long = 35_000,
  val sticksPerPack: Int = 16,
  val quitAt: Instant = Instant.now(),
)

/** Sesi lokal. Dipakai untuk mode tamu dan akun palsu (FakeAuthRepository). */
data class LocalSession(
  val userId: String,
  val name: String,
  val email: String,
  val role: UserRole,
  val isGuest: Boolean,
)

data class AppSettings(
  val onboardingDone: Boolean = false,
  val profileSetupDone: Boolean = false,
  val permissionStepDone: Boolean = false,
  val profiles: Set<UserProfile> = emptySet(),
  val displayName: String = "",
  val habit: HabitSettings? = null,
  val relapseCount: Int = 0,
  val aqiScale: AqiScale = AqiScale.US_AQI,
  val provider: AirQualityProviderType = AirQualityProviderType.WAQI,
  val alertsEnabled: Boolean = true,
  /** Ambang buatan pengguna; null = pakai ambang bawaan dari profil. */
  val alertThresholdOverride: Int? = null,
  /** Jam tenang, dalam menit sejak 00.00. */
  val quietStartMinute: Int = 22 * 60,
  val quietEndMinute: Int = 6 * 60,
  val useManualLocation: Boolean = false,
  val manualLocation: GeoPoint? = null,
  /** Lokasi terakhir yang diketahui. Hanya disimpan di perangkat. */
  val lastLocation: GeoPoint? = null,
  /** Tingkat kategori terakhir yang sudah diberi notifikasi (-1 = belum ada). */
  val lastNotifiedLevel: Int = -1,
  val session: LocalSession? = null,
) {
  /** Ambang peringatan efektif (dalam skala yang ditampilkan). */
  val effectiveThreshold: Int
    get() = alertThresholdOverride ?: defaultThresholdFor(profiles)

  companion object {
    const val DEFAULT_THRESHOLD_SENSITIVE = 100
    const val DEFAULT_THRESHOLD_GENERAL = 150

    /** AQI > 100 untuk NAPAS, PEROKOK, PENGASUH; AQI > 150 untuk UMUM. */
    fun defaultThresholdFor(profiles: Set<UserProfile>): Int =
      if (
        profiles.any {
          it == UserProfile.NAPAS || it == UserProfile.PEROKOK || it == UserProfile.PENGASUH
        }
      ) {
        DEFAULT_THRESHOLD_SENSITIVE
      } else {
        DEFAULT_THRESHOLD_GENERAL
      }
  }
}
