package com.junaidjamshid.i211203.presentation.chat

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.junaidjamshid.i211203.R
import com.junaidjamshid.i211203.databinding.ActivityChatsBinding
import com.junaidjamshid.i211203.presentation.call.VideoCallActivity
import com.junaidjamshid.i211203.presentation.chat.adapter.MessageAdapterNew
import dagger.hilt.android.AndroidEntryPoint
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
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = ActivityChatsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        receiverUserId = intent.getStringExtra("USER_ID") ?: ""
        if (receiverUserId.isEmpty()) {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        setupRecyclerView()
        setupClickListeners()
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
    
    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
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
        // Update other user info
        state.otherUser?.let { user ->
            binding.txtUserName.text = user.username
            
            if (!user.profilePicture.isNullOrEmpty()) {
                try {
                    val imageBytes = android.util.Base64.decode(user.profilePicture, android.util.Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    binding.userProfileImage.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    binding.userProfileImage.setImageResource(R.drawable.default_profile)
                }
            }
        }
        
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
}
