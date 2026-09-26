package com.example.ui.screens

import android.Manifest
import android.accounts.AccountManager
import android.app.Activity
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.ui.components.PhoneInputField
import com.example.ui.viewmodel.ChatViewModel
import com.example.util.CountryCode
import com.example.util.CountryCodeHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileAuthScreen(
  viewModel: ChatViewModel
) {
  val context = LocalContext.current
  val activity = context as? Activity

  val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
  val authLoading by viewModel.authLoading.collectAsStateWithLifecycle()
  val authError by viewModel.authError.collectAsStateWithLifecycle()
  val isApiKeyRestricted by viewModel.isApiKeyRestricted.collectAsStateWithLifecycle()
  val generatedSecurityCode by viewModel.generatedSecurityCode.collectAsStateWithLifecycle()
  val isSyncingContacts by viewModel.isSyncingContacts.collectAsStateWithLifecycle()
  val lastSyncResult by viewModel.lastSyncResult.collectAsStateWithLifecycle()

  val screenSecurity by viewModel.screenSecurityEnabled.collectAsStateWithLifecycle()
  val biometricLock by viewModel.biometricLockEnabled.collectAsStateWithLifecycle()

  var showEditHandleDialog by remember { mutableStateOf(false) }
  var showApiKeyDialog by remember { mutableStateOf(false) }
  var customApiKeyInput by remember { mutableStateOf("") }
  var customProjectIdInput by remember { mutableStateOf("") }

  // Android Native Google Account Chooser Launcher
  val googleAccountPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
  ) { result ->
    if (result.resultCode == Activity.RESULT_OK && result.data != null) {
      val accountName = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
      if (!accountName.isNullOrBlank()) {
        viewModel.handleGoogleAccountPicked(accountName) { success ->
          if (success) {
            Toast.makeText(context, "Google account verified: $accountName", Toast.LENGTH_SHORT).show()
          }
        }
      }
    }
  }

  // Auth Section Tab: 0 = Google, 1 = Phone OTP
  var authTab by remember { mutableIntStateOf(0) }

  // Phone OTP States
  var otpCountry by remember { mutableStateOf(CountryCodeHelper.defaultCountry) }
  var otpNationalNumber by remember { mutableStateOf("") }
  var otpCodeInput by remember { mutableStateOf("") }
  var currentVerificationId by remember { mutableStateOf<String?>(null) }
  var otpStatusMessage by remember { mutableStateOf<String?>(null) }
  var isSendingOtp by remember { mutableStateOf(false) }
  var isVerifyingOtp by remember { mutableStateOf(false) }

  // Permission Launcher for Contact Sync
  val contactPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    if (isGranted) {
      viewModel.syncContacts { res ->
        Toast.makeText(
          context,
          "Synced ${res.totalFound} contacts (${res.registeredAppUsers} on Crystal Chat)",
          Toast.LENGTH_LONG
        ).show()
      }
    } else {
      Toast.makeText(context, "Contacts permission denied", Toast.LENGTH_SHORT).show()
    }
  }

  fun triggerContactSync() {
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
      contactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
    }
  }

  val isUserSignedIn = userProfile != null

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Profile & Identity", fontWeight = FontWeight.Bold) },
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
                .background(
                  if (isUserSignedIn) MaterialTheme.colorScheme.primaryContainer
                  else MaterialTheme.colorScheme.surfaceVariant
                ),
              contentAlignment = Alignment.Center
            ) {
              val initial = (userProfile?.displayName ?: "C").take(1).uppercase()
              Text(
                text = initial,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (isUserSignedIn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          val displayName = userProfile?.displayName?.ifBlank { null }
            ?: (if (isUserSignedIn) "Crystal User" else "Sign In Required")
          Text(
            text = displayName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
          )

          // Status Badge
          Spacer(modifier = Modifier.height(4.dp))
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isUserSignedIn) Color(0xFF10B981).copy(alpha = 0.12f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = if (isUserSignedIn) Icons.Default.CheckCircle else Icons.Default.Person,
                contentDescription = null,
                tint = if (isUserSignedIn) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(13.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = when {
                  userProfile?.isGoogleAuth == true -> "Verified with Google"
                  userProfile?.isPhoneAuth == true -> "Verified Phone Account"
                  isUserSignedIn -> "Active Profile"
                  else -> "Not Signed In"
                },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isUserSignedIn) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
              )
            }
          }

          // Handle badge
          val handle = userProfile?.handle?.takeIf { it.isNotBlank() } ?: "not_set"
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
              .padding(top = 8.dp)
              .clip(RoundedCornerShape(12.dp))
              .clickable(enabled = isUserSignedIn) { showEditHandleDialog = true }
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.AlternateEmail,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
              )
              Text(
                text = handle.removePrefix("@"),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              if (isUserSignedIn) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                  imageVector = Icons.Default.Edit,
                  contentDescription = "Edit handle",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(13.dp)
                )
              }
            }
          }

          // Phone Number
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

      // Authentication Card
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
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Key,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (isUserSignedIn) "Connected Identity" else "Account & Authentication",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          if (!isUserSignedIn) {
            TabRow(
              selectedTabIndex = authTab,
              containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
              contentColor = MaterialTheme.colorScheme.primary,
              modifier = Modifier.clip(RoundedCornerShape(10.dp))
            ) {
              Tab(
                selected = authTab == 0,
                onClick = { authTab = 0 },
                text = { Text("Phone OTP", fontWeight = FontWeight.SemiBold) }
              )
              Tab(
                selected = authTab == 1,
                onClick = { authTab = 1 },
                text = { Text("Google Sign-In", fontWeight = FontWeight.SemiBold) }
              )
            }

            Spacer(modifier = Modifier.height(14.dp))

            authError?.let { err ->
              Text(
                text = err,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp)
              )
            }

            if (authTab == 0) {
              // Phone OTP Tab
              Text(
                text = "Enter your mobile number with country code to receive an official SMS verification code.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
              )

              Spacer(modifier = Modifier.height(12.dp))

              if (currentVerificationId == null) {
                PhoneInputField(
                  nationalNumber = otpNationalNumber,
                  onNationalNumberChange = { otpNationalNumber = it },
                  selectedCountry = otpCountry,
                  onCountrySelected = { otpCountry = it },
                  label = "Mobile Number",
                  modifier = Modifier.fillMaxWidth().testTag("otp_phone_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                val fullPhoneToVerify = "${otpCountry.dialCode}${otpNationalNumber.filter { it.isDigit() }.trimStart('0')}"

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Button(
                    onClick = {
                      if (activity != null && otpNationalNumber.isNotBlank()) {
                        isSendingOtp = true
                        otpStatusMessage = null
                        viewModel.sendPhoneOtp(
                          activity = activity,
                          phoneNumber = fullPhoneToVerify,
                          onCodeSent = { vid ->
                            isSendingOtp = false
                            currentVerificationId = vid
                            otpStatusMessage = "Verification code active for $fullPhoneToVerify"
                            Toast.makeText(context, "Verification code sent!", Toast.LENGTH_SHORT).show()
                          },
                          onAutoVerified = {
                            isSendingOtp = false
                            Toast.makeText(context, "Instant phone verification successful!", Toast.LENGTH_SHORT).show()
                          },
                          onError = { err ->
                            isSendingOtp = false
                            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                          }
                        )
                      }
                    },
                    enabled = !isSendingOtp && otpNationalNumber.length >= 6,
                    modifier = Modifier
                      .weight(1.2f)
                      .testTag("send_otp_button"),
                    shape = RoundedCornerShape(12.dp)
                  ) {
                    if (isSendingOtp) {
                      CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Text("Sending...", fontSize = 12.sp)
                    } else {
                      Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(6.dp))
                      Text("Send SMS Code", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                  }

                  OutlinedButton(
                    onClick = {
                      if (otpNationalNumber.length >= 6) {
                        viewModel.sendDeviceSecurityOtp(fullPhoneToVerify) { vid, code ->
                          currentVerificationId = vid
                          otpCodeInput = code
                          otpStatusMessage = "Device Verification Code: $code"
                          Toast.makeText(context, "Verification code: $code", Toast.LENGTH_SHORT).show()
                        }
                      }
                    },
                    enabled = !isSendingOtp && otpNationalNumber.length >= 6,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                  ) {
                    Text("Instant Code", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                  }
                }

                if (isApiKeyRestricted) {
                  Spacer(modifier = Modifier.height(10.dp))
                  Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Text(
                        text = "Need direct SMS via custom Firebase?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                      TextButton(onClick = { showApiKeyDialog = true }) {
                        Text("Custom Key", fontSize = 12.sp)
                      }
                    }
                  }
                }
              } else {
                // OTP Code Entry
                generatedSecurityCode?.let { genCode ->
                  Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Column {
                        Text("Verification Code:", style = MaterialTheme.typography.labelSmall)
                        Text(
                          text = genCode,
                          style = MaterialTheme.typography.titleMedium,
                          fontWeight = FontWeight.Bold,
                          color = MaterialTheme.colorScheme.primary
                        )
                      }
                      Button(
                        onClick = { otpCodeInput = genCode },
                        shape = RoundedCornerShape(8.dp)
                      ) {
                        Text("Auto-fill", fontSize = 12.sp)
                      }
                    }
                  }
                  Spacer(modifier = Modifier.height(10.dp))
                }

                otpStatusMessage?.let { msg ->
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Text(
                      text = msg,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      fontWeight = FontWeight.Medium,
                      modifier = Modifier.padding(8.dp)
                    )
                  }
                  Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedTextField(
                  value = otpCodeInput,
                  onValueChange = { if (it.length <= 6) otpCodeInput = it },
                  label = { Text("6-Digit Verification Code") },
                  placeholder = { Text("Enter 6-digit code") },
                  singleLine = true,
                  modifier = Modifier.fillMaxWidth().testTag("otp_code_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                val fullPhoneToVerify = "${otpCountry.dialCode}${otpNationalNumber.filter { it.isDigit() }.trimStart('0')}"

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  OutlinedButton(
                    onClick = {
                      currentVerificationId = null
                      otpCodeInput = ""
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                  ) {
                    Text("Change Number")
                  }

                  Button(
                    onClick = {
                      val vid = currentVerificationId ?: return@Button
                      isVerifyingOtp = true
                      viewModel.verifyPhoneOtp(
                        verificationId = vid,
                        otpCode = otpCodeInput,
                        phoneNumber = fullPhoneToVerify
                      ) { success, err ->
                        isVerifyingOtp = false
                        if (success) {
                          currentVerificationId = null
                          otpCodeInput = ""
                          Toast.makeText(context, "Phone verified successfully!", Toast.LENGTH_SHORT).show()
                        } else {
                          Toast.makeText(context, err ?: "Verification failed", Toast.LENGTH_SHORT).show()
                        }
                      }
                    },
                    enabled = !isVerifyingOtp && otpCodeInput.length >= 6,
                    modifier = Modifier.weight(1.3f).testTag("verify_otp_button"),
                    shape = RoundedCornerShape(10.dp)
                  ) {
                    if (isVerifyingOtp) {
                      CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                      Spacer(modifier = Modifier.width(6.dp))
                      Text("Verifying...")
                    } else {
                      Text("Verify & Sign In")
                    }
                  }
                }
              }
            } else {
              // Google Sign-In Tab
              Text(
                text = "Sign in using your Google account on this device to verify your identity and enable end-to-end encrypted messaging.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
              )

              Spacer(modifier = Modifier.height(14.dp))

              Button(
                onClick = {
                  try {
                    googleAccountPickerLauncher.launch(viewModel.getGoogleSystemPickerIntent())
                  } catch (e: Exception) {
                    if (activity != null) {
                      viewModel.signInWithGoogle(activity)
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
                  Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(8.dp))
                  Text("Sign in with Google Account", fontWeight = FontWeight.SemiBold)
                }
              }
            }
          } else {
            // Signed In View
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(vertical = 4.dp)
            ) {
              Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Verified",
                tint = Color(0xFF10B981),
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(
                  text = if (userProfile?.isGoogleAuth == true) "Connected via Google Identity" else "Connected via Phone Number",
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.SemiBold
                )
                Text(
                  text = if (userProfile?.isGoogleAuth == true) userProfile?.email ?: "" else userProfile?.phoneNumber ?: "",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

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
                  currentVerificationId = null
                  otpCodeInput = ""
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

      // Device Contacts Sync Card
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
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Sync,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Phone Contacts Synchronization",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          Text(
            text = "Sync contacts from your device phonebook to identify who is on Crystal Chat for end-to-end encrypted messaging.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          lastSyncResult?.let { res ->
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.surfaceVariant
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "${res.totalFound} device contacts • ${res.registeredAppUsers} on Crystal Chat",
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.Medium
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          Button(
            onClick = { triggerContactSync() },
            enabled = !isSyncingContacts,
            modifier = Modifier.fillMaxWidth().testTag("sync_contacts_button"),
            shape = RoundedCornerShape(12.dp)
          ) {
            if (isSyncingContacts) {
              CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Syncing Contacts...")
            } else {
              Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Sync Contacts from Phone", fontWeight = FontWeight.SemiBold)
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
                text = "Require fingerprint/PIN to unlock Crystal Chat",
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

  // Custom Firebase API Key Dialog
  if (showApiKeyDialog) {
    AlertDialog(
      onDismissRequest = { showApiKeyDialog = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Firebase API Configuration", fontWeight = FontWeight.Bold)
        }
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "Enter your custom Firebase Web API Key & Project ID to enable direct Firebase Phone SMS OTP:",
            style = MaterialTheme.typography.bodySmall
          )
          OutlinedTextField(
            value = customApiKeyInput,
            onValueChange = { customApiKeyInput = it },
            label = { Text("Firebase Web API Key (AIzaSy...)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("custom_api_key_input")
          )
          OutlinedTextField(
            value = customProjectIdInput,
            onValueChange = { customProjectIdInput = it },
            label = { Text("Project ID (Optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("custom_project_id_input")
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (customApiKeyInput.isNotBlank()) {
              val saved = viewModel.setCustomFirebaseApiKey(customApiKeyInput, customProjectIdInput)
              if (saved) {
                showApiKeyDialog = false
                Toast.makeText(context, "Firebase API Key saved successfully", Toast.LENGTH_SHORT).show()
              }
            }
          },
          enabled = customApiKeyInput.isNotBlank(),
          modifier = Modifier.testTag("save_custom_api_key_button")
        ) {
          Text("Save & Apply")
        }
      },
      dismissButton = {
        TextButton(onClick = { showApiKeyDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  // Edit Handle Dialog
  if (showEditHandleDialog) {
    var newHandleInput by remember { mutableStateOf(userProfile?.handle?.removePrefix("@") ?: "") }
    var displayNameInput by remember { mutableStateOf(userProfile?.displayName ?: "") }
    val initialPhoneExtracted = remember(userProfile?.phoneNumber) {
      CountryCodeHelper.extractDialCode(userProfile?.phoneNumber ?: "")
    }
    var editCountry by remember { mutableStateOf(initialPhoneExtracted.first) }
    var editNationalNumber by remember { mutableStateOf(initialPhoneExtracted.second) }

    var editError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    AlertDialog(
      onDismissRequest = { if (!isSaving) showEditHandleDialog = false },
      title = { Text("Edit Identity & Handle", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          OutlinedTextField(
            value = newHandleInput,
            onValueChange = {
              newHandleInput = it.filter { char -> char.isLetterOrDigit() || char == '_' }
              editError = null
            },
            label = { Text("Handle (@name)") },
            prefix = { Text("@") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("handle_input_field")
          )

          OutlinedTextField(
            value = displayNameInput,
            onValueChange = { displayNameInput = it },
            label = { Text("Display Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("display_name_input_field")
          )

          PhoneInputField(
            nationalNumber = editNationalNumber,
            onNationalNumberChange = { editNationalNumber = it },
            selectedCountry = editCountry,
            onCountrySelected = { editCountry = it },
            label = "Phone Number",
            modifier = Modifier.fillMaxWidth()
          )

          editError?.let { err ->
            Text(
              text = err,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.error
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            isSaving = true
            editError = null
            val fullPhone = "${editCountry.dialCode} ${editNationalNumber.trim()}"
            viewModel.updateHandleAndProfile(
              newHandle = newHandleInput,
              displayName = displayNameInput,
              phoneNumber = fullPhone
            ) { result ->
              isSaving = false
              result.onSuccess {
                showEditHandleDialog = false
                Toast.makeText(context, "Handle updated: @${it.handle}", Toast.LENGTH_SHORT).show()
              }.onFailure { ex ->
                editError = ex.message ?: "Failed to update handle"
              }
            }
          },
          enabled = !isSaving && newHandleInput.length >= 3,
          modifier = Modifier.testTag("save_handle_button")
        ) {
          if (isSaving) {
            CircularProgressIndicator(
              modifier = Modifier.size(16.dp),
              color = MaterialTheme.colorScheme.onPrimary,
              strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Saving...")
          } else {
            Text("Save")
          }
        }
      },
      dismissButton = {
        TextButton(
          onClick = { showEditHandleDialog = false },
          enabled = !isSaving
        ) {
          Text("Cancel")
        }
      }
    )
  }
}
