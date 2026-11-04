package com.example.data.repository

import com.example.data.local.AudioHistoryDao
import com.example.data.model.AudioHistoryItem
import kotlinx.coroutines.flow.Flow
import java.io.File

class AudioHistoryRepository(private val dao: AudioHistoryDao) {
    val allHistory: Flow<List<AudioHistoryItem>> = dao.getAllHistory()

    suspend fun insert(item: AudioHistoryItem): Long = dao.insert(item)

    suspend fun delete(item: AudioHistoryItem) {
        try {
            val file = File(item.filePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {}
        dao.delete(item)
    }

    suspend fun clearAll() {
        dao.clearAll()
    }
}
