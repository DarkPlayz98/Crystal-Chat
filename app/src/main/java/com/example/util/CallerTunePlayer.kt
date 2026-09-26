package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

enum class CallerTuneStyle(val displayName: String) {
  CRYSTAL_SYMPHONY("Crystal Symphony 🎵"),
  ACOUSTIC_MELODY("Acoustic Melody 🎸"),
  CHIME_SERENADE("Chime Serenade 🔔")
}

/**
 * CallerTunePlayer plays rich musical melodies (Caller Tune / Ringback Music)
 * to the caller while waiting for the recipient to answer an outgoing call.
 */
object CallerTunePlayer {
  private const val TAG = "CallerTunePlayer"
  private const val SAMPLE_RATE = 44100

  private var audioTrack: AudioTrack? = null
  private var playbackJob: Job? = null
  private var currentTune = CallerTuneStyle.CRYSTAL_SYMPHONY

  fun getCurrentTune(): CallerTuneStyle = currentTune

  fun setCallerTune(tune: CallerTuneStyle) {
    currentTune = tune
  }

  fun startCallerTune(
    context: Context,
    scope: CoroutineScope,
    tune: CallerTuneStyle = currentTune,
    isSpeakerOn: Boolean = false
  ) {
    stopCallerTune()
    currentTune = tune

    playbackJob = scope.launch(Dispatchers.Default) {
      try {
        val streamType = if (isSpeakerOn) AudioManager.STREAM_MUSIC else AudioManager.STREAM_VOICE_CALL
        val bufferSize = AudioTrack.getMinBufferSize(
          SAMPLE_RATE,
          AudioFormat.CHANNEL_OUT_MONO,
          AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(SAMPLE_RATE * 2)

        val track = AudioTrack.Builder()
          .setAudioAttributes(
            AudioAttributes.Builder()
              .setUsage(
                if (isSpeakerOn) AudioAttributes.USAGE_MEDIA
                else AudioAttributes.USAGE_VOICE_COMMUNICATION
              )
              .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
              .build()
          )
          .setAudioFormat(
            AudioFormat.Builder()
              .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
              .setSampleRate(SAMPLE_RATE)
              .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
              .build()
          )
          .setBufferSizeInBytes(bufferSize)
          .setTransferMode(AudioTrack.MODE_STREAM)
          .build()

        audioTrack = track
        track.play()

        // Generate musical notes buffer according to chosen melody
        val audioData = generateMelodyBuffer(tune)

        Log.i(TAG, "Caller tune started: ${tune.displayName}")

        // Loop the caller tune continuously while active
        while (isActive) {
          track.write(audioData, 0, audioData.size)
        }
      } catch (e: Exception) {
        Log.e(TAG, "Error playing caller tune: ${e.message}", e)
      } finally {
        cleanupTrack()
      }
    }
  }

  fun stopCallerTune() {
    playbackJob?.cancel()
    playbackJob = null
    cleanupTrack()
  }

  private fun cleanupTrack() {
    try {
      audioTrack?.let {
        if (it.playState == AudioTrack.PLAYSTATE_PLAYING) {
          it.stop()
        }
        it.release()
      }
    } catch (e: Exception) {
      Log.w(TAG, "Error cleaning up audio track: ${e.message}")
    }
    audioTrack = null
  }

  /**
   * Generates a 4-second harmonic polyphonic PCM sequence with smooth attack and decay.
   */
  private fun generateMelodyBuffer(tune: CallerTuneStyle): ShortArray {
    val durationSeconds = 4.0
    val totalSamples = (SAMPLE_RATE * durationSeconds).toInt()
    val samples = ShortArray(totalSamples)

    // Note frequencies in Hz
    // C4=261.63, D4=293.66, E4=329.63, F4=349.23, G4=392.00, A4=440.00, B4=493.88, C5=523.25
    val progression = when (tune) {
      CallerTuneStyle.CRYSTAL_SYMPHONY -> listOf(
        NoteEvent(0.0, 1.0, listOf(261.63, 329.63, 392.00, 523.25)), // C maj
        NoteEvent(1.0, 1.0, listOf(220.00, 261.63, 329.63, 440.00)), // A min
        NoteEvent(2.0, 1.0, listOf(174.61, 261.63, 349.23, 440.00)), // F maj
        NoteEvent(3.0, 1.0, listOf(196.00, 293.66, 392.00, 493.88))  // G maj
      )
      CallerTuneStyle.ACOUSTIC_MELODY -> listOf(
        NoteEvent(0.0, 0.9, listOf(329.63, 392.00, 493.88, 659.25)), // Em
        NoteEvent(0.9, 0.9, listOf(261.63, 329.63, 392.00, 523.25)), // C
        NoteEvent(1.8, 0.9, listOf(196.00, 246.94, 293.66, 392.00)), // G
        NoteEvent(2.7, 1.3, listOf(293.66, 369.99, 440.00, 587.33))  // D
      )
      CallerTuneStyle.CHIME_SERENADE -> listOf(
        NoteEvent(0.0, 0.8, listOf(523.25, 659.25, 783.99)), // C5, E5, G5 chimes
        NoteEvent(0.8, 0.8, listOf(587.33, 739.99, 880.00)), // D5, F#5, A5
        NoteEvent(1.6, 0.8, listOf(659.25, 783.99, 987.77)), // E5, G5, B5
        NoteEvent(2.4, 1.6, listOf(523.25, 659.25, 1046.50)) // C5, E5, C6
      )
    }

    for (event in progression) {
      val startSample = (event.startTimeSec * SAMPLE_RATE).toInt()
      val durationSample = (event.durationSec * SAMPLE_RATE).toInt()

      for (i in 0 until durationSample) {
        val sampleIndex = startSample + i
        if (sampleIndex >= totalSamples) break

        val t = i.toDouble() / SAMPLE_RATE
        val envelope = (1.0 - exp(-3.0 * t)) * exp(-1.8 * t) // ADSR acoustic bell envelope

        var mixedWave = 0.0
        for ((freqIdx, freq) in event.frequencies.withIndex()) {
          val weight = 1.0 / (freqIdx + 1)
          // Fundamental + soft 2nd harmonic
          val wave = sin(2.0 * PI * freq * t) + 0.35 * sin(4.0 * PI * freq * t)
          mixedWave += wave * weight
        }

        val sampleValue = (mixedWave * envelope * 0.45 * Short.MAX_VALUE).toInt()
          .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())

        // Add with saturation
        val existing = samples[sampleIndex].toInt()
        val combined = (existing + sampleValue).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
        samples[sampleIndex] = combined.toShort()
      }
    }

    return samples
  }

  private data class NoteEvent(
    val startTimeSec: Double,
    val durationSec: Double,
    val frequencies: List<Double>
  )
}
