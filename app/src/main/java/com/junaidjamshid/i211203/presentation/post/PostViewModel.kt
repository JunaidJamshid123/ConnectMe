package com.junaidjamshid.i211203.presentation.post

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.junaidjamshid.i211203.domain.repository.AuthRepository
import com.junaidjamshid.i211203.domain.repository.PostRepository
import com.junaidjamshid.i211203.domain.repository.UserRepository
import com.junaidjamshid.i211203.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Post-related screens.
 */
@HiltViewModel
class PostViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PostUiState())
    val uiState: StateFlow<PostUiState> = _uiState.asStateFlow()
    
    private val _addPostUiState = MutableStateFlow(AddPostUiState())
    val addPostUiState: StateFlow<AddPostUiState> = _addPostUiState.asStateFlow()
    
    /**
     * Load post details
     */
    fun loadPost(postId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            postRepository.getPost(postId).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _uiState.update { state ->
                            state.copy(
                                isLoading = false,
                                post = result.data,
                                error = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update { 
                            it.copy(isLoading = false, error = result.message) 
                        }
                    }
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                }
            }
        }
    }
    
    /**
     * Load comments for a post
     */
    fun loadComments(postId: String) {
        viewModelScope.launch {
            postRepository.getComments(postId).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _uiState.update { state ->
                            state.copy(comments = result.data ?: emptyList())
                        }
                    }
                    else -> { /* Handle other states */ }
                }
            }
        }
    }
    
    /**
     * Create a new post
     */
    fun createPost(caption: String, imageBytes: ByteArray) {
        viewModelScope.launch {
            _addPostUiState.update { it.copy(isLoading = true) }
            
            val result = postRepository.createPost(caption, imageBytes)
            
            when (result) {
                is Resource.Success -> {
                    _addPostUiState.update { state ->
                        state.copy(
                            isLoading = false,
                            postCreated = true,
                            error = null
                        )
                    }
                }
                is Resource.Error -> {
                    _addPostUiState.update { 
                        it.copy(isLoading = false, error = result.message) 
                    }
                }
                else -> { /* Loading state */ }
            }
        }
    }
    
    /**
     * Like a post
     */
    fun likePost(postId: String) {
        viewModelScope.launch {
            postRepository.likePost(postId)
        }
    }
    
    /**
     * Unlike a post
     */
    fun unlikePost(postId: String) {
        viewModelScope.launch {
            postRepository.unlikePost(postId)
        }
    }
    
    /**
     * Toggle like state on a post
     */
    fun toggleLike(postId: String) {
        val currentPost = _uiState.value.post
        if (currentPost?.isLikedByCurrentUser == true) {
            unlikePost(postId)
        } else {
            likePost(postId)
        }
    }
    
    /**
     * Add a comment to a post
     */
    fun addComment(postId: String, commentText: String) {
        viewModelScope.launch {
            postRepository.addComment(postId, commentText)
        }
    }
    
    /**
     * Delete a post
     */
    fun deletePost(postId: String) {
        viewModelScope.launch {
            val result = postRepository.deletePost(postId)
            when (result) {
                is Resource.Success -> {
                    _uiState.update { it.copy(post = null) }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(error = result.message) }
                }
                else -> { /* Loading state */ }
            }
        }
    }
    
    /**
     * Update selected image for new post
     */
    fun setSelectedImage(uri: String) {
        _addPostUiState.update { it.copy(selectedImageUri = uri) }
    }
    
    /**
     * Update caption for new post
     */
    fun setCaption(caption: String) {
        _addPostUiState.update { it.copy(caption = caption) }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
        _addPostUiState.update { it.copy(error = null) }
    }
    
    fun resetPostCreated() {
        _addPostUiState.update { it.copy(postCreated = false) }
    }
}
