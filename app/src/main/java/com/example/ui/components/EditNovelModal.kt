package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import com.example.model.NovelWithState
import com.example.ui.theme.AntiqueGold
import com.example.ui.theme.CharcoalSecondary
import com.example.ui.theme.CharcoalTertiary
import com.example.ui.theme.CharcoalText
import com.example.ui.theme.SoftCreamPaper
import com.example.ui.theme.SubtleBorder
import java.io.File
import java.io.FileOutputStream

private val LUXURY_LEATHER_COLORS = listOf(
  0xFF2C221E to "Obsidian Leather",
  0xFF4A1E27 to "Vintage Crimson",
  0xFF1A3329 to "Forest Archive",
  0xFF1E283C to "Midnight Velvet",
  0xFF5C3317 to "Antique Sienna",
  0xFF3D2542 to "Imperial Plum",
  0xFF333333 to "Studio Slate"
)

@Composable
fun EditNovelModal(
  novel: NovelWithState,
  onDismiss: () -> Unit,
  onUpdateNovel: (
    novelId: String,
    title: String,
    subtitle: String,
    originalAuthor: String,
    synopsis: String,
    chapterTitle: String,
    contentText: String,
    coverColorHex: Long?,
    coverImageUri: String?,
    novelStatus: String?,
    releaseFormat: String?,
  ) -> Unit,
  onDeleteNovel: (novelId: String) -> Unit,
) {
  val context = LocalContext.current

  var title by remember { mutableStateOf(novel.title) }
  var subtitle by remember { mutableStateOf(novel.subtitle) }
  var originalAuthor by remember { mutableStateOf(novel.originalAuthor) }
  var synopsis by remember { mutableStateOf(novel.excerpt) }
  var chapterTitle by remember { mutableStateOf(novel.chapterTitle) }
  var contentText by remember { mutableStateOf(novel.novel.contentText) }
  var selectedColorHex by remember { mutableLongStateOf(novel.coverColorHex) }
  var coverImageUri by remember { mutableStateOf<String?>(novel.coverImageUri) }
  var isFinishedNovel by remember { mutableStateOf(novel.isCompletedNovel) }
  var isPerVolumeRelease by remember { mutableStateOf(novel.isPerVolume) }

  var isConfirmDeleteOpen by remember { mutableStateOf(false) }

  val singleCoverPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    if (uri != null) {
      try {
        val coversDir = File(context.filesDir, "novel_covers").apply { mkdirs() }
        val destFile = File(coversDir, "cover_${novel.id}_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
          FileOutputStream(destFile).use { output ->
            input.copyTo(output)
          }
        }
        coverImageUri = destFile.absolutePath
      } catch (e: Exception) {
        coverImageUri = uri.toString()
      }
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
        .padding(horizontal = 12.dp, vertical = 20.dp)
        .testTag("edit_novel_modal")
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
            Box(
              modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AntiqueGold.copy(alpha = 0.16f))
                .border(1.dp, AntiqueGold.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = null,
                tint = AntiqueGold,
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "EDIT NOVEL",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontFamily = FontFamily.Serif,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 1.sp
                ),
                color = CharcoalText
              )
              Text(
                text = "Translator Editorial Studio • 3s Access",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = CharcoalSecondary
              )
            }
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(32.dp)
          ) {
            Icon(
              imageVector = Icons.Outlined.Close,
              contentDescription = "Close",
              tint = CharcoalTertiary
            )
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Title
        Text(
          text = "NOVEL TITLE",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
          ),
          color = AntiqueGold
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          placeholder = { Text("Novel title...") },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("edit_title_input"),
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AntiqueGold,
            unfocusedBorderColor = SubtleBorder,
            focusedContainerColor = SoftCreamPaper,
            unfocusedContainerColor = SoftCreamPaper,
            focusedTextColor = CharcoalText,
            unfocusedTextColor = CharcoalText
          )
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Subtitle / Tagline
        Text(
          text = "SUBTITLE / TAGLINE",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
          ),
          color = AntiqueGold
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
          value = subtitle,
          onValueChange = { subtitle = it },
          placeholder = { Text("A philosophical journey...") },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("edit_subtitle_input"),
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AntiqueGold,
            unfocusedBorderColor = SubtleBorder,
            focusedContainerColor = SoftCreamPaper,
            unfocusedContainerColor = SoftCreamPaper,
            focusedTextColor = CharcoalText,
            unfocusedTextColor = CharcoalText
          )
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Original Author
        Text(
          text = "ORIGINAL AUTHOR",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
          ),
          color = AntiqueGold
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
          value = originalAuthor,
          onValueChange = { originalAuthor = it },
          placeholder = { Text("e.g. Alexandre Dumas, Osamu Dazai...") },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("edit_original_author_input"),
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AntiqueGold,
            unfocusedBorderColor = SubtleBorder,
            focusedContainerColor = SoftCreamPaper,
            unfocusedContainerColor = SoftCreamPaper,
            focusedTextColor = CharcoalText,
            unfocusedTextColor = CharcoalText
          )
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Synopsis / Excerpt
        Text(
          text = "SYNOPSIS & BACK COVER SUMMARY",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
          ),
          color = AntiqueGold
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
          value = synopsis,
          onValueChange = { synopsis = it },
          placeholder = { Text("Write or edit the synopsis...") },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("edit_synopsis_input"),
          minLines = 3,
          maxLines = 6,
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AntiqueGold,
            unfocusedBorderColor = SubtleBorder,
            focusedContainerColor = SoftCreamPaper,
            unfocusedContainerColor = SoftCreamPaper,
            focusedTextColor = CharcoalText,
            unfocusedTextColor = CharcoalText
          )
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Chapter Title
        Text(
          text = "CHAPTER TITLE",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
          ),
          color = AntiqueGold
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
          value = chapterTitle,
          onValueChange = { chapterTitle = it },
          placeholder = { Text("e.g. Chapter I • The Arrival") },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("edit_chapter_title_input"),
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AntiqueGold,
            unfocusedBorderColor = SubtleBorder,
            focusedContainerColor = SoftCreamPaper,
            unfocusedContainerColor = SoftCreamPaper,
            focusedTextColor = CharcoalText,
            unfocusedTextColor = CharcoalText
          )
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Novel Content
        Text(
          text = "NOVEL CONTENT & CHAPTER TEXT",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
          ),
          color = AntiqueGold
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
          value = contentText,
          onValueChange = { contentText = it },
          placeholder = { Text("Full novel text here...") },
          modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .testTag("edit_content_input"),
          maxLines = 20,
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AntiqueGold,
            unfocusedBorderColor = SubtleBorder,
            focusedContainerColor = SoftCreamPaper,
            unfocusedContainerColor = SoftCreamPaper,
            focusedTextColor = CharcoalText,
            unfocusedTextColor = CharcoalText
          )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Cover Color Selection
        Text(
          text = "HARDCOVER EDITION COLOR",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
          ),
          color = AntiqueGold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          LUXURY_LEATHER_COLORS.forEach { (colorHex, _) ->
            val isSelected = selectedColorHex == colorHex
            Box(
              modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(colorHex))
                .border(
                  width = if (isSelected) 2.5.dp else 1.dp,
                  color = if (isSelected) AntiqueGold else SubtleBorder,
                  shape = CircleShape
                )
                .clickable { selectedColorHex = colorHex },
              contentAlignment = Alignment.Center
            ) {
              if (isSelected) {
                Icon(
                  imageVector = Icons.Outlined.Check,
                  contentDescription = "Selected",
                  tint = Color.White,
                  modifier = Modifier.size(16.dp)
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Cover Image Pick or Remove
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            if (coverImageUri != null) {
              AsyncImage(
                model = File(coverImageUri!!).takeIf { it.exists() } ?: coverImageUri,
                contentDescription = "Cover Image Preview",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                  .size(44.dp)
                  .clip(RoundedCornerShape(8.dp))
                  .border(1.dp, SubtleBorder, RoundedCornerShape(8.dp))
              )
              Spacer(modifier = Modifier.width(10.dp))
              TextButton(
                onClick = { coverImageUri = null },
                contentPadding = PaddingValues(0.dp)
              ) {
                Text(
                  text = "Remove Cover Art",
                  style = MaterialTheme.typography.labelSmall,
                  color = Color(0xFFC74350)
                )
              }
            } else {
              Text(
                text = "Using Hardcover Leather Palette",
                style = MaterialTheme.typography.bodySmall,
                color = CharcoalSecondary
              )
            }
          }

          OutlinedButton(
            onClick = {
              singleCoverPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, SubtleBorder),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
          ) {
            Icon(
              imageVector = Icons.Outlined.AddPhotoAlternate,
              contentDescription = null,
              tint = AntiqueGold,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (coverImageUri == null) "Pick Cover Image" else "Change Image",
              style = MaterialTheme.typography.labelSmall,
              color = CharcoalText
            )
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Novel Status (Ongoing vs Finished) & Release Format (Chapter vs Volume) Toggles
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = AntiqueGold.copy(alpha = 0.07f),
          border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.25f)),
          modifier = Modifier.fillMaxWidth().testTag("edit_status_and_format_card")
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            // 1. Novel Status Toggle: Ongoing vs Finished
            Column {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "NOVEL STATUS",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = AntiqueGold
                  )
                )
                Text(
                  text = if (isFinishedNovel) "✓ Finished" else "• Ongoing",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isFinishedNovel) Color(0xFF2E7D32) else Color(0xFFD87D2A)
                  )
                )
              }
              Spacer(modifier = Modifier.height(6.dp))
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(10.dp))
                  .background(SoftCreamPaper)
                  .border(BorderStroke(1.dp, SubtleBorder), shape = RoundedCornerShape(10.dp))
                  .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Surface(
                  onClick = { isFinishedNovel = false },
                  shape = RoundedCornerShape(8.dp),
                  color = if (!isFinishedNovel) Color(0xFFD87D2A) else Color.Transparent,
                  modifier = Modifier.weight(1f).testTag("edit_toggle_ongoing")
                ) {
                  Text(
                    text = "Ongoing",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = if (!isFinishedNovel) FontWeight.Bold else FontWeight.Normal,
                      fontSize = 11.sp
                    ),
                    color = if (!isFinishedNovel) Color.White else CharcoalSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 7.dp)
                  )
                }
                Surface(
                  onClick = { isFinishedNovel = true },
                  shape = RoundedCornerShape(8.dp),
                  color = if (isFinishedNovel) Color(0xFF2E7D32) else Color.Transparent,
                  modifier = Modifier.weight(1f).testTag("edit_toggle_finished")
                ) {
                  Text(
                    text = "Finished",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = if (isFinishedNovel) FontWeight.Bold else FontWeight.Normal,
                      fontSize = 11.sp
                    ),
                    color = if (isFinishedNovel) Color.White else CharcoalSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 7.dp)
                  )
                }
              }
            }

            // 2. Release Format: Per Chapter vs Per Volume
            Column {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "RELEASE FORMAT",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = AntiqueGold
                  )
                )
                Text(
                  text = if (isPerVolumeRelease) "Button: 'Read Volume'" else "Button: 'Read Chapters'",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = CharcoalSecondary
                  )
                )
              }
              Spacer(modifier = Modifier.height(6.dp))
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(10.dp))
                  .background(SoftCreamPaper)
                  .border(BorderStroke(1.dp, SubtleBorder), shape = RoundedCornerShape(10.dp))
                  .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Surface(
                  onClick = { isPerVolumeRelease = false },
                  shape = RoundedCornerShape(8.dp),
                  color = if (!isPerVolumeRelease) CharcoalText else Color.Transparent,
                  modifier = Modifier.weight(1f).testTag("edit_toggle_chapter")
                ) {
                  Text(
                    text = "Per Chapter",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = if (!isPerVolumeRelease) FontWeight.Bold else FontWeight.Normal,
                      fontSize = 11.sp
                    ),
                    color = if (!isPerVolumeRelease) SoftCreamPaper else CharcoalSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 7.dp)
                  )
                }
                Surface(
                  onClick = { isPerVolumeRelease = true },
                  shape = RoundedCornerShape(8.dp),
                  color = if (isPerVolumeRelease) CharcoalText else Color.Transparent,
                  modifier = Modifier.weight(1f).testTag("edit_toggle_volume")
                ) {
                  Text(
                    text = "Per Volume",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = if (isPerVolumeRelease) FontWeight.Bold else FontWeight.Normal,
                      fontSize = 11.sp
                    ),
                    color = if (isPerVolumeRelease) SoftCreamPaper else CharcoalSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 7.dp)
                  )
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action Buttons: Delete Novel & Save Changes
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Delete Novel Button
          OutlinedButton(
            onClick = { isConfirmDeleteOpen = true },
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.outlinedButtonColors(
              contentColor = Color(0xFFC74350)
            ),
            border = BorderStroke(1.dp, Color(0xFFC74350).copy(alpha = 0.5f)),
            modifier = Modifier
              .weight(1f)
              .testTag("delete_novel_button")
          ) {
            Icon(
              imageVector = Icons.Outlined.Delete,
              contentDescription = null,
              tint = Color(0xFFC74350),
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Delete Novel",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
            )
          }

          // Save Changes Button
          Button(
            onClick = {
              if (title.isNotBlank()) {
                onUpdateNovel(
                  novel.id,
                  title,
                  subtitle,
                  originalAuthor,
                  synopsis,
                  chapterTitle,
                  contentText,
                  selectedColorHex,
                  coverImageUri,
                  if (isFinishedNovel) "FINISHED" else "ONGOING",
                  if (isPerVolumeRelease) "VOLUME" else "CHAPTER"
                )
                onDismiss()
              }
            },
            enabled = title.isNotBlank(),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = CharcoalText,
              contentColor = SoftCreamPaper,
              disabledContainerColor = CharcoalText.copy(alpha = 0.4f)
            ),
            modifier = Modifier
              .weight(1.3f)
              .testTag("save_novel_button")
          ) {
            Icon(
              imageVector = Icons.Outlined.Check,
              contentDescription = null,
              tint = AntiqueGold,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Save Changes",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
            )
          }
        }
      }
    }
  }

  // Confirmation Alert for Deletion
  if (isConfirmDeleteOpen) {
    AlertDialog(
      onDismissRequest = { isConfirmDeleteOpen = false },
      icon = {
        Icon(
          imageVector = Icons.Outlined.WarningAmber,
          contentDescription = null,
          tint = Color(0xFFC74350),
          modifier = Modifier.size(32.dp)
        )
      },
      title = {
        Text(
          text = "Delete Novel Permanently?",
          style = MaterialTheme.typography.titleMedium.copy(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold
          ),
          color = CharcoalText,
          textAlign = TextAlign.Center
        )
      },
      text = {
        Text(
          text = "Are you sure you want to delete \"${novel.title}\"? This novel and its reader history will be permanently deleted from the Strawberrycandy archive.",
          style = MaterialTheme.typography.bodyMedium,
          color = CharcoalSecondary,
          textAlign = TextAlign.Center
        )
      },
      confirmButton = {
        Button(
          onClick = {
            isConfirmDeleteOpen = false
            onDeleteNovel(novel.id)
            onDismiss()
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFC74350),
            contentColor = Color.White
          ),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.testTag("confirm_delete_novel_button")
        ) {
          Text("Yes, Delete Novel")
        }
      },
      dismissButton = {
        TextButton(
          onClick = { isConfirmDeleteOpen = false }
        ) {
          Text("Cancel", color = CharcoalText)
        }
      },
      containerColor = SoftCreamPaper,
      shape = RoundedCornerShape(20.dp)
    )
  }
}
