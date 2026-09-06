package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.WorkspacePremium
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.StrawberrycandyRepository
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
  onSignInWithGoogle: (email: String, password: String, name: String, role: String, authorSlot: Int?) -> Unit,
  onSignInWithApple: (email: String, password: String, name: String, role: String, authorSlot: Int?) -> Unit,
  initialEmail: String = "",
  externalErrorMessage: String? = null,
  onClearError: () -> Unit = {},
) {
  var emailInput by remember { mutableStateOf(initialEmail) }
  var passwordInput by remember { mutableStateOf("") }
  var isPasswordVisible by remember { mutableStateOf(false) }
  var nameInput by remember { mutableStateOf("") }
  var localError by remember { mutableStateOf<String?>(null) }
  val displayErrorMessage = localError ?: externalErrorMessage

  val isOwnerDetected = StrawberrycandyRepository.isOwnerEmail(emailInput)

  fun validateAndSubmit(provider: String) {
    var trimmedEmail = emailInput.trim()
    if (provider == "GOOGLE") {
      if (trimmedEmail.isBlank()) {
        localError = "Please enter your Gmail address."
        return
      }
      if (!trimmedEmail.contains("@")) {
        trimmedEmail = "$trimmedEmail@gmail.com"
        emailInput = trimmedEmail
      }
      val normalizedEmail = trimmedEmail.lowercase()
      if (!normalizedEmail.contains("@") || !normalizedEmail.contains(".")) {
        localError = "Please enter a valid Gmail address (e.g. username@gmail.com)."
        return
      }
    } else {
      if (trimmedEmail.isBlank()) {
        localError = "Please enter your email address."
        return
      }
      if (!trimmedEmail.contains("@") || !trimmedEmail.contains(".")) {
        localError = "Please enter a valid email address."
        return
      }
    }

    if (passwordInput.isBlank()) {
      localError = "Please enter your password."
      return
    }

    localError = null
    onClearError()

    val isOwner = StrawberrycandyRepository.isOwnerEmail(trimmedEmail)
    val effectiveRole = if (isOwner) "OWNER" else "READER"

    val emailRegex = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
    val sanitizedNameInput = nameInput.replace(emailRegex, "").trim()

    val finalName = if (sanitizedNameInput.isNotBlank()) {
      sanitizedNameInput
    } else {
      if (isOwner) {
        "Clarify"
      } else {
        val prefix = trimmedEmail.substringBefore("@").replace(".", " ")
        prefix.split(" ").joinToString(" ") { word ->
          word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }.ifBlank { "Reader" }
      }
    }

    val assignedSlot: Int? = if (isOwner) 0 else null

    if (provider == "GOOGLE") {
      onSignInWithGoogle(trimmedEmail, passwordInput, finalName, effectiveRole, assignedSlot)
    } else {
      onSignInWithApple(trimmedEmail, passwordInput, finalName, effectiveRole, assignedSlot)
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Card(
      shape = RoundedCornerShape(22.dp),
      colors = CardDefaults.cardColors(containerColor = SoftCreamPaper),
      border = BorderStroke(1.dp, SubtleBorder),
      elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 24.dp)
        .testTag("auth_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 22.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Header Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(AntiqueGold)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "STRAWBERRYCANDY",
              style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 1.6.sp,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
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

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = "Sign In",
          style = MaterialTheme.typography.headlineSmall.copy(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp
          ),
          color = CharcoalText,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = "Sign in to access your personal reading library, favorites, or translation tools.",
          style = MaterialTheme.typography.bodySmall.copy(
            fontSize = 12.sp,
            lineHeight = 16.sp
          ),
          color = CharcoalSecondary,
          textAlign = TextAlign.Center
        )

        // Owner indicator badge if owner email detected
        if (isOwnerDetected) {
          Spacer(modifier = Modifier.height(10.dp))
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = AntiqueGoldLight.copy(alpha = 0.7f),
            border = BorderStroke(1.dp, AntiqueGold),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("auth_owner_verified_banner")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Outlined.WorkspacePremium,
                contentDescription = null,
                tint = AntiqueGold,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Founder & Sole Owner Account (Clarify)",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 11.sp,
                  color = CharcoalText
                )
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Gmail Input
        Column(modifier = Modifier.fillMaxWidth()) {
          Text(
            text = "GMAIL ADDRESS",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 9.5.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp
            ),
            color = CharcoalText
          )

          Spacer(modifier = Modifier.height(5.dp))

          OutlinedTextField(
            value = emailInput,
            onValueChange = {
              emailInput = it
              localError = null
              onClearError()
            },
            placeholder = { Text("username@gmail.com", color = CharcoalTertiary, fontSize = 13.sp) },
            leadingIcon = {
              Icon(
                imageVector = Icons.Outlined.Mail,
                contentDescription = null,
                tint = if (isOwnerDetected) AntiqueGold else CharcoalSecondary,
                modifier = Modifier.size(16.dp)
              )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = AntiqueGold,
              unfocusedBorderColor = SubtleBorder,
              focusedContainerColor = Color.White,
              unfocusedContainerColor = Color.White,
              focusedTextColor = CharcoalText,
              unfocusedTextColor = CharcoalText
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("auth_email_input")
          )

          if (emailInput.isNotBlank() && !emailInput.contains("@")) {
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
              onClick = {
                emailInput = "${emailInput.trim()}@gmail.com"
                localError = null
              },
              shape = RoundedCornerShape(8.dp),
              color = AntiqueGold.copy(alpha = 0.12f),
              border = BorderStroke(0.5.dp, AntiqueGold.copy(alpha = 0.4f)),
              modifier = Modifier.align(Alignment.Start)
            ) {
              Text(
                text = "+ @gmail.com",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 10.sp,
                  fontWeight = FontWeight.SemiBold
                ),
                color = AntiqueGold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Password Input
        Column(modifier = Modifier.fillMaxWidth()) {
          Text(
            text = "PASSWORD",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 9.5.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp
            ),
            color = CharcoalText
          )

          Spacer(modifier = Modifier.height(5.dp))

          OutlinedTextField(
            value = passwordInput,
            onValueChange = {
              passwordInput = it
              localError = null
              onClearError()
            },
            placeholder = { Text("Enter your account password", color = CharcoalTertiary, fontSize = 13.sp) },
            leadingIcon = {
              Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                tint = CharcoalSecondary,
                modifier = Modifier.size(16.dp)
              )
            },
            trailingIcon = {
              IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                Icon(
                  imageVector = if (isPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                  contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                  tint = CharcoalSecondary,
                  modifier = Modifier.size(16.dp)
                )
              }
            },
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = AntiqueGold,
              unfocusedBorderColor = SubtleBorder,
              focusedContainerColor = Color.White,
              unfocusedContainerColor = Color.White,
              focusedTextColor = CharcoalText,
              unfocusedTextColor = CharcoalText
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("auth_password_input")
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Optional Display / Pen Name Input
        Column(modifier = Modifier.fillMaxWidth()) {
          Text(
            text = "DISPLAY OR PEN NAME (OPTIONAL)",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 9.5.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp
            ),
            color = CharcoalSecondary
          )

          Spacer(modifier = Modifier.height(5.dp))

          OutlinedTextField(
            value = nameInput,
            onValueChange = { nameInput = it },
            placeholder = { Text("Leave blank to use email prefix", color = CharcoalTertiary, fontSize = 13.sp) },
            leadingIcon = {
              Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = null,
                tint = CharcoalSecondary,
                modifier = Modifier.size(16.dp)
              )
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = AntiqueGold,
              unfocusedBorderColor = SubtleBorder,
              focusedContainerColor = Color.White,
              unfocusedContainerColor = Color.White,
              focusedTextColor = CharcoalText,
              unfocusedTextColor = CharcoalText
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("auth_name_input")
          )
        }

        // Error message banner
        if (displayErrorMessage != null) {
          Spacer(modifier = Modifier.height(12.dp))
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFFFDEDEC),
            border = BorderStroke(1.dp, Color(0xFFE57373)),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("auth_error_banner")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = Color(0xFFC62828),
                modifier = Modifier.size(15.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = displayErrorMessage,
                style = MaterialTheme.typography.bodySmall.copy(
                  fontSize = 11.5.sp,
                  lineHeight = 15.sp
                ),
                color = Color(0xFFC62828)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Sign In with Google
        Surface(
          onClick = { validateAndSubmit("GOOGLE") },
          shape = RoundedCornerShape(12.dp),
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
              .padding(horizontal = 16.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Box(
              modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(Color(0xFF4285F4)),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "G",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "Sign in with Google",
              style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
              ),
              color = Color(0xFF3C4043)
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Sign In with Apple
        Surface(
          onClick = { validateAndSubmit("APPLE") },
          shape = RoundedCornerShape(12.dp),
          color = Color(0xFF1E1815),
          shadowElevation = 1.dp,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("sign_in_apple_button")
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Text(
              text = "",
              color = Color.White,
              fontSize = 15.sp,
              modifier = Modifier.padding(bottom = 1.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Sign in with Apple",
              style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
              ),
              color = Color.White
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center
        ) {
          Icon(
            imageVector = Icons.Outlined.Lock,
            contentDescription = null,
            tint = CharcoalTertiary,
            modifier = Modifier.size(10.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "Translators granted permission by Clarify automatically unlock editing access.",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 8.5.sp,
              lineHeight = 11.sp
            ),
            color = CharcoalTertiary,
            textAlign = TextAlign.Center
          )
        }
      }
    }
  }
}
