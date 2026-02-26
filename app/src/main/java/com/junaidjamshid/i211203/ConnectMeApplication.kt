package com.junaidjamshid.i211203

import android.app.Application
import com.junaidjamshid.i211203.data.remote.supabase.SupabaseClientProvider
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ConnectMeApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        // Initialize Supabase client with application context for session persistence
        SupabaseClientProvider.initialize(this)
    }
}
