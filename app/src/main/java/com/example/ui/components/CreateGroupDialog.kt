package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateGroupDialog(
  onDismiss: () -> Unit,
  availableContacts: List<String> = emptyList(),
  onCreateGroup: (title: String, members: List<String>, colorHex: Long) -> Unit
) {
  var groupName by remember { mutableStateOf("") }
  var newMemberInput by remember { mutableStateOf("") }
  var selectedColorHex by remember { mutableStateOf(0xFF2563EB) }

  val selectedMembers = remember { mutableStateListOf<String>() }

  val palette = listOf(
    0xFF2563EB, // Crystal Sapphire
    0xFF0D9488, // Teal
    0xFF4F46E5, // Indigo
    0xFF059669, // Emerald
    0xFFD97706, // Amber
    0xFFDB2777  // Rose
  )

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.size(48.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Default.Group,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
          )
        }
      }
    },
    title = {
      Text(text = "New Group", fontWeight = FontWeight.Bold)
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Text(
          text = "Create an end-to-end encrypted group with secure key exchange.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
          value = groupName,
          onValueChange = { groupName = it },
          label = { Text("Group Name") },
          placeholder = { Text("e.g. Project Alpha") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("group_name_input"),
          shape = RoundedCornerShape(12.dp)
        )

        // Member addition
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedTextField(
            value = newMemberInput,
            onValueChange = { newMemberInput = it },
            label = { Text("Add Member") },
            placeholder = { Text("Enter name or handle") },
            singleLine = true,
            modifier = Modifier
              .weight(1f)
              .testTag("new_member_input"),
            shape = RoundedCornerShape(12.dp)
          )

          Spacer(modifier = Modifier.width(8.dp))

          IconButton(
            onClick = {
              val trimmed = newMemberInput.trim()
              if (trimmed.isNotBlank() && !selectedMembers.contains(trimmed)) {
                selectedMembers.add(trimmed)
                newMemberInput = ""
              }
            },
            enabled = newMemberInput.isNotBlank(),
            modifier = Modifier.testTag("add_member_button")
          ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add member")
          }
        }

        // Available contacts chips if any
        if (availableContacts.isNotEmpty()) {
          Text(
            text = "Quick add contacts:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            availableContacts.forEach { contact ->
              val isSelected = selectedMembers.contains(contact)
              InputChip(
                selected = isSelected,
                onClick = {
                  if (isSelected) selectedMembers.remove(contact) else selectedMembers.add(contact)
                },
                label = { Text(contact) }
              )
            }
          }
        }

        // Selected members chips
        if (selectedMembers.isNotEmpty()) {
          Text(
            text = "Group Members (${selectedMembers.size})",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
          )
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            selectedMembers.forEach { member ->
              InputChip(
                selected = true,
                onClick = { selectedMembers.remove(member) },
                label = { Text(member) },
                trailingIcon = {
                  Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    modifier = Modifier.size(14.dp)
                  )
                },
                colors = InputChipDefaults.inputChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                  selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
              )
            }
          }
        }

        // Color selector
        Text(
          text = "Group Color",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          palette.forEach { colorVal ->
            val isSelected = selectedColorHex == colorVal
            Box(
              modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(colorVal))
                .clickable { selectedColorHex = colorVal },
              contentAlignment = Alignment.Center
            ) {
              if (isSelected) {
                Box(
                  modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (groupName.isNotBlank()) {
            val members = if (selectedMembers.isEmpty()) listOf("You") else selectedMembers.toList()
            onCreateGroup(groupName.trim(), members, selectedColorHex)
            onDismiss()
          }
        },
        enabled = groupName.isNotBlank(),
        modifier = Modifier.testTag("create_group_confirm_button")
      ) {
        Text("Create Group")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
