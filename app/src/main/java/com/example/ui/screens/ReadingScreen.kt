package com.example.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BookChapter
import com.example.model.NovelWithState
import com.example.model.SearchMatch
import com.example.model.StoryContentItem
import com.example.ui.components.BookmarksHighlightsModal
import com.example.ui.components.ChapterCommentsSection
import com.example.ui.components.ChapterSelectionModal
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

data class ReadingPage(
  val pageIndex: Int,
  val displayPageNumber: Int,
  val chapterIndex: Int,
  val chapterTitle: String,
  val chapterNumber: Int,
  val items: List<StoryContentItem>,
  val isChapterFirstPage: Boolean = false,
)

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

  // Typography preferences
  val typographyPrefs = remember {
    context.getSharedPreferences("reader_typography_settings", Context.MODE_PRIVATE)
  }

  // Page turning mode: "flip" (stylish 3D book pages) vs "scroll" (continuous vertical)
  var pageTurnMode by remember {
    mutableStateOf(typographyPrefs.getString("reading_turn_mode", "flip") ?: "flip")
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
  var paragraphSpacingScale by remember {
    mutableFloatStateOf(typographyPrefs.getFloat("paragraph_spacing_scale", 1.0f))
  }
  var isJustified by remember {
    mutableStateOf(typographyPrefs.getBoolean("is_justified", false))
  }
  var isFirstLineIndent by remember {
    mutableStateOf(typographyPrefs.getBoolean("is_first_line_indent", false))
  }
  var isBold by remember {
    mutableStateOf(typographyPrefs.getBoolean("is_bold", false))
  }
  var isItalic by remember {
    mutableStateOf(typographyPrefs.getBoolean("is_italic", false))
  }
  val readerFontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
  val readerFontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal

  // Modal states
  var viewingPhoto by remember { mutableStateOf<StoryContentItem.Photo?>(null) }
  var isTypographyModalOpen by remember { mutableStateOf(false) }
  var isBookmarksModalOpen by remember { mutableStateOf(false) }
  var isChapterModalOpen by remember { mutableStateOf(false) }
  var isHudVisible by remember { mutableStateOf(true) }
  var activeHighlightedIndex by remember { mutableStateOf<Int?>(null) }

  // In-Book Search state
  var isSearchOpen by remember { mutableStateOf(false) }
  var searchQuery by remember { mutableStateOf("") }
  var currentSearchMatchIndex by remember { mutableIntStateOf(0) }
  var isSearchResultsListOpen by remember { mutableStateOf(false) }

  // Reactive comments & bookmarks
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

  // Font family resolution
  val readerFontFamily = remember(selectedFontType, customFontPath) {
    when (selectedFontType) {
      "sans" -> FontFamily.SansSerif
      "mono" -> FontFamily.Monospace
      "cursive" -> FontFamily.Cursive
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

  // Build paginated pages for Flip mode
  val pages = remember(novel.storyItems, novel.chapters) {
    buildReadingPages(novel)
  }

  // Initial page calculation
  val initialPageIndex = remember(novel.currentPage, pages.size) {
    if (pages.isNotEmpty() && novel.totalPages > 0) {
      val frac = (novel.currentPage - 1).toFloat() / novel.totalPages.toFloat()
      (frac * pages.size).toInt().coerceIn(0, pages.size - 1)
    } else 0
  }

  val pagerState = rememberPagerState(initialPage = initialPageIndex) { pages.size }

  // Active chapter detection
  val currentChapterIndex by remember(pageTurnMode, pagerState.currentPage, scrollState.value, scrollState.maxValue) {
    derivedStateOf {
      if (pageTurnMode == "flip" && pages.isNotEmpty()) {
        val currentPage = pages[pagerState.currentPage.coerceIn(0, pages.size - 1)]
        currentPage.chapterIndex
      } else {
        if (scrollState.maxValue > 0 && novel.storyItems.isNotEmpty()) {
          val frac = scrollState.value.toFloat() / scrollState.maxValue.toFloat()
          val targetItemIndex = (frac * (novel.storyItems.size - 1)).toInt()
          val ch = novel.chapters.find { targetItemIndex in it.startParagraphIndex..it.endParagraphIndex }
          ch?.index ?: 0
        } else {
          0
        }
      }
    }
  }

  val currentChapter = novel.chapters.getOrNull(currentChapterIndex) ?: novel.chapters.firstOrNull()

  // Track page progress
  val estimatedCurrentPage by remember(pageTurnMode, pagerState.currentPage, scrollState.value, scrollState.maxValue) {
    derivedStateOf {
      if (pageTurnMode == "flip" && pages.isNotEmpty()) {
        val frac = pagerState.currentPage.toFloat() / (pages.size - 1).coerceAtLeast(1).toFloat()
        val p = (frac * (novel.totalPages - 1)).toInt() + 1
        p.coerceIn(1, novel.totalPages)
      } else {
        if (scrollState.maxValue > 0) {
          val frac = scrollState.value.toFloat() / scrollState.maxValue.toFloat()
          val calculated = (frac * (novel.totalPages - 1)).toInt() + 1
          calculated.coerceIn(1, novel.totalPages)
        } else {
          novel.currentPage.coerceAtLeast(1)
        }
      }
    }
  }

  // Auto-save reading progress
  LaunchedEffect(estimatedCurrentPage) {
    if (estimatedCurrentPage != novel.currentPage) {
      onSaveProgress(estimatedCurrentPage)
    }
  }

  // Restore scroll position in Scroll mode
  LaunchedEffect(novel.id, pageTurnMode) {
    if (pageTurnMode == "scroll" && novel.currentPage > 1 && scrollState.maxValue > 0) {
      val targetScroll = ((novel.currentPage - 1).toFloat() / (novel.totalPages - 1).toFloat() * scrollState.maxValue).toInt()
      scrollState.scrollTo(targetScroll)
    }
  }

  // In-Book Search Match computation
  val searchMatches: List<SearchMatch> = remember(novel.storyItems, novel.chapters, searchQuery) {
    if (searchQuery.isBlank() || searchQuery.trim().length < 2) {
      emptyList()
    } else {
      val query = searchQuery.trim()
      val results = mutableListOf<SearchMatch>()
      var matchCounter = 0

      novel.storyItems.forEachIndexed { pIndex, item ->
        if (item is StoryContentItem.Text) {
          val text = item.paragraph
          var startIndex = 0
          while (startIndex < text.length) {
            val found = text.indexOf(query, startIndex, ignoreCase = true)
            if (found == -1) break

            val snipStart = (found - 24).coerceAtLeast(0)
            val snipEnd = (found + query.length + 24).coerceAtMost(text.length)
            val snippet = (if (snipStart > 0) "…" else "") +
              text.substring(snipStart, snipEnd).trim() +
              (if (snipEnd < text.length) "…" else "")

            val ch = novel.chapters.find { pIndex in it.startParagraphIndex..it.endParagraphIndex }
              ?: novel.chapters.firstOrNull()

            results.add(
              SearchMatch(
                globalIndex = matchCounter++,
                paragraphIndex = pIndex,
                chapterIndex = ch?.index ?: 0,
                chapterTitle = ch?.title ?: novel.chapterTitle,
                startCharIndex = found,
                endCharIndex = found + query.length,
                surroundingSnippet = snippet
              )
            )
            startIndex = found + query.length
          }
        }
      }
      results
    }
  }

  // Auto-reset search match index if matches change
  LaunchedEffect(searchMatches.size) {
    if (currentSearchMatchIndex >= searchMatches.size) {
      currentSearchMatchIndex = 0
    }
  }

  // Function to navigate to a search match
  fun jumpToSearchMatch(match: SearchMatch) {
    if (pageTurnMode == "flip") {
      val targetPage = pages.indexOfFirst { page ->
        page.items.any { item ->
          novel.storyItems.indexOf(item) == match.paragraphIndex
        }
      }
      if (targetPage != -1) {
        coroutineScope.launch {
          pagerState.animateScrollToPage(targetPage)
        }
      }
    } else {
      if (scrollState.maxValue > 0 && novel.storyItems.isNotEmpty()) {
        val targetScroll = ((match.paragraphIndex.toFloat() / novel.storyItems.size.toFloat()) * scrollState.maxValue).toInt()
        coroutineScope.launch {
          scrollState.animateScrollTo(targetScroll)
        }
      }
    }
  }

  // Function to jump to a chapter
  fun jumpToChapter(chapterIndex: Int, startParagraphIndex: Int) {
    if (pageTurnMode == "flip") {
      val targetPage = pages.indexOfFirst { it.chapterIndex == chapterIndex }
      if (targetPage != -1) {
        coroutineScope.launch {
          pagerState.animateScrollToPage(targetPage)
        }
      }
    } else {
      if (scrollState.maxValue > 0 && novel.storyItems.isNotEmpty()) {
        val targetScroll = ((startParagraphIndex.toFloat() / novel.storyItems.size.toFloat()) * scrollState.maxValue).toInt()
        coroutineScope.launch {
          scrollState.animateScrollTo(targetScroll)
        }
      }
    }
  }

  // Main Container
  Box(
    modifier = modifier
      .fillMaxSize()
      .background(SoftCreamPaper)
      .testTag("reading_screen_container")
  ) {
    // 1. Soft subtle paper illumination
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          brush = Brush.verticalGradient(
            0.0f to Color(0x05FFFFFF),
            0.5f to Color(0x00FFFFFF),
            1.0f to Color(0x08000000)
          )
        )
    )

    // 2. Ghosted Security Background
    GhostedSecurityBackground()

    // 3. Reader Viewports (Flip vs Scroll)
    Column(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
    ) {
      // -------------------------------------------------------------
      // TOP HUD BAR: Back, Mode Switcher, Search, Chapters, Typography
      // -------------------------------------------------------------
      AnimatedVisibility(
        visible = isHudVisible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xE6F7F4EC))
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            // Left: Back button & Page Mode Toggle (Flip vs Scroll)
            Row(verticalAlignment = Alignment.CenterVertically) {
              IconButton(
                onClick = onBack,
                modifier = Modifier
                  .size(40.dp)
                  .clip(CircleShape)
                  .testTag("reading_back_arrow")
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                  contentDescription = "Return to Bookshelf",
                  tint = CharcoalSecondary,
                  modifier = Modifier.size(20.dp)
                )
              }

              Spacer(modifier = Modifier.width(4.dp))

              // Page Turning Mode Toggle Pill (Flip vs Scroll)
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0x14D4AF37),
                border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.5f)),
                modifier = Modifier
                  .clickable {
                    val nextMode = if (pageTurnMode == "flip") "scroll" else "flip"
                    pageTurnMode = nextMode
                    typographyPrefs.edit().putString("reading_turn_mode", nextMode).apply()
                  }
                  .testTag("page_turn_mode_toggle")
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                  Icon(
                    imageVector = if (pageTurnMode == "flip") Icons.Outlined.AutoStories else Icons.Outlined.SwapVert,
                    contentDescription = "Toggle turn page style",
                    tint = AntiqueGold,
                    modifier = Modifier.size(14.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = if (pageTurnMode == "flip") "Flip Pages" else "Scroll",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontSize = 10.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = AntiqueGold
                    )
                  )
                }
              }
            }

            // Right: In-Book Search, Chapters, Typography, Bookmarks, Favorite
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp),
              modifier = Modifier.padding(end = 4.dp)
            ) {
              // 1. In-Book Search Toggle Button
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSearchOpen) AntiqueGold else Color(0x0E000000),
                border = BorderStroke(1.dp, if (isSearchOpen) AntiqueGold else Color.Transparent),
                modifier = Modifier
                  .clickable {
                    isSearchOpen = !isSearchOpen
                    if (!isSearchOpen) {
                      searchQuery = ""
                      isSearchResultsListOpen = false
                    }
                  }
                  .testTag("search_in_book_button")
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Search keywords in book",
                    tint = if (isSearchOpen) SoftCreamPaper else CharcoalSecondary,
                    modifier = Modifier.size(15.dp)
                  )
                  if (searchMatches.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                      text = "${searchMatches.size}",
                      style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSearchOpen) SoftCreamPaper else AntiqueGold
                      )
                    )
                  }
                }
              }

              // 2. Chapters Selection Button
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0x0E000000),
                border = BorderStroke(1.dp, SubtleBorder),
                modifier = Modifier
                  .clickable { isChapterModalOpen = true }
                  .testTag("chapters_button")
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
                ) {
                  Icon(
                    imageVector = Icons.Outlined.FormatListNumbered,
                    contentDescription = "Chapters Table of Contents",
                    tint = CharcoalSecondary,
                    modifier = Modifier.size(14.dp)
                  )
                  Spacer(modifier = Modifier.width(3.dp))
                  Text(
                    text = "Ch. ${toRomanNumeral(currentChapter?.number ?: (currentChapterIndex + 1))}",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontSize = 10.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = CharcoalSecondary
                    )
                  )
                }
              }

              // 3. Typography & Paragraph Lining Button
              val isTypographyCustomized = selectedFontType == "custom" || isBold || isItalic
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isTypographyCustomized) Color(0x18D4AF37) else Color(0x0E000000),
                border = BorderStroke(1.dp, if (isTypographyCustomized) AntiqueGold.copy(alpha = 0.6f) else Color.Transparent),
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
                    contentDescription = "Reader typography, bold, italic, and lining",
                    tint = if (isTypographyCustomized) AntiqueGold else CharcoalSecondary,
                    modifier = Modifier.size(14.dp)
                  )
                  Spacer(modifier = Modifier.width(3.dp))
                  Text(
                    text = "Aa",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontSize = 10.sp,
                      fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold,
                      fontStyle = readerFontStyle,
                      color = if (isTypographyCustomized) AntiqueGold else CharcoalSecondary
                    )
                  )
                  if (isBold || isItalic) {
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                      text = if (isBold && isItalic) "BI" else if (isBold) "B" else "I",
                      style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = AntiqueGold
                      )
                    )
                  }
                }
              }

              // 4. Bookmarks Button
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
                    modifier = Modifier.size(14.dp)
                  )
                }
              }

              // 5. Favorite Heart Button
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (novel.isFavorite) Color(0x1AC74350) else Color(0x0E000000),
                modifier = Modifier
                  .clickable { onToggleFavorite() }
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
                }
              }
            }
          }

          // -------------------------------------------------------------
          // IN-BOOK SEARCH BAR (EXPANDABLE)
          // -------------------------------------------------------------
          AnimatedVisibility(
            visible = isSearchOpen,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
              Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFFAF6EE),
                border = BorderStroke(1.2.dp, AntiqueGold.copy(alpha = 0.7f)),
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = AntiqueGold,
                    modifier = Modifier.size(18.dp)
                  )

                  Spacer(modifier = Modifier.width(8.dp))

                  // Search text input
                  Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                      Text(
                        text = "Search keywords in book…",
                        style = MaterialTheme.typography.bodyMedium.copy(
                          fontSize = 13.sp,
                          color = CharcoalTertiary
                        )
                      )
                    }
                    BasicTextField(
                      value = searchQuery,
                      onValueChange = { query ->
                        searchQuery = query
                        currentSearchMatchIndex = 0
                      },
                      singleLine = true,
                      textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        color = CharcoalText
                      ),
                      cursorBrush = SolidColor(AntiqueGold),
                      modifier = Modifier
                        .fillMaxWidth()
                        .testTag("in_book_search_input")
                    )
                  }

                  // Clear query ('X')
                  if (searchQuery.isNotEmpty()) {
                    IconButton(
                      onClick = {
                        searchQuery = ""
                        isSearchResultsListOpen = false
                      },
                      modifier = Modifier.size(24.dp)
                    ) {
                      Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Clear search",
                        tint = CharcoalSecondary,
                        modifier = Modifier.size(14.dp)
                      )
                    }
                  }

                  // Match Counter and Navigation Steppers
                  if (searchMatches.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                      text = "${currentSearchMatchIndex + 1}/${searchMatches.size}",
                      style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = AntiqueGold
                      )
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    // Previous Match (↑)
                    IconButton(
                      onClick = {
                        val prev = if (currentSearchMatchIndex <= 0) searchMatches.size - 1 else currentSearchMatchIndex - 1
                        currentSearchMatchIndex = prev
                        jumpToSearchMatch(searchMatches[prev])
                      },
                      modifier = Modifier.size(26.dp)
                    ) {
                      Icon(
                        imageVector = Icons.Outlined.KeyboardArrowUp,
                        contentDescription = "Previous match",
                        tint = CharcoalText,
                        modifier = Modifier.size(16.dp)
                      )
                    }

                    // Next Match (↓)
                    IconButton(
                      onClick = {
                        val next = (currentSearchMatchIndex + 1) % searchMatches.size
                        currentSearchMatchIndex = next
                        jumpToSearchMatch(searchMatches[next])
                      },
                      modifier = Modifier.size(26.dp)
                    ) {
                      Icon(
                        imageVector = Icons.Outlined.KeyboardArrowDown,
                        contentDescription = "Next match",
                        tint = CharcoalText,
                        modifier = Modifier.size(16.dp)
                      )
                    }

                    // All matches dropdown list toggle
                    IconButton(
                      onClick = { isSearchResultsListOpen = !isSearchResultsListOpen },
                      modifier = Modifier.size(26.dp)
                    ) {
                      Icon(
                        imageVector = Icons.Outlined.List,
                        contentDescription = "List all search results",
                        tint = if (isSearchResultsListOpen) AntiqueGold else CharcoalSecondary,
                        modifier = Modifier.size(16.dp)
                      )
                    }
                  } else if (searchQuery.isNotBlank() && searchQuery.length >= 2) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = "0 matches",
                      style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        color = CharcoalTertiary
                      )
                    )
                  }
                }
              }

              // All search results snippets dropdown drawer
              AnimatedVisibility(
                visible = isSearchResultsListOpen && searchMatches.isNotEmpty(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
              ) {
                Surface(
                  shape = RoundedCornerShape(14.dp),
                  color = Color(0xFFF9F5EC),
                  border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.5f)),
                  shadowElevation = 6.dp,
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .heightIn(max = 200.dp)
                ) {
                  LazyColumn(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    items(searchMatches) { match ->
                      val isSelected = match.globalIndex == currentSearchMatchIndex
                      Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) Color(0x22D4AF37) else Color(0x08000000),
                        border = BorderStroke(
                          1.dp,
                          if (isSelected) AntiqueGold else Color.Transparent
                        ),
                        modifier = Modifier
                          .fillMaxWidth()
                          .clickable {
                            currentSearchMatchIndex = match.globalIndex
                            jumpToSearchMatch(match)
                          }
                      ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                          Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                          ) {
                            Text(
                              text = match.chapterTitle,
                              style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = AntiqueGold
                              )
                            )
                            Text(
                              text = "#${match.globalIndex + 1}",
                              style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.5.sp,
                                color = CharcoalTertiary
                              )
                            )
                          }
                          Spacer(modifier = Modifier.height(2.dp))
                          Text(
                            text = match.surroundingSnippet,
                            style = MaterialTheme.typography.bodySmall.copy(
                              fontSize = 10.5.sp,
                              lineHeight = 14.sp,
                              color = CharcoalText
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
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
      }

      // -------------------------------------------------------------
      // READING VIEWPORT: FLIP MODE vs CONTINUOUS SCROLL MODE
      // -------------------------------------------------------------
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
      ) {
        val baseFontSize = 17.5f * fontSizeScale
        val baseLineHeight = 30f * fontSizeScale * lineHeightScale
        val baseParagraphSpacing = (20f * paragraphSpacingScale).dp

        if (pageTurnMode == "flip") {
          // =========================================================
          // 1. PAGE FLIP / TURN MODE (3D Book Page Curl & Horizontal Pager)
          // =========================================================
          Box(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
              state = pagerState,
              modifier = Modifier
                .fillMaxSize()
                .testTag("reading_horizontal_pager")
            ) { pageIdx ->
              val page = pages[pageIdx]
              val pageOffset = ((pagerState.currentPage - pageIdx) + pagerState.currentPageOffsetFraction)

              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .graphicsLayer {
                    // Realistic paper page turning physics
                    if (pageOffset <= 0) {
                      transformOrigin = TransformOrigin(0f, 0.5f)
                      rotationY = (-pageOffset * 32f).coerceIn(-90f, 0f)
                      cameraDistance = 16f * density
                      shadowElevation = (-pageOffset * 10f).coerceIn(0f, 16f)
                    } else {
                      alpha = 1f - (pageOffset * 0.25f).coerceIn(0f, 0.45f)
                    }
                  }
                  .background(SoftCreamPaper)
              ) {
                // Left spine crease gradient shadow
                Box(
                  modifier = Modifier
                    .fillMaxHeight()
                    .width(18.dp)
                    .align(Alignment.CenterStart)
                    .background(
                      Brush.horizontalGradient(
                        0f to Color(0x18000000),
                        0.4f to Color(0x0A000000),
                        1f to Color.Transparent
                      )
                    )
                )

                // The Book Page Content
                Column(
                  modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 26.dp, vertical = 6.dp),
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  // Running Page Header
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = novel.title.uppercase(),
                      style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.5.sp,
                        letterSpacing = 1.6.sp,
                        fontWeight = FontWeight.Medium
                      ),
                      color = CharcoalTertiary,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                      modifier = Modifier.weight(1f, fill = false)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                      text = page.chapterTitle.uppercase(),
                      style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.5.sp,
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                      ),
                      color = AntiqueGold,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }

                  Spacer(modifier = Modifier.height(6.dp))

                  // Page Content Body
                  Column(
                    modifier = Modifier
                      .weight(1f)
                      .fillMaxWidth()
                      .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(baseParagraphSpacing)
                  ) {
                    page.items.forEach { item ->
                      when (item) {
                        is StoryContentItem.ChapterBreak -> {
                          // Chapter ornament header
                          Column(
                            modifier = Modifier
                              .fillMaxWidth()
                              .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                          ) {
                            Text(
                              text = "— CHAPTER ${toRomanNumeral(item.chapterNumber)} —",
                              style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                letterSpacing = 2.4.sp,
                                fontWeight = FontWeight.Bold
                              ),
                              color = AntiqueGold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                              text = item.title,
                              style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = readerFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 21.sp,
                                lineHeight = 26.sp
                              ),
                              color = CharcoalText,
                              textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                              text = "❦",
                              style = MaterialTheme.typography.bodySmall,
                              color = AntiqueGold.copy(alpha = 0.6f)
                            )
                          }
                        }

                        is StoryContentItem.Photo -> {
                          // Framed Photo Plate
                          Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF7F2)),
                            border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.4f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                            modifier = Modifier
                              .fillMaxWidth()
                              .clickable { viewingPhoto = item }
                              .testTag("story_photo_plate_flip")
                          ) {
                            Column(
                              modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                              horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                              Box(
                                modifier = Modifier
                                  .fillMaxWidth()
                                  .height(220.dp)
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
                                  shape = RoundedCornerShape(10.dp),
                                  color = Color(0x99000000),
                                  modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                                ) {
                                  Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                  ) {
                                    Icon(
                                      imageVector = Icons.Outlined.ZoomIn,
                                      contentDescription = null,
                                      tint = SoftCreamPaper,
                                      modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                      text = "Tap to enlarge",
                                      style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 8.5.sp,
                                        color = SoftCreamPaper
                                      )
                                    )
                                  }
                                }
                              }

                              if (item.caption.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                  text = item.caption,
                                  style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = readerFontFamily,
                                    fontStyle = FontStyle.Italic,
                                    fontSize = 11.5.sp
                                  ),
                                  color = CharcoalSecondary,
                                  textAlign = TextAlign.Center
                                )
                              }
                            }
                          }
                        }

                        is StoryContentItem.Text -> {
                          val paragraph = item.paragraph
                          val pIndex = novel.storyItems.indexOf(item)
                          val isBookmarked = bookmarksList.any {
                            it.paragraphIndex == pIndex || it.quoteText == paragraph.trim()
                          }

                          Box(
                            modifier = Modifier
                              .fillMaxWidth()
                              .clip(RoundedCornerShape(10.dp))
                              .background(if (isBookmarked) Color(0xFFF9F4E8) else Color.Transparent)
                              .border(
                                width = if (isBookmarked) 1.2.dp else 0.dp,
                                color = if (isBookmarked) AntiqueGold.copy(alpha = 0.65f) else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
                              )
                              .padding(if (isBookmarked) 8.dp else 0.dp)
                          ) {
                            if (page.isChapterFirstPage && page.items.firstOrNull() == item && paragraph.isNotEmpty()) {
                              // Drop cap on first paragraph of chapter
                              val firstChar = paragraph.take(1)
                              val rest = paragraph.drop(1)
                              Row(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                  text = firstChar,
                                  style = MaterialTheme.typography.displayLarge.copy(
                                    fontFamily = readerFontFamily,
                                    fontWeight = if (isBold) FontWeight.Bold else FontWeight.Light,
                                    fontStyle = readerFontStyle,
                                    fontSize = (52f * fontSizeScale).sp,
                                    lineHeight = (50f * fontSizeScale).sp
                                  ),
                                  color = AntiqueGold,
                                  modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                  text = buildSearchHighlightString(
                                    text = rest,
                                    query = searchQuery,
                                    isParagraphActive = searchMatches.getOrNull(currentSearchMatchIndex)?.paragraphIndex == pIndex
                                  ),
                                  style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = readerFontFamily,
                                    fontWeight = readerFontWeight,
                                    fontStyle = readerFontStyle,
                                    color = CharcoalText,
                                    lineHeight = baseLineHeight.sp,
                                    fontSize = baseFontSize.sp,
                                    textAlign = if (isJustified) TextAlign.Justify else TextAlign.Start
                                  )
                                )
                              }
                            } else {
                              Text(
                                text = buildSearchHighlightString(
                                  text = paragraph,
                                  query = searchQuery,
                                  isParagraphActive = searchMatches.getOrNull(currentSearchMatchIndex)?.paragraphIndex == pIndex
                                ),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                  fontFamily = readerFontFamily,
                                  fontWeight = readerFontWeight,
                                  fontStyle = readerFontStyle,
                                  color = CharcoalText,
                                  lineHeight = baseLineHeight.sp,
                                  fontSize = baseFontSize.sp,
                                  textAlign = if (isJustified) TextAlign.Justify else TextAlign.Start,
                                  textIndent = TextIndent(firstLine = if (isFirstLineIndent) 22.sp else 0.sp)
                                ),
                                modifier = Modifier.fillMaxWidth()
                              )
                            }
                          }
                        }
                      }
                    }
                  }

                  Spacer(modifier = Modifier.height(6.dp))

                  // Running Page Footer
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = "CHAPTER ${toRomanNumeral(page.chapterNumber)}",
                      style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.5.sp,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                      ),
                      color = AntiqueGold
                    )

                    Text(
                      text = "PAGE ${page.displayPageNumber} OF ${pages.size}",
                      style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.5.sp,
                        letterSpacing = 1.4.sp
                      ),
                      color = CharcoalTertiary
                    )
                  }
                }
              }
            }

            // Interactive Tap Zones: Left (Prev Page), Right (Next Page), Center (Toggle HUD)
            Row(modifier = Modifier.fillMaxSize()) {
              // Left tap zone: previous page
              Box(
                modifier = Modifier
                  .weight(0.22f)
                  .fillMaxHeight()
                  .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                  ) {
                    if (pagerState.currentPage > 0) {
                      coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                      }
                    }
                  }
              )

              // Center tap zone: toggle HUD
              Box(
                modifier = Modifier
                  .weight(0.56f)
                  .fillMaxHeight()
                  .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                  ) {
                    isHudVisible = !isHudVisible
                  }
              )

              // Right tap zone: next page
              Box(
                modifier = Modifier
                  .weight(0.22f)
                  .fillMaxHeight()
                  .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                  ) {
                    if (pagerState.currentPage < pages.size - 1) {
                      coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                      }
                    }
                  }
              )
            }

            // Floating subtle Chevrons for tactile turning
            if (isHudVisible) {
              if (pagerState.currentPage > 0) {
                Surface(
                  shape = CircleShape,
                  color = Color(0x66000000),
                  modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 6.dp)
                    .clickable {
                      coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                      }
                    }
                ) {
                  Icon(
                    imageVector = Icons.Outlined.ChevronLeft,
                    contentDescription = "Previous Page",
                    tint = SoftCreamPaper,
                    modifier = Modifier.size(24.dp)
                  )
                }
              }

              if (pagerState.currentPage < pages.size - 1) {
                Surface(
                  shape = CircleShape,
                  color = Color(0x66000000),
                  modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 6.dp)
                    .clickable {
                      coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                      }
                    }
                ) {
                  Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = "Next Page",
                    tint = SoftCreamPaper,
                    modifier = Modifier.size(24.dp)
                  )
                }
              }
            }
          }
        } else {
          // =========================================================
          // 2. CONTINUOUS SCROLL MODE (Fluid Vertical Flow)
          // =========================================================
          Column(
            modifier = Modifier
              .fillMaxSize()
              .verticalScroll(scrollState)
              .padding(horizontal = 28.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Chapter Header
            Text(
              text = (currentChapter?.title ?: novel.chapterTitle).uppercase(),
              style = MaterialTheme.typography.labelMedium.copy(
                letterSpacing = 2.4.sp,
                fontWeight = FontWeight.Bold
              ),
              color = AntiqueGold,
              textAlign = TextAlign.Center,
              modifier = Modifier.testTag("reading_chapter_title")
            )

            Spacer(modifier = Modifier.height(12.dp))

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

            // Author Header
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
              "BY ${novel.author.uppercase()} • STRAWBERRYCANDY ARCHIVE"
            }

            Text(
              text = authorHeader,
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.Bold
              ),
              color = CharcoalTertiary,
              textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Text Body & Plates
            Column(
              modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth(),
              verticalArrangement = Arrangement.spacedBy(baseParagraphSpacing)
            ) {
              novel.storyItems.forEachIndexed { index, item ->
                when (item) {
                  is StoryContentItem.ChapterBreak -> {
                    // Chapter Break Heading
                    Column(
                      modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 28.dp, bottom = 12.dp),
                      horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                      Text(
                        text = "— CHAPTER ${toRomanNumeral(item.chapterNumber)} —",
                        style = MaterialTheme.typography.labelSmall.copy(
                          fontSize = 10.sp,
                          letterSpacing = 2.4.sp,
                          fontWeight = FontWeight.Bold
                        ),
                        color = AntiqueGold
                      )
                      Spacer(modifier = Modifier.height(6.dp))
                      Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                          fontFamily = readerFontFamily,
                          fontWeight = FontWeight.Normal,
                          fontSize = 22.sp,
                          lineHeight = 28.sp
                        ),
                        color = CharcoalText,
                        textAlign = TextAlign.Center
                      )
                      Spacer(modifier = Modifier.height(8.dp))
                      Text(
                        text = "❦",
                        style = MaterialTheme.typography.bodySmall,
                        color = AntiqueGold.copy(alpha = 0.6f)
                      )
                    }
                  }

                  is StoryContentItem.Photo -> {
                    Card(
                      shape = RoundedCornerShape(16.dp),
                      colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF7F2)),
                      border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.35f)),
                      elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                      modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { viewingPhoto = item }
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
                            val firstChar = paragraph.take(1)
                            val restOfFirst = paragraph.drop(1)
                            Row(modifier = Modifier.fillMaxWidth()) {
                              Text(
                                text = firstChar,
                                style = MaterialTheme.typography.displayLarge.copy(
                                  fontFamily = readerFontFamily,
                                  fontWeight = if (isBold) FontWeight.Bold else FontWeight.Light,
                                  fontStyle = readerFontStyle,
                                  fontSize = (62f * fontSizeScale).sp,
                                  lineHeight = (60f * fontSizeScale).sp
                                ),
                                color = AntiqueGold,
                                modifier = Modifier.padding(end = 10.dp, bottom = 2.dp)
                              )
                              Text(
                                text = buildSearchHighlightString(
                                  text = restOfFirst,
                                  query = searchQuery,
                                  isParagraphActive = searchMatches.getOrNull(currentSearchMatchIndex)?.paragraphIndex == index
                                ),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                  fontFamily = readerFontFamily,
                                  fontWeight = readerFontWeight,
                                  fontStyle = readerFontStyle,
                                  color = CharcoalText,
                                  lineHeight = baseLineHeight.sp,
                                  fontSize = baseFontSize.sp,
                                  textAlign = if (isJustified) TextAlign.Justify else TextAlign.Start
                                )
                              )
                            }
                          } else {
                            Text(
                              text = buildSearchHighlightString(
                                text = paragraph,
                                query = searchQuery,
                                isParagraphActive = searchMatches.getOrNull(currentSearchMatchIndex)?.paragraphIndex == index
                              ),
                              style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = readerFontFamily,
                                fontWeight = readerFontWeight,
                                fontStyle = readerFontStyle,
                                color = CharcoalText,
                                lineHeight = baseLineHeight.sp,
                                fontSize = baseFontSize.sp,
                                textAlign = if (isJustified) TextAlign.Justify else TextAlign.Start,
                                textIndent = TextIndent(firstLine = if (isFirstLineIndent) 22.sp else 0.sp)
                              ),
                              modifier = Modifier.fillMaxWidth()
                            )
                          }
                        }
                      }

                      // Interactive Action Bar for bookmarking
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

            // Colophon
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

            // Comments Section
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
        }

        // Watermark Overlay
        SubtleWatermarkOverlay(
          modifier = Modifier.matchParentSize(),
          text = "LICENSED TO READER • STRAWBERRYCANDY SECURE ARCHIVE • PROPRIETARY COPY",
          textColor = CharcoalText.copy(alpha = 0.038f)
        )
      }

      // -------------------------------------------------------------
      // BOTTOM HUD BAR: CHAPTER QUICK PILL & PAGE SCRUBBER SLIDER
      // -------------------------------------------------------------
      AnimatedVisibility(
        visible = isHudVisible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
      ) {
        Surface(
          color = Color(0xF2FAF6EE),
          border = BorderStroke(1.dp, SubtleBorder),
          shadowElevation = 8.dp,
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 8.dp)
          ) {
            // Row 1: Chapter selection pill with Prev/Next chapter arrows
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              // Prev Chapter
              IconButton(
                onClick = {
                  if (currentChapterIndex > 0) {
                    val prevIdx = currentChapterIndex - 1
                    val prevCh = novel.chapters.getOrNull(prevIdx)
                    if (prevCh != null) {
                      jumpToChapter(prevIdx, prevCh.startParagraphIndex)
                    }
                  }
                },
                enabled = currentChapterIndex > 0,
                modifier = Modifier.size(30.dp)
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                  contentDescription = "Previous Chapter",
                  tint = if (currentChapterIndex > 0) AntiqueGold else CharcoalTertiary.copy(alpha = 0.4f),
                  modifier = Modifier.size(16.dp)
                )
              }

              // Center Chapter Label Pill (Tap to open Chapter Selection Modal)
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0x16D4AF37),
                border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.6f)),
                modifier = Modifier
                  .clickable { isChapterModalOpen = true }
                  .testTag("current_chapter_pill")
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                  Icon(
                    imageVector = Icons.Outlined.MenuBook,
                    contentDescription = null,
                    tint = AntiqueGold,
                    modifier = Modifier.size(13.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = currentChapter?.title ?: novel.chapterTitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontSize = 11.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = CharcoalText
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "▼",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontSize = 8.sp,
                      color = AntiqueGold
                    )
                  )
                }
              }

              // Next Chapter
              IconButton(
                onClick = {
                  if (currentChapterIndex < novel.chapters.size - 1) {
                    val nextIdx = currentChapterIndex + 1
                    val nextCh = novel.chapters.getOrNull(nextIdx)
                    if (nextCh != null) {
                      jumpToChapter(nextIdx, nextCh.startParagraphIndex)
                    }
                  }
                },
                enabled = currentChapterIndex < novel.chapters.size - 1,
                modifier = Modifier.size(30.dp)
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                  contentDescription = "Next Chapter",
                  tint = if (currentChapterIndex < novel.chapters.size - 1) AntiqueGold else CharcoalTertiary.copy(alpha = 0.4f),
                  modifier = Modifier.size(16.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Row 2: Page Scrubber Slider & Folio Info
            if (pageTurnMode == "flip" && pages.size > 1) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "${pagerState.currentPage + 1}",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = AntiqueGold
                  )
                )

                Slider(
                  value = pagerState.currentPage.toFloat(),
                  onValueChange = { pageVal ->
                    coroutineScope.launch {
                      pagerState.scrollToPage(pageVal.toInt())
                    }
                  },
                  valueRange = 0f..(pages.size - 1).toFloat(),
                  steps = (pages.size - 2).coerceAtLeast(0),
                  colors = SliderDefaults.colors(
                    thumbColor = AntiqueGold,
                    activeTrackColor = AntiqueGold,
                    inactiveTrackColor = Color(0x22000000)
                  ),
                  modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .height(26.dp)
                )

                Text(
                  text = "${pages.size}",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    color = CharcoalTertiary
                  )
                )
              }
            } else {
              Text(
                text = "PAGE $estimatedCurrentPage OF ${novel.totalPages}  •  ${novel.editionNumber}",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 9.sp,
                  letterSpacing = 1.5.sp
                ),
                color = CharcoalTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 4.dp)
              )
            }
          }
        }
      }
    }

    // -------------------------------------------------------------
    // MODALS & DIALOGS
    // -------------------------------------------------------------

    // 1. Chapter Selection Modal (Table of Contents & Pointer)
    if (isChapterModalOpen) {
      ChapterSelectionModal(
        bookTitle = novel.title,
        chapters = novel.chapters,
        currentChapterIndex = currentChapterIndex,
        onSelectChapter = { chIndex, startParagraphIndex ->
          jumpToChapter(chIndex, startParagraphIndex)
        },
        onDismiss = { isChapterModalOpen = false }
      )
    }

    // 2. Lightbox Viewer for story photos
    if (viewingPhoto != null) {
      StoryPhotoViewerModal(
        photoUri = viewingPhoto!!.imageUri,
        caption = viewingPhoto!!.caption,
        novelTitle = novel.title,
        onDismiss = { viewingPhoto = null }
      )
    }

    // 3. Typography & Reading Experience Customizer Modal
    if (isTypographyModalOpen) {
      TypographyCustomizerModal(
        currentFontType = selectedFontType,
        customFontName = customFontName,
        fontSizeScale = fontSizeScale,
        lineHeightScale = lineHeightScale,
        pageTurnMode = pageTurnMode,
        paragraphSpacingScale = paragraphSpacingScale,
        isJustified = isJustified,
        isFirstLineIndent = isFirstLineIndent,
        isBold = isBold,
        isItalic = isItalic,
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
        onSelectPageTurnMode = { mode ->
          pageTurnMode = mode
          typographyPrefs.edit().putString("reading_turn_mode", mode).apply()
        },
        onUpdateParagraphSpacingScale = { scale ->
          paragraphSpacingScale = scale
          typographyPrefs.edit().putFloat("paragraph_spacing_scale", scale).apply()
        },
        onToggleJustified = { justified ->
          isJustified = justified
          typographyPrefs.edit().putBoolean("is_justified", justified).apply()
        },
        onToggleFirstLineIndent = { indent ->
          isFirstLineIndent = indent
          typographyPrefs.edit().putBoolean("is_first_line_indent", indent).apply()
        },
        onToggleBold = { bold ->
          isBold = bold
          typographyPrefs.edit().putBoolean("is_bold", bold).apply()
        },
        onToggleItalic = { italic ->
          isItalic = italic
          typographyPrefs.edit().putBoolean("is_italic", italic).apply()
        },
        onSelectFontStyle = { bold, italic ->
          isBold = bold
          isItalic = italic
          typographyPrefs.edit()
            .putBoolean("is_bold", bold)
            .putBoolean("is_italic", italic)
            .apply()
        },
        onResetDefaults = {
          selectedFontType = "serif"
          fontSizeScale = 1.0f
          lineHeightScale = 1.0f
          paragraphSpacingScale = 1.0f
          isJustified = false
          isFirstLineIndent = false
          isBold = false
          isItalic = false
          pageTurnMode = "flip"
          typographyPrefs.edit()
            .putString("font_family_type", "serif")
            .putFloat("font_size_scale", 1.0f)
            .putFloat("line_height_scale", 1.0f)
            .putFloat("paragraph_spacing_scale", 1.0f)
            .putBoolean("is_justified", false)
            .putBoolean("is_first_line_indent", false)
            .putBoolean("is_bold", false)
            .putBoolean("is_italic", false)
            .putString("reading_turn_mode", "flip")
            .apply()
        },
        onDismiss = { isTypographyModalOpen = false }
      )
    }

    // 4. Favorite Lines & Bookmarks Highlights Modal
    if (isBookmarksModalOpen) {
      BookmarksHighlightsModal(
        bookmarks = bookmarksList,
        onJumpToParagraph = { paragraphIndex ->
          activeHighlightedIndex = paragraphIndex
          if (pageTurnMode == "flip") {
            val targetPage = pages.indexOfFirst { page ->
              page.items.any { novel.storyItems.indexOf(it) == paragraphIndex }
            }
            if (targetPage != -1) {
              coroutineScope.launch {
                pagerState.animateScrollToPage(targetPage)
              }
            }
          }
        },
        onDeleteBookmark = { bookmarkId ->
          viewModel?.deleteBookmark(bookmarkId)
        },
        onDismiss = { isBookmarksModalOpen = false }
      )
    }
  }
}

/**
 * Builds an AnnotatedString highlighting any occurrences of searchQuery.
 */
private fun buildSearchHighlightString(
  text: String,
  query: String,
  isParagraphActive: Boolean,
) = buildAnnotatedString {
  if (query.isBlank() || query.trim().length < 2) {
    append(text)
    return@buildAnnotatedString
  }

  val q = query.trim()
  var startIndex = 0

  while (startIndex < text.length) {
    val index = text.indexOf(q, startIndex, ignoreCase = true)
    if (index == -1) {
      append(text.substring(startIndex))
      break
    }

    // Append text leading up to match
    if (index > startIndex) {
      append(text.substring(startIndex, index))
    }

    // Append highlighted match
    val matchText = text.substring(index, index + q.length)
    withStyle(
      SpanStyle(
        background = if (isParagraphActive) AntiqueGold else Color(0x45D4AF37),
        color = if (isParagraphActive) Color.White else CharcoalText,
        fontWeight = FontWeight.Bold
      )
    ) {
      append(matchText)
    }

    startIndex = index + q.length
  }
}

/**
 * Converts a list of StoryContentItems into paginated ReadingPage units
 * formatted for horizontal page turning.
 */
private fun buildReadingPages(novel: NovelWithState): List<ReadingPage> {
  val pages = mutableListOf<ReadingPage>()
  var currentPageItems = mutableListOf<StoryContentItem>()
  var currentChapter = novel.chapters.firstOrNull()
  var currentChapterIndex = 0
  var pageNumber = 1
  var isChapterFirst = true

  novel.storyItems.forEachIndexed { index, item ->
    // Check if this item starts a new chapter
    val matchingChapter = novel.chapters.find { it.startParagraphIndex == index }
    if (matchingChapter != null && matchingChapter != currentChapter) {
      if (currentPageItems.isNotEmpty()) {
        pages.add(
          ReadingPage(
            pageIndex = pages.size,
            displayPageNumber = pageNumber++,
            chapterIndex = currentChapterIndex,
            chapterTitle = currentChapter?.title ?: novel.chapterTitle,
            chapterNumber = currentChapter?.number ?: (currentChapterIndex + 1),
            items = currentPageItems.toList(),
            isChapterFirstPage = isChapterFirst
          )
        )
        currentPageItems.clear()
        isChapterFirst = false
      }
      currentChapter = matchingChapter
      currentChapterIndex = matchingChapter.index
      isChapterFirst = true
    }

    when (item) {
      is StoryContentItem.ChapterBreak -> {
        if (currentPageItems.isNotEmpty()) {
          pages.add(
            ReadingPage(
              pageIndex = pages.size,
              displayPageNumber = pageNumber++,
              chapterIndex = currentChapterIndex,
              chapterTitle = currentChapter?.title ?: novel.chapterTitle,
              chapterNumber = currentChapter?.number ?: (currentChapterIndex + 1),
              items = currentPageItems.toList(),
              isChapterFirstPage = isChapterFirst
            )
          )
          currentPageItems.clear()
        }
        currentPageItems.add(item)
        isChapterFirst = true
      }

      is StoryContentItem.Photo -> {
        if (currentPageItems.isNotEmpty()) {
          pages.add(
            ReadingPage(
              pageIndex = pages.size,
              displayPageNumber = pageNumber++,
              chapterIndex = currentChapterIndex,
              chapterTitle = currentChapter?.title ?: novel.chapterTitle,
              chapterNumber = currentChapter?.number ?: (currentChapterIndex + 1),
              items = currentPageItems.toList(),
              isChapterFirstPage = isChapterFirst
            )
          )
          currentPageItems.clear()
          isChapterFirst = false
        }
        pages.add(
          ReadingPage(
            pageIndex = pages.size,
            displayPageNumber = pageNumber++,
            chapterIndex = currentChapterIndex,
            chapterTitle = currentChapter?.title ?: novel.chapterTitle,
            chapterNumber = currentChapter?.number ?: (currentChapterIndex + 1),
            items = listOf(item),
            isChapterFirstPage = false
          )
        )
      }

      is StoryContentItem.Text -> {
        val textLength = item.paragraph.length
        val hasChapterBreak = currentPageItems.any { it is StoryContentItem.ChapterBreak }
        if ((hasChapterBreak && currentPageItems.size >= 2) ||
            (!hasChapterBreak && currentPageItems.size >= 2) ||
            (currentPageItems.isNotEmpty() && textLength > 320)) {
          pages.add(
            ReadingPage(
              pageIndex = pages.size,
              displayPageNumber = pageNumber++,
              chapterIndex = currentChapterIndex,
              chapterTitle = currentChapter?.title ?: novel.chapterTitle,
              chapterNumber = currentChapter?.number ?: (currentChapterIndex + 1),
              items = currentPageItems.toList(),
              isChapterFirstPage = isChapterFirst
            )
          )
          currentPageItems.clear()
          isChapterFirst = false
        }
        currentPageItems.add(item)
      }
    }
  }

  if (currentPageItems.isNotEmpty()) {
    pages.add(
      ReadingPage(
        pageIndex = pages.size,
        displayPageNumber = pageNumber++,
        chapterIndex = currentChapterIndex,
        chapterTitle = currentChapter?.title ?: novel.chapterTitle,
        chapterNumber = currentChapter?.number ?: (currentChapterIndex + 1),
        items = currentPageItems.toList(),
        isChapterFirstPage = isChapterFirst
      )
    )
  }

  return if (pages.isEmpty()) {
    listOf(
      ReadingPage(
        pageIndex = 0,
        displayPageNumber = 1,
        chapterIndex = 0,
        chapterTitle = novel.chapterTitle,
        chapterNumber = 1,
        items = novel.storyItems,
        isChapterFirstPage = true
      )
    )
  } else pages
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
