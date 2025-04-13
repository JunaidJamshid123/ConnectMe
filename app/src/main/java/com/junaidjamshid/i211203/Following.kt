package com.junaidjamshid.i211203

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.junaidjamshid.i211203.Adapters.FollowersAdapter
import com.junaidjamshid.i211203.models.User

class Following : AppCompatActivity() {

    // UI Components
    private lateinit var backButton: ImageView
    private lateinit var editButton: ImageView
    private lateinit var usernameTextView: TextView
    private lateinit var followersTextView: TextView
    private lateinit var followingTextView: TextView
    private lateinit var searchEditText: TextView
    private lateinit var followingRecyclerView: RecyclerView
    private lateinit var emptyStateView: LinearLayout

    // Firebase references
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    // Data
    private val followingList = mutableListOf<User>()
    private lateinit var followingAdapter: FollowersAdapter  // Reusing the same adapter
    private var currentUserId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_followers)  // Reusing the same layout

        // Initialize UI components
        initViews()

        // Check Authentication
        checkAuthentication()

        // Setup RecyclerView
        setupRecyclerView()

        // Setup listeners
        setupListeners()

        // Load user data
        loadUserData()

        // Load following
        loadFollowing()
    }

    private fun initViews() {
        backButton = findViewById(R.id.back)
        editButton = findViewById(R.id.edit)
        usernameTextView = findViewById(R.id.user_name)
        followersTextView = findViewById(R.id.dms)
        followingTextView = findViewById(R.id.requests)
        searchEditText = findViewById(R.id.searchEditText)
        followingRecyclerView = findViewById(R.id.followersRecyclerView)  // Reusing the followers RecyclerView
        emptyStateView = findViewById(R.id.emptyStateView)

        // Update UI to show Following is active
        followersTextView.textSize = 20f
        followersTextView.setTextColor(resources.getColor(android.R.color.black))
        followingTextView.textSize = 20f
        followingTextView.setTextColor(resources.getColor(R.color.bottom_nav_icon_color))  // Using your defined color
    }

    private fun checkAuthentication() {
        currentUserId = auth.currentUser?.uid ?: run {
            finish()
            return
        }
    }

    private fun setupRecyclerView() {
        followingAdapter = FollowersAdapter(
            followingList,
            onFollowerClick = { user ->
                navigateToUserProfile(user)
            },
            onMessageClick = { user ->
                navigateToChat(user)
            }
        )

        followingRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@Following)
            adapter = followingAdapter
        }
    }

    private fun setupListeners() {
        // Back button click listener
        backButton.setOnClickListener {
            finish()
        }

        // Edit button click listener
        editButton.setOnClickListener {
            // Handle edit functionality
        }

        // Set up follower/following tab clicks
        followersTextView.setOnClickListener {
            val intent = Intent(this, Followers::class.java)
            startActivity(intent)
            finish()
        }

        followingTextView.setOnClickListener {
            // Already on following tab, do nothing
        }

        // Search functionality
        searchEditText.doOnTextChanged { text, _, _, _ ->
            filterFollowing(text.toString())
        }
    }

    private fun loadUserData() {
        database.child("Users").child(currentUserId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val username = snapshot.child("username").getValue(String::class.java) ?: "User"
                    usernameTextView.text = username
                }
            }
    }

    private fun loadFollowing() {
        showLoading(true)

        // Get following IDs from the database
        database.child("following").child(currentUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val followingIds = mutableListOf<String>()

                    // Get all following IDs
                    for (childSnapshot in snapshot.children) {
                        val followingId = childSnapshot.key
                        followingId?.let { followingIds.add(it) }
                    }

                    // Update following count
                    updateFollowingCount(followingIds.size)

                    if (followingIds.isEmpty()) {
                        showEmptyState(true)
                        showLoading(false)
                        return
                    }

                    // Fetch following details
                    fetchFollowingDetails(followingIds)
                }

                override fun onCancelled(error: DatabaseError) {
                    showLoading(false)
                    showEmptyState(true)
                }
            })
    }

    private fun fetchFollowingDetails(followingIds: List<String>) {
        val usersRef = database.child("Users")
        val fetchedFollowing = mutableListOf<User>()
        var fetchCount = 0

        for (followingId in followingIds) {
            usersRef.child(followingId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    fetchCount++

                    val user = snapshot.getValue(User::class.java)
                    user?.let {
                        // Make sure userId is set correctly
                        it.userId = followingId
                        fetchedFollowing.add(it)
                    }

                    // If all following have been fetched, update the adapter
                    if (fetchCount == followingIds.size) {
                        followingList.clear()
                        followingList.addAll(fetchedFollowing)

                        // Sort following alphabetically
                        followingList.sortWith(compareBy({ it.fullName }, { it.username }))

                        followingAdapter.updateFollowers(followingList)

                        // Show empty state if no following after filtering
                        showEmptyState(followingList.isEmpty())
                        showLoading(false)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    fetchCount++

                    // If all attempts are complete, update UI
                    if (fetchCount == followingIds.size) {
                        showEmptyState(followingList.isEmpty())
                        showLoading(false)
                    }
                }
            })
        }
    }

    private fun filterFollowing(query: String) {
        followingAdapter.filterFollowers(query)
        showEmptyState(followingAdapter.itemCount == 0)
    }

    private fun updateFollowingCount(count: Int) {
        followingTextView.text = "$count Following"
    }

    private fun showLoading(isLoading: Boolean) {
        followingRecyclerView.visibility = if (isLoading) View.GONE else View.VISIBLE
    }

    private fun showEmptyState(isEmpty: Boolean) {
        emptyStateView.visibility = if (isEmpty) View.VISIBLE else View.GONE
        followingRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun navigateToUserProfile(user: User) {
        val intent = Intent(this, UserProfile::class.java).apply {
            putExtra("USER_ID", user.userId)
        }
        startActivity(intent)
    }

    private fun navigateToChat(user: User) {
        val intent = Intent(this, chats::class.java).apply {
            putExtra("USER_ID", user.userId)
        }
        startActivity(intent)
    }
}