package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ImportContacts
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.model.ContactEntity
import com.example.ui.viewmodel.ChatViewModel
import com.example.ui.components.PhoneInputField
import com.example.util.CountryCodeHelper
import com.example.util.SmsHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
  viewModel: ChatViewModel
) {
  val context = LocalContext.current
  val contacts by viewModel.allContacts.collectAsStateWithLifecycle()

  var searchQuery by remember { mutableStateOf("") }
  var showAddDialog by remember { mutableStateOf(false) }
  var initialPhoneForDialog by remember { mutableStateOf("") }
  var initialNameForDialog by remember { mutableStateOf("") }

  val isSyncingContacts by viewModel.isSyncingContacts.collectAsStateWithLifecycle()

  val syncPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    if (isGranted) {
      viewModel.syncContacts { res ->
        Toast.makeText(
          context,
          "Synced ${res.totalFound} device contacts (${res.registeredAppUsers} on Crystal Chat)",
          Toast.LENGTH_LONG
        ).show()
      }
    } else {
      Toast.makeText(context, "Contacts permission required to sync", Toast.LENGTH_SHORT).show()
    }
  }

  // Contact Picker Launcher (zero-permission standard Android contact picker)
  val pickContactLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickContact()
  ) { uri: Uri? ->
    if (uri != null) {
      try {
        var contactName = ""
        var contactNumber = ""
        val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use { c ->
          if (c.moveToFirst()) {
            val idIndex = c.getColumnIndex(ContactsContract.Contacts._ID)
            val nameIndex = c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            val hasPhoneIndex = c.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)

            contactName = if (nameIndex >= 0) c.getString(nameIndex) ?: "" else ""
            val id = if (idIndex >= 0) c.getString(idIndex) else ""
            val hasPhone = if (hasPhoneIndex >= 0) c.getInt(hasPhoneIndex) else 0

            if (hasPhone > 0 && id.isNotBlank()) {
              val pCursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                arrayOf(id),
                null
              )
              pCursor?.use { pc ->
                if (pc.moveToFirst()) {
                  val numIndex = pc.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                  if (numIndex >= 0) {
                    contactNumber = pc.getString(numIndex) ?: ""
                  }
                }
              }
            }
          }
        }

        if (contactNumber.isNotBlank()) {
          initialNameForDialog = contactName
          initialPhoneForDialog = contactNumber
          showAddDialog = true
        } else if (contactName.isNotBlank()) {
          initialNameForDialog = contactName
          initialPhoneForDialog = ""
          showAddDialog = true
        }
      } catch (e: Exception) {
        Toast.makeText(context, "Could not read contact info", Toast.LENGTH_SHORT).show()
      }
    }
  }

  val filteredContacts = remember(contacts, searchQuery) {
    if (searchQuery.isBlank()) {
      contacts
    } else {
      val q = searchQuery.lowercase().trim()
      contacts.filter {
        it.name.lowercase().contains(q) ||
          it.phoneNumber.contains(q) ||
          (it.handle?.lowercase()?.contains(q) == true)
      }
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "Contacts",
            fontWeight = FontWeight.Bold
          )
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        ),
        actions = {
          IconButton(
            onClick = {
              val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
              ) == PackageManager.PERMISSION_GRANTED
              if (hasPermission) {
                viewModel.syncContacts { res ->
                  Toast.makeText(
                    context,
                    "Synced ${res.totalFound} contacts (${res.registeredAppUsers} on Crystal Chat)",
                    Toast.LENGTH_LONG
                  ).show()
                }
              } else {
                syncPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
              }
            },
            enabled = !isSyncingContacts,
            modifier = Modifier.testTag("sync_contacts_topbar_button")
          ) {
            Icon(
              imageVector = Icons.Default.Sync,
              contentDescription = "Sync all contacts from phone",
              tint = MaterialTheme.colorScheme.primary
            )
          }

          IconButton(
            onClick = { pickContactLauncher.launch(null) },
            modifier = Modifier.testTag("import_contacts_button")
          ) {
            Icon(
              imageVector = Icons.Default.ImportContacts,
              contentDescription = "Import from address book",
              tint = MaterialTheme.colorScheme.primary
            )
          }
        }
      )
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = {
          initialNameForDialog = ""
          initialPhoneForDialog = ""
          showAddDialog = true
        },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier.testTag("add_contact_fab")
      ) {
        Icon(Icons.Default.Add, contentDescription = "Add Contact")
      }
    }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ) {
      // Search Bar
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by name, phone or handle...", fontSize = 14.sp) },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("contacts_search_input"),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
              focusedBorderColor = Color.Transparent,
              unfocusedBorderColor = Color.Transparent
            )
          )
        }
      }

      // Quick Info Banner
      Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp)
      ) {
        Row(
          modifier = Modifier.padding(12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Sms,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "Universal Phone Messaging",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )
            Text(
              text = "If receiver does not have Crystal Chat, messages automatically route to their default SMS app.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              lineHeight = 16.sp
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      if (filteredContacts.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              imageVector = Icons.Default.ContactPhone,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
              modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
              text = if (searchQuery.isNotBlank()) "No contacts found" else "No phone contacts yet",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Add contacts with phone numbers. You can chat securely with app users or send directly via SMS to anyone.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedButton(
                onClick = { pickContactLauncher.launch(null) }
              ) {
                Icon(Icons.Default.ImportContacts, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Import")
              }
              OutlinedButton(
                onClick = {
                  initialNameForDialog = ""
                  initialPhoneForDialog = ""
                  showAddDialog = true
                }
              ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Manually")
              }
            }
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize()
        ) {
          items(filteredContacts, key = { it.id }) { contact ->
            ContactRowItem(
              contact = contact,
              onMessageClick = {
                viewModel.startChatWithContact(contact)
              },
              onCallClick = {
                viewModel.startVoiceCall(
                  contactName = contact.name,
                  phoneNumber = contact.phoneNumber,
                  handle = contact.handle,
                  avatarColorHex = contact.avatarColorHex
                )
              },
              onDirectSmsClick = {
                SmsHelper.openDefaultMessagingApp(context, contact.phoneNumber, "")
              },
              onDelete = {
                viewModel.deleteContact(contact.id)
                Toast.makeText(context, "Contact deleted", Toast.LENGTH_SHORT).show()
              }
            )
          }
        }
      }
    }
  }

  // Add Contact Dialog
  if (showAddDialog) {
    AddContactDialog(
      initialName = initialNameForDialog,
      initialPhone = initialPhoneForDialog,
      onDismiss = { showAddDialog = false },
      onConfirm = { name, phone, handle ->
        viewModel.addContact(
          name = name,
          phoneNumber = phone,
          handle = handle
        ) {
          Toast.makeText(context, "Contact saved", Toast.LENGTH_SHORT).show()
        }
        showAddDialog = false
      }
    )
  }
}

@Composable
fun ContactRowItem(
  contact: ContactEntity,
  onMessageClick: () -> Unit,
  onCallClick: () -> Unit,
  onDirectSmsClick: () -> Unit,
  onDelete: () -> Unit
) {
  var showMenu by remember { mutableStateOf(false) }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 4.dp)
      .clickable { onMessageClick() }
      .testTag("contact_item_${contact.id}"),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    shape = RoundedCornerShape(14.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Avatar
      Box(
        modifier = Modifier
          .size(46.dp)
          .clip(CircleShape)
          .background(Color(contact.avatarColorHex)),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = contact.name.take(1).uppercase(),
          color = Color.White,
          fontWeight = FontWeight.Bold,
          fontSize = 18.sp
        )
      }

      Spacer(modifier = Modifier.width(14.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = contact.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
          )
          if (contact.hasApp) {
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
              imageVector = Icons.Default.VerifiedUser,
              contentDescription = "Verified Crystal User",
              tint = Color(0xFF10B981),
              modifier = Modifier.size(15.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
          text = contact.phoneNumber,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Badge indicator
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = if (contact.hasApp) Color(0xFF10B981).copy(alpha = 0.12f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Chat,
              contentDescription = null,
              tint = if (contact.hasApp) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = if (contact.hasApp) "Crystal Chat User" else "End-to-End Encrypted",
              fontSize = 11.sp,
              fontWeight = FontWeight.Medium,
              color = if (contact.hasApp) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
            )
          }
        }
      }

      // Quick Voice Call Button
      IconButton(
        onClick = onCallClick,
        modifier = Modifier.testTag("contact_call_button_${contact.id}")
      ) {
        Icon(
          imageVector = Icons.Default.Call,
          contentDescription = "HD+ Voice Call",
          tint = MaterialTheme.colorScheme.primary
        )
      }

      // Quick Chat Button
      IconButton(
        onClick = onMessageClick,
        modifier = Modifier.testTag("contact_message_button")
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.Chat,
          contentDescription = "Chat",
          tint = MaterialTheme.colorScheme.primary
        )
      }

      Box {
        IconButton(onClick = { showMenu = true }) {
          Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = "More",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        DropdownMenu(
          expanded = showMenu,
          onDismissRequest = { showMenu = false }
        ) {
          DropdownMenuItem(
            text = { Text("Delete Contact", color = MaterialTheme.colorScheme.error) },
            leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            onClick = {
              showMenu = false
              onDelete()
            }
          )
        }
      }
    }
  }
}

@Composable
fun AddContactDialog(
  initialName: String = "",
  initialPhone: String = "",
  onDismiss: () -> Unit,
  onConfirm: (name: String, phone: String, handle: String?) -> Unit
) {
  var name by remember { mutableStateOf(initialName) }
  val initialParsed = remember(initialPhone) {
    if (initialPhone.isNotBlank()) CountryCodeHelper.extractDialCode(initialPhone)
    else Pair(CountryCodeHelper.defaultCountry, "")
  }
  var selectedCountry by remember { mutableStateOf(initialParsed.first) }
  var nationalPhone by remember { mutableStateOf(initialParsed.second) }
  var handle by remember { mutableStateOf("") }
  var errorText by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Add Phone Contact", fontWeight = FontWeight.Bold) },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it; errorText = null },
          label = { Text("Full Name") },
          placeholder = { Text("e.g. Sarah Connor") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("add_contact_name_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        PhoneInputField(
          nationalNumber = nationalPhone,
          onNationalNumberChange = { nationalPhone = it; errorText = null },
          selectedCountry = selectedCountry,
          onCountrySelected = { selectedCountry = it },
          label = "Phone Number (with Country Code)",
          placeholder = "98765 43210",
          testTagPrefix = "add_contact_phone"
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = handle,
          onValueChange = { handle = it },
          label = { Text("Optional @handle") },
          placeholder = { Text("e.g. sarah_c") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("add_contact_handle_input")
        )

        errorText?.let {
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = it,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
          )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "Note: If this contact does not have Crystal Chat installed, your messages will seamlessly open directly in their default SMS app.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          if (name.isBlank()) {
            errorText = "Please enter a name"
            return@TextButton
          }
          if (nationalPhone.isBlank()) {
            errorText = "Please enter a phone number"
            return@TextButton
          }
          val fullPhone = "${selectedCountry.dialCode} ${nationalPhone.trim()}"
          onConfirm(name.trim(), fullPhone.trim(), handle.trim().takeIf { it.isNotBlank() })
        },
        modifier = Modifier.testTag("save_contact_confirm_button")
      ) {
        Text("Save Contact", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
