package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.components.AudioPlayerCard
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.Mp3Color
import com.example.ui.theme.WavColor

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudioScreen(
    viewModel: MainViewModel,
    onOpenEngineSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val inputText by viewModel.inputText.collectAsStateWithLifecycle()
    val outputFormat by viewModel.outputFormat.collectAsStateWithLifecycle()
    val latestItem by viewModel.latestGeneratedItem.collectAsStateWithLifecycle()

    val currentEngine by viewModel.ttsManager.currentEngine.collectAsStateWithLifecycle()
    val selectedVoice by viewModel.ttsManager.selectedVoice.collectAsStateWithLifecycle()
    val speechRate by viewModel.ttsManager.speechRate.collectAsStateWithLifecycle()
    val pitch by viewModel.ttsManager.pitch.collectAsStateWithLifecycle()
    val isPreviewPlaying by viewModel.ttsManager.isPreviewPlaying.collectAsStateWithLifecycle()

    val isGenerating by viewModel.ttsManager.isGenerating.collectAsStateWithLifecycle()
    val generationProgress by viewModel.ttsManager.generationProgress.collectAsStateWithLifecycle()
    val generationStatus by viewModel.ttsManager.generationStatus.collectAsStateWithLifecycle()

    val playerTrackPath by viewModel.playerManager.currentTrackPath.collectAsStateWithLifecycle()
    val isAudioPlaying by viewModel.playerManager.isPlaying.collectAsStateWithLifecycle()
    val currentPositionMs by viewModel.playerManager.currentPositionMs.collectAsStateWithLifecycle()
    val totalDurationMs by viewModel.playerManager.totalDurationMs.collectAsStateWithLifecycle()

    var showTuningPanel by remember { mutableStateOf(false) }

    val characterCount = inputText.length
    val wordCount = if (inputText.isBlank()) 0 else inputText.trim().split(Regex("\\s+")).size

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Active Engine & Voice Status Card (with Set TTS Engine CTA)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable { onOpenEngineSheet() }
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp)
                )
                .testTag("engine_status_card"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Active Model:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = currentEngine?.label ?: "System TTS",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = selectedVoice?.displayName ?: "Default Voice",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable { onOpenEngineSheet() }
                ) {
                    Text(
                        text = "Set Engine",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Unlimited Text Banner
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AllInclusive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "আনলিমিটেড টেক্সট সাপোর্ট: কোনো অক্ষর সীমাবদ্ধতা নেই। দীর্ঘ নিবন্ধ বা গল্পও দ্রুত অডিও হবে।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 16.sp
                )
            }
        }

        // Text Input Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { viewModel.setInputText(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 140.dp, max = 280.dp)
                        .testTag("tts_text_input"),
                    placeholder = {
                        Text(
                            text = "এখানে যেকোনো টেক্সট লিখুন বা পেস্ট করুন...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Input meta info & action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "$characterCount অক্ষর | $wordCount শব্দ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Paste button
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                val clip = clipboard?.primaryClip
                                if (clip != null && clip.itemCount > 0) {
                                    val pasted = clip.getItemAt(0).text?.toString() ?: ""
                                    if (pasted.isNotBlank()) {
                                        viewModel.setInputText(if (inputText.isBlank()) pasted else "$inputText\n$pasted")
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("paste_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = "Paste",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Clear text button
                        if (inputText.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.setInputText("") },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("clear_text_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Sample Text Presets
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "নমুনা টেক্সট:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )

                    FilterChip(
                        selected = false,
                        onClick = {
                            viewModel.setInputText(
                                "বাংলাদেশ দক্ষিণ এশিয়ার একটি সুন্দর ও সবুজ দেশ। পদ্মা, মেঘনা ও যমুনার পলিমাটিতে গঠিত এই বদ্বীপ প্রাকৃতিক সম্পদে সমৃদ্ধ।"
                            )
                        },
                        label = { Text("বাংলা বর্ণনা", fontSize = 11.sp) },
                        modifier = Modifier.testTag("sample_text_1")
                    )

                    FilterChip(
                        selected = false,
                        onClick = {
                            viewModel.setInputText(
                                "সকালবেলা পাখির কিচিরমিচির শব্দে ঘুম ভেঙে গেল। স্নিগ্ধ বাতাসে মনটা ভরে উঠল এক অদ্ভুত প্রশান্তিতে।"
                            )
                        },
                        label = { Text("গল্পের অংশ", fontSize = 11.sp) },
                        modifier = Modifier.testTag("sample_text_2")
                    )

                    FilterChip(
                        selected = false,
                        onClick = {
                            viewModel.setInputText(
                                "Welcome to BD TTS Engine! Fast and unlimited text-to-speech audio converter."
                            )
                        },
                        label = { Text("English", fontSize = 11.sp) },
                        modifier = Modifier.testTag("sample_text_3")
                    )
                }
            }
        }

        // Format Selector (WAV & MP3)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Text(
                    text = "সেভ ফরম্যাট নির্বাচন (Output Audio Format)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // WAV Choice
                    val isWav = outputFormat.equals("WAV", ignoreCase = true)
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.setOutputFormat("WAV") }
                            .border(
                                width = if (isWav) 2.dp else 1.dp,
                                color = if (isWav) WavColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .testTag("format_wav_chip"),
                        color = if (isWav) WavColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "WAV",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isWav) WavColor else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                if (isWav) {
                                    Surface(
                                        color = WavColor,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "Selected",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "স্টুডিও কোয়ালিটি লসলেস",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // MP3 Choice
                    val isMp3 = outputFormat.equals("MP3", ignoreCase = true)
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.setOutputFormat("MP3") }
                            .border(
                                width = if (isMp3) 2.dp else 1.dp,
                                color = if (isMp3) Mp3Color else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .testTag("format_mp3_chip"),
                        color = if (isMp3) Mp3Color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "MP3",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMp3) Mp3Color else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                if (isMp3) {
                                    Surface(
                                        color = Mp3Color,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "Selected",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "কমপ্যাক্ট হালকা ফাইল",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Voice Pitch & Speed Tuning Panel (Expandable)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTuningPanel = !showTuningPanel },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ভয়েস টিউনিং (Speed & Pitch)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Icon(
                        imageVector = if (showTuningPanel) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AnimatedVisibility(visible = showTuningPanel) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp)
                    ) {
                        // Speech Rate
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "স্পিড / Speed: ${String.format("%.1fx", speechRate)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Reset (1.0x)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable { viewModel.setSpeechRate(1.0f) }
                                    .padding(4.dp)
                            )
                        }
                        Slider(
                            value = speechRate,
                            onValueChange = { viewModel.setSpeechRate(it) },
                            valueRange = 0.5f..2.0f,
                            steps = 15,
                            modifier = Modifier.fillMaxWidth().testTag("speech_rate_slider"),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Pitch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "পিচ / Pitch: ${String.format("%.1fx", pitch)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Reset (1.0x)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable { viewModel.setPitch(1.0f) }
                                    .padding(4.dp)
                            )
                        }
                        Slider(
                            value = pitch,
                            onValueChange = { viewModel.setPitch(it) },
                            valueRange = 0.5f..2.0f,
                            steps = 15,
                            modifier = Modifier.fillMaxWidth().testTag("speech_pitch_slider"),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Preview Voice Button
                        OutlinedButton(
                            onClick = {
                                if (isPreviewPlaying) {
                                    viewModel.stopPreviewVoice()
                                } else {
                                    viewModel.previewVoice()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("preview_voice_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = if (isPreviewPlaying) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isPreviewPlaying) "প্রিভিউ বন্ধ করুন (Stop)" else "ভয়েস প্রিভিউ শুনুন (Preview Voice)"
                            )
                        }
                    }
                }
            }
        }

        // Generate Audio Button
        Button(
            onClick = {
                if (isGenerating) {
                    viewModel.cancelGeneration()
                } else {
                    viewModel.generateAudio()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("generate_audio_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isGenerating) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                contentColor = if (isGenerating) Color.White else MaterialTheme.colorScheme.onPrimary
            )
        ) {
            if (isGenerating) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "বাতিল করুন (Cancel)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Generate Audio ($outputFormat)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Generation Progress Status
        if (isGenerating) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = generationStatus,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${(generationProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { generationProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                }
            }
        }

        // Generated Result Player Card (if available)
        AnimatedVisibility(
            visible = latestItem != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            latestItem?.let { item ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "সর্বশেষ তৈরি করা অডিও (Generated Result):",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val isThisPlaying = isAudioPlaying && playerTrackPath == item.filePath

                    AudioPlayerCard(
                        title = item.title,
                        subtitle = "${item.engineName} • ${item.voiceName}",
                        format = item.format,
                        filePath = item.filePath,
                        fileSizeBytes = item.fileSizeBytes,
                        durationMs = item.durationMs,
                        isPlaying = isThisPlaying,
                        currentPositionMs = if (isThisPlaying) currentPositionMs else 0L,
                        totalDurationMs = if (isThisPlaying) totalDurationMs else item.durationMs,
                        onTogglePlayPause = {
                            viewModel.playerManager.togglePlayPause(item.filePath)
                        },
                        onSeek = { targetMs ->
                            if (isThisPlaying) {
                                viewModel.playerManager.seekTo(targetMs)
                            }
                        },
                        onShare = {
                            viewModel.shareAudio(context, item)
                        },
                        onDelete = {
                            viewModel.deleteHistoryItem(item)
                        },
                        accentBorder = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Download to device button
                    OutlinedButton(
                        onClick = { viewModel.saveToMusicDirectory(context, item) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_to_device_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save to Device Music / ডিভাইসে সংরক্ষণ করুন")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
