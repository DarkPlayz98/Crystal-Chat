package com.example.util

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.UUID

object ImageStorageHelper {
  private const val TAG = "ImageStorageHelper"

  /**
   * Copies the chosen visual media from PhotoPicker Uri to local app storage,
   * guaranteeing persistent read access without expiring or losing content permissions.
   */
  fun saveImageFromUri(context: Context, uri: Uri): Pair<File, Long>? {
    return try {
      val mediaDir = File(context.filesDir, "chat_images")
      if (!mediaDir.exists()) {
        mediaDir.mkdirs()
      }

      val fileName = "img_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg"
      val destinationFile = File(mediaDir, fileName)

      context.contentResolver.openInputStream(uri)?.use { input ->
        FileOutputStream(destinationFile).use { output ->
          input.copyTo(output)
        }
      } ?: return null

      Pair(destinationFile, destinationFile.length())
    } catch (e: Exception) {
      Log.e(TAG, "Failed to copy image to internal storage", e)
      null
    }
  }

  fun formatFileSize(bytes: Long): String {
    return when {
      bytes < 1024 -> "$bytes B"
      bytes < 1024 * 1024 -> "${bytes / 1024} KB"
      else -> String.format(Locale.US, "%.1f MB", bytes.toDouble() / (1024 * 1024))
    }
  }
}
