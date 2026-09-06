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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MenuBook
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.BookChapter
import com.example.ui.theme.AntiqueGold
import com.example.ui.theme.CharcoalSecondary
import com.example.ui.theme.CharcoalTertiary
import com.example.ui.theme.CharcoalText
import com.example.ui.theme.SoftCreamPaper
import com.example.ui.theme.SubtleBorder

@Composable
fun ChapterSelectionModal(
  bookTitle: String,
  chapters: List<BookChapter>,
  currentChapterIndex: Int,
  onSelectChapter: (chapterIndex: Int, startParagraphIndex: Int) -> Unit,
  onAddChapterClick: (() -> Unit)? = null,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(26.dp),
      colors = CardDefaults.cardColors(containerColor = SoftCreamPaper),
      border = BorderStroke(1.2.dp, SubtleBorder),
      elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
      modifier = modifier
        .fillMaxWidth()
        .padding(horizontal = 4.dp, vertical = 20.dp)
        .testTag("chapter_selection_modal")
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
                .size(38.dp)
                .clip(CircleShape)
                .background(Color(0x18D4AF37)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Outlined.MenuBook,
                contentDescription = null,
                tint = AntiqueGold,
                modifier = Modifier.size(20.dp)
              )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
              Text(
                text = "TABLE OF CONTENTS",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 8.5.sp,
                  letterSpacing = 1.8.sp,
                  fontWeight = FontWeight.Bold
                ),
                color = AntiqueGold
              )
              Text(
                text = bookTitle,
                style = MaterialTheme.typography.titleMedium.copy(
                  fontFamily = FontFamily.Serif,
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 15.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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

        // Guidance banner: "Point to the chapter you like"
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = Color(0x0E000000),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Outlined.Bookmark,
              contentDescription = null,
              tint = AntiqueGold,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Tap any chapter below to point and jump immediately to it",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                color = CharcoalSecondary
              )
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Chapters List
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 380.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          itemsIndexed(chapters) { index, chapter ->
            val isCurrent = index == currentChapterIndex

            Surface(
              shape = RoundedCornerShape(16.dp),
              color = if (isCurrent) Color(0x1AD4AF37) else Color(0x09000000),
              border = BorderStroke(
                width = if (isCurrent) 1.5.dp else 1.dp,
                color = if (isCurrent) AntiqueGold else SubtleBorder
              ),
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  onSelectChapter(index, chapter.startParagraphIndex)
                  onDismiss()
                }
                .testTag("chapter_item_${index}")
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                // Chapter Number Badge + Details
                Row(
                  modifier = Modifier.weight(1f),
                  verticalAlignment = Alignment.Top
                ) {
                  // Roman numeral box
                  Box(
                    modifier = Modifier
                      .size(36.dp)
                      .clip(RoundedCornerShape(10.dp))
                      .background(if (isCurrent) AntiqueGold else Color(0x14000000)),
                    contentAlignment = Alignment.Center
                  ) {
                    Text(
                      text = toRomanNumeral(chapter.number),
                      style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCurrent) SoftCreamPaper else CharcoalText
                      )
                    )
                  }

                  Spacer(modifier = Modifier.width(12.dp))

                  Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Text(
                        text = chapter.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                          fontFamily = FontFamily.Serif,
                          fontWeight = FontWeight.Bold,
                          fontSize = 13.5.sp
                        ),
                        color = if (isCurrent) CharcoalText else CharcoalText.copy(alpha = 0.9f)
                      )
                    }

                    if (chapter.previewSnippet.isNotBlank()) {
                      Spacer(modifier = Modifier.height(4.dp))
                      Text(
                        text = chapter.previewSnippet,
                        style = MaterialTheme.typography.bodySmall.copy(
                          fontSize = 10.5.sp,
                          lineHeight = 14.sp,
                          color = CharcoalSecondary
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                      )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (isCurrent) {
                      Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = AntiqueGold
                      ) {
                        Row(
                          verticalAlignment = Alignment.CenterVertically,
                          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                          Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            tint = SoftCreamPaper,
                            modifier = Modifier.size(10.dp)
                          )
                          Spacer(modifier = Modifier.width(3.dp))
                          Text(
                            text = "POINTED • READING NOW",
                            style = MaterialTheme.typography.labelSmall.copy(
                              fontSize = 8.sp,
                              fontWeight = FontWeight.Bold,
                              letterSpacing = 0.8.sp,
                              color = SoftCreamPaper
                            )
                          )
                        }
                      }
                    } else {
                      Text(
                        text = "Tap to jump here",
                        style = MaterialTheme.typography.labelSmall.copy(
                          fontSize = 9.sp,
                          color = AntiqueGold,
                          fontWeight = FontWeight.SemiBold
                        )
                      )
                    }
                  }
                }

                // Right checkmark or chevron
                if (isCurrent) {
                  Box(
                    modifier = Modifier
                      .size(24.dp)
                      .clip(CircleShape)
                      .background(AntiqueGold),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(
                      imageVector = Icons.Outlined.Check,
                      contentDescription = "Currently active chapter",
                      tint = SoftCreamPaper,
                      modifier = Modifier.size(14.dp)
                    )
                  }
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Previous / Next chapter jump buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          val canGoPrev = currentChapterIndex > 0
          OutlinedButton(
            onClick = {
              if (canGoPrev) {
                val prevIndex = currentChapterIndex - 1
                onSelectChapter(prevIndex, chapters[prevIndex].startParagraphIndex)
                onDismiss()
              }
            },
            enabled = canGoPrev,
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
              contentDescription = null,
              modifier = Modifier.size(14.dp),
              tint = if (canGoPrev) CharcoalText else CharcoalTertiary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Prev Chapter",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
              color = if (canGoPrev) CharcoalText else CharcoalTertiary
            )
          }

          val canGoNext = currentChapterIndex < chapters.size - 1
          OutlinedButton(
            onClick = {
              if (canGoNext) {
                val nextIndex = currentChapterIndex + 1
                onSelectChapter(nextIndex, chapters[nextIndex].startParagraphIndex)
                onDismiss()
              }
            },
            enabled = canGoNext,
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
          ) {
            Text(
              text = "Next Chapter",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
              color = if (canGoNext) CharcoalText else CharcoalTertiary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
              imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
              contentDescription = null,
              modifier = Modifier.size(14.dp),
              tint = if (canGoNext) CharcoalText else CharcoalTertiary
            )
          }
        }

        if (onAddChapterClick != null) {
          Spacer(modifier = Modifier.height(14.dp))
          OutlinedButton(
            onClick = onAddChapterClick,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.2.dp, AntiqueGold),
            modifier = Modifier
              .fillMaxWidth()
              .height(38.dp)
              .testTag("add_chapter_from_toc_button"),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AntiqueGold)
          ) {
            Icon(
              imageVector = Icons.Outlined.Add,
              contentDescription = null,
              modifier = Modifier.size(14.dp),
              tint = AntiqueGold
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "+ Add New Chapter to Novel",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
              )
            )
          }
        }
      }
    }
  }
}

private fun toRomanNumeral(n: Int): String {
  return when (n) {
    1 -> "I"
    2 -> "II"
    3 -> "III"
    4 -> "IV"
    5 -> "V"
    6 -> "VI"
    7 -> "VII"
    8 -> "VIII"
    9 -> "IX"
    10 -> "X"
    else -> n.toString()
  }
}
