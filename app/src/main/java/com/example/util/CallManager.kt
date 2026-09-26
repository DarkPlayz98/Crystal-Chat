package com.example.util

import android.content.Context
import android.content.Intent
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.util.Log
import com.example.data.local.dao.CallDao
import com.example.data.local.model.CallEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.random.Random

enum class CallStateStatus {
  IDLE,
  OUTGOING_RINGING,
  INCOMING_RINGING,
  CONNECTED,
  ENDED
}

data class ActiveCallSession(
  val callId: String = "",
  val contactName: String = "",
  val phoneNumber: String = "",
  val handle: String? = null,
  val avatarColorHex: Long = 0xFF0D9488,
  val status: CallStateStatus = CallStateStatus.IDLE,
  val durationSeconds: Int = 0,
  val isMuted: Boolean = false,
  val isSpeakerOn: Boolean = false,
  val isHdVoice: Boolean = true,
  val isMinimized: Boolean = false,
  val statusMessage: String = "",
  val recipientHasApp: Boolean = true
)

class CallManager(
  private val context: Context,
  private val callDao: CallDao,
  private val scope: CoroutineScope
) {
  companion object {
    private const val TAG = "CallManager"

    /**
     * Dials a number using the device's default system phone / caller app.
     */
    fun dialWithDefaultCallerApp(context: Context, phoneNumber: String) {
      try {
        val cleanNumber = phoneNumber.trim().replace(" ", "")
        val intent = Intent(Intent.ACTION_DIAL).apply {
          data = Uri.parse("tel:$cleanNumber")
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
      } catch (e: Exception) {
        Log.e(TAG, "Failed to launch default caller app: ${e.message}", e)
      }
    }
  }

  private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

  private val _activeCall = MutableStateFlow<ActiveCallSession?>(null)
  val activeCall: StateFlow<ActiveCallSession?> = _activeCall.asStateFlow()

  // Dynamic visualizer amplitudes for HD+ voice visualizer
  private val _waveformHeights = MutableStateFlow(List(16) { 0.2f })
  val waveformHeights: StateFlow<List<Float>> = _waveformHeights.asStateFlow()

  private var timerJob: Job? = null
  private var waveJob: Job? = null
  private var ringtoneJob: Job? = null

  private var ringtonePlayer: Ringtone? = null
  private var toneGenerator: ToneGenerator? = null

  /**
   * Starts an outgoing call.
   * If recipient does not have the app, caller app should be launched instead.
   */
  fun startOutgoingCall(
    contactName: String,
    phoneNumber: String,
    handle: String? = null,
    avatarColorHex: Long = 0xFF0D9488,
    recipientHasApp: Boolean = true
  ) {
    val callId = "call_${UUID.randomUUID().toString().take(8)}"
    val session = ActiveCallSession(
      callId = callId,
      contactName = contactName.ifBlank { phoneNumber },
      phoneNumber = phoneNumber,
      handle = handle,
      avatarColorHex = avatarColorHex,
      status = CallStateStatus.OUTGOING_RINGING,
      statusMessage = if (recipientHasApp) "Ringing (Playing Caller Tune)..." else "Calling Cellular Receiver...",
      recipientHasApp = recipientHasApp
    )
    _activeCall.value = session

    setupAudioForCall()
    startWaveformAnimation()
    startCallerTune()
  }

  /**
   * Plays the musical Caller Tune to the caller while the recipient is ringing.
   */
  fun startCallerTune(tune: CallerTuneStyle = CallerTunePlayer.getCurrentTune()) {
    stopCallerTune()
    val isSpeaker = _activeCall.value?.isSpeakerOn == true
    CallerTunePlayer.startCallerTune(context, scope, tune, isSpeaker)
  }

  fun changeCallerTune(tune: CallerTuneStyle) {
    if (_activeCall.value?.status == CallStateStatus.OUTGOING_RINGING) {
      startCallerTune(tune)
    } else {
      CallerTunePlayer.setCallerTune(tune)
    }
  }

  private fun stopCallerTune() {
    CallerTunePlayer.stopCallerTune()
    ringtoneJob?.cancel()
    ringtoneJob = null
  }

  /**
   * Plays audible DTMF tone when keypad digits are pressed during a call.
   */
  fun playDtmfTone(digit: Char) {
    scope.launch(Dispatchers.Default) {
      try {
        val tg = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 80)
        val tone = when (digit) {
          '0' -> ToneGenerator.TONE_DTMF_0
          '1' -> ToneGenerator.TONE_DTMF_1
          '2' -> ToneGenerator.TONE_DTMF_2
          '3' -> ToneGenerator.TONE_DTMF_3
          '4' -> ToneGenerator.TONE_DTMF_4
          '5' -> ToneGenerator.TONE_DTMF_5
          '6' -> ToneGenerator.TONE_DTMF_6
          '7' -> ToneGenerator.TONE_DTMF_7
          '8' -> ToneGenerator.TONE_DTMF_8
          '9' -> ToneGenerator.TONE_DTMF_9
          '*' -> ToneGenerator.TONE_DTMF_S
          '#' -> ToneGenerator.TONE_DTMF_P
          else -> ToneGenerator.TONE_PROP_BEEP
        }
        tg.startTone(tone, 150)
        delay(160)
        tg.release()
      } catch (e: Exception) {
        Log.w(TAG, "DTMF error: ${e.message}")
      }
    }
  }

  /**
   * User or remote party answers the call.
   */
  fun answerIncomingCall() {
    connectCall()
  }

  /**
   * Connects the active voice session.
   */
  fun connectCall() {
    val curr = _activeCall.value ?: return
    stopCallerTune()

    _activeCall.value = curr.copy(
      status = CallStateStatus.CONNECTED,
      statusMessage = "Crystal HD+ Voice Connected"
    )

    timerJob?.cancel()
    timerJob = scope.launch(Dispatchers.Default) {
      var seconds = 0
      while (isActive && _activeCall.value?.status == CallStateStatus.CONNECTED) {
        delay(1000)
        seconds++
        _activeCall.value = _activeCall.value?.copy(durationSeconds = seconds)
      }
    }
  }

  fun toggleMute() {
    val curr = _activeCall.value ?: return
    val newMute = !curr.isMuted
    try {
      audioManager?.isMicrophoneMute = newMute
    } catch (e: Exception) {
      Log.w(TAG, "Mute toggle: ${e.message}")
    }
    _activeCall.value = curr.copy(isMuted = newMute)
  }

  fun toggleSpeaker() {
    val curr = _activeCall.value ?: return
    val newSpeaker = !curr.isSpeakerOn
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (newSpeaker) {
          val speakerDevice = audioManager?.availableCommunicationDevices?.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
          }
          if (speakerDevice != null) {
            audioManager?.setCommunicationDevice(speakerDevice)
          } else {
            @Suppress("DEPRECATION")
            audioManager?.isSpeakerphoneOn = true
          }
        } else {
          audioManager?.clearCommunicationDevice()
          @Suppress("DEPRECATION")
          audioManager?.isSpeakerphoneOn = false
        }
      } else {
        @Suppress("DEPRECATION")
        audioManager?.isSpeakerphoneOn = newSpeaker
      }
    } catch (e: Exception) {
      Log.w(TAG, "Speaker toggle: ${e.message}")
    }
    _activeCall.value = curr.copy(isSpeakerOn = newSpeaker)
  }

  fun toggleMinimize(minimize: Boolean) {
    val curr = _activeCall.value ?: return
    _activeCall.value = curr.copy(isMinimized = minimize)
  }

  fun endCall(reason: String = "Call Ended") {
    val curr = _activeCall.value ?: return
    timerJob?.cancel()
    waveJob?.cancel()
    stopCallerTune()

    // Play call-end disconnect tone
    scope.launch(Dispatchers.Default) {
      try {
        val tg = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 70)
        tg.startTone(ToneGenerator.TONE_PROP_PROMPT, 200)
        delay(220)
        tg.release()
      } catch (e: Exception) {
        // Ignore
      }
    }

    val duration = curr.durationSeconds
    val entity = CallEntity(
      id = curr.callId,
      contactName = curr.contactName,
      phoneNumber = curr.phoneNumber,
      handle = curr.handle,
      timestamp = System.currentTimeMillis(),
      durationSeconds = duration,
      callType = if (curr.status == CallStateStatus.CONNECTED) "OUTGOING" else "MISSED",
      isHdVoice = true,
      avatarColorHex = curr.avatarColorHex
    )

    scope.launch(Dispatchers.IO) {
      callDao.insert(entity)
    }

    _activeCall.value = curr.copy(
      status = CallStateStatus.ENDED,
      statusMessage = reason
    )

    resetAudio()

    scope.launch(Dispatchers.Default) {
      delay(700)
      _activeCall.value = null
    }
  }

  private fun setupAudioForCall() {
    try {
      audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
      audioManager?.isMicrophoneMute = false
    } catch (e: Exception) {
      Log.w(TAG, "setupAudioForCall: ${e.message}")
    }
  }

  private fun resetAudio() {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        audioManager?.clearCommunicationDevice()
      }
      @Suppress("DEPRECATION")
      audioManager?.isSpeakerphoneOn = false
      audioManager?.isMicrophoneMute = false
      audioManager?.mode = AudioManager.MODE_NORMAL
    } catch (e: Exception) {
      Log.w(TAG, "resetAudio: ${e.message}")
    }
  }

  private fun startWaveformAnimation() {
    waveJob?.cancel()
    waveJob = scope.launch(Dispatchers.Default) {
      while (isActive && (_activeCall.value?.status == CallStateStatus.OUTGOING_RINGING ||
          _activeCall.value?.status == CallStateStatus.CONNECTED)) {
        val isMuted = _activeCall.value?.isMuted == true
        val isConnected = _activeCall.value?.status == CallStateStatus.CONNECTED
        val newList = List(16) { index ->
          if (isMuted) {
            0.08f
          } else if (isConnected) {
            0.2f + Random.nextFloat() * 0.75f
          } else {
            0.15f + ((index % 4) * 0.12f)
          }
        }
        _waveformHeights.value = newList
        delay(90)
      }
    }
  }
}
