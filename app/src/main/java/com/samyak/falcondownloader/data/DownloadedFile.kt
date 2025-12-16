package com.samyak.falcondownloader.data

import java.io.File

data class DownloadedFile(
    val file: File,
    val name: String,
    val size: Long,
    val lastModified: Long,
    val isVideo: Boolean,
    val isAudio: Boolean
) {
    val formattedSize: String
        get() = when {
            size >= 1_073_741_824 -> "%.1f GB".format(size / 1_073_741_824.0)
            size >= 1_048_576 -> "%.1f MB".format(size / 1_048_576.0)
            size >= 1024 -> "%.1f KB".format(size / 1024.0)
            else -> "$size B"
        }
    
    val extension: String
        get() = file.extension.uppercase()
    
    companion object {
        private val videoExtensions = setOf("mp4", "mkv", "webm", "avi", "mov", "flv", "wmv", "m4v")
        private val audioExtensions = setOf("mp3", "m4a", "aac", "opus", "ogg", "wav", "flac", "wma")
        
        fun fromFile(file: File): DownloadedFile {
            val ext = file.extension.lowercase()
            return DownloadedFile(
                file = file,
                name = file.nameWithoutExtension,
                size = file.length(),
                lastModified = file.lastModified(),
                isVideo = ext in videoExtensions,
                isAudio = ext in audioExtensions
            )
        }
    }
}
