package com.junaidjamshid.i211203.presentation.chat

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.junaidjamshid.i211203.R
import com.junaidjamshid.i211203.databinding.ActivityChatsBinding
import com.junaidjamshid.i211203.presentation.call.VideoCallActivity
import com.junaidjamshid.i211203.presentation.chat.adapter.MessageAdapterNew
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Clean Architecture Chat Activity.
 */
@AndroidEntryPoint
class ChatActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityChatsBinding
    private val viewModel: ChatViewModel by viewModels()
    
    private lateinit var messageAdapter: MessageAdapterNew
    private var receiverUserId: String = ""
    private var typingAnimationJob: Job? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = ActivityChatsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Set light status bar with dark icons
        window.statusBarColor = Color.WHITE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
        
        receiverUserId = intent.getStringExtra("USER_ID") ?: ""
        if (receiverUserId.isEmpty()) {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        setupRecyclerView()
        setupClickListeners()
        setupTextWatcher()
        observeUiState()
        
        viewModel.initializeChat(receiverUserId)
    }
    
    private fun setupRecyclerView() {
        val currentUserId = viewModel.currentUserId ?: ""
        
        messageAdapter = MessageAdapterNew(
            currentUserId = currentUserId,
            onMessageLongClick = { message ->
                // Show edit/delete dialog
                showMessageOptionsDialog(message.messageId)
            }
        )
        
        binding.recyclerViewChats.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true
            }
            adapter = messageAdapter
        }
    }
    
    private fun setupTextWatcher() {
        binding.editTextMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val hasText = !s.isNullOrEmpty()
                
                // Update send button visibility/icon
                updateSendButton(hasText)
                
                // Notify typing
                if (hasText) {
                    viewModel.onUserTyping()
                }
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
        
        // Handle focus changes
        binding.editTextMessage.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                viewModel.onUserStoppedTyping()
            }
        }
    }
    
    private fun updateSendButton(hasText: Boolean) {
        if (hasText) {
            binding.btnSendMessage.setImageResource(R.drawable.ic_send_message)
            binding.btnSendMessage.alpha = 1f
        } else {
            binding.btnSendMessage.setImageResource(R.drawable.ic_mic)
            binding.btnSendMessage.alpha = 0.7f
        }
    }
    
    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            viewModel.onUserStoppedTyping()
            finish()
        }
        
        binding.btnSendMessage.setOnClickListener {
            sendMessage()
        }
        
        binding.btnVideoCall.setOnClickListener {
            initiateVideoCall()
        }
        
        binding.btnVoiceCall.setOnClickListener {
            initiateVideoCall()
        }
        
        binding.btnVanishMode.setOnClickListener {
            viewModel.toggleVanishMode()
            updateVanishModeUI()
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
    
    private fun handleUiState(state: ChatUiState) {
        // Handle shimmer loading
        if (state.isLoading && state.messages.isEmpty()) {
            binding.shimmerContainer.visibility = View.VISIBLE
            binding.shimmerContainer.startShimmer()
            binding.recyclerViewChats.visibility = View.GONE
        } else {
            binding.shimmerContainer.stopShimmer()
            binding.shimmerContainer.visibility = View.GONE
            binding.recyclerViewChats.visibility = View.VISIBLE
        }
        
        // Update other user info
        state.otherUser?.let { user ->
            binding.txtUserName.text = user.username
            
            if (!user.profilePicture.isNullOrEmpty()) {
                try {
                    val imageBytes = android.util.Base64.decode(user.profilePicture, android.util.Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    binding.userProfileImage.setImageBitmap(bitmap)
                    
                    // Also set profile image on typing indicator
                    binding.root.findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.imgTypingProfile)
                        ?.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    binding.userProfileImage.setImageResource(R.drawable.default_profile)
                }
            }
        }
        
        // Update online status
        updateOnlineStatus(state.isOtherUserOnline, state.lastSeenTimestamp)
        
        // Update typing indicator
        updateTypingIndicator(state.isOtherUserTyping)
        
        // Update messages
        messageAdapter.submitList(state.messages) {
            // Scroll to bottom after messages are updated
            if (state.messages.isNotEmpty()) {
                binding.recyclerViewChats.scrollToPosition(state.messages.size - 1)
            }
        }
        
        // Clear input on message sent
        if (state.messageSent) {
            binding.editTextMessage.text?.clear()
            viewModel.resetMessageSent()
        }
        
        // Handle error
        state.error?.let { error ->
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }
    
    private fun updateOnlineStatus(isOnline: Boolean, lastSeenTimestamp: Long?) {
        binding.txtOnlineStatus.isVisible = true
        
        if (isOnline) {
            binding.txtOnlineStatus.text = "Active now"
            binding.txtOnlineStatus.setTextColor(Color.parseColor("#00C853"))
        } else {
            binding.txtOnlineStatus.text = viewModel.formatLastSeen(lastSeenTimestamp)
            binding.txtOnlineStatus.setTextColor(Color.parseColor("#8E8E8E"))
        }
    }
    
    private fun updateTypingIndicator(isTyping: Boolean) {
        val typingContainer = binding.root.findViewById<View>(R.id.typingIndicator) ?: return
        
        if (isTyping) {
            typingContainer.visibility = View.VISIBLE
            startTypingAnimation()
        } else {
            stopTypingAnimation()
            typingContainer.visibility = View.GONE
        }
    }
    
    private fun startTypingAnimation() {
        // Cancel any existing animation
        typingAnimationJob?.cancel()
        
        val dot1 = binding.root.findViewById<View>(R.id.typingDot1) ?: return
        val dot2 = binding.root.findViewById<View>(R.id.typingDot2) ?: return
        val dot3 = binding.root.findViewById<View>(R.id.typingDot3) ?: return
        
        typingAnimationJob = lifecycleScope.launch {
            while (isActive) {
                // Animate dots in sequence
                animateDot(dot1)
                delay(150)
                animateDot(dot2)
                delay(150)
                animateDot(dot3)
                delay(400)
            }
        }
    }
    
    private fun animateDot(dot: View) {
        val scaleUp = ObjectAnimator.ofFloat(dot, "scaleX", 1f, 1.3f).apply { duration = 200 }
        val scaleUpY = ObjectAnimator.ofFloat(dot, "scaleY", 1f, 1.3f).apply { duration = 200 }
        val scaleDown = ObjectAnimator.ofFloat(dot, "scaleX", 1.3f, 1f).apply { duration = 200 }
        val scaleDownY = ObjectAnimator.ofFloat(dot, "scaleY", 1.3f, 1f).apply { duration = 200 }
        val translateUp = ObjectAnimator.ofFloat(dot, "translationY", 0f, -8f).apply { duration = 200 }
        val translateDown = ObjectAnimator.ofFloat(dot, "translationY", -8f, 0f).apply { duration = 200 }
        
        val upSet = AnimatorSet().apply {
            playTogether(scaleUp, scaleUpY, translateUp)
            interpolator = AccelerateDecelerateInterpolator()
        }
        
        val downSet = AnimatorSet().apply {
            playTogether(scaleDown, scaleDownY, translateDown)
            interpolator = AccelerateDecelerateInterpolator()
        }
        
        AnimatorSet().apply {
            playSequentially(upSet, downSet)
            start()
        }
    }
    
    private fun stopTypingAnimation() {
        typingAnimationJob?.cancel()
        typingAnimationJob = null
    }
    
    private fun sendMessage() {
        val messageText = binding.editTextMessage.text.toString().trim()
        if (messageText.isEmpty()) return
        
        viewModel.sendMessage(messageText)
    }
    
    private fun initiateVideoCall() {
        val channelName = if (viewModel.currentUserId!! < receiverUserId) {
            "${viewModel.currentUserId}_${receiverUserId}"
        } else {
            "${receiverUserId}_${viewModel.currentUserId}"
        }
        
        val intent = Intent(this, VideoCallActivity::class.java).apply {
            putExtra("CHANNEL_NAME", channelName)
            putExtra("USER_ID", receiverUserId)
            putExtra("IS_CALLER", true)
        }
        startActivity(intent)
    }
    
    private fun showMessageOptionsDialog(messageId: String) {
        val options = arrayOf("Delete")
        
        AlertDialog.Builder(this)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> viewModel.deleteMessage(messageId)
                }
            }
            .show()
    }
    
    private fun updateVanishModeUI() {
        val isEnabled = viewModel.uiState.value.isVanishModeEnabled
        Toast.makeText(
            this,
            if (isEnabled) "Vanish Mode Enabled" else "Vanish Mode Disabled",
            Toast.LENGTH_SHORT
        ).show()
    }
    
    override fun onPause() {
        super.onPause()
        viewModel.onUserStoppedTyping()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopTypingAnimation()
    }
}
