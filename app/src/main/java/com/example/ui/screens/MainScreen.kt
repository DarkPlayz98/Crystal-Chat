package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.ChatViewModel

@Composable
fun MainScreen(
  viewModel: ChatViewModel
) {
  val selectedChatId by viewModel.selectedConversationId.collectAsStateWithLifecycle()
  val isAppLocked by viewModel.isAppLocked.collectAsStateWithLifecycle()

  var currentTab by rememberSaveable { mutableIntStateOf(0) }

  // App Lock Screen takes precedence if biometric/pin is active
  if (isAppLocked) {
    AppLockScreen(onUnlock = { viewModel.unlockApp() })
    return
  }

  // If a chat is selected, show the ChatDetailScreen
  if (selectedChatId != null) {
    BackHandler {
      viewModel.selectConversation(null)
    }
    ChatDetailScreen(
      viewModel = viewModel,
      onNavigateBack = { viewModel.selectConversation(null) }
    )
    return
  }

  // Clean, Minimal 4-Tab Layout: Chats, Contacts, Vault, Profile & Privacy
  Scaffold(
    contentWindowInsets = WindowInsets(0.dp),
    bottomBar = {
      NavigationBar(
        windowInsets = WindowInsets.navigationBars,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
      ) {
        NavigationBarItem(
          selected = currentTab == 0,
          onClick = { currentTab = 0 },
          icon = {
            Icon(
              imageVector = if (currentTab == 0) Icons.Filled.ChatBubble else Icons.Outlined.ChatBubbleOutline,
              contentDescription = "Chats",
              modifier = Modifier.size(22.dp)
            )
          },
          label = { Text("Chats") },
          colors = NavigationBarItemDefaults.colors(
            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary
          ),
          modifier = Modifier.testTag("nav_tab_chats")
        )

        NavigationBarItem(
          selected = currentTab == 1,
          onClick = { currentTab = 1 },
          icon = {
            Icon(
              imageVector = if (currentTab == 1) Icons.Filled.Contacts else Icons.Outlined.Contacts,
              contentDescription = "Contacts",
              modifier = Modifier.size(22.dp)
            )
          },
          label = { Text("Contacts") },
          colors = NavigationBarItemDefaults.colors(
            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary
          ),
          modifier = Modifier.testTag("nav_tab_contacts")
        )

        NavigationBarItem(
          selected = currentTab == 2,
          onClick = { currentTab = 2 },
          icon = {
            Icon(
              imageVector = if (currentTab == 2) Icons.Filled.Devices else Icons.Outlined.Devices,
              contentDescription = "Vault",
              modifier = Modifier.size(22.dp)
            )
          },
          label = { Text("Vault") },
          colors = NavigationBarItemDefaults.colors(
            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary
          ),
          modifier = Modifier.testTag("nav_tab_vault")
        )

        NavigationBarItem(
          selected = currentTab == 3,
          onClick = { currentTab = 3 },
          icon = {
            Icon(
              imageVector = if (currentTab == 3) Icons.Filled.Person else Icons.Outlined.Person,
              contentDescription = "Profile",
              modifier = Modifier.size(22.dp)
            )
          },
          label = { Text("Profile") },
          colors = NavigationBarItemDefaults.colors(
            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary
          ),
          modifier = Modifier.testTag("nav_tab_profile")
        )
      }
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      AnimatedContent(
        targetState = currentTab,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "TabContent"
      ) { targetTab ->
        when (targetTab) {
          0 -> ConversationListScreen(
            viewModel = viewModel,
            onSelectConversation = { id -> viewModel.selectConversation(id) }
          )
          1 -> ContactsScreen(viewModel = viewModel)
          2 -> VaultScreen(viewModel = viewModel)
          3 -> ProfileAuthScreen(viewModel = viewModel)
        }
      }
    }
  }
}
