package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.CountryCode
import com.example.util.CountryCodeHelper

@Composable
fun PhoneInputField(
  nationalNumber: String,
  onNationalNumberChange: (String) -> Unit,
  selectedCountry: CountryCode,
  onCountrySelected: (CountryCode) -> Unit,
  modifier: Modifier = Modifier,
  label: String = "Phone Number",
  placeholder: String = "98765 43210",
  testTagPrefix: String = "phone_input"
) {
  var showPickerDialog by remember { mutableStateOf(false) }

  Column(modifier = modifier.fillMaxWidth()) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(bottom = 4.dp)
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Country Code Selector Button
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        border = androidx.compose.foundation.BorderStroke(
          1.dp,
          MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        modifier = Modifier
          .height(56.dp)
          .clip(RoundedCornerShape(12.dp))
          .clickable { showPickerDialog = true }
          .testTag("${testTagPrefix}_country_button")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = selectedCountry.flagEmoji,
            fontSize = 20.sp
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = selectedCountry.dialCode,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
          )
          Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = "Select country code",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
          )
        }
      }

      Spacer(modifier = Modifier.width(8.dp))

      // National Phone Number Field
      OutlinedTextField(
        value = nationalNumber,
        onValueChange = { input ->
          val digits = input.filter { it.isDigit() || it == ' ' || it == '-' }
          onNationalNumberChange(digits)
        },
        placeholder = { Text(placeholder) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .weight(1f)
          .testTag("${testTagPrefix}_number_field"),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = MaterialTheme.colorScheme.primary,
          unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
      )
    }
  }

  if (showPickerDialog) {
    CountryCodePickerDialog(
      selected = selectedCountry,
      onSelect = { country ->
        onCountrySelected(country)
        showPickerDialog = false
      },
      onDismiss = { showPickerDialog = false }
    )
  }
}

@Composable
fun CountryCodePickerDialog(
  selected: CountryCode,
  onSelect: (CountryCode) -> Unit,
  onDismiss: () -> Unit
) {
  var searchQuery by remember { mutableStateOf("") }

  val filteredCountries = remember(searchQuery) {
    if (searchQuery.isBlank()) {
      CountryCodeHelper.countries
    } else {
      val q = searchQuery.lowercase().trim()
      CountryCodeHelper.countries.filter {
        it.name.lowercase().contains(q) ||
          it.dialCode.contains(q) ||
          it.code.lowercase().contains(q)
      }
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = "Select Country Code",
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.titleLarge
      )
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        // Search bar
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Search country or code (e.g. India, +91)") },
          leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp))
          },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("country_search_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Countries list
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 350.dp)
        ) {
          items(filteredCountries, key = { it.code + it.dialCode }) { country ->
            val isSelected = country.dialCode == selected.dialCode && country.code == selected.code
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { onSelect(country) }
                .padding(vertical = 10.dp, horizontal = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = country.flagEmoji,
                fontSize = 22.sp
              )
              Spacer(modifier = Modifier.width(12.dp))
              Text(
                text = country.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.weight(1f)
              )
              Text(
                text = country.dialCode,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
              )
              if (isSelected) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                  imageVector = Icons.Default.Check,
                  contentDescription = "Selected",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(18.dp)
                )
              }
            }
            HorizontalDivider(
              color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
              thickness = 0.5.dp
            )
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
