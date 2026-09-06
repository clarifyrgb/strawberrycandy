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
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Lock
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.local.AuthorSlotEntity
import com.example.model.NovelWithState
import com.example.ui.theme.AntiqueGold
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
) {
  val context = LocalContext.current
  var searchQuery by remember { mutableStateOf("") }
  var selectedSort by remember { mutableStateOf(TranslatorWorksSort.NEWEST) }

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
            if (!slot.isPermissionGranted && isOwner && onToggleSlotPermission != null && slot.slotNumber != 0) {
              Button(
                onClick = {
                  onToggleSlotPermission(slot.slotNumber, true)
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AntiqueGold),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp)
              ) {
                Icon(
                  imageVector = Icons.Outlined.PersonAdd,
                  contentDescription = null,
                  tint = SoftCreamPaper,
                  modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "Grant Permission",
                  style = MaterialTheme.typography.labelSmall.copy(
                    color = SoftCreamPaper,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                  )
                )
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
                      if (slot.slotNumber == 0) "Strawberrycandy" else "Translator ${slot.slotNumber}"
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

                if (!slot.isPermissionGranted && isOwner && onToggleSlotPermission != null && slot.slotNumber != 0) {
                  Spacer(modifier = Modifier.height(14.dp))
                  Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AntiqueGold.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
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
                          text = "FUTURE TRANSLATOR SLOT",
                          style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = AntiqueGold,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp
                          )
                        )
                        Text(
                          text = "You can grant permission to activate Seat ${slot.slotNumber} and make this translator visible as an active curator.",
                          style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = CharcoalSecondary
                          )
                        )
                      }
                      Spacer(modifier = Modifier.width(8.dp))
                      Button(
                        onClick = { onToggleSlotPermission(slot.slotNumber, true) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CharcoalText),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp)
                      ) {
                        Text(
                          text = "Grant Permission",
                          style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.5.sp,
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
          }

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
              TranslatorNovelCard(
                novel = novel,
                onRead = {
                  onDismiss()
                  onSelectNovel(novel)
                }
              )
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
  onRead: () -> Unit
) {
  val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
  val formattedDate = remember(novel.createdAt) {
    dateFormat.format(Date(novel.createdAt))
  }

  Card(
    onClick = onRead,
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = SoftCreamPaper),
    border = BorderStroke(1.dp, SubtleBorder),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("translator_novel_${novel.id}")
  ) {
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
          Text(
            text = novel.year.ifEmpty { "2026" },
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 9.sp,
              color = AntiqueGold,
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.8.sp
            )
          )

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
}
