package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audio_history")
data class AudioHistoryItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val text: String,
    val filePath: String,
    val format: String, // "WAV" or "MP3"
    val fileSizeBytes: Long,
    val durationMs: Long,
    val engineName: String,
    val voiceName: String,
    val speechRate: Float,
    val pitch: Float,
    val createdAt: Long = System.currentTimeMillis()
)
