package com.junaidjamshid.i211203.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.junaidjamshid.i211203.domain.usecase.auth.SignUpUseCase
import com.junaidjamshid.i211203.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for SignUp screen.
 */
@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val signUpUseCase: SignUpUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()
    
    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, error = null) }
    }
    
    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, error = null) }
    }
    
    fun onConfirmPasswordChange(confirmPassword: String) {
        _uiState.update { it.copy(confirmPassword = confirmPassword, error = null) }
    }
    
    fun onUsernameChange(username: String) {
        _uiState.update { it.copy(username = username, error = null) }
    }
    
    fun onFullNameChange(fullName: String) {
        _uiState.update { it.copy(fullName = fullName, error = null) }
    }
    
    fun onPhoneNumberChange(phoneNumber: String) {
        _uiState.update { it.copy(phoneNumber = phoneNumber, error = null) }
    }
    
    fun onSignUpClick() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            when (val result = signUpUseCase(
                email = state.email,
                password = state.password,
                confirmPassword = state.confirmPassword,
                username = state.username,
                fullName = state.fullName,
                phoneNumber = state.phoneNumber
            )) {
                is Resource.Success -> {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            isSignedUp = true,
                            user = result.data
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
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
