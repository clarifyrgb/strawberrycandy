package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.local.AuthorSlotEntity
import com.example.data.local.ReaderProfileEntity
import com.example.ui.theme.AntiqueGold
import com.example.ui.theme.CharcoalSecondary
import com.example.ui.theme.CharcoalTertiary
import com.example.ui.theme.CharcoalText
import com.example.ui.theme.DeepBurgundy
import com.example.ui.theme.SoftCreamPaper
import com.example.ui.theme.SubtleBorder
import com.example.util.EpubParser
import java.io.File
import java.io.FileOutputStream

enum class ManuscriptFont(val label: String, val shortName: String, val fontFamily: FontFamily) {
  SERIF("Classic Serif", "Serif", FontFamily.Serif),
  SANS("Modern Sans", "Sans", FontFamily.SansSerif),
  MONOSPACE("Typewriter", "Typewriter", FontFamily.Monospace),
  CURSIVE("Cursive", "Cursive", FontFamily.Cursive)
}

enum class StoryPhotoPlacement(val label: String, val chipLabel: String, val defaultCaption: String) {
  FRONT("Front Page", "Front Page", "Frontispiece Illustration"),
  MIDDLE("Middle Page", "Middle Page", "Story Scene Illustration"),
  LAST("Last Page", "Last Page", "Concluding Illustration Plate")
}

@Composable
fun OwnerUploadDialog(
  authorSlots: List<AuthorSlotEntity> = emptyList(),
  initialSlot: Int = 0,
  activeUser: ReaderProfileEntity? = null,
  isOwner: Boolean = false,
  onDismiss: () -> Unit,
  onUpdateAuthorSlot: ((slotNumber: Int, name: String, penName: String, bio: String) -> Unit)? = null,
  onPublishNovel: (
    title: String,
    subtitle: String,
    chapterTitle: String,
    excerpt: String,
    content: String,
    coverColorHex: Long,
    author: String,
    authorSlot: Int,
    coverImageUri: String?,
    originalAuthor: String,
    novelStatus: String,
    releaseFormat: String,
    onDone: ((success: Boolean, message: String) -> Unit)?,
  ) -> Unit,
) {
  val defaultSlot = if (isOwner) initialSlot else if (initialSlot > 0) initialSlot else (activeUser?.authorSlot ?: 1)
  var selectedSlot by remember(defaultSlot) { mutableIntStateOf(defaultSlot) }
  var isEditingSlotProfile by remember { mutableStateOf(false) }

  // Publication status: Ongoing vs Finished
  var isFinishedNovel by remember { mutableStateOf(false) }
  // Release format: Per Chapter vs Per Volume
  var isPerVolumeRelease by remember { mutableStateOf(false) }
  // R19 18+/19+ Mature Content Toggle
  var isR19Novel by remember { mutableStateOf(false) }

  // Active author slot data
  val currentSlotEntity = authorSlots.find { it.slotNumber == selectedSlot }
  var customPenName by remember(selectedSlot, currentSlotEntity, activeUser) {
    mutableStateOf(
      if (selectedSlot == 0 && isOwner) {
        "Strawberrycandy"
      } else {
        currentSlotEntity?.penName?.takeIf {
          it.isNotBlank() && !it.startsWith("Author ", ignoreCase = true) && !it.startsWith("Translator ", ignoreCase = true)
        } ?: ""
      }
    )
  }
  var customBio by remember(selectedSlot, currentSlotEntity) {
    mutableStateOf(currentSlotEntity?.bio?.takeIf { !it.startsWith("Contributing Translator") } ?: "")
  }

  var title by remember { mutableStateOf("") }
  var originalAuthor by remember { mutableStateOf("") }
  var subtitle by remember { mutableStateOf("") }
  var chapterTitle by remember { mutableStateOf("") }
  var excerpt by remember { mutableStateOf("") }
  var coverImageUri by remember { mutableStateOf<String?>(null) }
  var content by remember { mutableStateOf("") }
  var selectedColorHex by remember { mutableLongStateOf(0xFF5C2D3B) }
  var errorText by remember { mutableStateOf<String?>(null) }
  var isPublishing by remember { mutableStateOf(false) }
  val context = LocalContext.current

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    if (uri != null) {
      try {
        val coversDir = File(context.filesDir, "covers").apply { mkdirs() }
        val destFile = File(coversDir, "cover_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
          FileOutputStream(destFile).use { outputStream ->
            inputStream.copyTo(outputStream)
          }
        }
        coverImageUri = destFile.absolutePath
      } catch (e: Exception) {
        coverImageUri = uri.toString()
      }
    }
  }

  var isParsingEpub by remember { mutableStateOf(false) }
  var epubStatusMessage by remember { mutableStateOf<String?>(null) }
  var extractedPhotosCount by remember { mutableIntStateOf(0) }

  val epubPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri: Uri? ->
    if (uri != null) {
      isParsingEpub = true
      errorText = null
      try {
        when (val parseResult = EpubParser.parse(context, uri)) {
          is com.example.util.EpubParseResult.Success -> {
            title = parseResult.title
            subtitle = parseResult.subtitle
            chapterTitle = parseResult.chapterTitle
            excerpt = parseResult.excerpt
            content = parseResult.fullContent
            if (parseResult.author.isNotBlank()) {
              originalAuthor = parseResult.author
              if (selectedSlot != 0) {
                customPenName = parseResult.author
              }
            }
            if (parseResult.coverImageUri != null) {
              coverImageUri = parseResult.coverImageUri
            }
            extractedPhotosCount = parseResult.storyImages.size
            epubStatusMessage = "EPUB Loaded: ${parseResult.totalChapters} chapters • ${parseResult.storyImages.size} illustrations • CSS & Typography preserved"
          }
          is com.example.util.EpubParseResult.Error -> {
            errorText = parseResult.message
          }
        }
      } catch (e: Exception) {
        errorText = "Could not parse EPUB: ${e.localizedMessage ?: "Invalid file"}"
      } finally {
        isParsingEpub = false
      }
    }
  }

  var selectedFont by remember { mutableStateOf(ManuscriptFont.SERIF) }
  var targetPhotoPlacement by remember { mutableStateOf(StoryPhotoPlacement.FRONT) }

  val storyPhotoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    if (uri != null) {
      val imagePath = try {
        val storyDir = File(context.filesDir, "story_images").apply { mkdirs() }
        val destFile = File(storyDir, "story_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { inStream ->
          FileOutputStream(destFile).use { outStream ->
            inStream.copyTo(outStream)
          }
        }
        destFile.absolutePath
      } catch (e: Exception) {
        uri.toString()
      }

      val marker = "[image:$imagePath:${targetPhotoPlacement.defaultCaption}]"
      content = when (targetPhotoPlacement) {
        StoryPhotoPlacement.FRONT -> {
          if (content.isBlank()) "$marker\n\n"
          else "$marker\n\n${content.trimStart()}"
        }
        StoryPhotoPlacement.MIDDLE -> {
          val paras = content.split("\n\n").filter { it.isNotBlank() }
          if (paras.size <= 1) {
            if (paras.isEmpty()) {
              "$marker\n\n"
            } else {
              val half = (paras[0].length / 2).coerceAtLeast(0)
              paras[0].take(half) + "\n\n" + marker + "\n\n" + paras[0].substring(half)
            }
          } else {
            val mid = (paras.size + 1) / 2
            val firstHalf = paras.take(mid).joinToString("\n\n")
            val secondHalf = paras.drop(mid).joinToString("\n\n")
            firstHalf + "\n\n" + marker + "\n\n" + secondHalf
          }
        }
        StoryPhotoPlacement.LAST -> {
          if (content.isBlank()) "$marker\n\n"
          else "${content.trimEnd()}\n\n$marker\n\n"
        }
      }
      extractedPhotosCount++
    }
  }

  val colorPalettes = listOf(
    Pair(0xFF5C2D3B, "Strawberry Damask"),
    Pair(0xFF28362D, "Forest Sage"),
    Pair(0xFF21252D, "Midnight Noir"),
    Pair(0xFF4A3428, "Tuscan Ochre"),
    Pair(0xFF3B2844, "Deep Plum"),
    Pair(0xFF292929, "Royal Obsidian")
  )

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = SoftCreamPaper),
      border = BorderStroke(1.dp, SubtleBorder),
      elevation = CardDefaults.cardElevation(defaultElevation = 14.dp),
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .widthIn(max = 520.dp)
        .heightIn(max = 750.dp)
        .padding(horizontal = 4.dp, vertical = 16.dp)
        .testTag("upload_novel_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(24.dp)
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
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "STRAWBERRYCANDY • WRITERS ROOM",
              style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 1.6.sp,
                fontWeight = FontWeight.Bold
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

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = "Publish Manuscript",
          style = MaterialTheme.typography.headlineSmall.copy(
            fontFamily = FontFamily.Serif
          ),
          color = CharcoalText
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Author & Contributor Room Access Selector
        Text(
          text = if (isOwner) "AUTHOR ACCESS (OWNER & TRANSLATOR ROOMS)" else "TRANSLATOR ARCHIVE SEAT",
          style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
          ),
          color = CharcoalTertiary
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Horizontal pills for Room 0 (Owner) and 4 contributor rooms
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Owner Strawberrycandy Pill
          val isOwnerSelected = selectedSlot == 0
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .background(if (isOwnerSelected) CharcoalText else SoftCreamPaper)
              .border(
                BorderStroke(
                  1.dp,
                  if (isOwnerSelected) CharcoalText else SubtleBorder
                ),
                RoundedCornerShape(12.dp)
              )
              .clickable(enabled = isOwner) {
                selectedSlot = 0
                isEditingSlotProfile = false
              }
              .padding(horizontal = 12.dp, vertical = 8.dp)
          ) {
            Column {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = "OWNER",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                  ),
                  color = if (isOwnerSelected) AntiqueGold else CharcoalTertiary
                )
                if (!isOwner) {
                  Spacer(modifier = Modifier.width(4.dp))
                  Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = CharcoalTertiary,
                    modifier = Modifier.size(9.dp)
                  )
                }
              }
              Text(
                text = "Strawberrycandy",
                style = MaterialTheme.typography.labelMedium.copy(
                  fontWeight = FontWeight.SemiBold
                ),
                color = if (isOwnerSelected) SoftCreamPaper else CharcoalText
              )
            }
          }

          // Contributor Author Rooms: Only show active ones permitted by owner
          val activeContributorSlots = authorSlots.filter { it.slotNumber in 1..10 && it.isPermissionGranted }
          activeContributorSlots.forEach { slot ->
            val slotNum = slot.slotNumber
            val isMyAssignedSlot = isOwner || (activeUser?.authorSlot == slotNum) ||
              (activeUser?.email != null && slot.translatorEmail?.equals(activeUser.email, ignoreCase = true) == true)
            val isSlotSelected = selectedSlot == slotNum
            val slotName = slot.penName.ifBlank { "Translator $slotNum" }

            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (isSlotSelected) CharcoalText else SoftCreamPaper)
                .border(
                  BorderStroke(
                    1.dp,
                    if (isSlotSelected) AntiqueGold else SubtleBorder
                  ),
                  RoundedCornerShape(12.dp)
                )
                .clickable(enabled = isOwner || isMyAssignedSlot) {
                  selectedSlot = slotNum
                  isEditingSlotProfile = false
                }
                .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
              Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = "ROOM $slotNum",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontSize = 8.sp,
                      fontWeight = FontWeight.Bold,
                      letterSpacing = 1.sp
                    ),
                    color = if (isSlotSelected) AntiqueGold else CharcoalTertiary
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Icon(
                    imageVector = if (isMyAssignedSlot) Icons.Outlined.Key else Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = if (isSlotSelected) AntiqueGold else CharcoalTertiary,
                    modifier = Modifier.size(9.dp)
                  )
                }
                Text(
                  text = slotName,
                  style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold
                  ),
                  color = if (isSlotSelected) SoftCreamPaper else CharcoalText
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Active Selected Room Card
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = AntiqueGold.copy(alpha = 0.08f)),
          border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.25f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = if (selectedSlot == 0 && isOwner) "OWNER UPLOAD" else "TRANSLATOR ROOM $selectedSlot",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                  ),
                  color = AntiqueGold
                )
                Text(
                  text = if (selectedSlot == 0 && isOwner) "Strawberrycandy" else customPenName,
                  style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold
                  ),
                  color = CharcoalText
                )
              }

              if (selectedSlot in 1..10) {
                IconButton(
                  onClick = { isEditingSlotProfile = !isEditingSlotProfile },
                  modifier = Modifier.size(32.dp)
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "Edit Pen Name",
                    tint = CharcoalSecondary,
                    modifier = Modifier.size(16.dp)
                  )
                }
              }
            }

            if (selectedSlot in 1..10) {
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = if (customPenName.isNotBlank()) "Translator Room $selectedSlot • Pen name: '$customPenName'" else "Translator Room $selectedSlot • No pen name set yet",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = CharcoalSecondary
              )
            }
          }
        }

        // Inline edit slot pen name if toggled
        AnimatedVisibility(visible = isEditingSlotProfile && selectedSlot in 1..10) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 10.dp)
          ) {
            OutlinedTextField(
              value = customPenName,
              onValueChange = { customPenName = it },
              label = { Text("Author Pen Name") },
              singleLine = true,
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AntiqueGold,
                unfocusedBorderColor = SubtleBorder
              ),
              modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
              value = customBio,
              onValueChange = { customBio = it },
              label = { Text("Short Author Bio") },
              singleLine = true,
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AntiqueGold,
                unfocusedBorderColor = SubtleBorder
              ),
              modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedButton(
              onClick = {
                onUpdateAuthorSlot?.invoke(selectedSlot, customPenName, customPenName, customBio)
                isEditingSlotProfile = false
              },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.align(Alignment.End)
            ) {
              Text("Save Pen Name")
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // EPUB Upload Feature Card (Upload whole novel instead of per-chapter)
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(
            containerColor = if (epubStatusMessage != null) AntiqueGold.copy(alpha = 0.10f) else SoftCreamPaper
          ),
          border = BorderStroke(1.2.dp, if (epubStatusMessage != null) AntiqueGold else SubtleBorder),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
              ) {
                Box(
                  modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AntiqueGold.copy(alpha = 0.16f)),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Outlined.AutoStories,
                    contentDescription = null,
                    tint = AntiqueGold,
                    modifier = Modifier.size(20.dp)
                  )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                  Text(
                    text = "FULL NOVEL EPUB UPLOAD",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontSize = 8.5.sp,
                      fontWeight = FontWeight.Bold,
                      letterSpacing = 1.1.sp
                    ),
                    color = AntiqueGold
                  )
                  Text(
                    text = if (epubStatusMessage != null) "EPUB Unpacked & Formatted" else "Upload complete novel (.epub)",
                    style = MaterialTheme.typography.titleSmall.copy(
                      fontWeight = FontWeight.SemiBold
                    ),
                    color = CharcoalText
                  )
                }
              }

              if (isParsingEpub) {
                CircularProgressIndicator(
                  modifier = Modifier.size(22.dp),
                  color = AntiqueGold,
                  strokeWidth = 2.5.dp
                )
              } else {
                Button(
                  onClick = {
                    epubPickerLauncher.launch(
                      arrayOf(
                        "application/epub+zip",
                        "application/octet-stream",
                        "application/zip",
                        "*/*"
                      )
                    )
                  },
                  shape = RoundedCornerShape(10.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = CharcoalText),
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                  modifier = Modifier.testTag("upload_epub_button")
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Upload,
                    contentDescription = null,
                    tint = SoftCreamPaper,
                    modifier = Modifier.size(13.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = if (epubStatusMessage != null) "Replace EPUB" else "Select EPUB",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = FontWeight.SemiBold,
                      color = SoftCreamPaper
                    )
                  )
                }
              }
            }

            if (epubStatusMessage != null) {
              Spacer(modifier = Modifier.height(10.dp))
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .background(AntiqueGold.copy(alpha = 0.15f))
                  .padding(horizontal = 10.dp, vertical = 5.dp)
              ) {
                Icon(
                  imageVector = Icons.Outlined.Check,
                  contentDescription = null,
                  tint = AntiqueGold,
                  modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = epubStatusMessage!!,
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                  ),
                  color = CharcoalText
                )
              }
            } else {
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "Upload a whole .epub book. Built-in book CSS and styling are preserved: chapter breaks, text alignments, poetry/epigraph quotes, scene fleuron breaks, bold, italic, and internal illustrations are styled automatically.",
                style = MaterialTheme.typography.bodySmall.copy(
                  fontSize = 11.sp,
                  lineHeight = 15.sp
                ),
                color = CharcoalSecondary
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Novel Title Field
        OutlinedTextField(
          value = title,
          onValueChange = {
            title = it
            errorText = null
          },
          label = { Text("Novel Title (e.g., The Velvet Hour)") },
          singleLine = true,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AntiqueGold,
            unfocusedBorderColor = SubtleBorder
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_novel_title")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Author's Name Field (Who wrote the novel)
        OutlinedTextField(
          value = originalAuthor,
          onValueChange = { originalAuthor = it },
          label = { Text("Author's Name (Who wrote the novel)") },
          placeholder = { Text("e.g., Alexandre Dumas, Osamu Dazai, Jane Austen") },
          singleLine = true,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AntiqueGold,
            unfocusedBorderColor = SubtleBorder
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_novel_original_author")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Subtitle Field
        OutlinedTextField(
          value = subtitle,
          onValueChange = { subtitle = it },
          label = { Text("Subtitle / Theme (e.g., A Venetian Chronicle)") },
          singleLine = true,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AntiqueGold,
            unfocusedBorderColor = SubtleBorder
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_novel_subtitle")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Chapter I Title
        OutlinedTextField(
          value = chapterTitle,
          onValueChange = { chapterTitle = it },
          label = { Text("Chapter Title") },
          singleLine = true,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AntiqueGold,
            unfocusedBorderColor = SubtleBorder
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_novel_chapter")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Excerpt
        OutlinedTextField(
          value = excerpt,
          onValueChange = { excerpt = it },
          label = { Text("Opening Excerpt / Synopsis") },
          maxLines = 3,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AntiqueGold,
            unfocusedBorderColor = SubtleBorder
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_novel_excerpt")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Full Manuscript Content & Writing Room with Font Picker, Photo Placement, and Smooth Writing Buttons
        val wordCount = remember(content) {
          content.split(Regex("""\s+""")).count { it.isNotBlank() }
        }
        val estimatedReadMin = remember(wordCount) {
          maxOf(1, wordCount / 200)
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "MANUSCRIPT WRITING ROOM",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
              ),
              color = CharcoalTertiary
            )
          }

          Text(
            text = "$wordCount words • ~$estimatedReadMin min read",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 8.5.sp,
              color = CharcoalSecondary
            )
          )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 1. Font Selection Options for Manuscript
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
              modifier = Modifier.testTag("font_option_${fontOption.name.lowercase()}")
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

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Photo Embedding Placement Options (Front Page, Middle, Last Page)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Outlined.PhotoLibrary,
              contentDescription = null,
              tint = AntiqueGold,
              modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Embed Photo At:",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = CharcoalTertiary
              )
            )
          }

          Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            StoryPhotoPlacement.entries.forEach { placement ->
              OutlinedButton(
                onClick = {
                  targetPhotoPlacement = placement
                  storyPhotoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                  )
                },
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(0.8.dp, AntiqueGold.copy(alpha = 0.5f)),
                contentPadding = PaddingValues(horizontal = 7.dp, vertical = 2.dp),
                modifier = Modifier.testTag("embed_photo_${placement.name.lowercase()}")
              ) {
                Text(
                  text = "+ ${placement.chipLabel}",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = CharcoalText
                  )
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 3. Smooth Writing Buttons Toolbar
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Text(
            text = "Smooth Writing:",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 8.5.sp,
              fontWeight = FontWeight.Bold,
              color = CharcoalTertiary
            )
          )

          // Quick Insert Chapter Break
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = SoftCreamPaper,
            border = BorderStroke(0.7.dp, SubtleBorder),
            onClick = {
              content = content.trimEnd() + "\n\n[chapter:New Chapter]\n\n"
            }
          ) {
            Text(
              text = "+ Chapter Break",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.sp,
                fontWeight = FontWeight.Medium,
                color = DeepBurgundy
              ),
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
            )
          }

          // Paragraph Break
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = SoftCreamPaper,
            border = BorderStroke(0.7.dp, SubtleBorder),
            onClick = {
              content = "$content\n\n"
            }
          ) {
            Text(
              text = "Paragraph §",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.sp,
                fontWeight = FontWeight.Medium,
                color = CharcoalText
              ),
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
            )
          }

          // Dialogue Quotes
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = SoftCreamPaper,
            border = BorderStroke(0.7.dp, SubtleBorder),
            onClick = {
              content = "$content\"\""
            }
          ) {
            Text(
              text = "Dialogue \"\"",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.sp,
                fontWeight = FontWeight.Medium,
                color = CharcoalText
              ),
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
            )
          }

          // Scene Divider
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = SoftCreamPaper,
            border = BorderStroke(0.7.dp, SubtleBorder),
            onClick = {
              content = content.trimEnd() + "\n\n❦ ❦ ❦\n\n"
            }
          ) {
            Text(
              text = "Scene Divider ❦",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.sp,
                fontWeight = FontWeight.Medium,
                color = AntiqueGold
              ),
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
            )
          }
        }

        if (extractedPhotosCount > 0) {
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "✨ $extractedPhotosCount story illustration(s) embedded in manuscript",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 8.sp,
              color = Color(0xFF2E7D32),
              fontWeight = FontWeight.Medium
            )
          )
        }

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
          value = content,
          onValueChange = {
            content = it
            errorText = null
          },
          label = { Text("Manuscript Content (${selectedFont.shortName} font)") },
          textStyle = TextStyle(
            fontFamily = selectedFont.fontFamily,
            fontSize = 14.sp,
            lineHeight = 22.sp,
            color = CharcoalText
          ),
          minLines = 5,
          maxLines = 10,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AntiqueGold,
            unfocusedBorderColor = SubtleBorder
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_novel_content")
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Novel Cover Upload Section
        Text(
          text = "NOVEL COVER ART",
          style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
          ),
          color = CharcoalTertiary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = AntiqueGold.copy(alpha = 0.07f)),
          border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.35f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Live Cover Thumbnail Preview
            Box(
              modifier = Modifier
                .width(68.dp)
                .height(96.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (coverImageUri != null) Color.Transparent else Color(selectedColorHex))
                .border(BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.45f)), RoundedCornerShape(8.dp)),
              contentAlignment = Alignment.Center
            ) {
              if (coverImageUri != null) {
                AsyncImage(
                  model = File(coverImageUri!!).takeIf { it.exists() } ?: coverImageUri,
                  contentDescription = "Uploaded Cover Preview",
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxSize()
                )
              } else {
                Column(
                  modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp),
                  verticalArrangement = Arrangement.SpaceBetween,
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  Text(
                    text = if (selectedSlot > 0) "ROOM $selectedSlot" else "STRAWBERRY",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontSize = 6.sp,
                      color = Color(0xD0E5C17D),
                      fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center
                  )
                  Icon(
                    imageVector = Icons.Outlined.AddPhotoAlternate,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                  )
                  Text(
                    text = "PALETTE",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontSize = 6.sp,
                      color = Color.White.copy(alpha = 0.7f)
                    )
                  )
                }
              }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = if (coverImageUri != null) "Custom Cover Attached" else "Upload Cover Art",
                style = MaterialTheme.typography.titleSmall.copy(
                  fontWeight = FontWeight.Bold
                ),
                color = CharcoalText
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = if (coverImageUri != null)
                  "Cover art selected. Translators can replace or remove it before publishing."
                else
                  "Upload custom cover artwork from device, or select a classic hardcover palette below.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = CharcoalSecondary
              )
              Spacer(modifier = Modifier.height(10.dp))

              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                  onClick = {
                    photoPickerLauncher.launch(
                      PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                  },
                  shape = RoundedCornerShape(10.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = CharcoalText),
                  contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                  modifier = Modifier.testTag("upload_cover_button")
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Upload,
                    contentDescription = null,
                    tint = SoftCreamPaper,
                    modifier = Modifier.size(13.dp)
                  )
                  Spacer(modifier = Modifier.width(5.dp))
                  Text(
                    text = if (coverImageUri != null) "Change Cover" else "Upload Cover",
                    style = MaterialTheme.typography.labelSmall.copy(
                      color = SoftCreamPaper,
                      fontWeight = FontWeight.SemiBold
                    )
                  )
                }

                if (coverImageUri != null) {
                  OutlinedButton(
                    onClick = { coverImageUri = null },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                  ) {
                    Text(
                      text = "Remove",
                      style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp)
                    )
                  }
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Binding Color Palette Picker
        Text(
          text = "HARDCOVER BINDING PALETTE",
          style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
          ),
          color = CharcoalTertiary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          colorPalettes.forEach { (colorHex, name) ->
            val isSelected = selectedColorHex == colorHex
            Box(
              modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(colorHex))
                .border(
                  BorderStroke(
                    width = if (isSelected) 2.5.dp else 1.dp,
                    color = if (isSelected) AntiqueGold else Color.White.copy(alpha = 0.4f)
                  ),
                  shape = CircleShape
                )
                .clickable { selectedColorHex = colorHex },
              contentAlignment = Alignment.Center
            ) {
              if (isSelected) {
                Icon(
                  imageVector = Icons.Outlined.Check,
                  contentDescription = name,
                  tint = Color.White,
                  modifier = Modifier.size(16.dp)
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Novel Status (Ongoing vs Finished) & Release Format (Chapter vs Volume) Toggles
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = AntiqueGold.copy(alpha = 0.07f),
          border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.25f)),
          modifier = Modifier.fillMaxWidth().testTag("upload_status_and_format_card")
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
                  modifier = Modifier.weight(1f).testTag("status_toggle_ongoing")
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
                  modifier = Modifier.weight(1f).testTag("status_toggle_finished")
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
                  modifier = Modifier.weight(1f).testTag("format_toggle_chapter")
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
                  modifier = Modifier.weight(1f).testTag("format_toggle_volume")
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

            // 3. R19 Age Restriction Toggle (Turns red when active)
            Column {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "MATURE CONTENT RATING",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = if (isR19Novel) Color(0xFFD32F2F) else AntiqueGold
                  )
                )
                Text(
                  text = if (isR19Novel) "⚠ R19 Restricted Content" else "General Audience",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isR19Novel) Color(0xFFD32F2F) else CharcoalSecondary
                  )
                )
              }
              Spacer(modifier = Modifier.height(6.dp))
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(10.dp))
                  .background(SoftCreamPaper)
                  .border(
                    BorderStroke(
                      1.dp,
                      if (isR19Novel) Color(0xFFEF5350) else SubtleBorder
                    ),
                    shape = RoundedCornerShape(10.dp)
                  )
                  .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Surface(
                  onClick = { isR19Novel = false },
                  shape = RoundedCornerShape(8.dp),
                  color = if (!isR19Novel) CharcoalText else Color.Transparent,
                  modifier = Modifier.weight(1f).testTag("r19_toggle_standard")
                ) {
                  Text(
                    text = "Standard (All Ages)",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = if (!isR19Novel) FontWeight.Bold else FontWeight.Normal,
                      fontSize = 10.sp
                    ),
                    color = if (!isR19Novel) SoftCreamPaper else CharcoalSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 7.dp)
                  )
                }
                Surface(
                  onClick = { isR19Novel = true },
                  shape = RoundedCornerShape(8.dp),
                  color = if (isR19Novel) Color(0xFFD32F2F) else Color.Transparent,
                  modifier = Modifier.weight(1f).testTag("r19_toggle_active")
                ) {
                  Text(
                    text = "🔴 R19 Mature",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = if (isR19Novel) FontWeight.Bold else FontWeight.Normal,
                      fontSize = 10.sp
                    ),
                    color = if (isR19Novel) Color.White else Color(0xFFD32F2F),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 7.dp)
                  )
                }
              }
            }
          }
        }

        val emailRegex = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
        val cleanCustomPenName = customPenName.replace(emailRegex, "").trim().let {
          if (it.startsWith("Translator ", ignoreCase = true) || it.startsWith("Author ", ignoreCase = true)) "" else it
        }
        val authorToCredit = if (selectedSlot == 0 && isOwner) {
          "Strawberrycandy"
        } else {
          cleanCustomPenName.ifBlank {
            activeUser?.displayName?.takeIf { it.isNotBlank() && !it.startsWith("Translator ", ignoreCase = true) } ?: ""
          }
        }
        val cleanOriginalAuthor = originalAuthor.replace(emailRegex, "").trim().ifEmpty { authorToCredit }

        Spacer(modifier = Modifier.height(16.dp))

        // Direct visible error feedback above the publish button
        if (errorText != null) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFFFDEDEC),
            border = BorderStroke(1.dp, Color(0xFFEF9A9A)),
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 12.dp)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = Color(0xFFC62828),
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = errorText ?: "",
                color = Color(0xFFC62828),
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
              )
            }
          }
        }

        // Publish Button
        Button(
          onClick = {
            if (selectedSlot > 0 && authorToCredit.isBlank()) {
              errorText = "Please set your translator pen name before publishing."
              return@Button
            }
            if (title.isBlank()) {
              errorText = "Please enter a novel title before publishing."
              return@Button
            }
            if (content.isBlank()) {
              errorText = "Please enter manuscript text or import an EPUB/TXT file."
              return@Button
            }
            errorText = null
            isPublishing = true
            onPublishNovel(
              title.trim(),
              subtitle.trim(),
              chapterTitle.trim().ifBlank { "Chapter 1: Prologue" },
              excerpt.trim().ifBlank { if (content.trim().length > 120) content.trim().take(120) + "..." else content.trim() },
              content.trim(),
              selectedColorHex,
              authorToCredit,
              selectedSlot,
              coverImageUri,
              cleanOriginalAuthor,
              if (isFinishedNovel) "FINISHED" else "ONGOING",
              if (isPerVolumeRelease) "VOLUME" else "CHAPTER"
            ) { success, message ->
              isPublishing = false
              if (!success) {
                errorText = message
              }
            }
          },
          enabled = !isPublishing,
          shape = RoundedCornerShape(16.dp),
          colors = ButtonDefaults.buttonColors(containerColor = CharcoalText),
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("publish_novel_button")
        ) {
          if (isPublishing) {
            CircularProgressIndicator(
              modifier = Modifier.size(18.dp),
              color = SoftCreamPaper,
              strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "Publishing Manuscript...",
              maxLines = 1,
              softWrap = false,
              style = MaterialTheme.typography.labelLarge.copy(
                letterSpacing = 0.2.sp,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
              ),
              color = SoftCreamPaper
            )
          } else {
            Icon(
              imageVector = Icons.Outlined.Upload,
              contentDescription = null,
              tint = SoftCreamPaper,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Publish Manuscript",
              maxLines = 1,
              softWrap = false,
              style = MaterialTheme.typography.labelLarge.copy(
                letterSpacing = 0.2.sp,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
              ),
              color = SoftCreamPaper
            )
          }
        }
      }
    }
  }
}
