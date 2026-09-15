package com.example.data.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class GoogleSignInOutcome {
  data class Success(
    val idToken: String,
    val email: String,
    val displayName: String?,
    val photoUrl: String?
  ) : GoogleSignInOutcome()

  data class Canceled(val message: String = "Google Sign-In was cancelled.") : GoogleSignInOutcome()
  data class FallbackNeeded(val message: String) : GoogleSignInOutcome()
  data class Error(val message: String) : GoogleSignInOutcome()
}

class GoogleSignInHandler(private val context: Context) {
  private val credentialManager = CredentialManager.create(context)

  suspend fun signInWithGoogle(webClientId: String? = null): GoogleSignInOutcome = withContext(Dispatchers.IO) {
    try {
      // Determine client ID: from parameter, string resource, or default
      val clientId = webClientId
        ?: getResourceString("default_web_client_id")
        ?: "962976326995-strawberrycandy.apps.googleusercontent.com"

      val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(false)
        .build()

      val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

      val result = credentialManager.getCredential(
        context = context,
        request = request
      )

      val credential = result.credential
      if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val tokenCred = GoogleIdTokenCredential.createFrom(credential.data)
        GoogleSignInOutcome.Success(
          idToken = tokenCred.idToken,
          email = tokenCred.id,
          displayName = tokenCred.displayName,
          photoUrl = tokenCred.profilePictureUri?.toString()
        )
      } else {
        Log.w("GoogleSignInHandler", "Unexpected credential type: ${credential.type}")
        GoogleSignInOutcome.FallbackNeeded("Received credentials of type: ${credential.type}")
      }
    } catch (e: GetCredentialCancellationException) {
      Log.d("GoogleSignInHandler", "User cancelled Google Sign-In")
      GoogleSignInOutcome.Canceled("Sign in was cancelled.")
    } catch (e: NoCredentialException) {
      Log.i("GoogleSignInHandler", "No Google credentials stored on device: ${e.message}")
      GoogleSignInOutcome.FallbackNeeded("No active Google account found on device. You can sign in using your Gmail address and password below.")
    } catch (e: Exception) {
      Log.w("GoogleSignInHandler", "Credential Manager error: ${e.localizedMessage}")
      GoogleSignInOutcome.FallbackNeeded(
        e.localizedMessage ?: "Google Sign-In prompt unavailable on this device."
      )
    }
  }

  private fun getResourceString(name: String): String? {
    return try {
      val resId = context.resources.getIdentifier(name, "string", context.packageName)
      if (resId != 0) context.getString(resId) else null
    } catch (_: Exception) {
      null
    }
  }
}
