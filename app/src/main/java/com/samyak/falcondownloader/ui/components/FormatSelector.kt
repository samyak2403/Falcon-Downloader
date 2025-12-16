package com.samyak.falcondownloader.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.samyak.falcondownloader.data.VideoFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormatSelector(
    formats: List<VideoFormat>,
    selectedFormat: VideoFormat?,
    audioOnly: Boolean,
    onFormatSelected: (VideoFormat?) -> Unit,
    onAudioOnlyChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSheet by remember { mutableStateOf(false) }
    
    Column(modifier = modifier.fillMaxWidth()) {
        // Audio only toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AudioFile,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("Audio only (MP3)")
            }
            Switch(
                checked = audioOnly,
                onCheckedChange = onAudioOnlyChanged
            )
        }
        
        // Format selection button
        if (!audioOnly) {
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showSheet = true },
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideoFile,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = selectedFormat?.displayName ?: "Best quality (auto)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
    
    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false }
        ) {
            FormatList(
                formats = formats,
                selectedFormat = selectedFormat,
                onFormatSelected = {
                    onFormatSelected(it)
                    showSheet = false
                }
            )
        }
    }
}

@Composable
private fun FormatList(
    formats: List<VideoFormat>,
    selectedFormat: VideoFormat?,
    onFormatSelected: (VideoFormat?) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "Select Quality",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        // Auto option
        item {
            FormatItem(
                title = "Best quality (auto)",
                subtitle = "Automatically select best available",
                isSelected = selectedFormat == null,
                onClick = { onFormatSelected(null) }
            )
        }

        // Video formats
        val videoFormats = formats.filter { !it.isAudioOnly }
            .sortedByDescending { it.resolution?.split("x")?.getOrNull(1)?.toIntOrNull() ?: 0 }
        
        if (videoFormats.isNotEmpty()) {
            item {
                Text(
                    text = "Video",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(videoFormats.take(10)) { format ->
                FormatItem(
                    title = format.displayName,
                    subtitle = format.formatId,
                    fileSize = format.fileSizeDisplay,
                    isSelected = selectedFormat?.formatId == format.formatId,
                    onClick = { onFormatSelected(format) }
                )
            }
        }
        
        // Audio formats
        val audioFormats = formats.filter { it.isAudioOnly }
        
        if (audioFormats.isNotEmpty()) {
            item {
                Text(
                    text = "Audio only",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(audioFormats.take(5)) { format ->
                FormatItem(
                    title = format.displayName,
                    subtitle = format.formatId,
                    fileSize = format.fileSizeDisplay,
                    isSelected = selectedFormat?.formatId == format.formatId,
                    onClick = { onFormatSelected(format) }
                )
            }
        }
        
        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
private fun FormatItem(
    title: String,
    subtitle: String,
    fileSize: String? = null,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (fileSize != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = fileSize,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
