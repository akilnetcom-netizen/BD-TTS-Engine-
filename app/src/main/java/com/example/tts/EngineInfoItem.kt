package com.example.tts

data class EngineInfoItem(
    val packageName: String,
    val label: String,
    val isSystemDefault: Boolean = false,
    val isCurrentActive: Boolean = false
)
