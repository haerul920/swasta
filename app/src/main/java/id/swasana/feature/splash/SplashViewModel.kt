package id.swasana.feature.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.swasana.domain.model.AppSettings
import id.swasana.domain.repository.AuthRepository
import id.swasana.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

enum class StartDestination {
  Onboarding,
  Auth,
  ProfileSelect,
  Permissions,
  Home,
}

/**
 * Menentukan layar pertama. Bila sudah masuk dan onboarding selesai, langsung ke Beranda;
 * jika tidak, lanjutkan dari langkah yang belum selesai.
 */
@HiltViewModel
class SplashViewModel
@Inject
constructor(settings: SettingsRepository, auth: AuthRepository) : ViewModel() {

  private val _destination = MutableStateFlow<StartDestination?>(null)
  val destination: StateFlow<StartDestination?> = _destination

  init {
    viewModelScope.launch {
      val minShown = launch { delay(MIN_SPLASH_MS) }
      val s = settings.settings.first()
      val user = withTimeoutOrNull(AUTH_TIMEOUT_MS) { auth.currentUser.first() }
      minShown.join()
      _destination.value = decide(s, loggedIn = user != null)
    }
  }

  companion object {
    private const val MIN_SPLASH_MS = 1200L
    private const val AUTH_TIMEOUT_MS = 3000L

    fun decide(s: AppSettings, loggedIn: Boolean): StartDestination =
      when {
        !s.onboardingDone -> StartDestination.Onboarding
        !loggedIn -> StartDestination.Auth
        !s.profileSetupDone -> StartDestination.ProfileSelect
        !s.permissionStepDone -> StartDestination.Permissions
        else -> StartDestination.Home
      }
  }
}
