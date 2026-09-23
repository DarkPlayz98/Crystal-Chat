package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.model.ConversationEntity
import com.example.ui.components.CreateChatDialog
import com.example.ui.components.CreateGroupDialog
import com.example.ui.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationListScreen(
  viewModel: ChatViewModel,
  onSelectConversation: (String) -> Unit
) {
  val conversations by viewModel.allConversations.collectAsStateWithLifecycle()
  val directChats by viewModel.directChats.collectAsStateWithLifecycle()
  val groupChats by viewModel.groupChats.collectAsStateWithLifecycle()
  val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()

  var selectedFilterTab by remember { mutableIntStateOf(0) } // 0 = All, 1 = Direct, 2 = Groups
  var showSearch by remember { mutableStateOf(false) }
  var searchQuery by remember { mutableStateOf("") }
  var showCreateGroupDialog by remember { mutableStateOf(false) }
  var showCreateChatDialog by remember { mutableStateOf(false) }
  var showMenu by remember { mutableStateOf(false) }

  val baseList = when (selectedFilterTab) {
    1 -> directChats
    2 -> groupChats
    else -> conversations
  }

  val filteredList = remember(baseList, searchQuery) {
    if (searchQuery.isBlank()) {
      baseList
    } else {
      baseList.filter {
        it.title.contains(searchQuery, ignoreCase = true) ||
          it.lastMessageText.contains(searchQuery, ignoreCase = true)
      }
    }
  }

  val existingContactNames = remember(directChats) {
    directChats.map { it.title }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        ),
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Lock,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Crystal Chat",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            // Minimal online/offline indicator dot
            Box(
              modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isOnline) Color(0xFF10B981) else Color(0xFFF59E0B))
                .clickable { viewModel.toggleOnline() }
                .testTag("network_toggle_button")
            )
          }
        },
        actions = {
          IconButton(
            onClick = { showSearch = !showSearch },
            modifier = Modifier.testTag("search_toggle_button")
          ) {
            Icon(
              imageVector = if (showSearch) Icons.Default.Close else Icons.Default.Search,
              contentDescription = "Search"
            )
          }

          IconButton(
            onClick = { showCreateChatDialog = true },
            modifier = Modifier.testTag("new_chat_top_button")
          ) {
            Icon(
              imageVector = Icons.Default.PersonAdd,
              contentDescription = "New Contact"
            )
          }

          IconButton(
            onClick = { showCreateGroupDialog = true },
            modifier = Modifier.testTag("new_group_top_button")
          ) {
            Icon(
              imageVector = Icons.Default.GroupAdd,
              contentDescription = "New Group"
            )
          }
        }
      )
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = { showCreateChatDialog = true },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier.testTag("create_chat_fab")
      ) {
        Icon(
          imageVector = Icons.Default.Add,
          contentDescription = "New Chat"
        )
      }
    }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ) {
      // Search bar
      AnimatedVisibility(visible = showSearch) {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Search messages and contacts...") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("search_text_field"),
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary
          )
        )
      }

      // Filter chips (shown when conversations exist)
      if (conversations.isNotEmpty()) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          FilterChip(
            selected = selectedFilterTab == 0,
            onClick = { selectedFilterTab = 0 },
            label = { Text("All (${conversations.size})") },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            modifier = Modifier.testTag("filter_all")
          )
          FilterChip(
            selected = selectedFilterTab == 1,
            onClick = { selectedFilterTab = 1 },
            label = { Text("Direct (${directChats.size})") },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            modifier = Modifier.testTag("filter_direct")
          )
          FilterChip(
            selected = selectedFilterTab == 2,
            onClick = { selectedFilterTab = 2 },
            label = { Text("Groups (${groupChats.size})") },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            modifier = Modifier.testTag("filter_groups")
          )
        }
      }

      // Chat List or Minimal Empty State
      if (filteredList.isEmpty()) {
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
            Surface(
              shape = CircleShape,
              color = MaterialTheme.colorScheme.surfaceVariant,
              modifier = Modifier.size(72.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.ChatBubbleOutline,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(32.dp)
                )
              }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
              text = if (searchQuery.isNotBlank()) "No chats found" else "No Conversations Yet",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = if (searchQuery.isNotBlank()) "Try searching for another name or keyword" else "Start a private conversation or create an encrypted group.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = androidx.compose.ui.text.style.TextAlign.Center,
              modifier = Modifier.padding(horizontal = 24.dp)
            )
            if (searchQuery.isBlank()) {
              Spacer(modifier = Modifier.height(20.dp))
              Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                  onClick = { showCreateChatDialog = true },
                  modifier = Modifier.testTag("start_chat_empty_button")
                ) {
                  Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Start a Chat")
                }

                OutlinedButton(
                  onClick = { showCreateGroupDialog = true },
                  modifier = Modifier.testTag("start_group_empty_button")
                ) {
                  Icon(imageVector = Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("New Group")
                }
              }
            }
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(bottom = 80.dp)
        ) {
          items(filteredList, key = { it.id }) { conv ->
            ConversationRowItem(
              conversation = conv,
              onClick = { onSelectConversation(conv.id) }
            )
          }
        }
      }
    }
  }

  if (showCreateGroupDialog) {
    CreateGroupDialog(
      availableContacts = existingContactNames,
      onDismiss = { showCreateGroupDialog = false },
      onCreateGroup = { title, members, colorHex ->
        viewModel.createGroup(title, members, colorHex)
      }
    )
  }

  if (showCreateChatDialog) {
    CreateChatDialog(
      onDismiss = { showCreateChatDialog = false },
      onCreateChat = { name, handleOrPhone, colorHex ->
        val isPhone = handleOrPhone.any { it.isDigit() } && !handleOrPhone.startsWith("@")
        if (isPhone) {
          viewModel.addContact(name, handleOrPhone) { contact ->
            viewModel.startChatWithContact(contact)
          }
        } else {
          viewModel.createDirectChat(name, handleOrPhone, colorHex)
        }
      }
    )
  }
}

@Composable
fun ConversationRowItem(
  conversation: ConversationEntity,
  onClick: () -> Unit
) {
  val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
  val formattedTime = remember(conversation.lastMessageTime) {
    timeFormatter.format(Date(conversation.lastMessageTime))
  }

  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .testTag("chat_item_${conversation.id}"),
    color = MaterialTheme.colorScheme.surface
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Avatar with initial letter and color
      Box(
        modifier = Modifier
          .size(48.dp)
          .clip(CircleShape)
          .background(Color(conversation.avatarColorHex)),
        contentAlignment = Alignment.Center
      ) {
        if (conversation.isGroup) {
          Icon(
            imageVector = Icons.Default.Group,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(22.dp)
          )
        } else {
          Text(
            text = conversation.title.take(1).uppercase(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
          )
        }
      }

      Spacer(modifier = Modifier.width(14.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            Text(
              text = conversation.title,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            if (conversation.isExternalSms) {
              Spacer(modifier = Modifier.width(6.dp))
              Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(4.dp)
              ) {
                Text(
                  text = "SMS",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
              }
            } else if (conversation.isVerified) {
              Spacer(modifier = Modifier.width(4.dp))
              Icon(
                imageVector = Icons.Default.Verified,
                contentDescription = "Safety numbers verified",
                tint = Color(0xFF10B981),
                modifier = Modifier.size(15.dp)
              )
            }
            if (conversation.disappearingSeconds > 0) {
              Spacer(modifier = Modifier.width(4.dp))
              Icon(
                imageVector = Icons.Default.AvTimer,
                contentDescription = "Disappearing timer active",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
              )
            }
          }

          Text(
            text = formattedTime,
            style = MaterialTheme.typography.labelSmall,
            color = if (conversation.unreadCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = conversation.lastMessageText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
          )

          if (conversation.unreadCount > 0) {
            Badge(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
              Text(
                text = "${conversation.unreadCount}",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
              )
            }
          }
        }
      }
    }
  }
}
