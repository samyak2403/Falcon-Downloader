package com.samyak.falcondownloader.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.samyak.falcondownloader.data.AppSettings
import com.samyak.falcondownloader.data.DownloadState
import com.samyak.falcondownloader.data.DownloadStatus
import com.samyak.falcondownloader.data.VideoFormat
import com.samyak.falcondownloader.data.VideoInfo
import com.samyak.falcondownloader.downloader.YtDlpDownloader
import com.samyak.falcondownloader.util.MediaScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class DownloadViewModel(application: Application) : AndroidViewModel(application) {
    
    private val downloader = YtDlpDownloader()
    private val settings = AppSettings.getInstance(application)
    
    private val _url = MutableStateFlow("")
    val url: StateFlow<String> = _url.asStateFlow()
    
    private val _videoInfo = MutableStateFlow<VideoInfo?>(null)
    val videoInfo: StateFlow<VideoInfo?> = _videoInfo.asStateFlow()
    
    private val _downloadState = MutableStateFlow(DownloadState())
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()
    
    private val _selectedFormat = MutableStateFlow<VideoFormat?>(null)
    val selectedFormat: StateFlow<VideoFormat?> = _selectedFormat.asStateFlow()
    
    private val _audioOnly = MutableStateFlow(settings.preferAudio.value)
    val audioOnly: StateFlow<Boolean> = _audioOnly.asStateFlow()
    
    private var downloadJob: Job? = null
    
    init {
        // Apply settings
        viewModelScope.launch {
            settings.preferAudio.collect { prefer ->
                if (_videoInfo.value == null) {
                    _audioOnly.value = prefer
                }
            }
        }
    }
    
    fun updateUrl(newUrl: String) {
        _url.value = newUrl
        if (newUrl.isEmpty()) {
            _videoInfo.value = null
            _downloadState.value = DownloadState()
        }
    }
    
    fun setAudioOnly(value: Boolean) {
        _audioOnly.value = value
        // Clear selected format when switching to audio only
        if (value) {
            _selectedFormat.value = null
        }
    }
    
    fun selectFormat(format: VideoFormat?) {
        _selectedFormat.value = format
        // If selecting an audio-only format, enable audio-only mode
        if (format?.isAudioOnly == true) {
            _audioOnly.value = true
        }
    }
    
    fun fetchVideoInfo() {
        val currentUrl = _url.value.trim()
        if (currentUrl.isEmpty()) return
        
        viewModelScope.launch {
            _downloadState.value = DownloadState(status = DownloadStatus.FETCHING_INFO)
            _videoInfo.value = null
            _selectedFormat.value = null

            downloader.getVideoInfo(currentUrl)
                .onSuccess { info ->
                    _videoInfo.value = info
                    _downloadState.value = DownloadState(status = DownloadStatus.IDLE)
                    // Apply default quality from settings
                    applyDefaultQuality(info)
                }
                .onFailure { error ->
                    _downloadState.value = DownloadState(
                        status = DownloadStatus.ERROR,
                        error = error.message ?: "Failed to fetch video info"
                    )
                }
        }
    }
    
    private fun applyDefaultQuality(info: VideoInfo) {
        val quality = settings.downloadQuality.value
        when (quality) {
            "audio" -> _audioOnly.value = true
            "best" -> _selectedFormat.value = null
            else -> {
                // Find format matching quality (e.g., "720", "1080")
                val targetHeight = quality.toIntOrNull() ?: return
                val matchingFormat = info.formats
                    .filter { !it.isAudioOnly }
                    .firstOrNull { format ->
                        format.resolution?.split("x")?.getOrNull(1)?.toIntOrNull() == targetHeight
                    }
                _selectedFormat.value = matchingFormat
            }
        }
    }
    
    fun startDownload() {
        val currentUrl = _url.value.trim()
        if (currentUrl.isEmpty()) return
        
        downloadJob = viewModelScope.launch {
            _downloadState.value = DownloadState(status = DownloadStatus.DOWNLOADING)
            
            val formatId = if (!_audioOnly.value) _selectedFormat.value?.formatId else null
            
            downloader.download(
                url = currentUrl,
                formatId = formatId,
                audioOnly = _audioOnly.value
            ) { progress, text ->
                _downloadState.value = DownloadState(
                    status = DownloadStatus.DOWNLOADING,
                    progress = progress,
                    progressText = text
                )
            }.onSuccess { filePath ->
                // Copy to public directory so it shows in gallery
                val sourceFile = File(filePath)
                if (sourceFile.exists()) {
                    withContext(Dispatchers.IO) {
                        MediaScanner.copyToPublicDirectory(getApplication(), sourceFile)
                            .onSuccess { publicFile ->
                                // Delete the original file from app directory to avoid duplicates
                                try {
                                    sourceFile.delete()
                                } catch (e: Exception) {
                                    // Ignore deletion errors
                                }
                                _downloadState.value = DownloadState(
                                    status = DownloadStatus.COMPLETED,
                                    progress = 1f,
                                    downloadedFile = publicFile.absolutePath
                                )
                            }
                            .onFailure {
                                // If copy fails, still mark as complete with original file
                                // and scan it so it appears in gallery
                                MediaScanner.scanFile(getApplication(), sourceFile)
                                _downloadState.value = DownloadState(
                                    status = DownloadStatus.COMPLETED,
                                    progress = 1f,
                                    downloadedFile = filePath
                                )
                            }
                    }
                } else {
                    _downloadState.value = DownloadState(
                        status = DownloadStatus.COMPLETED,
                        progress = 1f,
                        downloadedFile = filePath
                    )
                }
            }.onFailure { error ->
                _downloadState.value = DownloadState(
                    status = DownloadStatus.ERROR,
                    error = error.message ?: "Download failed"
                )
            }
        }
    }
    
    fun cancelDownload() {
        downloader.cancelDownload()
        downloadJob?.cancel()
        _downloadState.value = DownloadState(status = DownloadStatus.CANCELLED)
    }
    
    fun reset() {
        _url.value = ""
        _videoInfo.value = null
        _downloadState.value = DownloadState()
        _selectedFormat.value = null
        _audioOnly.value = settings.preferAudio.value
    }
    
    fun updateYtDlp(onResult: (String) -> Unit) {
        viewModelScope.launch {
            downloader.updateYtDlp()
                .onSuccess { onResult("yt-dlp updated: $it") }
                .onFailure { onResult("Update failed: ${it.message}") }
        }
    }
}
