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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
  onMinimize: () -> Unit,
  onAnswerCall: () -> Unit = {},
  onDtmfTone: (Char) -> Unit = {}
) {
  val isConnected = session.status == CallStateStatus.CONNECTED
  val isRinging = session.status == CallStateStatus.OUTGOING_RINGING
  var showKeypad by remember { mutableStateOf(false) }
  var dialedDigits by remember { mutableStateOf("") }

  val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = if (isRinging) 1.22f else 1.12f,
    animationSpec = infiniteRepeatable(
      animation = tween(if (isRinging) 900 else 1400, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulseScale"
  )

  val durationText = formatCallDuration(session.durationSeconds)

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.radialGradient(
          colors = listOf(
            Color(0xFF0F172A),
            Color(0xFF0B1120),
            Color(0xFF020617)
          ),
          radius = 1600f
        )
      )
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 24.dp, vertical = 36.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      // 1. Top Bar: Minimize button & Security Badge
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

        Surface(
          shape = RoundedCornerShape(20.dp),
          color = Color(0xFF10B981).copy(alpha = 0.15f),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f))
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
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

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = Color.White.copy(alpha = 0.08f)
        ) {
          Text(
            text = "48 kHz",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }

      // 2. Middle Content: Avatar, Name, Ringing / Call Status & Audio Waveform
      Column(
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Pulsing Acoustic Ring Around Avatar
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier.size(170.dp)
        ) {
          if (isRinging || isConnected) {
            Box(
              modifier = Modifier
                .size(170.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(
                  if (isRinging) Color(0xFF10B981).copy(alpha = 0.12f)
                  else Color(0xFF0284C7).copy(alpha = 0.14f)
                )
            )
            Box(
              modifier = Modifier
                .size(140.dp)
                .scale(pulseScale * 0.94f)
                .clip(CircleShape)
                .background(
                  if (isRinging) Color(0xFF10B981).copy(alpha = 0.18f)
                  else Color(0xFF38BDF8).copy(alpha = 0.2f)
                )
            )
          }

          // Main Avatar
          Box(
            modifier = Modifier
              .size(110.dp)
              .clip(CircleShape)
              .background(
                Brush.linearGradient(
                  colors = listOf(
                    Color(session.avatarColorHex),
                    Color(session.avatarColorHex).copy(alpha = 0.75f)
                  )
                )
              )
              .border(3.dp, Color.White.copy(alpha = 0.35f), CircleShape),
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

        Spacer(modifier = Modifier.height(18.dp))

        // Recipient Name
        Text(
          text = session.contactName,
          fontSize = 28.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Phone Number or Handle
        Text(
          text = session.handle ?: session.phoneNumber,
          fontSize = 15.sp,
          color = Color.White.copy(alpha = 0.65f)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Call State Badge & Duration
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = if (isConnected) Color(0xFF0284C7).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f),
          border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isConnected) Color(0xFF38BDF8).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.1f)
          )
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            if (isRinging) {
              Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = null,
                tint = Color(0xFF10B981),
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Ringing (Playing ringtone...)",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF10B981)
              )
            } else if (isConnected) {
              Text(
                text = durationText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF38BDF8)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "• Connected",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.8f)
              )
            } else {
              Text(
                text = session.statusMessage,
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.7f)
              )
            }
          }
        }

        // Quick Simulated Recipient Pickup action while ringing (if user wants to connect call immediately)
        if (isRinging) {
          Spacer(modifier = Modifier.height(12.dp))
          Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF10B981).copy(alpha = 0.2f),
            modifier = Modifier
              .clip(RoundedCornerShape(20.dp))
              .clickable { onAnswerCall() }
              .testTag("simulate_answer_button")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Call,
                contentDescription = null,
                tint = Color(0xFF10B981),
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Tap to Connect Voice Stream",
                color = Color(0xFF10B981),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Real-Time Minimalist Audio Waveform
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
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
                  } else if (isRinging) {
                    Color(0xFF10B981)
                  } else {
                    Color.White.copy(alpha = 0.3f)
                  }
                )
            )
          }
        }
      }

      // 3. Keypad Overlay (if activated)
      AnimatedVisibility(
        visible = showKeypad,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 }
      ) {
        Surface(
          shape = RoundedCornerShape(24.dp),
          color = Color(0xFF1E293B).copy(alpha = 0.95f),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = if (dialedDigits.isEmpty()) "DTMF Keypad" else dialedDigits,
              fontSize = 20.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )

            Spacer(modifier = Modifier.height(12.dp))

            val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#")
            LazyVerticalGrid(
              columns = GridCells.Fixed(3),
              modifier = Modifier.height(210.dp)
            ) {
              items(keys) { key ->
                Box(
                  contentAlignment = Alignment.Center,
                  modifier = Modifier
                    .padding(6.dp)
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .clickable {
                      dialedDigits += key
                      onDtmfTone(key.first())
                    }
                ) {
                  Text(
                    text = key,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }

            Text(
              text = "Tap to hide keypad",
              color = Color.White.copy(alpha = 0.5f),
              fontSize = 11.sp,
              modifier = Modifier
                .clickable { showKeypad = false }
                .padding(8.dp)
            )
          }
        }
      }

      // 4. Modern Control Dock
      Surface(
        shape = RoundedCornerShape(32.dp),
        color = Color(0xFF1E293B).copy(alpha = 0.85f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(vertical = 16.dp, horizontal = 20.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Mute Button
            CallControlButton(
              icon = if (session.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
              label = if (session.isMuted) "Unmute" else "Mute",
              isActive = session.isMuted,
              activeColor = Color.White,
              onClick = onToggleMute,
              testTag = "toggle_call_mute_button"
            )

            // Keypad Button
            CallControlButton(
              icon = Icons.Default.Dialpad,
              label = "Keypad",
              isActive = showKeypad,
              activeColor = Color(0xFF38BDF8),
              onClick = { showKeypad = !showKeypad },
              testTag = "toggle_keypad_button"
            )

            // Speaker Button
            CallControlButton(
              icon = if (session.isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeDown,
              label = if (session.isSpeakerOn) "Speaker On" else "Speaker",
              isActive = session.isSpeakerOn,
              activeColor = Color.White,
              onClick = onToggleSpeaker,
              testTag = "toggle_call_speaker_button"
            )
          }

          Spacer(modifier = Modifier.height(16.dp))

          // End Call Button (Large red pill / circle)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
          ) {
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
          }
        }
      }
    }
  }
}

@Composable
private fun CallControlButton(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  isActive: Boolean,
  activeColor: Color,
  onClick: () -> Unit,
  testTag: String
) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    IconButton(
      onClick = onClick,
      modifier = Modifier
        .size(54.dp)
        .clip(CircleShape)
        .background(
          if (isActive) activeColor else Color.White.copy(alpha = 0.1f)
        )
        .testTag(testTag)
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = if (isActive) Color(0xFF0F172A) else Color.White,
        modifier = Modifier.size(24.dp)
      )
    }
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = label,
      color = Color.White.copy(alpha = 0.75f),
      fontSize = 11.sp,
      fontWeight = FontWeight.Medium
    )
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
