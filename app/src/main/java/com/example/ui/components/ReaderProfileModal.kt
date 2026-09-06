package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.ReaderProfileEntity
import com.example.ui.theme.AntiqueGold
import com.example.ui.theme.AntiqueGoldLight
import com.example.ui.theme.CharcoalSecondary
import com.example.ui.theme.CharcoalTertiary
import com.example.ui.theme.CharcoalText
import com.example.ui.theme.SoftCreamPaper
import com.example.ui.theme.SubtleBorder

@Composable
fun ReaderProfileModal(
  activeUser: ReaderProfileEntity,
  readingCount: Int,
  finishedCount: Int,
  toBeReadCount: Int,
  isSoleOwner: Boolean,
  onDismiss: () -> Unit,
  onChangePenName: (String) -> Unit,
  onSwitchAccount: () -> Unit,
  onSignOut: () -> Unit,
) {
  val emailRegex = remember { Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}") }
  val currentSafePenName = remember(activeUser.displayName) {
    activeUser.displayName.replace(emailRegex, "").substringBefore("@").trim().ifBlank {
      if (activeUser.role == "TRANSLATOR") "Translator" else "Reader"
    }
  }

  var newPenName by remember { mutableStateOf(currentSafePenName) }
  val hasEnoughPoints = activeUser.penNamePoints >= 1 || isSoleOwner
  val isNameChanged = newPenName.trim().isNotBlank() && newPenName.trim() != currentSafePenName

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
        .testTag("reader_profile_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(24.dp),
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
                .size(8.dp)
                .clip(CircleShape)
                .background(AntiqueGold)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "ARCHIVE PASSPORT & PEN NAME",
              style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 1.4.sp,
                fontWeight = FontWeight.Bold
              ),
              color = AntiqueGold
            )
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier
              .size(28.dp)
              .testTag("profile_close_button")
          ) {
            Icon(
              imageVector = Icons.Outlined.Close,
              contentDescription = "Close dialog",
              tint = CharcoalSecondary,
              modifier = Modifier.size(18.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Avatar & Identity Pill
        Box(
          modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(if (activeUser.provider == "GOOGLE") Color(0xFF4285F4) else Color(0xFF1E1D1B)),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = activeUser.avatarInitial.ifBlank { "R" },
            style = MaterialTheme.typography.headlineMedium.copy(
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = currentSafePenName,
          style = MaterialTheme.typography.titleLarge.copy(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold
          ),
          color = CharcoalText,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Role Badge
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = if (isSoleOwner) AntiqueGold.copy(alpha = 0.15f) else CharcoalText.copy(alpha = 0.08f),
          border = BorderStroke(0.8.dp, if (isSoleOwner) AntiqueGold else SubtleBorder)
        ) {
          Text(
            text = if (isSoleOwner) "ARCHIVE OWNER • CLARIFY" else if (activeUser.role == "TRANSLATOR") "CONTRIBUTING TRANSLATOR • ROOM ${activeUser.authorSlot ?: 1}" else "LITERARY ARCHIVE READER",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp
            ),
            color = if (isSoleOwner) AntiqueGold else CharcoalText,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
          )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // POINT SYSTEM CARD (Highlighting point balance and finished novel reward)
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = AntiqueGoldLight.copy(alpha = 0.5f),
          border = BorderStroke(1.2.dp, AntiqueGold.copy(alpha = 0.8f)),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("user_point_system_card")
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center
            ) {
              Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = "Points",
                tint = AntiqueGold,
                modifier = Modifier.size(24.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "${activeUser.penNamePoints} Pen Name Point${if (activeUser.penNamePoints != 1) "s" else ""}",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = CharcoalText
                )
              )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
              text = "Earn 1 point per completed novel. 1 point is consumed to change your pen name to preserve archival stability.",
              style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                lineHeight = 16.sp
              ),
              color = CharcoalSecondary,
              textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Reading Stats Row
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceEvenly
            ) {
              // Reading
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Filled.MenuBook,
                    contentDescription = null,
                    tint = AntiqueGold,
                    modifier = Modifier.size(13.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = "$readingCount",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = CharcoalText
                  )
                }
                Text(
                  text = "Reading",
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                  color = CharcoalTertiary
                )
              }

              // Finished
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(13.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = "$finishedCount",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = CharcoalText
                  )
                }
                Text(
                  text = "Finished",
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                  color = CharcoalTertiary
                )
              }

              // To Be Read
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Filled.Bookmark,
                    contentDescription = null,
                    tint = CharcoalSecondary,
                    modifier = Modifier.size(13.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = "$toBeReadCount",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = CharcoalText
                  )
                }
                Text(
                  text = "To Read",
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                  color = CharcoalTertiary
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // PEN NAME MANAGEMENT SECTION
        Column(
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "PEN NAME SETTING",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
              ),
              color = CharcoalTertiary
            )

            if (!hasEnoughPoints) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Filled.Lock,
                  contentDescription = "Locked",
                  tint = Color(0xFFC62828),
                  modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "LOCKED (0/1 PT)",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC62828)
                  )
                )
              }
            } else {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Filled.Star,
                  contentDescription = "Available",
                  tint = AntiqueGold,
                  modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "UNLOCKED (COST: 1 PT)",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = AntiqueGold
                  )
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = newPenName,
            onValueChange = {
              val sanitized = it.replace(emailRegex, "").substringBefore("@")
              newPenName = sanitized
            },
            label = { Text("New Pen Name") },
            placeholder = { Text("e.g., Charlotte Sterling") },
            singleLine = true,
            enabled = hasEnoughPoints,
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = AntiqueGold,
              unfocusedBorderColor = SubtleBorder,
              disabledBorderColor = SubtleBorder.copy(alpha = 0.5f),
              disabledLabelColor = CharcoalTertiary.copy(alpha = 0.6f)
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("profile_pen_name_input")
          )

          Spacer(modifier = Modifier.height(8.dp))

          if (!hasEnoughPoints) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = Color(0xFFFDEDEC),
              border = BorderStroke(1.dp, Color(0xFFF5C6CB)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Filled.Lock,
                  contentDescription = null,
                  tint = Color(0xFFC62828),
                  modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "You cannot change your pen name until you finish at least 1 novel to earn a point.",
                  style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    color = Color(0xFFC62828)
                  )
                )
              }
            }
          } else {
            Button(
              onClick = {
                if (isNameChanged) {
                  onChangePenName(newPenName.trim())
                }
              },
              enabled = isNameChanged,
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = CharcoalText,
                disabledContainerColor = CharcoalText.copy(alpha = 0.3f)
              ),
              modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("submit_pen_name_change_button")
            ) {
              Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = null,
                modifier = Modifier.size(15.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = if (isSoleOwner) "Update Owner Pen Name" else "Use 1 Point to Change Pen Name",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 11.5.sp
                )
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Account Switch & Sign Out
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          OutlinedButton(
            onClick = {
              onDismiss()
              onSwitchAccount()
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = CharcoalText),
            border = BorderStroke(1.dp, SubtleBorder),
            modifier = Modifier.weight(1f).testTag("profile_switch_account_button")
          ) {
            Text("Switch Account", fontSize = 11.sp)
          }

          Spacer(modifier = Modifier.width(8.dp))

          OutlinedButton(
            onClick = {
              onDismiss()
              onSignOut()
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828)),
            border = BorderStroke(1.dp, Color(0xFFF5C6CB)),
            modifier = Modifier.weight(1f).testTag("profile_sign_out_button")
          ) {
            Icon(
              imageVector = Icons.Outlined.Logout,
              contentDescription = null,
              tint = Color(0xFFC62828),
              modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Sign Out", fontSize = 11.sp)
          }
        }
      }
    }
  }
}
