package com.samyak.falcondownloader.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppSettings(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    private val _downloadQuality = MutableStateFlow(prefs.getString(KEY_DOWNLOAD_QUALITY, "best") ?: "best")
    val downloadQuality: StateFlow<String> = _downloadQuality.asStateFlow()
    
    private val _preferAudio = MutableStateFlow(prefs.getBoolean(KEY_PREFER_AUDIO, false))
    val preferAudio: StateFlow<Boolean> = _preferAudio.asStateFlow()
    
    private val _darkMode = MutableStateFlow(prefs.getString(KEY_DARK_MODE, "system") ?: "system")
    val darkMode: StateFlow<String> = _darkMode.asStateFlow()
    
    private val _autoUpdate = MutableStateFlow(prefs.getBoolean(KEY_AUTO_UPDATE, true))
    val autoUpdate: StateFlow<Boolean> = _autoUpdate.asStateFlow()
    
    private val _concurrentDownloads = MutableStateFlow(prefs.getInt(KEY_CONCURRENT_DOWNLOADS, 1))
    val concurrentDownloads: StateFlow<Int> = _concurrentDownloads.asStateFlow()
    
    fun setDownloadQuality(quality: String) {
        prefs.edit().putString(KEY_DOWNLOAD_QUALITY, quality).apply()
        _downloadQuality.value = quality
    }
    
    fun setPreferAudio(prefer: Boolean) {
        prefs.edit().putBoolean(KEY_PREFER_AUDIO, prefer).apply()
        _preferAudio.value = prefer
    }
    
    fun setDarkMode(mode: String) {
        prefs.edit().putString(KEY_DARK_MODE, mode).apply()
        _darkMode.value = mode
    }
    
    fun setAutoUpdate(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_UPDATE, enabled).apply()
        _autoUpdate.value = enabled
    }
    
    fun setConcurrentDownloads(count: Int) {
        prefs.edit().putInt(KEY_CONCURRENT_DOWNLOADS, count).apply()
        _concurrentDownloads.value = count
    }
    
    fun getDownloadPath(): String {
        return prefs.getString(KEY_DOWNLOAD_PATH, "") ?: ""
    }
    
    companion object {
        private const val PREFS_NAME = "falcon_settings"
        private const val KEY_DOWNLOAD_QUALITY = "download_quality"
        private const val KEY_PREFER_AUDIO = "prefer_audio"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_AUTO_UPDATE = "auto_update"
        private const val KEY_CONCURRENT_DOWNLOADS = "concurrent_downloads"
        private const val KEY_DOWNLOAD_PATH = "download_path"
        
        @Volatile
        private var instance: AppSettings? = null
        
        fun getInstance(context: Context): AppSettings {
            return instance ?: synchronized(this) {
                instance ?: AppSettings(context.applicationContext).also { instance = it }
            }
        }
    }
}
