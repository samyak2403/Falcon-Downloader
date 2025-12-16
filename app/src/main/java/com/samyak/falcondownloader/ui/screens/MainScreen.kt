package com.samyak.falcondownloader.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samyak.falcondownloader.data.DownloadedFile
import com.samyak.falcondownloader.viewmodel.DownloadViewModel
import com.samyak.falcondownloader.viewmodel.DownloadsViewModel

enum class Screen(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("Download", Icons.Filled.Download, Icons.Outlined.Download),
    DOWNLOADS("Library", Icons.Filled.VideoLibrary, Icons.Outlined.VideoLibrary),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    sharedUrl: String? = null
) {
    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    var playingFile by remember { mutableStateOf<DownloadedFile?>(null) }
    var showAboutScreen by remember { mutableStateOf(false) }
    var showUpdateSnackbar by remember { mutableStateOf<String?>(null) }
    val downloadViewModel: DownloadViewModel = viewModel()
    val downloadsViewModel: DownloadsViewModel = viewModel()
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Show snackbar for updates
    LaunchedEffect(showUpdateSnackbar) {
        showUpdateSnackbar?.let {
            snackbarHostState.showSnackbar(it)
            showUpdateSnackbar = null
        }
    }
    
    // Switch to home if shared URL received
    LaunchedEffect(sharedUrl) {
        if (sharedUrl != null) {
            currentScreen = Screen.HOME
        }
    }
    
    // Show About screen
    if (showAboutScreen) {
        AboutScreen(onBack = { showAboutScreen = false })
        return
    }
    
    // Show player if a file is selected
    playingFile?.let { file ->
        PlayerScreen(
            file = file,
            onBack = { playingFile = null }
        )
        return
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        when (currentScreen) {
                            Screen.HOME -> "Falcon Downloader"
                            Screen.DOWNLOADS -> "Library"
                            Screen.SETTINGS -> "Settings"
                        }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    when (currentScreen) {
                        Screen.HOME -> {
                            IconButton(onClick = {
                                downloadViewModel.updateYtDlp { result ->
                                    showUpdateSnackbar = result
                                }
                            }) {
                                Icon(Icons.Default.Update, "Update yt-dlp")
                            }
                        }
                        Screen.DOWNLOADS -> {
                            IconButton(onClick = { downloadsViewModel.loadFiles() }) {
                                Icon(Icons.Default.Refresh, "Refresh")
                            }
                        }
                        Screen.SETTINGS -> { }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                Screen.entries.forEach { screen ->
                    NavigationBarItem(
                        selected = currentScreen == screen,
                        onClick = { currentScreen = screen },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen == screen) 
                                    screen.selectedIcon 
                                else 
                                    screen.unselectedIcon,
                                contentDescription = screen.title
                            )
                        },
                        label = { Text(screen.title) }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (currentScreen) {
                Screen.HOME -> HomeScreenContent(
                    sharedUrl = sharedUrl,
                    viewModel = downloadViewModel
                )
                Screen.DOWNLOADS -> DownloadsScreenContent(
                    viewModel = downloadsViewModel,
                    onPlayFile = { file ->
                        if (file.isVideo || file.isAudio) {
                            playingFile = file
                        }
                    }
                )
                Screen.SETTINGS -> SettingsScreen(
                    onUpdateYtDlp = {
                        downloadViewModel.updateYtDlp { result ->
                            showUpdateSnackbar = result
                        }
                    },
                    onAboutClick = { showAboutScreen = true }
                )
            }
        }
    }
}
