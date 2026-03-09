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
     * Create an enhanced post with multiple images, location, and music.
     */
    fun createEnhancedPost() {
        val state = _addPostUiState.value
        if (state.selectedImageBytesList.isEmpty()) return

        viewModelScope.launch {
            _addPostUiState.update { it.copy(isLoading = true) }

            val result = postRepository.createPost(
                caption = state.caption,
                imageBytesList = state.selectedImageBytesList,
                location = state.location,
                musicName = state.musicName,
                musicArtist = state.musicArtist
            )

            when (result) {
                is Resource.Success -> {
                    _addPostUiState.update { s ->
                        s.copy(isLoading = false, postCreated = true, error = null)
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

    /**
     * Add images to the post (multi-image support)
     */
    fun addImages(imageBytesList: List<ByteArray>) {
        _addPostUiState.update { state ->
            val combined = state.selectedImageBytesList + imageBytesList
            // Instagram allows max 10 images per post
            val limited = combined.take(10)
            state.copy(
                selectedImageBytesList = limited,
                hasImages = limited.isNotEmpty()
            )
        }
    }

    /**
     * Remove an image at a specific index
     */
    fun removeImage(index: Int) {
        _addPostUiState.update { state ->
            val updated = state.selectedImageBytesList.toMutableList()
            if (index in updated.indices) {
                updated.removeAt(index)
            }
            val newPreviewIndex = if (state.currentPreviewIndex >= updated.size) {
                (updated.size - 1).coerceAtLeast(0)
            } else {
                state.currentPreviewIndex
            }
            state.copy(
                selectedImageBytesList = updated,
                currentPreviewIndex = newPreviewIndex,
                hasImages = updated.isNotEmpty()
            )
        }
    }

    /**
     * Set the current preview index for carousel
     */
    fun setPreviewIndex(index: Int) {
        _addPostUiState.update { it.copy(currentPreviewIndex = index) }
    }

    /**
     * Set location for the post
     */
    fun setLocation(location: String) {
        _addPostUiState.update { it.copy(location = location) }
    }

    /**
     * Set music info for the post
     */
    fun setMusic(name: String, artist: String) {
        _addPostUiState.update { it.copy(musicName = name, musicArtist = artist) }
    }

    /**
     * Clear music selection
     */
    fun clearMusic() {
        _addPostUiState.update { it.copy(musicName = "", musicArtist = "") }
    }

    /**
     * Clear location
     */
    fun clearLocation() {
        _addPostUiState.update { it.copy(location = "") }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
        _addPostUiState.update { it.copy(error = null) }
    }
    
    fun resetPostCreated() {
        _addPostUiState.update { it.copy(postCreated = false) }
    }

    /**
     * Full reset of the add post form
     */
    fun resetAddPostForm() {
        _addPostUiState.value = AddPostUiState()
    }
}
