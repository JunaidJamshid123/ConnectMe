package com.junaidjamshid.i211203.domain.usecase.auth

import com.junaidjamshid.i211203.domain.model.User
import com.junaidjamshid.i211203.domain.repository.AuthRepository
import com.junaidjamshid.i211203.util.Resource
import javax.inject.Inject

/**
 * Use case to get the currently logged in user.
 */
class GetCurrentUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Resource<User?> {
        return authRepository.getCurrentUser()
    }
    
    fun getCurrentUserId(): String? {
        return authRepository.getCurrentUserId()
    }
    
    fun isUserLoggedIn(): Boolean {
        return authRepository.isUserLoggedIn()
    }
}
