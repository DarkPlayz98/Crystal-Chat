package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.ActiveCallSession
import com.example.util.CallStateStatus

@Composable
fun VoiceCallScreen(
  session: ActiveCallSession,
  waveformHeights: List<Float>,
  onEndCall: () -> Unit,
  onToggleMute: () -> Unit,
  onToggleSpeaker: () -> Unit,
  onMinimize: () -> Unit
) {
  val isConnected = session.status == CallStateStatus.CONNECTED

  val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = 1.15f,
    animationSpec = infiniteRepeatable(
      animation = tween(1400, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulseScale"
  )

  val durationText = formatCallDuration(session.durationSeconds)

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(
            Color(0xFF090D16),
            Color(0xFF0F172A),
            Color(0xFF020617)
          )
        )
      )
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 24.dp, vertical = 40.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      // Top Bar: Minimize and Security Badge
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = onMinimize,
          modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f))
            .testTag("minimize_call_button")
        ) {
          Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = "Minimize Call",
            tint = Color.White
          )
        }

        // HD+ Voice Security Chip
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = Color(0xFF10B981).copy(alpha = 0.15f),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f))
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Lock,
              contentDescription = null,
              tint = Color(0xFF10B981),
              modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "HD+ Voice • End-to-End Encrypted",
              color = Color(0xFF10B981),
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold
            )
          }
        }

        Spacer(modifier = Modifier.size(44.dp))
      }

      // Middle: Avatar, Name, Pulsing Waveforms & Call Timer
      Column(
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Outer Pulsing Ambient Ring
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier.size(180.dp)
        ) {
          if (isConnected) {
            Box(
              modifier = Modifier
                .size(180.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(Color(0xFF0284C7).copy(alpha = 0.12f))
            )
            Box(
              modifier = Modifier
                .size(150.dp)
                .scale(pulseScale * 0.95f)
                .clip(CircleShape)
                .background(Color(0xFF38BDF8).copy(alpha = 0.15f))
            )
          }

          // Main Avatar Circle
          Box(
            modifier = Modifier
              .size(110.dp)
              .clip(CircleShape)
              .background(
                Brush.linearGradient(
                  colors = listOf(
                    Color(session.avatarColorHex),
                    Color(session.avatarColorHex).copy(alpha = 0.7f)
                  )
                )
              )
              .border(3.dp, Color.White.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
          ) {
            val initial = session.contactName.take(1).uppercase().ifBlank { "C" }
            Text(
              text = initial,
              fontSize = 44.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Contact Name
        Text(
          text = session.contactName,
          fontSize = 26.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Phone Number or Handle
        Text(
          text = session.handle ?: session.phoneNumber,
          fontSize = 14.sp,
          color = Color.White.copy(alpha = 0.65f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Call State & Timer
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = Color.White.copy(alpha = 0.08f)
        ) {
          Text(
            text = if (isConnected) durationText else session.statusMessage,
            fontSize = if (isConnected) 18.sp else 13.sp,
            fontWeight = if (isConnected) FontWeight.Bold else FontWeight.Medium,
            color = if (isConnected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.8f),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
          )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Minimalist Dynamic Audio Waveform
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 32.dp),
          horizontalArrangement = Arrangement.SpaceEvenly,
          verticalAlignment = Alignment.CenterVertically
        ) {
          waveformHeights.forEach { amplitude ->
            val barHeight = (amplitude * 36.dp.value).coerceIn(4f, 40f).dp
            Box(
              modifier = Modifier
                .width(4.dp)
                .height(barHeight)
                .clip(RoundedCornerShape(2.dp))
                .background(
                  if (isConnected) {
                    if (session.isMuted) Color.White.copy(alpha = 0.2f)
                    else Color(0xFF38BDF8)
                  } else {
                    Color.White.copy(alpha = 0.35f)
                  }
                )
            )
          }
        }

        // Codec Quality Indicator
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "Opus 48 kHz Ultra HD Audio • Low Latency",
          fontSize = 11.sp,
          color = Color.White.copy(alpha = 0.4f)
        )
      }

      // Bottom Control Dock
      Surface(
        shape = RoundedCornerShape(32.dp),
        color = Color(0xFF1E293B).copy(alpha = 0.85f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp, horizontal = 24.dp),
          horizontalArrangement = Arrangement.SpaceEvenly,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Mute Button
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(
              onClick = onToggleMute,
              modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                  if (session.isMuted) Color.White else Color.White.copy(alpha = 0.12f)
                )
                .testTag("toggle_call_mute_button")
            ) {
              Icon(
                imageVector = if (session.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                contentDescription = "Mute",
                tint = if (session.isMuted) Color(0xFF0F172A) else Color.White,
                modifier = Modifier.size(24.dp)
              )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = if (session.isMuted) "Unmute" else "Mute",
              color = Color.White.copy(alpha = 0.7f),
              fontSize = 12.sp
            )
          }

          // End Call Button (Prominent Red FAB)
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(
              onClick = onEndCall,
              modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(Color(0xFFEF4444))
                .testTag("end_call_button")
            ) {
              Icon(
                imageVector = Icons.Default.CallEnd,
                contentDescription = "End Call",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
              )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "End",
              color = Color(0xFFEF4444),
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold
            )
          }

          // Speaker Button
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(
              onClick = onToggleSpeaker,
              modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                  if (session.isSpeakerOn) Color.White else Color.White.copy(alpha = 0.12f)
                )
                .testTag("toggle_call_speaker_button")
            ) {
              Icon(
                imageVector = if (session.isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeDown,
                contentDescription = "Speaker",
                tint = if (session.isSpeakerOn) Color(0xFF0F172A) else Color.White,
                modifier = Modifier.size(24.dp)
              )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = if (session.isSpeakerOn) "Speaker On" else "Speaker",
              color = Color.White.copy(alpha = 0.7f),
              fontSize = 12.sp
            )
          }
        }
      }
    }
  }
}

/**
 * Minimized floating call banner when user is navigating chats during a call.
 */
@Composable
fun MinimizedCallBanner(
  session: ActiveCallSession,
  onExpand: () -> Unit,
  onEndCall: () -> Unit
) {
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 6.dp)
      .clip(RoundedCornerShape(16.dp))
      .clickable { onExpand() }
      .testTag("minimized_call_banner"),
    shape = RoundedCornerShape(16.dp),
    color = Color(0xFF065F46),
    tonalElevation = 8.dp
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color(0xFF10B981)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Phone,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = "HD+ Call with ${session.contactName}",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
          )
          Text(
            text = formatCallDuration(session.durationSeconds) + " • Tap to return",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 11.sp
          )
        }
      }

      IconButton(
        onClick = onEndCall,
        modifier = Modifier
          .size(36.dp)
          .clip(CircleShape)
          .background(Color(0xFFEF4444))
          .testTag("minimized_end_call_button")
      ) {
        Icon(
          imageVector = Icons.Default.CallEnd,
          contentDescription = "End Call",
          tint = Color.White,
          modifier = Modifier.size(18.dp)
        )
      }
    }
  }
}

private fun formatCallDuration(seconds: Int): String {
  val mins = seconds / 60
  val secs = seconds % 60
  return String.format("%02d:%02d", mins, secs)
}
