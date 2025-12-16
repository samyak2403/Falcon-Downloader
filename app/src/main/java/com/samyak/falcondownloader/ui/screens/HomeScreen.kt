package com.samyak.falcondownloader.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samyak.falcondownloader.data.DownloadStatus
import com.samyak.falcondownloader.ui.components.DownloadProgress
import com.samyak.falcondownloader.ui.components.FormatSelector
import com.samyak.falcondownloader.ui.components.VideoInfoCard
import com.samyak.falcondownloader.viewmodel.DownloadViewModel

@Composable
fun HomeScreenContent(
    sharedUrl: String? = null,
    viewModel: DownloadViewModel = viewModel()
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current
    
    val url by viewModel.url.collectAsState()
    val videoInfo by viewModel.videoInfo.collectAsState()
    val downloadState by viewModel.downloadState.collectAsState()
    val selectedFormat by viewModel.selectedFormat.collectAsState()
    val audioOnly by viewModel.audioOnly.collectAsState()
    
    // Using app's external files directory - no permission needed on Android 10+
    // For older versions, check storage permission
    var hasStoragePermission by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasStoragePermission = granted
        if (granted) {
            viewModel.startDownload()
        }
    }
    
    // Handle shared URL
    LaunchedEffect(sharedUrl) {
        sharedUrl?.let {
            viewModel.updateUrl(it)
            viewModel.fetchVideoInfo()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        
        // URL Input
        OutlinedTextField(
            value = url,
            onValueChange = viewModel::updateUrl,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Video URL") },
            placeholder = { Text("Paste video link here") },
            leadingIcon = {
                Icon(Icons.Default.Link, contentDescription = null)
            },
            trailingIcon = {
                Row {
                    IconButton(onClick = {
                        clipboardManager.getText()?.text?.let {
                            viewModel.updateUrl(it)
                        }
                    }) {
                        Icon(Icons.Default.ContentPaste, "Paste")
                    }
                    if (url.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateUrl("") }) {
                            Icon(Icons.Default.Clear, "Clear")
                        }
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    focusManager.clearFocus()
                    viewModel.fetchVideoInfo()
                }
            )
        )

        // Fetch Info Button
        if (downloadState.status == DownloadStatus.IDLE && videoInfo == null && url.isNotEmpty()) {
            Button(
                onClick = {
                    focusManager.clearFocus()
                    viewModel.fetchVideoInfo()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Get Video Info")
            }
        }
        
        // Progress indicator
        if (downloadState.status != DownloadStatus.IDLE) {
            DownloadProgress(
                state = downloadState,
                onCancel = viewModel::cancelDownload
            )
        }
        
        // Video Info Card
        videoInfo?.let { info ->
            VideoInfoCard(videoInfo = info)
            
            // Format Selector
            if (downloadState.status == DownloadStatus.IDLE || 
                downloadState.status == DownloadStatus.ERROR ||
                downloadState.status == DownloadStatus.CANCELLED) {
                
                FormatSelector(
                    formats = info.formats,
                    selectedFormat = selectedFormat,
                    audioOnly = audioOnly,
                    onFormatSelected = viewModel::selectFormat,
                    onAudioOnlyChanged = viewModel::setAudioOnly
                )
                
                // Download Button
                Button(
                    onClick = {
                        if (!hasStoragePermission && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                            permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        } else {
                            viewModel.startDownload()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (audioOnly) "Download Audio" else "Download Video",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
        
        // Download complete actions
        if (downloadState.status == DownloadStatus.COMPLETED) {
            OutlinedButton(
                onClick = viewModel::reset,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("New Download")
            }
        }
        
        // Supported sites hint
        if (url.isEmpty() && videoInfo == null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Supported Sites",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Text(
                        text = "YouTube, Twitter/X, Instagram, TikTok, Facebook, Vimeo, and 1000+ more sites",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}
