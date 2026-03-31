package com.junaidjamshid.i211203.presentation.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowInsetsController
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.junaidjamshid.i211203.R
import com.junaidjamshid.i211203.presentation.home.HomeFragmentNew
import com.junaidjamshid.i211203.presentation.post.AddPostFragmentNew
import com.junaidjamshid.i211203.presentation.profile.ProfileFragmentNew
import com.junaidjamshid.i211203.presentation.reels.ReelsFragment
import com.junaidjamshid.i211203.presentation.search.SearchFragmentNew
import com.junaidjamshid.i211203.service.NotificationService
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main Activity with bottom navigation.
 * Clean Architecture implementation.
 */
@AndroidEntryPoint
class MainActivityNew : AppCompatActivity() {
    
    private val viewModel: MainViewModel by viewModels()
    
    private lateinit var bottomNav: BottomNavigationView
    
    private val homeFragment by lazy { HomeFragmentNew() }
    private val searchFragment by lazy { SearchFragmentNew() }
    private val addPostFragment by lazy { AddPostFragmentNew() }
    private val reelsFragment by lazy { ReelsFragment() }
    private val profileFragment by lazy { ProfileFragmentNew() }
    
    private var activeFragment: Fragment = homeFragment
    
    // Permission launcher for Android 13+ notification permission
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d("MainActivityNew", "Permission result: isGranted=$isGranted")
        if (isGranted) {
            // Permission granted, start notification service
            startNotificationService()
        } else {
            Log.w("MainActivityNew", "Notification permission denied by user")
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_new)
        
        setupSystemUI()
        setupBottomNavigation()
        setupFragments()
        
        // Request notification permission and start service
        requestNotificationPermissionAndStartService()
        
        // Handle navigation intent (e.g., from UserProfileActivity when viewing own profile)
        handleNavigationIntent(intent)
    }
    
    private fun requestNotificationPermissionAndStartService() {
        Log.d("MainActivityNew", "Checking notification permission...")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                    Log.d("MainActivityNew", "Notification permission already granted")
                    startNotificationService()
                }
                ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) -> {
                    // Show rationale if needed, then request
                    Log.d("MainActivityNew", "Showing permission rationale...")
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    // Request permission
                    Log.d("MainActivityNew", "Requesting notification permission...")
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // No permission needed for Android < 13
            Log.d("MainActivityNew", "Android < 13, no permission needed")
            startNotificationService()
        }
    }
    
    private fun startNotificationService() {
        Log.d("MainActivityNew", "Starting NotificationService...")
        NotificationService.start(this)
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNavigationIntent(intent)
    }
    
    private fun handleNavigationIntent(intent: Intent) {
        if (intent.getBooleanExtra("NAVIGATE_TO_OWN_PROFILE", false)) {
            // Delay slightly to ensure fragments are set up
            bottomNav.post {
                navigateToTab(R.id.nav_profile)
            }
        }
    }
    
    private fun setupSystemUI() {
        // Make status bar white with dark icons
        window.statusBarColor = ContextCompat.getColor(this, R.color.white)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.white)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsAppearance(
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            )
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = 
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
    }
    
    private fun setupFragments() {
        // Add all fragments but hide all except home
        supportFragmentManager.beginTransaction().apply {
            add(R.id.fragment_container, profileFragment, "profile").hide(profileFragment)
            add(R.id.fragment_container, reelsFragment, "reels").hide(reelsFragment)
            add(R.id.fragment_container, addPostFragment, "add_post").hide(addPostFragment)
            add(R.id.fragment_container, searchFragment, "search").hide(searchFragment)
            add(R.id.fragment_container, homeFragment, "home")
        }.commit()
    }
    
    private fun setupBottomNavigation() {
        bottomNav = findViewById(R.id.bottom_navigation)
        
        bottomNav.setOnItemSelectedListener { menuItem ->
            val fragment = when (menuItem.itemId) {
                R.id.nav_home -> homeFragment
                R.id.nav_search -> searchFragment
                R.id.nav_add_post -> addPostFragment
                R.id.nav_contacts -> reelsFragment  // Changed from contactsFragment to reelsFragment
                R.id.nav_profile -> profileFragment
                else -> return@setOnItemSelectedListener false
            }
            
            switchFragment(fragment)
            true
        }
    }
    
    private fun switchFragment(targetFragment: Fragment) {
        if (activeFragment != targetFragment) {
            supportFragmentManager.beginTransaction()
                .hide(activeFragment)
                .show(targetFragment)
                .commit()
            activeFragment = targetFragment
        }
    }
    
    /**
     * Navigate to specific tab programmatically
     */
    fun navigateToTab(tabId: Int) {
        bottomNav.selectedItemId = tabId
    }
    
    /**
     * Navigate to home tab and refresh feed
     */
    fun navigateToHomeAndRefresh() {
        navigateToTab(R.id.nav_home)
        (homeFragment as? HomeFragmentNew)?.refreshFeed()
    }
    
    override fun onBackPressed() {
        // If not on home tab, go to home tab
        if (activeFragment != homeFragment) {
            navigateToTab(R.id.nav_home)
        } else {
            super.onBackPressed()
        }
    }
}
