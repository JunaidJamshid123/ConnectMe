package com.junaidjamshid.i211203.presentation.main

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsetsController
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.junaidjamshid.i211203.R
import com.junaidjamshid.i211203.presentation.contacts.ContactsFragmentNew
import com.junaidjamshid.i211203.presentation.home.HomeFragmentNew
import com.junaidjamshid.i211203.presentation.post.AddPostFragmentNew
import com.junaidjamshid.i211203.presentation.profile.ProfileFragmentNew
import com.junaidjamshid.i211203.presentation.search.SearchFragmentNew
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
    private val contactsFragment by lazy { ContactsFragmentNew() }
    private val profileFragment by lazy { ProfileFragmentNew() }
    
    private var activeFragment: Fragment = homeFragment
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_new)
        
        setupSystemUI()
        setupBottomNavigation()
        setupFragments()
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
            add(R.id.fragment_container, contactsFragment, "contacts").hide(contactsFragment)
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
                R.id.nav_contacts -> contactsFragment
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
