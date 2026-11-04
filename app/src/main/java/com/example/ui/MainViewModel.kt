package com.example.ui

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.AudioHistoryItem
import com.example.data.repository.AudioHistoryRepository
import com.example.player.AudioPlayerManager
import com.example.tts.EngineInfoItem
import com.example.tts.TtsEngineManager
import com.example.tts.VoiceInfoItem
import com.example.tts.WavHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = AudioHistoryRepository(db.audioHistoryDao())

    val ttsManager = TtsEngineManager(application)
    val playerManager = AudioPlayerManager(application)

    // Generator Input State
    private val _inputText = MutableStateFlow(
        "স্বাগতম! বিডি টিটিএস ইঞ্জিন দিয়ে যেকোনো টেক্সটকে নিখুঁত অডিওতে রূপান্তর করুন।"
    )
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _outputFormat = MutableStateFlow("WAV") // "WAV" or "MP3"
    val outputFormat: StateFlow<String> = _outputFormat.asStateFlow()

    private val _latestGeneratedItem = MutableStateFlow<AudioHistoryItem?>(null)
    val latestGeneratedItem: StateFlow<AudioHistoryItem?> = _latestGeneratedItem.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    // History Flow from Room DB
    val historyItems: StateFlow<List<AudioHistoryItem>> = repository.allHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun setInputText(text: String) {
        _inputText.value = text
    }

    fun setOutputFormat(format: String) {
        _outputFormat.value = format.uppercase()
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun switchEngine(packageName: String) {
        ttsManager.switchEngine(packageName)
        _userMessage.value = "টিটিএস মডেল সফলভাবে পরিবর্তন হয়েছে।"
    }

    fun setSpeechRate(rate: Float) {
        ttsManager.setSpeechRate(rate)
    }

    fun setPitch(pitch: Float) {
        ttsManager.setPitch(pitch)
    }

    fun selectVoice(voice: VoiceInfoItem) {
        ttsManager.selectVoice(voice)
    }

    fun previewVoice() {
        ttsManager.previewSpeak(_inputText.value)
    }

    fun stopPreviewVoice() {
        ttsManager.stopPreview()
    }

    fun generateAudio() {
        val textToSpeak = _inputText.value.trim()
        if (textToSpeak.isBlank()) {
            _userMessage.value = "অনুগ্রহ করে কিছু টেক্সট লিখুন।"
            return
        }

        viewModelScope.launch {
            val format = _outputFormat.value
            val result = ttsManager.synthesizeTextToFile(textToSpeak, format)

            result.onSuccess { audioFile ->
                val durationMs = WavHelper.getAudioDurationMs(audioFile)
                val fileSizeBytes = audioFile.length()
                val title = if (textToSpeak.length > 35) {
                    textToSpeak.take(35) + "..."
                } else {
                    textToSpeak
                }

                val engineLabel = ttsManager.currentEngine.value?.label ?: "Default TTS"
                val voiceLabel = ttsManager.selectedVoice.value?.displayName ?: "Default Voice"

                val historyItem = AudioHistoryItem(
                    title = title,
                    text = textToSpeak,
                    filePath = audioFile.absolutePath,
                    format = format,
                    fileSizeBytes = fileSizeBytes,
                    durationMs = durationMs,
                    engineName = engineLabel,
                    voiceName = voiceLabel,
                    speechRate = ttsManager.speechRate.value,
                    pitch = ttsManager.pitch.value,
                    createdAt = System.currentTimeMillis()
                )

                val id = repository.insert(historyItem)
                val savedItem = historyItem.copy(id = id)
                _latestGeneratedItem.value = savedItem
                _userMessage.value = "অডিও সফলভাবে তৈরি হয়েছে! ($format)"

                // Auto-play the generated audio
                playerManager.play(audioFile.absolutePath)
            }.onFailure { error ->
                _userMessage.value = "অডিও তৈরিতে ত্রুটি: ${error.localizedMessage ?: "অজানা সমস্যা"}"
            }
        }
    }

    fun cancelGeneration() {
        ttsManager.cancelGeneration()
    }

    fun deleteHistoryItem(item: AudioHistoryItem) {
        viewModelScope.launch {
            if (playerManager.currentTrackPath.value == item.filePath) {
                playerManager.stop()
            }
            if (_latestGeneratedItem.value?.id == item.id) {
                _latestGeneratedItem.value = null
            }
            repository.delete(item)
            _userMessage.value = "অডিও মুছে ফেলা হয়েছে।"
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            playerManager.stop()
            _latestGeneratedItem.value = null
            repository.clearAll()
            _userMessage.value = "সব হিস্টোরি মুছে ফেলা হয়েছে।"
        }
    }

    fun shareAudio(context: Context, item: AudioHistoryItem) {
        try {
            val file = File(item.filePath)
            if (!file.exists()) {
                Toast.makeText(context, "Audio file not found on device", Toast.LENGTH_SHORT).show()
                return
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val mimeType = if (item.format.equals("WAV", ignoreCase = true)) "audio/wav" else "audio/mpeg"

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, item.title)
                putExtra(Intent.EXTRA_TEXT, "Generated with BD TTS Engine:\n${item.title}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share Audio via")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Sharing failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun saveToMusicDirectory(context: Context, item: AudioHistoryItem) {
        viewModelScope.launch {
            try {
                val sourceFile = File(item.filePath)
                if (!sourceFile.exists()) {
                    Toast.makeText(context, "Source file missing", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val ext = if (item.format.equals("MP3", ignoreCase = true)) "mp3" else "wav"
                val mime = if (ext == "mp3") "audio/mpeg" else "audio/wav"
                val filename = "BD_TTS_${System.currentTimeMillis()}.$ext"

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Audio.Media.DISPLAY_NAME, filename)
                        put(MediaStore.Audio.Media.MIME_TYPE, mime)
                        put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/BD_TTS")
                        put(MediaStore.Audio.Media.IS_PENDING, 1)
                    }

                    val resolver = context.contentResolver
                    val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
                    if (uri != null) {
                        resolver.openOutputStream(uri)?.use { out ->
                            FileInputStream(sourceFile).use { input ->
                                input.copyTo(out)
                            }
                        }
                        values.clear()
                        values.put(MediaStore.Audio.Media.IS_PENDING, 0)
                        resolver.update(uri, values, null, null)
                        Toast.makeText(context, "Saved to Music/BD_TTS/$filename", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Could not create file in MediaStore", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                    val targetDir = File(musicDir, "BD_TTS").apply { mkdirs() }
                    val targetFile = File(targetDir, filename)
                    sourceFile.copyTo(targetFile, overwrite = true)
                    Toast.makeText(context, "Saved to Music/BD_TTS/$filename", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Save error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
        ttsManager.shutdown()
    }
}
