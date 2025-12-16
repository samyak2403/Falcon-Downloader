package com.samyak.falcondownloader.downloader

import android.content.Context
import com.samyak.falcondownloader.FalconApp
import com.samyak.falcondownloader.data.VideoFormat
import com.samyak.falcondownloader.data.VideoInfo
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class YtDlpDownloader {
    
    private var currentProcessId: String? = null
    private val isCancelled = AtomicBoolean(false)
    
    companion object {
        const val DOWNLOAD_FOLDER_NAME = "Falcon Downloader"
        
        fun getDownloadDirectory(context: Context = FalconApp.instance): File {
            // Use app's external files directory (internal to app, no permission needed)
            // Path: /storage/emulated/0/Android/data/com.samyak.falcondownloader/files/Falcon Downloader
            val appDir = context.getExternalFilesDir(null)
            val downloadDir = File(appDir, DOWNLOAD_FOLDER_NAME)
            if (!downloadDir.exists()) {
                downloadDir.mkdirs()
            }
            return downloadDir
        }
    }
    
    suspend fun getVideoInfo(url: String): Result<VideoInfo> = withContext(Dispatchers.IO) {
        try {
            val request = YoutubeDLRequest(url)
            request.addOption("--dump-json")
            request.addOption("--no-playlist")
            
            // Add options to bypass 403 errors
            addAntiBlockOptions(request)
            
            val response = YoutubeDL.getInstance().getInfo(request)
            
            val formats = response.formats?.mapNotNull { format ->
                val formatId = format.formatId ?: return@mapNotNull null
                val ext = format.ext ?: "mp4"
                val vcodec = format.vcodec
                val acodec = format.acodec
                val isVideoOnly = acodec == "none" || acodec.isNullOrEmpty()
                val isAudioOnly = vcodec == "none" || vcodec.isNullOrEmpty()
                
                // Get filesize from format
                val filesize = format.fileSize
                
                VideoFormat(
                    formatId = formatId,
                    ext = ext,
                    quality = format.formatNote ?: format.format ?: "Unknown",
                    filesize = filesize,
                    resolution = if (!isAudioOnly) "${format.width ?: 0}x${format.height ?: 0}" else null,
                    vcodec = if (!isAudioOnly) vcodec else null,
                    acodec = acodec,
                    isAudioOnly = isAudioOnly,
                    isVideoOnly = isVideoOnly && !isAudioOnly
                )
            }?.filter { it.formatId.isNotEmpty() } ?: emptyList()
            
            Result.success(
                VideoInfo(
                    id = response.id ?: "",
                    title = response.title ?: "Unknown",
                    thumbnail = response.thumbnail,
                    duration = response.duration?.toLong() ?: 0L,
                    uploader = response.uploader,
                    formats = formats,
                    url = url
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun cancelDownload() {
        isCancelled.set(true)
        currentProcessId?.let { processId ->
            YoutubeDL.getInstance().destroyProcessById(processId)
        }
        currentProcessId = null
    }
    
    fun resetCancellation() {
        isCancelled.set(false)
        currentProcessId = null
    }
    
    suspend fun download(
        url: String,
        formatId: String?,
        audioOnly: Boolean = false,
        onProgress: (Float, String) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Reset cancellation state at start
            resetCancellation()
            
            val falconDir = getDownloadDirectory()
            if (!falconDir.exists()) {
                val created = falconDir.mkdirs()
                if (!created && !falconDir.exists()) {
                    return@withContext Result.failure(Exception("Failed to create download directory"))
                }
            }

            val request = YoutubeDLRequest(url)
            
            // Output template with sanitized filename
            request.addOption("-o", "${falconDir.absolutePath}/%(title).200s.%(ext)s")
            request.addOption("--no-playlist")
            request.addOption("--restrict-filenames")
            
            // Add options to bypass 403 errors
            addAntiBlockOptions(request)
            
            // Format selection
            if (audioOnly) {
                // Extract audio and convert to MP3
                request.addOption("-x")
                request.addOption("--audio-format", "mp3")
                request.addOption("--audio-quality", "0")
                // If a specific audio format is selected, use it
                if (formatId != null) {
                    request.addOption("-f", formatId)
                } else {
                    request.addOption("-f", "bestaudio/best")
                }
            } else if (formatId != null) {
                request.addOption("-f", "$formatId+bestaudio/best/$formatId")
                request.addOption("--merge-output-format", "mp4")
            } else {
                request.addOption("-f", "bestvideo[ext=mp4]+bestaudio[ext=m4a]/bestvideo+bestaudio/best")
                request.addOption("--merge-output-format", "mp4")
            }
            
            // Retry options
            request.addOption("--retries", "10")
            request.addOption("--fragment-retries", "10")
            request.addOption("--retry-sleep", "exp=1:20:2")
            
            // Continue partial downloads
            request.addOption("--continue")
            
            // Don't overwrite existing files
            request.addOption("--no-overwrites")
            
            var outputFile = ""
            
            // Generate unique process ID for this download
            val processId = System.currentTimeMillis().toString()
            currentProcessId = processId
            
            YoutubeDL.getInstance().execute(request, processId) { progress, etaInSeconds, line ->
                // Check if cancelled
                if (isCancelled.get()) {
                    return@execute
                }
                
                val progressPercent = progress.coerceIn(0f, 100f)
                val eta = if (etaInSeconds > 0) {
                    val mins = etaInSeconds / 60
                    val secs = etaInSeconds % 60
                    "ETA: ${mins}m ${secs}s"
                } else ""
                
                onProgress(progressPercent / 100f, "${progressPercent.toInt()}% $eta")
                
                // Capture output file path from various yt-dlp output patterns
                when {
                    line.contains("[download] Destination:") -> {
                        outputFile = line.substringAfter("[download] Destination:").trim()
                    }
                    line.contains("[Merger] Merging formats into") -> {
                        outputFile = line.substringAfter("[Merger] Merging formats into").trim()
                            .removeSurrounding("\"").removeSurrounding("'")
                    }
                    line.contains("[ExtractAudio] Destination:") -> {
                        outputFile = line.substringAfter("[ExtractAudio] Destination:").trim()
                    }
                    line.contains("[ffmpeg] Destination:") -> {
                        outputFile = line.substringAfter("[ffmpeg] Destination:").trim()
                    }
                    line.contains("has already been downloaded") -> {
                        // Extract path from "file.mp4 has already been downloaded"
                        val path = line.substringBefore(" has already been downloaded").trim()
                            .removePrefix("[download]").trim()
                        if (path.isNotEmpty()) outputFile = path
                    }
                }
            }
            
            // Check if was cancelled
            if (isCancelled.get()) {
                return@withContext Result.failure(Exception("Download cancelled"))
            }
            
            currentProcessId = null
            
            // If output file is empty or doesn't exist, try to find the most recent file
            val finalFile = if (outputFile.isNotEmpty() && File(outputFile).exists()) {
                outputFile
            } else {
                // Find the most recently modified file in the download directory
                falconDir.listFiles()
                    ?.filter { it.isFile }
                    ?.maxByOrNull { it.lastModified() }
                    ?.absolutePath ?: falconDir.absolutePath
            }
            
            Result.success(finalFile)
        } catch (e: Exception) {
            currentProcessId = null
            if (isCancelled.get()) {
                Result.failure(Exception("Download cancelled"))
            } else {
                Result.failure(e)
            }
        }
    }
    
    suspend fun updateYtDlp(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val status = YoutubeDL.getInstance().updateYoutubeDL(
                com.samyak.falcondownloader.FalconApp.instance,
                YoutubeDL.UpdateChannel.STABLE
            )
            Result.success(status?.name ?: "Updated")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Add options to bypass HTTP 403 and other blocking errors
     */
    private fun addAntiBlockOptions(request: YoutubeDLRequest) {
        // Use Android-like user agent
        request.addOption(
            "--user-agent",
            "Mozilla/5.0 (Linux; Android 13; SM-G991B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        )
        
        // Add referer header
        request.addOption("--referer", "https://www.youtube.com/")
        
        // Force IPv4 (some networks have IPv6 issues)
        request.addOption("--force-ipv4")
        
        // Ignore SSL errors (some networks have certificate issues)
        request.addOption("--no-check-certificates")
        
        // Use chunked downloads to avoid throttling
        request.addOption("--http-chunk-size", "10M")
        
        // Add sleep between requests to avoid rate limiting
        request.addOption("--sleep-requests", "1")
        
        // Extractor retries
        request.addOption("--extractor-retries", "3")
        
        // Don't use .part files (can cause issues on some devices)
        request.addOption("--no-part")
        
        // Geo bypass
        request.addOption("--geo-bypass")
        
        // Add cookies workaround for age-restricted content
        request.addOption("--no-warnings")
        
        // Buffer size
        request.addOption("--buffer-size", "16K")
    }
}
