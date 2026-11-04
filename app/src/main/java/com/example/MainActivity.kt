package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.components.EngineSelectorSheet
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.MyApplicationTheme

enum class NavTab {
    STUDIO,
    HISTORY,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                var currentTab by rememberSaveable { mutableStateOf(NavTab.STUDIO) }
                var showEngineSheet by remember { mutableStateOf(false) }

                val currentEngine by viewModel.ttsManager.currentEngine.collectAsStateWithLifecycle()
                val installedEngines by viewModel.ttsManager.installedEngines.collectAsStateWithLifecycle()
                val historyItems by viewModel.historyItems.collectAsStateWithLifecycle()
                val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()

                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(userMessage) {
                    userMessage?.let {
                        snackbarHostState.showSnackbar(it)
                        viewModel.clearUserMessage()
                    }
                }

                // Handle Back button to return to Studio tab
                BackHandler(enabled = currentTab != NavTab.STUDIO) {
                    currentTab = NavTab.STUDIO
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.GraphicEq,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(horizontalAlignment = Alignment.Start) {
                                        Text(
                                            text = "BD TTS Engine",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "High-Speed Text to Audio",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            },
                            actions = {
                                // Direct model switcher icon
                                IconButton(
                                    onClick = { showEngineSheet = true },
                                    modifier = Modifier.testTag("topbar_engine_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = "Set TTS Engine",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp
                        ) {
                            NavigationBarItem(
                                selected = currentTab == NavTab.STUDIO,
                                onClick = { currentTab = NavTab.STUDIO },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = "Studio"
                                    )
                                },
                                label = { Text("Studio") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.testTag("nav_tab_studio")
                            )

                            NavigationBarItem(
                                selected = currentTab == NavTab.HISTORY,
                                onClick = { currentTab = NavTab.HISTORY },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (historyItems.isNotEmpty()) {
                                                Badge { Text("${historyItems.size}") }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = "History"
                                        )
                                    }
                                },
                                label = { Text("History") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.testTag("nav_tab_history")
                            )

                            NavigationBarItem(
                                selected = currentTab == NavTab.SETTINGS,
                                onClick = { currentTab = NavTab.SETTINGS },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Settings"
                                    )
                                },
                                label = { Text("Settings") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.testTag("nav_tab_settings")
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        Crossfade(targetState = currentTab, label = "tab_fade") { tab ->
                            when (tab) {
                                NavTab.STUDIO -> StudioScreen(
                                    viewModel = viewModel,
                                    onOpenEngineSheet = { showEngineSheet = true }
                                )
                                NavTab.HISTORY -> HistoryScreen(
                                    viewModel = viewModel,
                                    onNavigateToStudio = { currentTab = NavTab.STUDIO }
                                )
                                NavTab.SETTINGS -> SettingsScreen(
                                    viewModel = viewModel
                                )
                            }
                        }
                    }

                    // Engine Selector Modal Bottom Sheet
                    if (showEngineSheet) {
                        EngineSelectorSheet(
                            installedEngines = installedEngines,
                            currentEngine = currentEngine,
                            onSelectEngine = { selectedEngine ->
                                viewModel.switchEngine(selectedEngine.packageName)
                            },
                            onOpenSystemTtsSettings = {
                                try {
                                    context.startActivity(viewModel.ttsManager.openSystemTtsSettings())
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            },
                            onDismiss = { showEngineSheet = false }
                        )
                    }
                }
            }
        }
    }
}
