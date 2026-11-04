package com.example.tts

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.UUID

class TtsEngineManager(private val context: Context) {

    private var tts: TextToSpeech? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _installedEngines = MutableStateFlow<List<EngineInfoItem>>(emptyList())
    val installedEngines: StateFlow<List<EngineInfoItem>> = _installedEngines.asStateFlow()

    private val _currentEngine = MutableStateFlow<EngineInfoItem?>(null)
    val currentEngine: StateFlow<EngineInfoItem?> = _currentEngine.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<VoiceInfoItem>>(emptyList())
    val availableVoices: StateFlow<List<VoiceInfoItem>> = _availableVoices.asStateFlow()

    private val _selectedVoice = MutableStateFlow<VoiceInfoItem?>(null)
    val selectedVoice: StateFlow<VoiceInfoItem?> = _selectedVoice.asStateFlow()

    private val _speechRate = MutableStateFlow(1.0f)
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    private val _pitch = MutableStateFlow(1.0f)
    val pitch: StateFlow<Float> = _pitch.asStateFlow()

    private val _isPreviewPlaying = MutableStateFlow(false)
    val isPreviewPlaying: StateFlow<Boolean> = _isPreviewPlaying.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _generationProgress = MutableStateFlow(0f)
    val generationProgress: StateFlow<Float> = _generationProgress.asStateFlow()

    private val _generationStatus = MutableStateFlow("")
    val generationStatus: StateFlow<String> = _generationStatus.asStateFlow()

    private var currentUtteranceDeferred: CompletableDeferred<Boolean>? = null
    private var isCancelled = false

    private val prefs = context.getSharedPreferences("bd_tts_prefs", Context.MODE_PRIVATE)

    init {
        val savedEngine = prefs.getString("selected_engine_package", null)
        initEngine(savedEngine)
    }

    /**
     * Initialize TextToSpeech with either a specific engine package or system default.
     */
    fun initEngine(enginePackage: String? = null) {
        _isInitialized.value = false
        tts?.shutdown()

        val listener = TextToSpeech.OnInitListener { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.let { currentTts ->
                    setupUtteranceListener(currentTts)
                    refreshEnginesAndVoices(currentTts, enginePackage)
                    applyRateAndPitch()
                    _isInitialized.value = true
                }
            } else {
                _isInitialized.value = false
                _generationStatus.value = "Failed to initialize TTS Engine ($status)"
            }
        }

        tts = if (enginePackage.isNullOrBlank()) {
            TextToSpeech(context.applicationContext, listener)
        } else {
            TextToSpeech(context.applicationContext, listener, enginePackage)
        }
    }

    private fun setupUtteranceListener(engine: TextToSpeech) {
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                if (utteranceId?.startsWith("preview_") == true) {
                    _isPreviewPlaying.value = true
                }
            }

            override fun onDone(utteranceId: String?) {
                if (utteranceId?.startsWith("preview_") == true) {
                    _isPreviewPlaying.value = false
                }
                currentUtteranceDeferred?.complete(true)
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                if (utteranceId?.startsWith("preview_") == true) {
                    _isPreviewPlaying.value = false
                }
                currentUtteranceDeferred?.complete(false)
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                if (utteranceId?.startsWith("preview_") == true) {
                    _isPreviewPlaying.value = false
                }
                currentUtteranceDeferred?.complete(false)
            }
        })
    }

    private fun refreshEnginesAndVoices(currentTts: TextToSpeech, activePackage: String?) {
        val defaultPkg = try {
            currentTts.defaultEngine ?: ""
        } catch (_: Exception) { "" }

        val activePkg = activePackage ?: defaultPkg

        val enginesList = mutableListOf<EngineInfoItem>()
        try {
            val systemEngines = currentTts.engines ?: emptyList()
            for (eng in systemEngines) {
                val isDef = eng.name.equals(defaultPkg, ignoreCase = true)
                val isActive = eng.name.equals(activePkg, ignoreCase = true)
                enginesList.add(
                    EngineInfoItem(
                        packageName = eng.name,
                        label = eng.label ?: eng.name,
                        isSystemDefault = isDef,
                        isCurrentActive = isActive
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (enginesList.none { it.packageName.equals(activePkg, ignoreCase = true) }) {
            enginesList.add(
                0,
                EngineInfoItem(
                    packageName = activePkg,
                    label = "Active Engine ($activePkg)",
                    isSystemDefault = activePkg == defaultPkg,
                    isCurrentActive = true
                )
            )
        }

        _installedEngines.value = enginesList
        _currentEngine.value = enginesList.firstOrNull { it.isCurrentActive }
            ?: enginesList.firstOrNull { it.isSystemDefault }
            ?: enginesList.firstOrNull()

        // Fetch voices
        val voicesList = mutableListOf<VoiceInfoItem>()
        try {
            val allVoices = currentTts.voices ?: emptySet()
            for (v in allVoices) {
                val locale = v.locale ?: Locale.getDefault()
                val lang = locale.language ?: ""
                val country = locale.country ?: ""
                val displayName = "${locale.displayLanguage} (${locale.displayCountry.ifBlank { lang }}) - ${v.name.takeLast(10)}"
                voicesList.add(
                    VoiceInfoItem(
                        name = v.name,
                        locale = locale,
                        displayName = displayName,
                        languageCode = lang,
                        countryCode = country,
                        quality = v.quality,
                        latency = v.latency,
                        isNetworkRequired = v.isNetworkConnectionRequired
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Sort voices: Bengali first, then English, then alphabetical
        val sortedVoices = voicesList.sortedWith(
            compareByDescending<VoiceInfoItem> { it.isBengali }
                .thenByDescending { it.languageCode == "en" }
                .thenBy { it.displayName }
        )
        _availableVoices.value = sortedVoices

        // Auto-select preferred voice or first Bengali/English voice
        val savedVoiceName = prefs.getString("selected_voice_name", null)
        val matchedVoice = sortedVoices.firstOrNull { it.name == savedVoiceName }
            ?: sortedVoices.firstOrNull { it.isBengali }
            ?: sortedVoices.firstOrNull { it.languageCode == "en" }
            ?: sortedVoices.firstOrNull()

        _selectedVoice.value = matchedVoice
        matchedVoice?.let { applyVoice(it.name) }
    }

    fun switchEngine(packageName: String) {
        prefs.edit().putString("selected_engine_package", packageName).apply()
        initEngine(packageName)
    }

    fun setSpeechRate(rate: Float) {
        val clamped = rate.coerceIn(0.5f, 2.0f)
        _speechRate.value = clamped
        tts?.setSpeechRate(clamped)
    }

    fun setPitch(pitchVal: Float) {
        val clamped = pitchVal.coerceIn(0.5f, 2.0f)
        _pitch.value = clamped
        tts?.setPitch(clamped)
    }

    fun selectVoice(voiceItem: VoiceInfoItem) {
        _selectedVoice.value = voiceItem
        prefs.edit().putString("selected_voice_name", voiceItem.name).apply()
        applyVoice(voiceItem.name)
    }

    private fun applyVoice(voiceName: String) {
        try {
            val voiceObj = tts?.voices?.firstOrNull { it.name == voiceName }
            if (voiceObj != null) {
                tts?.voice = voiceObj
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun applyRateAndPitch() {
        tts?.setSpeechRate(_speechRate.value)
        tts?.setPitch(_pitch.value)
    }

    fun previewSpeak(text: String) {
        if (!_isInitialized.value) return
        stopPreview()

        val previewSample = if (text.isNotBlank()) {
            text.take(150)
        } else {
            if (_selectedVoice.value?.isBengali == true) {
                "স্বাগতম! এটি বিডি টিটিএস ইঞ্জিনের একটি প্রিভিউ অডিও।"
            } else {
                "Welcome! This is a preview from BD TTS Engine."
            }
        }

        applyRateAndPitch()
        _selectedVoice.value?.let { applyVoice(it.name) }

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "preview_${UUID.randomUUID()}")
        }
        tts?.speak(previewSample, TextToSpeech.QUEUE_FLUSH, params, "preview_${UUID.randomUUID()}")
    }

    fun stopPreview() {
        tts?.stop()
        _isPreviewPlaying.value = false
    }

    fun cancelGeneration() {
        isCancelled = true
        tts?.stop()
        currentUtteranceDeferred?.complete(false)
        _isGenerating.value = false
        _generationStatus.value = "Generation cancelled"
    }

    /**
     * Splits long text into natural sentence chunks respecting sentence boundaries
     * such as Bengali dari (।), periods, exclamation marks, question marks, and newlines.
     * Guarantees chunks are safely under the ~4000 character engine limit for unlimited text synthesis!
     */
    fun splitTextIntoChunks(text: String, maxChunkLength: Int = 2000): List<String> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return emptyList()
        if (trimmed.length <= maxChunkLength) return listOf(trimmed)

        val chunks = mutableListOf<String>()
        // Split on Bengali dari, newline, exclamation, question mark, or period
        val sentenceRegex = Regex("(?<=[।!?.\n])\\s+")
        val rawSentences = trimmed.split(sentenceRegex).filter { it.isNotBlank() }

        var currentChunk = StringBuilder()
        for (sentence in rawSentences) {
            if (sentence.length > maxChunkLength) {
                // Large sentence without punctuation: split by commas or words
                if (currentChunk.isNotEmpty()) {
                    chunks.add(currentChunk.toString().trim())
                    currentChunk = StringBuilder()
                }
                val words = sentence.split(Regex("\\s+"))
                var subChunk = StringBuilder()
                for (word in words) {
                    if (subChunk.length + word.length + 1 > maxChunkLength) {
                        if (subChunk.isNotEmpty()) {
                            chunks.add(subChunk.toString().trim())
                            subChunk = StringBuilder()
                        }
                    }
                    if (subChunk.isNotEmpty()) subChunk.append(" ")
                    subChunk.append(word)
                }
                if (subChunk.isNotEmpty()) {
                    chunks.add(subChunk.toString().trim())
                }
            } else {
                if (currentChunk.length + sentence.length + 1 > maxChunkLength) {
                    chunks.add(currentChunk.toString().trim())
                    currentChunk = StringBuilder()
                }
                if (currentChunk.isNotEmpty()) currentChunk.append(" ")
                currentChunk.append(sentence)
            }
        }
        if (currentChunk.isNotEmpty()) {
            chunks.add(currentChunk.toString().trim())
        }
        return chunks
    }

    /**
     * Synthesizes full text to audio file (.wav or .mp3) without length limits.
     */
    suspend fun synthesizeTextToFile(
        text: String,
        outputFormat: String // "WAV" or "MP3"
    ): Result<File> = withContext(Dispatchers.IO) {
        if (!_isInitialized.value) {
            return@withContext Result.failure(IllegalStateException("TTS Engine is not initialized yet"))
        }

        isCancelled = false
        _isGenerating.value = true
        _generationProgress.value = 0.05f
        _generationStatus.value = "Analyzing text and preparing synthesis..."

        val chunks = splitTextIntoChunks(text)
        if (chunks.isEmpty()) {
            _isGenerating.value = false
            return@withContext Result.failure(IllegalArgumentException("Text cannot be empty"))
        }

        val audioDir = File(context.filesDir, "audio").apply { mkdirs() }
        val tempFiles = mutableListOf<File>()

        try {
            applyRateAndPitch()
            _selectedVoice.value?.let { applyVoice(it.name) }

            val totalChunks = chunks.size
            for ((index, chunk) in chunks.withIndex()) {
                if (isCancelled) {
                    throw IllegalStateException("Synthesis was cancelled")
                }

                val chunkProgress = (index.toFloat() / totalChunks) * 0.8f + 0.1f
                _generationProgress.value = chunkProgress
                _generationStatus.value = if (totalChunks == 1) {
                    "Generating audio stream..."
                } else {
                    "Synthesizing sentence ${index + 1} of $totalChunks (${(chunkProgress * 100).toInt()}%)..."
                }

                val tempFile = File(audioDir, "temp_${System.currentTimeMillis()}_chunk_$index.wav")
                tempFiles.add(tempFile)

                val deferred = CompletableDeferred<Boolean>()
                currentUtteranceDeferred = deferred

                val utteranceId = "synth_${System.currentTimeMillis()}_$index"
                val params = Bundle().apply {
                    putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
                }

                val res = tts?.synthesizeToFile(chunk, params, tempFile, utteranceId)
                if (res != TextToSpeech.SUCCESS) {
                    throw IllegalStateException("TTS engine failed to start synthesis for chunk $index")
                }

                val success = deferred.await()
                if (!success || !tempFile.exists() || tempFile.length() == 0L) {
                    throw IllegalStateException("Audio generation failed for chunk ${index + 1}")
                }
            }

            _generationProgress.value = 0.90f
            _generationStatus.value = "Assembling master audio..."

            val timeStamp = System.currentTimeMillis()
            val finalExtension = if (outputFormat.equals("MP3", ignoreCase = true)) "mp3" else "wav"
            val finalFile = File(audioDir, "BD_TTS_${timeStamp}.$finalExtension")

            // Merge temp WAV chunks
            val mergedSuccess = WavHelper.mergeWavFiles(tempFiles, finalFile)
            if (!mergedSuccess || !finalFile.exists()) {
                throw IllegalStateException("Failed to compile audio into final file")
            }

            _generationProgress.value = 1.0f
            _generationStatus.value = "Complete! Audio ready."
            _isGenerating.value = false

            Result.success(finalFile)
        } catch (e: Exception) {
            _isGenerating.value = false
            _generationStatus.value = "Error: ${e.localizedMessage ?: "Unknown error"}"
            Result.failure(e)
        } finally {
            // Cleanup temp files
            for (f in tempFiles) {
                try {
                    if (f.exists()) f.delete()
                } catch (_: Exception) {}
            }
        }
    }

    /**
     * Intent to open Android's system Text-To-Speech settings.
     */
    fun openSystemTtsSettings(): Intent {
        return Intent("com.android.settings.TTS_SETTINGS").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
