package com.junaidjamshid.i211203.presentation.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.junaidjamshid.i211203.R
import com.junaidjamshid.i211203.databinding.ActivityLoginScreemBinding
import com.junaidjamshid.i211203.presentation.main.MainActivityNew
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Clean Architecture Login Activity using ViewModel and StateFlow.
 */
@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLoginScreemBinding
    private val viewModel: LoginViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = ActivityLoginScreemBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupClickListeners()
        observeUiState()
    }
    
    private fun setupClickListeners() {
        binding.registerLink.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }
        
        binding.LoginBtn.setOnClickListener {
            val email = binding.Email.text.toString().trim()
            val password = binding.Password.text.toString().trim()
            
            if (validateInputs(email, password)) {
                viewModel.onEmailChange(email)
                viewModel.onPasswordChange(password)
                viewModel.onLoginClick()
            }
        }
    }
    
    private fun validateInputs(email: String, password: String): Boolean {
        return when {
            email.isEmpty() -> {
                binding.Email.error = "Email is required"
                false
            }
            password.isEmpty() -> {
                binding.Password.error = "Password is required"
                false
            }
            else -> true
        }
    }
    
    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    handleUiState(state)
                }
            }
        }
    }
    
    private fun handleUiState(state: LoginUiState) {
        // Show/hide loading
        binding.LoginBtn.isEnabled = !state.isLoading
        binding.loginProgressBar.isVisible = state.isLoading
        binding.LoginBtn.text = if (state.isLoading) "" else "Log In"
        
        // Handle success
        if (state.isLoggedIn) {
            Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()
            navigateToMain()
        }
        
        // Handle error
        state.error?.let { error ->
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }
    
    private fun navigateToMain() {
        startActivity(Intent(this, MainActivityNew::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        })
        finish()
    }
}
