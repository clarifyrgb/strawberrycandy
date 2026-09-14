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
   * Signs in the user via Firebase Email/Password Authentication.
   * Strictly enforces password verification and returns an error if credentials are invalid.
   */
  suspend fun signIn(email: String, password: String): FirebaseAuthResult {
    val cleanEmail = email.trim().lowercase()
    val cleanPass = password.trim()

    if (cleanEmail.isBlank() || cleanPass.isBlank()) {
      return FirebaseAuthResult.Error("Email and password cannot be blank.")
    }

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
    } catch (e: Exception) {
      val msg = e.localizedMessage ?: "Authentication failed."
      Log.w("FirebaseAuthManager", "Sign in failed for $cleanEmail: $msg", e)
      val isWrong = e is FirebaseAuthInvalidCredentialsException || e is FirebaseAuthInvalidUserException || msg.contains("password", ignoreCase = true) || msg.contains("credential", ignoreCase = true) || msg.contains("user", ignoreCase = true) || msg.contains("no user record", ignoreCase = true)
      FirebaseAuthResult.Error(
        message = if (isWrong) "Incorrect email or password for $cleanEmail. Please check your credentials or tap 'Forgot Password?'." else msg,
        isWrongPassword = isWrong,
        rawException = e
      )
    }
  }

  /**
   * Registers a new user via Firebase Email/Password Authentication.
   */
  suspend fun signUp(email: String, password: String): FirebaseAuthResult {
    val cleanEmail = email.trim().lowercase()
    val cleanPass = password.trim()

    if (cleanEmail.isBlank() || cleanPass.isBlank()) {
      return FirebaseAuthResult.Error("Email and password cannot be blank.")
    }

    return try {
      val newUser = suspendCancellableCoroutine<FirebaseUser> { cont ->
        auth.createUserWithEmailAndPassword(cleanEmail, cleanPass)
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
      val msg = e.localizedMessage ?: "Failed to create account with Firebase."
      Log.e("FirebaseAuthManager", "Failed to create Firebase user: $msg", e)
      FirebaseAuthResult.Error(message = msg, rawException = e)
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
