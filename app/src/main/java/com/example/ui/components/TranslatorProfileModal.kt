package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WorkspacePremium
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.launch
import com.example.data.local.AuthorSlotEntity
import com.example.model.NovelWithState
import com.example.ui.theme.AntiqueGold
import com.example.ui.theme.DeepBurgundy
import com.example.ui.theme.CharcoalSecondary
import com.example.ui.theme.CharcoalTertiary
import com.example.ui.theme.CharcoalText
import com.example.ui.theme.CreamBackground
import com.example.ui.theme.SoftCreamPaper
import com.example.ui.theme.SubtleBorder
import com.example.util.formatStatCount
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class TranslatorProfileTab {
  WORKS,
  MY_SHELF
}

enum class TranslatorShelfCategory {
  READING,
  TO_BE_READ,
  FINISHED,
  FAVORITES
}

enum class TranslatorWorksSort {
  NEWEST,
  OLDEST,
  POPULAR,
  TITLE_AZ,
  PAGES
}

/**
 * Dedicated Personal Archive Page for a Translator.
 * Displays their custom cover/profile picture, credentials, works count,
 * a subtle search bar, a 'Sort by' filter (e.g. date uploaded),
 * and clean library cards of all manuscripts uploaded by this translator.
 *
 * Readers can strictly view and read the manuscripts.
 */
@Composable
fun TranslatorProfileModal(
  slot: AuthorSlotEntity,
  novels: List<NovelWithState>,
  isOwner: Boolean = false,
  isOwnerOrTranslator: Boolean,
  onDismiss: () -> Unit,
  onSelectNovel: (NovelWithState) -> Unit,
  onUpdateCoverImage: (slotNumber: Int, imagePath: String) -> Unit,
  onOpenUploadForSlot: ((Int) -> Unit)? = null,
  onToggleSlotPermission: ((slotNumber: Int, isGranted: Boolean) -> Unit)? = null,
  onAddChapterToNovel: ((novelId: String, title: String, content: String) -> Unit)? = null,
  onEditNovel: ((NovelWithState) -> Unit)? = null,
) {
  val context = LocalContext.current
  var novelForAddChapter by remember { mutableStateOf<NovelWithState?>(null) }
  var searchQuery by remember { mutableStateOf("") }
  var selectedSort by remember { mutableStateOf(TranslatorWorksSort.NEWEST) }
  var selectedProfileTab by remember { mutableStateOf(TranslatorProfileTab.WORKS) }
  var selectedShelfCategory by remember { mutableStateOf(TranslatorShelfCategory.READING) }

  val readingNovels = remember(novels) { novels.filter { it.inReadingList && !it.isFinished } }
  val tbrNovels = remember(novels) { novels.filter { it.isToBeRead || it.inTbrList } }
  val finishedNovels = remember(novels) { novels.filter { it.isFinished } }
  val favoriteNovels = remember(novels) { novels.filter { it.isFavorite } }

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    if (uri != null) {
      try {
        val coversDir = File(context.filesDir, "translator_covers").apply { mkdirs() }
        val destFile = File(coversDir, "translator_${slot.slotNumber}_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
          FileOutputStream(destFile).use { output ->
            input.copyTo(output)
          }
        }
        onUpdateCoverImage(slot.slotNumber, destFile.absolutePath)
      } catch (e: Exception) {
        onUpdateCoverImage(slot.slotNumber, uri.toString())
      }
    }
  }

  // Filter novels for this translator slot
  val translatorNovels = remember(novels, slot.slotNumber) {
    novels.filter { it.authorSlot == slot.slotNumber }
  }

  // Filter and sort works
  val displayedNovels = remember(translatorNovels, searchQuery, selectedSort) {
    val filtered = if (searchQuery.isBlank()) {
      translatorNovels
    } else {
      val q = searchQuery.trim().lowercase()
      translatorNovels.filter {
        it.title.lowercase().contains(q) ||
          it.subtitle.lowercase().contains(q) ||
          it.excerpt.lowercase().contains(q) ||
          it.chapterTitle.lowercase().contains(q)
      }
    }

    when (selectedSort) {
      TranslatorWorksSort.NEWEST -> filtered.sortedByDescending { n: NovelWithState -> n.createdAt }
      TranslatorWorksSort.OLDEST -> filtered.sortedBy { n: NovelWithState -> n.createdAt }
      TranslatorWorksSort.POPULAR -> filtered.sortedByDescending { n: NovelWithState -> n.readsCount + (n.favoritesCount * 3) }
      TranslatorWorksSort.TITLE_AZ -> filtered.sortedBy { n: NovelWithState -> n.title.lowercase() }
      TranslatorWorksSort.PAGES -> filtered.sortedByDescending { n: NovelWithState -> n.totalPages }
    }
  }

  if (novelForAddChapter != null) {
    AddChapterDialog(
      novel = novelForAddChapter!!,
      onDismiss = { novelForAddChapter = null },
      onAddChapter = { novelId, title, content ->
        onAddChapterToNovel?.invoke(novelId, title, content)
        novelForAddChapter = null
      }
    )
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier
        .fillMaxSize()
        .background(CreamBackground)
        .testTag("translator_profile_screen"),
      color = CreamBackground
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        // Top Navigation Bar
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
              onClick = onDismiss,
              modifier = Modifier
                .size(36.dp)
                .background(SoftCreamPaper, CircleShape)
                .clip(CircleShape)
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = CharcoalText,
                modifier = Modifier.size(18.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = if (slot.slotNumber == 0) "FOUNDER & ARCHIVE CURATOR" else "TRANSLATOR ARCHIVE • SEAT ${slot.slotNumber}",
                style = MaterialTheme.typography.labelSmall.copy(
                  letterSpacing = 1.6.sp,
                  fontWeight = FontWeight.Bold,
                  fontSize = 9.sp
                ),
                color = AntiqueGold
              )
              Text(
                text = slot.penName,
                style = MaterialTheme.typography.titleMedium.copy(
                  fontFamily = FontFamily.Serif,
                  fontWeight = FontWeight.Bold
                ),
                color = CharcoalText
              )
            }
          }

          // If authorized translator or owner, show quick actions
          if (isOwnerOrTranslator) {
            if (isOwner && onToggleSlotPermission != null && slot.slotNumber != 0) {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (slot.isPermissionGranted) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                border = BorderStroke(1.dp, if (slot.isPermissionGranted) Color(0xFFA5D6A7) else Color(0xFFFFCC80)),
                modifier = Modifier.padding(end = 6.dp)
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                  Text(
                    text = if (slot.isPermissionGranted) "Permission: ON" else "Permission: OFF",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      color = if (slot.isPermissionGranted) Color(0xFF2E7D32) else Color(0xFFE65100)
                    )
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Switch(
                    checked = slot.isPermissionGranted,
                    onCheckedChange = { isChecked ->
                      onToggleSlotPermission(slot.slotNumber, isChecked)
                    },
                    colors = SwitchDefaults.colors(
                      checkedThumbColor = SoftCreamPaper,
                      checkedTrackColor = Color(0xFF2E7D32),
                      uncheckedThumbColor = CharcoalSecondary,
                      uncheckedTrackColor = SubtleBorder
                    )
                  )
                }
              }
            } else if (slot.isPermissionGranted && onOpenUploadForSlot != null) {
              Button(
                onClick = {
                  onDismiss()
                  onOpenUploadForSlot(slot.slotNumber)
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CharcoalText),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp)
              ) {
                Icon(
                  imageVector = Icons.Outlined.Upload,
                  contentDescription = null,
                  tint = SoftCreamPaper,
                  modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "Upload Novel",
                  style = MaterialTheme.typography.labelSmall.copy(color = SoftCreamPaper, fontSize = 11.sp)
                )
              }
            }
          }
        }

        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          // Translator Profile Card with Picture
          item {
            Card(
              shape = RoundedCornerShape(22.dp),
              colors = CardDefaults.cardColors(containerColor = SoftCreamPaper),
              border = BorderStroke(1.dp, SubtleBorder),
              elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(18.dp)) {
                val emailRegex = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  // Translator Custom Profile / Cover Picture
                  Box(
                    modifier = Modifier
                      .size(76.dp)
                      .clip(RoundedCornerShape(16.dp))
                      .background(Color(slot.avatarColorHex).copy(alpha = 0.85f))
                      .clickable(enabled = isOwnerOrTranslator) {
                        photoPickerLauncher.launch(
                          PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                      },
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
                      // Classical Monogram
                      Text(
                        text = slot.penName.take(2).uppercase(),
                        style = MaterialTheme.typography.headlineMedium.copy(
                          fontFamily = FontFamily.Serif,
                          fontWeight = FontWeight.Bold,
                          color = SoftCreamPaper
                        )
                      )
                    }

                    // Camera overlay icon if editable
                    if (isOwnerOrTranslator) {
                      Box(
                        modifier = Modifier
                          .align(Alignment.BottomEnd)
                          .size(24.dp)
                          .background(CharcoalText.copy(alpha = 0.85f), RoundedCornerShape(topStart = 8.dp)),
                        contentAlignment = Alignment.Center
                      ) {
                        Icon(
                          imageVector = Icons.Outlined.AddPhotoAlternate,
                          contentDescription = "Upload Picture",
                          tint = AntiqueGold,
                          modifier = Modifier.size(13.dp)
                        )
                      }
                    }
                  }

                  Spacer(modifier = Modifier.width(14.dp))

                  Column(modifier = Modifier.weight(1f)) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                      if (slot.slotNumber == 0) {
                        Surface(
                          shape = RoundedCornerShape(8.dp),
                          color = AntiqueGold.copy(alpha = 0.12f)
                        ) {
                          Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                          ) {
                            Icon(
                              imageVector = Icons.Outlined.WorkspacePremium,
                              contentDescription = null,
                              tint = AntiqueGold,
                              modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                              text = "PERMANENT CURATOR",
                              style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = AntiqueGold
                              )
                            )
                          }
                        }
                      } else if (slot.isPermissionGranted) {
                        Surface(
                          shape = RoundedCornerShape(8.dp),
                          color = Color(0xFF2E7D32).copy(alpha = 0.12f)
                        ) {
                          Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                          ) {
                            Icon(
                              imageVector = Icons.Outlined.CheckCircle,
                              contentDescription = null,
                              tint = Color(0xFF2E7D32),
                              modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                              text = "GRANTED TRANSLATOR",
                              style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                              )
                            )
                          }
                        }
                      } else {
                        Surface(
                          shape = RoundedCornerShape(8.dp),
                          color = Color.Gray.copy(alpha = 0.15f)
                        ) {
                          Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                          ) {
                            Icon(
                              imageVector = Icons.Outlined.Lock,
                              contentDescription = null,
                              tint = CharcoalTertiary,
                              modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                              text = "INVITATION PENDING",
                              style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.sp,
                                color = CharcoalTertiary
                              )
                            )
                          }
                        }
                      }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    val safePenName = slot.penName.replace(emailRegex, "").trim().ifBlank {
                      if (slot.slotNumber == 0) "Strawberrycandy" else "Writer's Room #${slot.slotNumber}"
                    }

                    Text(
                      text = safePenName,
                      style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold
                      ),
                      color = CharcoalText
                    )

                    Text(
                      text = "${translatorNovels.size} manuscripts uploaded to archive",
                      style = MaterialTheme.typography.bodySmall.copy(
                        color = AntiqueGold,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.5.sp
                      )
                    )
                  }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val safeBio = slot.bio.replace(emailRegex, "").trim()
                Text(
                  text = safeBio,
                  style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                  ),
                  color = CharcoalSecondary
                )

                // Optional Upload Cover Action for Translator/Owner
                if (isOwnerOrTranslator) {
                  Spacer(modifier = Modifier.height(10.dp))
                  OutlinedButton(
                    onClick = {
                      photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                      )
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SubtleBorder),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(32.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Outlined.AddPhotoAlternate,
                      contentDescription = null,
                      tint = AntiqueGold,
                      modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = if (slot.coverImageUri != null) "Change Profile Picture" else "Upload Picture",
                      style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.5.sp,
                        color = CharcoalText
                      )
                    )
                  }
                }

                // Translator Gmail Banner (Prominently visible to Owner Clarify)
                if (isOwner && slot.slotNumber != 0) {
                  Spacer(modifier = Modifier.height(12.dp))
                  Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = AntiqueGold.copy(alpha = 0.09f),
                    border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth().testTag("translator_gmail_owner_badge")
                  ) {
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                      ) {
                        Icon(
                          imageVector = Icons.Outlined.Mail,
                          contentDescription = null,
                          tint = AntiqueGold,
                          modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                          Text(
                            text = "TRANSLATOR GMAIL (VISIBLE TO OWNER ONLY)",
                            style = MaterialTheme.typography.labelSmall.copy(
                              fontSize = 8.5.sp,
                              fontWeight = FontWeight.Bold,
                              letterSpacing = 1.1.sp,
                              color = AntiqueGold
                            )
                          )
                          Text(
                            text = slot.translatorEmail ?: "translator${slot.slotNumber}@gmail.com",
                            style = MaterialTheme.typography.bodySmall.copy(
                              fontSize = 12.5.sp,
                              fontWeight = FontWeight.Bold,
                              color = CharcoalText
                            )
                          )
                        }
                      }
                      if (!slot.isPermissionGranted && onToggleSlotPermission != null) {
                        Button(
                          onClick = { onToggleSlotPermission(slot.slotNumber, true) },
                          shape = RoundedCornerShape(10.dp),
                          colors = ButtonDefaults.buttonColors(containerColor = AntiqueGold),
                          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                          modifier = Modifier.height(28.dp)
                        ) {
                          Text(
                            text = "Grant Permission",
                            style = MaterialTheme.typography.labelSmall.copy(
                              fontSize = 10.sp,
                              color = SoftCreamPaper,
                              fontWeight = FontWeight.Bold
                            )
                          )
                        }
                      }
                    }
                  }
                }

                if (isOwner && onToggleSlotPermission != null && slot.slotNumber != 0) {
                  Spacer(modifier = Modifier.height(14.dp))
                  Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (slot.isPermissionGranted) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                    border = BorderStroke(1.dp, if (slot.isPermissionGranted) Color(0xFFA5D6A7) else Color(0xFFFFCC80)),
                    modifier = Modifier.fillMaxWidth().testTag("translator_profile_permission_banner")
                  ) {
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                      Column(modifier = Modifier.weight(1f)) {
                        Text(
                          text = if (slot.isPermissionGranted) "TRANSLATOR PERMISSION ACTIVE" else "TRANSLATOR PERMISSION REVOKED",
                          style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (slot.isPermissionGranted) Color(0xFF2E7D32) else Color(0xFFE65100),
                            fontSize = 10.sp,
                            letterSpacing = 1.sp
                          )
                        )
                        Text(
                          text = if (slot.isPermissionGranted)
                            "Seat #${slot.slotNumber} is active. This translator can upload and manage manuscripts in the archive."
                          else
                            "Grant permission to ${slot.translatorEmail ?: "this translator"} to activate Seat #${slot.slotNumber} and allow them to publish.",
                          style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = CharcoalSecondary
                          )
                        )
                      }
                      Spacer(modifier = Modifier.width(8.dp))
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                          text = if (slot.isPermissionGranted) "ON" else "OFF",
                          style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            color = if (slot.isPermissionGranted) Color(0xFF2E7D32) else CharcoalTertiary
                          ),
                          modifier = Modifier.padding(end = 4.dp)
                        )
                        Switch(
                          checked = slot.isPermissionGranted,
                          onCheckedChange = { isChecked ->
                            onToggleSlotPermission(slot.slotNumber, isChecked)
                          },
                          colors = SwitchDefaults.colors(
                            checkedThumbColor = SoftCreamPaper,
                            checkedTrackColor = Color(0xFF2E7D32),
                            uncheckedThumbColor = CharcoalSecondary,
                            uncheckedTrackColor = SubtleBorder
                          ),
                          modifier = Modifier.testTag("profile_modal_slot_switch")
                        )
                      }
                    }
                  }
                }
              }
            }
          }

          // Tab Switcher: Published Works vs My Reading Shelf
          item {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x0E000000))
                .padding(3.dp),
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Surface(
                onClick = { selectedProfileTab = TranslatorProfileTab.WORKS },
                shape = RoundedCornerShape(10.dp),
                color = if (selectedProfileTab == TranslatorProfileTab.WORKS) SoftCreamPaper else Color.Transparent,
                shadowElevation = if (selectedProfileTab == TranslatorProfileTab.WORKS) 2.dp else 0.dp,
                modifier = Modifier
                  .weight(1f)
                  .testTag("translator_tab_works")
              ) {
                Row(
                  modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                  horizontalArrangement = Arrangement.Center,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = Icons.Outlined.AutoStories,
                    contentDescription = null,
                    tint = if (selectedProfileTab == TranslatorProfileTab.WORKS) AntiqueGold else CharcoalSecondary,
                    modifier = Modifier.size(13.dp)
                  )
                  Spacer(modifier = Modifier.width(5.dp))
                  Text(
                    text = "Works (${translatorNovels.size})",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = if (selectedProfileTab == TranslatorProfileTab.WORKS) FontWeight.Bold else FontWeight.Medium,
                      fontSize = 11.sp
                    ),
                    color = if (selectedProfileTab == TranslatorProfileTab.WORKS) CharcoalText else CharcoalSecondary
                  )
                }
              }

              Surface(
                onClick = { selectedProfileTab = TranslatorProfileTab.MY_SHELF },
                shape = RoundedCornerShape(10.dp),
                color = if (selectedProfileTab == TranslatorProfileTab.MY_SHELF) SoftCreamPaper else Color.Transparent,
                shadowElevation = if (selectedProfileTab == TranslatorProfileTab.MY_SHELF) 2.dp else 0.dp,
                modifier = Modifier
                  .weight(1f)
                  .testTag("translator_tab_shelf")
              ) {
                Row(
                  modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                  horizontalArrangement = Arrangement.Center,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = Icons.Filled.MenuBook,
                    contentDescription = null,
                    tint = if (selectedProfileTab == TranslatorProfileTab.MY_SHELF) AntiqueGold else CharcoalSecondary,
                    modifier = Modifier.size(13.dp)
                  )
                  Spacer(modifier = Modifier.width(5.dp))
                  Text(
                    text = "My Shelf",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = if (selectedProfileTab == TranslatorProfileTab.MY_SHELF) FontWeight.Bold else FontWeight.Medium,
                      fontSize = 11.sp
                    ),
                    color = if (selectedProfileTab == TranslatorProfileTab.MY_SHELF) CharcoalText else CharcoalSecondary
                  )
                }
              }
            }
          }

          if (selectedProfileTab == TranslatorProfileTab.WORKS) {
            // Subtle Search Bar
            item {
              OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                  Text(
                    "Search manuscripts by title, chapter or excerpt...",
                    style = MaterialTheme.typography.bodySmall.copy(
                      color = CharcoalTertiary,
                      fontSize = 12.sp
                    )
                  )
                },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Search",
                    tint = AntiqueGold,
                    modifier = Modifier.size(17.dp)
                  )
                },
                trailingIcon = {
                  if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                      Icon(
                        imageVector = Icons.Outlined.Clear,
                        contentDescription = "Clear",
                        tint = CharcoalSecondary,
                        modifier = Modifier.size(16.dp)
                      )
                    }
                  }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedContainerColor = SoftCreamPaper,
                  unfocusedContainerColor = SoftCreamPaper,
                  focusedBorderColor = AntiqueGold,
                  unfocusedBorderColor = SubtleBorder
                ),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("translator_search_bar")
              )
            }

            // Subtle 'Sort by' Filter Chips
            item {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Outlined.Sort,
                    contentDescription = null,
                    tint = AntiqueGold,
                    modifier = Modifier.size(14.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = "Sort:",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = FontWeight.Bold,
                      fontSize = 10.5.sp
                    ),
                    color = CharcoalTertiary
                  )
                }

                TranslatorSortChip(
                  label = "Newest First",
                  selected = selectedSort == TranslatorWorksSort.NEWEST,
                  onClick = { selectedSort = TranslatorWorksSort.NEWEST }
                )

                TranslatorSortChip(
                  label = "Most Popular",
                  selected = selectedSort == TranslatorWorksSort.POPULAR,
                  onClick = { selectedSort = TranslatorWorksSort.POPULAR }
                )

                TranslatorSortChip(
                  label = "Oldest First",
                  selected = selectedSort == TranslatorWorksSort.OLDEST,
                  onClick = { selectedSort = TranslatorWorksSort.OLDEST }
                )

                TranslatorSortChip(
                  label = "Title (A–Z)",
                  selected = selectedSort == TranslatorWorksSort.TITLE_AZ,
                  onClick = { selectedSort = TranslatorWorksSort.TITLE_AZ }
                )

                TranslatorSortChip(
                  label = "Length (Pages)",
                  selected = selectedSort == TranslatorWorksSort.PAGES,
                  onClick = { selectedSort = TranslatorWorksSort.PAGES }
                )
              }
            }

            // Section Title
            item {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "TRANSLATED WORKS (${displayedNovels.size})",
                  style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.3.sp,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                  ),
                  color = CharcoalTertiary
                )

                if (searchQuery.isNotEmpty()) {
                  Text(
                    text = "Filtered results",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontSize = 10.sp,
                      color = AntiqueGold
                    )
                  )
                }
              }
            }

            // Empty state if no works match
            if (displayedNovels.isEmpty()) {
              item {
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                  contentAlignment = Alignment.Center
                ) {
                  Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                      imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                      contentDescription = null,
                      tint = AntiqueGold.copy(alpha = 0.5f),
                      modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                      text = if (searchQuery.isNotEmpty()) "No manuscripts match '$searchQuery'" else "No works published in this translator's archive yet.",
                      style = MaterialTheme.typography.bodyMedium,
                      color = CharcoalSecondary
                    )
                    if (searchQuery.isNotEmpty()) {
                      Spacer(modifier = Modifier.height(6.dp))
                      OutlinedButton(
                        onClick = { searchQuery = "" },
                        shape = RoundedCornerShape(10.dp)
                      ) {
                        Text("Clear Search", fontSize = 11.sp)
                      }
                    }
                  }
                }
              }
            } else {
              // Clean Library Grid / Cards for all works
              items(displayedNovels, key = { it.id }) { novel ->
                val canEditThisNovel = isOwner || (isOwnerOrTranslator && (novel.authorSlot == slot.slotNumber || novel.author.equals(slot.penName, ignoreCase = true) || novel.author.equals(slot.authorName, ignoreCase = true)))
                val canAddChapter = isOwnerOrTranslator && (isOwner || novel.authorSlot == slot.slotNumber)
                TranslatorNovelCard(
                  novel = novel,
                  canEdit = canEditThisNovel,
                  onRead = {
                    onDismiss()
                    onSelectNovel(novel)
                  },
                  onEdit = if (canEditThisNovel && onEditNovel != null) {
                    {
                      onDismiss()
                      onEditNovel(novel)
                    }
                  } else null,
                  onAddChapter = if (canAddChapter && onAddChapterToNovel != null) {
                    { novelForAddChapter = novel }
                  } else null
                )
              }
            }
          } else {
            // My Reading Shelf (TBR, Reading, Finished, Favorites)
            item {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                listOf(
                  Triple(TranslatorShelfCategory.READING, "Reading (${readingNovels.size})", Icons.Filled.MenuBook),
                  Triple(TranslatorShelfCategory.TO_BE_READ, "TBR (${tbrNovels.size})", Icons.Filled.Bookmark),
                  Triple(TranslatorShelfCategory.FINISHED, "Finished (${finishedNovels.size})", Icons.Filled.CheckCircle),
                  Triple(TranslatorShelfCategory.FAVORITES, "Favs (${favoriteNovels.size})", Icons.Filled.Favorite),
                ).forEach { (cat, label, icon) ->
                  val isSel = selectedShelfCategory == cat
                  Surface(
                    onClick = { selectedShelfCategory = cat },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSel) CharcoalText else SoftCreamPaper,
                    border = BorderStroke(1.dp, if (isSel) CharcoalText else SubtleBorder),
                    modifier = Modifier.weight(1f)
                  ) {
                    Row(
                      modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
                      horizontalArrangement = Arrangement.Center,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSel) AntiqueGold else CharcoalSecondary,
                        modifier = Modifier.size(11.dp)
                      )
                      Spacer(modifier = Modifier.width(3.dp))
                      Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(
                          fontSize = 9.5.sp,
                          fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (isSel) SoftCreamPaper else CharcoalSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                      )
                    }
                  }
                }
              }
            }

            val currentShelfNovels = when (selectedShelfCategory) {
              TranslatorShelfCategory.READING -> readingNovels
              TranslatorShelfCategory.TO_BE_READ -> tbrNovels
              TranslatorShelfCategory.FINISHED -> finishedNovels
              TranslatorShelfCategory.FAVORITES -> favoriteNovels
            }

            if (currentShelfNovels.isEmpty()) {
              item {
                Surface(
                  shape = RoundedCornerShape(16.dp),
                  color = SoftCreamPaper,
                  border = BorderStroke(1.dp, SubtleBorder),
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                ) {
                  Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                  ) {
                    Icon(
                      imageVector = when (selectedShelfCategory) {
                        TranslatorShelfCategory.READING -> Icons.Filled.MenuBook
                        TranslatorShelfCategory.TO_BE_READ -> Icons.Filled.Bookmark
                        TranslatorShelfCategory.FINISHED -> Icons.Filled.CheckCircle
                        TranslatorShelfCategory.FAVORITES -> Icons.Filled.Favorite
                      },
                      contentDescription = null,
                      tint = AntiqueGold,
                      modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                      text = when (selectedShelfCategory) {
                        TranslatorShelfCategory.READING -> "No novels currently being read"
                        TranslatorShelfCategory.TO_BE_READ -> "Your TBR shelf is empty"
                        TranslatorShelfCategory.FINISHED -> "No novels finished yet"
                        TranslatorShelfCategory.FAVORITES -> "No favorited novels yet"
                      },
                      style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold
                      ),
                      color = CharcoalText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                      text = "Explore the strawberrycandy archive to discover manuscripts and add them to your shelf.",
                      style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                      color = CharcoalSecondary,
                      textAlign = TextAlign.Center
                    )
                  }
                }
              }
            } else {
              items(currentShelfNovels, key = { it.id }) { novel ->
                Card(
                  shape = RoundedCornerShape(14.dp),
                  colors = CardDefaults.cardColors(containerColor = SoftCreamPaper),
                  border = BorderStroke(1.dp, SubtleBorder),
                  modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                      onDismiss()
                      onSelectNovel(novel)
                    }
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    // Cover thumbnail
                    Box(
                      modifier = Modifier
                        .size(width = 46.dp, height = 64.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(novel.coverColorHex)),
                      contentAlignment = Alignment.Center
                    ) {
                      if (novel.coverImageUri != null) {
                        AsyncImage(
                          model = novel.coverImageUri,
                          contentDescription = novel.title,
                          contentScale = ContentScale.Crop,
                          modifier = Modifier.matchParentSize()
                        )
                      } else {
                        Text(
                          text = novel.title.take(1).uppercase(),
                          style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                          )
                        )
                      }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                      ) {
                        Surface(
                          shape = RoundedCornerShape(4.dp),
                          color = if (novel.isCompletedNovel) Color(0xFF2E7D32).copy(alpha = 0.15f) else Color(0xFFD87D2A).copy(alpha = 0.15f)
                        ) {
                          Text(
                            text = if (novel.isCompletedNovel) "✓ FINISHED" else "• ONGOING",
                            style = MaterialTheme.typography.labelSmall.copy(
                              fontSize = 7.5.sp,
                              fontWeight = FontWeight.Bold,
                              color = if (novel.isCompletedNovel) Color(0xFF2E7D32) else Color(0xFFD87D2A)
                            ),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
                          )
                        }
                        Text(
                          text = "${novel.totalPages} pages",
                          style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                          color = CharcoalSecondary
                        )
                      }

                      Spacer(modifier = Modifier.height(3.dp))

                      Text(
                        text = novel.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                          fontFamily = FontFamily.Serif,
                          fontWeight = FontWeight.Bold,
                          fontSize = 13.sp
                        ),
                        color = CharcoalText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                      )

                      Text(
                        text = novel.authorLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = CharcoalSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                      )

                      if (selectedShelfCategory == TranslatorShelfCategory.READING) {
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                          progress = { novel.progressFraction },
                          modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                          color = AntiqueGold,
                          trackColor = CharcoalText.copy(alpha = 0.1f),
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                          text = "Page ${novel.currentPage} of ${novel.totalPages} (${(novel.progressFraction * 100).toInt()}%)",
                          style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                          color = AntiqueGold
                        )
                      }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                      onClick = {
                        onDismiss()
                        onSelectNovel(novel)
                      },
                      shape = RoundedCornerShape(10.dp),
                      colors = ButtonDefaults.buttonColors(containerColor = CharcoalText),
                      contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                      modifier = Modifier.height(30.dp)
                    ) {
                      Text(
                        text = novel.readButtonLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                          fontSize = 10.sp,
                          fontWeight = FontWeight.Bold,
                          color = SoftCreamPaper
                        )
                      )
                    }
                  }
                }
              }
            }
          }

          item {
            Spacer(modifier = Modifier.height(30.dp))
          }
        }
      }
    }
  }
}

@Composable
private fun TranslatorSortChip(
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
          fontSize = 10.5.sp,
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
    shape = RoundedCornerShape(10.dp)
  )
}

/**
 * Individual Novel / Work Card in Translator's Archive.
 * Readers can strictly view metadata and click "Read Novel".
 */
@Composable
private fun TranslatorNovelCard(
  novel: NovelWithState,
  canEdit: Boolean = false,
  onRead: () -> Unit,
  onEdit: (() -> Unit)? = null,
  onAddChapter: (() -> Unit)? = null,
) {
  val haptic = LocalHapticFeedback.current
  val coroutineScope = rememberCoroutineScope()
  var holdProgress by remember { mutableFloatStateOf(0f) }
  val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
  val formattedDate = remember(novel.createdAt) {
    dateFormat.format(Date(novel.createdAt))
  }

  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = SoftCreamPaper),
    border = BorderStroke(1.dp, SubtleBorder),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("translator_novel_${novel.id}")
      .pointerInput(novel.id, canEdit) {
        if (canEdit && onEdit != null) {
          detectTapGestures(
            onPress = {
              val startTime = System.currentTimeMillis()
              val totalMs = 2000L
              val interval = 40L
              val job = coroutineScope.launch {
                var elapsed = 0L
                while (elapsed < totalMs) {
                  kotlinx.coroutines.delay(interval)
                  elapsed += interval
                  holdProgress = (elapsed.toFloat() / totalMs).coerceIn(0f, 1f)
                  if (elapsed >= totalMs) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onEdit()
                    holdProgress = 0f
                    return@launch
                  }
                }
              }
              try {
                val released = tryAwaitRelease()
                if (released) {
                  val elapsed = System.currentTimeMillis() - startTime
                  if (elapsed < 500L) {
                    onRead()
                  }
                }
              } finally {
                job.cancel()
                holdProgress = 0f
              }
            }
          )
        } else {
          detectTapGestures(onTap = { onRead() })
        }
      }
  ) {
    Box(modifier = Modifier.fillMaxWidth()) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(14.dp),
        verticalAlignment = Alignment.Top
      ) {
      // Book Cover Thumbnail
      Box(
        modifier = Modifier
          .width(72.dp)
          .height(104.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(Color(novel.coverColorHex)),
        contentAlignment = Alignment.Center
      ) {
        if (novel.coverImageUri != null) {
          AsyncImage(
            model = File(novel.coverImageUri!!).takeIf { it.exists() } ?: novel.coverImageUri,
            contentDescription = novel.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
        } else if (novel.coverDrawableRes != 0) {
          Image(
            painter = painterResource(id = novel.coverDrawableRes),
            contentDescription = novel.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
        } else {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(6.dp)
          ) {
            Text(
              text = novel.title.take(16),
              style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Serif,
                fontSize = 8.sp,
                color = SoftCreamPaper,
                fontWeight = FontWeight.Bold
              ),
              maxLines = 3,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }

      Spacer(modifier = Modifier.width(14.dp))

      // Novel Details
      Column(
        modifier = Modifier.weight(1f)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = novel.year.ifEmpty { "2026" },
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                color = AntiqueGold,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
              )
            )
            if (novel.isNewRelease) {
              Spacer(modifier = Modifier.width(6.dp))
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = AntiqueGold,
              ) {
                Text(
                  text = "NEW",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepBurgundy
                  ),
                  modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
              }
            }
          }

          Text(
            text = "Uploaded $formattedDate",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 8.5.sp,
              color = CharcoalTertiary
            )
          )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
          text = novel.title,
          style = MaterialTheme.typography.titleMedium.copy(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold
          ),
          color = CharcoalText,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )

        Text(
          text = novel.subtitle,
          style = MaterialTheme.typography.bodySmall.copy(
            fontSize = 11.sp
          ),
          color = CharcoalSecondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = novel.excerpt,
          style = MaterialTheme.typography.bodySmall.copy(
            fontSize = 11.sp,
            lineHeight = 15.sp
          ),
          color = CharcoalSecondary,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Readership & Favorites stats
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.padding(vertical = 2.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Outlined.Visibility,
              contentDescription = null,
              tint = CharcoalTertiary,
              modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              text = "${formatStatCount(novel.readsCount)} reads",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
              color = CharcoalTertiary
            )
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Filled.Favorite,
              contentDescription = null,
              tint = if (novel.favoritesCount > 0) Color(0xFFEF5350) else CharcoalTertiary,
              modifier = Modifier.size(10.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              text = "${formatStatCount(novel.favoritesCount)} favs",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
              color = CharcoalTertiary
            )
          }

          if (novel.storyPhotos.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Outlined.PhotoLibrary,
                contentDescription = null,
                tint = AntiqueGold,
                modifier = Modifier.size(10.dp)
              )
              Spacer(modifier = Modifier.width(3.dp))
              Text(
                text = "${novel.storyPhotos.size} photos",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 10.sp,
                  color = AntiqueGold,
                  fontWeight = FontWeight.Medium
                )
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = if (novel.currentPage > 1) "Reading • Page ${novel.currentPage} of ${novel.totalPages}" else "${novel.totalPages} pages",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 10.sp,
              color = if (novel.currentPage > 1) AntiqueGold else CharcoalTertiary,
              fontWeight = if (novel.currentPage > 1) FontWeight.Bold else FontWeight.Normal
            )
          )

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            if (canEdit && onEdit != null) {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = AntiqueGold.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.65f)),
                modifier = Modifier
                  .clickable { onEdit() }
                  .testTag("edit_novel_translator_card_${novel.id}")
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = null,
                    tint = AntiqueGold,
                    modifier = Modifier.size(11.dp)
                  )
                  Spacer(modifier = Modifier.width(3.dp))
                  Text(
                    text = "Edit",
                    style = MaterialTheme.typography.labelSmall.copy(
                      color = CharcoalText,
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold
                    )
                  )
                }
              }
            }

            if (onAddChapter != null) {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = AntiqueGold.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.65f)),
                modifier = Modifier
                  .clickable { onAddChapter() }
                  .testTag("add_chapter_translator_card_${novel.id}")
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    tint = AntiqueGold,
                    modifier = Modifier.size(11.dp)
                  )
                  Spacer(modifier = Modifier.width(3.dp))
                  Text(
                    text = "+ Chapter",
                    style = MaterialTheme.typography.labelSmall.copy(
                      color = CharcoalText,
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold
                    )
                  )
                }
              }
            }

            Surface(
              shape = RoundedCornerShape(12.dp),
              color = CharcoalText,
              modifier = Modifier.clickable { onRead() }
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                  contentDescription = null,
                  tint = SoftCreamPaper,
                  modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = if (novel.currentPage > 1) "Continue" else "Read",
                  style = MaterialTheme.typography.labelSmall.copy(
                    color = SoftCreamPaper,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold
                  )
                )
              }
            }
          }
        }
      }
    }

    // 2-Second Hold progress overlay for editing
    if (canEdit && holdProgress > 0f) {
      Box(
        modifier = Modifier
          .matchParentSize()
          .background(Color(0xEE1E1815))
          .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          CircularProgressIndicator(
            progress = { holdProgress },
            modifier = Modifier.size(24.dp),
            color = AntiqueGold,
            trackColor = AntiqueGold.copy(alpha = 0.25f),
            strokeWidth = 2.5.dp
          )
          val remainingSec = ((2000L - (holdProgress * 2000L).toLong() + 900L) / 1000L).coerceIn(1L, 2L)
          Text(
            text = "Holding to Edit Manuscript (${remainingSec}s)...",
            style = MaterialTheme.typography.labelMedium.copy(
              fontWeight = FontWeight.Bold,
              color = SoftCreamPaper
            )
          )
        }
      }
    }
  }
}
}
