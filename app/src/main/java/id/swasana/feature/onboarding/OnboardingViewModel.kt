package id.swasana.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.swasana.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class OnboardingViewModel @Inject constructor(private val settings: SettingsRepository) :
  ViewModel() {

  /** Tandai onboarding selesai, lalu panggil [then] (navigasi). */
  fun finish(then: () -> Unit) {
    viewModelScope.launch {
      settings.update { it.copy(onboardingDone = true) }
      then()
    }
  }
}
