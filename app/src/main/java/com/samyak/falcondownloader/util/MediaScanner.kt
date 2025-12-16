package com.samyak.falcondownloader.util

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileInputStream

object MediaScanner {

    /**
     * Scan a file to make it visible in the gallery
     */
    fun scanFile(
        context: Context,
        file: File,
        onComplete: ((String?, android.net.Uri?) -> Unit)? = null
    ) {
        MediaScannerConnection.scanFile(
            context,
            arrayOf(file.absolutePath),
            arrayOf(getMimeType(file))
        ) { path, uri ->
            onComplete?.invoke(path, uri)
        }
    }

    /**
     * Copy file to public Downloads/Movies folder and add to MediaStore
     * This makes the file visible in gallery apps
     */
    fun copyToPublicDirectory(context: Context, sourceFile: File): Result<File> {
        return try {
            if (!sourceFile.exists()) {
                return Result.failure(Exception("Source file does not exist"))
            }

            val mimeType = getMimeType(sourceFile)
            val isVideo = mimeType.startsWith("video/")
            val isAudio = mimeType.startsWith("audio/")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ use MediaStore API
                copyToMediaStoreQ(context, sourceFile, mimeType, isVideo, isAudio)
            } else {
                // Android 9 and below - copy to public directory directly
                copyToPublicDirectoryLegacy(context, sourceFile, isVideo, isAudio)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun copyToMediaStoreQ(
        context: Context,
        sourceFile: File,
        mimeType: String,
        isVideo: Boolean,
        isAudio: Boolean
    ): Result<File> {
        val resolver = context.contentResolver

        val relativePath = when {
            isVideo -> Environment.DIRECTORY_MOVIES + "/Falcon Downloader"
            isAudio -> Environment.DIRECTORY_MUSIC + "/Falcon Downloader"
            else -> Environment.DIRECTORY_DOWNLOADS + "/Falcon Downloader"
        }

        val collection = when {
            isVideo -> MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            isAudio -> MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            else -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Files.getContentUri("external")
            }
        }

        // Check if file already exists and delete it
        deleteExistingMediaStoreEntry(context, sourceFile.name, relativePath, isVideo, isAudio)

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, sourceFile.name)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val uri = resolver.insert(collection, contentValues)
            ?: return Result.failure(Exception("Failed to create MediaStore entry"))

        try {
            resolver.openOutputStream(uri)?.use { outputStream ->
                FileInputStream(sourceFile).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: return Result.failure(Exception("Failed to open output stream"))

            // Mark as not pending anymore
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val updateValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
                resolver.update(uri, updateValues, null, null)
            }

            // Get the actual file path for display
            val publicDir = when {
                isVideo -> Environment.DIRECTORY_MOVIES
                isAudio -> Environment.DIRECTORY_MUSIC
                else -> Environment.DIRECTORY_DOWNLOADS
            }
            val publicFile = File(
                Environment.getExternalStoragePublicDirectory(publicDir),
                "Falcon Downloader/${sourceFile.name}"
            )

            return Result.success(publicFile)
        } catch (e: Exception) {
            // Clean up on failure
            resolver.delete(uri, null, null)
            throw e
        }
    }

    private fun deleteExistingMediaStoreEntry(
        context: Context,
        fileName: String,
        relativePath: String,
        isVideo: Boolean,
        isAudio: Boolean
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return

        val resolver = context.contentResolver
        val collection = when {
            isVideo -> MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            isAudio -> MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            else -> MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }

        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} = ?"
        val selectionArgs = arrayOf(fileName, "$relativePath/")

        try {
            resolver.delete(collection, selection, selectionArgs)
        } catch (e: Exception) {
            // Ignore deletion errors
        }
    }

    private fun copyToPublicDirectoryLegacy(
        context: Context,
        sourceFile: File,
        isVideo: Boolean,
        isAudio: Boolean
    ): Result<File> {
        val publicDir = when {
            isVideo -> File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                "Falcon Downloader"
            )
            isAudio -> File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                "Falcon Downloader"
            )
            else -> File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "Falcon Downloader"
            )
        }

        if (!publicDir.exists()) {
            publicDir.mkdirs()
        }

        val destFile = File(publicDir, sourceFile.name)

        // Copy file
        sourceFile.inputStream().use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        // Scan the file to make it visible in gallery
        scanFile(context, destFile)

        return Result.success(destFile)
    }

    private fun getMimeType(file: File): String {
        val extension = file.extension.lowercase()
        return when (extension) {
            "mp4", "m4v" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "webm" -> "video/webm"
            "avi" -> "video/x-msvideo"
            "mov" -> "video/quicktime"
            "flv" -> "video/x-flv"
            "3gp" -> "video/3gpp"
            "mp3" -> "audio/mpeg"
            "m4a" -> "audio/mp4"
            "aac" -> "audio/aac"
            "ogg" -> "audio/ogg"
            "opus" -> "audio/opus"
            "wav" -> "audio/wav"
            "flac" -> "audio/flac"
            else -> "video/mp4" // Default to video/mp4 for unknown
        }
    }
}
