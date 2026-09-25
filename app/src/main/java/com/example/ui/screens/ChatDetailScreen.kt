package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.local.model.ConversationEntity
import com.example.data.local.model.MessageEntity
import com.example.ui.components.CryptoPayloadDialog
import com.example.ui.components.DisappearingTimerDialog
import com.example.ui.components.EncryptedMediaViewerDialog
import com.example.ui.components.SafetyNumberDialog
import com.example.ui.viewmodel.ChatViewModel
import com.example.util.SmsHelper
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
  viewModel: ChatViewModel,
  onNavigateBack: () -> Unit
) {
  val conversation by viewModel.activeConversation.collectAsStateWithLifecycle()
  val messages by viewModel.activeMessages.collectAsStateWithLifecycle()
  val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()

  var inputText by remember { mutableStateOf("") }
  var showMenu by remember { mutableStateOf(false) }
  var showAttachMenu by remember { mutableStateOf(false) }
  var selectedInspectMessage by remember { mutableStateOf<MessageEntity?>(null) }
  var showSafetyDialog by remember { mutableStateOf(false) }
  var showDisappearingDialog by remember { mutableStateOf(false) }
  var selectedMediaMessage by remember { mutableStateOf<MessageEntity?>(null) }

  val listState = rememberLazyListState()
  val context = LocalContext.current

  // Real Photo Picker launcher
  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      viewModel.sendRealImage(uri, caption = inputText)
      inputText = ""
    }
  }

  // Scroll to bottom on new message
  LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.size - 1)
    }
  }

  val activeConv = conversation
  if (activeConv == null) {
    Scaffold(
      topBar = {
        TopAppBar(
          navigationIcon = {
            IconButton(onClick = onNavigateBack) {
              Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
          },
          title = { Text("Loading conversation...") }
        )
      }
    ) { padding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(padding),
        contentAlignment = Alignment.Center
      ) {
        CircularProgressIndicator()
      }
    }
    return
  }

  Scaffold(
    topBar = {
      TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        ),
        navigationIcon = {
          IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.testTag("chat_back_button")
          ) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        title = {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .clickable { showSafetyDialog = true }
              .padding(vertical = 4.dp, horizontal = 2.dp)
          ) {
            // Avatar
            Box(
              modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color(activeConv.avatarColorHex)),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = activeConv.title.take(1).uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = activeConv.title,
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.SemiBold
                )
                if (activeConv.isVerified && !activeConv.isExternalSms) {
                  Spacer(modifier = Modifier.width(4.dp))
                  Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = "Verified Safety Number",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(15.dp)
                  )
                }
              }
              Text(
                text = when {
                  activeConv.isExternalSms -> "SMS Contact • Default Messaging App"
                  activeConv.isGroup -> "${activeConv.participantNames.split(",").size} members • E2EE"
                  else -> "End-to-End Encrypted"
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (activeConv.isExternalSms) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        },
        actions = {
          if (activeConv.disappearingSeconds > 0) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
              modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable { showDisappearingDialog = true }
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.AvTimer,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                val timeStr = when (activeConv.disappearingSeconds) {
                  30 -> "30s"
                  300 -> "5m"
                  3600 -> "1h"
                  86400 -> "24h"
                  else -> "${activeConv.disappearingSeconds}s"
                }
                Text(
                  text = timeStr,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary
                )
              }
            }
          }

          IconButton(
            onClick = {
              if (activeConv.isExternalSms) {
                viewModel.dialWithDefaultCallerApp(activeConv.phoneNumber ?: activeConv.title)
              } else {
                viewModel.startVoiceCall(
                  contactName = activeConv.title,
                  phoneNumber = activeConv.phoneNumber ?: activeConv.title,
                  handle = activeConv.participantHandles,
                  avatarColorHex = activeConv.avatarColorHex,
                  recipientHasApp = true
                )
              }
            },
            modifier = Modifier.testTag("chat_voice_call_button")
          ) {
            Icon(
              imageVector = Icons.Default.Call,
              contentDescription = if (activeConv.isExternalSms) "Call with Default Phone App" else "HD+ Voice Call",
              tint = MaterialTheme.colorScheme.primary
            )
          }

          IconButton(onClick = { showMenu = true }) {
            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More options")
          }

          DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
          ) {
            DropdownMenuItem(
              text = { Text("Safety Numbers") },
              leadingIcon = { Icon(Icons.Default.Security, contentDescription = null) },
              onClick = {
                showMenu = false
                showSafetyDialog = true
              }
            )
            DropdownMenuItem(
              text = { Text("Disappearing Messages") },
              leadingIcon = { Icon(Icons.Default.AvTimer, contentDescription = null) },
              onClick = {
                showMenu = false
                showDisappearingDialog = true
              }
            )
            DropdownMenuItem(
              text = { Text("Clear Chat History") },
              onClick = {
                showMenu = false
                viewModel.clearChat(activeConv.id)
                Toast.makeText(context, "Chat history cleared", Toast.LENGTH_SHORT).show()
              }
            )
          }
        }
      )
    }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ) {
      // SMS delivery informational banner if recipient does not have the app
      if (activeConv.isExternalSms) {
        Surface(
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Sms,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "Recipient does not have the app. Tapping Call dials via your default Phone app, and messages send via your default SMS app (${activeConv.phoneNumber ?: "Phone"}).",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface,
              lineHeight = 16.sp,
              modifier = Modifier.weight(1f)
            )
          }
        }
      }

      // Offline mode alert if applicable
      AnimatedVisibility(visible = !isOnline && !activeConv.isExternalSms) {
        Surface(
          color = Color(0xFFF59E0B).copy(alpha = 0.15f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Lock,
              contentDescription = null,
              tint = Color(0xFFD97706),
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Offline Mode: Encrypted messages queued locally in vault",
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFFD97706)
            )
          }
        }
      }

      // Messages LazyColumn
      LazyColumn(
        state = listState,
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
      ) {
        items(messages, key = { it.id }) { message ->
          MessageBubble(
            message = message,
            isGroup = activeConv.isGroup,
            onInspectCrypto = { selectedInspectMessage = message },
            onViewMedia = { selectedMediaMessage = message }
          )
        }
      }

      // Attachment Action Bottom Drawer
      AnimatedVisibility(visible = showAttachMenu) {
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
          shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
          ) {
            AttachmentActionItem(
              icon = Icons.Default.Image,
              label = "Photo / Image",
              onClick = {
                showAttachMenu = false
                photoPickerLauncher.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              }
            )
            if (activeConv.isExternalSms && !activeConv.phoneNumber.isNullOrBlank()) {
              AttachmentActionItem(
                icon = Icons.Default.Sms,
                label = "SMS App",
                onClick = {
                  showAttachMenu = false
                  SmsHelper.openDefaultMessagingApp(context, activeConv.phoneNumber, inputText)
                }
              )
            }
          }
        }
      }

      // Input Row
      Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(
            onClick = {
              // Open real photo picker directly or toggle menu
              photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
            modifier = Modifier.testTag("attach_button")
          ) {
            Icon(
              imageVector = Icons.Default.AttachFile,
              contentDescription = "Attach media",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            placeholder = {
              Text(
                if (activeConv.isExternalSms) "Send SMS..." else "Message...",
                fontSize = 14.sp
              )
            },
            keyboardOptions = KeyboardOptions(
              imeAction = ImeAction.Send
            ),
            keyboardActions = KeyboardActions(
              onSend = {
                if (inputText.isNotBlank()) {
                  viewModel.sendMessage(inputText)
                  inputText = ""
                }
              }
            ),
            modifier = Modifier
              .weight(1f)
              .testTag("message_input"),
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MaterialTheme.colorScheme.primary,
              unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            ),
            maxLines = 4
          )

          Spacer(modifier = Modifier.width(8.dp))

          IconButton(
            onClick = {
              if (inputText.isNotBlank()) {
                if (activeConv.isExternalSms) {
                  viewModel.openDefaultSms(activeConv.phoneNumber ?: activeConv.title, inputText)
                } else {
                  viewModel.sendMessage(inputText)
                }
                inputText = ""
              }
            },
            enabled = inputText.isNotBlank(),
            modifier = Modifier
              .size(44.dp)
              .clip(CircleShape)
              .background(if (inputText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
              .testTag("send_button")
          ) {
            Icon(
              imageVector = if (activeConv.isExternalSms) Icons.Default.Sms else Icons.AutoMirrored.Filled.Send,
              contentDescription = if (activeConv.isExternalSms) "Send with Default SMS App" else "Send message",
              tint = if (inputText.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }
    }
  }

  // Dialogs
  selectedInspectMessage?.let { msg ->
    CryptoPayloadDialog(
      message = msg,
      onDismiss = { selectedInspectMessage = null }
    )
  }

  if (showSafetyDialog) {
    SafetyNumberDialog(
      conversation = activeConv,
      onToggleVerification = {
        viewModel.toggleSafetyVerification(activeConv.id, activeConv.isVerified)
      },
      onDismiss = { showSafetyDialog = false }
    )
  }

  if (showDisappearingDialog) {
    DisappearingTimerDialog(
      currentSeconds = activeConv.disappearingSeconds,
      onSelectSeconds = { seconds ->
        viewModel.updateDisappearingTimer(activeConv.id, seconds)
      },
      onDismiss = { showDisappearingDialog = false }
    )
  }

  selectedMediaMessage?.let { msg ->
    EncryptedMediaViewerDialog(
      message = msg,
      onDismiss = { selectedMediaMessage = null }
    )
  }
}

@Composable
fun MessageBubble(
  message: MessageEntity,
  isGroup: Boolean,
  onInspectCrypto: () -> Unit,
  onViewMedia: () -> Unit
) {
  val isOutgoing = message.isOutgoing
  val bubbleColor = if (isOutgoing) {
    MaterialTheme.colorScheme.primary
  } else {
    MaterialTheme.colorScheme.surfaceVariant
  }
  val textColor = if (isOutgoing) {
    MaterialTheme.colorScheme.onPrimary
  } else {
    MaterialTheme.colorScheme.onSurfaceVariant
  }

  val shape = RoundedCornerShape(
    topStart = 16.dp,
    topEnd = 16.dp,
    bottomStart = if (isOutgoing) 16.dp else 4.dp,
    bottomEnd = if (isOutgoing) 4.dp else 16.dp
  )

  val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
  val formattedTime = remember(message.timestamp) { timeFormatter.format(Date(message.timestamp)) }

  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start
  ) {
    Box(
      modifier = Modifier
        .widthIn(max = 280.dp)
        .clip(shape)
        .background(bubbleColor)
        .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
      Column {
        // Group sender name
        if (!isOutgoing && isGroup && message.senderName != "System") {
          Text(
            text = message.senderName,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 2.dp)
          )
        }

        // Real Media attachment: Render real image when mediaType == "IMAGE"
        if (message.mediaType == "IMAGE") {
          val imageFile = message.mediaUri?.let { File(it) }
          if (imageFile != null && imageFile.exists()) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable { onViewMedia() }
            ) {
              AsyncImage(
                model = imageFile,
                contentDescription = "Sent image",
                modifier = Modifier
                  .fillMaxWidth()
                  .heightIn(max = 220.dp),
                contentScale = ContentScale.Crop
              )
            }
          } else {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (isOutgoing) Color.Black.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { onViewMedia() }
                .padding(bottom = 6.dp)
            ) {
              Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.Image,
                  contentDescription = null,
                  tint = if (isOutgoing) Color.White else MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Photo Attachment",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = textColor
                )
              }
            }
          }
        }

        // Message text
        if (message.plainText.isNotBlank()) {
          Text(
            text = message.plainText,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            lineHeight = 20.sp
          )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Bottom row: Time, SMS tag, Status icon
        Row(
          modifier = Modifier.align(Alignment.End),
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (message.isSms || message.status == "SENT_SMS") {
            Text(
              text = "SMS",
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = textColor.copy(alpha = 0.85f),
              modifier = Modifier
                .background(
                  color = Color.Black.copy(alpha = 0.2f),
                  shape = RoundedCornerShape(3.dp)
                )
                .padding(horizontal = 4.dp, vertical = 1.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
          }

          Text(
            text = formattedTime,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color = textColor.copy(alpha = 0.7f)
          )

          if (isOutgoing) {
            Spacer(modifier = Modifier.width(4.dp))
            when (message.status) {
              "SENT_SMS" -> Icon(
                imageVector = Icons.Default.Sms,
                contentDescription = "Sent via SMS",
                tint = textColor.copy(alpha = 0.9f),
                modifier = Modifier.size(13.dp)
              )
              "SENT" -> Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Sent",
                tint = textColor.copy(alpha = 0.7f),
                modifier = Modifier.size(13.dp)
              )
              "DELIVERED" -> Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = "Delivered",
                tint = textColor.copy(alpha = 0.7f),
                modifier = Modifier.size(13.dp)
              )
              "READ" -> Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = "Read",
                tint = if (isOutgoing) Color(0xFF67E8F9) else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(13.dp)
              )
              else -> Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = "Queued",
                tint = textColor.copy(alpha = 0.6f),
                modifier = Modifier.size(13.dp)
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun AttachmentActionItem(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  onClick: () -> Unit
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .clip(RoundedCornerShape(12.dp))
      .clickable { onClick() }
      .padding(12.dp)
  ) {
    Surface(
      shape = CircleShape,
      color = MaterialTheme.colorScheme.primaryContainer,
      modifier = Modifier.size(48.dp)
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = icon,
          contentDescription = label,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(24.dp)
        )
      }
    }
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.Medium
    )
  }
}
