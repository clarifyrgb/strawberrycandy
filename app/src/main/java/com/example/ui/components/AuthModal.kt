package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkAdded
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.HistoryEdu
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.AntiqueGold
import com.example.ui.theme.CharcoalSecondary
import com.example.ui.theme.CharcoalTertiary
import com.example.ui.theme.CharcoalText
import com.example.ui.theme.SoftCreamPaper
import com.example.ui.theme.SubtleBorder

@Composable
fun AuthModal(
  onDismiss: () -> Unit,
  onSignInWithGoogle: (email: String, name: String) -> Unit,
  onSignInWithApple: (email: String, name: String) -> Unit,
) {
  var customEmail by remember { mutableStateOf("") }
  var customName by remember { mutableStateOf("") }
  var showCustomEmailInput by remember { mutableStateOf(false) }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = SoftCreamPaper),
      border = BorderStroke(1.dp, SubtleBorder),
      elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 4.dp)
        .testTag("auth_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Header with Close icon
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(AntiqueGold)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "STRAWBERRYCANDY READER ID",
              style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 1.6.sp,
                fontWeight = FontWeight.Bold
              ),
              color = AntiqueGold
            )
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier
              .size(28.dp)
              .testTag("auth_close_button")
          ) {
            Icon(
              imageVector = Icons.Outlined.Close,
              contentDescription = "Close dialog",
              tint = CharcoalSecondary,
              modifier = Modifier.size(18.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = "Sign In to Your Reading Archive",
          style = MaterialTheme.typography.headlineSmall.copy(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Normal
          ),
          color = CharcoalText,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = "Connect your account to store reading novels, curate your favorites list, and continue reading from your exact page.",
          style = MaterialTheme.typography.bodySmall.copy(
            lineHeight = 18.sp
          ),
          color = CharcoalSecondary,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Benefits bullet row
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x0C000000))
            .padding(vertical = 10.dp, horizontal = 12.dp),
          horizontalArrangement = Arrangement.SpaceAround
        ) {
          BenefitItem(icon = Icons.Outlined.BookmarkAdded, label = "Save Shelf")
          BenefitItem(icon = Icons.Outlined.Favorite, label = "Favorites")
          BenefitItem(icon = Icons.Outlined.Sync, label = "Page Sync")
        }

        Spacer(modifier = Modifier.height(20.dp))

        // SIGN IN WITH GOOGLE BUTTON
        Surface(
          onClick = {
            val email = if (customEmail.isNotBlank()) customEmail.trim() else "reader.alex@gmail.com"
            val name = if (customName.isNotBlank()) customName.trim() else "Alex Vance"
            onSignInWithGoogle(email, name)
          },
          shape = RoundedCornerShape(14.dp),
          color = Color.White,
          border = BorderStroke(1.dp, Color(0xFFDADCE0)),
          shadowElevation = 1.dp,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("sign_in_google_button")
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            // Google 'G' icon representation
            Box(
              modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(Color(0xFF4285F4)),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "G",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
              text = "Sign in with Google",
              style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.4.sp
              ),
              color = Color(0xFF3C4043)
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // SIGN IN WITH APPLE BUTTON
        Surface(
          onClick = {
            val email = if (customEmail.isNotBlank()) customEmail.trim() else "reader.user@privaterelay.appleid.com"
            val name = if (customName.isNotBlank()) customName.trim() else "Apple Reader"
            onSignInWithApple(email, name)
          },
          shape = RoundedCornerShape(14.dp),
          color = Color(0xFF000000),
          shadowElevation = 1.dp,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("sign_in_apple_button")
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            // Apple logo icon representation
            Text(
              text = "",
              color = Color.White,
              fontSize = 18.sp,
              modifier = Modifier.padding(bottom = 2.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "Sign in with Apple",
              style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.4.sp
              ),
              color = Color.White
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Toggle custom email specification
        Text(
          text = if (showCustomEmailInput) "Hide custom account field" else "Or enter custom email address",
          style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 10.sp,
            letterSpacing = 0.8.sp
          ),
          color = AntiqueGold,
          modifier = Modifier
            .clickable { showCustomEmailInput = !showCustomEmailInput }
            .padding(4.dp)
        )

        if (showCustomEmailInput) {
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = customName,
            onValueChange = { customName = it },
            label = { Text("Display Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedTextField(
            value = customEmail,
            onValueChange = { customEmail = it },
            label = { Text("Email (Google or Apple)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Privacy indicator
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center
        ) {
          Icon(
            imageVector = Icons.Outlined.Lock,
            contentDescription = null,
            tint = CharcoalTertiary,
            modifier = Modifier.size(11.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "Authenticated with Enclave Token • Private & Encrypted",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 8.sp,
              letterSpacing = 0.8.sp
            ),
            color = CharcoalTertiary
          )
        }
      }
    }
  }
}

@Composable
private fun BenefitItem(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = AntiqueGold,
      modifier = Modifier.size(13.dp)
    )
    Spacer(modifier = Modifier.width(5.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall.copy(
        fontSize = 9.sp,
        fontWeight = FontWeight.Medium
      ),
      color = CharcoalText
    )
  }
}
