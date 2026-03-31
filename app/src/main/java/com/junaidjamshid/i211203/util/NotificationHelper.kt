package com.junaidjamshid.i211203.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Base64
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.junaidjamshid.i211203.R
import com.junaidjamshid.i211203.domain.model.Notification
import com.junaidjamshid.i211203.domain.model.NotificationType
import com.junaidjamshid.i211203.presentation.notifications.NotificationsActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for showing local/in-app notifications.
 * Uses Android's NotificationManager for displaying system notifications.
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val TAG = "NotificationHelper"
        const val CHANNEL_ID_SOCIAL = "social_notifications"
        const val CHANNEL_NAME_SOCIAL = "Social Notifications"
        const val CHANNEL_DESC_SOCIAL = "Likes, comments, follows, and mentions"
        
        const val CHANNEL_ID_MESSAGES = "message_notifications"
        const val CHANNEL_NAME_MESSAGES = "Messages"
        const val CHANNEL_DESC_MESSAGES = "Direct message notifications"
        
        private const val GROUP_KEY_SOCIAL = "com.junaidjamshid.i211203.SOCIAL_NOTIFICATIONS"
        private const val SUMMARY_ID = 0
    }
    
    private val notificationManager = NotificationManagerCompat.from(context)
    private var notificationIdCounter = 1
    
    init {
        Log.d(TAG, "NotificationHelper initialized")
        createNotificationChannels()
    }
    
    /**
     * Create notification channels (required for Android O+)
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(TAG, "Creating notification channels for Android O+")
            
            val socialChannel = NotificationChannel(
                CHANNEL_ID_SOCIAL,
                CHANNEL_NAME_SOCIAL,
                NotificationManager.IMPORTANCE_HIGH  // Changed to HIGH for better visibility
            ).apply {
                description = CHANNEL_DESC_SOCIAL
                enableVibration(true)
                setShowBadge(true)
            }
            
            val messageChannel = NotificationChannel(
                CHANNEL_ID_MESSAGES,
                CHANNEL_NAME_MESSAGES,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC_MESSAGES
                enableVibration(true)
                setShowBadge(true)
            }
            
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(socialChannel)
            manager.createNotificationChannel(messageChannel)
            
            Log.d(TAG, "Notification channels created successfully")
        } else {
            Log.d(TAG, "Android version < O, no channels needed")
        }
    }
    
    /**
     * Show a notification for a social event (like, comment, follow, mention)
     */
    fun showSocialNotification(notification: Notification) {
        Log.d(TAG, "showSocialNotification called for type: ${notification.type}")
        
        if (!hasNotificationPermission()) {
            Log.w(TAG, "Notification permission not granted, cannot show notification")
            return
        }
        
        val (title, body) = getNotificationContent(notification)
        Log.d(TAG, "Notification content - Title: $title, Body: $body")
        
        // Create intent to open notifications activity
        val intent = NotificationsActivity.newIntent(context).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            notification.notificationId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Try to decode profile image
        val largeIcon = decodeBase64ToBitmap(notification.userProfileImage)
        
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID_SOCIAL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)  // Changed to HIGH
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)  // Add defaults for sound/vibration
            .apply {
                largeIcon?.let { setLargeIcon(it) }
            }
        
        try {
            val notifId = notificationIdCounter++
            notificationManager.notify(notifId, notificationBuilder.build())
            Log.d(TAG, "Notification posted successfully with ID: $notifId")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException showing notification: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error showing notification: ${e.message}", e)
        }
    }
    
    /**
     * Show a like notification
     */
    fun showLikeNotification(
        actorUsername: String,
        actorProfileImage: String?,
        postId: String
    ) {
        if (!hasNotificationPermission()) return
        
        val title = "New Like"
        val body = "$actorUsername liked your post"
        
        showSimpleNotification(title, body, actorProfileImage)
    }
    
    /**
     * Show a comment notification
     */
    fun showCommentNotification(
        actorUsername: String,
        actorProfileImage: String?,
        commentText: String,
        postId: String
    ) {
        if (!hasNotificationPermission()) return
        
        val title = "New Comment"
        val body = "$actorUsername commented: ${commentText.take(50)}${if (commentText.length > 50) "..." else ""}"
        
        showSimpleNotification(title, body, actorProfileImage)
    }
    
    /**
     * Show a follow notification
     */
    fun showFollowNotification(
        actorUsername: String,
        actorProfileImage: String?
    ) {
        if (!hasNotificationPermission()) return
        
        val title = "New Follower"
        val body = "$actorUsername started following you"
        
        showSimpleNotification(title, body, actorProfileImage)
    }
    
    /**
     * Show a mention notification
     */
    fun showMentionNotification(
        actorUsername: String,
        actorProfileImage: String?,
        postId: String
    ) {
        if (!hasNotificationPermission()) return
        
        val title = "You were mentioned"
        val body = "$actorUsername mentioned you in a post"
        
        showSimpleNotification(title, body, actorProfileImage)
    }
    
    /**
     * Show a message notification
     */
    fun showMessageNotification(
        senderUsername: String,
        senderProfileImage: String?,
        messagePreview: String,
        conversationId: String
    ) {
        if (!hasNotificationPermission()) return
        
        val title = senderUsername
        val body = messagePreview.take(100)
        
        val largeIcon = decodeBase64ToBitmap(senderProfileImage)
        
        val intent = Intent(context, Class.forName("com.junaidjamshid.i211203.presentation.messages.DmsActivity")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            conversationId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID_MESSAGES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .apply {
                largeIcon?.let { setLargeIcon(it) }
            }
        
        try {
            notificationManager.notify(conversationId.hashCode(), notificationBuilder.build())
        } catch (e: SecurityException) {
            // Permission denied
        }
    }
    
    /**
     * Helper to show a simple notification
     */
    private fun showSimpleNotification(
        title: String,
        body: String,
        profileImage: String?
    ) {
        val largeIcon = decodeBase64ToBitmap(profileImage)
        
        val intent = NotificationsActivity.newIntent(context).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID_SOCIAL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setGroup(GROUP_KEY_SOCIAL)
            .apply {
                largeIcon?.let { setLargeIcon(it) }
            }
        
        try {
            notificationManager.notify(notificationIdCounter++, notificationBuilder.build())
            showSummaryNotification()
        } catch (e: SecurityException) {
            // Permission denied
        }
    }
    
    /**
     * Show summary notification for grouping
     */
    private fun showSummaryNotification() {
        val summaryNotification = NotificationCompat.Builder(context, CHANNEL_ID_SOCIAL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("ConnectMe")
            .setContentText("New notifications")
            .setStyle(NotificationCompat.InboxStyle()
                .setSummaryText("New activity"))
            .setGroup(GROUP_KEY_SOCIAL)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .build()
        
        try {
            notificationManager.notify(SUMMARY_ID, summaryNotification)
        } catch (e: SecurityException) {
            // Permission denied
        }
    }
    
    /**
     * Get notification title and body based on type
     */
    private fun getNotificationContent(notification: Notification): Pair<String, String> {
        return when (notification.type) {
            NotificationType.LIKE -> {
                "New Like" to "${notification.username} liked your post"
            }
            NotificationType.COMMENT -> {
                val commentPreview = notification.commentText?.take(50) ?: ""
                "New Comment" to "${notification.username} commented: $commentPreview"
            }
            NotificationType.FOLLOW -> {
                "New Follower" to "${notification.username} started following you"
            }
            NotificationType.FOLLOW_REQUEST -> {
                "Follow Request" to "${notification.username} requested to follow you"
            }
            NotificationType.MENTION -> {
                "You were mentioned" to "${notification.username} mentioned you in a post"
            }
            NotificationType.LIKE_COMMENT -> {
                "Comment Liked" to "${notification.username} liked your comment"
            }
            NotificationType.TAG -> {
                "You were tagged" to "${notification.username} tagged you in a post"
            }
        }
    }
    
    /**
     * Decode base64 string to bitmap for notification icon
     */
    private fun decodeBase64ToBitmap(base64String: String?): Bitmap? {
        if (base64String.isNullOrEmpty()) return null
        
        return try {
            val cleanBase64 = if (base64String.contains(",")) {
                base64String.substringAfter(",")
            } else {
                base64String
            }
            
            val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            
            // Scale down for notification
            bitmap?.let {
                val size = 128
                Bitmap.createScaledBitmap(it, size, size, true)
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Check if we have notification permission
     */
    private fun hasNotificationPermission(): Boolean {
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        Log.d(TAG, "hasNotificationPermission: $hasPermission (SDK: ${Build.VERSION.SDK_INT})")
        return hasPermission
    }
    
    /**
     * Show a test notification to verify the system is working
     */
    fun showTestNotification() {
        Log.d(TAG, "showTestNotification called")
        
        if (!hasNotificationPermission()) {
            Log.w(TAG, "Cannot show test notification - no permission")
            return
        }
        
        val intent = NotificationsActivity.newIntent(context).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_SOCIAL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Test Notification")
            .setContentText("If you see this, notifications are working!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
        
        try {
            notificationManager.notify(9999, notification)
            Log.d(TAG, "Test notification posted successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing test notification: ${e.message}", e)
        }
    }
    
    /**
     * Cancel all notifications
     */
    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }
    
    /**
     * Cancel a specific notification
     */
    fun cancelNotification(id: Int) {
        notificationManager.cancel(id)
    }
}
