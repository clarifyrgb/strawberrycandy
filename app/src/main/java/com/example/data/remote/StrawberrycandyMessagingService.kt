package com.example.data.remote

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class StrawberrycandyMessagingService : FirebaseMessagingService() {

  companion object {
    const val CHANNEL_ID = "strawberrycandy_notifications"
    const val CHANNEL_NAME = "Strawberrycandy Notifications"

    fun updateFcmTokenOnServer(email: String?) {
      if (email.isNullOrBlank()) return
      try {
        com.google.firebase.messaging.FirebaseMessaging.getInstance().token
          .addOnCompleteListener { task ->
            if (!task.isSuccessful) {
              Log.d("FCM", "FCM registration token not available in sandbox environment: ${task.exception?.message}")
              return@addOnCompleteListener
            }
            val token = task.result ?: return@addOnCompleteListener
            Log.d("FCM", "Got FCM token for $email: $token")
            try {
              val db = FirebaseFirestore.getInstance()
              val userMap = hashMapOf(
                "email" to email.trim().lowercase(),
                "fcmToken" to token,
                "updatedAt" to System.currentTimeMillis()
              )
              db.collection("users").document(email.trim().lowercase())
                .set(userMap, SetOptions.merge())
                .addOnSuccessListener {
                  Log.d("FCM", "Successfully saved FCM token to Firestore for $email")
                }
                .addOnFailureListener { e ->
                  Log.e("FCM", "Failed to save FCM token to Firestore", e)
                }
            } catch (e: Exception) {
              Log.e("FCM", "Error saving FCM token", e)
            }
          }
      } catch (e: Exception) {
        Log.d("FCM", "Firebase Messaging not supported in this environment: ${e.message}")
      }
    }
  }

  override fun onNewToken(token: String) {
    super.onNewToken(token)
    Log.d("FCM", "Refreshed token: $token")
    val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email
    if (!currentUserEmail.isNullOrBlank()) {
      updateFcmTokenOnServer(currentUserEmail)
    }
  }

  override fun onMessageReceived(remoteMessage: RemoteMessage) {
    super.onMessageReceived(remoteMessage)
    Log.d("FCM", "From: ${remoteMessage.from}")

    val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "Strawberrycandy"
    val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: "You have a new update or reply."
    val novelId = remoteMessage.data["novelId"]
    val chapterTitle = remoteMessage.data["chapterTitle"]
    val commentId = remoteMessage.data["commentId"]

    showNotification(title, body, novelId, chapterTitle, commentId)
  }

  private fun showNotification(title: String, body: String, novelId: String?, chapterTitle: String?, commentId: String?) {
    createNotificationChannel()

    val intent = Intent(this, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      if (!novelId.isNullOrBlank()) putExtra("novelId", novelId)
      if (!chapterTitle.isNullOrBlank()) putExtra("chapterTitle", chapterTitle)
      if (!commentId.isNullOrBlank()) putExtra("commentId", commentId)
      putExtra("openComments", true)
    }

    val pendingIntentFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    } else {
      PendingIntent.FLAG_UPDATE_CURRENT
    }

    val pendingIntent = PendingIntent.getActivity(this, System.currentTimeMillis().toInt(), intent, pendingIntentFlag)

    val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle(title)
      .setContentText(body)
      .setStyle(NotificationCompat.BigTextStyle().bigText(body))
      .setAutoCancel(true)
      .setContentIntent(pendingIntent)
      .setPriority(NotificationCompat.PRIORITY_HIGH)

    try {
      with(NotificationManagerCompat.from(this)) {
        notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
      }
    } catch (e: SecurityException) {
      Log.e("FCM", "SecurityException when posting notification: ${e.message}")
    }
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        CHANNEL_ID,
        CHANNEL_NAME,
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "Notifications for new novel chapters and comment replies"
      }
      val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      notificationManager.createNotificationChannel(channel)
    }
  }
}
