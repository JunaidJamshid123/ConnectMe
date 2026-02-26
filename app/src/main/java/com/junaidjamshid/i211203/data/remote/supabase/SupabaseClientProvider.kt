package com.junaidjamshid.i211203.data.remote.supabase

import android.content.Context
import android.content.SharedPreferences
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Singleton object providing the Supabase client instance.
 */
object SupabaseClientProvider {
    
    private var _client: SupabaseClient? = null
    private var sharedPreferences: SharedPreferences? = null
    
    private const val PREFS_NAME = "supabase_auth"
    private const val KEY_SESSION = "session"
    
    /**
     * Initialize the Supabase client with context for session persistence.
     * Call this in Application.onCreate()
     */
    fun initialize(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _client = createSupabaseClient(
            supabaseUrl = SupabaseConfig.SUPABASE_URL,
            supabaseKey = SupabaseConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth) {
                autoLoadFromStorage = true
                alwaysAutoRefresh = true
            }
            install(Postgrest)
            install(Storage)
            install(Realtime)
        }
    }
    
    val client: SupabaseClient
        get() = _client ?: throw IllegalStateException(
            "SupabaseClientProvider not initialized. Call initialize(context) in Application.onCreate()"
        )
    
    /**
     * Check if the client has been initialized
     */
    fun isInitialized(): Boolean = _client != null
}
