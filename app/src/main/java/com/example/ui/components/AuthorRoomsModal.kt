package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.local.AuthorSlotEntity
import com.example.data.local.ReaderProfileEntity
import com.example.model.NovelWithState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.widthIn
import android.widget.Toast
import com.example.ui.theme.AntiqueGoldLight
import com.example.ui.theme.AntiqueGold
import com.example.viewmodel.StrawberrycandyViewModel
import com.example.ui.theme.CharcoalSecondary
import com.example.ui.theme.CharcoalTertiary
import com.example.ui.theme.CharcoalText
import com.example.ui.theme.SoftCreamPaper
import com.example.ui.theme.SubtleBorder
import java.io.File
import java.io.FileOutputStream

enum class UserRoleView {
  READER,
  ADMIN_OWNER,
  TRANSLATOR
}

@Composable
fun AuthorRoomsModal(
  authorSlots: List<AuthorSlotEntity>,
  novels: List<NovelWithState>,
  currentUser: ReaderProfileEntity? = null,
  onDismiss: () -> Unit,
  onOpenUploadForSlot: (Int) -> Unit,
  onUpdateSlot: (slotNumber: Int, name: String, penName: String, bio: String) -> Unit,
  onUpdateSlotCover: (slotNumber: Int, imagePath: String) -> Unit,
  onToggleSlotPermission: (slotNumber: Int, isGranted: Boolean) -> Unit,
  onViewTranslatorArchive: (AuthorSlotEntity) -> Unit,
  onGrantPermissionByEmail: ((email: String, slotNumber: Int?) -> Unit)? = null,
  onUpdateSlotByOwner: ((slotNumber: Int, translatorEmail: String?, penName: String, bio: String, isPermissionGranted: Boolean) -> Unit)? = null,
  onOpenAuth: (() -> Unit)? = null,
) {
  val context = LocalContext.current
  val isOwnerUser = currentUser != null && StrawberrycandyViewModel.isOwnerEmail(currentUser.email)
  val isTranslatorUser = currentUser?.role == "TRANSLATOR" || (currentUser?.authorSlot != null && currentUser.authorSlot > 0)
  val isReaderUser = currentUser == null || (!isOwnerUser && !isTranslatorUser)

  var activeRoleView by remember(currentUser) {
    mutableStateOf(
      if (isOwnerUser) UserRoleView.ADMIN_OWNER
      else if (isTranslatorUser) UserRoleView.TRANSLATOR
      else UserRoleView.READER
    )
  }
  var editingSlotNumber by remember { mutableIntStateOf(-1) }
  var editPenName by remember { mutableStateOf("") }
  var editBio by remember { mutableStateOf("") }
  var editEmail by remember { mutableStateOf("") }
  var editPermissionGranted by remember { mutableStateOf(false) }
  var targetPhotoSlot by remember { mutableIntStateOf(-1) }
  var isGrantByGmailDialogOpen by remember { mutableStateOf(false) }
  var inputGrantEmail by remember { mutableStateOf("") }
  var selectedGrantSlot by remember { mutableIntStateOf(1) }
  var grantEmailError by remember { mutableStateOf<String?>(null) }

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    val slotNum = targetPhotoSlot
    if (uri != null && slotNum >= 0) {
      try {
        val coversDir = File(context.filesDir, "translator_covers").apply { mkdirs() }
        val destFile = File(coversDir, "translator_${slotNum}_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
          FileOutputStream(destFile).use { output ->
            input.copyTo(output)
          }
        }
        onUpdateSlotCover(slotNum, destFile.absolutePath)
      } catch (e: Exception) {
        onUpdateSlotCover(slotNum, uri.toString())
      }
    }
    targetPhotoSlot = -1
  }

  // Ensure slot 0 (strawberrycandy) exists
  val ownerSlot = authorSlots.find { it.slotNumber == 0 } ?: AuthorSlotEntity(
    slotNumber = 0,
    authorName = "strawberrycandy",
    penName = "strawberrycandy",
    bio = "Founder & Curator at Strawberrycandy Archive. Permanent editorial administrator.",
    avatarColorHex = 0xFF8C2D48,
    accessCode = "ARCHIVE-OWNER-0",
    isClaimed = true,
    isPermissionGranted = true
  )

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Card(
      shape = RoundedCornerShape(26.dp),
      colors = CardDefaults.cardColors(containerColor = SoftCreamPaper),
      border = BorderStroke(1.dp, SubtleBorder),
      elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 20.dp)
        .testTag("author_rooms_modal")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(20.dp)
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Outlined.WorkspacePremium,
              contentDescription = null,
              tint = AntiqueGold,
              modifier = Modifier.size(19.dp)
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text(
              text = "TRANSLATOR ARCHIVE",
              style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 1.8.sp,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
              ),
              color = AntiqueGold
            )
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(28.dp)
          ) {
            Icon(
              imageVector = Icons.Outlined.Close,
              contentDescription = "Close",
              tint = CharcoalSecondary,
              modifier = Modifier.size(18.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val permittedTranslators = remember(authorSlots) {
          (1..10).mapNotNull { slotNum ->
            val slot = authorSlots.find { it.slotNumber == slotNum }
            if (slot != null && slot.isPermissionGranted) slot else null
          }
        }
        val totalCurators = permittedTranslators.size + 1 // +1 for strawberrycandy (Founder)

        Text(
          text = if (activeRoleView == UserRoleView.READER) {
            "Curatorial Collective of $totalCurators"
          } else {
            "Translator Administration (10 Slots)"
          },
          style = MaterialTheme.typography.headlineSmall.copy(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold
          ),
          color = CharcoalText
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = if (activeRoleView == UserRoleView.READER) {
            "An exclusive archive featuring $totalCurators curators: strawberrycandy (Founder) and ${permittedTranslators.size} permitted translators who publish translated manuscripts."
          } else {
            "Founder (strawberrycandy) plus 10 configurable translator slots. Readers currently view the $totalCurators active curators."
          },
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
          color = CharcoalSecondary
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Role View Mode Switcher (ONLY shown if Owner, completely HIDDEN for readers and translators)
        if (isOwnerUser) {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = AntiqueGold.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "View Mode:",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 10.5.sp
                ),
                color = AntiqueGold
              )

              Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                RoleChip(
                  label = "Owner (Admin)",
                  selected = activeRoleView == UserRoleView.ADMIN_OWNER,
                  onClick = { activeRoleView = UserRoleView.ADMIN_OWNER }
                )
                RoleChip(
                  label = "Reader Mode",
                  selected = activeRoleView == UserRoleView.READER,
                  onClick = { activeRoleView = UserRoleView.READER }
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(16.dp))
        }

        // Guest / Reader Guidance: Inform users how to manage permissions if they are the owner
        if (currentUser == null) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = AntiqueGold.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth().testTag("author_rooms_guest_banner")
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Outlined.AdminPanelSettings,
                  contentDescription = null,
                  tint = AntiqueGold,
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Viewing in Reader Mode. Archive Owner: Sign in with clarifymanga@gmail.com to assign translator Gmails, manage slots, and upload novels.",
                  style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 15.sp),
                  color = CharcoalText
                )
              }
              if (onOpenAuth != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                  onClick = {
                    onDismiss()
                    onOpenAuth()
                  },
                  shape = RoundedCornerShape(10.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = AntiqueGold),
                  contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                  modifier = Modifier.height(34.dp).testTag("author_rooms_guest_sign_in_button")
                ) {
                  Icon(Icons.Outlined.Lock, contentDescription = null, tint = SoftCreamPaper, modifier = Modifier.size(13.dp))
                  Spacer(modifier = Modifier.width(5.dp))
                  Text("Sign In as Writer / Owner", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SoftCreamPaper)
                }
              }
            }
          }
          Spacer(modifier = Modifier.height(14.dp))
        }

        // Dedicated Primary Upload Novel Action Button in Writer's Room
        if (isOwnerUser || isTranslatorUser) {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = AntiqueGold,
            shadowElevation = 2.dp,
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                val targetSlot = if (isOwnerUser) 0 else (currentUser?.authorSlot ?: 1)
                onDismiss()
                onOpenUploadForSlot(targetSlot)
              }
              .testTag("author_rooms_primary_upload_button")
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center
            ) {
              Icon(
                imageVector = Icons.Outlined.Upload,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Upload Novel Manuscript",
                style = MaterialTheme.typography.labelLarge.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.5.sp
                ),
                color = Color.White
              )
            }
          }
          Spacer(modifier = Modifier.height(14.dp))
        }

        // OWNER DIRECT GRANT PANEL: Unmissable at the top of Writer's Rooms
        if (isOwnerUser && onGrantPermissionByEmail != null) {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = AntiqueGoldLight.copy(alpha = 0.55f),
            border = BorderStroke(1.2.dp, AntiqueGold),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("owner_grant_by_gmail_panel")
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Outlined.PersonAdd,
                    contentDescription = null,
                    tint = AntiqueGold,
                    modifier = Modifier.size(18.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "GRANT TRANSLATOR RIGHTS BY GMAIL",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontSize = 10.5.sp,
                      fontWeight = FontWeight.ExtraBold,
                      letterSpacing = 1.sp
                    ),
                    color = AntiqueGold
                  )
                }

                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = AntiqueGold.copy(alpha = 0.2f)
                ) {
                  Text(
                    text = "OWNER CONTROL",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontSize = 8.sp,
                      fontWeight = FontWeight.Bold,
                      color = AntiqueGold
                    ),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }

              Spacer(modifier = Modifier.height(6.dp))

              Text(
                text = "Enter a translator's Gmail below to grant them publishing permissions and assign their Writer's Room seat:",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, lineHeight = 15.sp),
                color = CharcoalSecondary
              )

              Spacer(modifier = Modifier.height(10.dp))

              OutlinedTextField(
                value = inputGrantEmail,
                onValueChange = {
                  inputGrantEmail = it
                  grantEmailError = null
                },
                placeholder = { Text("e.g. translator@gmail.com", fontSize = 12.sp, color = CharcoalTertiary) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Outlined.Mail,
                    contentDescription = null,
                    tint = AntiqueGold,
                    modifier = Modifier.size(17.dp)
                  )
                },
                trailingIcon = {
                  if (inputGrantEmail.isNotBlank()) {
                    IconButton(onClick = { inputGrantEmail = "" }) {
                      Icon(Icons.Outlined.Close, contentDescription = "Clear", tint = CharcoalTertiary, modifier = Modifier.size(15.dp))
                    }
                  }
                },
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
                  .testTag("grant_translator_gmail_field")
              )

              if (grantEmailError != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = grantEmailError!!,
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = Color(0xFFC62828))
                )
              }

              Spacer(modifier = Modifier.height(10.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                // Room selector
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = "Seat:",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    color = CharcoalSecondary
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.widthIn(max = 180.dp)
                  ) {
                    items(10) { index ->
                      val slotNum = index + 1
                      val isSelected = selectedGrantSlot == slotNum
                      Surface(
                        onClick = { selectedGrantSlot = slotNum },
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSelected) AntiqueGold else SoftCreamPaper,
                        border = BorderStroke(1.dp, if (isSelected) AntiqueGold else SubtleBorder)
                      ) {
                        Text(
                          text = "#$slotNum",
                          fontSize = 10.sp,
                          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                          color = if (isSelected) SoftCreamPaper else CharcoalText,
                          modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                      }
                    }
                  }
                }

                Button(
                  onClick = {
                    val clean = inputGrantEmail.trim().lowercase()
                    if (clean.isBlank()) {
                      grantEmailError = "Please enter a Gmail address"
                      return@Button
                    }
                    if (!clean.contains("@")) {
                      grantEmailError = "Enter a valid Gmail address"
                      return@Button
                    }
                    onGrantPermissionByEmail(clean, selectedGrantSlot)
                    Toast.makeText(context, "Granted permission to $clean in Room #$selectedGrantSlot!", Toast.LENGTH_LONG).show()
                    inputGrantEmail = ""
                    grantEmailError = null
                  },
                  shape = RoundedCornerShape(10.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = AntiqueGold),
                  contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                  modifier = Modifier
                    .height(36.dp)
                    .testTag("submit_grant_translator_button")
                ) {
                  Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = SoftCreamPaper, modifier = Modifier.size(13.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Grant Access", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SoftCreamPaper)
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(16.dp))
        }

        // 1. OWNER / FOUNDER ARCHIVE PROFILE (Strawberrycandy - Slot 0)
        val ownerNovels = novels.filter { it.authorSlot == 0 }
        TranslatorCardItem(
          slot = ownerSlot,
          worksCount = ownerNovels.size,
          roleView = activeRoleView,
          isOwner = true,
          isOwnerUser = isOwnerUser,
          isTranslatorUser = isTranslatorUser,
          isReaderUser = isReaderUser,
          isMyOwnRoom = isOwnerUser,
          currentUserPoints = currentUser?.penNamePoints ?: 0,
          isEditing = editingSlotNumber == 0,
          editPenName = editPenName,
          editBio = editBio,
          onEditStart = {
            editingSlotNumber = 0
            editPenName = ownerSlot.penName
            editBio = ownerSlot.bio
          },
          onEditCancel = { editingSlotNumber = -1 },
          onEditSave = { penName, bio ->
            onUpdateSlot(0, penName, penName, bio)
            editingSlotNumber = -1
          },
          onPenNameChange = { editPenName = it },
          onBioChange = { editBio = it },
          onPickPhoto = {
            targetPhotoSlot = 0
            photoPickerLauncher.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
          },
          onTogglePermission = { /* Slot 0 is always permitted */ },
          onOpenUpload = {
            onDismiss()
            onOpenUploadForSlot(0)
          },
          onViewArchive = {
            onDismiss()
            onViewTranslatorArchive(ownerSlot)
          }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = if (activeRoleView == UserRoleView.READER) {
              "PERMITTED TRANSLATORS (${permittedTranslators.size})"
            } else {
              "ALL TRANSLATOR SLOTS (${permittedTranslators.size} / 10 ACTIVE)"
            },
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 9.5.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.2.sp
            ),
            color = CharcoalTertiary
          )

          if (isOwnerUser && onGrantPermissionByEmail != null) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = AntiqueGold.copy(alpha = 0.12f),
              border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.4f)),
              modifier = Modifier
                .clickable { isGrantByGmailDialogOpen = true }
                .testTag("author_rooms_grant_gmail_button")
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Outlined.PersonAdd,
                  contentDescription = null,
                  tint = AntiqueGold,
                  modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "Grant by Gmail",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = AntiqueGold
                  )
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val slotsToDisplay = if (activeRoleView == UserRoleView.READER) {
          permittedTranslators
        } else {
          (1..10).map { slotNum ->
            authorSlots.find { it.slotNumber == slotNum } ?: AuthorSlotEntity(
              slotNumber = slotNum,
              authorName = "",
              penName = "",
              bio = "",
              avatarColorHex = 0xFF353C48,
              accessCode = "AUTH-ROOM-$slotNum",
              isClaimed = false,
              isPermissionGranted = false,
              translatorEmail = null
            )
          }
        }

        slotsToDisplay.forEach { slot ->
          val slotNum = slot.slotNumber
          val slotNovels = novels.filter { it.authorSlot == slotNum }
          val isEditingThis = editingSlotNumber == slotNum
          val isMyOwnSlot = isOwnerUser || (isTranslatorUser && (
            currentUser?.authorSlot == slotNum ||
            (currentUser?.email != null && slot.translatorEmail?.equals(currentUser.email, ignoreCase = true) == true)
          ))

          TranslatorCardItem(
            slot = slot,
            worksCount = slotNovels.size,
            roleView = activeRoleView,
            isOwner = false,
            isOwnerUser = isOwnerUser,
            isTranslatorUser = isTranslatorUser,
            isReaderUser = isReaderUser,
            isMyOwnRoom = isMyOwnSlot,
            currentUserPoints = currentUser?.penNamePoints ?: 0,
            isEditing = isEditingThis,
            editPenName = editPenName,
            editBio = editBio,
            editEmail = editEmail,
            onEmailChange = { editEmail = it },
            editPermissionGranted = editPermissionGranted,
            onPermissionChange = { editPermissionGranted = it },
            onEditStart = {
              editingSlotNumber = slotNum
              editPenName = if (slot.penName.startsWith("Translator ", ignoreCase = true) || slot.penName.startsWith("Author ", ignoreCase = true)) "" else slot.penName
              editBio = if (slot.bio.startsWith("Contributing translator")) "" else slot.bio
              editEmail = slot.translatorEmail ?: ""
              editPermissionGranted = slot.isPermissionGranted
            },
            onEditCancel = { editingSlotNumber = -1 },
            onEditSave = { penName, bio ->
              onUpdateSlot(slotNum, penName, penName, bio)
              editingSlotNumber = -1
            },
            onOwnerSave = { email, penName, bio, isGranted ->
              onUpdateSlotByOwner?.invoke(slotNum, email.ifBlank { null }, penName, bio, isGranted)
              editingSlotNumber = -1
            },
            onPenNameChange = { editPenName = it },
            onBioChange = { editBio = it },
            onPickPhoto = {
              targetPhotoSlot = slotNum
              photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
            onTogglePermission = { isGranted ->
              onToggleSlotPermission(slotNum, isGranted)
            },
            onOpenUpload = {
              onDismiss()
              onOpenUploadForSlot(slotNum)
            },
            onViewArchive = {
              onDismiss()
              onViewTranslatorArchive(slot)
            }
          )

          Spacer(modifier = Modifier.height(10.dp))
        }
      }
    }
  }

  // Owner Dialog: Grant Permission Directly by Gmail
  if (isGrantByGmailDialogOpen && isOwnerUser) {
    AlertDialog(
      onDismissRequest = {
        isGrantByGmailDialogOpen = false
        grantEmailError = null
        inputGrantEmail = ""
      },
      icon = {
        Icon(
          imageVector = Icons.Outlined.PersonAdd,
          contentDescription = null,
          tint = AntiqueGold,
          modifier = Modifier.size(28.dp)
        )
      },
      title = {
        Text(
          text = "Grant Permission by Gmail",
          style = MaterialTheme.typography.titleMedium.copy(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold
          ),
          color = CharcoalText,
          textAlign = TextAlign.Center
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "Enter the translator's Gmail to grant them translation rights and assign their archive seat.",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
            color = CharcoalSecondary
          )

          OutlinedTextField(
            value = inputGrantEmail,
            onValueChange = {
              inputGrantEmail = it
              grantEmailError = null
            },
            placeholder = { Text("translator@gmail.com", color = CharcoalTertiary) },
            leadingIcon = {
              Icon(
                imageVector = Icons.Outlined.Mail,
                contentDescription = null,
                tint = AntiqueGold,
                modifier = Modifier.size(18.dp)
              )
            },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = AntiqueGold,
              unfocusedBorderColor = SubtleBorder,
              focusedContainerColor = SoftCreamPaper,
              unfocusedContainerColor = SoftCreamPaper,
              focusedTextColor = CharcoalText,
              unfocusedTextColor = CharcoalText
            ),
            modifier = Modifier.fillMaxWidth().testTag("grant_gmail_input")
          )

          if (inputGrantEmail.isNotBlank() && !inputGrantEmail.contains("@")) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = Color(0xFF4285F4).copy(alpha = 0.12f),
              border = BorderStroke(1.dp, Color(0xFF4285F4).copy(alpha = 0.3f)),
              onClick = {
                inputGrantEmail = "$inputGrantEmail@gmail.com"
                grantEmailError = null
              }
            ) {
              Text(
                text = "+ @gmail.com",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 10.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = Color(0xFF1A73E8)
                ),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }

          if (grantEmailError != null) {
            Text(
              text = grantEmailError ?: "",
              style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                color = Color(0xFFC62828)
              )
            )
          }

          Text(
            text = "ASSIGN TO TRANSLATOR SLOT (1 - 10):",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp,
              color = AntiqueGold
            )
          )

          // Row 1 (Slots 1 to 5)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            (1..5).forEach { slotNum ->
              val isSlotActive = authorSlots.find { it.slotNumber == slotNum }?.isPermissionGranted == true
              val isSelected = selectedGrantSlot == slotNum
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) AntiqueGold else if (isSlotActive) Color(0xFFE8F5E9) else Color.Gray.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, if (isSelected) AntiqueGold else SubtleBorder),
                modifier = Modifier
                  .weight(1f)
                  .clickable { selectedGrantSlot = slotNum }
              ) {
                Box(
                  modifier = Modifier.padding(vertical = 6.dp),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = "#$slotNum",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontSize = 10.5.sp,
                      fontWeight = FontWeight.Bold,
                      color = if (isSelected) SoftCreamPaper else if (isSlotActive) Color(0xFF2E7D32) else CharcoalText
                    )
                  )
                }
              }
            }
          }

          // Row 2 (Slots 6 to 10)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            (6..10).forEach { slotNum ->
              val isSlotActive = authorSlots.find { it.slotNumber == slotNum }?.isPermissionGranted == true
              val isSelected = selectedGrantSlot == slotNum
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) AntiqueGold else if (isSlotActive) Color(0xFFE8F5E9) else Color.Gray.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, if (isSelected) AntiqueGold else SubtleBorder),
                modifier = Modifier
                  .weight(1f)
                  .clickable { selectedGrantSlot = slotNum }
              ) {
                Box(
                  modifier = Modifier.padding(vertical = 6.dp),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = "#$slotNum",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontSize = 10.5.sp,
                      fontWeight = FontWeight.Bold,
                      color = if (isSelected) SoftCreamPaper else if (isSlotActive) Color(0xFF2E7D32) else CharcoalText
                    )
                  )
                }
              }
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            var email = inputGrantEmail.trim()
            if (!email.contains("@")) {
              email = "$email@gmail.com"
              inputGrantEmail = email
            }
            if (email.isBlank() || !email.contains(".")) {
              grantEmailError = "Please enter a valid Gmail address (e.g. user@gmail.com)."
              return@Button
            }
            onGrantPermissionByEmail?.invoke(email, selectedGrantSlot)
            isGrantByGmailDialogOpen = false
            inputGrantEmail = ""
          },
          colors = ButtonDefaults.buttonColors(containerColor = AntiqueGold),
          shape = RoundedCornerShape(10.dp)
        ) {
          Text("Grant Permission", color = SoftCreamPaper, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(
          onClick = {
            isGrantByGmailDialogOpen = false
            inputGrantEmail = ""
            grantEmailError = null
          }
        ) {
          Text("Cancel", color = CharcoalSecondary)
        }
      }
    )
  }
}

@Composable
private fun RoleChip(
  label: String,
  selected: Boolean,
  onClick: () -> Unit
) {
  FilterChip(
    selected = selected,
    onClick = onClick,
    label = {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(
          fontSize = 10.sp,
          fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
      )
    },
    colors = FilterChipDefaults.filterChipColors(
      selectedContainerColor = CharcoalText,
      selectedLabelColor = SoftCreamPaper,
      containerColor = SoftCreamPaper,
      labelColor = CharcoalSecondary
    ),
    border = BorderStroke(1.dp, if (selected) CharcoalText else SubtleBorder),
    shape = RoundedCornerShape(8.dp)
  )
}

/**
 * Individual Translator Archive Card in the Collective Archive Modal.
 */
@Composable
private fun TranslatorCardItem(
  slot: AuthorSlotEntity,
  worksCount: Int,
  roleView: UserRoleView,
  isOwner: Boolean,
  isOwnerUser: Boolean = false,
  isTranslatorUser: Boolean = false,
  isReaderUser: Boolean = false,
  isMyOwnRoom: Boolean = false,
  currentUserPoints: Int = 0,
  isEditing: Boolean,
  editPenName: String,
  editBio: String,
  editEmail: String = "",
  onEmailChange: (String) -> Unit = {},
  editPermissionGranted: Boolean = false,
  onPermissionChange: (Boolean) -> Unit = {},
  onEditStart: () -> Unit,
  onEditCancel: () -> Unit,
  onEditSave: (String, String) -> Unit,
  onOwnerSave: ((String, String, String, Boolean) -> Unit)? = null,
  onPenNameChange: (String) -> Unit,
  onBioChange: (String) -> Unit,
  onPickPhoto: () -> Unit,
  onTogglePermission: (Boolean) -> Unit,
  onOpenUpload: () -> Unit,
  onViewArchive: () -> Unit,
) {
  val isReader = roleView == UserRoleView.READER || isReaderUser
  val canManage = !isReader && !isReaderUser && (isOwnerUser || (isTranslatorUser && isMyOwnRoom && (slot.isPermissionGranted || isOwner)))

  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (isOwner) AntiqueGold.copy(alpha = 0.07f) else SoftCreamPaper
    ),
    border = BorderStroke(
      1.dp,
      if (isOwner) AntiqueGold.copy(alpha = 0.4f) else SubtleBorder
    ),
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onViewArchive() }
      .testTag("translator_card_slot_${slot.slotNumber}")
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Customizable Profile Picture
        Box(
          modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(slot.avatarColorHex))
            .clickable(enabled = canManage) { onPickPhoto() },
          contentAlignment = Alignment.Center
        ) {
          if (slot.coverImageUri != null) {
            AsyncImage(
              model = File(slot.coverImageUri).takeIf { it.exists() } ?: slot.coverImageUri,
              contentDescription = slot.penName,
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize()
            )
          } else {
            Text(
              text = if (slot.penName.isNotBlank()) slot.penName.take(2).uppercase() else "#${slot.slotNumber}",
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = SoftCreamPaper,
                fontFamily = FontFamily.Serif
              )
            )
          }

          if (canManage) {
            Box(
              modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(18.dp)
                .background(CharcoalText.copy(alpha = 0.8f), RoundedCornerShape(topStart = 6.dp)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Outlined.AddPhotoAlternate,
                contentDescription = "Upload Picture",
                tint = AntiqueGold,
                modifier = Modifier.size(10.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text(
              text = if (isOwner) "FOUNDER" else "SLOT ${slot.slotNumber}",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
              ),
              color = AntiqueGold
            )

            if (isOwner) {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = AntiqueGold.copy(alpha = 0.15f)
              ) {
                Text(
                  text = "PERMANENT ACCESS",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = AntiqueGold
                  ),
                  modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
              }
            } else if (slot.isPermissionGranted) {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF2E7D32).copy(alpha = 0.15f)
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                  Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(9.dp)
                  )
                  Spacer(modifier = Modifier.width(2.dp))
                  Text(
                    text = "GRANTED",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontSize = 7.5.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color(0xFF2E7D32)
                    )
                  )
                }
              }
            } else {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color.Gray.copy(alpha = 0.15f)
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = CharcoalTertiary,
                    modifier = Modifier.size(9.dp)
                  )
                  Spacer(modifier = Modifier.width(2.dp))
                  Text(
                    text = "LOCKED",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontSize = 7.5.sp,
                      fontWeight = FontWeight.Bold,
                      color = CharcoalTertiary
                    )
                  )
                }
              }
            }
          }

          val emailRegex = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
          val rawPenName = slot.penName.replace(emailRegex, "").trim()
          val safePenName = if (rawPenName.isNotBlank()) {
            rawPenName
          } else if (isOwner) {
            "Strawberrycandy"
          } else {
            "Curator Seat #${slot.slotNumber}"
          }

          Text(
            text = safePenName,
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Serif
            ),
            color = if (rawPenName.isNotBlank() || isOwner) CharcoalText else CharcoalSecondary
          )

          if (isOwnerUser && !isOwner) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(top = 2.dp)
            ) {
              Icon(
                imageVector = Icons.Outlined.Mail,
                contentDescription = null,
                tint = if (slot.translatorEmail != null) AntiqueGold else Color(0xFFC62828),
                modifier = Modifier.size(12.dp)
              )
              Spacer(modifier = Modifier.width(3.dp))
              Text(
                text = if (slot.translatorEmail.isNullOrBlank()) "Gmail: Not assigned" else "Gmail: ${slot.translatorEmail}",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 10.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (slot.translatorEmail.isNullOrBlank()) Color(0xFFC62828) else CharcoalText
                )
              )
              if (slot.translatorEmail.isNullOrBlank() && !isEditing) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "[Assign]",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = AntiqueGold
                  ),
                  modifier = Modifier.clickable { onEditStart() }
                )
              }
            }
          }

          Text(
            text = "$worksCount manuscripts in archive",
            style = MaterialTheme.typography.bodySmall.copy(
              fontSize = 10.5.sp,
              color = CharcoalSecondary
            )
          )
        }

        // Action Buttons
        Row(verticalAlignment = Alignment.CenterVertically) {
          if (canManage) {
            IconButton(
              onClick = {
                if (isEditing) onEditCancel() else onEditStart()
              },
              modifier = Modifier.size(28.dp)
            ) {
              Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = "Edit Profile",
                tint = CharcoalSecondary,
                modifier = Modifier.size(15.dp)
              )
            }
          }

          // Primary "View All Works" button
          Button(
            onClick = onViewArchive,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CharcoalText),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            modifier = Modifier.height(30.dp)
          ) {
            Text(
              text = "Works ($worksCount)",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.5.sp,
                color = SoftCreamPaper,
                fontWeight = FontWeight.SemiBold
              )
            )
            Spacer(modifier = Modifier.width(3.dp))
            Icon(
              imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
              contentDescription = null,
              tint = AntiqueGold,
              modifier = Modifier.size(11.dp)
            )
          }
        }
      }

      // Inline Editing Form
      if (isEditing) {
        val emailRegex = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
        val isChangingPenName = editPenName.replace(emailRegex, "").trim() != slot.penName.replace(emailRegex, "").trim()
        val hasPointForRename = currentUserPoints >= 1 || isOwnerUser

        Spacer(modifier = Modifier.height(10.dp))

        if (isOwnerUser && !isOwner) {
          OutlinedTextField(
            value = editEmail,
            onValueChange = onEmailChange,
            label = { Text("Translator Gmail (Owner Grant)") },
            placeholder = { Text("e.g. translator@gmail.com") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = AntiqueGold,
              unfocusedBorderColor = SubtleBorder
            ),
            modifier = Modifier.fillMaxWidth()
          )
          Spacer(modifier = Modifier.height(6.dp))

          // Owner Permission Toggle
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (editPermissionGranted) Color(0xFFE8F5E9) else Color(0xFFFDEDEC),
            border = BorderStroke(1.dp, if (editPermissionGranted) Color(0xFFA5D6A7) else Color(0xFFEF9A9A)),
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onPermissionChange(!editPermissionGranted) }
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = if (editPermissionGranted) Icons.Outlined.CheckCircle else Icons.Outlined.Lock,
                  contentDescription = null,
                  tint = if (editPermissionGranted) Color(0xFF2E7D32) else Color(0xFFC62828),
                  modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = if (editPermissionGranted) "Translation Permission: GRANTED" else "Translation Permission: REVOKED",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.5.sp,
                    color = if (editPermissionGranted) Color(0xFF2E7D32) else Color(0xFFC62828)
                  )
                )
              }
              Text(
                text = if (editPermissionGranted) "Revoke" else "Grant",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (editPermissionGranted) Color(0xFFC62828) else Color(0xFF2E7D32)
                )
              )
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
        }

        OutlinedTextField(
          value = editPenName,
          onValueChange = onPenNameChange,
          label = { Text("Translator Pen Name") },
          placeholder = { Text("Choose your pen name...") },
          singleLine = true,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AntiqueGold,
            unfocusedBorderColor = SubtleBorder
          ),
          modifier = Modifier.fillMaxWidth()
        )

        if (isChangingPenName && !hasPointForRename) {
          Spacer(modifier = Modifier.height(4.dp))
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFFDEDEC),
            border = BorderStroke(1.dp, Color(0xFFF5C6CB)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = "🔒 Changing your pen name requires 1 point. Finish a novel to earn 1 point! (Balance: $currentUserPoints pts)",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, color = Color(0xFFC62828)),
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            )
          }
        } else if (isChangingPenName && hasPointForRename) {
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "⭐ Saving this pen name will consume 1 point (Balance: $currentUserPoints pts)",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, color = AntiqueGold),
            modifier = Modifier.padding(start = 4.dp)
          )
        }

        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
          value = editBio,
          onValueChange = onBioChange,
          label = { Text("Bio & Credentials") },
          singleLine = true,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AntiqueGold,
            unfocusedBorderColor = SubtleBorder
          ),
          modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          OutlinedButton(
            onClick = onEditCancel,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.height(32.dp)
          ) {
            Text("Cancel", fontSize = 11.sp)
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = {
              if (isOwnerUser && !isOwner) {
                onOwnerSave?.invoke(editEmail, editPenName, editBio, editPermissionGranted)
              } else {
                onEditSave(editPenName, editBio)
              }
            },
            enabled = !isChangingPenName || hasPointForRename,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = CharcoalText,
              disabledContainerColor = CharcoalText.copy(alpha = 0.3f)
            ),
            modifier = Modifier.height(32.dp)
          ) {
            Text(
              text = if (isOwnerUser && !isOwner) "Save Seat Details" else if (isChangingPenName && !isOwnerUser) "Save (Cost: 1 Pt)" else "Save Profile",
              fontSize = 11.sp
            )
          }
        }
      } else {
        Spacer(modifier = Modifier.height(6.dp))
        val emailRegex = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
        val safeBio = slot.bio.replace(emailRegex, "").trim()
        Text(
          text = safeBio,
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 15.sp),
          color = CharcoalSecondary
        )
      }

      // Owner Controls: Dedicated, Unmissable Permission Toggle Switch & Actions
      if (!isOwner && isOwnerUser) {
        Spacer(modifier = Modifier.height(10.dp))
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = if (slot.isPermissionGranted) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
          border = BorderStroke(1.dp, if (slot.isPermissionGranted) Color(0xFFA5D6A7) else Color(0xFFFFCC80)),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("owner_permission_strip_${slot.slotNumber}")
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = if (slot.isPermissionGranted) Icons.Outlined.CheckCircle else Icons.Outlined.Lock,
                  contentDescription = null,
                  tint = if (slot.isPermissionGranted) Color(0xFF2E7D32) else Color(0xFFE65100),
                  modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                  text = if (slot.isPermissionGranted) "TRANSLATOR PERMISSION: GRANTED" else "TRANSLATOR PERMISSION: REVOKED",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.5.sp,
                    color = if (slot.isPermissionGranted) Color(0xFF2E7D32) else Color(0xFFE65100)
                  )
                )
              }
              Text(
                text = if (slot.isPermissionGranted) "Can publish novel & edit manuscripts" else "Locked: Translator cannot upload or edit",
                style = MaterialTheme.typography.bodySmall.copy(
                  fontSize = 9.5.sp,
                  color = CharcoalSecondary
                )
              )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = if (slot.isPermissionGranted) "ON" else "OFF",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 11.sp,
                  color = if (slot.isPermissionGranted) Color(0xFF2E7D32) else CharcoalTertiary
                ),
                modifier = Modifier.padding(end = 6.dp)
              )
              // Standard-sized, unclipped Material 3 switch
              Switch(
                checked = slot.isPermissionGranted,
                onCheckedChange = { isChecked ->
                  onTogglePermission(isChecked)
                },
                colors = SwitchDefaults.colors(
                  checkedThumbColor = SoftCreamPaper,
                  checkedTrackColor = Color(0xFF2E7D32),
                  uncheckedThumbColor = CharcoalSecondary,
                  uncheckedTrackColor = SubtleBorder
                ),
                modifier = Modifier.testTag("slot_permission_toggle_${slot.slotNumber}")
              )
            }
          }
        }

        // Upload Novel button if owner wants to upload directly into this slot
        if (slot.isPermissionGranted) {
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
          ) {
            OutlinedButton(
              onClick = onOpenUpload,
              shape = RoundedCornerShape(8.dp),
              border = BorderStroke(1.dp, SubtleBorder),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
              modifier = Modifier.height(30.dp)
            ) {
              Icon(
                imageVector = Icons.Outlined.Upload,
                contentDescription = null,
                tint = AntiqueGold,
                modifier = Modifier.size(13.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text("Upload Novel to Room #${slot.slotNumber}", fontSize = 10.sp, color = CharcoalText)
            }
          }
        }
      } else if (canManage && !isOwner && isTranslatorUser && slot.isPermissionGranted) {
        // Permitted translator can upload manuscript
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          OutlinedButton(
            onClick = onOpenUpload,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, SubtleBorder),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            modifier = Modifier.height(28.dp)
          ) {
            Icon(
              imageVector = Icons.Outlined.Upload,
              contentDescription = null,
              tint = AntiqueGold,
              modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text("Upload Novel", fontSize = 10.sp, color = CharcoalText)
          }
        }
      } else if (canManage && isOwner && (isOwnerUser || isTranslatorUser)) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          OutlinedButton(
            onClick = onOpenUpload,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, SubtleBorder),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            modifier = Modifier.height(28.dp)
          ) {
            Icon(
              imageVector = Icons.Outlined.Upload,
              contentDescription = null,
              tint = AntiqueGold,
              modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text("Upload Novel", fontSize = 10.sp, color = CharcoalText)
          }
        }
      }
    }
  }
}
