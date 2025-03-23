package com.junaidjamshid.i211203

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.junaidjamshid.i211203.adapters.ContactsAdapter
import com.junaidjamshid.i211203.models.User
import androidx.core.widget.doOnTextChanged

class ContactsFragment : Fragment() {

    private lateinit var contactsRecyclerView: RecyclerView
    private lateinit var contactsAdapter: ContactsAdapter
    private lateinit var contactsCountTextView: TextView
    private lateinit var searchEditText: EditText
    private lateinit var backButton: ImageView
    private lateinit var editButton: ImageView

    private lateinit var databaseReference: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private var currentUserId: String = ""

    private val contactsList = mutableListOf<User>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize Firebase instances
        auth = FirebaseAuth.getInstance()
        currentUserId = auth.currentUser?.uid ?: ""
        databaseReference = FirebaseDatabase.getInstance().reference
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_contacts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupRecyclerView()
        setupListeners()
        fetchContacts()
    }

    private fun initViews(view: View) {
        contactsRecyclerView = view.findViewById(R.id.contacts_recycler_view)
        contactsCountTextView = view.findViewById(R.id.contacts_count)
        searchEditText = view.findViewById(R.id.search_contacts)
        backButton = view.findViewById(R.id.back)
        editButton = view.findViewById(R.id.edit)
    }

    private fun setupRecyclerView() {
        contactsAdapter = ContactsAdapter(
            contactsList,
            onContactClick = { user ->
                // Handle contact click - navigate to user profile
                navigateToUserProfile(user)
            },
            onMessageClick = { user ->
                // Handle message button click - navigate to chat
                navigateToChat(user)
            }
        )

        contactsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = contactsAdapter
        }
    }

    private fun setupListeners() {
        // Setup search functionality
        searchEditText.doOnTextChanged { text, _, _, _ ->
            contactsAdapter.filterContacts(text.toString())
            updateContactsCount()
        }

        // Setup back button
        backButton.setOnClickListener {
            requireActivity().onBackPressed()
        }

        // Setup edit button
        editButton.setOnClickListener {
            // Navigate to edit contacts or other functionality
            Toast.makeText(context, "Edit functionality", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchContacts() {
        if (currentUserId.isEmpty()) {
            Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading state
        showLoading(true)

        // Reference to the following list of current user
        val followingRef = databaseReference.child("Users")

        followingRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val followingIds = mutableListOf<String>()

                // Get all users the current user is following
                for (childSnapshot in snapshot.children) {
                    childSnapshot.key?.let { followingIds.add(it) }
                }

                if (followingIds.isEmpty()) {
                    showLoading(false)
                    updateContactsCount()
                    return
                }

                // Fetch user details for each followingId
                fetchUserDetails(followingIds)
            }

            override fun onCancelled(error: DatabaseError) {
                showLoading(false)
                Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchUserDetails(userIds: List<String>) {
        val usersRef = databaseReference.child("Users")
        val fetchedUsers = mutableListOf<User>()
        var fetchCount = 0

        for (userId in userIds) {
            usersRef.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    fetchCount++

                    val user = snapshot.getValue(User::class.java)
                    user?.let {
                        fetchedUsers.add(it)
                    }

                    // If all users have been fetched, update the adapter
                    if (fetchCount == userIds.size) {
                        contactsList.clear()
                        contactsList.addAll(fetchedUsers)

                        // Sort contacts alphabetically by fullName, then username
                        contactsList.sortWith(compareBy({ it.fullName }, { it.username }))

                        contactsAdapter.updateContacts(contactsList)
                        updateContactsCount()
                        showLoading(false)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    fetchCount++

                    if (fetchCount == userIds.size) {
                        contactsAdapter.updateContacts(contactsList)
                        updateContactsCount()
                        showLoading(false)
                    }
                }
            })
        }
    }

    private fun updateContactsCount() {
        val count = contactsAdapter.itemCount
        contactsCountTextView.text = count.toString()
    }

    private fun showLoading(isLoading: Boolean) {
        // Implement loading indicator if needed
        // For now, we'll just show/hide the RecyclerView
        contactsRecyclerView.visibility = if (isLoading) View.GONE else View.VISIBLE
    }

    private fun navigateToUserProfile(user: User) {
        // TODO: Implement navigation to user profile
        Toast.makeText(context, "Navigate to ${user.username}'s profile", Toast.LENGTH_SHORT).show()
    }

    private fun navigateToChat(user: User) {
        // TODO: Implement navigation to chat with user
        Toast.makeText(context, "Start chat with ${user.username}", Toast.LENGTH_SHORT).show()
    }
}