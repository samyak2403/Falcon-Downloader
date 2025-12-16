package com.samyak.falcondownloader.ui.screens

import android.content.Intent
import android.text.format.DateUtils
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samyak.falcondownloader.data.DownloadedFile
import com.samyak.falcondownloader.viewmodel.DownloadsViewModel

@Composable
fun DownloadsScreen(
    viewModel: DownloadsViewModel = viewModel()
) {
    DownloadsScreenContent(
        viewModel = viewModel,
        onPlayFile = { }
    )
}

@Composable
fun DownloadsScreenContent(
    viewModel: DownloadsViewModel = viewModel(),
    onPlayFile: (DownloadedFile) -> Unit
) {
    val files by viewModel.files.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    
    val filteredFiles = remember(files, selectedFilter) {
        viewModel.getFilteredFiles()
    }
    
    LaunchedEffect(Unit) {
        viewModel.loadFiles()
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Filter chips
        FilterChips(
            selectedFilter = selectedFilter,
            onFilterSelected = viewModel::setFilter,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            filteredFiles.isEmpty() -> {
                EmptyState(selectedFilter)
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = filteredFiles,
                        key = { it.file.absolutePath }
                    ) { file ->
                        DownloadedFileItem(
                            file = file,
                            onPlay = { onPlayFile(file) },
                            onDelete = { viewModel.deleteFile(file) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChips(
    selectedFilter: DownloadsViewModel.FilterType,
    onFilterSelected: (DownloadsViewModel.FilterType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedFilter == DownloadsViewModel.FilterType.ALL,
            onClick = { onFilterSelected(DownloadsViewModel.FilterType.ALL) },
            label = { Text("All") },
            leadingIcon = if (selectedFilter == DownloadsViewModel.FilterType.ALL) {
                { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
            } else null
        )
        FilterChip(
            selected = selectedFilter == DownloadsViewModel.FilterType.VIDEO,
            onClick = { onFilterSelected(DownloadsViewModel.FilterType.VIDEO) },
            label = { Text("Videos") },
            leadingIcon = if (selectedFilter == DownloadsViewModel.FilterType.VIDEO) {
                { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
            } else {
                { Icon(Icons.Default.VideoFile, null, Modifier.size(18.dp)) }
            }
        )
        FilterChip(
            selected = selectedFilter == DownloadsViewModel.FilterType.AUDIO,
            onClick = { onFilterSelected(DownloadsViewModel.FilterType.AUDIO) },
            label = { Text("Audio") },
            leadingIcon = if (selectedFilter == DownloadsViewModel.FilterType.AUDIO) {
                { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
            } else {
                { Icon(Icons.Default.AudioFile, null, Modifier.size(18.dp)) }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DownloadedFileItem(
    file: DownloadedFile,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onPlay() },
                onLongClick = { showMenu = true }
            ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // File type icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = if (file.isVideo) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (file.isVideo) Icons.Default.VideoFile 
                                         else Icons.Default.AudioFile,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = if (file.isVideo) 
                                MaterialTheme.colorScheme.onPrimaryContainer 
                            else 
                                MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
            
            // File info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = file.extension,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = file.formattedSize,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = DateUtils.getRelativeTimeSpanString(
                            file.lastModified,
                            System.currentTimeMillis(),
                            DateUtils.MINUTE_IN_MILLIS
                        ).toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Menu button
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, "More options")
                }
                
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Play") },
                        onClick = {
                            showMenu = false
                            onPlay()
                        },
                        leadingIcon = { Icon(Icons.Default.PlayArrow, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Open with...") },
                        onClick = {
                            showMenu = false
                            try {
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.provider",
                                    file.file
                                )
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(
                                        uri,
                                        if (file.isVideo) "video/*" else "audio/*"
                                    )
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Open with"))
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        leadingIcon = { Icon(Icons.Default.OpenInNew, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Share") },
                        onClick = {
                            showMenu = false
                            try {
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.provider",
                                    file.file
                                )
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = if (file.isVideo) "video/*" else "audio/*"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share"))
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        leadingIcon = { Icon(Icons.Default.Share, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            showMenu = false
                            showDeleteDialog = true
                        },
                        leadingIcon = { 
                            Icon(
                                Icons.Default.Delete, 
                                null,
                                tint = MaterialTheme.colorScheme.error
                            ) 
                        }
                    )
                }
            }
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete file?") },
            text = { Text("\"${file.name}\" will be permanently deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun EmptyState(filter: DownloadsViewModel.FilterType) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = when (filter) {
                    DownloadsViewModel.FilterType.VIDEO -> Icons.Default.VideoFile
                    DownloadsViewModel.FilterType.AUDIO -> Icons.Default.AudioFile
                    else -> Icons.Default.FolderOpen
                },
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Text(
                text = when (filter) {
                    DownloadsViewModel.FilterType.VIDEO -> "No videos yet"
                    DownloadsViewModel.FilterType.AUDIO -> "No audio files yet"
                    else -> "No downloads yet"
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Downloaded files will appear here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
