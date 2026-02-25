package com.junaidjamshid.i211203

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.junaidjamshid.i211203.presentation.main.MainActivityNew

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val CHANNEL_ID = "ChatNotificationChannel"
        private const val CHANNEL_NAME = "Chat Notifications"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Check if message contains a notification payload
        remoteMessage.notification?.let {
            sendNotification(it.title, it.body)
        }

        // Check if message contains data payload
        remoteMessage.data.isNotEmpty().let {
            // Handle data payload if needed
            val title = remoteMessage.data["title"] ?: "New Message"
            val body = remoteMessage.data["body"] ?: "You have a new message"
            sendNotification(title, body)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Send this token to your server or store it for future use
        sendRegistrationToServer(token)
    }

    private fun sendNotification(title: String?, body: String?) {
        val intent = Intent(this, MainActivityNew::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.bell)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android Oreo and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Show notification
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    private fun sendRegistrationToServer(token: String) {
        // Implement method to send token to your backend
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        currentUserId?.let {
            val userRef = FirebaseDatabase.getInstance().reference
                .child("Users")
                .child(it)
                .child("fcmToken")

            userRef.setValue(token)
        }
    }
}