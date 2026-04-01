package com.junaidjamshid.i211203.util

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.IOException

/**
 * Helper class for recording and playing voice messages.
 */
class VoiceRecorderHelper(private val context: Context) {
    
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var audioFile: File? = null
    private var recordingStartTime: Long = 0
    
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    
    private val _recordingDuration = MutableStateFlow(0L)
    val recordingDuration: StateFlow<Long> = _recordingDuration.asStateFlow()
    
    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress: StateFlow<Float> = _playbackProgress.asStateFlow()
    
    /**
     * Start recording voice
     */
    @Throws(IOException::class)
    fun startRecording(): File {
        val outputDir = context.cacheDir
        audioFile = File.createTempFile("voice_", ".3gp", outputDir)
        
        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(audioFile?.absolutePath)
            
            prepare()
            start()
        }
        
        recordingStartTime = System.currentTimeMillis()
        _isRecording.value = true
        
        return audioFile!!
    }
    
    /**
     * Stop recording and return the recorded file
     */
    fun stopRecording(): Pair<File?, Long>? {
        if (!_isRecording.value) return null
        
        val duration = System.currentTimeMillis() - recordingStartTime
        
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        mediaRecorder = null
        _isRecording.value = false
        _recordingDuration.value = duration
        
        return Pair(audioFile, duration)
    }
    
    /**
     * Cancel recording and delete the file
     */
    fun cancelRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            // Ignore - recorder may not have started
        }
        
        mediaRecorder = null
        audioFile?.delete()
        audioFile = null
        _isRecording.value = false
        _recordingDuration.value = 0
    }
    
    /**
     * Get current recording amplitude (for waveform visualization)
     */
    fun getAmplitude(): Int {
        return try {
            mediaRecorder?.maxAmplitude ?: 0
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * Play a voice message from URL
     */
    fun playVoice(url: String, onComplete: () -> Unit = {}) {
        stopPlayback()
        
        mediaPlayer = MediaPlayer().apply {
            setDataSource(url)
            setOnPreparedListener {
                start()
                _isPlaying.value = true
            }
            setOnCompletionListener {
                _isPlaying.value = false
                _playbackProgress.value = 0f
                onComplete()
            }
            setOnErrorListener { _, _, _ ->
                _isPlaying.value = false
                true
            }
            prepareAsync()
        }
    }
    
    /**
     * Pause playback
     */
    fun pausePlayback() {
        mediaPlayer?.pause()
        _isPlaying.value = false
    }
    
    /**
     * Resume playback
     */
    fun resumePlayback() {
        mediaPlayer?.start()
        _isPlaying.value = true
    }
    
    /**
     * Stop playback
     */
    fun stopPlayback() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
        _isPlaying.value = false
        _playbackProgress.value = 0f
    }
    
    /**
     * Get playback position (0-1)
     */
    fun getPlaybackPosition(): Float {
        return try {
            val player = mediaPlayer ?: return 0f
            player.currentPosition.toFloat() / player.duration.toFloat()
        } catch (e: Exception) {
            0f
        }
    }
    
    /**
     * Seek to position
     */
    fun seekTo(position: Float) {
        try {
            val player = mediaPlayer ?: return
            val seekPos = (position * player.duration).toInt()
            player.seekTo(seekPos)
        } catch (e: Exception) {
            // Ignore
        }
    }
    
    /**
     * Get the current playback duration formatted
     */
    fun getCurrentDuration(): String {
        return try {
            val player = mediaPlayer ?: return "0:00"
            val current = player.currentPosition / 1000
            val minutes = current / 60
            val seconds = current % 60
            String.format("%d:%02d", minutes, seconds)
        } catch (e: Exception) {
            "0:00"
        }
    }
    
    /**
     * Get the total duration formatted
     */
    fun getTotalDuration(): String {
        return try {
            val player = mediaPlayer ?: return "0:00"
            val total = player.duration / 1000
            val minutes = total / 60
            val seconds = total % 60
            String.format("%d:%02d", minutes, seconds)
        } catch (e: Exception) {
            "0:00"
        }
    }
    
    /**
     * Release all resources
     */
    fun release() {
        cancelRecording()
        stopPlayback()
    }
    
    companion object {
        /**
         * Format milliseconds to mm:ss
         */
        fun formatDuration(millis: Long): String {
            val seconds = (millis / 1000).toInt()
            val minutes = seconds / 60
            val secs = seconds % 60
            return String.format("%d:%02d", minutes, secs)
        }
    }
}
