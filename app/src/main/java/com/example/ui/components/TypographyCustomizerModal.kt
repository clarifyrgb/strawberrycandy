package com.example.ui.components

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FontDownload
import androidx.compose.material.icons.outlined.FormatAlignJustify
import androidx.compose.material.icons.outlined.FormatAlignLeft
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatLineSpacing
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.AntiqueGold
import com.example.ui.theme.CharcoalSecondary
import com.example.ui.theme.CharcoalTertiary
import com.example.ui.theme.CharcoalText
import com.example.ui.theme.SoftCreamPaper
import com.example.ui.theme.SubtleBorder
import java.io.File
import java.io.FileOutputStream

@Composable
fun TypographyCustomizerModal(
  currentFontType: String,
  customFontName: String?,
  fontSizeScale: Float,
  lineHeightScale: Float,
  pageTurnMode: String = "flip", // "flip" or "scroll"
  paragraphSpacingScale: Float = 1.0f,
  isJustified: Boolean = false,
  isFirstLineIndent: Boolean = false,
  isBold: Boolean = false,
  isItalic: Boolean = false,
  readingTheme: String = "light", // "light", "dark", "sepia"
  onSelectReadingTheme: (String) -> Unit = {},
  onSelectFontType: (String) -> Unit,
  onCustomFontUploaded: (filePath: String, fontName: String) -> Unit,
  onUpdateFontSizeScale: (Float) -> Unit,
  onUpdateLineHeightScale: (Float) -> Unit,
  onSelectPageTurnMode: (String) -> Unit = {},
  onUpdateParagraphSpacingScale: (Float) -> Unit = {},
  onToggleJustified: (Boolean) -> Unit = {},
  onToggleFirstLineIndent: (Boolean) -> Unit = {},
  onToggleBold: (Boolean) -> Unit = {},
  onToggleItalic: (Boolean) -> Unit = {},
  onSelectFontStyle: (isBold: Boolean, isItalic: Boolean) -> Unit = { _, _ -> },
  onResetDefaults: () -> Unit,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  var uploadStatusMessage by remember { mutableStateOf<String?>(null) }
  var isError by remember { mutableStateOf(false) }

  val fontPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri: Uri? ->
    if (uri != null) {
      try {
        val fontsDir = File(context.filesDir, "custom_fonts").apply { mkdirs() }
        var detectedName = "custom_font_${System.currentTimeMillis()}.ttf"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
          val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
          if (nameIndex != -1 && cursor.moveToFirst()) {
            detectedName = cursor.getString(nameIndex) ?: detectedName
          }
        }

        val destFile = File(fontsDir, detectedName)
        context.contentResolver.openInputStream(uri)?.use { inStream ->
          FileOutputStream(destFile).use { outStream ->
            inStream.copyTo(outStream)
          }
        }

        val friendlyName = detectedName.substringBeforeLast(".")
        onCustomFontUploaded(destFile.absolutePath, friendlyName)
        uploadStatusMessage = "Applied '$friendlyName' successfully!"
        isError = false
      } catch (e: Exception) {
        uploadStatusMessage = "Font load failed: ${e.localizedMessage ?: "Invalid file"}"
        isError = true
      }
    }
  }

  val isDarkModal = readingTheme == "dark"
  val modalBgColor = if (isDarkModal) Color(0xFF1E1C1A) else SoftCreamPaper
  val modalBorderColor = if (isDarkModal) Color(0xFF38332E) else SubtleBorder
  val modalTextColor = if (isDarkModal) Color(0xFFE8E2D9) else CharcoalText
  val modalSecondaryTextColor = if (isDarkModal) Color(0xFFA8A099) else CharcoalSecondary
  val modalTertiaryTextColor = if (isDarkModal) Color(0xFF7A736C) else CharcoalTertiary

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(26.dp),
      colors = CardDefaults.cardColors(containerColor = modalBgColor),
      border = BorderStroke(1.2.dp, modalBorderColor),
      elevation = CardDefaults.cardElevation(defaultElevation = 18.dp),
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 4.dp, vertical = 16.dp)
        .testTag("typography_customizer_modal")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(22.dp)
      ) {
        // Modal Header
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
                .background(Color(0x18D4AF37)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Outlined.FontDownload,
                contentDescription = null,
                tint = AntiqueGold,
                modifier = Modifier.size(19.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "READING EXPERIENCE",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 8.5.sp,
                  letterSpacing = 1.8.sp,
                  fontWeight = FontWeight.Bold
                ),
                color = AntiqueGold
              )
              Text(
                text = "Appearance & Typography",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontFamily = FontFamily.Serif,
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 15.sp
                ),
                color = modalTextColor
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
              tint = modalSecondaryTextColor,
              modifier = Modifier.size(18.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Scrollable Options Body
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f, fill = false)
            .heightIn(max = 480.dp)
            .verticalScroll(rememberScrollState())
        ) {
          // 0. SECTION: READING THEME / DARK MODE
          Text(
            text = "APPEARANCE & READING THEME",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 9.sp,
              letterSpacing = 1.4.sp,
              fontWeight = FontWeight.Bold
            ),
            color = modalTertiaryTextColor
          )
          Spacer(modifier = Modifier.height(8.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            // Option 1: Light Paper
            val isLightSelected = readingTheme == "light"
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = if (isLightSelected) AntiqueGold.copy(alpha = 0.15f) else if (isDarkModal) Color(0xFF282522) else Color(0xFFFBF8F2),
              border = BorderStroke(if (isLightSelected) 1.5.dp else 1.dp, if (isLightSelected) AntiqueGold else if (isDarkModal) Color(0xFF38332E) else SubtleBorder),
              modifier = Modifier
                .weight(1f)
                .clickable { onSelectReadingTheme("light") }
                .testTag("theme_option_light")
            ) {
              Column(
                modifier = Modifier.padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Box(
                  modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFDFBF7))
                    .border(1.dp, Color(0xFFE0D8CB), CircleShape),
                  contentAlignment = Alignment.Center
                ) {
                  Text("☀️", fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = "Light Paper",
                  fontSize = 11.sp,
                  fontWeight = if (isLightSelected) FontWeight.Bold else FontWeight.Medium,
                  color = if (isLightSelected) AntiqueGold else modalTextColor,
                  textAlign = TextAlign.Center
                )
              }
            }

            // Option 2: Midnight Dark (Dark Mode)
            val isDarkSelected = readingTheme == "dark"
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = if (isDarkSelected) AntiqueGold.copy(alpha = 0.22f) else if (isDarkModal) Color(0xFF282522) else Color(0xFFFBF8F2),
              border = BorderStroke(if (isDarkSelected) 1.5.dp else 1.dp, if (isDarkSelected) AntiqueGold else if (isDarkModal) Color(0xFF38332E) else SubtleBorder),
              modifier = Modifier
                .weight(1f)
                .clickable { onSelectReadingTheme("dark") }
                .testTag("theme_option_dark")
            ) {
              Column(
                modifier = Modifier.padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Box(
                  modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF141211))
                    .border(1.dp, if (isDarkSelected) AntiqueGold else Color(0xFF44403C), CircleShape),
                  contentAlignment = Alignment.Center
                ) {
                  Text("🌙", fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = "Dark Mode",
                  fontSize = 11.sp,
                  fontWeight = if (isDarkSelected) FontWeight.Bold else FontWeight.Medium,
                  color = if (isDarkSelected) AntiqueGold else modalTextColor,
                  textAlign = TextAlign.Center
                )
              }
            }

            // Option 3: Warm Sepia
            val isSepiaSelected = readingTheme == "sepia"
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = if (isSepiaSelected) AntiqueGold.copy(alpha = 0.15f) else if (isDarkModal) Color(0xFF282522) else Color(0xFFFBF8F2),
              border = BorderStroke(if (isSepiaSelected) 1.5.dp else 1.dp, if (isSepiaSelected) AntiqueGold else if (isDarkModal) Color(0xFF38332E) else SubtleBorder),
              modifier = Modifier
                .weight(1f)
                .clickable { onSelectReadingTheme("sepia") }
                .testTag("theme_option_sepia")
            ) {
              Column(
                modifier = Modifier.padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Box(
                  modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF4ECD8))
                    .border(1.dp, Color(0xFFD8CCB0), CircleShape),
                  contentAlignment = Alignment.Center
                ) {
                  Text("📜", fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = "Warm Sepia",
                  fontSize = 11.sp,
                  fontWeight = if (isSepiaSelected) FontWeight.Bold else FontWeight.Medium,
                  color = if (isSepiaSelected) AntiqueGold else modalTextColor,
                  textAlign = TextAlign.Center
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            // Option 4: Soft Pink
            val isPinkSelected = readingTheme == "pink"
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = if (isPinkSelected) AntiqueGold.copy(alpha = 0.15f) else if (isDarkModal) Color(0xFF282522) else Color(0xFFFBF8F2),
              border = BorderStroke(if (isPinkSelected) 1.5.dp else 1.dp, if (isPinkSelected) AntiqueGold else if (isDarkModal) Color(0xFF38332E) else SubtleBorder),
              modifier = Modifier
                .weight(1f)
                .clickable { onSelectReadingTheme("pink") }
                .testTag("theme_option_pink")
            ) {
              Column(
                modifier = Modifier.padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Box(
                  modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFCE4EC))
                    .border(1.dp, Color(0xFFF48FB1), CircleShape),
                  contentAlignment = Alignment.Center
                ) {
                  Text("🌸", fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = "Soft Pink",
                  fontSize = 11.sp,
                  fontWeight = if (isPinkSelected) FontWeight.Bold else FontWeight.Medium,
                  color = if (isPinkSelected) AntiqueGold else modalTextColor,
                  textAlign = TextAlign.Center
                )
              }
            }

            // Option 5: Soft Blue
            val isBlueSelected = readingTheme == "blue"
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = if (isBlueSelected) AntiqueGold.copy(alpha = 0.15f) else if (isDarkModal) Color(0xFF282522) else Color(0xFFFBF8F2),
              border = BorderStroke(if (isBlueSelected) 1.5.dp else 1.dp, if (isBlueSelected) AntiqueGold else if (isDarkModal) Color(0xFF38332E) else SubtleBorder),
              modifier = Modifier
                .weight(1f)
                .clickable { onSelectReadingTheme("blue") }
                .testTag("theme_option_blue")
            ) {
              Column(
                modifier = Modifier.padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Box(
                  modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE3F2FD))
                    .border(1.dp, Color(0xFF90CAF9), CircleShape),
                  contentAlignment = Alignment.Center
                ) {
                  Text("🌊", fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = "Soft Blue",
                  fontSize = 11.sp,
                  fontWeight = if (isBlueSelected) FontWeight.Bold else FontWeight.Medium,
                  color = if (isBlueSelected) AntiqueGold else modalTextColor,
                  textAlign = TextAlign.Center
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(18.dp))

          // 1. SECTION: PAGE TURNING STYLE
          Text(
            text = "PAGE TURNING STYLE",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 9.sp,
              letterSpacing = 1.4.sp,
              fontWeight = FontWeight.Bold
            ),
            color = CharcoalTertiary
          )
          Spacer(modifier = Modifier.height(8.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            // Continuous Scroll Mode
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = Color(0x1AD4AF37),
              border = BorderStroke(1.5.dp, AntiqueGold),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("page_mode_scroll")
            ) {
              Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(AntiqueGold),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Outlined.SwapVert,
                    contentDescription = null,
                    tint = SoftCreamPaper,
                    modifier = Modifier.size(20.dp)
                  )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "Smooth Vertical Scroll Mode",
                    style = MaterialTheme.typography.titleSmall.copy(
                      fontSize = 13.sp,
                      fontWeight = FontWeight.SemiBold
                    ),
                    color = CharcoalText
                  )
                  Text(
                    text = "Fluid continuous reading with instant chapter navigation",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                    color = CharcoalSecondary
                  )
                }
                Icon(
                  imageVector = Icons.Outlined.Check,
                  contentDescription = null,
                  tint = AntiqueGold,
                  modifier = Modifier.size(18.dp)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(18.dp))

          // 2. SECTION: FONT SIZE (BIGGER / SMALLER)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "FONT SIZE (ADJUST BIGGER OR SMALLER)",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                letterSpacing = 1.4.sp,
                fontWeight = FontWeight.Bold
              ),
              color = CharcoalTertiary
            )
            Text(
              text = "${(fontSizeScale * 100).toInt()}%",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = AntiqueGold
              )
            )
          }
          Spacer(modifier = Modifier.height(8.dp))

          // Stepper: Smaller [-] and [+] Bigger controls
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            // Make Smaller Button
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = Color(0x0C000000),
              border = BorderStroke(1.dp, SubtleBorder),
              modifier = Modifier
                .weight(1f)
                .clickable {
                  val newScale = (fontSizeScale - 0.10f).coerceIn(0.80f, 1.50f)
                  onUpdateFontSizeScale(newScale)
                }
                .testTag("font_size_smaller")
            ) {
              Row(
                modifier = Modifier.padding(vertical = 9.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Outlined.Remove,
                  contentDescription = "Make font smaller",
                  tint = CharcoalText,
                  modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "Smaller",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold
                  ),
                  color = CharcoalText
                )
              }
            }

            // Make Bigger Button
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = Color(0x16D4AF37),
              border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.7f)),
              modifier = Modifier
                .weight(1f)
                .clickable {
                  val newScale = (fontSizeScale + 0.10f).coerceIn(0.80f, 1.50f)
                  onUpdateFontSizeScale(newScale)
                }
                .testTag("font_size_bigger")
            ) {
              Row(
                modifier = Modifier.padding(vertical = 9.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Outlined.Add,
                  contentDescription = "Make font bigger",
                  tint = AntiqueGold,
                  modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "Bigger",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                  ),
                  color = AntiqueGold
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Preset Chips
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            listOf(
              Triple(0.85f, "Small", "14sp"),
              Triple(1.0f, "Standard", "17.5sp"),
              Triple(1.15f, "Large", "20sp"),
              Triple(1.35f, "Editorial", "24sp")
            ).forEach { (scale, label, _) ->
              val isSelected = kotlin.math.abs(fontSizeScale - scale) < 0.06f
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) AntiqueGold else Color(0x0C000000),
                border = BorderStroke(1.dp, if (isSelected) AntiqueGold else SubtleBorder),
                modifier = Modifier
                  .weight(1f)
                  .clickable { onUpdateFontSizeScale(scale) }
              ) {
                Text(
                  text = label,
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.5.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                  ),
                  color = if (isSelected) SoftCreamPaper else CharcoalText,
                  modifier = Modifier.padding(vertical = 7.dp),
                  textAlign = TextAlign.Center
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(18.dp))

          // 3. SECTION: FONT WEIGHT & STYLE (BOLD, ITALIC, REGULAR)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "FONT WEIGHT & STYLE (BOLD, ITALIC, REGULAR)",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                letterSpacing = 1.4.sp,
                fontWeight = FontWeight.Bold
              ),
              color = CharcoalTertiary
            )
            val currentStyleName = when {
              isBold && isItalic -> "Bold Italic"
              isBold -> "Bold"
              isItalic -> "Italic"
              else -> "Regular"
            }
            Text(
              text = currentStyleName,
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = AntiqueGold
              )
            )
          }
          Spacer(modifier = Modifier.height(8.dp))

          // Primary Style Cards (Regular, Bold, Italic, Bold Italic)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            // 1. Regular Font Option
            val isRegularSelected = !isBold && !isItalic
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (isRegularSelected) AntiqueGold else Color(0x0C000000),
              border = BorderStroke(1.dp, if (isRegularSelected) AntiqueGold else SubtleBorder),
              modifier = Modifier
                .weight(1f)
                .clickable { onSelectFontStyle(false, false) }
                .testTag("font_style_regular")
            ) {
              Column(
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Text(
                  text = "Aa",
                  style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    fontStyle = FontStyle.Normal
                  ),
                  color = if (isRegularSelected) SoftCreamPaper else CharcoalText
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "Regular",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = if (isRegularSelected) FontWeight.Bold else FontWeight.Normal
                  ),
                  color = if (isRegularSelected) SoftCreamPaper else CharcoalSecondary
                )
              }
            }

            // 2. Bold Font Option
            val isBoldSelected = isBold && !isItalic
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (isBoldSelected) AntiqueGold else Color(0x0C000000),
              border = BorderStroke(1.dp, if (isBoldSelected) AntiqueGold else SubtleBorder),
              modifier = Modifier
                .weight(1f)
                .clickable { onSelectFontStyle(true, false) }
                .testTag("font_style_bold")
            ) {
              Column(
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Text(
                  text = "Aa",
                  style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Normal
                  ),
                  color = if (isBoldSelected) SoftCreamPaper else CharcoalText
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "Bold",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = if (isBoldSelected) FontWeight.Bold else FontWeight.Normal
                  ),
                  color = if (isBoldSelected) SoftCreamPaper else CharcoalSecondary
                )
              }
            }

            // 3. Italic Font Option
            val isItalicSelected = !isBold && isItalic
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (isItalicSelected) AntiqueGold else Color(0x0C000000),
              border = BorderStroke(1.dp, if (isItalicSelected) AntiqueGold else SubtleBorder),
              modifier = Modifier
                .weight(1f)
                .clickable { onSelectFontStyle(false, true) }
                .testTag("font_style_italic")
            ) {
              Column(
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Text(
                  text = "Aa",
                  style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    fontStyle = FontStyle.Italic
                  ),
                  color = if (isItalicSelected) SoftCreamPaper else CharcoalText
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "Italic",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = if (isItalicSelected) FontWeight.Bold else FontWeight.Normal
                  ),
                  color = if (isItalicSelected) SoftCreamPaper else CharcoalSecondary
                )
              }
            }

            // 4. Bold Italic Option
            val isBoldItalicSelected = isBold && isItalic
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (isBoldItalicSelected) AntiqueGold else Color(0x0C000000),
              border = BorderStroke(1.dp, if (isBoldItalicSelected) AntiqueGold else SubtleBorder),
              modifier = Modifier
                .weight(1f)
                .clickable { onSelectFontStyle(true, true) }
                .testTag("font_style_bold_italic")
            ) {
              Column(
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Text(
                  text = "Aa",
                  style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic
                  ),
                  color = if (isBoldItalicSelected) SoftCreamPaper else CharcoalText
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "Bold Italic",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = if (isBoldItalicSelected) FontWeight.Bold else FontWeight.Normal
                  ),
                  color = if (isBoldItalicSelected) SoftCreamPaper else CharcoalSecondary
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Granular Modifier Quick Toggles
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            // Bold Toggle Pill
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = if (isBold) Color(0x1AD4AF37) else Color(0x0A000000),
              border = BorderStroke(1.dp, if (isBold) AntiqueGold else SubtleBorder),
              modifier = Modifier
                .weight(1f)
                .clickable { onToggleBold(!isBold) }
                .testTag("toggle_bold_modifier")
            ) {
              Row(
                modifier = Modifier.padding(vertical = 7.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Outlined.FormatBold,
                  contentDescription = "Toggle bold font",
                  tint = if (isBold) AntiqueGold else CharcoalSecondary,
                  modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = if (isBold) "Bold: ON" else "Bold: OFF",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                  ),
                  color = if (isBold) AntiqueGold else CharcoalText
                )
              }
            }

            // Italic Toggle Pill
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = if (isItalic) Color(0x1AD4AF37) else Color(0x0A000000),
              border = BorderStroke(1.dp, if (isItalic) AntiqueGold else SubtleBorder),
              modifier = Modifier
                .weight(1f)
                .clickable { onToggleItalic(!isItalic) }
                .testTag("toggle_italic_modifier")
            ) {
              Row(
                modifier = Modifier.padding(vertical = 7.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Outlined.FormatItalic,
                  contentDescription = "Toggle italic font",
                  tint = if (isItalic) AntiqueGold else CharcoalSecondary,
                  modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = if (isItalic) "Italic: ON" else "Italic: OFF",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontStyle = FontStyle.Italic
                  ),
                  color = if (isItalic) AntiqueGold else CharcoalText
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(18.dp))

          // 4. SECTION: PARAGRAPH LINING (LINE SPACING)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "PARAGRAPH LINING (LINE SPACING)",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                letterSpacing = 1.4.sp,
                fontWeight = FontWeight.Bold
              ),
              color = CharcoalTertiary
            )
            Icon(
              imageVector = Icons.Outlined.FormatLineSpacing,
              contentDescription = null,
              tint = CharcoalSecondary,
              modifier = Modifier.size(14.dp)
            )
          }
          Spacer(modifier = Modifier.height(8.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            listOf(
              Pair(0.85f, "Compact"),
              Pair(1.0f, "Balanced"),
              Pair(1.20f, "Relaxed"),
              Pair(1.40f, "Spacious")
            ).forEach { (scale, label) ->
              val isSelected = kotlin.math.abs(lineHeightScale - scale) < 0.06f
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) CharcoalText else Color(0x0C000000),
                border = BorderStroke(1.dp, if (isSelected) CharcoalText else SubtleBorder),
                modifier = Modifier
                  .weight(1f)
                  .clickable { onUpdateLineHeightScale(scale) }
              ) {
                Text(
                  text = label,
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.5.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                  ),
                  color = if (isSelected) SoftCreamPaper else CharcoalText,
                  modifier = Modifier.padding(vertical = 7.dp),
                  textAlign = TextAlign.Center
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(18.dp))

          // 4. SECTION: PARAGRAPH ALIGNMENT & INDENT
          Text(
            text = "PARAGRAPH FORMAT & ALIGNMENT",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 9.sp,
              letterSpacing = 1.4.sp,
              fontWeight = FontWeight.Bold
            ),
            color = CharcoalTertiary
          )
          Spacer(modifier = Modifier.height(8.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            // Left Aligned
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = if (!isJustified) Color(0x1AD4AF37) else Color(0x0A000000),
              border = BorderStroke(1.dp, if (!isJustified) AntiqueGold else SubtleBorder),
              modifier = Modifier
                .weight(1f)
                .clickable { onToggleJustified(false) }
            ) {
              Row(
                modifier = Modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Outlined.FormatAlignLeft,
                  contentDescription = null,
                  tint = if (!isJustified) AntiqueGold else CharcoalSecondary,
                  modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "Left-Aligned",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = if (!isJustified) FontWeight.Bold else FontWeight.Normal
                  ),
                  color = if (!isJustified) AntiqueGold else CharcoalText
                )
              }
            }

            // Justified
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = if (isJustified) Color(0x1AD4AF37) else Color(0x0A000000),
              border = BorderStroke(1.dp, if (isJustified) AntiqueGold else SubtleBorder),
              modifier = Modifier
                .weight(1f)
                .clickable { onToggleJustified(true) }
            ) {
              Row(
                modifier = Modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Outlined.FormatAlignJustify,
                  contentDescription = null,
                  tint = if (isJustified) AntiqueGold else CharcoalSecondary,
                  modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "Justified",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = if (isJustified) FontWeight.Bold else FontWeight.Normal
                  ),
                  color = if (isJustified) AntiqueGold else CharcoalText
                )
              }
            }

            // First Line Indent Toggle
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = if (isFirstLineIndent) Color(0x1AD4AF37) else Color(0x0A000000),
              border = BorderStroke(1.dp, if (isFirstLineIndent) AntiqueGold else SubtleBorder),
              modifier = Modifier
                .weight(1f)
                .clickable { onToggleFirstLineIndent(!isFirstLineIndent) }
            ) {
              Row(
                modifier = Modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = if (isFirstLineIndent) "Indent: ON" else "Indent: OFF",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = if (isFirstLineIndent) FontWeight.Bold else FontWeight.Normal
                  ),
                  color = if (isFirstLineIndent) AntiqueGold else CharcoalText
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(18.dp))

          // 5. SECTION: FONT FAMILY SELECTION
          Text(
            text = "FONT FAMILY",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 9.sp,
              letterSpacing = 1.4.sp,
              fontWeight = FontWeight.Bold
            ),
            color = CharcoalTertiary
          )
          Spacer(modifier = Modifier.height(8.dp))

          val activeFontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
          val activeFontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal

          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FontOptionRow(
              title = "Classic Serif",
              subtitle = "Editorial book typography • Recommended",
              isSelected = currentFontType == "serif",
              fontFamily = FontFamily.Serif,
              fontWeight = activeFontWeight,
              fontStyle = activeFontStyle,
              onClick = { onSelectFontType("serif") }
            )

            FontOptionRow(
              title = "Modern Sans",
              subtitle = "Clean geometric readability",
              isSelected = currentFontType == "sans",
              fontFamily = FontFamily.SansSerif,
              fontWeight = activeFontWeight,
              fontStyle = activeFontStyle,
              onClick = { onSelectFontType("sans") }
            )

            FontOptionRow(
              title = "Literary Monospace",
              subtitle = "Manuscript typewriter aesthetic",
              isSelected = currentFontType == "mono",
              fontFamily = FontFamily.Monospace,
              fontWeight = activeFontWeight,
              fontStyle = activeFontStyle,
              onClick = { onSelectFontType("mono") }
            )

            FontOptionRow(
              title = "Romantic Cursive",
              subtitle = "Calligraphic manuscript elegance",
              isSelected = currentFontType == "cursive",
              fontFamily = FontFamily.Cursive,
              fontWeight = activeFontWeight,
              fontStyle = activeFontStyle,
              onClick = { onSelectFontType("cursive") }
            )

            if (!customFontName.isNullOrBlank()) {
              FontOptionRow(
                title = customFontName,
                subtitle = "Your uploaded custom font",
                isSelected = currentFontType == "custom",
                badge = "CUSTOM",
                fontFamily = FontFamily.Serif,
                fontWeight = activeFontWeight,
                fontStyle = activeFontStyle,
                onClick = { onSelectFontType("custom") }
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Upload custom font button
          OutlinedButton(
            onClick = {
              fontPickerLauncher.launch(
                arrayOf(
                  "font/ttf",
                  "font/otf",
                  "application/x-font-ttf",
                  "application/x-font-otf",
                  "application/font-sfnt",
                  "*/*"
                )
              )
            },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AntiqueGold),
            border = BorderStroke(1.2.dp, AntiqueGold.copy(alpha = 0.65f)),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("upload_custom_font_button")
          ) {
            Icon(
              imageVector = Icons.Outlined.UploadFile,
              contentDescription = null,
              tint = AntiqueGold,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (customFontName.isNullOrBlank()) "Upload Custom Font (.ttf / .otf)" else "Change Custom Font (.ttf / .otf)",
              style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold
              ),
              color = AntiqueGold
            )
          }

          if (uploadStatusMessage != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = uploadStatusMessage ?: "",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
              color = if (isError) MaterialTheme.colorScheme.error else AntiqueGold
            )
          }

          Spacer(modifier = Modifier.height(18.dp))

          // 6. LIVE VISUAL SAMPLE PREVIEW CARD
          Text(
            text = "LIVE VISUAL SAMPLE",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 9.sp,
              letterSpacing = 1.4.sp,
              fontWeight = FontWeight.Bold
            ),
            color = CharcoalTertiary
          )
          Spacer(modifier = Modifier.height(8.dp))

          val previewFontFamily = when (currentFontType) {
            "sans" -> FontFamily.SansSerif
            "mono" -> FontFamily.Monospace
            "cursive" -> FontFamily.Cursive
            else -> FontFamily.Serif
          }

          Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFFFAF6EE),
            border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Text(
                text = "“To construct a room for silence is not merely to subtract sound, but to tune the subtle resonance of what remains.”",
                style = MaterialTheme.typography.bodyMedium.copy(
                  fontFamily = previewFontFamily,
                  fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                  fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
                  fontSize = (15f * fontSizeScale).sp,
                  lineHeight = (26f * fontSizeScale * lineHeightScale).sp,
                  textAlign = if (isJustified) TextAlign.Justify else TextAlign.Start
                ),
                color = CharcoalText
              )
              Spacer(modifier = Modifier.height(6.dp))
              val styleCaption = when {
                isBold && isItalic -> "Bold Italic"
                isBold -> "Bold"
                isItalic -> "Italic"
                else -> "Regular"
              }
              Text(
                text = "Previewing ${(fontSizeScale * 100).toInt()}% • $styleCaption • ${(lineHeightScale * 100).toInt()}% lining",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 8.5.sp,
                  color = CharcoalSecondary
                )
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Reset and Apply footer
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedButton(
            onClick = onResetDefaults,
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
          ) {
            Icon(
              imageVector = Icons.Outlined.RestartAlt,
              contentDescription = null,
              modifier = Modifier.size(14.dp),
              tint = CharcoalSecondary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Reset Defaults",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
              color = CharcoalSecondary
            )
          }

          Button(
            onClick = onDismiss,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CharcoalText),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
          ) {
            Text(
              text = "Apply Changes",
              style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = SoftCreamPaper
              )
            )
          }
        }
      }
    }
  }
}

@Composable
private fun FontOptionRow(
  title: String,
  subtitle: String,
  isSelected: Boolean,
  fontFamily: FontFamily,
  fontWeight: FontWeight = FontWeight.Normal,
  fontStyle: FontStyle = FontStyle.Normal,
  badge: String? = null,
  onClick: () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = if (isSelected) Color(0x15D4AF37) else Color(0x08000000),
    border = BorderStroke(
      width = if (isSelected) 1.5.dp else 1.dp,
      color = if (isSelected) AntiqueGold else SubtleBorder
    ),
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(
              fontFamily = fontFamily,
              fontSize = 13.5.sp,
              fontWeight = if (isSelected) FontWeight.Bold else fontWeight,
              fontStyle = fontStyle
            ),
            color = CharcoalText
          )
          if (badge != null) {
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = AntiqueGold
            ) {
              Text(
                text = badge,
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 8.sp,
                  fontWeight = FontWeight.Bold,
                  color = SoftCreamPaper
                ),
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
              )
            }
          }
        }
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
          color = CharcoalSecondary
        )
      }

      if (isSelected) {
        Box(
          modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(AntiqueGold),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Outlined.Check,
            contentDescription = null,
            tint = SoftCreamPaper,
            modifier = Modifier.size(13.dp)
          )
        }
      }
    }
  }
}
