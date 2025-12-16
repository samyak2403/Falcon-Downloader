package com.samyak.falcondownloader.data

data class VideoInfo(
    val id: String,
    val title: String,
    val thumbnail: String?,
    val duration: Long,
    val uploader: String?,
    val formats: List<VideoFormat>,
    val url: String
)

data class VideoFormat(
    val formatId: String,
    val ext: String,
    val quality: String,
    val filesize: Long?,
    val resolution: String?,
    val vcodec: String?,
    val acodec: String?,
    val isAudioOnly: Boolean,
    val isVideoOnly: Boolean
) {
    val displayName: String
        get() = buildString {
            if (isAudioOnly) {
                append("Audio • $ext")
                acodec?.let { append(" • $it") }
            } else {
                resolution?.let { append(it) } ?: append(quality)
                append(" • $ext")
                if (isVideoOnly) append(" (video only)")
            }
        }
    
    val fileSizeDisplay: String?
        get() = filesize?.let { formatFileSize(it) }
    
    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
            bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
            bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
            else -> "$bytes B"
        }
    }
}

enum class DownloadStatus {
    IDLE, FETCHING_INFO, DOWNLOADING, COMPLETED, ERROR, CANCELLED
}

data class DownloadState(
    val status: DownloadStatus = DownloadStatus.IDLE,
    val progress: Float = 0f,
    val progressText: String = "",
    val error: String? = null,
    val downloadedFile: String? = null
)
