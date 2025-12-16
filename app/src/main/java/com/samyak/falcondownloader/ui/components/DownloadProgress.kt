package com.samyak.falcondownloader.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import com.samyak.falcondownloader.data.DownloadState
import com.samyak.falcondownloader.data.DownloadStatus

@Composable
fun DownloadProgress(
    state: DownloadState,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (state.status) {
                DownloadStatus.COMPLETED -> MaterialTheme.colorScheme.primaryContainer
                DownloadStatus.ERROR -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusIcon(state.status)
                    Text(
                        text = getStatusText(state),
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                
                if (state.status == DownloadStatus.DOWNLOADING || 
                    state.status == DownloadStatus.FETCHING_INFO) {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel"
                        )
                    }
                }
            }
            
            if (state.status == DownloadStatus.DOWNLOADING) {
                LinearProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    trackColor = MaterialTheme.colorScheme.surface,
                )
                
                if (state.progressText.isNotEmpty()) {
                    Text(
                        text = state.progressText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (state.status == DownloadStatus.FETCHING_INFO) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(4.dp)
                )
            }
            
            state.error?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            state.downloadedFile?.let {
                Text(
                    text = "Saved to app storage",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun StatusIcon(status: DownloadStatus) {
    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    when (status) {
        DownloadStatus.FETCHING_INFO, DownloadStatus.DOWNLOADING -> {
            Icon(
                imageVector = Icons.Default.Sync,
                contentDescription = null,
                modifier = Modifier.rotate(rotation),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        DownloadStatus.COMPLETED -> {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
        DownloadStatus.ERROR -> {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        }
        DownloadStatus.CANCELLED -> {
            Icon(
                imageVector = Icons.Default.Cancel,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
        else -> {}
    }
}

private fun getStatusText(state: DownloadState): String {
    return when (state.status) {
        DownloadStatus.FETCHING_INFO -> "Fetching video info..."
        DownloadStatus.DOWNLOADING -> "Downloading..."
        DownloadStatus.COMPLETED -> "Download complete!"
        DownloadStatus.ERROR -> "Download failed"
        DownloadStatus.CANCELLED -> "Cancelled"
        else -> ""
    }
}
