package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.theme.AntiqueGold
import com.example.ui.theme.SoftCreamPaper
import java.io.File

/**
 * Renders an image either from local drawable name or file path / Uri.
 */
@Composable
fun StoryPhotoItem(
  uri: String,
  contentDescription: String?,
  modifier: Modifier = Modifier,
  contentScale: ContentScale = ContentScale.Crop,
) {
  val context = LocalContext.current

  if (uri.startsWith("drawable:")) {
    val resName = uri.removePrefix("drawable:")
    val resId = remember(resName) {
      when (resName) {
        "img_book_1" -> R.drawable.img_book_1
        "img_book_2" -> R.drawable.img_book_2
        "img_book_3" -> R.drawable.img_book_3
        "img_novel_crimson_bloom" -> R.drawable.img_novel_crimson_bloom
        "img_novel_celestial" -> R.drawable.img_novel_celestial
        "img_novel_whispering_pines" -> R.drawable.img_novel_whispering_pines
        "img_novel_moonlight" -> R.drawable.img_novel_moonlight
        else -> context.resources.getIdentifier(resName, "drawable", context.packageName)
      }
    }
    if (resId != 0) {
      Image(
        painter = painterResource(id = resId),
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier
      )
    } else {
      Box(modifier = modifier.background(Color(0xFF2A2825)))
    }
  } else {
    val file = remember(uri) { File(uri) }
    val model = if (file.exists()) file else uri
    AsyncImage(
      model = model,
      contentDescription = contentDescription,
      contentScale = contentScale,
      modifier = modifier
    )
  }
}

/**
 * Fullscreen Interactive Lightbox Modal for Story Photos inside the novel.
 * Supports pinch-to-zoom and pan gestures with reset controls.
 */
@Composable
fun StoryPhotoViewerModal(
  photoUri: String,
  caption: String,
  novelTitle: String,
  onDismiss: () -> Unit,
) {
  var scale by remember { mutableFloatStateOf(1f) }
  var offsetX by remember { mutableFloatStateOf(0f) }
  var offsetY by remember { mutableFloatStateOf(0f) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color(0xF5111111))
        .testTag("story_photo_viewer_modal")
    ) {
      // Zoomable image container
      Box(
        modifier = Modifier
          .fillMaxSize()
          .pointerInput(Unit) {
            detectTransformGestures { _, pan, zoom, _ ->
              scale = (scale * zoom).coerceIn(1f, 4.5f)
              if (scale > 1f) {
                val maxX = (size.width * (scale - 1)) / 2
                val maxY = (size.height * (scale - 1)) / 2
                offsetX = (offsetX + pan.x).coerceIn(-maxX, maxX)
                offsetY = (offsetY + pan.y).coerceIn(-maxY, maxY)
              } else {
                offsetX = 0f
                offsetY = 0f
              }
            }
          },
        contentAlignment = Alignment.Center
      ) {
        StoryPhotoItem(
          uri = photoUri,
          contentDescription = caption,
          contentScale = ContentScale.Fit,
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 70.dp)
            .graphicsLayer {
              scaleX = scale
              scaleY = scale
              translationX = offsetX
              translationY = offsetY
            }
        )
      }

      // Top control bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .statusBarsPadding()
          .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Outlined.ZoomIn,
            contentDescription = null,
            tint = AntiqueGold,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "STORY ILLUSTRATION",
            style = MaterialTheme.typography.labelSmall.copy(
              letterSpacing = 1.6.sp,
              fontWeight = FontWeight.Bold
            ),
            color = AntiqueGold
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          if (scale > 1.05f) {
            IconButton(
              onClick = {
                scale = 1f
                offsetX = 0f
                offsetY = 0f
              },
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0x33FFFFFF))
            ) {
              Icon(
                imageVector = Icons.Outlined.RestartAlt,
                contentDescription = "Reset Zoom",
                tint = SoftCreamPaper,
                modifier = Modifier.size(18.dp)
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(Color(0x33FFFFFF))
              .testTag("close_photo_viewer")
          ) {
            Icon(
              imageVector = Icons.Outlined.Close,
              contentDescription = "Close",
              tint = SoftCreamPaper,
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }

      // Bottom caption sheet
      Surface(
        color = Color(0xD81E1E1E),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .fillMaxWidth()
          .navigationBarsPadding()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            text = caption,
            style = MaterialTheme.typography.bodyMedium.copy(
              fontFamily = FontFamily.Serif,
              fontStyle = FontStyle.Italic,
              lineHeight = 22.sp
            ),
            color = SoftCreamPaper,
            textAlign = TextAlign.Center
          )

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            text = novelTitle.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 9.sp,
              letterSpacing = 1.4.sp
            ),
            color = AntiqueGold.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
          )
        }
      }
    }
  }
}
