package com.junaidjamshid.i211203.domain.repository

import com.junaidjamshid.i211203.domain.model.User
import com.junaidjamshid.i211203.util.Resource
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for authentication operations.
 * This is the contract that the data layer must implement.
 */
interface AuthRepository {
    
    suspend fun login(email: String, password: String): Resource<User>
    
    suspend fun signUp(
        email: String,
        password: String,
        username: String,
        fullName: String,
        phoneNumber: String
    ): Resource<User>
    
    suspend fun logout(): Resource<Unit>
    
    suspend fun getCurrentUser(): Resource<User?>
    
    fun getCurrentUserId(): String?
    
    fun isUserLoggedIn(): Boolean
    
    suspend fun sendPasswordResetEmail(email: String): Resource<Unit>
    
    suspend fun updatePushToken(token: String): Resource<Unit>
}
