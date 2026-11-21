package id.swasana.data.repository

import id.swasana.domain.model.LocalSession
import id.swasana.domain.model.User
import id.swasana.domain.model.UserRole
import id.swasana.domain.repository.AuthRepository
import id.swasana.domain.repository.SettingsRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Kesalahan autentikasi dengan pesan yang aman ditampilkan ke pengguna. */
class AuthException(val reason: Reason, message: String? = null) : Exception(message) {
  enum class Reason {
    INVALID_EMAIL,
    WEAK_PASSWORD,
    WRONG_CREDENTIALS,
    EMAIL_IN_USE,
    NOT_CONFIGURED,
    CANCELLED,
    NETWORK,
    UNKNOWN,
  }
}

fun isValidEmail(email: String): Boolean = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(email.trim())

/**
 * Autentikasi lokal untuk pengembangan dan mode tamu. Tidak ada server: kredensial apa pun yang
 * valid diterima. Alamat admin@swasana.id mendapat peran ADMIN agar alur admin bisa dicoba.
 */
@Singleton
class FakeAuthRepository @Inject constructor(private val settings: SettingsRepository) :
  AuthRepository {

  override val isRemote = false

  override val currentUser: Flow<User?> =
    settings.settings.map { s ->
      s.session?.let { sess ->
        User(sess.userId, sess.name, sess.email, sess.role, s.profiles, sess.isGuest)
      }
    }

  override suspend fun signInWithEmail(email: String, password: String): Result<User> {
    if (!isValidEmail(email)) return Result.failure(AuthException(AuthException.Reason.INVALID_EMAIL))
    if (password.length < MIN_PASSWORD) {
      return Result.failure(AuthException(AuthException.Reason.WEAK_PASSWORD))
    }
    return Result.success(startSession(nameFromEmail(email), email.trim().lowercase()))
  }

  override suspend fun signUpWithEmail(name: String, email: String, password: String): Result<User> {
    if (!isValidEmail(email)) return Result.failure(AuthException(AuthException.Reason.INVALID_EMAIL))
    if (password.length < MIN_PASSWORD) {
      return Result.failure(AuthException(AuthException.Reason.WEAK_PASSWORD))
    }
    return Result.success(
      startSession(name.ifBlank { nameFromEmail(email) }, email.trim().lowercase())
    )
  }

  override suspend fun signInWithGoogle(idToken: String?): Result<User> =
    Result.success(startSession("Pengguna Google", "pengguna.google@contoh.id"))

  override suspend fun continueAsGuest(): User {
    val session = LocalSession("guest", "Tamu", "", UserRole.USER, isGuest = true)
    settings.update { it.copy(session = session, displayName = it.displayName.ifBlank { "Tamu" }) }
    return User(session.userId, session.name, session.email, session.role, emptySet(), true)
  }

  override suspend fun signOut() {
    settings.update { it.copy(session = null) }
  }

  override suspend fun deleteAccount(): Result<Unit> = runCatching { settings.clearAll() }

  private suspend fun startSession(name: String, email: String): User {
    val role = if (email == ADMIN_EMAIL) UserRole.ADMIN else UserRole.USER
    val id = UUID.nameUUIDFromBytes(email.toByteArray()).toString()
    val session = LocalSession(id, name, email, role, isGuest = false)
    settings.update { it.copy(session = session, displayName = name) }
    return User(id, name, email, role, emptySet())
  }

  private fun nameFromEmail(email: String): String =
    email.substringBefore("@").replace('.', ' ').replace('_', ' ').split(" ").joinToString(" ") {
      it.replaceFirstChar { c -> c.uppercase() }
    }

  companion object {
    const val MIN_PASSWORD = 8
    const val ADMIN_EMAIL = "admin@swasana.id"
  }
}
