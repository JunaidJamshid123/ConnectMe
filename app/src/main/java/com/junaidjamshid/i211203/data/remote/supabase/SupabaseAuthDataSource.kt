package com.junaidjamshid.i211203.data.remote.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data source for Supabase Authentication operations.
 */
@Singleton
class SupabaseAuthDataSource @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    
    suspend fun login(email: String, password: String): String {
        supabaseClient.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        return supabaseClient.auth.currentUserOrNull()?.id 
            ?: throw Exception("Login failed")
    }
    
    suspend fun signUp(email: String, password: String): String {
        supabaseClient.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
        return supabaseClient.auth.currentUserOrNull()?.id 
            ?: throw Exception("Sign up failed")
    }
    
    suspend fun logout() {
        supabaseClient.auth.signOut()
    }
    
    fun getCurrentUserId(): String? {
        return supabaseClient.auth.currentUserOrNull()?.id
    }
    
    fun isUserLoggedIn(): Boolean {
        return supabaseClient.auth.currentUserOrNull() != null
    }
    
    suspend fun sendPasswordResetEmail(email: String) {
        supabaseClient.auth.resetPasswordForEmail(email)
    }
    
    suspend fun createUserInDatabase(
        userId: String,
        email: String,
        username: String,
        fullName: String,
        phoneNumber: String
    ) {
        val userData = buildJsonObject {
            put("user_id", userId)
            put("email", email)
            put("username", username)
            put("full_name", fullName)
            put("phone_number", phoneNumber)
            put("profile_picture", "")
            put("cover_photo", "")
            put("bio", "")
            put("online_status", true)
            put("push_token", "")
            put("created_at", System.currentTimeMillis())
            put("last_seen", System.currentTimeMillis())
            put("vanish_mode_enabled", false)
        }
        
        supabaseClient.postgrest[SupabaseConfig.USERS_TABLE]
            .insert(userData)
    }
    
    suspend fun updatePushToken(userId: String, token: String) {
        supabaseClient.postgrest[SupabaseConfig.USERS_TABLE]
            .update({
                set("push_token", token)
            }) {
                filter {
                    eq("user_id", userId)
                }
            }
    }
}
