package com.junaidjamshid.i211203

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ConnectMeApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
    }
}
