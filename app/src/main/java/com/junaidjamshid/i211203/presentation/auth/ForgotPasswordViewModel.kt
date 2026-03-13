package com.junaidjamshid.i211203.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.junaidjamshid.i211203.domain.usecase.auth.ResetPasswordUseCase
import com.junaidjamshid.i211203.domain.usecase.auth.VerifyEmailExistsUseCase
import com.junaidjamshid.i211203.domain.usecase.auth.VerifyUsernameUseCase
import com.junaidjamshid.i211203.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Steps in the forgot password flow.
 */
enum class ForgotPasswordStep {
    EMAIL_INPUT,      // Step 1: Enter email
    USERNAME_VERIFY,  // Step 2: Enter username for verification
    NEW_PASSWORD,     // Step 3: Enter new password
    SUCCESS           // Step 4: Password reset successful
}

/**
 * UI State for Forgot Password screen.
 */
data class ForgotPasswordUiState(
    val currentStep: ForgotPasswordStep = ForgotPasswordStep.EMAIL_INPUT,
    val email: String = "",
    val username: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel for Forgot Password screen with multi-step verification.
 */
@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val verifyEmailExistsUseCase: VerifyEmailExistsUseCase,
    private val verifyUsernameUseCase: VerifyUsernameUseCase,
    private val resetPasswordUseCase: ResetPasswordUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()
    
    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, error = null) }
    }
    
    fun onUsernameChange(username: String) {
        _uiState.update { it.copy(username = username, error = null) }
    }
    
    fun onNewPasswordChange(password: String) {
        _uiState.update { it.copy(newPassword = password, error = null) }
    }
    
    fun onConfirmPasswordChange(password: String) {
        _uiState.update { it.copy(confirmPassword = password, error = null) }
    }
    
    /**
     * Step 1: Verify email exists in database.
     */
    fun onVerifyEmail() {
        val email = _uiState.value.email.trim()
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            when (val result = verifyEmailExistsUseCase(email)) {
                is Resource.Success -> {
                    if (result.data == true) {
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                currentStep = ForgotPasswordStep.USERNAME_VERIFY,
                                error = null
                            ) 
                        }
                    } else {
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                error = "No account found with this email"
                            ) 
                        }
                    }
                }
                is Resource.Error -> {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = result.message
                        ) 
                    }
                }
                is Resource.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }
    
    /**
     * Step 2: Verify username matches the email account.
     */
    fun onVerifyUsername() {
        val email = _uiState.value.email.trim()
        val username = _uiState.value.username.trim()
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            when (val result = verifyUsernameUseCase(email, username)) {
                is Resource.Success -> {
                    if (result.data == true) {
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                currentStep = ForgotPasswordStep.NEW_PASSWORD,
                                error = null
                            ) 
                        }
                    } else {
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                error = "Username doesn't match. Please try again."
                            ) 
                        }
                    }
                }
                is Resource.Error -> {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = result.message
                        ) 
                    }
                }
                is Resource.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }
    
    /**
     * Step 3: Reset password with new password.
     */
    fun onResetPassword() {
        val email = _uiState.value.email.trim()
        val newPassword = _uiState.value.newPassword
        val confirmPassword = _uiState.value.confirmPassword
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            when (val result = resetPasswordUseCase(email, newPassword, confirmPassword)) {
                is Resource.Success -> {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            currentStep = ForgotPasswordStep.SUCCESS,
                            error = null
                        ) 
                    }
                }
                is Resource.Error -> {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = result.message
                        ) 
                    }
                }
                is Resource.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }
    
    /**
     * Go back to previous step.
     */
    fun onGoBack() {
        _uiState.update { state ->
            when (state.currentStep) {
                ForgotPasswordStep.USERNAME_VERIFY -> state.copy(
                    currentStep = ForgotPasswordStep.EMAIL_INPUT,
                    username = "",
                    error = null
                )
                ForgotPasswordStep.NEW_PASSWORD -> state.copy(
                    currentStep = ForgotPasswordStep.USERNAME_VERIFY,
                    newPassword = "",
                    confirmPassword = "",
                    error = null
                )
                else -> state
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun resetState() {
        _uiState.update { ForgotPasswordUiState() }
    }
}
