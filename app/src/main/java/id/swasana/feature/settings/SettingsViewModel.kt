package id.swasana.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.swasana.domain.model.AirQualityProviderType
import id.swasana.domain.model.AppSettings
import id.swasana.domain.model.AqiScale
import id.swasana.domain.model.User
import id.swasana.domain.model.UserProfile
import id.swasana.domain.repository.AuthRepository
import id.swasana.domain.repository.CravingRepository
import id.swasana.domain.repository.JournalRepository
import id.swasana.domain.repository.NotificationRepository
import id.swasana.domain.repository.SettingsRepository
import id.swasana.domain.usecase.SmokeZoneService
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
  val settings: AppSettings = AppSettings(),
  val user: User? = null,
  val busy: Boolean = false,
  val remoteBackend: Boolean = false,
)

@HiltViewModel
class SettingsViewModel
@Inject
constructor(
  private val settingsRepository: SettingsRepository,
  private val auth: AuthRepository,
  private val journal: JournalRepository,
  private val cravings: CravingRepository,
  private val notifications: NotificationRepository,
  private val smokeService: SmokeZoneService,
) : ViewModel() {

  private val busy = MutableStateFlow(false)

  val state: StateFlow<SettingsUiState> =
    combine(settingsRepository.settings, auth.currentUser, busy) { s, user, b ->
        SettingsUiState(s, user, b, auth.isRemote)
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState(remoteBackend = auth.isRemote))

  /** Minimal satu profil harus aktif. */
  fun toggleProfile(profile: UserProfile) {
    viewModelScope.launch {
      settingsRepository.update { s ->
        val next = if (profile in s.profiles) s.profiles - profile else s.profiles + profile
        if (next.isEmpty()) s else s.copy(profiles = next)
      }
    }
  }

  fun setScale(scale: AqiScale) = update { it.copy(aqiScale = scale) }

  fun setProvider(type: AirQualityProviderType) = update { it.copy(provider = type) }

  fun setAlertsEnabled(enabled: Boolean) = update { it.copy(alertsEnabled = enabled) }

  /** null = ikuti ambang bawaan profil. */
  fun setThreshold(value: Int?) = update { it.copy(alertThresholdOverride = value) }

  fun setQuietHours(startMinute: Int, endMinute: Int) =
    update { it.copy(quietStartMinute = startMinute, quietEndMinute = endMinute) }

  fun useDeviceLocation() = update { it.copy(useManualLocation = false) }

  private fun update(transform: (AppSettings) -> AppSettings) {
    viewModelScope.launch { settingsRepository.update(transform) }
  }

  fun signOut(onDone: () -> Unit) {
    viewModelScope.launch {
      auth.signOut()
      onDone()
    }
  }

  /** Hapus jurnal, catatan craving, riwayat notifikasi, dan data kebiasaan di perangkat ini. */
  fun deleteLocalData(onDone: () -> Unit) {
    viewModelScope.launch {
      busy.value = true
      journal.deleteAll()
      cravings.deleteAll()
      notifications.deleteAll()
      settingsRepository.update { it.copy(habit = null, relapseCount = 0, lastLocation = null, lastNotifiedLevel = -1) }
      busy.value = false
      onDone()
    }
  }

  /** Hapus akun dan semua data: laporan milik pengguna, data lokal, lalu akun itu sendiri. */
  fun deleteAccount(onDone: () -> Unit) {
    viewModelScope.launch {
      busy.value = true
      val user = auth.currentUser.first()
      user?.let { if (!it.isGuest) smokeService.deleteReportsBy(it.id) }
      journal.deleteAll()
      cravings.deleteAll()
      notifications.deleteAll()
      auth.deleteAccount()
      settingsRepository.clearAll()
      busy.value = false
      onDone()
    }
  }
}
