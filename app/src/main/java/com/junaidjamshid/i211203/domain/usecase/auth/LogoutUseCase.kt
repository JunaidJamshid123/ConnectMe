package com.junaidjamshid.i211203.domain.usecase.auth

import com.junaidjamshid.i211203.domain.repository.AuthRepository
import com.junaidjamshid.i211203.util.Resource
import javax.inject.Inject

/**
 * Use case for user logout.
 */
class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Resource<Unit> {
        return authRepository.logout()
    }
}
