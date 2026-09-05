package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.NearMe
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.BookmarkHighlightEntity
import com.example.ui.theme.AntiqueGold
import com.example.ui.theme.CharcoalSecondary
import com.example.ui.theme.CharcoalTertiary
import com.example.ui.theme.CharcoalText
import com.example.ui.theme.SoftCreamPaper
import com.example.ui.theme.SubtleBorder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BookmarksHighlightsModal(
  bookmarks: List<BookmarkHighlightEntity>,
  onJumpToParagraph: (Int) -> Unit,
  onDeleteBookmark: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  val dateFormatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = SoftCreamPaper),
      border = BorderStroke(1.dp, SubtleBorder),
      elevation = CardDefaults.cardElevation(defaultElevation = 14.dp),
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 4.dp, vertical = 20.dp)
        .testTag("bookmarks_highlights_modal")
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
                imageVector = Icons.Filled.Bookmark,
                contentDescription = null,
                tint = AntiqueGold,
                modifier = Modifier.size(18.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "FAVORITE PASSAGES",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 8.5.sp,
                  letterSpacing = 1.6.sp,
                  fontWeight = FontWeight.Bold
                ),
                color = AntiqueGold
              )
              Text(
                text = "Bookmarked Lines (${bookmarks.size})",
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

        Spacer(modifier = Modifier.height(14.dp))

        if (bookmarks.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 32.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(
                imageVector = Icons.Filled.Bookmark,
                contentDescription = null,
                tint = AntiqueGold.copy(alpha = 0.4f),
                modifier = Modifier.size(40.dp)
              )
              Spacer(modifier = Modifier.height(10.dp))
              Text(
                text = "No Bookmarks Yet",
                style = MaterialTheme.typography.titleSmall.copy(
                  fontFamily = FontFamily.Serif,
                  fontWeight = FontWeight.SemiBold
                ),
                color = CharcoalText
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Tap on any paragraph or favorite line in the novel to highlight and bookmark it here.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                color = CharcoalSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
              )
            }
          }
        } else {
          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(max = 420.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            items(bookmarks, key = { it.id }) { bookmark ->
              Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F5EC)),
                border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
                ) {
                  // Chapter context tag and date
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = bookmark.chapterTitle.uppercase(),
                      style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.sp,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                      ),
                      color = AntiqueGold,
                      maxLines = 1
                    )

                    Text(
                      text = dateFormatter.format(Date(bookmark.timestamp)),
                      style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                      color = CharcoalTertiary
                    )
                  }

                  Spacer(modifier = Modifier.height(8.dp))

                  // Quote Text
                  Text(
                    text = "“${bookmark.quoteText}”",
                    style = MaterialTheme.typography.bodyMedium.copy(
                      fontFamily = FontFamily.Serif,
                      fontStyle = FontStyle.Italic,
                      lineHeight = 22.sp,
                      fontSize = 13.5.sp
                    ),
                    color = CharcoalText
                  )

                  Spacer(modifier = Modifier.height(10.dp))

                  // Actions: Jump to passage & delete
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    OutlinedButton(
                      onClick = {
                        onJumpToParagraph(bookmark.paragraphIndex)
                        onDismiss()
                      },
                      shape = RoundedCornerShape(10.dp),
                      contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                      colors = ButtonDefaults.outlinedButtonColors(contentColor = AntiqueGold),
                      border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.6f))
                    ) {
                      Icon(
                        imageVector = Icons.Outlined.NearMe,
                        contentDescription = null,
                        tint = AntiqueGold,
                        modifier = Modifier.size(12.dp)
                      )
                      Spacer(modifier = Modifier.width(4.dp))
                      Text(
                        text = "Jump to Passage",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = AntiqueGold
                      )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                      onClick = { onDeleteBookmark(bookmark.id) },
                      modifier = Modifier.size(30.dp)
                    ) {
                      Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Remove bookmark",
                        tint = CharcoalTertiary,
                        modifier = Modifier.size(15.dp)
                      )
                    }
                  }
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
          onClick = onDismiss,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = CharcoalText),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = "Back to Reading",
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
