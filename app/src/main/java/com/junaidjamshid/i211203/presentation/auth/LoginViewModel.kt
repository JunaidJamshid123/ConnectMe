package com.junaidjamshid.i211203.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.junaidjamshid.i211203.domain.usecase.auth.GetCurrentUserUseCase
import com.junaidjamshid.i211203.domain.usecase.auth.LoginUseCase
import com.junaidjamshid.i211203.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Login screen.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()
    
    init {
        checkIfLoggedIn()
    }
    
    private fun checkIfLoggedIn() {
        if (getCurrentUserUseCase.isUserLoggedIn()) {
            _uiState.update { it.copy(isLoggedIn = true) }
        }
    }
    
    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, error = null) }
    }
    
    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, error = null) }
    }
    
    fun onLoginClick() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            when (val result = loginUseCase(state.email, state.password)) {
                is Resource.Success -> {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            isLoggedIn = true,
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
