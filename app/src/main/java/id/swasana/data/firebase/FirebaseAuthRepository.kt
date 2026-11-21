package id.swasana.data.firebase

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import id.swasana.data.repository.AuthException
import id.swasana.data.repository.AuthException.Reason
import id.swasana.data.repository.isValidEmail
import id.swasana.domain.model.AppSettings
import id.swasana.domain.model.LocalSession
import id.swasana.domain.model.User
import id.swasana.domain.model.UserRole
import id.swasana.domain.repository.AuthRepository
import id.swasana.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.tasks.await

/**
 * Autentikasi Firebase (email dan Google). Peran (USER/ADMIN) dibaca dari dokumen
 * `users/{uid}` di Firestore. Mode tamu tetap lokal dan tidak menyentuh Firebase.
 */
@Singleton
class FirebaseAuthRepository
@Inject
constructor(
  private val auth: FirebaseAuth,
  private val db: FirebaseFirestore,
  private val settings: SettingsRepository,
) : AuthRepository {

  override val isRemote = true

  private val firebaseUser: Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
    auth.addAuthStateListener(listener)
    awaitClose { auth.removeAuthStateListener(listener) }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  override val currentUser: Flow<User?> =
    combine(firebaseUser, settings.settings) { fu, s -> fu to s }
      .mapLatest { (fu, s) -> resolve(fu, s) }

  private suspend fun resolve(fu: FirebaseUser?, s: AppSettings): User? {
    val guest = s.session?.takeIf { it.isGuest }
    if (guest != null && fu == null) {
      return User(guest.userId, guest.name, "", UserRole.USER, s.profiles, isGuest = true)
    }
    if (fu == null) return null
    val role = fetchRole(fu.uid)
    return User(
      id = fu.uid,
      name = fu.displayName?.takeIf { it.isNotBlank() } ?: fu.email?.substringBefore("@").orEmpty(),
      email = fu.email.orEmpty(),
      role = role,
      profiles = s.profiles,
    )
  }

  private suspend fun fetchRole(uid: String): UserRole =
    runCatching {
        val snap = db.collection("users").document(uid).get().await()
        UserRole.valueOf(snap.getString("role") ?: "USER")
      }
      .getOrDefault(UserRole.USER)

  override suspend fun signInWithEmail(email: String, password: String): Result<User> {
    if (!isValidEmail(email)) return Result.failure(AuthException(Reason.INVALID_EMAIL))
    return runAuth {
      auth.signInWithEmailAndPassword(email.trim(), password).await()
      afterSignIn()
    }
  }

  override suspend fun signUpWithEmail(name: String, email: String, password: String): Result<User> {
    if (!isValidEmail(email)) return Result.failure(AuthException(Reason.INVALID_EMAIL))
    return runAuth {
      val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
      val user = result.user ?: error("Pengguna kosong")
      if (name.isNotBlank()) {
        user.updateProfile(userProfileChangeRequest { displayName = name.trim() }).await()
      }
      ensureUserDoc(user, name.ifBlank { email.substringBefore("@") })
      afterSignIn()
    }
  }

  override suspend fun signInWithGoogle(idToken: String?): Result<User> {
    if (idToken.isNullOrBlank()) return Result.failure(AuthException(Reason.NOT_CONFIGURED))
    return runAuth {
      val credential = GoogleAuthProvider.getCredential(idToken, null)
      val result = auth.signInWithCredential(credential).await()
      result.user?.let { ensureUserDoc(it, it.displayName ?: it.email.orEmpty()) }
      afterSignIn()
    }
  }

  override suspend fun continueAsGuest(): User {
    val session = LocalSession("guest", "Tamu", "", UserRole.USER, isGuest = true)
    settings.update { it.copy(session = session, displayName = it.displayName.ifBlank { "Tamu" }) }
    return User(session.userId, session.name, "", UserRole.USER, emptySet(), true)
  }

  override suspend fun signOut() {
    auth.signOut()
    settings.update { it.copy(session = null) }
  }

  override suspend fun deleteAccount(): Result<Unit> = runCatching {
    val user = auth.currentUser
    if (user != null) {
      runCatching { db.collection("users").document(user.uid).delete().await() }
      user.delete().await()
    }
    settings.clearAll()
  }

  private suspend fun afterSignIn(): User {
    // Sesi lokal (mis. tamu) digantikan oleh akun nyata.
    settings.update { it.copy(session = null) }
    val fu = auth.currentUser ?: error("Pengguna kosong")
    return resolve(fu, settings.settingsSnapshot()) ?: error("Pengguna kosong")
  }

  private suspend fun ensureUserDoc(user: FirebaseUser, name: String) {
    runCatching {
      val ref = db.collection("users").document(user.uid)
      if (!ref.get().await().exists()) {
        ref.set(mapOf("name" to name, "email" to user.email.orEmpty(), "role" to "USER")).await()
      }
    }
  }

  private suspend fun runAuth(block: suspend () -> User): Result<User> =
    try {
      Result.success(block())
    } catch (e: FirebaseAuthWeakPasswordException) {
      Result.failure(AuthException(Reason.WEAK_PASSWORD, e.message))
    } catch (e: FirebaseAuthUserCollisionException) {
      Result.failure(AuthException(Reason.EMAIL_IN_USE, e.message))
    } catch (e: FirebaseAuthInvalidCredentialsException) {
      Result.failure(AuthException(Reason.WRONG_CREDENTIALS, e.message))
    } catch (e: FirebaseAuthInvalidUserException) {
      Result.failure(AuthException(Reason.WRONG_CREDENTIALS, e.message))
    } catch (e: FirebaseNetworkException) {
      Result.failure(AuthException(Reason.NETWORK, e.message))
    } catch (e: FirebaseAuthException) {
      Result.failure(AuthException(Reason.UNKNOWN, e.message))
    } catch (e: Exception) {
      Result.failure(AuthException(Reason.UNKNOWN, e.message))
    }
}

private suspend fun SettingsRepository.settingsSnapshot(): AppSettings =
  settings.first()
