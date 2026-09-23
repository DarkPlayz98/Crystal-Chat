package com.example.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat

object SmsHelper {
  private const val TAG = "SmsHelper"

  /**
   * Sends an SMS message directly in the background via Android's SmsManager.
   *
   * CRITICAL REQUIREMENT:
   * The SENDER is NEVER routed or redirected to their default messaging app!
   * The sender stays entirely inside Crystal Chat.
   * The RECEIVER (who does not have Crystal Chat) receives the message in their default SMS app.
   */
  fun sendDirectSmsInBackground(
    context: Context,
    phoneNumber: String,
    messageText: String
  ): Boolean {
    val cleanPhone = phoneNumber.filter { it.isDigit() || it == '+' }
    if (cleanPhone.isBlank()) return false

    val hasSmsPermission = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.SEND_SMS
    ) == PackageManager.PERMISSION_GRANTED

    if (hasSmsPermission) {
      return try {
        val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          context.getSystemService(SmsManager::class.java)
        } else {
          @Suppress("DEPRECATION")
          SmsManager.getDefault()
        }

        val parts = smsManager.divideMessage(messageText)
        if (parts.size > 1) {
          smsManager.sendMultipartTextMessage(cleanPhone, null, parts, null, null)
        } else {
          smsManager.sendTextMessage(cleanPhone, null, messageText, null, null)
        }
        Log.i(TAG, "Direct SMS dispatched in background to $cleanPhone")
        true
      } catch (e: Exception) {
        Log.w(TAG, "Direct SmsManager dispatch exception (swallowed to keep sender in-app): ${e.message}", e)
        false
      }
    } else {
      Log.i(TAG, "SEND_SMS permission not granted; processed as simulated in-app delivery without redirecting sender")
      return false
    }
  }

  /**
   * Backwards-compatible alias for background SMS dispatch.
   * NEVER opens or redirects the sender to any external application.
   */
  fun sendSmsWithFallback(
    context: Context,
    phoneNumber: String,
    messageText: String
  ): Boolean {
    return sendDirectSmsInBackground(context, phoneNumber, messageText)
  }

  /**
   * Only used if the user explicitly requests to open external system SMS via a manual menu item.
   * Never invoked automatically on message send.
   */
  fun openDefaultMessagingApp(
    context: Context,
    phoneNumber: String,
    messageText: String
  ): Boolean {
    return try {
      val cleanPhone = phoneNumber.filter { it.isDigit() || it == '+' }
      val uri = Uri.parse("smsto:${Uri.encode(cleanPhone)}")
      val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
        putExtra("sms_body", messageText)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(intent)
      true
    } catch (e: Exception) {
      Log.e(TAG, "Failed to open default SMS app with smsto:", e)
      false
    }
  }

  /**
   * Only used if explicitly requested from a manual menu.
   */
  fun openDefaultMessagingAppWithImage(
    context: Context,
    phoneNumber: String,
    imageUri: Uri,
    caption: String = ""
  ): Boolean {
    return try {
      val cleanPhone = phoneNumber.filter { it.isDigit() || it == '+' }
      val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/*"
        putExtra("address", cleanPhone)
        putExtra(Intent.EXTRA_STREAM, imageUri)
        if (caption.isNotBlank()) {
          putExtra("sms_body", caption)
          putExtra(Intent.EXTRA_TEXT, caption)
        }
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      val chooser = Intent.createChooser(intent, "Share Image via SMS/MMS").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(chooser)
      true
    } catch (e: Exception) {
      Log.e(TAG, "Failed to launch MMS sharing intent", e)
      false
    }
  }
}
