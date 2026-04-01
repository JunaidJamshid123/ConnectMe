package com.junaidjamshid.i211203.presentation.highlight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.junaidjamshid.i211203.domain.model.HighlightStory
import com.junaidjamshid.i211203.domain.model.StoryHighlight
import com.junaidjamshid.i211203.domain.usecase.highlight.CreateHighlightUseCase
import com.junaidjamshid.i211203.domain.usecase.highlight.DeleteHighlightUseCase
import com.junaidjamshid.i211203.domain.usecase.highlight.GetHighlightUseCase
import com.junaidjamshid.i211203.domain.usecase.highlight.GetUserHighlightsUseCase
import com.junaidjamshid.i211203.domain.usecase.highlight.UpdateHighlightUseCase
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
 * ViewModel for Story Highlight screens.
 */
@HiltViewModel
class HighlightViewModel @Inject constructor(
    private val getUserHighlightsUseCase: GetUserHighlightsUseCase,
    private val getHighlightUseCase: GetHighlightUseCase,
    private val createHighlightUseCase: CreateHighlightUseCase,
    private val updateHighlightUseCase: UpdateHighlightUseCase,
    private val deleteHighlightUseCase: DeleteHighlightUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HighlightUiState())
    val uiState: StateFlow<HighlightUiState> = _uiState.asStateFlow()
    
    private var progressJob: Job? = null
    private val storyDuration = 5000L // 5 seconds per story
    
    /**
     * Load all highlights for a user
     */
    fun loadUserHighlights(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            when (val result = getUserHighlightsUseCase(userId)) {
                is Resource.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            highlights = result.data ?: emptyList(),
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
    
    /**
     * Load a specific highlight with its stories
     */
    fun loadHighlight(highlightId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            when (val result = getHighlightUseCase(highlightId)) {
                is Resource.Success -> {
                    val highlight = result.data
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            currentHighlight = highlight,
                            currentStory = highlight?.stories?.firstOrNull(),
                            currentStoryIndex = 0,
                            storyProgress = 0f,
                            error = null
                        )
                    }
                    if (highlight != null && highlight.stories.isNotEmpty()) {
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
     * Display a highlight directly (when we already have the data)
     */
    fun displayHighlight(highlight: StoryHighlight, startIndex: Int = 0) {
        _uiState.update { state ->
            state.copy(
                currentHighlight = highlight,
                currentStory = highlight.stories.getOrNull(startIndex),
                currentStoryIndex = startIndex,
                storyProgress = 0f
            )
        }
        startStoryProgress()
    }
    
    /**
     * Create a new highlight
     */
    fun createHighlight(
        name: String,
        coverImageBytes: ByteArray? = null,
        storyImageUrls: List<String>
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            when (val result = createHighlightUseCase(name, coverImageBytes, storyImageUrls)) {
                is Resource.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            highlightCreated = true,
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
    
    /**
     * Update a highlight
     */
    fun updateHighlight(
        highlightId: String,
        name: String? = null,
        coverImageBytes: ByteArray? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            when (val result = updateHighlightUseCase(highlightId, name, coverImageBytes)) {
                is Resource.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            highlightUpdated = true,
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
    
    /**
     * Delete a highlight
     */
    fun deleteHighlight(highlightId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            when (val result = deleteHighlightUseCase(highlightId)) {
                is Resource.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            highlightDeleted = true,
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
    
    /**
     * Move to next story in highlight
     */
    fun nextStory() {
        val currentIndex = _uiState.value.currentStoryIndex
        val stories = _uiState.value.currentHighlight?.stories ?: return
        
        if (currentIndex < stories.size - 1) {
            val nextIndex = currentIndex + 1
            _uiState.update { state ->
                state.copy(
                    currentStoryIndex = nextIndex,
                    currentStory = stories[nextIndex],
                    storyProgress = 0f
                )
            }
            startStoryProgress()
        } else {
            // No more stories, signal to close
            stopStoryProgress()
        }
    }
    
    /**
     * Move to previous story in highlight
     */
    fun previousStory() {
        val currentIndex = _uiState.value.currentStoryIndex
        val stories = _uiState.value.currentHighlight?.stories ?: return
        
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
     * Pause story progress
     */
    fun pauseStory() {
        progressJob?.cancel()
    }
    
    /**
     * Resume story progress
     */
    fun resumeStory() {
        startStoryProgress()
    }
    
    /**
     * Start the story progress animation
     */
    private fun startStoryProgress() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            val startProgress = _uiState.value.storyProgress
            val remainingTime = (storyDuration * (1 - startProgress)).toLong()
            val steps = 100
            val stepDuration = remainingTime / steps
            
            repeat(((1f - startProgress) * steps).toInt()) {
                delay(stepDuration)
                val newProgress = _uiState.value.storyProgress + (1f / steps)
                if (newProgress >= 1f) {
                    nextStory()
                    return@launch
                }
                _uiState.update { it.copy(storyProgress = newProgress) }
            }
            
            nextStory()
        }
    }
    
    /**
     * Stop story progress
     */
    private fun stopStoryProgress() {
        progressJob?.cancel()
    }
    
    /**
     * Clear error state
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    /**
     * Reset highlight created state
     */
    fun resetHighlightCreated() {
        _uiState.update { it.copy(highlightCreated = false) }
    }
    
    /**
     * Reset highlight deleted state
     */
    fun resetHighlightDeleted() {
        _uiState.update { it.copy(highlightDeleted = false) }
    }
    
    override fun onCleared() {
        super.onCleared()
        stopStoryProgress()
    }
}
