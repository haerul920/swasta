package id.swasana.feature.auth

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.swasana.R
import id.swasana.data.repository.AuthException
import id.swasana.domain.model.User
import id.swasana.domain.repository.AuthRepository
import id.swasana.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
  val registerMode: Boolean = false,
  val name: String = "",
  val email: String = "",
  val password: String = "",
  val loading: Boolean = false,
  @StringRes val errorRes: Int? = null,
  val remoteBackend: Boolean = false,
)

sealed interface AuthEvent {
  /** [profileDone] = true bila pengguna sudah pernah memilih profil. */
  data class Authenticated(val profileDone: Boolean) : AuthEvent
}

@HiltViewModel
class AuthViewModel
@Inject
constructor(private val auth: AuthRepository, private val settings: SettingsRepository) :
  ViewModel() {

  private val _state = MutableStateFlow(AuthUiState(remoteBackend = auth.isRemote))
  val state: StateFlow<AuthUiState> = _state.asStateFlow()

  private val _events = Channel<AuthEvent>(Channel.BUFFERED)
  val events = _events.receiveAsFlow()

  fun onName(v: String) = _state.update { it.copy(name = v, errorRes = null) }

  fun onEmail(v: String) = _state.update { it.copy(email = v, errorRes = null) }

  fun onPassword(v: String) = _state.update { it.copy(password = v, errorRes = null) }

  fun toggleMode() = _state.update { it.copy(registerMode = !it.registerMode, errorRes = null) }

  fun submit() {
    val s = _state.value
    if (s.loading) return
    launchAuth {
      if (s.registerMode) auth.signUpWithEmail(s.name, s.email, s.password)
      else auth.signInWithEmail(s.email, s.password)
    }
  }

  fun signInWithGoogle(idToken: String?) = launchAuth { auth.signInWithGoogle(idToken) }

  fun googleUnavailable() = _state.update { it.copy(errorRes = R.string.auth_error_google_not_configured) }

  fun googleFailed() = _state.update { it.copy(errorRes = R.string.auth_error_unknown) }

  fun continueAsGuest() {
    if (_state.value.loading) return
    viewModelScope.launch {
      _state.update { it.copy(loading = true) }
      auth.continueAsGuest()
      finish()
    }
  }

  private fun launchAuth(block: suspend () -> Result<User>) {
    viewModelScope.launch {
      _state.update { it.copy(loading = true, errorRes = null) }
      block()
        .onSuccess { finish() }
        .onFailure { e ->
          _state.update { it.copy(loading = false, errorRes = errorFor(e)) }
        }
    }
  }

  private suspend fun finish() {
    val done = settings.settings.first().profileSetupDone
    _state.update { it.copy(loading = false) }
    _events.send(AuthEvent.Authenticated(done))
  }

  private fun errorFor(e: Throwable): Int =
    when ((e as? AuthException)?.reason) {
      AuthException.Reason.INVALID_EMAIL -> R.string.auth_error_invalid_email
      AuthException.Reason.WEAK_PASSWORD -> R.string.auth_error_weak_password
      AuthException.Reason.WRONG_CREDENTIALS -> R.string.auth_error_wrong_credentials
      AuthException.Reason.EMAIL_IN_USE -> R.string.auth_error_email_in_use
      AuthException.Reason.NOT_CONFIGURED -> R.string.auth_error_google_not_configured
      AuthException.Reason.CANCELLED -> R.string.auth_error_cancelled
      AuthException.Reason.NETWORK -> R.string.auth_error_network
      else -> R.string.auth_error_unknown
    }
}
