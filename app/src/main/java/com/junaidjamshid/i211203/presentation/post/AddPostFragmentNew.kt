package com.junaidjamshid.i211203.presentation.post

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.junaidjamshid.i211203.R
import com.junaidjamshid.i211203.databinding.FragmentPostBinding
import com.junaidjamshid.i211203.presentation.main.MainActivityNew
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

/**
 * Clean Architecture Add Post Fragment - Instagram style.
 */
@AndroidEntryPoint
class AddPostFragmentNew : Fragment() {
    
    private var _binding: FragmentPostBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: PostViewModel by viewModels()
    
    private var selectedImageBytes: ByteArray? = null
    
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                try {
                    val bitmap = MediaStore.Images.Media.getBitmap(
                        requireContext().contentResolver, uri
                    )
                    binding.postImage.setImageBitmap(bitmap)
                    binding.postImage.visibility = View.VISIBLE
                    binding.selectImageButton.visibility = View.GONE
                    
                    val stream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                    selectedImageBytes = stream.toByteArray()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Failed to load image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        observeUiState()
    }
    
    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            // Navigate back to home tab
            (activity as? MainActivityNew)?.navigateToTab(R.id.nav_home)
        }
        
        binding.selectImageButton.setOnClickListener {
            openGallery()
        }
        
        binding.imagePlaceholder.setOnClickListener {
            openGallery()
        }
        
        binding.share.setOnClickListener {
            sharePost()
        }
    }
    
    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.addPostUiState.collect { state ->
                    handleUiState(state)
                }
            }
        }
    }
    
    private fun handleUiState(state: AddPostUiState) {
        // Show/hide loading
        binding.loadingOverlay.isVisible = state.isLoading
        binding.share.isEnabled = !state.isLoading
        binding.share.alpha = if (state.isLoading) 0.5f else 1.0f
        
        // Handle post created
        if (state.postCreated) {
            Toast.makeText(requireContext(), "Post shared successfully!", Toast.LENGTH_SHORT).show()
            viewModel.resetPostCreated()
            resetForm()
            // Navigate to home and refresh feed
            (activity as? MainActivityNew)?.navigateToHomeAndRefresh()
        }
        
        // Handle error
        state.error?.let { error ->
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }
    
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }
    
    private fun sharePost() {
        val caption = binding.captionInput.text.toString().trim()
        
        if (selectedImageBytes == null) {
            Toast.makeText(requireContext(), "Please select an image", Toast.LENGTH_SHORT).show()
            return
        }
        
        viewModel.createPost(caption, selectedImageBytes!!)
    }
    
    private fun resetForm() {
        binding.captionInput.text?.clear()
        binding.postImage.setImageDrawable(null)
        binding.postImage.visibility = View.GONE
        binding.selectImageButton.visibility = View.VISIBLE
        selectedImageBytes = null
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
