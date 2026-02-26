package com.junaidjamshid.i211203

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.junaidjamshid.i211203.data.remote.supabase.SupabaseClientProvider
import com.junaidjamshid.i211203.presentation.auth.LoginActivity
import com.junaidjamshid.i211203.presentation.main.MainActivityNew
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@AndroidEntryPoint
class SplashScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_splash_screen)

        lifecycleScope.launch {
            // Wait minimum splash duration for branding
            delay(1500)
            
            // Wait for session to be loaded from storage (with timeout)
            val sessionStatus = withTimeoutOrNull(3000) {
                SupabaseClientProvider.client.auth.sessionStatus.first { status ->
                    status !is SessionStatus.Initializing
                }
            }
            
            val isLoggedIn = when (sessionStatus) {
                is SessionStatus.Authenticated -> true
                else -> false
            }
            
            if (isLoggedIn) {
                // User is already logged in, navigate to Main
                startActivity(Intent(this@SplashScreen, MainActivityNew::class.java))
            } else {
                // User not logged in, navigate to Login
                startActivity(Intent(this@SplashScreen, LoginActivity::class.java))
            }
            finish()
        }
    }
}
