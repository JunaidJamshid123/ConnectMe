package com.junaidjamshid.i211203.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.junaidjamshid.i211203.domain.usecase.auth.GetCurrentUserUseCase
import com.junaidjamshid.i211203.domain.usecase.post.GetFeedPostsUseCase
import com.junaidjamshid.i211203.domain.usecase.post.LikePostUseCase
import com.junaidjamshid.i211203.domain.usecase.post.UnlikePostUseCase
import com.junaidjamshid.i211203.domain.usecase.story.GetStoriesUseCase
import com.junaidjamshid.i211203.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Home screen following Clean Architecture.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getFeedPostsUseCase: GetFeedPostsUseCase,
    private val getStoriesUseCase: GetStoriesUseCase,
    private val likePostUseCase: LikePostUseCase,
    private val unlikePostUseCase: UnlikePostUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    private val currentUserId: String?
        get() = getCurrentUserUseCase.getCurrentUserId()
    
    init {
        loadCurrentUser()
        loadPosts()
        loadStories()
    }
    
    private fun loadCurrentUser() {
        viewModelScope.launch {
            when (val result = getCurrentUserUseCase()) {
                is Resource.Success -> {
                    _uiState.update { it.copy(currentUser = result.data) }
                }
                is Resource.Error -> {
                    // Handle error if needed
                }
                is Resource.Loading -> {
                    // Handle loading if needed
                }
            }
        }
    }
    
    private fun loadPosts() {
        currentUserId?.let { userId ->
            viewModelScope.launch {
                _uiState.update { it.copy(isLoadingPosts = true, postsError = null) }
                
                getFeedPostsUseCase(userId).collect { result ->
                    when (result) {
                        is Resource.Success -> {
                            _uiState.update { 
                                it.copy(
                                    posts = result.data ?: emptyList(),
                                    isLoadingPosts = false,
                                    isRefreshing = false
                                ) 
                            }
                        }
                        is Resource.Error -> {
                            _uiState.update { 
                                it.copy(
                                    postsError = result.message,
                                    isLoadingPosts = false,
                                    isRefreshing = false
                                ) 
                            }
                        }
                        is Resource.Loading -> {
                            _uiState.update { it.copy(isLoadingPosts = true) }
                        }
                    }
                }
            }
        }
    }
    
    private fun loadStories() {
        currentUserId?.let { userId ->
            viewModelScope.launch {
                _uiState.update { it.copy(isLoadingStories = true, storiesError = null) }
                
                getStoriesUseCase(userId).collect { result ->
                    when (result) {
                        is Resource.Success -> {
                            _uiState.update { 
                                it.copy(
                                    stories = result.data ?: emptyList(),
                                    isLoadingStories = false
                                ) 
                            }
                        }
                        is Resource.Error -> {
                            _uiState.update { 
                                it.copy(
                                    storiesError = result.message,
                                    isLoadingStories = false
                                ) 
                            }
                        }
                        is Resource.Loading -> {
                            _uiState.update { it.copy(isLoadingStories = true) }
                        }
                    }
                }
            }
        }
    }
    
    fun onRefresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        loadPosts()
        loadStories()
    }
    
    fun onLikePost(postId: String) {
        currentUserId?.let { userId ->
            viewModelScope.launch {
                val post = _uiState.value.posts.find { it.postId == postId }
                if (post?.isLikedByCurrentUser == true) {
                    unlikePostUseCase(postId, userId)
                } else {
                    likePostUseCase(postId, userId)
                }
            }
        }
    }
    
    fun onSavePost(postId: String) {
        // Save post functionality - can be extended later
        currentUserId?.let { userId ->
            viewModelScope.launch {
                // TODO: Implement save post use case
            }
        }
    }
}
