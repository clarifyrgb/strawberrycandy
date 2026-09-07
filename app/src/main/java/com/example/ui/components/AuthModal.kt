package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import com.example.data.auth.GoogleSignInHandler
import com.example.data.auth.GoogleSignInOutcome
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.example.data.local.ReaderProfileEntity
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
  rememberedAccounts: List<ReaderProfileEntity> = emptyList(),
  onRequestPasswordResetCode: (email: String, onResult: (Result<String>) -> Unit) -> Unit = { _, _ -> },
  onResetPasswordWithCode: (email: String, code: String, newPassword: String, onResult: (Result<Unit>) -> Unit) -> Unit = { _, _, _, _ -> },
  initialEmail: String = "",
  externalErrorMessage: String? = null,
  onClearError: () -> Unit = {},
  onSignInWithGoogleCredential: ((idToken: String, email: String, name: String?, role: String, authorSlot: Int?) -> Unit)? = null,
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val googleSignInHandler = remember { GoogleSignInHandler(context) }
  var isGoogleSignInProcessing by remember { mutableStateOf(false) }

  // Mode: Sign In vs. Forgot Password
  var isForgotPasswordMode by remember { mutableStateOf(false) }

  // Sign In state
  var emailInput by remember { mutableStateOf(initialEmail.ifBlank { rememberedAccounts.firstOrNull()?.email ?: "" }) }
  val initialSavedPass = remember(rememberedAccounts, initialEmail) {
    val clean = (initialEmail.ifBlank { rememberedAccounts.firstOrNull()?.email ?: "" }).trim().lowercase()
    rememberedAccounts.firstOrNull { it.email.lowercase() == clean }?.passwordHash?.takeIf { it != "clarify123" } ?: ""
  }
  var passwordInput by remember { mutableStateOf(initialSavedPass) }
  var isPasswordVisible by remember { mutableStateOf(false) }
  var nameInput by remember { mutableStateOf("") }
  var rememberAccountOnDevice by remember { mutableStateOf(true) }
  var localError by remember { mutableStateOf<String?>(null) }
  val displayErrorMessage = localError ?: externalErrorMessage

  // Check if current typed email matches a remembered account
  val matchedAccount = remember(emailInput, rememberedAccounts) {
    val clean = emailInput.trim().lowercase()
    if (clean.isNotEmpty()) rememberedAccounts.firstOrNull { it.email.lowercase() == clean } else null
  }

  val isOwnerDetected = StrawberrycandyRepository.isOwnerEmail(emailInput)

  // Forgot Password / Recovery state
  var recoveryEmailInput by remember { mutableStateOf(emailInput) }
  var recoveryCodeInput by remember { mutableStateOf("") }
  var newPasswordInput by remember { mutableStateOf("") }
  var confirmPasswordInput by remember { mutableStateOf("") }
  var isNewPasswordVisible by remember { mutableStateOf(false) }
  var recoveryStep by remember { mutableStateOf(1) } // 1: Request Code, 2: Verify & Set New Password
  var isSendingCode by remember { mutableStateOf(false) }
  var isSubmittingReset by remember { mutableStateOf(false) }
  var recoveryMessage by remember { mutableStateOf<String?>(null) }
  var recoveryError by remember { mutableStateOf<String?>(null) }

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
    if (passwordInput.trim().length < 4) {
      localError = "Password must be at least 4 characters."
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
    } else if (matchedAccount != null && matchedAccount.displayName.isNotBlank()) {
      matchedAccount.displayName
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
            if (isForgotPasswordMode) {
              IconButton(
                onClick = {
                  isForgotPasswordMode = false
                  recoveryError = null
                  recoveryMessage = null
                },
                modifier = Modifier.size(28.dp)
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                  contentDescription = "Back to Sign In",
                  tint = AntiqueGold,
                  modifier = Modifier.size(18.dp)
                )
              }
              Spacer(modifier = Modifier.width(4.dp))
            } else {
              Box(
                modifier = Modifier
                  .size(7.dp)
                  .clip(CircleShape)
                  .background(AntiqueGold)
              )
              Spacer(modifier = Modifier.width(6.dp))
            }

            Text(
              text = if (isForgotPasswordMode) "PASSWORD RECOVERY" else "STRAWBERRYCANDY",
              style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 1.5.sp,
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

        Spacer(modifier = Modifier.height(10.dp))

        if (!isForgotPasswordMode) {
          // -------------------------------------------------------------
          // NORMAL SIGN IN SCREEN WITH REMEMBERED ACCOUNTS RECOGNITION
          // -------------------------------------------------------------
          Text(
            text = "Welcome Back",
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
            text = "Sign in to access your personal reading library, favorites, or translator tools.",
            style = MaterialTheme.typography.bodySmall.copy(
              fontSize = 12.sp,
              lineHeight = 16.sp
            ),
            color = CharcoalSecondary,
            textAlign = TextAlign.Center
          )

          // Remembered Accounts Horizontal Bar
          if (rememberedAccounts.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
              Text(
                text = "SAVED ACCOUNTS ON THIS DEVICE",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 8.5.sp,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.8.sp
                ),
                color = CharcoalSecondary
              )
              Spacer(modifier = Modifier.height(6.dp))
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                rememberedAccounts.forEach { acc ->
                  val isSelected = acc.email.equals(emailInput.trim(), ignoreCase = true)
                  Surface(
                    onClick = {
                      emailInput = acc.email
                      recoveryEmailInput = acc.email
                      if (!acc.passwordHash.isNullOrBlank() && acc.passwordHash != "clarify123") {
                        passwordInput = acc.passwordHash
                      }
                      localError = null
                      onClearError()
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) AntiqueGoldLight else Color.White,
                    border = BorderStroke(1.dp, if (isSelected) AntiqueGold else SubtleBorder),
                    modifier = Modifier.testTag("remembered_account_${acc.email}")
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                      Box(
                        modifier = Modifier
                          .size(18.dp)
                          .clip(CircleShape)
                          .background(if (isSelected) AntiqueGold else CharcoalSecondary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                      ) {
                        Text(
                          text = acc.avatarInitial.take(1),
                          color = Color.White,
                          fontSize = 10.sp,
                          fontWeight = FontWeight.Bold
                        )
                      }
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        text = acc.displayName.ifBlank { acc.email.substringBefore("@") },
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = CharcoalText
                      )
                    }
                  }
                }
              }
            }
          }

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
                  text = "Archive Owner Account • Log in with your password",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = CharcoalText
                  )
                )
              }
            }
          } else if (matchedAccount != null) {
            // Recognized Account Notification
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = AntiqueGoldLight.copy(alpha = 0.4f),
              border = BorderStroke(0.8.dp, AntiqueGold.copy(alpha = 0.5f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Outlined.AccountCircle,
                  contentDescription = null,
                  tint = AntiqueGold,
                  modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Recognized: Welcome back, ${matchedAccount.displayName}!",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.5.sp,
                    color = CharcoalText
                  )
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

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
                recoveryEmailInput = it
                localError = null
                onClearError()
                val clean = it.trim().lowercase()
                val saved = rememberedAccounts.firstOrNull { acc -> acc.email.lowercase() == clean }?.passwordHash
                if (!saved.isNullOrBlank() && saved != "clarify123" && passwordInput.isBlank()) {
                  passwordInput = saved
                }
              },
              placeholder = { Text("username@gmail.com", color = CharcoalTertiary, fontSize = 13.sp) },
              leadingIcon = {
                Icon(
                  imageVector = Icons.Outlined.Mail,
                  contentDescription = null,
                  tint = if (isOwnerDetected || matchedAccount != null) AntiqueGold else CharcoalSecondary,
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
                  recoveryEmailInput = emailInput
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
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "PASSWORD",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 9.5.sp,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 1.sp
                ),
                color = CharcoalText
              )

              if (matchedAccount?.passwordHash?.isNotBlank() == true && matchedAccount.passwordHash != "clarify123" && passwordInput == matchedAccount.passwordHash) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = AntiqueGold,
                    modifier = Modifier.size(12.dp)
                  )
                  Spacer(modifier = Modifier.width(3.dp))
                  Text(
                    text = "Saved password loaded",
                    fontSize = 10.sp,
                    color = AntiqueGold,
                    fontWeight = FontWeight.SemiBold
                  )
                }
              }
            }

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

            Spacer(modifier = Modifier.height(6.dp))

            // Remember me & Forgot Password row
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { rememberAccountOnDevice = !rememberAccountOnDevice }
              ) {
                Checkbox(
                  checked = rememberAccountOnDevice,
                  onCheckedChange = { rememberAccountOnDevice = it },
                  colors = CheckboxDefaults.colors(checkedColor = AntiqueGold),
                  modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "Remember account",
                  fontSize = 11.sp,
                  color = CharcoalSecondary
                )
              }

              // Forgot Password link
              Text(
                text = "Forgot password?",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = AntiqueGold,
                modifier = Modifier
                  .clickable {
                    isForgotPasswordMode = true
                    recoveryEmailInput = emailInput.trim()
                    recoveryStep = 1
                    recoveryError = null
                    recoveryMessage = null
                  }
                  .padding(4.dp)
                  .testTag("forgot_password_button")
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

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
              placeholder = {
                Text(
                  matchedAccount?.displayName?.let { "Saved name: $it" } ?: "Leave blank to use email prefix",
                  color = CharcoalTertiary,
                  fontSize = 13.sp
                )
              },
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

          Spacer(modifier = Modifier.height(16.dp))

          // Sign In with Google
          Surface(
            onClick = {
              if (isGoogleSignInProcessing) return@Surface
              if (passwordInput.isNotBlank() && emailInput.isNotBlank()) {
                validateAndSubmit("GOOGLE")
              } else if (onSignInWithGoogleCredential != null) {
                coroutineScope.launch {
                  isGoogleSignInProcessing = true
                  localError = null
                  onClearError()
                  try {
                    when (val outcome = googleSignInHandler.signInWithGoogle()) {
                      is GoogleSignInOutcome.Success -> {
                        val role = if (isOwnerDetected) "OWNER" else "READER"
                        val effectiveName = outcome.displayName ?: nameInput.trim().ifBlank { null }
                        onSignInWithGoogleCredential(
                          outcome.idToken,
                          outcome.email,
                          effectiveName,
                          role,
                          null
                        )
                      }
                      is GoogleSignInOutcome.FallbackNeeded -> {
                        if (passwordInput.isNotBlank() && emailInput.isNotBlank()) {
                          validateAndSubmit("GOOGLE")
                        } else {
                          localError = outcome.message.ifBlank { "Please enter your password to sign in" }
                        }
                      }
                      is GoogleSignInOutcome.Canceled -> {
                        // User dismissed
                      }
                      is GoogleSignInOutcome.Error -> {
                        if (passwordInput.isNotBlank() && emailInput.isNotBlank()) {
                          validateAndSubmit("GOOGLE")
                        } else {
                          localError = outcome.message
                        }
                      }
                    }
                  } finally {
                    isGoogleSignInProcessing = false
                  }
                }
              } else {
                validateAndSubmit("GOOGLE")
              }
            },
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
              if (isGoogleSignInProcessing) {
                CircularProgressIndicator(
                  modifier = Modifier.size(16.dp),
                  color = Color(0xFF4285F4),
                  strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                  text = "Signing in with Google...",
                  style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                  ),
                  color = Color(0xFF3C4043)
                )
              } else {
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
                  text = if (matchedAccount != null) "Sign in as ${matchedAccount.displayName}" else "Sign in with Google",
                  style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                  ),
                  color = Color(0xFF3C4043)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Apple Sign-In: Reserved for future iOS release (App Store license pending)
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1E1815).copy(alpha = 0.35f),
            border = BorderStroke(0.8.dp, Color(0xFF1E1815).copy(alpha = 0.2f)),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("sign_in_apple_button_disabled")
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
                color = CharcoalSecondary,
                fontSize = 15.sp,
                modifier = Modifier.padding(bottom = 1.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Sign in with Apple (Coming on iOS)",
                style = MaterialTheme.typography.labelLarge.copy(
                  fontWeight = FontWeight.Normal,
                  fontSize = 12.5.sp
                ),
                color = CharcoalSecondary
              )
            }
          }
        } else {
          // -------------------------------------------------------------
          // FORGOT PASSWORD / GMAIL CODE RETRIEVAL & RESET SCREEN
          // -------------------------------------------------------------
          Text(
            text = "Retrieve Account",
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
            text = if (recoveryStep == 1) {
              "Enter your registered Gmail address. We will send a 6-digit verification passcode to your Gmail inbox."
            } else {
              "Enter the 6-digit verification passcode sent to your Gmail and set a new password."
            },
            style = MaterialTheme.typography.bodySmall.copy(
              fontSize = 12.sp,
              lineHeight = 16.sp
            ),
            color = CharcoalSecondary,
            textAlign = TextAlign.Center
          )

          Spacer(modifier = Modifier.height(14.dp))

          // Step 1: Request Passcode to Gmail
          if (recoveryStep == 1) {
            Column(modifier = Modifier.fillMaxWidth()) {
              Text(
                text = "REGISTERED GMAIL ADDRESS",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 9.5.sp,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 1.sp
                ),
                color = CharcoalText
              )

              Spacer(modifier = Modifier.height(5.dp))

              OutlinedTextField(
                value = recoveryEmailInput,
                onValueChange = {
                  recoveryEmailInput = it
                  recoveryError = null
                },
                placeholder = { Text("username@gmail.com", color = CharcoalTertiary, fontSize = 13.sp) },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Outlined.Mail,
                    contentDescription = null,
                    tint = AntiqueGold,
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
                  .testTag("recovery_email_input")
              )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
              onClick = {
                val cleanEmail = recoveryEmailInput.trim().lowercase()
                if (cleanEmail.isBlank()) {
                  recoveryError = "Please enter your Gmail address."
                  return@Button
                }
                isSendingCode = true
                recoveryError = null
                recoveryMessage = null

                onRequestPasswordResetCode(cleanEmail) { result ->
                  isSendingCode = false
                  result.onSuccess {
                    recoveryCodeInput = ""
                    recoveryStep = 2
                    recoveryError = null
                    recoveryMessage = "Verification passcode sent! Please check your Gmail inbox (and Spam folder) for the 6-digit code."
                  }.onFailure { err ->
                    recoveryError = err.message ?: "Could not process password recovery"
                  }
                }
              },
              enabled = !isSendingCode && recoveryEmailInput.isNotBlank(),
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(containerColor = AntiqueGold),
              modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("send_recovery_code_button")
            ) {
              if (isSendingCode) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = SoftCreamPaper, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Dispatching Passcode...", fontSize = 12.sp, color = SoftCreamPaper)
              } else {
                Icon(Icons.Outlined.Mail, contentDescription = null, tint = SoftCreamPaper, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Send Passcode to Gmail", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = SoftCreamPaper)
              }
            }

            Spacer(modifier = Modifier.height(6.dp))

            TextButton(
              onClick = {
                isForgotPasswordMode = false
                recoveryError = null
              },
              modifier = Modifier.testTag("cancel_recovery_button")
            ) {
              Text("Back to Sign In", fontSize = 11.5.sp, color = CharcoalSecondary, fontWeight = FontWeight.SemiBold)
            }
          } else {
            // Step 2: Verification Passcode & New Password
            // Confirmation notice: Code dispatched to Gmail
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = AntiqueGoldLight.copy(alpha = 0.4f),
              border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.5f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Outlined.MarkEmailRead,
                  contentDescription = null,
                  tint = AntiqueGold,
                  modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                  Text(
                    text = "PASSCODE SENT TO GMAIL",
                    fontSize = 9.sp,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold,
                    color = AntiqueGold
                  )
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = "A 6-digit verification passcode was sent to ${recoveryEmailInput.trim()}. Check your inbox or Spam folder and enter the code below.",
                    fontSize = 11.5.sp,
                    color = CharcoalText,
                    lineHeight = 15.sp
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 6-digit Code Input
            Column(modifier = Modifier.fillMaxWidth()) {
              Text(
                text = "6-DIGIT VERIFICATION CODE",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 9.5.sp,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 1.sp
                ),
                color = CharcoalText
              )

              Spacer(modifier = Modifier.height(5.dp))

              OutlinedTextField(
                value = recoveryCodeInput,
                onValueChange = {
                  if (it.length <= 6) {
                    recoveryCodeInput = it
                    recoveryError = null
                  }
                },
                placeholder = { Text("123456", color = CharcoalTertiary, fontSize = 13.sp) },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Outlined.Key,
                    contentDescription = null,
                    tint = AntiqueGold,
                    modifier = Modifier.size(16.dp)
                  )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                  .testTag("recovery_code_input")
              )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // New Password Input
            Column(modifier = Modifier.fillMaxWidth()) {
              Text(
                text = "NEW PASSWORD",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 9.5.sp,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 1.sp
                ),
                color = CharcoalText
              )

              Spacer(modifier = Modifier.height(5.dp))

              OutlinedTextField(
                value = newPasswordInput,
                onValueChange = {
                  newPasswordInput = it
                  recoveryError = null
                },
                placeholder = { Text("Enter new password", color = CharcoalTertiary, fontSize = 13.sp) },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = CharcoalSecondary,
                    modifier = Modifier.size(16.dp)
                  )
                },
                trailingIcon = {
                  IconButton(onClick = { isNewPasswordVisible = !isNewPasswordVisible }) {
                    Icon(
                      imageVector = if (isNewPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                      contentDescription = null,
                      tint = CharcoalSecondary,
                      modifier = Modifier.size(16.dp)
                    )
                  }
                },
                visualTransformation = if (isNewPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
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
                  .testTag("recovery_new_password_input")
              )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Confirm Password Input
            Column(modifier = Modifier.fillMaxWidth()) {
              Text(
                text = "CONFIRM NEW PASSWORD",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 9.5.sp,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 1.sp
                ),
                color = CharcoalText
              )

              Spacer(modifier = Modifier.height(5.dp))

              OutlinedTextField(
                value = confirmPasswordInput,
                onValueChange = {
                  confirmPasswordInput = it
                  recoveryError = null
                },
                placeholder = { Text("Re-enter new password", color = CharcoalTertiary, fontSize = 13.sp) },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = CharcoalSecondary,
                    modifier = Modifier.size(16.dp)
                  )
                },
                visualTransformation = if (isNewPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
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
                  .testTag("recovery_confirm_password_input")
              )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
              onClick = {
                val cleanEmail = recoveryEmailInput.trim().lowercase()
                val cleanCode = recoveryCodeInput.trim()
                val cleanPass = newPasswordInput.trim()

                if (cleanCode.length != 6) {
                  recoveryError = "Please enter the 6-digit code sent to your Gmail."
                  return@Button
                }
                if (cleanPass.length < 4) {
                  recoveryError = "New password must be at least 4 characters."
                  return@Button
                }
                if (cleanPass != confirmPasswordInput.trim()) {
                  recoveryError = "Passwords do not match."
                  return@Button
                }

                isSubmittingReset = true
                recoveryError = null

                onResetPasswordWithCode(cleanEmail, cleanCode, cleanPass) { result ->
                  isSubmittingReset = false
                  result.onSuccess {
                    Toast.makeText(context, "Password updated successfully! Welcome back.", Toast.LENGTH_SHORT).show()
                    onDismiss()
                  }.onFailure { err ->
                    recoveryError = err.message ?: "Failed to reset password"
                  }
                }
              },
              enabled = !isSubmittingReset && recoveryCodeInput.isNotBlank() && newPasswordInput.isNotBlank(),
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(containerColor = AntiqueGold),
              modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("confirm_reset_password_button")
            ) {
              if (isSubmittingReset) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = SoftCreamPaper, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Updating password...", fontSize = 12.sp, color = SoftCreamPaper)
              } else {
                Text("Reset Password & Sign In", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = SoftCreamPaper)
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
              onClick = {
                recoveryStep = 1
                recoveryError = null
              },
              shape = RoundedCornerShape(12.dp),
              border = BorderStroke(0.8.dp, AntiqueGold),
              modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
            ) {
              Text("Request New 2FA Verification Code", fontSize = 11.5.sp, color = AntiqueGold)
            }
          }

          // Error in recovery
          if (recoveryError != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = Color(0xFFFDEDEC),
              border = BorderStroke(1.dp, Color(0xFFE57373)),
              modifier = Modifier.fillMaxWidth()
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
                  text = recoveryError ?: "",
                  style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.5.sp,
                    lineHeight = 15.sp
                  ),
                  color = Color(0xFFC62828)
                )
              }
            }
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
