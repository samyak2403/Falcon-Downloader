package com.samyak.falcondownloader

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.aria2c.Aria2c
import com.yausername.ffmpeg.FFmpeg
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FalconApp : Application() {
    
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
        initYoutubeDL()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                DOWNLOAD_CHANNEL_ID,
                "Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Video download progress"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
    
    private fun initYoutubeDL() {
        applicationScope.launch(Dispatchers.IO) {
            try {
                YoutubeDL.getInstance().init(this@FalconApp)
                FFmpeg.getInstance().init(this@FalconApp)
                Aria2c.getInstance().init(this@FalconApp)
                
                // Auto-update yt-dlp to fix 403 errors and get latest extractors
                try {
                    YoutubeDL.getInstance().updateYoutubeDL(
                        this@FalconApp,
                        YoutubeDL.UpdateChannel.STABLE
                    )
                } catch (e: Exception) {
                    // Ignore update errors, will use existing version
                    e.printStackTrace()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    companion object {
        const val DOWNLOAD_CHANNEL_ID = "falcon_downloads"
        lateinit var instance: FalconApp
            private set
    }
}
