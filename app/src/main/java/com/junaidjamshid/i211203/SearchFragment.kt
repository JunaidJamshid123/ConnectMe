package com.junaidjamshid.i211203

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.junaidjamshid.i211203.adapters.RecentSearchAdapter
import com.junaidjamshid.i211203.adapters.SearchAdapter
import com.junaidjamshid.i211203.models.User
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class SearchFragment : Fragment(), SearchAdapter.OnUserClickListener, RecentSearchAdapter.OnRecentSearchListener {

    private lateinit var etSearch: EditText
    private lateinit var rvRecentSearches: RecyclerView
    private lateinit var rvSearchResults: RecyclerView
    private lateinit var tvNoSearches: TextView
    private lateinit var tvClearAll: TextView
    private lateinit var recentSearchesContainer: LinearLayout
    private lateinit var searchResultsContainer: LinearLayout

    private lateinit var allUsers: ArrayList<User>
    private lateinit var filteredUsers: ArrayList<User>
    private lateinit var recentSearches: ArrayList<User>

    private lateinit var searchAdapter: SearchAdapter
    private lateinit var recentSearchAdapter: RecentSearchAdapter

    private lateinit var database: FirebaseDatabase
    private lateinit var usersRef: DatabaseReference
    private lateinit var recentSearchesRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private var currentUserId: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_search, container, false)

        // Initialize Firebase
        database = FirebaseDatabase.getInstance()
        auth = FirebaseAuth.getInstance()
        currentUserId = auth.currentUser?.uid ?: ""
        usersRef = database.reference.child("Users")
        recentSearchesRef = database.reference.child("recentSearches").child(currentUserId)

        // Initialize views
        etSearch = view.findViewById(R.id.et_search)
        rvRecentSearches = view.findViewById(R.id.rv_recent_searches)
        rvSearchResults = view.findViewById(R.id.rv_search_results)
        tvNoSearches = view.findViewById(R.id.tv_no_searches)
        tvClearAll = view.findViewById(R.id.tv_clear_all)
        recentSearchesContainer = view.findViewById(R.id.recent_searches_container)
        searchResultsContainer = view.findViewById(R.id.search_results_container)

        // Initialize RecyclerViews
        setupRecyclerViews()

        // Initialize data
        allUsers = ArrayList()
        filteredUsers = ArrayList()
        recentSearches = ArrayList()

        // Fetch users from Firebase
        fetchUsers()

        // Fetch recent searches
        fetchRecentSearches()

        // Set up search functionality
        setupSearchListener()

        // Clear all recent searches button click
        tvClearAll.setOnClickListener {
            clearAllRecentSearches()
        }

        return view
    }

    private fun setupRecyclerViews() {
        // Setup recent searches RecyclerView
        rvRecentSearches.layoutManager = LinearLayoutManager(context)
        recentSearchAdapter = RecentSearchAdapter(requireContext(), ArrayList(), this)
        rvRecentSearches.adapter = recentSearchAdapter

        // Setup search results RecyclerView
        rvSearchResults.layoutManager = LinearLayoutManager(context)
        searchAdapter = SearchAdapter(requireContext(), ArrayList(), this)
        rvSearchResults.adapter = searchAdapter
    }

    private fun fetchUsers() {
        usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allUsers.clear()
                for (dataSnapshot in snapshot.children) {
                    val user = dataSnapshot.getValue(User::class.java)
                    user?.let {
                        // Don't include current user in search results
                        if (it.userId != currentUserId) {
                            allUsers.add(it)
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Error fetching users: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchRecentSearches() {
        recentSearchesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                recentSearches.clear()

                for (dataSnapshot in snapshot.children) {
                    val userId = dataSnapshot.key

                    // Get the user from allUsers
                    userId?.let { id ->
                        getUserById(id) { user ->
                            user?.let {
                                recentSearches.add(it)
                                recentSearchAdapter.updateList(recentSearches)

                                // Show/hide no searches message
                                if (recentSearches.isEmpty()) {
                                    tvNoSearches.visibility = View.VISIBLE
                                } else {
                                    tvNoSearches.visibility = View.GONE
                                }
                            }
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Error fetching recent searches: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getUserById(userId: String, callback: (User?) -> Unit) {
        usersRef.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                callback(user)
            }

            override fun onCancelled(error: DatabaseError) {
                callback(null)
            }
        })
    }

    private fun setupSearchListener() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Not needed
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val searchText = s.toString().trim()

                if (searchText.isEmpty()) {
                    // Show recent searches, hide search results
                    recentSearchesContainer.visibility = View.VISIBLE
                    searchResultsContainer.visibility = View.GONE
                } else {
                    // Show search results, hide recent searches
                    recentSearchesContainer.visibility = View.GONE
                    searchResultsContainer.visibility = View.VISIBLE

                    // Filter users based on search query
                    filterUsers(searchText)
                }
            }

            override fun afterTextChanged(s: Editable?) {
                // Not needed
            }
        })
    }

    private fun filterUsers(query: String) {
        filteredUsers.clear()

        val lowerCaseQuery = query.lowercase()
        for (user in allUsers) {
            if (user.username.lowercase().contains(lowerCaseQuery) ||
                user.fullName.lowercase().contains(lowerCaseQuery)) {
                filteredUsers.add(user)
            }
        }

        searchAdapter.updateList(filteredUsers)
    }

    private fun saveRecentSearch(user: User) {
        val timestamp = ServerValue.TIMESTAMP
        recentSearchesRef.child(user.userId).setValue(timestamp)
    }

    private fun removeRecentSearch(user: User) {
        recentSearchesRef.child(user.userId).removeValue()
    }

    private fun clearAllRecentSearches() {
        recentSearchesRef.removeValue()
        recentSearches.clear()
        recentSearchAdapter.updateList(recentSearches)
        tvNoSearches.visibility = View.VISIBLE
    }

    // SearchAdapter.OnUserClickListener implementation
    override fun onUserClick(user: User, position: Int) {
        // Save to recent searches
        saveRecentSearch(user)

        // Navigate to user profile or perform other actions
        // For example:
        // val intent = Intent(context, UserProfileActivity::class.java)
        // intent.putExtra("userId", user.userId)
        // startActivity(intent)

        Toast.makeText(context, "Clicked on ${user.username}", Toast.LENGTH_SHORT).show()
    }

    override fun onRemoveClick(user: User, position: Int) {
        // Remove from current search results
        filteredUsers.removeAt(position)
        searchAdapter.updateList(filteredUsers)
    }

    // RecentSearchAdapter.OnRecentSearchListener implementation
    override fun onRecentSearchClick(user: User, position: Int) {
        // Update timestamp for this recent search
        saveRecentSearch(user)

        // Navigate to user profile or perform other actions
        // Same as onUserClick

        Toast.makeText(context, "Clicked on recent search ${user.username}", Toast.LENGTH_SHORT).show()
    }

    override fun onRemoveRecentSearch(user: User, position: Int) {
        // Remove from recent searches in Firebase
        removeRecentSearch(user)

        // Remove from local list and update adapter
        recentSearches.removeAt(position)
        recentSearchAdapter.updateList(recentSearches)

        // Show no searches message if list is empty
        if (recentSearches.isEmpty()) {
            tvNoSearches.visibility = View.VISIBLE
        }
    }
}