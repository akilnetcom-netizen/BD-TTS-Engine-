package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.AudioHistoryItem
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioHistoryDao {
    @Query("SELECT * FROM audio_history ORDER BY createdAt DESC")
    fun getAllHistory(): Flow<List<AudioHistoryItem>>

    @Query("SELECT * FROM audio_history WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): AudioHistoryItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: AudioHistoryItem): Long

    @Delete
    suspend fun delete(item: AudioHistoryItem)

    @Query("DELETE FROM audio_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM audio_history")
    suspend fun clearAll()
}
