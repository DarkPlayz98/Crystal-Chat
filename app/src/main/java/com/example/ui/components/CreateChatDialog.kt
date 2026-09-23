package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import com.example.util.CountryCodeHelper
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateChatDialog(
  onDismiss: () -> Unit,
  onCreateChat: (name: String, handleOrPhone: String, colorHex: Long) -> Unit
) {
  var name by remember { mutableStateOf("") }
  var selectedTab by remember { mutableIntStateOf(0) } // 0 = Phone Number, 1 = @Handle
  var nationalPhone by remember { mutableStateOf("") }
  var selectedCountry by remember { mutableStateOf(CountryCodeHelper.defaultCountry) }
  var handle by remember { mutableStateOf("") }
  var selectedColorHex by remember { mutableStateOf(0xFF0D9488) }

  val palette = listOf(
    0xFF0D9488, // Teal
    0xFF0284C7, // Cyan
    0xFF6366F1, // Indigo
    0xFF10B981, // Emerald
    0xFFF59E0B, // Amber
    0xFFEC4899  // Pink
  )

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.size(48.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Default.PersonAdd,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
          )
        }
      }
    },
    title = {
      Text(text = "Start Direct Chat", fontWeight = FontWeight.Bold)
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Contact Name") },
          placeholder = { Text("e.g. Sarah Connor") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("create_chat_name_input"),
          shape = RoundedCornerShape(12.dp)
        )

        PrimaryTabRow(
          selectedTabIndex = selectedTab,
          modifier = Modifier.fillMaxWidth()
        ) {
          Tab(
            selected = selectedTab == 0,
            onClick = { selectedTab = 0 },
            text = { Text("Phone Number") }
          )
          Tab(
            selected = selectedTab == 1,
            onClick = { selectedTab = 1 },
            text = { Text("@Handle") }
          )
        }

        if (selectedTab == 0) {
          PhoneInputField(
            nationalNumber = nationalPhone,
            onNationalNumberChange = { nationalPhone = it },
            selectedCountry = selectedCountry,
            onCountrySelected = { selectedCountry = it },
            label = "Mobile Phone (with country code)",
            placeholder = "98765 43210",
            testTagPrefix = "create_chat_phone"
          )
          Text(
            text = "If receiver doesn't have Crystal Chat, messages will send directly via SMS to their phone.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        } else {
          OutlinedTextField(
            value = handle,
            onValueChange = { handle = it },
            label = { Text("Crystal @handle") },
            placeholder = { Text("@sarah or sarah_12") },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("create_chat_handle_input"),
            shape = RoundedCornerShape(12.dp)
          )
        }

        Text(
          text = "Avatar Color",
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
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(colorVal))
                .clickable { selectedColorHex = colorVal },
              contentAlignment = Alignment.Center
            ) {
              if (isSelected) {
                Box(
                  modifier = Modifier
                    .size(12.dp)
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
      val canSubmit = name.isNotBlank() && (
        (selectedTab == 0 && nationalPhone.isNotBlank()) ||
        (selectedTab == 1 && handle.isNotBlank())
      )

      Button(
        onClick = {
          if (canSubmit) {
            val contactIdentifier = if (selectedTab == 0) {
              "${selectedCountry.dialCode} ${nationalPhone.trim()}"
            } else {
              val h = handle.trim()
              if (h.startsWith("@")) h else "@$h"
            }
            onCreateChat(name.trim(), contactIdentifier, selectedColorHex)
            onDismiss()
          }
        },
        enabled = canSubmit,
        modifier = Modifier.testTag("confirm_create_chat_button")
      ) {
        Text("Start Chat")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
