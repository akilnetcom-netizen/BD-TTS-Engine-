package com.example.tts

import java.util.Locale

data class VoiceInfoItem(
    val name: String,
    val locale: Locale,
    val displayName: String,
    val languageCode: String,
    val countryCode: String,
    val quality: Int,
    val latency: Int,
    val isNetworkRequired: Boolean,
    val isBengali: Boolean = languageCode.equals("bn", ignoreCase = true)
)
