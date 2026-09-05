package com.example.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.NoPhotography
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.ZoomIn
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NovelWithState
import com.example.model.StoryContentItem
import com.example.ui.components.BookmarksHighlightsModal
import com.example.ui.components.ChapterCommentsSection
import com.example.ui.components.GhostedSecurityBackground
import com.example.ui.components.StoryPhotoItem
import com.example.ui.components.StoryPhotoViewerModal
import com.example.ui.components.SubtleWatermarkOverlay
import com.example.ui.components.TypographyCustomizerModal
import com.example.ui.theme.AntiqueGold
import com.example.ui.theme.CharcoalSecondary
import com.example.ui.theme.CharcoalTertiary
import com.example.ui.theme.CharcoalText
import com.example.ui.theme.SoftCreamPaper
import com.example.ui.theme.SubtleBorder
import com.example.util.formatStatCount
import com.example.viewmodel.StrawberrycandyViewModel
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ReadingScreen(
  novel: NovelWithState,
  onBack: () -> Unit,
  onSaveProgress: (page: Int) -> Unit = {},
  onToggleFavorite: () -> Unit = {},
  viewModel: StrawberrycandyViewModel? = null,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onBack)

  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val scrollState = rememberScrollState()

  var viewingPhoto by remember { mutableStateOf<StoryContentItem.Photo?>(null) }
  var isTypographyModalOpen by remember { mutableStateOf(false) }
  var isBookmarksModalOpen by remember { mutableStateOf(false) }
  var activeHighlightedIndex by remember { mutableStateOf<Int?>(null) }

  // Shared preferences for reader typography & custom fonts
  val typographyPrefs = remember {
    context.getSharedPreferences("reader_typography_settings", Context.MODE_PRIVATE)
  }
  var selectedFontType by remember {
    mutableStateOf(typographyPrefs.getString("font_family_type", "serif") ?: "serif")
  }
  var customFontPath by remember {
    mutableStateOf(typographyPrefs.getString("custom_font_file_path", null))
  }
  var customFontName by remember {
    mutableStateOf(typographyPrefs.getString("custom_font_name", null))
  }
  var fontSizeScale by remember {
    mutableFloatStateOf(typographyPrefs.getFloat("font_size_scale", 1.0f))
  }
  var lineHeightScale by remember {
    mutableFloatStateOf(typographyPrefs.getFloat("line_height_scale", 1.0f))
  }

  // Dynamic Compose FontFamily constructed based on user selection / uploaded font file
  val readerFontFamily = remember(selectedFontType, customFontPath) {
    when (selectedFontType) {
      "sans" -> FontFamily.SansSerif
      "mono" -> FontFamily.Monospace
      "custom" -> {
        val path = customFontPath
        if (path != null && File(path).exists()) {
          try {
            FontFamily(Font(file = File(path)))
          } catch (e: Exception) {
            FontFamily.Serif
          }
        } else {
          FontFamily.Serif
        }
      }
      else -> FontFamily.Serif
    }
  }

  // Reactive state for chapter comments and bookmarks
  val commentsFlow = remember(novel.id, novel.chapterTitle, viewModel) {
    viewModel?.getCommentsForChapter(novel.id, novel.chapterTitle)
  }
  val commentsList by (commentsFlow?.collectAsState(initial = emptyList())
    ?: remember { mutableStateOf(emptyList()) })

  val bookmarksFlow = remember(novel.id, viewModel) {
    viewModel?.getBookmarksForNovel(novel.id)
  }
  val bookmarksList by (bookmarksFlow?.collectAsState(initial = emptyList())
    ?: remember { mutableStateOf(emptyList()) })

  val activeUser by (viewModel?.activeUser?.collectAsState()
    ?: remember { mutableStateOf(null) })

  // Track and save reader position so they can continue where they stopped reading
  val estimatedCurrentPage by remember(scrollState.maxValue) {
    derivedStateOf {
      if (scrollState.maxValue > 0) {
        val fraction = scrollState.value.toFloat() / scrollState.maxValue.toFloat()
        val calculated = (fraction * (novel.totalPages - 1)).toInt() + 1
        calculated.coerceIn(1, novel.totalPages)
      } else {
        novel.currentPage.coerceAtLeast(1)
      }
    }
  }

  // Auto-save reading progress as the user scrolls
  LaunchedEffect(estimatedCurrentPage) {
    if (estimatedCurrentPage != novel.currentPage) {
      onSaveProgress(estimatedCurrentPage)
    }
  }

  // Restore scroll position on initial load if continuing where reader stopped
  LaunchedEffect(novel.id) {
    if (novel.currentPage > 1 && scrollState.maxValue > 0) {
      val targetScroll = ((novel.currentPage - 1).toFloat() / (novel.totalPages - 1).toFloat() * scrollState.maxValue).toInt()
      scrollState.scrollTo(targetScroll)
    }
  }

  // Pure distraction-free reader canvas: soft cream background
  Box(
    modifier = modifier
      .fillMaxSize()
      .background(SoftCreamPaper)
      .testTag("reading_screen_container")
  ) {
    // 1. Soft natural lighting subtle gradient over cream paper
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          brush = Brush.verticalGradient(
            0.0f to Color(0x06FFFFFF),
            0.5f to Color(0x00FFFFFF),
            1.0f to Color(0x08000000)
          )
        )
    )

    // 2. Security Feature Visual Indicator
    GhostedSecurityBackground()

    // 3. Main Reading Content
    Column(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
    ) {
      // Top utility bar: Back arrow, Font customizer, Bookmarks list, Chapter comments jump, Favorite toggle
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        // Return to Bookshelf
        IconButton(
          onClick = onBack,
          modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .testTag("reading_back_arrow")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = "Return to Bookshelf",
            tint = CharcoalSecondary.copy(alpha = 0.8f),
            modifier = Modifier.size(20.dp)
          )
        }

        // Right-aligned controls: Font customization, Bookmarks, Chapter Comments, Favorite heart
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.padding(end = 6.dp)
        ) {
          // Typography & Custom Font Upload Button
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (selectedFontType == "custom") Color(0x18D4AF37) else Color(0x0E000000),
            border = BorderStroke(1.dp, if (selectedFontType == "custom") AntiqueGold.copy(alpha = 0.6f) else Color.Transparent),
            modifier = Modifier
              .clickable { isTypographyModalOpen = true }
              .testTag("typography_button")
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
            ) {
              Icon(
                imageVector = Icons.Outlined.FormatSize,
                contentDescription = "Reader typography",
                tint = if (selectedFontType == "custom") AntiqueGold else CharcoalSecondary,
                modifier = Modifier.size(15.dp)
              )
              Spacer(modifier = Modifier.width(3.dp))
              Text(
                text = if (selectedFontType == "custom") "Font" else "Aa",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 10.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = if (selectedFontType == "custom") AntiqueGold else CharcoalSecondary
                )
              )
            }
          }

          // Bookmarked Lines / Favorite Passages Button
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (bookmarksList.isNotEmpty()) Color(0x18D4AF37) else Color(0x0E000000),
            border = BorderStroke(1.dp, if (bookmarksList.isNotEmpty()) AntiqueGold.copy(alpha = 0.5f) else Color.Transparent),
            modifier = Modifier
              .clickable { isBookmarksModalOpen = true }
              .testTag("bookmarks_button")
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
            ) {
              Icon(
                imageVector = if (bookmarksList.isNotEmpty()) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                contentDescription = "Favorite lines & bookmarks",
                tint = if (bookmarksList.isNotEmpty()) AntiqueGold else CharcoalSecondary,
                modifier = Modifier.size(15.dp)
              )
              if (bookmarksList.isNotEmpty()) {
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                  text = bookmarksList.size.toString(),
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = AntiqueGold
                  )
                )
              }
            }
          }

          // Chapter Comments Counter & Scroll Jump Button
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0x0E000000),
            modifier = Modifier
              .clickable {
                coroutineScope.launch {
                  scrollState.animateScrollTo(scrollState.maxValue)
                }
              }
              .testTag("jump_to_comments_button")
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
            ) {
              Icon(
                imageVector = Icons.Outlined.ChatBubbleOutline,
                contentDescription = "Chapter comments",
                tint = CharcoalSecondary,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(3.dp))
              Text(
                text = commentsList.size.toString(),
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 10.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = CharcoalSecondary
                )
              )
            }
          }

          // Favorite Heart Toggle
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (novel.isFavorite) Color(0x1AC74350) else Color(0x0E000000),
            modifier = Modifier
              .clickable { onToggleFavorite() }
              .padding(horizontal = 2.dp)
              .testTag("reading_favorite_button")
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
            ) {
              Icon(
                imageVector = if (novel.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = "Toggle favorite",
                tint = if (novel.isFavorite) Color(0xFFC74350) else CharcoalSecondary.copy(alpha = 0.65f),
                modifier = Modifier.size(13.dp)
              )
              Spacer(modifier = Modifier.width(3.dp))
              Text(
                text = formatStatCount(novel.favoritesCount),
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 10.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = if (novel.isFavorite) Color(0xFFC74350) else CharcoalSecondary
                )
              )
            }
          }
        }
      }

      // Scrollable distraction-free reading canvas
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 28.dp, vertical = 8.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Spacer(modifier = Modifier.height(20.dp))

          // Chapter Header
          Text(
            text = novel.chapterTitle.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(
              letterSpacing = 2.4.sp,
              fontWeight = FontWeight.Medium
            ),
            color = AntiqueGold,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("reading_chapter_title")
          )

          Spacer(modifier = Modifier.height(14.dp))

          // Novel Title
          Text(
            text = novel.title,
            style = MaterialTheme.typography.headlineMedium.copy(
              fontFamily = readerFontFamily,
              fontWeight = FontWeight.Normal,
              lineHeight = 28.sp
            ),
            color = CharcoalText,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("reading_book_title")
          )

          Spacer(modifier = Modifier.height(8.dp))

          // Prominent display of who wrote the novel and who translated/curated it
          val authorHeader = if (novel.originalAuthor.isNotBlank()) {
            if (novel.authorSlot > 0) {
              "BY ${novel.originalAuthor.uppercase()} • TRANSLATED BY ${novel.author.uppercase()} (ROOM ${novel.authorSlot})"
            } else if (novel.author.isNotBlank() && !novel.author.equals(novel.originalAuthor, ignoreCase = true)) {
              "BY ${novel.originalAuthor.uppercase()} • CURATED BY ${novel.author.uppercase()}"
            } else {
              "BY ${novel.originalAuthor.uppercase()} • STRAWBERRYCANDY ARCHIVE"
            }
          } else if (novel.authorSlot > 0) {
            "BY ${novel.author.uppercase()} • ROOM ${novel.authorSlot} • STRAWBERRYCANDY"
          } else {
            "WRITTEN & PUBLISHED BY STRAWBERRYCANDY"
          }

          Text(
            text = authorHeader,
            style = MaterialTheme.typography.labelSmall.copy(
              letterSpacing = 1.6.sp,
              fontWeight = FontWeight.SemiBold
            ),
            color = AntiqueGold,
            textAlign = TextAlign.Center
          )

          Spacer(modifier = Modifier.height(28.dp))

          // Horizontal separator line
          Box(
            modifier = Modifier
              .width(36.dp)
              .height(1.dp)
              .background(AntiqueGold.copy(alpha = 0.35f))
          )

          Spacer(modifier = Modifier.height(28.dp))

          // Instruction hint banner for highlighting favorite lines
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0x0C000000),
            modifier = Modifier.padding(bottom = 16.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
              Icon(
                imageVector = Icons.Outlined.BookmarkBorder,
                contentDescription = null,
                tint = AntiqueGold,
                modifier = Modifier.size(13.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Tap any paragraph or favorite line to bookmark & highlight it",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 10.sp,
                  color = CharcoalSecondary
                )
              )
            }
          }

          // Distraction-Free Text Body and Story Photos
          Column(
            modifier = Modifier
              .widthIn(max = 640.dp)
              .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
          ) {
            val baseFontSize = 17.5f * fontSizeScale
            val baseLineHeight = 31f * fontSizeScale * lineHeightScale

            novel.storyItems.forEachIndexed { index, item ->
              when (item) {
                is StoryContentItem.Photo -> {
                  // Story illustration plate
                  Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF7F2)),
                    border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.35f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(vertical = 8.dp)
                      .clickable { viewingPhoto = item }
                      .testTag("story_photo_plate")
                  ) {
                    Column(
                      modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                      horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                      Box(
                        modifier = Modifier
                          .fillMaxWidth()
                          .height(230.dp)
                          .clip(RoundedCornerShape(10.dp))
                          .background(Color(0xFF22201E)),
                        contentAlignment = Alignment.Center
                      ) {
                        StoryPhotoItem(
                          uri = item.imageUri,
                          contentDescription = item.caption,
                          contentScale = ContentScale.Crop,
                          modifier = Modifier.fillMaxSize()
                        )

                        Surface(
                          shape = RoundedCornerShape(12.dp),
                          color = Color(0x95000000),
                          modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                        ) {
                          Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                          ) {
                            Icon(
                              imageVector = Icons.Outlined.ZoomIn,
                              contentDescription = null,
                              tint = SoftCreamPaper,
                              modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                              text = "Tap to view photo",
                              style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                color = SoftCreamPaper
                              )
                            )
                          }
                        }
                      }

                      if (item.caption.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                          text = item.caption,
                          style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = readerFontFamily,
                            fontStyle = FontStyle.Italic,
                            fontSize = 12.5.sp,
                            lineHeight = 17.sp
                          ),
                          color = CharcoalSecondary,
                          textAlign = TextAlign.Center,
                          modifier = Modifier.padding(horizontal = 8.dp)
                        )
                      }
                    }
                  }
                }

                is StoryContentItem.Text -> {
                  val paragraph = item.paragraph
                  val isBookmarked = bookmarksList.any {
                    it.paragraphIndex == index || it.quoteText == paragraph.trim()
                  }
                  val isActionsActive = activeHighlightedIndex == index

                  Column(modifier = Modifier.fillMaxWidth()) {
                    // Paragraph Box with tap to highlight / bookmark
                    Box(
                      modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                          if (isBookmarked) Color(0xFFF9F4E8)
                          else if (isActionsActive) Color(0x0E000000)
                          else Color.Transparent
                        )
                        .border(
                          border = if (isBookmarked) {
                            BorderStroke(1.2.dp, AntiqueGold.copy(alpha = 0.65f))
                          } else if (isActionsActive) {
                            BorderStroke(1.dp, SubtleBorder)
                          } else {
                            BorderStroke(0.dp, Color.Transparent)
                          },
                          shape = RoundedCornerShape(12.dp)
                        )
                        .clickable {
                          activeHighlightedIndex = if (isActionsActive) null else index
                        }
                        .padding(
                          horizontal = if (isBookmarked || isActionsActive) 12.dp else 0.dp,
                          vertical = if (isBookmarked || isActionsActive) 8.dp else 0.dp
                        )
                    ) {
                      Column {
                        if (isBookmarked) {
                          Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 6.dp)
                          ) {
                            Icon(
                              imageVector = Icons.Filled.Bookmark,
                              contentDescription = null,
                              tint = AntiqueGold,
                              modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                              text = "BOOKMARKED FAVORITE PASSAGE",
                              style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.5.sp,
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.Bold
                              ),
                              color = AntiqueGold
                            )
                          }
                        }

                        if (index == 0 && paragraph.isNotEmpty()) {
                          // Editorial Drop Cap treatment on opening paragraph
                          val firstChar = paragraph.take(1)
                          val restOfFirst = paragraph.drop(1)

                          Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                              text = firstChar,
                              style = MaterialTheme.typography.displayLarge.copy(
                                fontFamily = readerFontFamily,
                                fontWeight = FontWeight.Light,
                                fontSize = (62f * fontSizeScale).sp,
                                lineHeight = (60f * fontSizeScale).sp
                              ),
                              color = AntiqueGold,
                              modifier = Modifier.padding(end = 10.dp, bottom = 2.dp)
                            )
                            Text(
                              text = restOfFirst,
                              style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = readerFontFamily,
                                color = CharcoalText,
                                lineHeight = baseLineHeight.sp,
                                fontSize = baseFontSize.sp
                              )
                            )
                          }
                        } else {
                          Text(
                            text = paragraph,
                            style = MaterialTheme.typography.bodyLarge.copy(
                              fontFamily = readerFontFamily,
                              color = CharcoalText,
                              lineHeight = baseLineHeight.sp,
                              fontSize = baseFontSize.sp
                            ),
                            modifier = Modifier.fillMaxWidth()
                          )
                        }
                      }
                    }

                    // Interactive Action Bar when paragraph is tapped
                    AnimatedVisibility(
                      visible = isActionsActive,
                      enter = fadeIn(),
                      exit = fadeOut()
                    ) {
                      Row(
                        modifier = Modifier
                          .fillMaxWidth()
                          .padding(top = 8.dp, start = 4.dp, end = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        OutlinedButton(
                          onClick = {
                            viewModel?.toggleBookmarkLine(
                              novelId = novel.id,
                              chapterTitle = novel.chapterTitle,
                              quoteText = paragraph,
                              paragraphIndex = index
                            )
                            activeHighlightedIndex = null
                          },
                          shape = RoundedCornerShape(10.dp),
                          colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isBookmarked) Color(0x18D4AF37) else Color.Transparent,
                            contentColor = AntiqueGold
                          ),
                          border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.7f)),
                          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                          Icon(
                            imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.Bookmark,
                            contentDescription = null,
                            tint = AntiqueGold,
                            modifier = Modifier.size(13.dp)
                          )
                          Spacer(modifier = Modifier.width(4.dp))
                          Text(
                            text = if (isBookmarked) "Remove Bookmark" else "Bookmark Line",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = AntiqueGold
                          )
                        }

                        OutlinedButton(
                          onClick = {
                            activeHighlightedIndex = null
                            coroutineScope.launch {
                              scrollState.animateScrollTo(scrollState.maxValue)
                            }
                          },
                          shape = RoundedCornerShape(10.dp),
                          border = BorderStroke(1.dp, SubtleBorder),
                          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                          Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = null,
                            tint = CharcoalSecondary,
                            modifier = Modifier.size(13.dp)
                          )
                          Spacer(modifier = Modifier.width(4.dp))
                          Text(
                            text = "Comment on Chapter",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = CharcoalSecondary
                          )
                        }
                      }
                    }
                  }
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(44.dp))

          // Manuscript colophon mark
          Text(
            text = "— ❦ —",
            style = MaterialTheme.typography.bodyMedium,
            color = AntiqueGold.copy(alpha = 0.6f)
          )

          Spacer(modifier = Modifier.height(8.dp))

          Text(
            text = "Read by ${formatStatCount(novel.readsCount)} readers • Favorited by ${formatStatCount(novel.favoritesCount)} members",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 9.5.sp,
              letterSpacing = 0.8.sp
            ),
            color = CharcoalTertiary
          )

          Spacer(modifier = Modifier.height(36.dp))

          // Interactive Chapter Comment Section
          ChapterCommentsSection(
            chapterTitle = novel.chapterTitle,
            comments = commentsList,
            activeReaderName = activeUser?.displayName,
            onPostComment = { text, penName ->
              viewModel?.postComment(novel.id, novel.chapterTitle, text, penName)
            },
            onLikeComment = { commentId ->
              viewModel?.likeComment(commentId)
            },
            modifier = Modifier.widthIn(max = 640.dp)
          )

          Spacer(modifier = Modifier.height(48.dp))
        }

        // Subtle watermark overlaying the text
        SubtleWatermarkOverlay(
          modifier = Modifier.matchParentSize(),
          text = "LICENSED TO READER • STRAWBERRYCANDY SECURE ARCHIVE • PROPRIETARY COPY",
          textColor = CharcoalText.copy(alpha = 0.040f)
        )
      }

      // Minimal running footer with saved reading progress
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 24.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "PAGE $estimatedCurrentPage OF ${novel.totalPages}  •  ${novel.editionNumber}",
          style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 9.sp,
            letterSpacing = 1.5.sp
          ),
          color = CharcoalTertiary.copy(alpha = 0.65f),
          textAlign = TextAlign.Center,
          modifier = Modifier.testTag("reading_folio_page_number")
        )
      }
    }

    // Lightbox viewer for story photos
    if (viewingPhoto != null) {
      StoryPhotoViewerModal(
        photoUri = viewingPhoto!!.imageUri,
        caption = viewingPhoto!!.caption,
        novelTitle = novel.title,
        onDismiss = { viewingPhoto = null }
      )
    }

    // Typography & Font Customizer Modal (including upload user font)
    if (isTypographyModalOpen) {
      TypographyCustomizerModal(
        currentFontType = selectedFontType,
        customFontName = customFontName,
        fontSizeScale = fontSizeScale,
        lineHeightScale = lineHeightScale,
        onSelectFontType = { type ->
          selectedFontType = type
          typographyPrefs.edit().putString("font_family_type", type).apply()
        },
        onCustomFontUploaded = { path, name ->
          customFontPath = path
          customFontName = name
          selectedFontType = "custom"
          typographyPrefs.edit()
            .putString("font_family_type", "custom")
            .putString("custom_font_file_path", path)
            .putString("custom_font_name", name)
            .apply()
        },
        onUpdateFontSizeScale = { scale ->
          fontSizeScale = scale
          typographyPrefs.edit().putFloat("font_size_scale", scale).apply()
        },
        onUpdateLineHeightScale = { scale ->
          lineHeightScale = scale
          typographyPrefs.edit().putFloat("line_height_scale", scale).apply()
        },
        onResetDefaults = {
          selectedFontType = "serif"
          fontSizeScale = 1.0f
          lineHeightScale = 1.0f
          typographyPrefs.edit()
            .putString("font_family_type", "serif")
            .putFloat("font_size_scale", 1.0f)
            .putFloat("line_height_scale", 1.0f)
            .apply()
        },
        onDismiss = { isTypographyModalOpen = false }
      )
    }

    // Favorite Lines & Bookmarks Highlights Modal
    if (isBookmarksModalOpen) {
      BookmarksHighlightsModal(
        bookmarks = bookmarksList,
        onJumpToParagraph = { paragraphIndex ->
          activeHighlightedIndex = paragraphIndex
        },
        onDeleteBookmark = { bookmarkId ->
          viewModel?.deleteBookmark(bookmarkId)
        },
        onDismiss = { isBookmarksModalOpen = false }
      )
    }
  }
}
