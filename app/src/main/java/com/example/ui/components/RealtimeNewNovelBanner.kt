package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NovelWithState
import com.example.ui.theme.AntiqueGold
import com.example.ui.theme.CharcoalSecondary
import com.example.ui.theme.CharcoalText
import com.example.ui.theme.DeepBurgundy
import com.example.ui.theme.SoftCreamPaper

/**
 * Real-time banner that appears whenever a new manuscript is posted in the APK.
 * Visible to all readers and translators with instant reactive updates.
 */
@Composable
fun RealtimeNewNovelBanner(
  novel: NovelWithState,
  onReadNow: () -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "pulse_banner")
  val pulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.35f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(800, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse_alpha"
  )

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = SoftCreamPaper),
    border = BorderStroke(1.5.dp, AntiqueGold.copy(alpha = 0.8f)),
    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    modifier = modifier
      .testTag("realtime_new_novel_banner")
      .clickable { onReadNow() }
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .clip(CircleShape)
              .background(Color(0xFF2E7D32).copy(alpha = pulseAlpha))
          )
          Spacer(modifier = Modifier.width(6.dp))
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = AntiqueGold.copy(alpha = 0.2f),
            border = BorderStroke(0.6.dp, AntiqueGold)
          ) {
            Text(
              text = "REAL-TIME ALERT • JUST POSTED",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp,
                color = DeepBurgundy
              ),
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }

        IconButton(
          onClick = onDismiss,
          modifier = Modifier
            .size(24.dp)
            .testTag("dismiss_new_novel_alert_button")
        ) {
          Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = "Dismiss Alert",
            tint = CharcoalSecondary,
            modifier = Modifier.size(16.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = novel.title,
        style = MaterialTheme.typography.titleMedium.copy(
          fontFamily = FontFamily.Serif,
          fontWeight = FontWeight.Bold,
          color = CharcoalText
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )

      val authorText = if (novel.authorSlot > 0) {
        "By ${novel.author} (Translator Room ${novel.authorSlot})"
      } else {
        "By ${novel.author}"
      }
      Text(
        text = authorText,
        style = MaterialTheme.typography.labelSmall.copy(
          fontSize = 11.sp,
          fontWeight = FontWeight.Medium,
          color = AntiqueGold
        )
      )

      if (novel.excerpt.isNotBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = novel.excerpt,
          style = MaterialTheme.typography.bodySmall.copy(
            fontSize = 11.sp,
            color = CharcoalSecondary,
            lineHeight = 15.sp
          ),
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "● Available now to all readers & translators",
          style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 9.sp,
            color = CharcoalSecondary
          )
        )

        Button(
          onClick = onReadNow,
          colors = ButtonDefaults.buttonColors(
            containerColor = DeepBurgundy,
            contentColor = Color.White
          ),
          shape = RoundedCornerShape(10.dp),
          contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
          modifier = Modifier.testTag("read_new_novel_now_button")
        ) {
          Text(
            text = "Read Now",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            modifier = Modifier.size(12.dp)
          )
        }
      }
    }
  }
}
