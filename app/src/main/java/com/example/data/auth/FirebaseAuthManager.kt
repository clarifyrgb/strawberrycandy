package com.example.data.auth

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed class FirebaseAuthResult {
  data class Success(val user: FirebaseUser, val isNewUser: Boolean = false) : FirebaseAuthResult()
  data class Error(val message: String, val isWrongPassword: Boolean = false, val rawException: Throwable? = null) : FirebaseAuthResult()
}

class FirebaseAuthManager {
  private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

  val currentFirebaseUser: FirebaseUser?
    get() = try {
      auth.currentUser
    } catch (e: Exception) {
      Log.w("FirebaseAuthManager", "Error accessing current user", e)
      null
    }

  val currentUserEmail: String?
    get() = currentFirebaseUser?.email

  /**
   * Signs in or registers the user via Firebase Email/Password Authentication.
   * If the user doesn't exist in Firebase yet, it automatically creates the account.
   */
  suspend fun signInOrRegister(email: String, password: String): FirebaseAuthResult {
    val cleanEmail = email.trim().lowercase()
    val cleanPass = password.trim()

    if (cleanEmail.isBlank() || cleanPass.isBlank()) {
      return FirebaseAuthResult.Error("Email and password cannot be blank.")
    }

    // First attempt: Firebase Sign In
    return try {
      val user = suspendCancellableCoroutine<FirebaseUser> { cont ->
        auth.signInWithEmailAndPassword(cleanEmail, cleanPass)
          .addOnSuccessListener { result ->
            val u = result.user
            if (u != null) {
              cont.resume(u)
            } else {
              cont.resumeWithException(IllegalStateException("Authenticated user is null"))
            }
          }
          .addOnFailureListener { exc ->
            cont.resumeWithException(exc)
          }
      }
      Log.d("FirebaseAuthManager", "Successfully signed in via Firebase Auth: ${user.email}")
      FirebaseAuthResult.Success(user, isNewUser = false)
    } catch (e: FirebaseAuthInvalidCredentialsException) {
      // In modern Firebase Identity, INVALID_LOGIN_CREDENTIALS is returned for both
      // wrong passwords and unregistered accounts. Attempt user creation:
      Log.i("FirebaseAuthManager", "Sign-in credential check for $cleanEmail, trying user creation or verifying collision...")
      val createResult = tryCreateAccount(cleanEmail, cleanPass)
      if (createResult is FirebaseAuthResult.Success) {
        createResult
      } else if (createResult is FirebaseAuthResult.Error && (createResult.rawException is FirebaseAuthUserCollisionException || createResult.message.contains("already in use", ignoreCase = true))) {
        // Account exists in Firebase, so the password was wrong
        Log.w("FirebaseAuthManager", "Account exists in Firebase, wrong password entered for $cleanEmail")
        FirebaseAuthResult.Error(
          message = "Incorrect password for $cleanEmail. If you forgot your password, tap 'Forgot Password?'.",
          isWrongPassword = true,
          rawException = e
        )
      } else {
        createResult
      }
    } catch (e: FirebaseAuthInvalidUserException) {
      // User does not exist yet -> create account
      Log.i("FirebaseAuthManager", "User does not exist in Firebase Auth yet. Creating account: $cleanEmail")
      tryCreateAccount(cleanEmail, cleanPass)
    } catch (e: FirebaseAuthUserCollisionException) {
      FirebaseAuthResult.Error(
        message = "An account already exists for $cleanEmail with a different sign-in method.",
        rawException = e
      )
    } catch (e: Exception) {
      val msg = e.localizedMessage ?: "Firebase authentication failed"
      Log.e("FirebaseAuthManager", "Firebase Auth general error: $msg", e)
      // Check if the error is user not found
      if (msg.contains("no user record") || msg.contains("user-not-found") || msg.contains("USER_NOT_FOUND")) {
        tryCreateAccount(cleanEmail, cleanPass)
      } else {
        FirebaseAuthResult.Error(message = msg, rawException = e)
      }
    }
  }

  private suspend fun tryCreateAccount(email: String, password: String): FirebaseAuthResult {
    return try {
      val newUser = suspendCancellableCoroutine<FirebaseUser> { cont ->
        auth.createUserWithEmailAndPassword(email, password)
          .addOnSuccessListener { result ->
            val u = result.user
            if (u != null) {
              cont.resume(u)
            } else {
              cont.resumeWithException(IllegalStateException("Created Firebase user is null"))
            }
          }
          .addOnFailureListener { exc ->
            cont.resumeWithException(exc)
          }
      }
      Log.d("FirebaseAuthManager", "Successfully created user in Firebase Auth: ${newUser.email}")
      FirebaseAuthResult.Success(newUser, isNewUser = true)
    } catch (e: Exception) {
      Log.e("FirebaseAuthManager", "Failed to create Firebase user: ${e.localizedMessage}", e)
      FirebaseAuthResult.Error(
        message = e.localizedMessage ?: "Failed to authenticate account with Firebase.",
        rawException = e
      )
    }
  }

  /**
   * Sends an official Firebase Password Reset email to the specified address.
   */
  suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
    val cleanEmail = email.trim().lowercase()
    return try {
      suspendCancellableCoroutine<Unit> { cont ->
        auth.sendPasswordResetEmail(cleanEmail)
          .addOnSuccessListener {
            Log.d("FirebaseAuthManager", "Firebase reset email sent successfully to $cleanEmail")
            cont.resume(Unit)
          }
          .addOnFailureListener { exc ->
            Log.w("FirebaseAuthManager", "Firebase reset email failed: ${exc.localizedMessage}")
            cont.resumeWithException(exc)
          }
      }
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  /**
   * Update password for the currently signed in Firebase user.
   */
  suspend fun updateCurrentUserPassword(newPassword: String): Result<Unit> {
    val user = auth.currentUser ?: return Result.failure(IllegalStateException("No Firebase user logged in"))
    return try {
      suspendCancellableCoroutine<Unit> { cont ->
        user.updatePassword(newPassword.trim())
          .addOnSuccessListener { cont.resume(Unit) }
          .addOnFailureListener { exc -> cont.resumeWithException(exc) }
      }
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  /**
   * Signs in to Firebase Auth using a Google ID Token from Google Sign-In / Credential Manager.
   * Securely identifies the user with Firebase Authentication.
   */
  suspend fun signInWithGoogleIdToken(idToken: String): FirebaseAuthResult {
    if (idToken.isBlank()) {
      return FirebaseAuthResult.Error("Google ID token cannot be blank.")
    }
    return try {
      val credential = GoogleAuthProvider.getCredential(idToken, null)
      val user = suspendCancellableCoroutine<FirebaseUser> { cont ->
        auth.signInWithCredential(credential)
          .addOnSuccessListener { authResult ->
            val u = authResult.user
            if (u != null) {
              val isNew = authResult.additionalUserInfo?.isNewUser ?: false
              cont.resume(u)
            } else {
              cont.resumeWithException(IllegalStateException("FirebaseUser is null after Google sign-in"))
            }
          }
          .addOnFailureListener { exc ->
            cont.resumeWithException(exc)
          }
      }
      Log.i("FirebaseAuthManager", "Successfully identified via Google Sign-In with Firebase Auth: ${user.email} (uid: ${user.uid})")
      FirebaseAuthResult.Success(user)
    } catch (e: Exception) {
      Log.e("FirebaseAuthManager", "Google credential authentication error: ${e.localizedMessage}", e)
      FirebaseAuthResult.Error(
        message = e.localizedMessage ?: "Failed to sign in with Google account.",
        rawException = e
      )
    }
  }

  fun signOut() {
    try {
      auth.signOut()
    } catch (e: Exception) {
      Log.w("FirebaseAuthManager", "Error during Firebase sign out", e)
    }
  }
}
