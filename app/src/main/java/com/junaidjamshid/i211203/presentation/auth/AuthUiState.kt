package com.junaidjamshid.i211203.presentation.auth

import com.junaidjamshid.i211203.domain.model.User

/**
 * UI State for Login screen.
 */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    val user: User? = null
)

/**
 * UI State for SignUp screen.
 */
data class SignUpUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val username: String = "",
    val fullName: String = "",
    val phoneNumber: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSignedUp: Boolean = false,
    val user: User? = null
)
