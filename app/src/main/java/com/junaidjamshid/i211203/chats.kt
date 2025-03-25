package com.junaidjamshid.i211203

import android.app.AlertDialog
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import de.hdodenhof.circleimageview.CircleImageView

class chats : AppCompatActivity() {
    // Firebase components
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    // UI Components
    private lateinit var btnBack: ImageView
    private lateinit var userProfileImage: CircleImageView
    private lateinit var txtUserName: TextView
    private lateinit var recyclerViewChats: RecyclerView
    private lateinit var editTextMessage: TextInputEditText
    private lateinit var btnSendMessage: FloatingActionButton
    private lateinit var btnVanishMode: ImageView

    // Message-related variables
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var messageList: ArrayList<Message>
    private lateinit var receiverUserId: String
    private lateinit var senderUserId: String

    // Additional features
    private var isVanishModeEnabled = false
    private var currentEditingMessage: Message? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide() // Hide Action Bar
        setContentView(R.layout.activity_chats)

        // Initialize Firebase components
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Get current user ID
        senderUserId = auth.currentUser?.uid ?: ""

        // Initialize UI components
        btnBack = findViewById(R.id.btnBack)
        userProfileImage = findViewById(R.id.userProfileImage)
        txtUserName = findViewById(R.id.txtUserName)
        recyclerViewChats = findViewById(R.id.recyclerViewChats)
        editTextMessage = findViewById(R.id.editTextMessage)
        btnSendMessage = findViewById(R.id.btnSendMessage)
        btnVanishMode = findViewById(R.id.btnVanishMode)

        // Initialize message list and adapter
        messageList = ArrayList()
        messageAdapter = MessageAdapter(
            this,
            messageList,
            userProfileImage
        ) { messageToEdit ->
            // Enable message editing
            showMessageEditDialog(messageToEdit)
        }

        // Setup RecyclerView
        recyclerViewChats.layoutManager = LinearLayoutManager(this)
        recyclerViewChats.adapter = messageAdapter

        // Retrieve user ID from intent
        receiverUserId = intent.getStringExtra("USER_ID") ?: ""
        if (receiverUserId.isEmpty()) {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Fetch and display user details
        fetchUserDetails(receiverUserId)

        // Setup back button
        btnBack.setOnClickListener { onBackPressed() }

        // Setup send message button
        btnSendMessage.setOnClickListener {
            sendMessage()
        }

        // Setup vanish mode toggle
        setupVanishModeToggle()

        // Load messages
        loadMessages()
    }

    private fun setupVanishModeToggle() {
        btnVanishMode.setOnClickListener {
            isVanishModeEnabled = !isVanishModeEnabled

            // Update button appearance
            btnVanishMode.setImageResource(
                if (isVanishModeEnabled) R.drawable.switchh
                else R.drawable.switchh
            )

            Toast.makeText(
                this,
                if (isVanishModeEnabled) "Vanish Mode Enabled" else "Vanish Mode Disabled",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showMessageEditDialog(message: Message) {
        val editDialog = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_message, null)
        val editMessageInput: TextInputEditText = dialogView.findViewById(R.id.editMessageInput)

        // Populate existing message
        editMessageInput.setText(message.message)

        editDialog.setView(dialogView)
            .setTitle("Edit Message")
            .setPositiveButton("Save") { _, _ ->
                val newMessageText = editMessageInput.text.toString().trim()

                if (newMessageText.isNotEmpty()) {
                    editMessage(message, newMessageText)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun editMessage(originalMessage: Message, newMessageText: String) {
        val messageRef = database.reference.child("Chats").child(originalMessage.messageId)

        val updateMap = mapOf(
            "message" to newMessageText,
            "isEdited" to true,
            "editedTimestamp" to System.currentTimeMillis()
        )

        messageRef.updateChildren(updateMap)
            .addOnSuccessListener {
                Toast.makeText(this, "Message edited", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to edit message", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchUserDetails(userId: String) {
        val userRef = database.reference.child("Users").child(userId)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Fetch user name
                    val userName = snapshot.child("username").getValue(String::class.java) ?: "Unknown User"
                    txtUserName.text = userName

                    // Fetch and display profile image
                    val profileImageData = snapshot.child("profilePicture").getValue(String::class.java)

                    if (!profileImageData.isNullOrEmpty()) {
                        try {
                            // Decode Base64 string to bitmap
                            val imageBytes = Base64.decode(profileImageData, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                            userProfileImage.setImageBitmap(bitmap)
                        } catch (e: Exception) {
                            // Set default profile image if decoding fails
                            userProfileImage.setImageResource(R.drawable.default_profile)
                        }
                    } else {
                        // Set default profile image if no image data
                        userProfileImage.setImageResource(R.drawable.default_profile)
                    }
                } else {
                    Toast.makeText(this@chats, "User not found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@chats, "Failed to fetch user details: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun sendMessage() {
        val messageText = editTextMessage.text.toString().trim()
        if (messageText.isEmpty()) return

        // Create message object with vanish mode option
        val messageRef = database.reference.child("Chats").push()
        val messageId = messageRef.key ?: ""
        val message = Message(
            senderId = senderUserId,
            receiverId = receiverUserId,
            message = messageText,
            messageId = messageId,
            isVanishMode = isVanishModeEnabled,
            isRead = false
        )

        // Save message to Firebase
        messageRef.setValue(message)
            .addOnSuccessListener {
                // Clear input field
                editTextMessage.text?.clear()

                // Update last message for sender and receiver
                updateLastMessage(message)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateLastMessage(message: Message) {
        // Update last message for sender's contact list
        val senderContactRef = database.reference
            .child("UserContacts")
            .child(senderUserId)
            .child(receiverUserId)
        senderContactRef.child("lastMessage").setValue(message.message)
        senderContactRef.child("lastMessageTime").setValue(message.timestamp)

        // Update last message for receiver's contact list
        val receiverContactRef = database.reference
            .child("UserContacts")
            .child(receiverUserId)
            .child(senderUserId)
        receiverContactRef.child("lastMessage").setValue(message.message)
        receiverContactRef.child("lastMessageTime").setValue(message.timestamp)
    }

    private fun loadMessages() {
        val messagesRef = database.reference.child("Chats")
        messagesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                messageList.clear()
                for (messageSnapshot in snapshot.children) {
                    val message = messageSnapshot.getValue(Message::class.java)
                    message?.let {
                        // Filter messages for current chat
                        if ((it.senderId == senderUserId && it.receiverId == receiverUserId) ||
                            (it.senderId == receiverUserId && it.receiverId == senderUserId)
                        ) {
                            // Handle vanish mode messages
                            if (!it.isVanishMode || !it.isRead) {
                                messageList.add(it)
                            }
                        }
                    }
                }
                // Sort messages by timestamp
                messageList.sortBy { it.timestamp }
                messageAdapter.notifyDataSetChanged()

                // Scroll to bottom of RecyclerView
                if (messageList.isNotEmpty()) {
                    recyclerViewChats.scrollToPosition(messageList.size - 1)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@chats, "Failed to load messages", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        // Delete vanish mode messages when chat is closed
        deleteVanishModeMessages()
    }

    private fun deleteVanishModeMessages() {
        val messagesRef = database.reference.child("Chats")
        messagesRef.orderByChild("isVanishMode").equalTo(true)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (messageSnapshot in snapshot.children) {
                        val message = messageSnapshot.getValue(Message::class.java)
                        if (message?.isRead == true) {
                            messageSnapshot.ref.removeValue()
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle potential errors
                }
            })
    }
}