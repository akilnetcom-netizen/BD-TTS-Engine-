package com.example.tts

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

object WavHelper {

    /**
     * Retrieves audio duration in milliseconds using MediaMetadataRetriever,
     * falling back to WAV header PCM duration calculation.
     */
    fun getAudioDurationMs(file: File): Long {
        if (!file.exists() || file.length() == 0L) return 0L
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            durationStr?.toLongOrNull() ?: calculateWavDurationMs(file)
        } catch (_: Exception) {
            calculateWavDurationMs(file)
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }

    private fun calculateWavDurationMs(file: File): Long {
        return try {
            FileInputStream(file).use { fis ->
                val header = ByteArray(44)
                val read = fis.read(header)
                if (read >= 44 && String(header, 0, 4) == "RIFF") {
                    val channels = ByteBuffer.wrap(header, 22, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt()
                    val sampleRate = ByteBuffer.wrap(header, 24, 4).order(ByteOrder.LITTLE_ENDIAN).int.toLong()
                    val bitsPerSample = ByteBuffer.wrap(header, 34, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt()
                    val bytesPerSecond = sampleRate * channels * (bitsPerSample / 8)
                    val dataSize = file.length() - 44
                    if (bytesPerSecond > 0) {
                        (dataSize * 1000L) / bytesPerSecond
                    } else 0L
                } else 0L
            }
        } catch (_: Exception) {
            0L
        }
    }

    /**
     * Merges multiple WAV audio files into a single WAV file.
     * Preserves audio sample rate, channels, and PCM 16-bit specs.
     */
    fun mergeWavFiles(wavFiles: List<File>, outputFile: File): Boolean {
        if (wavFiles.isEmpty()) return false
        if (wavFiles.size == 1) {
            wavFiles[0].copyTo(outputFile, overwrite = true)
            return true
        }

        try {
            val firstFile = wavFiles[0]
            val headerBytes = ByteArray(44)
            FileInputStream(firstFile).use { fis ->
                fis.read(headerBytes)
            }

            // Create or overwrite output file
            if (outputFile.exists()) outputFile.delete()
            outputFile.parentFile?.mkdirs()

            var totalPcmBytes = 0L

            FileOutputStream(outputFile).use { fos ->
                // Write placeholder 44-byte header
                fos.write(headerBytes)

                val buffer = ByteArray(8192)
                for (wavFile in wavFiles) {
                    if (!wavFile.exists() || wavFile.length() <= 44) continue
                    FileInputStream(wavFile).use { fis ->
                        // Skip the 44-byte WAV header of this chunk
                        val skipped = fis.skip(44)
                        if (skipped >= 44) {
                            var bytesRead: Int
                            while (fis.read(buffer).also { bytesRead = it } != -1) {
                                fos.write(buffer, 0, bytesRead)
                                totalPcmBytes += bytesRead
                            }
                        }
                    }
                }
            }

            // Now update the RIFF chunk size and data chunk size in the output file
            val totalFileSize = totalPcmBytes + 44
            val riffChunkSize = totalFileSize - 8

            RandomAccessFile(outputFile, "rw").use { raf ->
                // RIFF chunk size at offset 4 (4 bytes, little endian)
                raf.seek(4)
                val riffBuffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(riffChunkSize.toInt()).array()
                raf.write(riffBuffer)

                // Data chunk size at offset 40 (4 bytes, little endian)
                raf.seek(40)
                val dataBuffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(totalPcmBytes.toInt()).array()
                raf.write(dataBuffer)
            }

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun formatDuration(ms: Long): String {
        val totalSeconds = (ms / 1000).coerceAtLeast(0)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            else -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
        }
    }
}
