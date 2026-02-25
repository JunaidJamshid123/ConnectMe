package com.junaidjamshid.i211203.presentation.story

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.junaidjamshid.i211203.domain.model.Story
import com.junaidjamshid.i211203.domain.repository.StoryRepository
import com.junaidjamshid.i211203.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Story screens.
 */
@HiltViewModel
class StoryViewModel @Inject constructor(
    private val storyRepository: StoryRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(StoryUiState())
    val uiState: StateFlow<StoryUiState> = _uiState.asStateFlow()
    
    private var progressJob: Job? = null
    private val storyDuration = 5000L // 5 seconds per story
    
    /**
     * Load stories for a user
     */
    fun loadUserStories(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            when (val result = storyRepository.getUserStories(userId)) {
                is Resource.Success -> {
                    val stories = result.data ?: emptyList()
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            stories = stories,
                            currentStory = stories.firstOrNull(),
                            currentStoryIndex = 0,
                            error = null
                        )
                    }
                    if (stories.isNotEmpty()) {
                        startStoryProgress()
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
    
    /**
     * Start displaying a story at specific index
     */
    fun displayStory(stories: List<Story>, startIndex: Int = 0) {
        _uiState.update { state ->
            state.copy(
                stories = stories,
                currentStory = stories.getOrNull(startIndex),
                currentStoryIndex = startIndex,
                storyProgress = 0f
            )
        }
        startStoryProgress()
    }
    
    /**
     * Move to next story
     */
    fun nextStory() {
        val currentIndex = _uiState.value.currentStoryIndex
        val stories = _uiState.value.stories
        
        if (currentIndex < stories.size - 1) {
            val nextIndex = currentIndex + 1
            _uiState.update { state ->
                state.copy(
                    currentStoryIndex = nextIndex,
                    currentStory = stories[nextIndex],
                    storyProgress = 0f
                )
            }
            markCurrentStoryAsViewed()
            startStoryProgress()
        } else {
            // No more stories, close
            stopStoryProgress()
        }
    }
    
    /**
     * Move to previous story
     */
    fun previousStory() {
        val currentIndex = _uiState.value.currentStoryIndex
        val stories = _uiState.value.stories
        
        if (currentIndex > 0) {
            val prevIndex = currentIndex - 1
            _uiState.update { state ->
                state.copy(
                    currentStoryIndex = prevIndex,
                    currentStory = stories[prevIndex],
                    storyProgress = 0f
                )
            }
            startStoryProgress()
        }
    }
    
    /**
     * Create a new story
     */
    fun createStory(imageBytes: ByteArray) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val result = storyRepository.createStory(imageBytes)
            
            when (result) {
                is Resource.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            storyCreated = true,
                            error = null
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update { 
                        it.copy(isLoading = false, error = result.message) 
                    }
                }
                else -> { /* Loading state */ }
            }
        }
    }
    
    /**
     * Mark current story as viewed
     */
    private fun markCurrentStoryAsViewed() {
        val story = _uiState.value.currentStory ?: return
        viewModelScope.launch {
            storyRepository.markStoryAsViewed(story.storyId)
        }
    }
    
    /**
     * Start story progress timer
     */
    private fun startStoryProgress() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            val steps = 100
            val stepDuration = storyDuration / steps
            
            for (i in 0..steps) {
                _uiState.update { it.copy(storyProgress = i.toFloat() / steps) }
                delay(stepDuration)
            }
            
            // Auto advance to next story
            nextStory()
        }
    }
    
    /**
     * Stop story progress
     */
    private fun stopStoryProgress() {
        progressJob?.cancel()
        progressJob = null
    }
    
    /**
     * Pause story progress
     */
    fun pauseProgress() {
        progressJob?.cancel()
    }
    
    /**
     * Resume story progress
     */
    fun resumeProgress() {
        startStoryProgress()
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun resetStoryCreated() {
        _uiState.update { it.copy(storyCreated = false) }
    }
    
    override fun onCleared() {
        super.onCleared()
        stopStoryProgress()
    }
}
