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

class Followers : AppCompatActivity() {

    // UI Components
    private lateinit var backButton: ImageView
    private lateinit var editButton: ImageView
    private lateinit var usernameTextView: TextView
    private lateinit var followersTextView: TextView
    private lateinit var followingTextView: TextView
    private lateinit var searchEditText: TextView
    private lateinit var followersRecyclerView: RecyclerView
    private lateinit var emptyStateView: LinearLayout

    // Firebase references
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    // Data
    private val followersList = mutableListOf<User>()
    private lateinit var followersAdapter: FollowersAdapter
    private var currentUserId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_followers)

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

        // Load followers
        loadFollowers()
    }

    private fun initViews() {
        backButton = findViewById(R.id.back)
        editButton = findViewById(R.id.edit)
        usernameTextView = findViewById(R.id.user_name)
        followersTextView = findViewById(R.id.dms)
        followingTextView = findViewById(R.id.requests)
        searchEditText = findViewById(R.id.searchEditText)
        followersRecyclerView = findViewById(R.id.followersRecyclerView)
        emptyStateView = findViewById(R.id.emptyStateView)
    }

    private fun checkAuthentication() {
        currentUserId = auth.currentUser?.uid ?: run {
            finish()
            return
        }
    }

    private fun setupRecyclerView() {
        followersAdapter = FollowersAdapter(
            followersList,
            onFollowerClick = { user ->
                navigateToUserProfile(user)
            },
            onMessageClick = { user ->
                navigateToChat(user)
            }
        )

        followersRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@Followers)
            adapter = followersAdapter
        }
    }

    private fun setupListeners() {
        // Back button click listener
        backButton.setOnClickListener {
            finish()
        }

        // Edit button click listener
        editButton.setOnClickListener {
            // Handle edit functionality - maybe blocking followers
            // or some other management functionality
        }

        // Set up follower/following tab clicks
        followersTextView.setOnClickListener {
            // Already on followers tab, do nothing
        }

        followingTextView.setOnClickListener {
            // Navigate to following activity/screen
            val intent = Intent(this, Following::class.java)
            startActivity(intent)
            finish()
        }

        // Search functionality
        searchEditText.doOnTextChanged { text, _, _, _ ->
            filterFollowers(text.toString())
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

    private fun loadFollowers() {
        showLoading(true)

        // Get followers IDs from the database
        database.child("followers").child(currentUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val followerIds = mutableListOf<String>()

                    // Get all follower IDs
                    for (childSnapshot in snapshot.children) {
                        val followerId = childSnapshot.key
                        followerId?.let { followerIds.add(it) }
                    }

                    // Update followers count
                    updateFollowersCount(followerIds.size)

                    if (followerIds.isEmpty()) {
                        showEmptyState(true)
                        showLoading(false)
                        return
                    }

                    // Fetch followers details
                    fetchFollowersDetails(followerIds)
                }

                override fun onCancelled(error: DatabaseError) {
                    showLoading(false)
                    showEmptyState(true)
                }
            })
    }

    private fun fetchFollowersDetails(followerIds: List<String>) {
        val usersRef = database.child("Users")
        val fetchedFollowers = mutableListOf<User>()
        var fetchCount = 0

        for (followerId in followerIds) {
            usersRef.child(followerId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    fetchCount++

                    val user = snapshot.getValue(User::class.java)
                    user?.let {
                        // Make sure userId is set correctly
                        it.userId = followerId
                        fetchedFollowers.add(it)
                    }

                    // If all followers have been fetched, update the adapter
                    if (fetchCount == followerIds.size) {
                        followersList.clear()
                        followersList.addAll(fetchedFollowers)

                        // Sort followers alphabetically
                        followersList.sortWith(compareBy({ it.fullName }, { it.username }))

                        followersAdapter.updateFollowers(followersList)

                        // Show empty state if no followers after filtering
                        showEmptyState(followersList.isEmpty())
                        showLoading(false)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    fetchCount++

                    // If all attempts are complete, update UI
                    if (fetchCount == followerIds.size) {
                        showEmptyState(followersList.isEmpty())
                        showLoading(false)
                    }
                }
            })
        }
    }

    private fun filterFollowers(query: String) {
        followersAdapter.filterFollowers(query)
        showEmptyState(followersAdapter.itemCount == 0)
    }

    private fun updateFollowersCount(count: Int) {
        followersTextView.text = "$count Followers"
    }

    private fun showLoading(isLoading: Boolean) {
        // In a real app, you might want to add a progress indicator
        followersRecyclerView.visibility = if (isLoading) View.GONE else View.VISIBLE
    }

    private fun showEmptyState(isEmpty: Boolean) {
        emptyStateView.visibility = if (isEmpty) View.VISIBLE else View.GONE
        followersRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
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