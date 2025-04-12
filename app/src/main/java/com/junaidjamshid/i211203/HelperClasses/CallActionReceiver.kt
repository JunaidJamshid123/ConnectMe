package com.junaidjamshid.i211203.HelperClasses

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.firebase.database.FirebaseDatabase

class CallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val callId = intent.getStringExtra("CALL_ID") ?: return

        when (action) {
            "DECLINE_CALL" -> {
                // Update call status in Firebase
                val database = FirebaseDatabase.getInstance()
                val callRef = database.reference.child("Calls").child(callId)

                callRef.child("status").setValue("declined")
                    .addOnSuccessListener {
                        // Cancel the notification
                        val notificationHelper = CallNotificationHelper(context)
                        notificationHelper.cancelCallNotification()
                    }
            }
        }
    }
}