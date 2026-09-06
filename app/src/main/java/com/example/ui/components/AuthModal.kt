package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.AntiqueGold
import com.example.ui.theme.AntiqueGoldLight
import com.example.ui.theme.CharcoalSecondary
import com.example.ui.theme.CharcoalTertiary
import com.example.ui.theme.CharcoalText
import com.example.ui.theme.SoftCreamPaper
import com.example.ui.theme.SubtleBorder

@Composable
fun AuthModal(
  onDismiss: () -> Unit,
  onSignInWithGoogle: (email: String, name: String, role: String, authorSlot: Int?) -> Unit,
  onSignInWithApple: (email: String, name: String, role: String, authorSlot: Int?) -> Unit,
  initialEmail: String = "",
) {
  var emailInput by remember { mutableStateOf(initialEmail) }
  var nameInput by remember { mutableStateOf("") }
  var selectedRole by remember { mutableStateOf("TRANSLATOR") } // "TRANSLATOR", "READER", "OWNER"
  var errorMessage by remember { mutableStateOf<String?>(null) }

  fun validateAndSubmit(provider: String) {
    val trimmedEmail = emailInput.trim()
    if (trimmedEmail.isBlank()) {
      errorMessage = "Please enter your Gmail / email address before signing in."
      return
    }
    if (!trimmedEmail.contains("@") || !trimmedEmail.contains(".")) {
      errorMessage = "Please enter a valid email address (e.g. clarifymanga@gmail.com)."
      return
    }

    errorMessage = null
    val finalName = if (nameInput.isNotBlank()) {
      nameInput.trim()
    } else {
      val prefix = trimmedEmail.substringBefore("@").replace(".", " ")
      prefix.split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
      }
    }

    val assignedSlot: Int? = when (selectedRole) {
      "OWNER" -> 0
      "TRANSLATOR" -> 1
      else -> null
    }

    if (provider == "GOOGLE") {
      onSignInWithGoogle(trimmedEmail, finalName, selectedRole, assignedSlot)
    } else {
      onSignInWithApple(trimmedEmail, finalName, selectedRole, assignedSlot)
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Card(
      shape = RoundedCornerShape(26.dp),
      colors = CardDefaults.cardColors(containerColor = SoftCreamPaper),
      border = BorderStroke(1.dp, SubtleBorder),
      elevation = CardDefaults.cardElevation(defaultElevation = 14.dp),
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 24.dp)
        .testTag("auth_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Header
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
              text = "STRAWBERRYCANDY ARCHIVE ID",
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

        Spacer(modifier = Modifier.height(14.dp))

        Text(
          text = "Sign In to Your Account",
          style = MaterialTheme.typography.headlineSmall.copy(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold
          ),
          color = CharcoalText,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = "Enter your Gmail to authenticate. Translators can edit novels and publish works; Readers enjoy a clean read-only catalog.",
          style = MaterialTheme.typography.bodySmall.copy(
            lineHeight = 18.sp
          ),
          color = CharcoalSecondary,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Role Selector Section
        Column(modifier = Modifier.fillMaxWidth()) {
          Text(
            text = "SELECT YOUR ACCOUNT ROLE",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.2.sp
            ),
            color = AntiqueGold
          )
          Spacer(modifier = Modifier.height(8.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            // Translator Role
            RoleSelectionCard(
              title = "Translator",
              subtitle = "Can edit & manage novels",
              isSelected = selectedRole == "TRANSLATOR",
              icon = Icons.Outlined.Edit,
              onClick = { selectedRole = "TRANSLATOR" },
              modifier = Modifier.weight(1f)
            )

            // Reader Role
            RoleSelectionCard(
              title = "Reader",
              subtitle = "Read-only • Edits hidden",
              isSelected = selectedRole == "READER",
              icon = Icons.Outlined.MenuBook,
              onClick = { selectedRole = "READER" },
              modifier = Modifier.weight(1f)
            )

            // Owner Role
            RoleSelectionCard(
              title = "Owner",
              subtitle = "Archive Founder",
              isSelected = selectedRole == "OWNER",
              icon = Icons.Outlined.WorkspacePremium,
              onClick = { selectedRole = "OWNER" },
              modifier = Modifier.weight(1f)
            )
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Email Text Field (Google / Email)
        Column(modifier = Modifier.fillMaxWidth()) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "GMAIL / EMAIL ADDRESS *",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp
              ),
              color = CharcoalText
            )

            // Quick suggestion chip
            Text(
              text = "clarifymanga@gmail.com",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = AntiqueGold
              ),
              modifier = Modifier
                .clickable {
                  emailInput = "clarifymanga@gmail.com"
                  if (nameInput.isBlank()) nameInput = "Clarify"
                  errorMessage = null
                }
                .padding(2.dp)
            )
          }

          Spacer(modifier = Modifier.height(6.dp))

          OutlinedTextField(
            value = emailInput,
            onValueChange = {
              emailInput = it
              errorMessage = null
            },
            placeholder = { Text("e.g. clarifymanga@gmail.com", color = CharcoalTertiary) },
            leadingIcon = {
              Icon(
                imageVector = Icons.Outlined.Mail,
                contentDescription = null,
                tint = AntiqueGold,
                modifier = Modifier.size(18.dp)
              )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = AntiqueGold,
              unfocusedBorderColor = SubtleBorder,
              focusedContainerColor = SoftCreamPaper,
              unfocusedContainerColor = SoftCreamPaper,
              focusedTextColor = CharcoalText,
              unfocusedTextColor = CharcoalText
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("auth_email_input")
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Name / Pen Name Text Field
        Column(modifier = Modifier.fillMaxWidth()) {
          Text(
            text = "NAME OR PEN NAME",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.1.sp
            ),
            color = CharcoalText
          )
          Spacer(modifier = Modifier.height(6.dp))

          OutlinedTextField(
            value = nameInput,
            onValueChange = { nameInput = it },
            placeholder = { Text("Your pen name or reader display name", color = CharcoalTertiary) },
            leadingIcon = {
              Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = null,
                tint = CharcoalSecondary,
                modifier = Modifier.size(18.dp)
              )
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = AntiqueGold,
              unfocusedBorderColor = SubtleBorder,
              focusedContainerColor = SoftCreamPaper,
              unfocusedContainerColor = SoftCreamPaper,
              focusedTextColor = CharcoalText,
              unfocusedTextColor = CharcoalText
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("auth_name_input")
          )
        }

        // Error message banner
        if (errorMessage != null) {
          Spacer(modifier = Modifier.height(12.dp))
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(Color(0xFFFBE9E7))
              .border(1.dp, Color(0xFFFFCCBC), RoundedCornerShape(10.dp))
              .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Outlined.ErrorOutline,
              contentDescription = null,
              tint = Color(0xFFD32F2F),
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = errorMessage ?: "",
              style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                color = Color(0xFFC62828),
                fontWeight = FontWeight.Medium
              )
            )
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Role summary banner
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = if (selectedRole == "READER") Color(0x0F000000) else AntiqueGoldLight.copy(alpha = 0.45f),
          border = BorderStroke(1.dp, if (selectedRole == "READER") SubtleBorder else AntiqueGold.copy(alpha = 0.35f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = when (selectedRole) {
                "OWNER" -> Icons.Outlined.Security
                "TRANSLATOR" -> Icons.Outlined.Check
                else -> Icons.Outlined.MenuBook
              },
              contentDescription = null,
              tint = AntiqueGold,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = when (selectedRole) {
                "TRANSLATOR" -> "Translators can edit novel content, synopsis, title & covers via 3-second hold or edit button."
                "OWNER" -> "Archive Owner has full rights to upload, edit all manuscripts, and manage translator permissions."
                else -> "Reader Mode: Edit buttons & toggle switches are hidden to preserve clean book immersion."
              },
              style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 10.5.sp,
                lineHeight = 14.sp,
                color = CharcoalText
              )
            )
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // SIGN IN WITH GOOGLE BUTTON
        Surface(
          onClick = { validateAndSubmit("GOOGLE") },
          shape = RoundedCornerShape(14.dp),
          color = Color.White,
          border = BorderStroke(1.dp, Color(0xFFDADCE0)),
          shadowElevation = 2.dp,
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
            // Google 'G' icon
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
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.4.sp
              ),
              color = Color(0xFF3C4043)
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // SIGN IN WITH APPLE BUTTON
        Surface(
          onClick = { validateAndSubmit("APPLE") },
          shape = RoundedCornerShape(14.dp),
          color = Color(0xFF1E1815),
          shadowElevation = 2.dp,
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
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.4.sp
              ),
              color = Color.White
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Privacy note
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
              fontSize = 8.5.sp,
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
private fun RoleSelectionCard(
  title: String,
  subtitle: String,
  isSelected: Boolean,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(14.dp),
    color = if (isSelected) AntiqueGoldLight.copy(alpha = 0.85f) else Color.White,
    border = BorderStroke(
      width = if (isSelected) 1.5.dp else 1.dp,
      color = if (isSelected) AntiqueGold else SubtleBorder
    ),
    modifier = modifier.height(86.dp)
  ) {
    Column(
      modifier = Modifier
        .padding(horizontal = 8.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = if (isSelected) AntiqueGold else CharcoalSecondary,
          modifier = Modifier.size(16.dp)
        )
        if (isSelected) {
          Box(
            modifier = Modifier
              .size(14.dp)
              .clip(CircleShape)
              .background(AntiqueGold),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Outlined.Check,
              contentDescription = null,
              tint = SoftCreamPaper,
              modifier = Modifier.size(10.dp)
            )
          }
        }
      }

      Column {
        Text(
          text = title,
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 11.sp
          ),
          color = CharcoalText
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall.copy(
            fontSize = 8.5.sp,
            lineHeight = 11.sp
          ),
          color = if (isSelected) CharcoalText else CharcoalTertiary,
          maxLines = 2
        )
      }
    }
  }
}
