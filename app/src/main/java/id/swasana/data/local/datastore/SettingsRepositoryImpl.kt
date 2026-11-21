package id.swasana.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import id.swasana.domain.model.AirQualityProviderType
import id.swasana.domain.model.AppSettings
import id.swasana.domain.model.AqiScale
import id.swasana.domain.model.GeoPoint
import id.swasana.domain.model.HabitSettings
import id.swasana.domain.model.LocalSession
import id.swasana.domain.model.ProductType
import id.swasana.domain.model.UserProfile
import id.swasana.domain.model.UserRole
import id.swasana.domain.repository.SettingsRepository
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class SettingsRepositoryImpl @Inject constructor(private val store: DataStore<Preferences>) :
  SettingsRepository {

  override val settings: Flow<AppSettings> = store.data.map { it.toSettings() }

  override suspend fun update(transform: (AppSettings) -> AppSettings) {
    store.edit { prefs ->
      val updated = transform(prefs.toSettings())
      prefs.write(updated)
    }
  }

  override suspend fun clearAll() {
    store.edit { it.clear() }
  }

  private object K {
    val onboardingDone = booleanPreferencesKey("onboarding_done")
    val profileSetupDone = booleanPreferencesKey("profile_setup_done")
    val permissionStepDone = booleanPreferencesKey("permission_step_done")
    val profiles = stringPreferencesKey("profiles")
    val displayName = stringPreferencesKey("display_name")
    val habitProduct = stringPreferencesKey("habit_product")
    val habitDaily = intPreferencesKey("habit_daily")
    val habitPrice = longPreferencesKey("habit_price")
    val habitSticks = intPreferencesKey("habit_sticks")
    val habitQuitAt = longPreferencesKey("habit_quit_at")
    val relapseCount = intPreferencesKey("relapse_count")
    val aqiScale = stringPreferencesKey("aqi_scale")
    val provider = stringPreferencesKey("provider")
    val alertsEnabled = booleanPreferencesKey("alerts_enabled")
    val threshold = intPreferencesKey("alert_threshold")
    val quietStart = intPreferencesKey("quiet_start")
    val quietEnd = intPreferencesKey("quiet_end")
    val useManual = booleanPreferencesKey("use_manual_location")
    val manualLocation = stringPreferencesKey("manual_location")
    val lastLocation = stringPreferencesKey("last_location")
    val lastNotified = intPreferencesKey("last_notified_level")
    val sessionId = stringPreferencesKey("session_id")
    val sessionName = stringPreferencesKey("session_name")
    val sessionEmail = stringPreferencesKey("session_email")
    val sessionRole = stringPreferencesKey("session_role")
    val sessionGuest = booleanPreferencesKey("session_guest")
  }

  private fun Preferences.toSettings(): AppSettings {
    val defaults = AppSettings()
    val habit =
      this[K.habitQuitAt]?.let {
        HabitSettings(
          productType = enumOr(this[K.habitProduct], ProductType.CIGARETTE),
          dailyCount = this[K.habitDaily] ?: 12,
          packPrice = this[K.habitPrice] ?: 35_000,
          sticksPerPack = this[K.habitSticks] ?: 16,
          quitAt = Instant.ofEpochMilli(it),
        )
      }
    val session =
      this[K.sessionId]?.let {
        LocalSession(
          userId = it,
          name = this[K.sessionName].orEmpty(),
          email = this[K.sessionEmail].orEmpty(),
          role = enumOr(this[K.sessionRole], UserRole.USER),
          isGuest = this[K.sessionGuest] ?: false,
        )
      }
    return AppSettings(
      onboardingDone = this[K.onboardingDone] ?: false,
      profileSetupDone = this[K.profileSetupDone] ?: false,
      permissionStepDone = this[K.permissionStepDone] ?: false,
      profiles =
        this[K.profiles]
          .orEmpty()
          .split(",")
          .filter { it.isNotBlank() }
          .mapNotNull { runCatching { UserProfile.valueOf(it) }.getOrNull() }
          .toSet(),
      displayName = this[K.displayName].orEmpty(),
      habit = habit,
      relapseCount = this[K.relapseCount] ?: 0,
      aqiScale = enumOr(this[K.aqiScale], AqiScale.US_AQI),
      provider = enumOr(this[K.provider], AirQualityProviderType.WAQI),
      alertsEnabled = this[K.alertsEnabled] ?: true,
      alertThresholdOverride = this[K.threshold],
      quietStartMinute = this[K.quietStart] ?: defaults.quietStartMinute,
      quietEndMinute = this[K.quietEnd] ?: defaults.quietEndMinute,
      useManualLocation = this[K.useManual] ?: false,
      manualLocation = decodePoint(this[K.manualLocation]),
      lastLocation = decodePoint(this[K.lastLocation]),
      lastNotifiedLevel = this[K.lastNotified] ?: -1,
      session = session,
    )
  }

  private fun androidx.datastore.preferences.core.MutablePreferences.write(s: AppSettings) {
    this[K.onboardingDone] = s.onboardingDone
    this[K.profileSetupDone] = s.profileSetupDone
    this[K.permissionStepDone] = s.permissionStepDone
    this[K.profiles] = s.profiles.joinToString(",") { it.name }
    this[K.displayName] = s.displayName
    val h = s.habit
    if (h == null) {
      remove(K.habitQuitAt)
      remove(K.habitProduct)
      remove(K.habitDaily)
      remove(K.habitPrice)
      remove(K.habitSticks)
    } else {
      this[K.habitProduct] = h.productType.name
      this[K.habitDaily] = h.dailyCount
      this[K.habitPrice] = h.packPrice
      this[K.habitSticks] = h.sticksPerPack
      this[K.habitQuitAt] = h.quitAt.toEpochMilli()
    }
    this[K.relapseCount] = s.relapseCount
    this[K.aqiScale] = s.aqiScale.name
    this[K.provider] = s.provider.name
    this[K.alertsEnabled] = s.alertsEnabled
    s.alertThresholdOverride?.let { this[K.threshold] = it } ?: remove(K.threshold)
    this[K.quietStart] = s.quietStartMinute
    this[K.quietEnd] = s.quietEndMinute
    this[K.useManual] = s.useManualLocation
    s.manualLocation?.let { this[K.manualLocation] = encodePoint(it) } ?: remove(K.manualLocation)
    s.lastLocation?.let { this[K.lastLocation] = encodePoint(it) } ?: remove(K.lastLocation)
    this[K.lastNotified] = s.lastNotifiedLevel
    val sess = s.session
    if (sess == null) {
      remove(K.sessionId)
      remove(K.sessionName)
      remove(K.sessionEmail)
      remove(K.sessionRole)
      remove(K.sessionGuest)
    } else {
      this[K.sessionId] = sess.userId
      this[K.sessionName] = sess.name
      this[K.sessionEmail] = sess.email
      this[K.sessionRole] = sess.role.name
      this[K.sessionGuest] = sess.isGuest
    }
  }

  private inline fun <reified E : Enum<E>> enumOr(value: String?, default: E): E =
    value?.let { v -> runCatching { enumValueOf<E>(v) }.getOrNull() } ?: default

  private fun encodePoint(p: GeoPoint) = "${p.lat}|${p.lng}|${p.name.replace("|", " ")}"

  private fun decodePoint(raw: String?): GeoPoint? {
    if (raw.isNullOrBlank()) return null
    val parts = raw.split("|", limit = 3)
    if (parts.size < 3) return null
    val lat = parts[0].toDoubleOrNull() ?: return null
    val lng = parts[1].toDoubleOrNull() ?: return null
    return GeoPoint(lat, lng, parts[2])
  }
}
