package com.samyak.falcondownloader.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.samyak.falcondownloader.BuildConfig
import com.samyak.falcondownloader.data.AppSettings
import com.samyak.falcondownloader.downloader.YtDlpDownloader
import com.samyak.falcondownloader.util.AppIconManager

@Composable
fun SettingsScreen(
    onUpdateYtDlp: () -> Unit,
    onAboutClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val settings = remember { AppSettings.getInstance(context) }
    
    val downloadQuality by settings.downloadQuality.collectAsState()
    val preferAudio by settings.preferAudio.collectAsState()
    val darkMode by settings.darkMode.collectAsState()
    val autoUpdate by settings.autoUpdate.collectAsState()
    
    var showQualityDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Download Settings
        SettingsSection(title = "Download") {
            // Quality
            SettingsItem(
                icon = Icons.Default.HighQuality,
                title = "Default Quality",
                subtitle = getQualityLabel(downloadQuality),
                onClick = { showQualityDialog = true }
            )
            
            // Prefer Audio
            SettingsSwitchItem(
                icon = Icons.Default.AudioFile,
                title = "Prefer Audio Only",
                subtitle = "Download audio by default",
                checked = preferAudio,
                onCheckedChange = { settings.setPreferAudio(it) }
            )
        }
        
        // App Settings
        SettingsSection(title = "Appearance") {
            SettingsItem(
                icon = Icons.Default.DarkMode,
                title = "Theme",
                subtitle = getThemeLabel(darkMode),
                onClick = { showThemeDialog = true }
            )
            
            var showIconDialog by remember { mutableStateOf(false) }
            var currentIcon by remember { mutableStateOf(AppIconManager.getCurrentIcon(context)) }
            
            SettingsItem(
                icon = Icons.Default.Apps,
                title = "App Icon",
                subtitle = currentIcon.displayName,
                onClick = { showIconDialog = true }
            )
            
            if (showIconDialog) {
                AppIconDialog(
                    currentIcon = currentIcon,
                    onIconSelected = { icon ->
                        AppIconManager.setIcon(context, icon)
                        currentIcon = icon
                        showIconDialog = false
                    },
                    onDismiss = { showIconDialog = false }
                )
            }
        }
        
        // Updates
        SettingsSection(title = "Updates") {
            SettingsSwitchItem(
                icon = Icons.Default.Update,
                title = "Auto-check Updates",
                subtitle = "Check for yt-dlp updates on startup",
                checked = autoUpdate,
                onCheckedChange = { settings.setAutoUpdate(it) }
            )
            
            SettingsItem(
                icon = Icons.Default.Refresh,
                title = "Update yt-dlp Now",
                subtitle = "Download latest version",
                onClick = onUpdateYtDlp
            )
        }
        
        // Storage
        SettingsSection(title = "Storage") {
            val downloadDir = YtDlpDownloader.getDownloadDirectory(context)
            val downloadSize = remember { calculateFolderSize(downloadDir) }
            var cacheCleared by remember { mutableStateOf(false) }
            
            SettingsItem(
                icon = Icons.Default.Folder,
                title = "Download Location",
                subtitle = "App Storage/Falcon Downloader",
                onClick = { }
            )
            
            SettingsItem(
                icon = Icons.Default.Storage,
                title = "Downloaded Files",
                subtitle = formatSize(downloadSize),
                onClick = { }
            )
            
            SettingsItem(
                icon = Icons.Default.DeleteSweep,
                title = "Clear Cache",
                subtitle = if (cacheCleared) "Cache cleared!" else "Free up temporary space",
                onClick = {
                    context.cacheDir.deleteRecursively()
                    context.externalCacheDir?.deleteRecursively()
                    cacheCleared = true
                }
            )
        }
        
        // About
        SettingsSection(title = "About") {
            SettingsItem(
                icon = Icons.Default.Info,
                title = "About Falcon Downloader",
                subtitle = "Version ${BuildConfig.VERSION_NAME}",
                onClick = onAboutClick
            )
            
            SettingsItem(
                icon = Icons.Default.Code,
                title = "Powered by yt-dlp",
                subtitle = "Open source video downloader",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/yt-dlp/yt-dlp"))
                    context.startActivity(intent)
                }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
    
    // Quality Dialog
    if (showQualityDialog) {
        QualitySelectionDialog(
            currentQuality = downloadQuality,
            onQualitySelected = {
                settings.setDownloadQuality(it)
                showQualityDialog = false
            },
            onDismiss = { showQualityDialog = false }
        )
    }
    
    // Theme Dialog
    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = darkMode,
            onThemeSelected = {
                settings.setDarkMode(it)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
        content()
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    }
}


@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun QualitySelectionDialog(
    currentQuality: String,
    onQualitySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val qualities = listOf(
        "best" to "Best Available",
        "1080" to "1080p (Full HD)",
        "720" to "720p (HD)",
        "480" to "480p (SD)",
        "360" to "360p (Low)",
        "audio" to "Audio Only"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Default Quality") },
        text = {
            Column {
                qualities.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onQualitySelected(value) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentQuality == value,
                            onClick = { onQualitySelected(value) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ThemeSelectionDialog(
    currentTheme: String,
    onThemeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val themes = listOf(
        "system" to "System Default",
        "light" to "Light",
        "dark" to "Dark"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Theme") },
        text = {
            Column {
                themes.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onThemeSelected(value) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentTheme == value,
                            onClick = { onThemeSelected(value) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun getQualityLabel(quality: String): String {
    return when (quality) {
        "best" -> "Best Available"
        "1080" -> "1080p (Full HD)"
        "720" -> "720p (HD)"
        "480" -> "480p (SD)"
        "360" -> "360p (Low)"
        "audio" -> "Audio Only"
        else -> quality
    }
}

private fun getThemeLabel(theme: String): String {
    return when (theme) {
        "system" -> "System Default"
        "light" -> "Light"
        "dark" -> "Dark"
        else -> theme
    }
}

private fun calculateFolderSize(folder: java.io.File): Long {
    if (!folder.exists()) return 0L
    return folder.walkTopDown()
        .filter { it.isFile }
        .map { it.length() }
        .sum()
}

private fun formatSize(bytes: Long): String {
    return when {
        bytes >= 1_073_741_824 -> "%.2f GB".format(bytes / 1_073_741_824.0)
        bytes >= 1_048_576 -> "%.2f MB".format(bytes / 1_048_576.0)
        bytes >= 1024 -> "%.2f KB".format(bytes / 1024.0)
        else -> "$bytes bytes"
    }
}


@Composable
private fun AppIconDialog(
    currentIcon: AppIconManager.AppIcon,
    onIconSelected: (AppIconManager.AppIcon) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("App Icon") },
        text = {
            Column {
                Text(
                    text = "Changes may take a moment to apply",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                AppIconManager.AppIcon.entries.forEach { icon ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onIconSelected(icon) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentIcon == icon,
                            onClick = { onIconSelected(icon) }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            imageVector = when (icon) {
                                AppIconManager.AppIcon.DEFAULT -> Icons.Default.Download
                                AppIconManager.AppIcon.BLUE -> Icons.Default.Download
                                AppIconManager.AppIcon.DARK -> Icons.Default.Download
                            },
                            contentDescription = null,
                            tint = when (icon) {
                                AppIconManager.AppIcon.DEFAULT -> MaterialTheme.colorScheme.primary
                                AppIconManager.AppIcon.BLUE -> Color(0xFF2196F3)
                                AppIconManager.AppIcon.DARK -> Color(0xFF424242)
                            },
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(icon.displayName)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
