package com.example.ui.screens

import android.app.Activity
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import com.example.ui.components.PhoneInputField
import com.example.util.CountryCodeHelper
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
import com.example.ui.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileAuthScreen(
  viewModel: ChatViewModel
) {
  val context = LocalContext.current
  val activity = context as? Activity

  val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
  val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
  val authLoading by viewModel.authLoading.collectAsStateWithLifecycle()
  val authError by viewModel.authError.collectAsStateWithLifecycle()

  val screenSecurity by viewModel.screenSecurityEnabled.collectAsStateWithLifecycle()
  val biometricLock by viewModel.biometricLockEnabled.collectAsStateWithLifecycle()

  var showEditHandleDialog by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Profile & Privacy", fontWeight = FontWeight.Bold) },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Identity Header Card
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          // Avatar
          val photoUrl = userProfile?.photoUrl?.takeIf { it.isNotBlank() }
          if (photoUrl != null) {
            AsyncImage(
              model = photoUrl,
              contentDescription = "Profile Picture",
              modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
              contentScale = ContentScale.Crop
            )
          } else {
            Box(
              modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
              contentAlignment = Alignment.Center
            ) {
              val initial = (userProfile?.displayName ?: currentUser?.displayName ?: "C").take(1).uppercase()
              Text(
                text = initial,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          val displayName = userProfile?.displayName?.ifBlank { null }
            ?: currentUser?.displayName
            ?: "Crystal User"
          Text(
            text = displayName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
          )

          // Handle badge
          val handle = userProfile?.handle?.takeIf { it.isNotBlank() } ?: "anonymous"
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            modifier = Modifier
              .padding(top = 6.dp)
              .clip(RoundedCornerShape(12.dp))
              .clickable { showEditHandleDialog = true }
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.AlternateEmail,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
              )
              Text(
                text = handle,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
              )
              Spacer(modifier = Modifier.width(6.dp))
              Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit handle",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(13.dp)
              )
            }
          }

          if (userProfile?.phoneNumber?.isNotBlank() == true) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(top = 4.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(13.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = userProfile?.phoneNumber ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }

      // Firebase Authentication Section
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Key,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Firebase Authentication & Handle",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          if (currentUser == null) {
            Text(
              text = "Sign in with your Google account to claim your unique @handle, link your phone number, and sync cryptographic identity across devices.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              lineHeight = 20.sp
            )

            authError?.let { err ->
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = err,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
              )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
              onClick = {
                if (activity != null) {
                  viewModel.signInWithGoogle(activity) { success ->
                    if (success) {
                      Toast.makeText(context, "Signed in successfully!", Toast.LENGTH_SHORT).show()
                    }
                  }
                }
              },
              enabled = !authLoading,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("google_signin_button"),
              shape = RoundedCornerShape(12.dp)
            ) {
              if (authLoading) {
                CircularProgressIndicator(
                  modifier = Modifier.size(18.dp),
                  color = MaterialTheme.colorScheme.onPrimary,
                  strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Authenticating...")
              } else {
                Text("Sign in with Google", fontWeight = FontWeight.SemiBold)
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
              onClick = {
                viewModel.loadPreviewTestAccount()
                Toast.makeText(context, "Loaded test account (@crystal_tester)", Toast.LENGTH_SHORT).show()
              },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("preview_test_account_button"),
              shape = RoundedCornerShape(12.dp)
            ) {
              Text("Use Preview Test Account (@crystal_tester)")
            }
          } else {
            // Signed In Status
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(vertical = 4.dp)
            ) {
              Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Verified",
                tint = Color(0xFF10B981),
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(
                  text = "Connected via Google Identity",
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.SemiBold
                )
                Text(
                  text = currentUser?.email ?: "",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              OutlinedButton(
                onClick = { showEditHandleDialog = true },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
              ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Edit Handle")
              }

              OutlinedButton(
                onClick = {
                  viewModel.signOut()
                  Toast.makeText(context, "Signed out", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                  contentColor = MaterialTheme.colorScheme.error
                )
              ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Sign Out")
              }
            }
          }
        }
      }

      // Security Settings Card
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Shield,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Device Protection",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Screen Security
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Screen Security (Anti-Screenshot)",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
              )
              Text(
                text = "Prevents screenshots and app switcher previews (FLAG_SECURE)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Switch(
              checked = screenSecurity,
              onCheckedChange = { viewModel.setScreenSecurity(it) }
            )
          }

          HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

          // Biometric App Lock
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Biometric Lock",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
              )
              Text(
                text = "Require fingerprint or passcode upon opening",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Switch(
              checked = biometricLock,
              onCheckedChange = { viewModel.setBiometricLock(it) }
            )
          }
        }
      }
    }
  }

  // Edit Handle Dialog
  if (showEditHandleDialog) {
    EditHandleDialog(
      currentHandle = userProfile?.handle ?: "",
      currentDisplayName = userProfile?.displayName ?: currentUser?.displayName ?: "",
      currentPhone = userProfile?.phoneNumber ?: "",
      onDismiss = { showEditHandleDialog = false },
      onConfirm = { newHandle, newName, newPhone ->
        viewModel.updateHandleAndProfile(newHandle, newName, newPhone) { result ->
          if (result.isSuccess) {
            Toast.makeText(context, "Handle updated to @$newHandle", Toast.LENGTH_SHORT).show()
            showEditHandleDialog = false
          } else {
            val error = result.exceptionOrNull()?.message ?: "Update failed"
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
          }
        }
      }
    )
  }
}

@Composable
fun EditHandleDialog(
  currentHandle: String,
  currentDisplayName: String,
  currentPhone: String,
  onDismiss: () -> Unit,
  onConfirm: (handle: String, displayName: String, phoneNumber: String) -> Unit
) {
  var handle by remember { mutableStateOf(currentHandle.removePrefix("@")) }
  var displayName by remember { mutableStateOf(currentDisplayName) }
  val initialParsed = remember(currentPhone) {
    if (currentPhone.isNotBlank()) CountryCodeHelper.extractDialCode(currentPhone)
    else Pair(CountryCodeHelper.defaultCountry, "")
  }
  var selectedCountry by remember { mutableStateOf(initialParsed.first) }
  var nationalPhone by remember { mutableStateOf(initialParsed.second) }
  var errorText by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Edit Handle & Profile", fontWeight = FontWeight.Bold) },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
          value = handle,
          onValueChange = {
            handle = it.lowercase().filter { c -> c.isLetterOrDigit() || c == '_' }
            errorText = null
          },
          label = { Text("Unique @handle") },
          prefix = { Text("@") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("edit_handle_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = displayName,
          onValueChange = { displayName = it },
          label = { Text("Display Name") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        PhoneInputField(
          nationalNumber = nationalPhone,
          onNationalNumberChange = { nationalPhone = it },
          selectedCountry = selectedCountry,
          onCountrySelected = { selectedCountry = it },
          label = "Phone Number (with Country Code)",
          placeholder = "98765 43210",
          testTagPrefix = "edit_profile_phone"
        )

        errorText?.let {
          Spacer(modifier = Modifier.height(6.dp))
          Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "Your handle allows others to look you up on Crystal Chat without exposing personal identifiers.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          if (handle.length < 3) {
            errorText = "Handle must be at least 3 characters"
            return@TextButton
          }
          val fullPhone = if (nationalPhone.isNotBlank()) "${selectedCountry.dialCode} ${nationalPhone.trim()}" else ""
          onConfirm(handle, displayName, fullPhone)
        },
        modifier = Modifier.testTag("save_handle_button")
      ) {
        Text("Save", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
