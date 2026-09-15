package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class StrawberrycandyApplication : Application() {
  override fun onCreate() {
    super.onCreate()
    try {
      Log.d("StrawberrycandyApp", "Initializing application and services...")
      if (FirebaseApp.getApps(this).isEmpty()) {
        try {
          FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
          Log.w("StrawberrycandyApp", "Default init failed, initializing with fallback FirebaseOptions", e)
          val options = FirebaseOptions.Builder()
              .setApplicationId("1:821912500640:android:strawberrycandy")
              .setApiKey("AIzaSyDummyKeyStrawberrycandyFallback123456789")
              .setProjectId("strawberrycandy-app")
              .setDatabaseUrl("https://strawberrycandy-app-default-rtdb.firebaseio.com")
              .setStorageBucket("strawberrycandy.firebasestorage.app")
              .build()
          FirebaseApp.initializeApp(this, options)
        }
      }
      com.example.data.auth.AuthMemoryStore.init(this)
    } catch (e: Exception) {
      Log.e("StrawberrycandyApp", "Critical error during Application onCreate initialization", e)
    }
  }
}
