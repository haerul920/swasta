package id.swasana.feature.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import id.swasana.BuildConfig

/** Masuk dengan Google lewat Credential Manager; menghasilkan ID token untuk Firebase Auth. */
object GoogleSignInHelper {

  sealed interface Outcome {
    data class Token(val idToken: String) : Outcome

    data object NotConfigured : Outcome

    data object Cancelled : Outcome

    data object Failed : Outcome
  }

  val isConfigured: Boolean get() = BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()

  suspend fun requestIdToken(activityContext: Context): Outcome {
    if (!isConfigured) return Outcome.NotConfigured
    val option =
      GetGoogleIdOption.Builder()
        .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
        .setFilterByAuthorizedAccounts(false)
        .build()
    val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
    return try {
      val result = CredentialManager.create(activityContext).getCredential(activityContext, request)
      val credential = result.credential
      if (
        credential is CustomCredential &&
          credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
      ) {
        Outcome.Token(GoogleIdTokenCredential.createFrom(credential.data).idToken)
      } else {
        Outcome.Failed
      }
    } catch (e: GetCredentialCancellationException) {
      Outcome.Cancelled
    } catch (e: GetCredentialException) {
      Outcome.Failed
    }
  }
}
