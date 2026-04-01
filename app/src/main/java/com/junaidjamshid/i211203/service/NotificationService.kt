package com.junaidjamshid.i211203.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.junaidjamshid.i211203.domain.repository.NotificationRepository
import com.junaidjamshid.i211203.util.NotificationEventBus
import com.junaidjamshid.i211203.util.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Background service for listening to real-time notifications
 * and showing local notifications when new ones arrive.
 */
@AndroidEntryPoint
class NotificationService : Service() {
    
    companion object {
        private const val TAG = "NotificationService"
        private var isRunning = false
        
        fun start(context: Context) {
            if (!isRunning) {
                Log.d(TAG, "Starting NotificationService...")
                val intent = Intent(context, NotificationService::class.java)
                context.startService(intent)
            } else {
                Log.d(TAG, "NotificationService already running")
            }
        }
        
        fun stop(context: Context) {
            Log.d(TAG, "Stopping NotificationService...")
            val intent = Intent(context, NotificationService::class.java)
            context.stopService(intent)
            isRunning = false
        }
    }
    
    @Inject
    lateinit var notificationRepository: NotificationRepository
    
    @Inject
    lateinit var notificationHelper: NotificationHelper
    
    @Inject
    lateinit var notificationEventBus: NotificationEventBus
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var subscriptionJob: Job? = null
    
    override fun onCreate() {
        super.onCreate()
        isRunning = true
        Log.d(TAG, "NotificationService created")
        startListeningForNotifications()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "NotificationService onStartCommand")
        return START_STICKY
    }
    
    private fun startListeningForNotifications() {
        subscriptionJob?.cancel()
        subscriptionJob = serviceScope.launch {
            Log.d(TAG, "Starting notification subscription...")
            
            notificationRepository.subscribeToNewNotifications()
                .catch { e ->
                    Log.e(TAG, "Error in notification subscription: ${e.message}", e)
                }
                .collect { notification ->
                    Log.d(TAG, ">>> RECEIVED NEW NOTIFICATION <<<")
                    Log.d(TAG, "Type: ${notification.type}")
                    Log.d(TAG, "From: ${notification.username}")
                    Log.d(TAG, "NotificationId: ${notification.notificationId}")
                    
                    // Show local notification on main thread
                    launch(Dispatchers.Main) {
                        Log.d(TAG, "Showing local notification...")
                        notificationHelper.showSocialNotification(notification)
                    }
                    
                    // Emit to event bus for UI updates
                    notificationEventBus.tryEmitNewNotification(notification)
                    Log.d(TAG, "Emitted to event bus")
                }
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        subscriptionJob?.cancel()
        serviceScope.cancel()
        Log.d(TAG, "NotificationService destroyed")
    }
}
