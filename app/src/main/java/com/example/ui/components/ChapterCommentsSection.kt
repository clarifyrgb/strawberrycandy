package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ChapterCommentEntity
import com.example.ui.theme.AntiqueGold
import com.example.ui.theme.CharcoalSecondary
import com.example.ui.theme.CharcoalTertiary
import com.example.ui.theme.CharcoalText
import com.example.ui.theme.SoftCreamPaper
import com.example.ui.theme.SubtleBorder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChapterCommentsSection(
  chapterTitle: String,
  comments: List<ChapterCommentEntity>,
  activeReaderName: String?,
  onPostComment: (text: String, penName: String?) -> Unit,
  onLikeComment: (commentId: String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var newCommentText by remember { mutableStateOf("") }
  var customPenName by remember(activeReaderName) {
    mutableStateOf(activeReaderName ?: "")
  }
  var isEditingPenName by remember { mutableStateOf(false) }

  val dateFormatter = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }

  val quickReactions = listOf(
    "✨ Eloquent passage",
    "🕯️ Poignant reflection",
    "✒️ Masterful translation",
    "📖 Timeless classic",
    "🌹 Beautiful chapter"
  )

  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F6F0)),
    border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.35f)),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = modifier
      .fillMaxWidth()
      .testTag("chapter_comments_section")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
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
              .size(32.dp)
              .clip(CircleShape)
              .background(Color(0x1AD4AF37)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Outlined.ChatBubbleOutline,
              contentDescription = null,
              tint = AntiqueGold,
              modifier = Modifier.size(16.dp)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "CHAPTER DISCUSSION",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.5.sp,
                letterSpacing = 1.6.sp,
                fontWeight = FontWeight.Bold
              ),
              color = AntiqueGold
            )
            Text(
              text = "${comments.size} Reader ${if (comments.size == 1) "Reflection" else "Reflections"}",
              style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold
              ),
              color = CharcoalText
            )
          }
        }

        Surface(
          shape = RoundedCornerShape(10.dp),
          color = AntiqueGold.copy(alpha = 0.12f)
        ) {
          Text(
            text = "Active Chapter",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 9.sp,
              fontWeight = FontWeight.SemiBold,
              color = AntiqueGold
            ),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Reader pen name indicator / editor
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = if (customPenName.isNotBlank()) "Posting as: $customPenName" else "Posting as: Literary Reader",
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
          color = CharcoalSecondary
        )

        Text(
          text = if (isEditingPenName) "Done" else "Change Pen Name",
          style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 10.sp,
            color = AntiqueGold,
            fontWeight = FontWeight.SemiBold
          ),
          modifier = Modifier.clickable { isEditingPenName = !isEditingPenName }
        )
      }

      if (isEditingPenName) {
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
          value = customPenName,
          onValueChange = { customPenName = it },
          label = { Text("Your Pen Name") },
          singleLine = true,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AntiqueGold,
            unfocusedBorderColor = SubtleBorder
          ),
          modifier = Modifier.fillMaxWidth()
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Fast reaction chips
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        quickReactions.forEach { reaction ->
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0x0E000000),
            border = BorderStroke(1.dp, SubtleBorder),
            modifier = Modifier.clickable {
              newCommentText = if (newCommentText.isBlank()) reaction else "$newCommentText $reaction"
            }
          ) {
            Text(
              text = reaction,
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.5.sp,
                color = CharcoalText
              ),
              modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Input field
      OutlinedTextField(
        value = newCommentText,
        onValueChange = { newCommentText = it },
        placeholder = {
          Text(
            text = "Share your thoughts or critique on this chapter...",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp)
          )
        },
        maxLines = 4,
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = AntiqueGold,
          unfocusedBorderColor = SubtleBorder,
          focusedContainerColor = SoftCreamPaper,
          unfocusedContainerColor = SoftCreamPaper
        ),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("input_chapter_comment")
      )

      Spacer(modifier = Modifier.height(8.dp))

      // Post button
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
      ) {
        Button(
          onClick = {
            if (newCommentText.isNotBlank()) {
              onPostComment(newCommentText.trim(), customPenName.ifBlank { null })
              newCommentText = ""
            }
          },
          enabled = newCommentText.isNotBlank(),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = CharcoalText,
            disabledContainerColor = CharcoalTertiary.copy(alpha = 0.3f)
          ),
          modifier = Modifier.testTag("submit_chapter_comment")
        ) {
          Icon(
            imageVector = Icons.Outlined.Send,
            contentDescription = null,
            tint = SoftCreamPaper,
            modifier = Modifier.size(13.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Post Thought",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold,
              color = SoftCreamPaper
            )
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Comments List
      if (comments.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "Be the first reader to reflect on this chapter.",
            style = MaterialTheme.typography.bodySmall.copy(
              fontSize = 11.5.sp,
              fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            ),
            color = CharcoalSecondary
          )
        }
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          comments.forEach { comment ->
            CommentCardItem(
              comment = comment,
              dateFormatted = dateFormatter.format(Date(comment.timestamp)),
              onLike = { onLikeComment(comment.id) }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun CommentCardItem(
  comment: ChapterCommentEntity,
  dateFormatted: String,
  onLike: () -> Unit,
) {
  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = SoftCreamPaper),
    border = BorderStroke(1.dp, SubtleBorder),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          val safeReaderName = comment.readerName
            .replace(Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"), "")
            .substringBefore("@")
            .trim()
            .ifBlank { "Literary Reader" }

          Box(
            modifier = Modifier
              .size(26.dp)
              .clip(CircleShape)
              .background(Color(comment.avatarColorHex)),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = safeReaderName.take(1).uppercase(),
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
            )
          }

          Spacer(modifier = Modifier.width(8.dp))

          Column {
            Text(
              text = safeReaderName,
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp
              ),
              color = CharcoalText
            )
            Text(
              text = dateFormatted,
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
              color = CharcoalTertiary
            )
          }
        }

        // Like button & counter
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (comment.isLikedByMe) Color(0x18C74350) else Color(0x0C000000))
            .clickable(onClick = onLike)
            .padding(horizontal = 7.dp, vertical = 3.dp)
        ) {
          Icon(
            imageVector = if (comment.isLikedByMe) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = "Like comment",
            tint = if (comment.isLikedByMe) Color(0xFFC74350) else CharcoalSecondary,
            modifier = Modifier.size(12.dp)
          )
          if (comment.likesCount > 0) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = comment.likesCount.toString(),
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (comment.isLikedByMe) Color(0xFFC74350) else CharcoalSecondary
              )
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = comment.commentText,
        style = MaterialTheme.typography.bodyMedium.copy(
          fontSize = 12.5.sp,
          lineHeight = 18.sp
        ),
        color = CharcoalText
      )
    }
  }
}
