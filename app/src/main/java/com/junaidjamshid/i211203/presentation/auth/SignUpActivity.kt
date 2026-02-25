package com.junaidjamshid.i211203.presentation.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.junaidjamshid.i211203.databinding.ActivitySignUpScreenBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Clean Architecture SignUp Activity using ViewModel and StateFlow.
 */
@AndroidEntryPoint
class SignUpActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySignUpScreenBinding
    private val viewModel: SignUpViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = ActivitySignUpScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupClickListeners()
        observeUiState()
    }
    
    private fun setupClickListeners() {
        binding.LoginLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }
        
        binding.registerBtn.setOnClickListener {
            val fullName = binding.FullName.text.toString().trim()
            val username = binding.username.text.toString().trim()
            val phone = binding.Phone.text.toString().trim()
            val email = binding.Email.text.toString().trim()
            val password = binding.Password.text.toString().trim()
            
            if (validateInputs(fullName, username, phone, email, password)) {
                viewModel.onFullNameChange(fullName)
                viewModel.onUsernameChange(username)
                viewModel.onPhoneNumberChange(phone)
                viewModel.onEmailChange(email)
                viewModel.onPasswordChange(password)
                viewModel.onSignUpClick()
            }
        }
    }
    
    private fun validateInputs(
        fullName: String,
        username: String,
        phone: String,
        email: String,
        password: String
    ): Boolean {
        return when {
            fullName.isEmpty() -> {
                binding.FullName.error = "Full name is required"
                false
            }
            username.isEmpty() -> {
                binding.username.error = "Username is required"
                false
            }
            phone.isEmpty() -> {
                binding.Phone.error = "Phone is required"
                false
            }
            email.isEmpty() -> {
                binding.Email.error = "Email is required"
                false
            }
            password.isEmpty() -> {
                binding.Password.error = "Password is required"
                false
            }
            password.length < 6 -> {
                binding.Password.error = "Password must be at least 6 characters"
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
    
    private fun handleUiState(state: SignUpUiState) {
        // Show/hide loading
        binding.registerBtn.isEnabled = !state.isLoading
        
        // Handle success
        if (state.isSignedUp) {
            Toast.makeText(this, "Registration Successful!", Toast.LENGTH_SHORT).show()
            navigateToLogin()
        }
        
        // Handle error
        state.error?.let { error ->
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }
    
    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        })
        finish()
    }
}
