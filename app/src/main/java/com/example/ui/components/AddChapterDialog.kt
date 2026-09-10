package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Close
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.NovelWithState
import com.example.ui.theme.AntiqueGold
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.outlined.FormatSize
import com.example.ui.theme.CharcoalSecondary
import com.example.ui.theme.CharcoalTertiary
import com.example.ui.theme.CharcoalText
import com.example.ui.theme.SoftCreamPaper
import com.example.ui.theme.SubtleBorder
import java.io.File
import java.io.FileOutputStream

@Composable
fun AddChapterDialog(
  novel: NovelWithState,
  onDismiss: () -> Unit,
  onAddChapter: (novelId: String, chapterTitle: String, chapterContent: String) -> Unit,
) {
  val context = LocalContext.current
  val nextChapterNum = novel.chapters.size + 1
  val roman = toRoman(nextChapterNum)

  var chapterTitle by remember {
    mutableStateOf("Chapter $roman • ")
  }
  var chapterContent by remember { mutableStateOf("") }
  var selectedFont by remember { mutableStateOf(ManuscriptFont.SERIF) }
  var errorText by remember { mutableStateOf<String?>(null) }

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    if (uri != null) {
      try {
        val photosDir = File(context.filesDir, "story_photos").apply { mkdirs() }
        val destFile = File(photosDir, "photo_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
          FileOutputStream(destFile).use { output ->
            input.copyTo(output)
          }
        }
        val tag = "\n\n[image:${destFile.absolutePath}:Chapter Illustration]\n\n"
        chapterContent = chapterContent + tag
      } catch (e: Exception) {
        val tag = "\n\n[image:$uri:Chapter Illustration]\n\n"
        chapterContent = chapterContent + tag
      }
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = SoftCreamPaper),
      border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.5f)),
      elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 24.dp)
        .testTag("add_chapter_dialog")
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
                .clip(CircleShape)
                .background(AntiqueGold.copy(alpha = 0.16f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Outlined.NoteAdd,
                contentDescription = null,
                tint = AntiqueGold,
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "ADD NEW CHAPTER",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 8.5.sp,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 1.6.sp
                ),
                color = AntiqueGold
              )
              Text(
                text = novel.title,
                style = MaterialTheme.typography.titleMedium.copy(
                  fontFamily = FontFamily.Serif,
                  fontWeight = FontWeight.Bold,
                  fontSize = 15.sp
                ),
                color = CharcoalText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
              tint = CharcoalSecondary,
              modifier = Modifier.size(18.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = AntiqueGold.copy(alpha = 0.08f),
          border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.25f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "POSTING PER CHAPTER",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                letterSpacing = 1.sp
              ),
              color = AntiqueGold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "• Current chapters in manuscript: ${novel.chapters.size}",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
              color = CharcoalSecondary
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Chapter Title Field
        Text(
          text = "CHAPTER TITLE",
          style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
          ),
          color = AntiqueGold
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
          value = chapterTitle,
          onValueChange = {
            chapterTitle = it
            errorText = null
          },
          label = { Text("Chapter Title (e.g., Chapter $roman • The Midnight Bell)") },
          placeholder = { Text("Chapter $roman • ") },
          singleLine = true,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AntiqueGold,
            unfocusedBorderColor = SubtleBorder
          ),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_new_chapter_title")
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Chapter Content Field
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "CHAPTER CONTENT",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 9.5.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.2.sp
            ),
            color = AntiqueGold
          )

          OutlinedButton(
            onClick = {
              photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier.height(30.dp)
          ) {
            Icon(
              imageVector = Icons.Outlined.AddPhotoAlternate,
              contentDescription = null,
              tint = AntiqueGold,
              modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Embed Photo",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.5.sp,
                color = CharcoalText
              )
            )
          }
        }
        Spacer(modifier = Modifier.height(6.dp))

        // Manuscript Font Selection
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Icon(
            imageVector = Icons.Outlined.FormatSize,
            contentDescription = "Font options",
            tint = AntiqueGold,
            modifier = Modifier.size(14.dp)
          )
          Text(
            text = "Font:",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = CharcoalTertiary
            )
          )
          ManuscriptFont.entries.forEach { fontOption ->
            val isSelected = selectedFont == fontOption
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (isSelected) AntiqueGold else SoftCreamPaper,
              border = BorderStroke(1.dp, if (isSelected) AntiqueGold else SubtleBorder),
              onClick = { selectedFont = fontOption },
              modifier = Modifier.testTag("chapter_font_${fontOption.name.lowercase()}")
            ) {
              Text(
                text = fontOption.shortName,
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 9.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                  fontFamily = fontOption.fontFamily
                ),
                color = if (isSelected) Color.White else CharcoalText,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Manuscript Line Formatting Toolbar: Bold, Italic, Regular, Dialogue, Scene
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Text(
            text = "Format:",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = CharcoalTertiary
            )
          )
          // Bold Button
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = SoftCreamPaper,
            border = BorderStroke(1.dp, SubtleBorder),
            onClick = {
              chapterContent = if (chapterContent.isBlank()) "<b>Bold line</b>\n\n"
              else chapterContent.trimEnd() + "\n\n<b>Bold line</b>\n\n"
            },
            modifier = Modifier.testTag("format_bold_button")
          ) {
            Text(
              text = "<b> Bold </b>",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif
              ),
              color = CharcoalText,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }

          // Italic Button
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = SoftCreamPaper,
            border = BorderStroke(1.dp, SubtleBorder),
            onClick = {
              chapterContent = if (chapterContent.isBlank()) "<i>Italic line</i>\n\n"
              else chapterContent.trimEnd() + "\n\n<i>Italic line</i>\n\n"
            },
            modifier = Modifier.testTag("format_italic_button")
          ) {
            Text(
              text = "<i> Italic </i>",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.5.sp,
                fontStyle = FontStyle.Italic,
                fontFamily = FontFamily.Serif
              ),
              color = CharcoalText,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }

          // Regular Button
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = SoftCreamPaper,
            border = BorderStroke(1.dp, SubtleBorder),
            onClick = {
              chapterContent = if (chapterContent.isBlank()) "Regular line\n\n"
              else chapterContent.trimEnd() + "\n\nRegular line\n\n"
            },
            modifier = Modifier.testTag("format_regular_button")
          ) {
            Text(
              text = "Regular",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Normal
              ),
              color = CharcoalText,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }

          // Dialogue Quote Button
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = SoftCreamPaper,
            border = BorderStroke(1.dp, SubtleBorder),
            onClick = {
              chapterContent = chapterContent.trimEnd() + "\n\n\"Dialogue line...\"\n\n"
            },
            modifier = Modifier.testTag("format_dialogue_button")
          ) {
            Text(
              text = "Dialogue \"\"",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Medium
              ),
              color = CharcoalText,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }

          // Scene Divider Button
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = SoftCreamPaper,
            border = BorderStroke(1.dp, SubtleBorder),
            onClick = {
              chapterContent = chapterContent.trimEnd() + "\n\n❦ ❦ ❦\n\n"
            },
            modifier = Modifier.testTag("format_divider_button")
          ) {
            Text(
              text = "Scene ❦",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                color = AntiqueGold
              ),
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
          value = chapterContent,
          onValueChange = {
            chapterContent = it
            errorText = null
          },
          label = { Text("Chapter Manuscript (${selectedFont.shortName} font)") },
          textStyle = TextStyle(
            fontFamily = selectedFont.fontFamily,
            fontSize = 14.sp,
            lineHeight = 22.sp,
            color = CharcoalText
          ),
          placeholder = {
            Text(
              "Type or paste this chapter's translated manuscript paragraphs...\n\nParagraphs separated by blank lines will be formatted into continuous scroll reading.",
              style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = selectedFont.fontFamily,
                fontSize = 12.sp,
                lineHeight = 18.sp
              )
            )
          },
          minLines = 8,
          maxLines = 16,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AntiqueGold,
            unfocusedBorderColor = SubtleBorder
          ),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_new_chapter_content")
        )

        if (errorText != null) {
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = errorText!!,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
            color = Color(0xFFC74350)
          )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Action Buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically
        ) {
          TextButton(
            onClick = onDismiss,
            shape = RoundedCornerShape(10.dp)
          ) {
            Text("Cancel", color = CharcoalSecondary)
          }

          Spacer(modifier = Modifier.width(10.dp))

          Button(
            onClick = {
              if (chapterTitle.trim().isBlank()) {
                errorText = "Please provide a chapter title."
                return@Button
              }
              if (chapterContent.trim().isBlank()) {
                errorText = "Please enter the manuscript content for this chapter."
                return@Button
              }
              onAddChapter(novel.id, chapterTitle.trim(), chapterContent.trim())
              onDismiss()
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = CharcoalText
            ),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
            modifier = Modifier.testTag("confirm_publish_chapter_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Outlined.NoteAdd,
              contentDescription = null,
              tint = SoftCreamPaper,
              modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Publish Chapter",
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

private fun toRoman(number: Int): String {
  return number.toString()
}
