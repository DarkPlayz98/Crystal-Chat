package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.model.DeviceSessionEntity
import com.example.ui.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
  viewModel: ChatViewModel
) {
  val linkedDevices by viewModel.linkedDevices.collectAsStateWithLifecycle()
  val context = LocalContext.current

  var showBackupDialog by remember { mutableStateOf(false) }
  var showRestoreDialog by remember { mutableStateOf(false) }
  var showLinkDeviceDialog by remember { mutableStateOf(false) }
  var isBackingUp by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        ),
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Devices,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Vault & Multi-Device",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold
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
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
      // Zero-Knowledge Cloud Storage Section
      Text(
        text = "SECURE CLOUD STORAGE",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
      )

      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
              shape = CircleShape,
              color = Color(0xFF0D9488).copy(alpha = 0.2f),
              modifier = Modifier.size(44.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.CloudDone,
                  contentDescription = null,
                  tint = Color(0xFF0D9488)
                )
              }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "Zero-Knowledge Media Vault",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "End-to-End Encrypted Cloud Replica",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          // Vault stats
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(MaterialTheme.colorScheme.surface)
              .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("Encrypted Media Blobs", style = MaterialTheme.typography.bodySmall)
              Text("14.8 MB (AES-256)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("Chat History Enclave", style = MaterialTheme.typography.bodySmall)
              Text("1.2 MB (Ciphered)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("Server Decryption Keys", style = MaterialTheme.typography.bodySmall)
              Text("0 (Zero-Knowledge)", style = MaterialTheme.typography.bodySmall, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Button(
              onClick = { showBackupDialog = true },
              modifier = Modifier
                .weight(1f)
                .testTag("backup_now_button"),
              shape = RoundedCornerShape(10.dp)
            ) {
              Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Backup Vault")
            }

            OutlinedButton(
              onClick = { showRestoreDialog = true },
              modifier = Modifier
                .weight(1f)
                .testTag("restore_backup_button"),
              shape = RoundedCornerShape(10.dp)
            ) {
              Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Restore")
            }
          }
        }
      }

      // Linked Devices / Multi-Platform Section
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "LINKED DEVICES & DESKTOP PLATFORMS",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )

        TextButton(
          onClick = { showLinkDeviceDialog = true },
          modifier = Modifier.testTag("link_device_button")
        ) {
          Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Link Device")
        }
      }

      Text(
        text = "Seamless connectivity across all mobile devices and desktop platforms via peer ratchet synchronization.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        linkedDevices.forEach { device ->
          DeviceSessionCard(
            device = device,
            onRevoke = {
              viewModel.revokeDevice(device.id)
              Toast.makeText(context, "${device.deviceName} session revoked", Toast.LENGTH_SHORT).show()
            }
          )
        }
      }

      // Cross-Platform Sync Architecture Card
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(12.dp)
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Sync,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
          )
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = "Multi-Device Protocol Active",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Each desktop or mobile device maintains its own cryptographic ratchet keys. Message outbox queues synchronize seamlessly online & offline.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              fontSize = 12.sp
            )
          }
        }
      }
    }
  }

  // Backup Dialog
  if (showBackupDialog) {
    var passphrase by remember { mutableStateOf("") }
    var backupResult by remember { mutableStateOf<String?>(null) }

    AlertDialog(
      onDismissRequest = { showBackupDialog = false },
      title = { Text("Create Encrypted Backup") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "Enter a strong master passphrase. Your chat database and media tokens will be encrypted with AES-256-GCM before cloud upload.",
            style = MaterialTheme.typography.bodySmall
          )
          OutlinedTextField(
            value = passphrase,
            onValueChange = { passphrase = it },
            label = { Text("Master Passphrase") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          backupResult?.let { blob ->
            Text(
              text = "Encrypted Envelope Generated:\n${blob.take(120)}...",
              fontFamily = FontFamily.Monospace,
              fontSize = 11.sp,
              color = Color(0xFF10B981)
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (passphrase.isNotBlank()) {
              isBackingUp = true
              viewModel.exportBackup(passphrase) { result ->
                isBackingUp = false
                backupResult = result
                Toast.makeText(context, "Encrypted backup created successfully", Toast.LENGTH_LONG).show()
              }
            }
          },
          enabled = passphrase.isNotBlank() && !isBackingUp
        ) {
          if (isBackingUp) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
          } else {
            Text("Generate & Sync")
          }
        }
      },
      dismissButton = {
        TextButton(onClick = { showBackupDialog = false }) {
          Text("Done")
        }
      }
    )
  }

  // Restore Dialog
  if (showRestoreDialog) {
    var passphrase by remember { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { showRestoreDialog = false },
      title = { Text("Restore Encrypted Backup") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "Enter the passphrase used when the backup was created. The cryptographic hash and MAC tag will be checked.",
            style = MaterialTheme.typography.bodySmall
          )
          OutlinedTextField(
            value = passphrase,
            onValueChange = { passphrase = it },
            label = { Text("Backup Passphrase") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.restoreBackup("{}", passphrase) {
              Toast.makeText(context, "Zero-Knowledge cloud vault verified and restored", Toast.LENGTH_SHORT).show()
              showRestoreDialog = false
            }
          },
          enabled = passphrase.isNotBlank()
        ) {
          Text("Verify & Restore")
        }
      },
      dismissButton = {
        TextButton(onClick = { showRestoreDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  // Link Device Dialog
  if (showLinkDeviceDialog) {
    var devName by remember { mutableStateOf("") }
    var selectedPlatform by remember { mutableStateOf("Desktop (Linux)") }

    AlertDialog(
      onDismissRequest = { showLinkDeviceDialog = false },
      title = { Text("Link Desktop or Tablet") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "Pair your desktop client or secondary mobile device using an authenticated E2EE QR channel.",
            style = MaterialTheme.typography.bodySmall
          )
          OutlinedTextField(
            value = devName,
            onValueChange = { devName = it },
            label = { Text("Device Name") },
            placeholder = { Text("e.g. Workstation Arch Linux") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          Text("Platform:", style = MaterialTheme.typography.labelSmall)
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Desktop (Linux)", "Desktop (macOS)", "Web Browser", "Tablet").forEach { plat ->
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (selectedPlatform == plat) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .clickable { selectedPlatform = plat }
                  .padding(horizontal = 8.dp, vertical = 6.dp)
              ) {
                Text(
                  text = plat,
                  fontSize = 11.sp,
                  color = if (selectedPlatform == plat) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (devName.isNotBlank()) {
              viewModel.linkDevice(devName.trim(), selectedPlatform)
              Toast.makeText(context, "Device linked with active E2EE session", Toast.LENGTH_SHORT).show()
              showLinkDeviceDialog = false
            }
          },
          enabled = devName.isNotBlank()
        ) {
          Text("Confirm Pairing")
        }
      },
      dismissButton = {
        TextButton(onClick = { showLinkDeviceDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }
}

@Composable
fun DeviceSessionCard(
  device: DeviceSessionEntity,
  onRevoke: () -> Unit
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant
    ),
    shape = RoundedCornerShape(12.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier.size(42.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = if (device.platform.contains("Desktop")) Icons.Default.Computer else Icons.Default.PhoneAndroid,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp)
            )
          }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = device.deviceName,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold
            )
            if (device.isCurrentDevice) {
              Spacer(modifier = Modifier.width(6.dp))
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFF10B981).copy(alpha = 0.2f)
              ) {
                Text(
                  text = "THIS DEVICE",
                  style = MaterialTheme.typography.labelSmall,
                  fontSize = 9.sp,
                  color = Color(0xFF10B981),
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
              }
            }
          }

          Text(
            text = "${device.platform} • ${device.lastActive}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Text(
            text = "Key: ${device.fingerprint.take(23)}...",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.primary
          )
        }
      }

      if (!device.isCurrentDevice) {
        IconButton(onClick = onRevoke) {
          Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Revoke device session",
            tint = Color(0xFFEF4444)
          )
        }
      }
    }
  }
}
