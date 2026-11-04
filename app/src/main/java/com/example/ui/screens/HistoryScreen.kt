package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AudioHistoryItem
import com.example.ui.MainViewModel
import com.example.ui.components.AudioPlayerCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: MainViewModel,
    onNavigateToStudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val historyItems by viewModel.historyItems.collectAsStateWithLifecycle()

    val playerTrackPath by viewModel.playerManager.currentTrackPath.collectAsStateWithLifecycle()
    val isAudioPlaying by viewModel.playerManager.isPlaying.collectAsStateWithLifecycle()
    val currentPositionMs by viewModel.playerManager.currentPositionMs.collectAsStateWithLifecycle()
    val totalDurationMs by viewModel.playerManager.totalDurationMs.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFormatFilter by remember { mutableStateOf("ALL") } // "ALL", "WAV", "MP3"
    var showClearDialog by remember { mutableStateOf(false) }

    val filteredItems = remember(historyItems, searchQuery, selectedFormatFilter) {
        historyItems.filter { item ->
            val matchesQuery = searchQuery.isBlank() ||
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.text.contains(searchQuery, ignoreCase = true)

            val matchesFormat = when (selectedFormatFilter) {
                "WAV" -> item.format.equals("WAV", ignoreCase = true)
                "MP3" -> item.format.equals("MP3", ignoreCase = true)
                else -> true
            }

            matchesQuery && matchesFormat
        }
    }

    val dateFormatter = remember {
        SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Audio History",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "পূর্বে তৈরি করা অডিও তালিকা (${historyItems.size})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (historyItems.isNotEmpty()) {
                IconButton(
                    onClick = { showClearDialog = true },
                    modifier = Modifier.testTag("clear_history_icon_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear all history",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("history_search_input"),
            placeholder = { Text("অডিও খুঁজুন / Search by text...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Chips (All, WAV, MP3)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = selectedFormatFilter == "ALL",
                onClick = { selectedFormatFilter = "ALL" },
                label = { Text("সবগুলো (All)") },
                modifier = Modifier.testTag("filter_all_chip")
            )
            FilterChip(
                selected = selectedFormatFilter == "WAV",
                onClick = { selectedFormatFilter = "WAV" },
                label = { Text("WAV") },
                modifier = Modifier.testTag("filter_wav_chip")
            )
            FilterChip(
                selected = selectedFormatFilter == "MP3",
                onClick = { selectedFormatFilter = "MP3" },
                label = { Text("MP3") },
                modifier = Modifier.testTag("filter_mp3_chip")
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Content
        if (filteredItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (searchQuery.isNotBlank()) "কোন ফলাফল পাওয়া যায়নি" else "কোনো অডিও তৈরি করা হয়নি",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (searchQuery.isNotBlank()) "অন্য কি-ওয়ার্ড দিয়ে খুঁজে দেখুন।" else "টেক্সট লিখে আপনার প্রথম অডিও তৈরি করুন।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = onNavigateToStudio,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("go_to_studio_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("অডিও তৈরি করুন")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredItems, key = { it.id }) { item ->
                    val isThisPlaying = isAudioPlaying && playerTrackPath == item.filePath
                    val dateStr = dateFormatter.format(Date(item.createdAt))

                    AudioPlayerCard(
                        title = item.title,
                        subtitle = "$dateStr • ${item.engineName}",
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
                        onReuse = {
                            viewModel.setInputText(item.text)
                            onNavigateToStudio()
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    // Clear all history confirmation dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("সব হিস্টোরি মুছে ফেলবেন?") },
            text = { Text("এটি আপনার তৈরি করা সমস্ত পূর্ববর্তী অডিও ফাইল স্থায়ীভাবে মুছে ফেলবে।") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("মুছে ফেলুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }
}
