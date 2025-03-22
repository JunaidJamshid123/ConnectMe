package com.junaidjamshid.i211203

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast

class ProfileFragment : Fragment() {
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find views
        val editProfileButton = view.findViewById<ImageView>(R.id.edit_profile)
        val logoutButton = view.findViewById<ImageView>(R.id.logout)

        // Set click listener for edit profile button
        editProfileButton.setOnClickListener {
            navigateToEditProfileActivity()
        }

        // Set click listener for logout button
        logoutButton.setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }

    // Function to navigate to the EditProfileActivity
    private fun navigateToEditProfileActivity() {
        val intent = Intent(requireActivity(), EditProfile::class.java)
        // You can pass current profile data if needed
        // intent.putExtra("username", "Junaid Jamshid")
        // intent.putExtra("bio", "two bananas for a pound, three bananas for a euro")
        startActivity(intent)
    }

    // Function to show logout confirmation dialog
    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { dialog, _ ->
                performLogout()
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    // Function to perform the actual logout
    private fun performLogout() {
        // Assuming you're using Firebase Auth or a similar service
        try {
            // Example with Firebase Auth
            // FirebaseAuth.getInstance().signOut()

            // For demonstration purposes, just show a toast
            Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()

            // Navigate to login activity
            val intent = Intent(requireActivity(), LoginScreem::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Logout failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val ARG_PARAM1 = "param1"
        private const val ARG_PARAM2 = "param2"

        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ProfileFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}