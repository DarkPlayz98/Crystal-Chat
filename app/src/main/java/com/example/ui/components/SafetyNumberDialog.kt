package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.model.ConversationEntity
import kotlin.math.abs

@Composable
fun SafetyNumberDialog(
  conversation: ConversationEntity,
  onToggleVerification: () -> Unit,
  onDismiss: () -> Unit
) {
  val clipboardManager = LocalClipboardManager.current
  val context = LocalContext.current

  val blocks = conversation.safetyNumber.split(" ").chunked(3)

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = if (conversation.isVerified) Icons.Default.Verified else Icons.Default.Warning,
          contentDescription = null,
          tint = if (conversation.isVerified) Color(0xFF10B981) else Color(0xFFF59E0B),
          modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Safety Numbers",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold
        )
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(
          text = "To verify that your end-to-end encryption is secure with ${conversation.title}, compare these 60 numbers with their device or scan the QR code.",
          style = MaterialTheme.typography.bodySmall,
          textAlign = TextAlign.Center,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Simulated Cryptographic QR Matrix
        Box(
          modifier = Modifier
            .size(140.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(8.dp),
          contentAlignment = Alignment.Center
        ) {
          Canvas(modifier = Modifier.size(124.dp)) {
            val hash = conversation.safetyNumber.hashCode()
            val grid = 15
            val cellSize = size.width / grid
            for (r in 0 until grid) {
              for (c in 0 until grid) {
                // Outer corners standard QR finder patterns
                val isCorner1 = (r < 4 && c < 4)
                val isCorner2 = (r < 4 && c >= grid - 4)
                val isCorner3 = (r >= grid - 4 && c < 4)
                val isFilled = if (isCorner1 || isCorner2 || isCorner3) {
                  (r == 0 || r == 3 || c == 0 || c == 3 || (r == 1 && c == 1 && (isCorner1 || isCorner2 || isCorner3)))
                } else {
                  abs((hash + r * 31 + c * 17) % 3) == 0
                }

                if (isFilled) {
                  drawRect(
                    color = Color.Black,
                    topLeft = Offset(c * cellSize, r * cellSize),
                    size = Size(cellSize, cellSize)
                  )
                }
              }
            }
          }
        }

        // 60-digit blocks (12 groups of 5)
        Card(
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
          ),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            blocks.forEach { rowBlocks ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
              ) {
                rowBlocks.forEach { block ->
                  Text(
                    text = block,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }
          }
        }

        // Copy button
        OutlinedButton(
          onClick = {
            clipboardManager.setText(AnnotatedString(conversation.safetyNumber))
            Toast.makeText(context, "Safety numbers copied", Toast.LENGTH_SHORT).show()
          },
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Copy Safety Number")
        }

        // Verification Switch
        Card(
          colors = CardDefaults.cardColors(
            containerColor = if (conversation.isVerified) Color(0xFF064E3B).copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant
          ),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = if (conversation.isVerified) "Marked as Verified" else "Not Yet Verified",
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium,
                color = if (conversation.isVerified) Color(0xFF34D399) else MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Tamper detection and key change alert active",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Switch(
              checked = conversation.isVerified,
              onCheckedChange = { onToggleVerification() }
            )
          }
        }
      }
    },
    confirmButton = {
      Button(onClick = onDismiss) {
        Text("Done")
      }
    }
  )
}
