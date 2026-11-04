package com.example.player

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class AudioPlayerManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private var progressJob: Job? = null

    private val _currentTrackPath = MutableStateFlow<String?>(null)
    val currentTrackPath: StateFlow<String?> = _currentTrackPath.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _totalDurationMs = MutableStateFlow(0L)
    val totalDurationMs: StateFlow<Long> = _totalDurationMs.asStateFlow()

    fun togglePlayPause(filePath: String) {
        if (_currentTrackPath.value == filePath && mediaPlayer != null) {
            if (_isPlaying.value) {
                pause()
            } else {
                resume()
            }
        } else {
            play(filePath)
        }
    }

    fun play(filePath: String) {
        val file = File(filePath)
        if (!file.exists()) return

        stop()

        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(filePath)
                prepare()
            }

            mediaPlayer = player
            _currentTrackPath.value = filePath
            _totalDurationMs.value = player.duration.toLong().coerceAtLeast(0L)
            _currentPositionMs.value = 0L

            player.setOnCompletionListener {
                _isPlaying.value = false
                _currentPositionMs.value = 0L
                progressJob?.cancel()
            }

            player.start()
            _isPlaying.value = true
            startProgressTracker()
        } catch (e: Exception) {
            e.printStackTrace()
            stop()
        }
    }

    fun pause() {
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.pause()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _isPlaying.value = false
        progressJob?.cancel()
    }

    fun resume() {
        try {
            mediaPlayer?.let { player ->
                player.start()
                _isPlaying.value = true
                startProgressTracker()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun seekTo(positionMs: Long) {
        try {
            mediaPlayer?.let { player ->
                player.seekTo(positionMs.toInt())
                _currentPositionMs.value = positionMs
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stop() {
        progressJob?.cancel()
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            }
        } catch (_: Exception) {}
        mediaPlayer = null
        _isPlaying.value = false
        _currentTrackPath.value = null
        _currentPositionMs.value = 0L
        _totalDurationMs.value = 0L
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && _isPlaying.value) {
                try {
                    mediaPlayer?.let { player ->
                        _currentPositionMs.value = player.currentPosition.toLong()
                    }
                } catch (_: Exception) {}
                delay(100)
            }
        }
    }

    fun release() {
        stop()
    }
}
