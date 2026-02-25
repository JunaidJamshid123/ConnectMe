package com.junaidjamshid.i211203.data.remote.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.junaidjamshid.i211203.util.Constants
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data source for Firebase Authentication operations.
 */
@Singleton
class FirebaseAuthDataSource @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firebaseDatabase: FirebaseDatabase
) {
    
    suspend fun login(email: String, password: String): String {
        val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
        return result.user?.uid ?: throw Exception("Login failed")
    }
    
    suspend fun signUp(email: String, password: String): String {
        val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        return result.user?.uid ?: throw Exception("Sign up failed")
    }
    
    fun logout() {
        firebaseAuth.signOut()
    }
    
    fun getCurrentUserId(): String? {
        return firebaseAuth.currentUser?.uid
    }
    
    fun isUserLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }
    
    suspend fun sendPasswordResetEmail(email: String) {
        firebaseAuth.sendPasswordResetEmail(email).await()
    }
    
    suspend fun createUserInDatabase(
        userId: String,
        email: String,
        username: String,
        fullName: String,
        phoneNumber: String
    ) {
        val userData = hashMapOf(
            "userId" to userId,
            "email" to email,
            "username" to username,
            "fullName" to fullName,
            "phoneNumber" to phoneNumber,
            "profilePicture" to "",
            "coverPhoto" to "",
            "bio" to "",
            "followers" to hashMapOf<String, Any>(),
            "following" to hashMapOf<String, Any>(),
            "blockedUsers" to hashMapOf<String, Any>(),
            "onlineStatus" to true,
            "pushToken" to "",
            "createdAt" to System.currentTimeMillis(),
            "lastSeen" to System.currentTimeMillis(),
            "vanishModeEnabled" to false
        )
        
        firebaseDatabase.reference
            .child(Constants.USERS_REF)
            .child(userId)
            .setValue(userData)
            .await()
    }
    
    suspend fun updatePushToken(userId: String, token: String) {
        firebaseDatabase.reference
            .child(Constants.USERS_REF)
            .child(userId)
            .child("pushToken")
            .setValue(token)
            .await()
    }
}
