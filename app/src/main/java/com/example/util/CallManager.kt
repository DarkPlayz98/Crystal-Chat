package com.example.util

import android.content.Context
import android.media.AudioManager
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
  val statusMessage: String = ""
)

class CallManager(
  private val context: Context,
  private val callDao: CallDao,
  private val scope: CoroutineScope
) {
  companion object {
    private const val TAG = "CallManager"
  }

  private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

  private val _activeCall = MutableStateFlow<ActiveCallSession?>(null)
  val activeCall: StateFlow<ActiveCallSession?> = _activeCall.asStateFlow()

  // Audio wave visualization amplitude for HD+ voice visualizer
  private val _waveformHeights = MutableStateFlow(List(16) { 0.2f })
  val waveformHeights: StateFlow<List<Float>> = _waveformHeights.asStateFlow()

  private var timerJob: Job? = null
  private var waveJob: Job? = null
  private var simulatedPickupJob: Job? = null

  fun startOutgoingCall(
    contactName: String,
    phoneNumber: String,
    handle: String? = null,
    avatarColorHex: Long = 0xFF0D9488
  ) {
    val callId = "call_${UUID.randomUUID().toString().take(8)}"
    val session = ActiveCallSession(
      callId = callId,
      contactName = contactName.ifBlank { phoneNumber },
      phoneNumber = phoneNumber,
      handle = handle,
      avatarColorHex = avatarColorHex,
      status = CallStateStatus.OUTGOING_RINGING,
      statusMessage = "Calling with HD+ Voice..."
    )
    _activeCall.value = session

    try {
      audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
      audioManager?.isMicrophoneMute = false
    } catch (e: Exception) {
      Log.w(TAG, "AudioManager setup failed: ${e.message}")
    }

    startWaveformAnimation()

    // Seamless connection: simulate pickup after 3 seconds for immediate voice readiness
    simulatedPickupJob?.cancel()
    simulatedPickupJob = scope.launch(Dispatchers.Default) {
      delay(2800)
      if (_activeCall.value?.status == CallStateStatus.OUTGOING_RINGING) {
        connectCall()
      }
    }
  }

  fun answerIncomingCall() {
    connectCall()
  }

  private fun connectCall() {
    val curr = _activeCall.value ?: return
    _activeCall.value = curr.copy(
      status = CallStateStatus.CONNECTED,
      statusMessage = "HD+ Voice Connected (Opus 48kHz)"
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
      audioManager?.isSpeakerphoneOn = newSpeaker
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
    simulatedPickupJob?.cancel()

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

    try {
      audioManager?.mode = AudioManager.MODE_NORMAL
      audioManager?.isSpeakerphoneOn = false
      audioManager?.isMicrophoneMute = false
    } catch (e: Exception) {
      Log.w(TAG, "AudioManager reset: ${e.message}")
    }

    _activeCall.value = curr.copy(
      status = CallStateStatus.ENDED,
      statusMessage = reason
    )

    scope.launch(Dispatchers.Default) {
      delay(1200)
      _activeCall.value = null
    }
  }

  private fun startWaveformAnimation() {
    waveJob?.cancel()
    waveJob = scope.launch(Dispatchers.Default) {
      while (isActive && _activeCall.value != null) {
        val session = _activeCall.value
        if (session?.status == CallStateStatus.CONNECTED && !session.isMuted) {
          _waveformHeights.value = List(16) { Random.nextFloat().coerceIn(0.15f, 0.95f) }
        } else if (session?.status == CallStateStatus.OUTGOING_RINGING) {
          val phase = (System.currentTimeMillis() % 1500) / 1500f
          val wave = (kotlin.math.sin(phase * 2 * Math.PI) * 0.35f + 0.5f).toFloat()
          _waveformHeights.value = List(16) { wave.coerceIn(0.2f, 0.85f) }
        } else {
          _waveformHeights.value = List(16) { 0.15f }
        }
        delay(90)
      }
    }
  }
}
