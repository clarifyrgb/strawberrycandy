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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.ChapterCommentEntity
import com.example.data.local.ReaderProfileEntity
import com.example.model.NovelWithState
import com.example.ui.theme.AntiqueGold
import com.example.ui.theme.AntiqueGoldLight
import com.example.ui.theme.CharcoalSecondary
import com.example.ui.theme.CharcoalTertiary
import com.example.ui.theme.CharcoalText
import com.example.ui.theme.SoftCreamPaper
import com.example.ui.theme.SubtleBorder
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class ProfileModalTab {
  MY_SHELF,
  DETAILS_AND_NAME,
  COMMENT_HISTORY
}

private enum class ReaderShelfFilter {
  READING,
  TO_BE_READ,
  FINISHED,
  FAVORITES
}

@Composable
fun ReaderProfileModal(
  activeUser: ReaderProfileEntity,
  readingCount: Int,
  finishedCount: Int,
  toBeReadCount: Int,
  isSoleOwner: Boolean,
  canUpload: Boolean = false,
  commentsHistory: List<ChapterCommentEntity> = emptyList(),
  novelsList: List<NovelWithState> = emptyList(),
  onDismiss: () -> Unit,
  onChangePenName: (String) -> Unit,
  onDeleteComment: (String) -> Unit = {},
  onSelectNovel: (NovelWithState) -> Unit = {},
  onSwitchAccount: () -> Unit,
  onSignOut: () -> Unit,
  onOpenAbout: () -> Unit = {},
  onOpenUpload: () -> Unit = {},
  onOpenAuthorRooms: () -> Unit = {},
) {
  val emailRegex = remember { Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}") }
  val currentSafeName = remember(activeUser.displayName) {
    activeUser.displayName.replace(emailRegex, "").substringBefore("@").trim().ifBlank {
      if (activeUser.role == "TRANSLATOR") "Translator" else "Reader"
    }
  }

  var editedName by remember(currentSafeName) { mutableStateOf(currentSafeName) }
  var isNameSavedJustNow by remember { mutableStateOf(false) }
  var selectedTab by remember { mutableStateOf(ProfileModalTab.MY_SHELF) }
  var selectedShelfCategory by remember { mutableStateOf(ReaderShelfFilter.READING) }

  var liveTickerMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
  LaunchedEffect(Unit) {
    while (true) {
      delay(4000L)
      liveTickerMs = System.currentTimeMillis()
    }
  }

  val isNameChanged = editedName.trim().isNotBlank() && editedName.trim() != currentSafeName

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Card(
      shape = RoundedCornerShape(26.dp),
      colors = CardDefaults.cardColors(containerColor = SoftCreamPaper),
      border = BorderStroke(1.dp, SubtleBorder),
      elevation = CardDefaults.cardElevation(defaultElevation = 14.dp),
      modifier = Modifier
        .fillMaxWidth(0.92f)
        .widthIn(max = 480.dp)
        .heightIn(max = 720.dp)
        .padding(horizontal = 8.dp, vertical = 16.dp)
        .testTag("reader_profile_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Header Row
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
                .background(AntiqueGold)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "READER ARCHIVE PASSPORT",
              style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 1.4.sp,
                fontWeight = FontWeight.Bold
              ),
              color = AntiqueGold
            )
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
              onClick = {
                onDismiss()
                onSignOut()
              },
              shape = RoundedCornerShape(8.dp),
              color = Color(0xFFFDEDEC),
              border = BorderStroke(1.dp, Color(0xFFE57373)),
              modifier = Modifier.testTag("profile_top_sign_out_button")
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              ) {
                Icon(
                  imageVector = Icons.Outlined.Logout,
                  contentDescription = "Sign Out",
                  tint = Color(0xFFC62828),
                  modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                  text = "Sign Out",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC62828)
                  )
                )
              }
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
              onClick = onDismiss,
              modifier = Modifier
                .size(28.dp)
                .testTag("profile_close_button")
            ) {
              Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Close dialog",
                tint = CharcoalSecondary,
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Segmented Tabs: My Shelf vs Profile & Edit Name vs Comment History
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x0E000000))
            .padding(3.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          // Tab 1: My Shelf (TBR, Finished, Reading, Favorites)
          Surface(
            onClick = { selectedTab = ProfileModalTab.MY_SHELF },
            shape = RoundedCornerShape(10.dp),
            color = if (selectedTab == ProfileModalTab.MY_SHELF) SoftCreamPaper else Color.Transparent,
            shadowElevation = if (selectedTab == ProfileModalTab.MY_SHELF) 2.dp else 0.dp,
            modifier = Modifier
              .weight(1f)
              .testTag("tab_reader_my_shelf")
          ) {
            Row(
              modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Filled.MenuBook,
                contentDescription = null,
                tint = if (selectedTab == ProfileModalTab.MY_SHELF) AntiqueGold else CharcoalSecondary,
                modifier = Modifier.size(13.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "My Shelf",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = if (selectedTab == ProfileModalTab.MY_SHELF) FontWeight.Bold else FontWeight.Medium,
                  fontSize = 10.5.sp
                ),
                color = if (selectedTab == ProfileModalTab.MY_SHELF) CharcoalText else CharcoalSecondary
              )
            }
          }

          // Tab 2: Profile & Edit Name
          Surface(
            onClick = { selectedTab = ProfileModalTab.DETAILS_AND_NAME },
            shape = RoundedCornerShape(10.dp),
            color = if (selectedTab == ProfileModalTab.DETAILS_AND_NAME) SoftCreamPaper else Color.Transparent,
            shadowElevation = if (selectedTab == ProfileModalTab.DETAILS_AND_NAME) 2.dp else 0.dp,
            modifier = Modifier
              .weight(1f)
              .testTag("tab_reader_profile_edit")
          ) {
            Row(
              modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = null,
                tint = if (selectedTab == ProfileModalTab.DETAILS_AND_NAME) AntiqueGold else CharcoalSecondary,
                modifier = Modifier.size(13.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Profile",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = if (selectedTab == ProfileModalTab.DETAILS_AND_NAME) FontWeight.Bold else FontWeight.Medium,
                  fontSize = 10.5.sp
                ),
                color = if (selectedTab == ProfileModalTab.DETAILS_AND_NAME) CharcoalText else CharcoalSecondary
              )
            }
          }

          // Tab 3: Comment History
          Surface(
            onClick = { selectedTab = ProfileModalTab.COMMENT_HISTORY },
            shape = RoundedCornerShape(10.dp),
            color = if (selectedTab == ProfileModalTab.COMMENT_HISTORY) SoftCreamPaper else Color.Transparent,
            shadowElevation = if (selectedTab == ProfileModalTab.COMMENT_HISTORY) 2.dp else 0.dp,
            modifier = Modifier
              .weight(1f)
              .testTag("tab_reader_comment_history")
          ) {
            Row(
              modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Outlined.ChatBubbleOutline,
                contentDescription = null,
                tint = if (selectedTab == ProfileModalTab.COMMENT_HISTORY) AntiqueGold else CharcoalSecondary,
                modifier = Modifier.size(13.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Comments (${commentsHistory.size})",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = if (selectedTab == ProfileModalTab.COMMENT_HISTORY) FontWeight.Bold else FontWeight.Medium,
                  fontSize = 10.5.sp
                ),
                color = if (selectedTab == ProfileModalTab.COMMENT_HISTORY) CharcoalText else CharcoalSecondary
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTab) {
          ProfileModalTab.MY_SHELF -> {
            val readingNovels = remember(novelsList) { novelsList.filter { it.inReadingList && !it.isFinished } }
            val tbrNovels = remember(novelsList) { novelsList.filter { it.isToBeRead || it.inTbrList } }
            val finishedNovels = remember(novelsList) { novelsList.filter { it.isFinished } }
            val favoriteNovels = remember(novelsList) { novelsList.filter { it.isFavorite } }

            // Sub-category filters for My Shelf
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              listOf(
                Triple(ReaderShelfFilter.READING, "Reading (${readingNovels.size})", Icons.Filled.MenuBook),
                Triple(ReaderShelfFilter.TO_BE_READ, "TBR (${tbrNovels.size})", Icons.Filled.Bookmark),
                Triple(ReaderShelfFilter.FINISHED, "Finished (${finishedNovels.size})", Icons.Filled.CheckCircle),
                Triple(ReaderShelfFilter.FAVORITES, "Favs (${favoriteNovels.size})", Icons.Filled.Favorite),
              ).forEach { (cat, label, icon) ->
                val isSel = selectedShelfCategory == cat
                Surface(
                  onClick = { selectedShelfCategory = cat },
                  shape = RoundedCornerShape(10.dp),
                  color = if (isSel) CharcoalText else SoftCreamPaper,
                  border = BorderStroke(1.dp, if (isSel) CharcoalText else SubtleBorder),
                  modifier = Modifier.weight(1f)
                ) {
                  Row(
                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(
                      imageVector = icon,
                      contentDescription = null,
                      tint = if (isSel) AntiqueGold else CharcoalSecondary,
                      modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                      text = label,
                      style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.5.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                      ),
                      color = if (isSel) SoftCreamPaper else CharcoalSecondary,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                }
              }
            }

            val currentFilteredNovels = when (selectedShelfCategory) {
              ReaderShelfFilter.READING -> readingNovels
              ReaderShelfFilter.TO_BE_READ -> tbrNovels
              ReaderShelfFilter.FINISHED -> finishedNovels
              ReaderShelfFilter.FAVORITES -> favoriteNovels
            }

            if (currentFilteredNovels.isEmpty()) {
              Surface(
                shape = RoundedCornerShape(16.dp),
                color = SoftCreamPaper,
                border = BorderStroke(1.dp, SubtleBorder),
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 16.dp)
              ) {
                Column(
                  modifier = Modifier.padding(24.dp),
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  Icon(
                    imageVector = when (selectedShelfCategory) {
                      ReaderShelfFilter.READING -> Icons.Filled.MenuBook
                      ReaderShelfFilter.TO_BE_READ -> Icons.Filled.Bookmark
                      ReaderShelfFilter.FINISHED -> Icons.Filled.CheckCircle
                      ReaderShelfFilter.FAVORITES -> Icons.Filled.Favorite
                    },
                    contentDescription = null,
                    tint = AntiqueGold,
                    modifier = Modifier.size(32.dp)
                  )
                  Spacer(modifier = Modifier.height(10.dp))
                  Text(
                    text = when (selectedShelfCategory) {
                      ReaderShelfFilter.READING -> "No novels currently being read"
                      ReaderShelfFilter.TO_BE_READ -> "Your TBR (To-Be-Read) shelf is empty"
                      ReaderShelfFilter.FINISHED -> "No novels finished yet"
                      ReaderShelfFilter.FAVORITES -> "No favorited novels yet"
                    },
                    style = MaterialTheme.typography.titleSmall.copy(
                      fontFamily = FontFamily.Serif,
                      fontWeight = FontWeight.Bold
                    ),
                    color = CharcoalText
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = "Explore the strawberrycandy archive to discover and save new titles to your shelf.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                    color = CharcoalSecondary,
                    textAlign = TextAlign.Center
                  )
                }
              }
            } else {
              Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                currentFilteredNovels.forEach { novel ->
                  Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SoftCreamPaper),
                    border = BorderStroke(1.dp, SubtleBorder),
                    modifier = Modifier
                      .fillMaxWidth()
                      .clickable {
                        onDismiss()
                        onSelectNovel(novel)
                      }
                  ) {
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      // Cover thumbnail
                      Box(
                        modifier = Modifier
                          .size(width = 46.dp, height = 64.dp)
                          .clip(RoundedCornerShape(6.dp))
                          .background(Color(novel.coverColorHex)),
                        contentAlignment = Alignment.Center
                      ) {
                        if (novel.coverImageUri != null) {
                          AsyncImage(
                            model = novel.coverImageUri,
                            contentDescription = novel.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                          )
                        } else {
                          Text(
                            text = novel.title.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(
                              fontFamily = FontFamily.Serif,
                              fontWeight = FontWeight.Bold,
                              color = Color.White
                            )
                          )
                        }
                      }

                      Spacer(modifier = Modifier.width(12.dp))

                      Column(modifier = Modifier.weight(1f)) {
                        Row(
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                          // Status badge
                          Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (novel.isCompletedNovel) Color(0xFF2E7D32).copy(alpha = 0.15f) else Color(0xFFD87D2A).copy(alpha = 0.15f)
                          ) {
                            Text(
                              text = if (novel.isCompletedNovel) "✓ FINISHED" else "• ONGOING",
                              style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (novel.isCompletedNovel) Color(0xFF2E7D32) else Color(0xFFD87D2A)
                              ),
                              modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
                            )
                          }
                          Text(
                            text = "${novel.totalPages} pages",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                            color = CharcoalSecondary
                          )
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                          text = novel.title,
                          style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                          ),
                          color = CharcoalText,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis
                        )

                        Text(
                          text = novel.authorLabel,
                          style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                          color = CharcoalSecondary,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis
                        )

                        if (selectedShelfCategory == ReaderShelfFilter.READING) {
                          Spacer(modifier = Modifier.height(4.dp))
                          LinearProgressIndicator(
                            progress = { novel.progressFraction },
                            modifier = Modifier
                              .fillMaxWidth()
                              .height(3.dp)
                              .clip(RoundedCornerShape(2.dp)),
                            color = AntiqueGold,
                            trackColor = CharcoalText.copy(alpha = 0.1f),
                          )
                          Spacer(modifier = Modifier.height(2.dp))
                          Text(
                            text = "Page ${novel.currentPage} of ${novel.totalPages} (${(novel.progressFraction * 100).toInt()}%)",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                            color = AntiqueGold
                          )
                        }
                      }

                      Spacer(modifier = Modifier.width(8.dp))

                      // Action Button
                      Button(
                        onClick = {
                          onDismiss()
                          onSelectNovel(novel)
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CharcoalText),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                      ) {
                        Text(
                          text = novel.readButtonLabel,
                          style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = SoftCreamPaper
                          )
                        )
                      }
                    }
                  }
                }
              }
            }
          }
          ProfileModalTab.DETAILS_AND_NAME -> {
            // Avatar & Identity Pill
            Box(
              modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(if (activeUser.provider == "GOOGLE") Color(0xFF4285F4) else Color(0xFF1E1D1B)),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = activeUser.avatarInitial.ifBlank { "R" },
                style = MaterialTheme.typography.headlineMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = Color.White
                )
              )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
              text = currentSafeName,
              style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold
              ),
              color = CharcoalText,
              textAlign = TextAlign.Center
            )

            if (activeUser.email.isNotBlank()) {
              Text(
                text = activeUser.email,
                style = MaterialTheme.typography.bodySmall.copy(
                  fontSize = 11.sp,
                  color = CharcoalSecondary
                )
              )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Role Badge
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (isSoleOwner) AntiqueGold.copy(alpha = 0.15f) else CharcoalText.copy(alpha = 0.08f),
              border = BorderStroke(0.8.dp, if (isSoleOwner) AntiqueGold else SubtleBorder)
            ) {
              Text(
                text = if (isSoleOwner) "ARCHIVE OWNER • CLARIFY" else if (activeUser.role == "TRANSLATOR") "CONTRIBUTING TRANSLATOR • ROOM ${activeUser.authorSlot ?: 1}" else "LITERARY ARCHIVE READER",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 8.5.sp,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 1.sp
                ),
                color = if (isSoleOwner) AntiqueGold else CharcoalText,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
              )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isSoleOwner || canUpload) {
              Button(
                onClick = {
                  onDismiss()
                  onOpenUpload()
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AntiqueGold),
                modifier = Modifier
                  .fillMaxWidth()
                  .height(44.dp)
                  .testTag("profile_upload_novel_button")
              ) {
                Icon(
                  imageVector = Icons.Outlined.Upload,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Upload Novel Manuscript",
                  style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 12.sp
                  )
                )
              }
              Spacer(modifier = Modifier.height(16.dp))
            }

            if (isSoleOwner) {
              Button(
                onClick = {
                  onDismiss()
                  onOpenAuthorRooms()
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CharcoalText),
                modifier = Modifier
                  .fillMaxWidth()
                  .height(44.dp)
                  .testTag("profile_manage_translators_button")
              ) {
                Icon(
                  imageVector = Icons.Outlined.Person,
                  contentDescription = null,
                  tint = SoftCreamPaper,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Manage Translators & Grant Access",
                  style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = SoftCreamPaper,
                    fontSize = 12.sp
                  )
                )
              }
              Spacer(modifier = Modifier.height(16.dp))
            }

            // Reading Stats Card
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = AntiqueGoldLight.copy(alpha = 0.35f),
              border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.4f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 12.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
              ) {
                // Reading
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = Icons.Filled.MenuBook,
                      contentDescription = null,
                      tint = AntiqueGold,
                      modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = "$readingCount",
                      style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                      color = CharcoalText
                    )
                  }
                  Text(
                    text = "Reading",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = CharcoalTertiary
                  )
                }

                // Finished
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = Icons.Filled.CheckCircle,
                      contentDescription = null,
                      tint = Color(0xFF2E7D32),
                      modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = "$finishedCount",
                      style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                      color = CharcoalText
                    )
                  }
                  Text(
                    text = "Finished",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = CharcoalTertiary
                  )
                }

                // To Read
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = Icons.Filled.Bookmark,
                      contentDescription = null,
                      tint = CharcoalSecondary,
                      modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = "$toBeReadCount",
                      style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                      color = CharcoalText
                    )
                  }
                  Text(
                    text = "To Read",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = CharcoalTertiary
                  )
                }

                // Points Badge
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = Icons.Filled.Star,
                      contentDescription = null,
                      tint = AntiqueGold,
                      modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = "${activeUser.penNamePoints}",
                      style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                      color = AntiqueGold
                    )
                  }
                  Text(
                    text = "Points",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = AntiqueGold
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // EDIT READER NAME SECTION (Freely editable by readers!)
            Card(
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(containerColor = SoftCreamPaper),
              border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.5f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(16.dp)
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = Icons.Outlined.Edit,
                      contentDescription = null,
                      tint = AntiqueGold,
                      modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = "EDIT READER NAME",
                      style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                      ),
                      color = CharcoalText
                    )
                  }

                  if (isNameSavedJustNow) {
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
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                      )
                    }
                  }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                  text = "Customize how your name appears on chapter comments and literary reflections.",
                  style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    color = CharcoalSecondary
                  )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                  value = editedName,
                  onValueChange = {
                    val sanitized = it.replace(emailRegex, "").substringBefore("@")
                    editedName = sanitized
                    isNameSavedJustNow = false
                  },
                  label = { Text("Display Name / Pen Name") },
                  placeholder = { Text("e.g., Charlotte Sterling") },
                  singleLine = true,
                  trailingIcon = {
                    if (editedName.isNotBlank()) {
                      IconButton(onClick = { editedName = "" }) {
                        Icon(
                          imageVector = Icons.Outlined.Close,
                          contentDescription = "Clear",
                          tint = CharcoalTertiary,
                          modifier = Modifier.size(14.dp)
                        )
                      }
                    }
                  },
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AntiqueGold,
                    unfocusedBorderColor = SubtleBorder
                  ),
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_pen_name_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                  onClick = {
                    if (isNameChanged) {
                      onChangePenName(editedName.trim())
                      isNameSavedJustNow = true
                    }
                  },
                  enabled = isNameChanged,
                  shape = RoundedCornerShape(12.dp),
                  colors = ButtonDefaults.buttonColors(
                    containerColor = AntiqueGold,
                    disabledContainerColor = AntiqueGold.copy(alpha = 0.35f)
                  ),
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .testTag("submit_pen_name_change_button")
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = SoftCreamPaper,
                    modifier = Modifier.size(15.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = if (isNameChanged) "Save New Name" else "Name Unchanged",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = FontWeight.Bold,
                      fontSize = 11.5.sp,
                      color = SoftCreamPaper
                    )
                  )
                }


              }
            }
          }

          ProfileModalTab.COMMENT_HISTORY -> {
            // COMMENT HISTORY SECTION
            Column(
              modifier = Modifier.fillMaxWidth(),
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "YOUR CHAPTER REFLECTIONS",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                  ),
                  color = AntiqueGold
                )
                Text(
                  text = "${commentsHistory.size} recorded",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.5.sp,
                    color = CharcoalTertiary
                  )
                )
              }

              if (commentsHistory.isEmpty()) {
                Card(
                  shape = RoundedCornerShape(16.dp),
                  colors = CardDefaults.cardColors(containerColor = SoftCreamPaper),
                  border = BorderStroke(1.dp, SubtleBorder),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                  ) {
                    Icon(
                      imageVector = Icons.Outlined.ChatBubbleOutline,
                      contentDescription = null,
                      tint = AntiqueGold.copy(alpha = 0.6f),
                      modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                      text = "No Reflections Yet",
                      style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold
                      ),
                      color = CharcoalText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                      text = "As you read novel chapters, leave your thoughts, reactions, and commentary. Your entire reflection history will be cataloged here.",
                      style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                      ),
                      color = CharcoalSecondary,
                      textAlign = TextAlign.Center
                    )
                  }
                }
              } else {
                commentsHistory.forEach { comment ->
                  val novelTitle = novelsList.find { it.id == comment.novelId }?.title ?: "Archival Novel"
                  val formattedDate = formatRealtimeModalDate(comment.timestamp, liveTickerMs)
                  val isRecent = (liveTickerMs - comment.timestamp) in 0..120_000L

                  Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SoftCreamPaper),
                    border = BorderStroke(1.dp, if (isRecent) AntiqueGold.copy(alpha = 0.4f) else SubtleBorder),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Column(
                      modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                    ) {
                      // Novel & Chapter header + Delete button
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Column(modifier = Modifier.weight(1f)) {
                          Text(
                            text = novelTitle,
                            style = MaterialTheme.typography.labelSmall.copy(
                              fontWeight = FontWeight.Bold,
                              fontSize = 11.sp
                            ),
                            color = AntiqueGold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                          )
                          Text(
                            text = comment.chapterTitle,
                            style = MaterialTheme.typography.labelSmall.copy(
                              fontSize = 9.sp,
                              fontWeight = FontWeight.Medium
                            ),
                            color = CharcoalSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                          )
                        }

                        // Date & Delete button
                        Row(verticalAlignment = Alignment.CenterVertically) {
                          Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                            color = CharcoalTertiary
                          )

                          Spacer(modifier = Modifier.width(4.dp))

                          IconButton(
                            onClick = { onDeleteComment(comment.id) },
                            modifier = Modifier.size(24.dp)
                          ) {
                            Icon(
                              imageVector = Icons.Outlined.Delete,
                              contentDescription = "Delete comment",
                              tint = Color(0xFFC62828).copy(alpha = 0.6f),
                              modifier = Modifier.size(14.dp)
                            )
                          }
                        }
                      }

                      Spacer(modifier = Modifier.height(6.dp))

                      // Comment Text
                      Text(
                        text = "\"${comment.commentText}\"",
                        style = MaterialTheme.typography.bodySmall.copy(
                          fontSize = 11.5.sp,
                          fontFamily = FontFamily.Serif
                        ),
                        color = CharcoalText
                      )

                      if (comment.likesCount > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                          Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = "Likes",
                            tint = Color(0xFFC74350),
                            modifier = Modifier.size(10.dp)
                          )
                          Spacer(modifier = Modifier.width(3.dp))
                          Text(
                            text = "${comment.likesCount} liked",
                            style = MaterialTheme.typography.labelSmall.copy(
                              fontSize = 8.5.sp,
                              color = Color(0xFFC74350)
                            )
                          )
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // Account Switch & Prominent Sign Out Button
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedButton(
            onClick = {
              onDismiss()
              onSwitchAccount()
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = CharcoalText),
            border = BorderStroke(1.dp, SubtleBorder),
            modifier = Modifier
              .weight(1f)
              .testTag("profile_switch_account_button")
          ) {
            Text("Switch Account", fontSize = 11.sp)
          }

          OutlinedButton(
            onClick = {
              onDismiss()
              onSignOut()
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
              containerColor = Color(0xFFFDEDEC),
              contentColor = Color(0xFFC62828)
            ),
            border = BorderStroke(1.dp, Color(0xFFF5C6CB)),
            modifier = Modifier
              .weight(1f)
              .testTag("profile_sign_out_button")
          ) {
            Icon(
              imageVector = Icons.Outlined.Logout,
              contentDescription = "Sign Out",
              tint = Color(0xFFC62828),
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
              text = "Sign Out",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 11.5.sp
              ),
              color = Color(0xFFC62828)
            )
          }
        }
      }
    }
  }
}

private fun formatRealtimeModalDate(timestamp: Long, liveNow: Long): String {
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
