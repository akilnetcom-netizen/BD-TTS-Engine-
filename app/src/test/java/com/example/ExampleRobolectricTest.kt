package com.example

import com.example.tts.WavHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTestLogic {

    @Test
    fun testWavFormatting() {
        val durationStr = WavHelper.formatDuration(65000L)
        assertEquals("01:05", durationStr)

        val sizeStr = WavHelper.formatFileSize(1048576L)
        assertTrue(sizeStr.contains("1.0") || sizeStr.contains("MB"))
    }

    @Test
    fun testTextChunkingSeparators() {
        val bengaliText = "প্রথম বাক্য। দ্বিতীয় বাক্য! তৃতীয় বাক্য? চতুর্থ বাক্য।"
        val chunks = bengaliText.split(Regex("(?<=[।!?.\n])\\s+"))
        assertEquals(4, chunks.size)
    }
}
