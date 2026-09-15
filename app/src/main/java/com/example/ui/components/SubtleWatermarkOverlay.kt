package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NoPhotography
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CharcoalText

/**
 * Subtle watermark overlay that repeats diagonal security authentication text
 * across the reading surface.
 */
@Composable
fun SubtleWatermarkOverlay(
  modifier: Modifier = Modifier,
  text: String = "AP-8842 • LICENSED TO AUTHORIZED READER • JULIAN VANCE OFFICIAL ARCHIVE",
  textColor: Color = CharcoalText.copy(alpha = 0.042f),
) {
  Canvas(
    modifier = modifier
      .fillMaxSize()
      .testTag("subtle_watermark_canvas")
  ) {
    if (size.width <= 0f || size.height <= 0f) return@Canvas

    val paint = android.graphics.Paint().apply {
      isAntiAlias = true
      textSize = 34f
      color = android.graphics.Color.argb(
        (textColor.alpha * 255).toInt(),
        (textColor.red * 255).toInt(),
        (textColor.green * 255).toInt(),
        (textColor.blue * 255).toInt()
      )
      letterSpacing = 0.18f
    }

    val stepY = 160f
    val stepX = 540f
    val diagonalAngle = -26f

    rotate(diagonalAngle, pivot = Offset(size.width / 2f, size.height / 2f)) {
      var y = -size.height * 0.6f
      var rowIndex = 0
      while (y < size.height * 1.8f) {
        val offsetX = if (rowIndex % 2 == 0) 0f else stepX / 2f
        var x = -size.width * 0.8f + offsetX
        while (x < size.width * 1.8f) {
          drawContext.canvas.nativeCanvas.drawText(text, x, y, paint)
          x += stepX
        }
        y += stepY
        rowIndex++
      }
    }
  }
}

/**
 * Visual indicator of security features:
 * A disabled screenshot icon ghosted in the background with soft natural lighting.
 */
@Composable
fun GhostedSecurityBackground(
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .fillMaxSize()
      .testTag("ghosted_security_background"),
    contentAlignment = Alignment.Center
  ) {
    // Large ghosted disabled screenshot / no photography icon in background
    Icon(
      imageVector = Icons.Outlined.NoPhotography,
      contentDescription = "Screenshot restricted by security enclave",
      tint = CharcoalText.copy(alpha = 0.038f),
      modifier = Modifier
        .size(260.dp)
        .testTag("ghosted_screenshot_icon")
    )

    // Secondary subtle security shield motif watermark
    Icon(
      imageVector = Icons.Outlined.Security,
      contentDescription = "Proprietary document security indicator",
      tint = CharcoalText.copy(alpha = 0.025f),
      modifier = Modifier
        .size(420.dp)
        .testTag("ghosted_security_shield")
    )
  }
}
