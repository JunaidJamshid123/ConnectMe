package com.junaidjamshid.i211203.presentation.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.junaidjamshid.i211203.R
import com.junaidjamshid.i211203.databinding.ActivityForgotPasswordBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Activity for password reset functionality.
 * Multi-step flow: Email → Username Verification → New Password
 */
@AndroidEntryPoint
class ForgotPasswordActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityForgotPasswordBinding
    private val viewModel: ForgotPasswordViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupClickListeners()
        setupTextWatchers()
        observeUiState()
    }
    
    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            handleBackPress()
        }
        
        binding.actionButton.setOnClickListener {
            handleActionButtonClick()
        }
        
        binding.createAccountLink.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            })
            finish()
        }
        
        binding.backToLoginLink.setOnClickListener {
            finish()
        }
        
        binding.goToLoginButton.setOnClickListener {
            finish()
        }
    }
    
    private fun handleBackPress() {
        val currentStep = viewModel.uiState.value.currentStep
        if (currentStep == ForgotPasswordStep.EMAIL_INPUT || currentStep == ForgotPasswordStep.SUCCESS) {
            finish()
        } else {
            viewModel.onGoBack()
        }
    }
    
    private fun handleActionButtonClick() {
        when (viewModel.uiState.value.currentStep) {
            ForgotPasswordStep.EMAIL_INPUT -> viewModel.onVerifyEmail()
            ForgotPasswordStep.USERNAME_VERIFY -> viewModel.onVerifyUsername()
            ForgotPasswordStep.NEW_PASSWORD -> viewModel.onResetPassword()
            ForgotPasswordStep.SUCCESS -> finish()
        }
    }
    
    private fun setupTextWatchers() {
        binding.emailInput.doAfterTextChanged { text ->
            viewModel.onEmailChange(text?.toString() ?: "")
        }
        
        binding.usernameInput.doAfterTextChanged { text ->
            viewModel.onUsernameChange(text?.toString() ?: "")
        }
        
        binding.newPasswordInput.doAfterTextChanged { text ->
            viewModel.onNewPasswordChange(text?.toString() ?: "")
        }
        
        binding.confirmPasswordInput.doAfterTextChanged { text ->
            viewModel.onConfirmPasswordChange(text?.toString() ?: "")
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
    
    private fun handleUiState(state: ForgotPasswordUiState) {
        // Show/hide loading
        binding.actionButton.isEnabled = !state.isLoading
        binding.progressBar.isVisible = state.isLoading
        
        // Handle error
        binding.errorText.isVisible = state.error != null
        binding.errorText.text = state.error
        
        // Update UI based on step
        updateStepUi(state.currentStep)
    }
    
    private fun updateStepUi(step: ForgotPasswordStep) {
        // Hide all containers first
        binding.emailInputContainer.isVisible = false
        binding.usernameInputContainer.isVisible = false
        binding.passwordInputContainer.isVisible = false
        binding.successContainer.isVisible = false
        binding.actionButton.isVisible = true
        binding.orDivider.isVisible = true
        binding.createAccountLink.isVisible = true
        binding.stepIndicator.isVisible = true
        
        // Reset step indicators
        binding.stepDot1.setBackgroundResource(R.drawable.step_dot_inactive)
        binding.stepDot2.setBackgroundResource(R.drawable.step_dot_inactive)
        binding.stepDot3.setBackgroundResource(R.drawable.step_dot_inactive)
        
        when (step) {
            ForgotPasswordStep.EMAIL_INPUT -> {
                binding.emailInputContainer.isVisible = true
                binding.titleText.text = "Trouble logging in?"
                binding.subtitleText.text = "Enter your email and we'll verify your account."
                binding.actionButton.text = "Next"
                binding.stepDot1.setBackgroundResource(R.drawable.step_dot_active)
            }
            
            ForgotPasswordStep.USERNAME_VERIFY -> {
                binding.usernameInputContainer.isVisible = true
                binding.titleText.text = "Verify your identity"
                binding.subtitleText.text = "Enter your username to confirm this is your account."
                binding.actionButton.text = "Verify"
                binding.stepDot1.setBackgroundResource(R.drawable.step_dot_active)
                binding.stepDot2.setBackgroundResource(R.drawable.step_dot_active)
            }
            
            ForgotPasswordStep.NEW_PASSWORD -> {
                binding.passwordInputContainer.isVisible = true
                binding.titleText.text = "Create new password"
                binding.subtitleText.text = "Your new password must be different from previous passwords."
                binding.actionButton.text = "Reset Password"
                binding.stepDot1.setBackgroundResource(R.drawable.step_dot_active)
                binding.stepDot2.setBackgroundResource(R.drawable.step_dot_active)
                binding.stepDot3.setBackgroundResource(R.drawable.step_dot_active)
            }
            
            ForgotPasswordStep.SUCCESS -> {
                binding.successContainer.isVisible = true
                binding.actionButton.isVisible = false
                binding.orDivider.isVisible = false
                binding.createAccountLink.isVisible = false
                binding.stepIndicator.isVisible = false
                binding.titleText.text = "All done!"
                binding.subtitleText.text = "Your password has been reset successfully."
            }
        }
    }
    
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        handleBackPress()
    }
}
