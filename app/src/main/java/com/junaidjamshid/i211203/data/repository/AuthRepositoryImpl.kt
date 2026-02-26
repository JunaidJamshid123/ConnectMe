package com.junaidjamshid.i211203.data.repository

import com.junaidjamshid.i211203.data.mapper.UserMapper.toDomain
import com.junaidjamshid.i211203.data.remote.supabase.SupabaseAuthDataSource
import com.junaidjamshid.i211203.data.remote.supabase.SupabaseUserDataSource
import com.junaidjamshid.i211203.domain.model.User
import com.junaidjamshid.i211203.domain.repository.AuthRepository
import com.junaidjamshid.i211203.util.Resource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of AuthRepository that uses Supabase.
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authDataSource: SupabaseAuthDataSource,
    private val userDataSource: SupabaseUserDataSource
) : AuthRepository {
    
    override suspend fun login(email: String, password: String): Resource<User> {
        return try {
            val userId = authDataSource.login(email, password)
            val userDto = userDataSource.getUserById(userId)
            if (userDto != null) {
                Resource.Success(userDto.toDomain())
            } else {
                Resource.Error("User data not found")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Login failed")
        }
    }
    
    override suspend fun signUp(
        email: String,
        password: String,
        username: String,
        fullName: String,
        phoneNumber: String
    ): Resource<User> {
        return try {
            val userId = authDataSource.signUp(email, password)
            authDataSource.createUserInDatabase(userId, email, username, fullName, phoneNumber)
            val userDto = userDataSource.getUserById(userId)
            if (userDto != null) {
                Resource.Success(userDto.toDomain())
            } else {
                Resource.Error("User creation failed")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Sign up failed")
        }
    }
    
    override suspend fun logout(): Resource<Unit> {
        return try {
            getCurrentUserId()?.let { userId ->
                userDataSource.updateOnlineStatus(userId, false)
            }
            authDataSource.logout()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Logout failed")
        }
    }
    
    override suspend fun getCurrentUser(): Resource<User?> {
        return try {
            val userId = authDataSource.getCurrentUserId()
            if (userId != null) {
                val userDto = userDataSource.getUserById(userId)
                Resource.Success(userDto?.toDomain())
            } else {
                Resource.Success(null)
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to get current user")
        }
    }
    
    override fun getCurrentUserId(): String? {
        return authDataSource.getCurrentUserId()
    }
    
    override fun isUserLoggedIn(): Boolean {
        return authDataSource.isUserLoggedIn()
    }
    
    override suspend fun sendPasswordResetEmail(email: String): Resource<Unit> {
        return try {
            authDataSource.sendPasswordResetEmail(email)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to send reset email")
        }
    }
    
    override suspend fun updatePushToken(token: String): Resource<Unit> {
        return try {
            val userId = getCurrentUserId()
            if (userId != null) {
                authDataSource.updatePushToken(userId, token)
                Resource.Success(Unit)
            } else {
                Resource.Error("User not logged in")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update push token")
        }
    }
}
