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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FontDownload
import androidx.compose.material.icons.outlined.RestartAlt
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
  onSelectFontType: (String) -> Unit,
  onCustomFontUploaded: (filePath: String, fontName: String) -> Unit,
  onUpdateFontSizeScale: (Float) -> Unit,
  onUpdateLineHeightScale: (Float) -> Unit,
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

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = SoftCreamPaper),
      border = BorderStroke(1.dp, SubtleBorder),
      elevation = CardDefaults.cardElevation(defaultElevation = 14.dp),
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 24.dp)
        .testTag("typography_customizer_modal")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(22.dp)
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
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0x18D4AF37)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Outlined.FontDownload,
                contentDescription = null,
                tint = AntiqueGold,
                modifier = Modifier.size(18.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "READER TYPOGRAPHY",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 8.5.sp,
                  letterSpacing = 1.6.sp,
                  fontWeight = FontWeight.Bold
                ),
                color = AntiqueGold
              )
              Text(
                text = "Font & Reading Variety",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontFamily = FontFamily.Serif,
                  fontWeight = FontWeight.SemiBold
                ),
                color = CharcoalText
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

        Spacer(modifier = Modifier.height(18.dp))

        // Section: Font Family Selection
        Text(
          text = "FONT FAMILY",
          style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 9.sp,
            letterSpacing = 1.2.sp,
            fontWeight = FontWeight.Bold
          ),
          color = CharcoalTertiary
        )
        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          FontOptionRow(
            title = "Classic Serif",
            subtitle = "Literary editorial style • Recommended",
            isSelected = currentFontType == "serif",
            onClick = { onSelectFontType("serif") }
          )

          FontOptionRow(
            title = "Modern Sans",
            subtitle = "Clean geometric readability",
            isSelected = currentFontType == "sans",
            onClick = { onSelectFontType("sans") }
          )

          FontOptionRow(
            title = "Literary Monospace",
            subtitle = "Manuscript typescript aesthetic",
            isSelected = currentFontType == "mono",
            onClick = { onSelectFontType("mono") }
          )

          if (!customFontName.isNullOrBlank()) {
            FontOptionRow(
              title = customFontName,
              subtitle = "Your uploaded custom font",
              isSelected = currentFontType == "custom",
              badge = "CUSTOM",
              onClick = { onSelectFontType("custom") }
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

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
            text = if (customFontName.isNullOrBlank()) "Upload Your Own Font (.ttf / .otf)" else "Upload Different Font (.ttf / .otf)",
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

        // Section: Font Size Scale
        Text(
          text = "TEXT SIZE",
          style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 9.sp,
            letterSpacing = 1.2.sp,
            fontWeight = FontWeight.Bold
          ),
          color = CharcoalTertiary
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          listOf(
            Triple(0.88f, "Small", "15sp"),
            Triple(1.0f, "Standard", "17.5sp"),
            Triple(1.15f, "Large", "20sp"),
            Triple(1.30f, "Editorial", "23sp")
          ).forEach { (scale, label, _) ->
            val isSelected = kotlin.math.abs(fontSizeScale - scale) < 0.05f
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (isSelected) AntiqueGold else Color(0x0C000000),
              border = BorderStroke(1.dp, if (isSelected) AntiqueGold else SubtleBorder),
              modifier = Modifier
                .weight(1f)
                .clickable { onUpdateFontSizeScale(scale) }
            ) {
              Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 10.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                ),
                color = if (isSelected) SoftCreamPaper else CharcoalText,
                modifier = Modifier.padding(vertical = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Section: Line Spacing
        Text(
          text = "LINE SPACING",
          style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 9.sp,
            letterSpacing = 1.2.sp,
            fontWeight = FontWeight.Bold
          ),
          color = CharcoalTertiary
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          listOf(
            Pair(0.9f, "Compact"),
            Pair(1.0f, "Balanced"),
            Pair(1.15f, "Spacious")
          ).forEach { (scale, label) ->
            val isSelected = kotlin.math.abs(lineHeightScale - scale) < 0.05f
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (isSelected) CharcoalText else Color(0x0C000000),
              border = BorderStroke(1.dp, if (isSelected) CharcoalText else SubtleBorder),
              modifier = Modifier
                .weight(1f)
                .clickable { onUpdateLineHeightScale(scale) }
            ) {
              Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 10.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                ),
                color = if (isSelected) SoftCreamPaper else CharcoalText,
                modifier = Modifier.padding(vertical = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
          ) {
            Icon(
              imageVector = Icons.Outlined.RestartAlt,
              contentDescription = null,
              modifier = Modifier.size(14.dp),
              tint = CharcoalSecondary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Reset",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
              color = CharcoalSecondary
            )
          }

          Button(
            onClick = onDismiss,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CharcoalText),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 8.dp)
          ) {
            Text(
              text = "Done",
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
              fontSize = 13.sp,
              fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
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
