package com.samyak.falcondownloader.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.samyak.falcondownloader.FalconApp
import com.samyak.falcondownloader.MainActivity
import com.samyak.falcondownloader.R

class DownloadService : Service() {
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification("Preparing download...")
        startForeground(NOTIFICATION_ID, notification)
        return START_NOT_STICKY
    }
    
    private fun createNotification(text: String, progress: Int = -1): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, FalconApp.DOWNLOAD_CHANNEL_ID)
            .setContentTitle("Falcon Downloader")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .apply {
                if (progress >= 0) {
                    setProgress(100, progress, false)
                } else {
                    setProgress(0, 0, true)
                }
            }
            .build()
    }
    
    companion object {
        const val NOTIFICATION_ID = 1001
    }
}
