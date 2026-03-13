package com.junaidjamshid.i211203.domain.usecase.auth

import com.junaidjamshid.i211203.domain.repository.AuthRepository
import com.junaidjamshid.i211203.util.Resource
import javax.inject.Inject

/**
 * Use case for verifying email exists for password reset.
 */
class VerifyEmailExistsUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String): Resource<Boolean> {
        if (email.isBlank()) {
            return Resource.Error("Email cannot be empty")
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return Resource.Error("Please enter a valid email address")
        }
        return authRepository.verifyEmailExists(email)
    }
}

/**
 * Use case for verifying username matches the email account.
 */
class VerifyUsernameUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, username: String): Resource<Boolean> {
        if (username.isBlank()) {
            return Resource.Error("Username cannot be empty")
        }
        return authRepository.verifyUsernameForEmail(email, username)
    }
}

/**
 * Use case for resetting password after verification.
 */
class ResetPasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, newPassword: String, confirmPassword: String): Resource<Unit> {
        if (newPassword.isBlank()) {
            return Resource.Error("Password cannot be empty")
        }
        if (newPassword.length < 6) {
            return Resource.Error("Password must be at least 6 characters")
        }
        if (newPassword != confirmPassword) {
            return Resource.Error("Passwords do not match")
        }
        return authRepository.resetPassword(email, newPassword)
    }
}

/**
 * Use case for sending password reset email (legacy - can be removed if not needed).
 */
class ForgotPasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String): Resource<Unit> {
        if (email.isBlank()) {
            return Resource.Error("Email cannot be empty")
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return Resource.Error("Please enter a valid email address")
        }
        return authRepository.sendPasswordResetEmail(email)
    }
}
