package com.junaidjamshid.i211203.domain.usecase.auth

import com.junaidjamshid.i211203.domain.model.User
import com.junaidjamshid.i211203.domain.repository.AuthRepository
import com.junaidjamshid.i211203.util.Resource
import javax.inject.Inject

/**
 * Use case for user registration.
 */
class SignUpUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        email: String,
        password: String,
        confirmPassword: String,
        username: String,
        fullName: String,
        phoneNumber: String
    ): Resource<User> {
        if (email.isBlank()) {
            return Resource.Error("Email cannot be empty")
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return Resource.Error("Invalid email format")
        }
        if (password.isBlank()) {
            return Resource.Error("Password cannot be empty")
        }
        if (password.length < 6) {
            return Resource.Error("Password must be at least 6 characters")
        }
        if (password != confirmPassword) {
            return Resource.Error("Passwords do not match")
        }
        if (username.isBlank()) {
            return Resource.Error("Username cannot be empty")
        }
        if (username.length < 3) {
            return Resource.Error("Username must be at least 3 characters")
        }
        if (fullName.isBlank()) {
            return Resource.Error("Full name cannot be empty")
        }
        
        return authRepository.signUp(email, password, username, fullName, phoneNumber)
    }
}
