package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.AddIcCall
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.model.CallEntity
import com.example.ui.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallsScreen(
  viewModel: ChatViewModel
) {
  val callLogs by viewModel.callLogs.collectAsStateWithLifecycle()
  val allContacts by viewModel.allContacts.collectAsStateWithLifecycle()

  var showContactPickerForCall by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text("Calls", fontWeight = FontWeight.Bold)
            Text(
              "Crystal HD+ Voice • End-to-End Encrypted",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.primary
            )
          }
        },
        actions = {
          if (callLogs.isNotEmpty()) {
            IconButton(
              onClick = { viewModel.clearAllCalls() },
              modifier = Modifier.testTag("clear_calls_button")
            ) {
              Icon(Icons.Default.Delete, contentDescription = "Clear Call Logs")
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = { showContactPickerForCall = true },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier.testTag("new_call_fab")
      ) {
        Icon(Icons.Default.AddIcCall, contentDescription = "Start Voice Call")
      }
    }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ) {
      // HD+ Voice Banner
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF10B981).copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.25f))
      ) {
        Row(
          modifier = Modifier.padding(12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = Color(0xFF10B981),
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Calls are secured with AES-256 and Opus 48kHz audio. Crystal clarity without carrier eavesdropping.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 16.sp
          )
        }
      }

      if (callLogs.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
          ) {
            Box(
              modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Call,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
              )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
              text = "No Recent Calls",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Tap the call button on any contact or chat to start an HD+ encrypted voice call.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize()
        ) {
          items(callLogs, key = { it.id }) { call ->
            val contact = allContacts.find { it.phoneNumber == call.phoneNumber }
            val recipientHasApp = contact?.hasApp ?: false
            CallHistoryItem(
              call = call,
              onCallAgain = {
                viewModel.startVoiceCall(
                  contactName = call.contactName,
                  phoneNumber = call.phoneNumber,
                  handle = call.handle,
                  avatarColorHex = call.avatarColorHex,
                  recipientHasApp = recipientHasApp
                )
              }
            )
          }
        }
      }
    }
  }

  // Contact Picker Dialog for Starting Voice Call
  if (showContactPickerForCall) {
    androidx.compose.material3.AlertDialog(
      onDismissRequest = { showContactPickerForCall = false },
      title = { Text("Start HD+ Voice Call", fontWeight = FontWeight.Bold) },
      text = {
        if (allContacts.isEmpty()) {
          Text("No synced contacts found. Import contacts to call.")
        } else {
          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .height(300.dp)
          ) {
            items(allContacts, key = { it.id }) { contact ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(8.dp))
                  .clickable {
                    showContactPickerForCall = false
                    viewModel.startVoiceCall(
                      contactName = contact.name,
                      phoneNumber = contact.phoneNumber,
                      handle = contact.handle,
                      avatarColorHex = contact.avatarColorHex,
                      recipientHasApp = contact.hasApp
                    )
                  }
                  .padding(vertical = 10.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(contact.avatarColorHex)),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = contact.name.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                  )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(contact.name, fontWeight = FontWeight.SemiBold)
                  Text(
                    contact.phoneNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
                Icon(
                  imageVector = Icons.Default.Call,
                  contentDescription = "Call",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(20.dp)
                )
              }
            }
          }
        }
      },
      confirmButton = {
        androidx.compose.material3.TextButton(
          onClick = { showContactPickerForCall = false }
        ) {
          Text("Close")
        }
      }
    )
  }
}

@Composable
fun CallHistoryItem(
  call: CallEntity,
  onCallAgain: () -> Unit
) {
  val dateFormatter = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
  val formattedTime = remember(call.timestamp) { dateFormatter.format(Date(call.timestamp)) }

  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onCallAgain() }
      .testTag("call_item_${call.id}"),
    color = MaterialTheme.colorScheme.surface
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Avatar
      Box(
        modifier = Modifier
          .size(46.dp)
          .clip(CircleShape)
          .background(Color(call.avatarColorHex)),
        contentAlignment = Alignment.Center
      ) {
        val initial = call.contactName.take(1).uppercase().ifBlank { "C" }
        Text(
          text = initial,
          color = Color.White,
          fontWeight = FontWeight.Bold,
          fontSize = 18.sp
        )
      }

      Spacer(modifier = Modifier.width(14.dp))

      // Name & Status
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = call.contactName,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(3.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
          val (icon, tint) = when (call.callType) {
            "MISSED" -> Icons.AutoMirrored.Filled.CallMissed to MaterialTheme.colorScheme.error
            "INCOMING" -> Icons.AutoMirrored.Filled.CallReceived to Color(0xFF10B981)
            else -> Icons.AutoMirrored.Filled.CallMade to MaterialTheme.colorScheme.primary
          }

          Icon(
            imageVector = icon,
            contentDescription = call.callType,
            tint = tint,
            modifier = Modifier.size(14.dp)
          )

          Spacer(modifier = Modifier.width(4.dp))

          Text(
            text = "$formattedTime • ${if (call.durationSeconds > 0) "${call.durationSeconds}s" else "No answer"}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      // HD+ Badge & Redial Icon
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF10B981).copy(alpha = 0.12f),
        modifier = Modifier.padding(end = 8.dp)
      ) {
        Text(
          text = "HD+",
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          color = Color(0xFF10B981),
          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
      }

      IconButton(
        onClick = onCallAgain,
        modifier = Modifier.testTag("redial_button_${call.id}")
      ) {
        Icon(
          imageVector = Icons.Default.Call,
          contentDescription = "Call Again",
          tint = MaterialTheme.colorScheme.primary
        )
      }
    }
  }
}
