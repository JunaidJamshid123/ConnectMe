package com.junaidjamshid.i211203

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.junaidjamshid.i211203.data.remote.supabase.SupabaseClientProvider
import com.junaidjamshid.i211203.domain.repository.UserRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class ConnectMeApplication : Application(), DefaultLifecycleObserver {
    
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface UserRepositoryEntryPoint {
        fun userRepository(): UserRepository
    }
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // Lazy access to UserRepository after Hilt is initialized
    private val userRepository: UserRepository by lazy {
        EntryPointAccessors.fromApplication(this, UserRepositoryEntryPoint::class.java).userRepository()
    }
    
    override fun onCreate() {
        super<Application>.onCreate()
        // Initialize Supabase client with application context for session persistence
        SupabaseClientProvider.initialize(this)
        
        // Register lifecycle observer for online status management
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }
    
    override fun onStart(owner: LifecycleOwner) {
        // App came to foreground - set online status to true
        updateOnlineStatus(true)
    }
    
    override fun onStop(owner: LifecycleOwner) {
        // App went to background - set online status to false and update lastSeen
        updateOnlineStatus(false)
    }
    
    private fun updateOnlineStatus(isOnline: Boolean) {
        applicationScope.launch {
            try {
                val currentUserId = SupabaseClientProvider.client.auth.currentUserOrNull()?.id
                if (currentUserId != null) {
                    userRepository.updateOnlineStatus(currentUserId, isOnline)
                    if (!isOnline) {
                        userRepository.updateLastSeen(currentUserId)
                    }
                }
            } catch (e: Exception) {
                // Silently ignore errors for online status updates
            }
        }
    }
}
