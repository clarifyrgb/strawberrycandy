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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
fun ChapterDiscussionModal(
  bookTitle: String,
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
  onDismiss: () -> Unit
) {
  val modalBgColor = if (isDarkMode) Color(0xFF1B1917) else Color(0xFFFAF7F0)
  val modalBorderColor = if (isDarkMode) Color(0xFF3A342F) else AntiqueGold.copy(alpha = 0.45f)
  val textColor = if (isDarkMode) Color(0xFFE8E2D9) else CharcoalText
  val secondaryTextColor = if (isDarkMode) Color(0xFFA8A099) else CharcoalSecondary
  val tertiaryTextColor = if (isDarkMode) Color(0xFF78726C) else CharcoalTertiary
  val inputBgColor = if (isDarkMode) Color(0xFF141211) else SoftCreamPaper
  val chipBgColor = if (isDarkMode) Color(0xFF262220) else Color(0x0E000000)
  val itemBgColor = if (isDarkMode) Color(0xFF24201E) else SoftCreamPaper
  val replyBgColor = if (isDarkMode) Color(0xFF2A2623) else Color(0xFFFDFBF7)

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
      delay(4000L)
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

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = modalBgColor),
      border = BorderStroke(1.2.dp, modalBorderColor),
      elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 20.dp)
        .widthIn(max = 580.dp)
        .heightIn(max = 720.dp)
        .testTag("chapter_discussion_modal")
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        // Modal Header
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isDarkMode) Color(0xFF2A2521) else Color(0x1AD4AF37)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Outlined.ChatBubbleOutline,
                contentDescription = null,
                tint = AntiqueGold,
                modifier = Modifier.size(18.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "CHAPTER DISCUSSION",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 9.sp,
                  letterSpacing = 1.4.sp,
                  fontWeight = FontWeight.Bold
                ),
                color = AntiqueGold
              )
              Text(
                text = chapterTitle,
                style = MaterialTheme.typography.titleMedium.copy(
                  fontFamily = FontFamily.Serif,
                  fontWeight = FontWeight.Bold
                ),
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = "$bookTitle • ${comments.size} ${if (comments.size == 1) "reflection" else "reflections"}",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 9.5.sp,
                  color = secondaryTextColor
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier
              .size(34.dp)
              .testTag("close_discussion_modal_button")
          ) {
            Icon(
              imageVector = Icons.Outlined.Close,
              contentDescription = "Close discussion",
              tint = secondaryTextColor,
              modifier = Modifier.size(20.dp)
            )
          }
        }

        HorizontalDivider(
          thickness = 1.dp,
          color = if (isDarkMode) Color(0xFF332F2B) else Color(0x1AD4AF37)
        )

        // Scrollable Discussion Body
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f, fill = false)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
          // Pen name editor row
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = if (customPenName.isNotBlank()) "Posting as: $customPenName" else "Posting as: Literary Reader",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Medium
                ),
                color = secondaryTextColor
              )
              if (isPenNameSavedJustNow) {
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = Color(0xFF2E7D32).copy(alpha = 0.15f),
                  border = BorderStroke(0.5.dp, Color(0xFF2E7D32).copy(alpha = 0.5f))
                ) {
                  Text(
                    text = "Saved ✓",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontSize = 8.5.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color(0xFF4CAF50)
                    ),
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                  )
                }
              }
            }

            Text(
              text = if (isEditingPenName) "Done" else "Change Pen Name",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                color = AntiqueGold,
                fontWeight = FontWeight.SemiBold
              ),
              modifier = Modifier
                .clickable {
                  isEditingPenName = !isEditingPenName
                  isPenNameSavedJustNow = false
                }
                .testTag("toggle_pen_name_editor")
            )
          }

          if (isEditingPenName) {
            Spacer(modifier = Modifier.height(8.dp))
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
                unfocusedBorderColor = if (isDarkMode) Color(0xFF3E3934) else SubtleBorder,
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                focusedContainerColor = inputBgColor,
                unfocusedContainerColor = inputBgColor
              ),
              modifier = Modifier.fillMaxWidth().testTag("input_custom_pen_name")
            )

            Spacer(modifier = Modifier.height(6.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.End
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
                modifier = Modifier.height(32.dp).testTag("save_comment_pen_name_button")
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
                  fontSize = 10.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = SoftCreamPaper
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Quick reaction chips
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            quickReactions.forEach { reaction ->
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = chipBgColor,
                border = BorderStroke(1.dp, if (isDarkMode) Color(0xFF38332E) else SubtleBorder),
                modifier = Modifier.clickable {
                  newCommentText = if (newCommentText.isBlank()) reaction else "$newCommentText $reaction"
                }
              ) {
                Text(
                  text = reaction,
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.5.sp,
                    color = textColor
                  ),
                  modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Replying-to banner
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
                        color = secondaryTextColor
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
                    tint = secondaryTextColor,
                    modifier = Modifier.size(14.dp)
                  )
                }
              }
            }
            Spacer(modifier = Modifier.height(8.dp))
          }

          // Comment text input
          OutlinedTextField(
            value = newCommentText,
            onValueChange = { newCommentText = it },
            placeholder = {
              Text(
                text = if (activeReply != null)
                  "Write your reply to ${activeReply.readerName}..."
                else
                  "Share your thoughts or critique on this chapter...",
                style = MaterialTheme.typography.bodyMedium.copy(
                  fontSize = 12.5.sp,
                  color = tertiaryTextColor
                )
              )
            },
            maxLines = 4,
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = AntiqueGold,
              unfocusedBorderColor = if (isDarkMode) Color(0xFF38332E) else SubtleBorder,
              focusedTextColor = textColor,
              unfocusedTextColor = textColor,
              focusedContainerColor = inputBgColor,
              unfocusedContainerColor = inputBgColor
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_chapter_comment")
          )

          Spacer(modifier = Modifier.height(8.dp))

          // Post thought button
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
                containerColor = AntiqueGold,
                disabledContainerColor = AntiqueGold.copy(alpha = 0.3f)
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

          Spacer(modifier = Modifier.height(20.dp))

          // Reader reflections section header
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "READER REFLECTIONS",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.Bold
              ),
              color = AntiqueGold
            )
            Text(
              text = "${comments.size} total",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.5.sp,
                color = tertiaryTextColor
              )
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Comments List
          if (comments.isEmpty()) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "Be the first reader to reflect on this chapter.",
                style = MaterialTheme.typography.bodySmall.copy(
                  fontSize = 12.sp,
                  fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                ),
                color = secondaryTextColor
              )
            }
          } else {
            val rootComments = comments.filter { it.parentCommentId == null }
            val repliesMap = comments.filter { it.parentCommentId != null }.groupBy { it.parentCommentId!! }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              rootComments.forEach { rootComment ->
                val replies = (repliesMap[rootComment.id] ?: emptyList()).sortedBy { it.timestamp }
                ModalCommentThreadItem(
                  rootComment = rootComment,
                  replies = replies,
                  liveTickerMs = liveTickerMs,
                  activeReaderName = activeReaderName,
                  activeReaderEmail = activeReaderEmail,
                  isOwner = isOwner,
                  isDarkMode = isDarkMode,
                  itemBgColor = itemBgColor,
                  replyBgColor = replyBgColor,
                  textColor = textColor,
                  secondaryTextColor = secondaryTextColor,
                  tertiaryTextColor = tertiaryTextColor,
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
  }
}

@Composable
private fun ModalCommentThreadItem(
  rootComment: ChapterCommentEntity,
  replies: List<ChapterCommentEntity>,
  liveTickerMs: Long,
  activeReaderName: String?,
  activeReaderEmail: String?,
  isOwner: Boolean,
  isDarkMode: Boolean,
  itemBgColor: Color,
  replyBgColor: Color,
  textColor: Color,
  secondaryTextColor: Color,
  tertiaryTextColor: Color,
  onLike: (String) -> Unit,
  onReply: (ChapterCommentEntity) -> Unit,
  onDelete: ((String) -> Unit)?,
) {
  var areRepliesExpanded by remember { mutableStateOf(true) }
  val canDeleteRoot = isOwner ||
    (activeReaderName != null && rootComment.readerName.equals(activeReaderName, ignoreCase = true)) ||
    (!activeReaderEmail.isNullOrBlank() && rootComment.readerEmail.equals(activeReaderEmail, ignoreCase = true))

  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    ModalCommentCardItem(
      comment = rootComment,
      liveTickerMs = liveTickerMs,
      replyCount = replies.size,
      isDarkMode = isDarkMode,
      containerBg = itemBgColor,
      textColor = textColor,
      secondaryTextColor = secondaryTextColor,
      tertiaryTextColor = tertiaryTextColor,
      onLike = { onLike(rootComment.id) },
      onReply = { onReply(rootComment) },
      onDelete = if (canDeleteRoot && onDelete != null) { { onDelete(rootComment.id) } } else null
    )

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

            ModalCommentCardItem(
              comment = reply,
              liveTickerMs = liveTickerMs,
              isNestedReply = true,
              isDarkMode = isDarkMode,
              containerBg = replyBgColor,
              textColor = textColor,
              secondaryTextColor = secondaryTextColor,
              tertiaryTextColor = tertiaryTextColor,
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

@Composable
private fun ModalCommentCardItem(
  comment: ChapterCommentEntity,
  liveTickerMs: Long,
  isNestedReply: Boolean = false,
  replyCount: Int = 0,
  isDarkMode: Boolean = false,
  containerBg: Color,
  textColor: Color,
  secondaryTextColor: Color,
  tertiaryTextColor: Color,
  onLike: () -> Unit,
  onReply: () -> Unit,
  onDelete: (() -> Unit)? = null,
) {
  val dateFormatted = remember(comment.timestamp, liveTickerMs) {
    formatRealtimeDate(comment.timestamp, liveTickerMs)
  }

  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = containerBg),
    border = BorderStroke(
      width = if (isNestedReply) 0.8.dp else 1.dp,
      color = if (isNestedReply) AntiqueGold.copy(alpha = 0.25f) else (if (isDarkMode) Color(0xFF38332E) else SubtleBorder)
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
                color = textColor
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
              color = tertiaryTextColor
            )
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          // Reply button
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .clip(RoundedCornerShape(10.dp))
              .background(if (isDarkMode) Color(0xFF2A2623) else Color(0x0A000000))
              .clickable(onClick = onReply)
              .padding(horizontal = 7.dp, vertical = 3.dp)
              .testTag("reply_comment_${comment.id}")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Outlined.Reply,
              contentDescription = "Reply to comment",
              tint = secondaryTextColor,
              modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              text = "Reply",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Medium,
                color = secondaryTextColor
              )
            )
          }

          // Like button & counter
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .clip(RoundedCornerShape(10.dp))
              .background(if (comment.isLikedByMe) Color(0x18C74350) else (if (isDarkMode) Color(0xFF2A2623) else Color(0x0C000000)))
              .clickable(onClick = onLike)
              .padding(horizontal = 7.dp, vertical = 3.dp)
          ) {
            Icon(
              imageVector = if (comment.isLikedByMe) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
              contentDescription = "Like comment",
              tint = if (comment.isLikedByMe) Color(0xFFC74350) else secondaryTextColor,
              modifier = Modifier.size(12.dp)
            )
            if (comment.likesCount > 0) {
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = comment.likesCount.toString(),
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 10.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = if (comment.isLikedByMe) Color(0xFFC74350) else secondaryTextColor
                )
              )
            }
          }

          // Optional Delete button
          if (onDelete != null) {
            IconButton(
              onClick = onDelete,
              modifier = Modifier.size(24.dp)
            ) {
              Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = "Delete comment",
                tint = tertiaryTextColor.copy(alpha = 0.7f),
                modifier = Modifier.size(13.dp)
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

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
        color = textColor
      )
    }
  }
}

private fun formatRealtimeDate(timestamp: Long, liveNow: Long): String {
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
