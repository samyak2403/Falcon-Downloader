package com.samyak.falcondownloader.viewmodel

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samyak.falcondownloader.data.DownloadedFile
import com.samyak.falcondownloader.downloader.YtDlpDownloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class DownloadsViewModel : ViewModel() {
    
    private val _files = MutableStateFlow<List<DownloadedFile>>(emptyList())
    val files: StateFlow<List<DownloadedFile>> = _files.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _selectedFilter = MutableStateFlow(FilterType.ALL)
    val selectedFilter: StateFlow<FilterType> = _selectedFilter.asStateFlow()
    
    init {
        loadFiles()
    }
    
    fun loadFiles() {
        viewModelScope.launch {
            _isLoading.value = true
            _files.value = withContext(Dispatchers.IO) {
                getDownloadedFiles()
            }
            _isLoading.value = false
        }
    }
    
    fun setFilter(filter: FilterType) {
        _selectedFilter.value = filter
    }
    
    fun deleteFile(file: DownloadedFile) {
        viewModelScope.launch(Dispatchers.IO) {
            if (file.file.delete()) {
                loadFiles()
            }
        }
    }
    
    fun getFilteredFiles(): List<DownloadedFile> {
        return when (_selectedFilter.value) {
            FilterType.ALL -> _files.value
            FilterType.VIDEO -> _files.value.filter { it.isVideo }
            FilterType.AUDIO -> _files.value.filter { it.isAudio }
        }
    }
    
    private fun getDownloadedFiles(): List<DownloadedFile> {
        val allFiles = mutableListOf<DownloadedFile>()
        
        // Get files from app's private directory
        val appDir = YtDlpDownloader.getDownloadDirectory()
        if (appDir.exists()) {
            appDir.listFiles()
                ?.filter { it.isFile }
                ?.map { DownloadedFile.fromFile(it) }
                ?.let { allFiles.addAll(it) }
        }
        
        // Get files from public Movies/Falcon Downloader
        val publicMoviesDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            "Falcon Downloader"
        )
        if (publicMoviesDir.exists()) {
            publicMoviesDir.listFiles()
                ?.filter { it.isFile }
                ?.map { DownloadedFile.fromFile(it) }
                ?.let { allFiles.addAll(it) }
        }
        
        // Get files from public Music/Falcon Downloader
        val publicMusicDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            "Falcon Downloader"
        )
        if (publicMusicDir.exists()) {
            publicMusicDir.listFiles()
                ?.filter { it.isFile }
                ?.map { DownloadedFile.fromFile(it) }
                ?.let { allFiles.addAll(it) }
        }
        
        // Get files from public Downloads/Falcon Downloader
        val publicDownloadsDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "Falcon Downloader"
        )
        if (publicDownloadsDir.exists()) {
            publicDownloadsDir.listFiles()
                ?.filter { it.isFile }
                ?.map { DownloadedFile.fromFile(it) }
                ?.let { allFiles.addAll(it) }
        }
        
        // Remove duplicates by file name and sort by date
        return allFiles
            .distinctBy { it.name }
            .sortedByDescending { it.lastModified }
    }
    
    enum class FilterType {
        ALL, VIDEO, AUDIO
    }
}
