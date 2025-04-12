package com.junaidjamshid.i211203.HelperClasses

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.junaidjamshid.i211203.R
import com.junaidjamshid.i211203.VideoCalls

class CallNotificationHelper(private val context: Context) {
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val CHANNEL_ID = "call_notifications"
    private val NOTIFICATION_ID = 1001

    init {
        createNotificationChannel()
    }

    fun startListeningForCalls() {
        val currentUserId = auth.currentUser?.uid ?: return

        // Listen for new call entries where the current user is the receiver
        val callsRef = database.reference.child("Calls")
        callsRef.orderByChild("receiverId").equalTo(currentUserId)
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val call = snapshot.getValue(Call::class.java) ?: return

                    // Only notify for new calls with "ongoing" status
                    if (call.status == "ongoing" && call.receiverId == currentUserId) {
                        fetchCallerDetails(call.callerId) { callerName ->
                            showIncomingCallNotification(call, callerName)
                        }
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    val call = snapshot.getValue(Call::class.java) ?: return

                    // If call was accepted or declined, cancel notification
                    if (call.status != "ongoing" && call.receiverId == currentUserId) {
                        cancelCallNotification()
                    }
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    // Cancel notification if call record is removed
                    cancelCallNotification()
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                    // Not needed for this use case
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }

    private fun fetchCallerDetails(callerId: String, callback: (String) -> Unit) {
        val userRef = database.reference.child("Users").child(callerId)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val callerName = if (snapshot.exists()) {
                    snapshot.child("username").getValue(String::class.java) ?: "Unknown"
                } else {
                    "Unknown"
                }
                callback(callerName)
            }

            override fun onCancelled(error: DatabaseError) {
                callback("Unknown")
            }
        })
    }

    private fun showIncomingCallNotification(call: Call, callerName: String) {
        // Create intent for answering the call
        val answerIntent = Intent(context, VideoCalls::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("CHANNEL_NAME", call.channelName)
            putExtra("USER_ID", call.callerId)
            putExtra("IS_CALLER", false)
        }
        val answerPendingIntent = PendingIntent.getActivity(
            context,
            0,
            answerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create intent for declining the call
        val declineIntent = Intent(context, CallActionReceiver::class.java).apply {
            action = "DECLINE_CALL"
            putExtra("CALL_ID", call.callId)
        }
        val declinePendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build the notification
        val callSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Incoming Video Call")
            .setContentText("$callerName is calling you")
            .setSmallIcon(R.drawable.video_camera)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setSound(callSound)
            .setVibrate(longArrayOf(1000, 1000, 1000, 1000))
            .setFullScreenIntent(answerPendingIntent, true)
            .setContentIntent(answerPendingIntent)
            .addAction(R.drawable.telephone, "Answer", answerPendingIntent)
            .addAction(R.drawable.telephone, "Decline", declinePendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()

        // Show the notification
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun cancelCallNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Call Notifications"
            val descriptionText = "Notifications for incoming calls"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                vibrationPattern = longArrayOf(1000, 1000, 1000, 1000)
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    // Data class to represent a call
    data class Call(
        val callId: String = "",
        val callerId: String = "",
        val receiverId: String = "",
        val channelName: String = "",
        val timestamp: Long = 0,
        val type: String = "video",
        val status: String = "ongoing"
    )
}