package com.example.data.auth

import android.content.Context
import android.content.SharedPreferences
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory and persistent credential store that securely remembers
 * the user's first login and subsequent accounts in memory and device preferences.
 */
object AuthMemoryStore {
  private val inMemoryCredentials = ConcurrentHashMap<String, String>()
  private var lastLoggedInEmail: String? = null
  private var lastLoggedInPassword: String? = null
  private var sharedPreferences: SharedPreferences? = null

  fun init(context: Context) {
    if (sharedPreferences == null) {
      sharedPreferences = context.applicationContext.getSharedPreferences(
        "auth_credentials_memory",
        Context.MODE_PRIVATE
      )
      // Populate memory cache from device storage
      sharedPreferences?.let { prefs ->
        prefs.all.forEach { (key, value) ->
          if (key.startsWith("pass_") && value is String && value.isNotBlank()) {
            val email = key.removePrefix("pass_")
            inMemoryCredentials[email.lowercase()] = value
          }
        }
        lastLoggedInEmail = prefs.getString("last_email", null)?.takeIf { it.isNotBlank() }
        lastLoggedInPassword = prefs.getString("last_pass", null)?.takeIf { it.isNotBlank() }
      }
    }
  }

  fun rememberCredential(email: String, password: String) {
    val cleanEmail = email.trim().lowercase()
    val cleanPass = password.trim()
    if (cleanEmail.isBlank() || cleanPass.isBlank()) return

    inMemoryCredentials[cleanEmail] = cleanPass
    lastLoggedInEmail = cleanEmail
    lastLoggedInPassword = cleanPass

    sharedPreferences?.edit()
      ?.putString("pass_$cleanEmail", cleanPass)
      ?.putString("last_email", cleanEmail)
      ?.putString("last_pass", cleanPass)
      ?.apply()
  }

  fun getPassword(email: String?): String? {
    if (email.isNullOrBlank()) return null
    val clean = email.trim().lowercase()
    val inMem = inMemoryCredentials[clean]
    if (!inMem.isNullOrBlank()) return inMem
    val fromPrefs = sharedPreferences?.getString("pass_$clean", null)?.takeIf { it.isNotBlank() }
    if (!fromPrefs.isNullOrBlank()) {
      inMemoryCredentials[clean] = fromPrefs
      return fromPrefs
    }
    return null
  }

  fun getLastEmail(): String? {
    return lastLoggedInEmail ?: sharedPreferences?.getString("last_email", null)?.takeIf { it.isNotBlank() }
  }

  fun getLastPassword(): String? {
    return lastLoggedInPassword ?: sharedPreferences?.getString("last_pass", null)?.takeIf { it.isNotBlank() }
  }

  fun hasCredential(email: String?): Boolean {
    return !getPassword(email).isNullOrBlank()
  }

  fun clearCredential(email: String) {
    val clean = email.trim().lowercase()
    inMemoryCredentials.remove(clean)
    sharedPreferences?.edit()?.remove("pass_$clean")?.apply()
    if (lastLoggedInEmail == clean) {
      lastLoggedInEmail = null
      lastLoggedInPassword = null
      sharedPreferences?.edit()?.remove("last_email")?.remove("last_pass")?.apply()
    }
  }
}
