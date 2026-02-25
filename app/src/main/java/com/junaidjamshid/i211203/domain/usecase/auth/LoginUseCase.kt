package com.junaidjamshid.i211203.domain.usecase.auth

import com.junaidjamshid.i211203.domain.model.User
import com.junaidjamshid.i211203.domain.repository.AuthRepository
import com.junaidjamshid.i211203.util.Resource
import javax.inject.Inject

/**
 * Use case for user login.
 */
class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Resource<User> {
        if (email.isBlank()) {
            return Resource.Error("Email cannot be empty")
        }
        if (password.isBlank()) {
            return Resource.Error("Password cannot be empty")
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return Resource.Error("Invalid email format")
        }
        return authRepository.login(email, password)
    }
}
