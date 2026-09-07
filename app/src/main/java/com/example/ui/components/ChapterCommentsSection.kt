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
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChapterCommentsSection(
  chapterTitle: String,
  comments: List<ChapterCommentEntity>,
  activeReaderName: String?,
  activeReaderEmail: String? = null,
  isOwner: Boolean = false,
  isDarkMode: Boolean = false,
  onPostComment: (text: String, penName: String?, parentCommentId: String?, replyToReaderName: String?) -> Unit,
  onLikeComment: (commentId: String) -> Unit,
  onDeleteComment: ((commentId: String) -> Unit)? = null,
  onSavePenName: ((String) -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val sectionBgColor = if (isDarkMode) Color(0xFF221F1D) else Color(0xFFF9F6F0)
  val sectionBorderColor = if (isDarkMode) Color(0xFF38332E) else AntiqueGold.copy(alpha = 0.35f)
  val sectionTextColor = if (isDarkMode) Color(0xFFE8E2D9) else CharcoalText
  val sectionSecondaryTextColor = if (isDarkMode) Color(0xFFA8A099) else CharcoalSecondary
  val sectionInputBg = if (isDarkMode) Color(0xFF191716) else SoftCreamPaper

  var newCommentText by remember { mutableStateOf("") }
  var customPenName by remember(activeReaderName) {
    mutableStateOf(activeReaderName ?: "")
  }
  var isEditingPenName by remember { mutableStateOf(false) }
  var isPenNameSavedJustNow by remember { mutableStateOf(false) }
  var replyingToComment by remember { mutableStateOf<ChapterCommentEntity?>(null) }

  var liveTickerMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
  LaunchedEffect(Unit) {
    while (true) {
      delay(4000L) // Refresh relative timestamps in real-time every 4 seconds
      liveTickerMs = System.currentTimeMillis()
    }
  }

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
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = if (customPenName.isNotBlank()) "Posting as: $customPenName" else "Posting as: Literary Reader",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = CharcoalSecondary
          )
          if (isPenNameSavedJustNow) {
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = Color(0xFF2E7D32).copy(alpha = 0.12f),
              border = BorderStroke(0.5.dp, Color(0xFF2E7D32).copy(alpha = 0.4f))
            ) {
              Text(
                text = "Saved ✓",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 8.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF2E7D32)
                ),
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
              )
            }
          }
        }

        Text(
          text = if (isEditingPenName) "Close" else "Change Pen Name",
          style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 10.sp,
            color = AntiqueGold,
            fontWeight = FontWeight.SemiBold
          ),
          modifier = Modifier.clickable {
            isEditingPenName = !isEditingPenName
            isPenNameSavedJustNow = false
          }
        )
      }

      if (isEditingPenName) {
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
          value = customPenName,
          onValueChange = {
            customPenName = it
            isPenNameSavedJustNow = false
          },
          label = { Text("Your Pen Name") },
          singleLine = true,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AntiqueGold,
            unfocusedBorderColor = SubtleBorder
          ),
          modifier = Modifier.fillMaxWidth().testTag("input_custom_pen_name")
        )

        Spacer(modifier = Modifier.height(6.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Button(
            onClick = {
              val cleanName = customPenName.trim()
              if (cleanName.isNotBlank()) {
                onSavePenName?.invoke(cleanName)
                isPenNameSavedJustNow = true
                isEditingPenName = false
              }
            },
            enabled = customPenName.trim().isNotBlank(),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = AntiqueGold,
              disabledContainerColor = AntiqueGold.copy(alpha = 0.35f)
            ),
            modifier = Modifier
              .height(34.dp)
              .testTag("save_comment_pen_name_button")
          ) {
            Icon(
              imageVector = Icons.Filled.Check,
              contentDescription = null,
              tint = SoftCreamPaper,
              modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Save Pen Name",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = SoftCreamPaper
            )
          }
        }
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

      // Replying to banner if active
      val activeReply = replyingToComment
      if (activeReply != null) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = AntiqueGold.copy(alpha = 0.12f),
          border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.4f)),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("replying_to_banner")
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f)
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Outlined.Reply,
                contentDescription = null,
                tint = AntiqueGold,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Column {
                Text(
                  text = "Replying to ${activeReply.readerName}",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = AntiqueGold
                  )
                )
                Text(
                  text = "“${activeReply.commentText.take(45)}${if (activeReply.commentText.length > 45) "…" else ""}”",
                  style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Serif,
                    color = CharcoalSecondary
                  ),
                  maxLines = 1
                )
              }
            }
            IconButton(
              onClick = { replyingToComment = null },
              modifier = Modifier.size(24.dp)
            ) {
              Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Cancel reply",
                tint = CharcoalSecondary,
                modifier = Modifier.size(14.dp)
              )
            }
          }
        }
        Spacer(modifier = Modifier.height(8.dp))
      }

      // Input field
      OutlinedTextField(
        value = newCommentText,
        onValueChange = { newCommentText = it },
        placeholder = {
          Text(
            text = if (activeReply != null)
              "Write your reply to ${activeReply.readerName}..."
            else
              "Share your thoughts or critique on this chapter...",
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
              val parentId = replyingToComment?.let { it.parentCommentId ?: it.id }
              val replyToName = replyingToComment?.readerName
              onPostComment(
                newCommentText.trim(),
                customPenName.ifBlank { null },
                parentId,
                replyToName
              )
              newCommentText = ""
              replyingToComment = null
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
            imageVector = if (replyingToComment != null) Icons.AutoMirrored.Outlined.Reply else Icons.Outlined.Send,
            contentDescription = null,
            tint = SoftCreamPaper,
            modifier = Modifier.size(13.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (replyingToComment != null) "Post Reply" else "Post Thought",
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
        val rootComments = comments.filter { it.parentCommentId == null }
        val repliesMap = comments.filter { it.parentCommentId != null }.groupBy { it.parentCommentId!! }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          rootComments.forEach { rootComment ->
            val replies = (repliesMap[rootComment.id] ?: emptyList()).sortedBy { it.timestamp }
            CommentThreadItem(
              rootComment = rootComment,
              replies = replies,
              liveTickerMs = liveTickerMs,
              activeReaderName = activeReaderName,
              activeReaderEmail = activeReaderEmail,
              isOwner = isOwner,
              onLike = { onLikeComment(it) },
              onReply = { replyingToComment = it },
              onDelete = onDeleteComment
            )
          }
        }
      }
    }
  }
}

@Composable
private fun CommentThreadItem(
  rootComment: ChapterCommentEntity,
  replies: List<ChapterCommentEntity>,
  liveTickerMs: Long,
  activeReaderName: String?,
  activeReaderEmail: String?,
  isOwner: Boolean,
  onLike: (String) -> Unit,
  onReply: (ChapterCommentEntity) -> Unit,
  onDelete: ((String) -> Unit)?,
) {
  var areRepliesExpanded by remember { mutableStateOf(true) }
  val canDeleteRoot = isOwner ||
    (activeReaderName != null && rootComment.readerName.equals(activeReaderName, ignoreCase = true)) ||
    (!activeReaderEmail.isNullOrBlank() && rootComment.readerEmail.equals(activeReaderEmail, ignoreCase = true))

  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    CommentCardItem(
      comment = rootComment,
      liveTickerMs = liveTickerMs,
      replyCount = replies.size,
      onLike = { onLike(rootComment.id) },
      onReply = { onReply(rootComment) },
      onDelete = if (canDeleteRoot && onDelete != null) { { onDelete(rootComment.id) } } else null
    )

    // Expand/Collapse replies toggle if replies exist
    if (replies.isNotEmpty()) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .padding(start = 24.dp)
          .clip(RoundedCornerShape(8.dp))
          .clickable { areRepliesExpanded = !areRepliesExpanded }
          .padding(horizontal = 6.dp, vertical = 3.dp)
          .testTag("toggle_replies_${rootComment.id}")
      ) {
        Icon(
          imageVector = if (areRepliesExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
          contentDescription = null,
          tint = AntiqueGold,
          modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = if (areRepliesExpanded) "Hide ${replies.size} ${if (replies.size == 1) "reply" else "replies"}"
                 else "View ${replies.size} ${if (replies.size == 1) "reply" else "replies"}",
          style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = AntiqueGold
          )
        )
      }

      // Threaded nested replies container with vertical thread connector guide line
      if (areRepliesExpanded) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp)
            .drawBehind {
              val strokeWidth = 1.5.dp.toPx()
              val lineX = -10.dp.toPx()
              drawLine(
                color = AntiqueGold.copy(alpha = 0.35f),
                start = Offset(lineX, 0f),
                end = Offset(lineX, size.height),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
              )
            },
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          replies.forEach { reply ->
            val canDeleteReply = isOwner ||
              (activeReaderName != null && reply.readerName.equals(activeReaderName, ignoreCase = true)) ||
              (!activeReaderEmail.isNullOrBlank() && reply.readerEmail.equals(activeReaderEmail, ignoreCase = true))

            CommentCardItem(
              comment = reply,
              liveTickerMs = liveTickerMs,
              isNestedReply = true,
              onLike = { onLike(reply.id) },
              onReply = { onReply(reply) },
              onDelete = if (canDeleteReply && onDelete != null) { { onDelete(reply.id) } } else null
            )
          }
        }
      }
    }
  }
}

private fun formatRealtimeCommentDate(timestamp: Long, liveNow: Long): String {
  val diff = (liveNow - timestamp).coerceAtLeast(0L)
  val exactTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
  val exactDate = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
  return when {
    diff < 8_000L -> "Just now • $exactTime"
    diff < 60_000L -> "${(diff / 1000L).coerceAtLeast(1)}s ago • $exactTime"
    diff < 3600_000L -> "${diff / 60_000L}m ago • $exactTime"
    diff < 86400_000L -> "${diff / 3600_000L}h ago • $exactTime"
    diff < 172800_000L -> "Yesterday • $exactTime"
    else -> "$exactDate • $exactTime"
  }
}

@Composable
private fun CommentCardItem(
  comment: ChapterCommentEntity,
  liveTickerMs: Long,
  isNestedReply: Boolean = false,
  replyCount: Int = 0,
  onLike: () -> Unit,
  onReply: () -> Unit,
  onDelete: (() -> Unit)? = null,
) {
  val dateFormatted = remember(comment.timestamp, liveTickerMs) {
    formatRealtimeCommentDate(comment.timestamp, liveTickerMs)
  }
  val isRealtimeRecent = (liveTickerMs - comment.timestamp) in 0..120_000L

  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (isNestedReply) Color(0xFFFDFBF7) else SoftCreamPaper
    ),
    border = BorderStroke(
      width = if (isNestedReply) 0.8.dp else 1.dp,
      color = if (isNestedReply) AntiqueGold.copy(alpha = 0.25f) else SubtleBorder
    ),
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
              .size(if (isNestedReply) 22.dp else 26.dp)
              .clip(CircleShape)
              .background(Color(comment.avatarColorHex)),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = safeReaderName.take(1).uppercase(),
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = if (isNestedReply) 9.5.sp else 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
            )
          }

          Spacer(modifier = Modifier.width(8.dp))

          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = safeReaderName,
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 11.sp
                ),
                color = CharcoalText
              )
              if (replyCount > 0 && !isNestedReply) {
                Spacer(modifier = Modifier.width(6.dp))
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(AntiqueGold.copy(alpha = 0.12f))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                  Icon(
                    imageVector = Icons.Outlined.ChatBubbleOutline,
                    contentDescription = null,
                    tint = AntiqueGold,
                    modifier = Modifier.size(9.dp)
                  )
                  Spacer(modifier = Modifier.width(3.dp))
                  Text(
                    text = "$replyCount",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontSize = 8.5.sp,
                      fontWeight = FontWeight.Bold,
                      color = AntiqueGold
                    )
                  )
                }
              }
            }
            Text(
              text = dateFormatted,
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Normal
              ),
              color = CharcoalTertiary
            )
          }
        }

        // Reply, Like, and optional Delete buttons
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          // Reply button
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .clip(RoundedCornerShape(10.dp))
              .background(Color(0x0A000000))
              .clickable(onClick = onReply)
              .padding(horizontal = 7.dp, vertical = 3.dp)
              .testTag("reply_comment_${comment.id}")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Outlined.Reply,
              contentDescription = "Reply to comment",
              tint = CharcoalSecondary,
              modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              text = "Reply",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Medium,
                color = CharcoalSecondary
              )
            )
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

          // Optional Delete button (author or archive curator)
          if (onDelete != null) {
            IconButton(
              onClick = onDelete,
              modifier = Modifier.size(24.dp)
            ) {
              Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = "Delete comment",
                tint = CharcoalTertiary.copy(alpha = 0.7f),
                modifier = Modifier.size(13.dp)
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Reply badge if replying to someone specific
      if (comment.replyToReaderName != null) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .padding(bottom = 6.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(AntiqueGold.copy(alpha = 0.1f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Outlined.Reply,
            contentDescription = null,
            tint = AntiqueGold,
            modifier = Modifier.size(10.dp)
          )
          Spacer(modifier = Modifier.width(3.dp))
          Text(
            text = "Replying to @${comment.replyToReaderName}",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 9.sp,
              fontWeight = FontWeight.Medium,
              color = AntiqueGold
            )
          )
        }
      }

      Text(
        text = comment.commentText,
        style = MaterialTheme.typography.bodyMedium.copy(
          fontSize = if (isNestedReply) 12.sp else 12.5.sp,
          lineHeight = 18.sp
        ),
        color = CharcoalText
      )
    }
  }
}
