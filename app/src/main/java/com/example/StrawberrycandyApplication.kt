package com.example

import android.app.Application
import android.util.Log

class StrawberrycandyApplication : Application() {
  override fun onCreate() {
    super.onCreate()
    try {
      Log.d("StrawberrycandyApp", "Initializing application and services...")
      com.example.data.auth.AuthMemoryStore.init(this)
    } catch (e: Exception) {
      Log.e("StrawberrycandyApp", "Critical error during Application onCreate initialization", e)
    }
  }
}
