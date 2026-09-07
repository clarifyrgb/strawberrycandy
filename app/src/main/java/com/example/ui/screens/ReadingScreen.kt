package com.example.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import kotlinx.coroutines.delay
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AuthorSlotEntity
import com.example.data.local.BookmarkHighlightEntity
import com.example.model.NovelWithState
import com.example.model.SearchMatch
import com.example.model.StoryContentItem
import com.example.ui.components.AddChapterDialog
import com.example.ui.components.BookmarksHighlightsModal
import com.example.ui.components.ChapterCommentsSection
import com.example.ui.components.ChapterSelectionModal
import com.example.ui.components.StoryPhotoItem
import com.example.ui.components.StoryPhotoViewerModal
import com.example.ui.components.TranslatorProfileModal
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

data class HighlightColorOption(
  val name: String,
  val hex: Long,
  val displayColor: Color,
  val emoji: String,
)

val ReaderHighlightColors = listOf(
  HighlightColorOption("Amber Gold", 0xFFD4AF37, Color(0xFFD4AF37), "🍯"),
  HighlightColorOption("Rose Blush", 0xFFFF6B81, Color(0xFFFF6B81), "🌸"),
  HighlightColorOption("Sage Mint", 0xFF58B368, Color(0xFF58B368), "🌿"),
  HighlightColorOption("Ocean Sky", 0xFF3D9BE9, Color(0xFF3D9BE9), "🌊"),
  HighlightColorOption("Lilac Violet", 0xFFA569BD, Color(0xFFA569BD), "💜"),
  HighlightColorOption("Sunset Coral", 0xFFFF8C42, Color(0xFFFF8C42), "🍑")
)

data class ActiveParagraphSelection(
  val paragraphIndex: Int,
  val paragraphText: String,
  val sentences: List<String>,
  val selectedSentenceIndex: Int, // -1: whole paragraph, >=0: individual sentence index
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReadingScreen(
  novel: NovelWithState,
  onBack: () -> Unit,
  onSaveProgress: (page: Int) -> Unit = {},
  onToggleFavorite: () -> Unit = {},
  onSelectNovel: ((NovelWithState) -> Unit)? = null,
  viewModel: StrawberrycandyViewModel? = null,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onBack)

  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val lazyListState = rememberLazyListState()

  // Typography preferences
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

  // Modals & UI states
  var viewingPhoto by remember { mutableStateOf<StoryContentItem.Photo?>(null) }
  var isTypographyModalOpen by remember { mutableStateOf(false) }
  var isBookmarksModalOpen by remember { mutableStateOf(false) }
  var isChapterModalOpen by remember { mutableStateOf(false) }
  var isAddChapterModalOpen by remember { mutableStateOf(false) }
  var isHudVisible by remember { mutableStateOf(true) }

  // Sentence / line highlighting selection state
  var activeParagraphSelection by remember { mutableStateOf<ActiveParagraphSelection?>(null) }

  // In-Book Search state
  var isSearchOpen by remember { mutableStateOf(false) }
  var searchQuery by remember { mutableStateOf("") }
  var currentSearchMatchIndex by remember { mutableIntStateOf(0) }
  var isSearchResultsListOpen by remember { mutableStateOf(false) }

  // Reactive comments across all chapters & online sync
  val commentsFlow = remember(novel.id, viewModel) {
    viewModel?.getAllCommentsForNovel(novel.id)
  }
  val allNovelComments by (commentsFlow?.collectAsState(initial = emptyList())
    ?: remember { mutableStateOf(emptyList()) })

  // Auto-sync comments from online cloud archive in real-time
  LaunchedEffect(novel.id) {
    viewModel?.listenToCloudComments(novel.id)
    viewModel?.syncRemoteComments(novel.id)
    while (true) {
      delay(12000L)
      viewModel?.syncRemoteComments(novel.id)
    }
  }

  val bookmarksFlow = remember(novel.id, viewModel) {
    viewModel?.getBookmarksForNovel(novel.id)
  }
  val bookmarksList by (bookmarksFlow?.collectAsState(initial = emptyList())
    ?: remember { mutableStateOf(emptyList()) })

  val activeUser by (viewModel?.activeUser?.collectAsState()
    ?: remember { mutableStateOf(null) })
  val canAddChapter = activeUser?.role == "OWNER" || activeUser?.authorSlot == 0 || (activeUser?.role == "TRANSLATOR" && activeUser?.authorSlot == novel.authorSlot)

  val uiState by (viewModel?.uiState?.collectAsState()
    ?: remember { mutableStateOf(null) })
  val authorSlots = uiState?.authorSlots ?: emptyList()
  val allNovelsList = uiState?.novels ?: listOf(novel)
  var viewingTranslatorSlot by remember { mutableStateOf<AuthorSlotEntity?>(null) }

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

  // Initial sync to saved reading progress
  var hasInitialSynced by remember(novel.id) { mutableStateOf(false) }
  LaunchedEffect(novel.id) {
    if (!hasInitialSynced && novel.storyItems.isNotEmpty()) {
      if (novel.currentPage > 1 && novel.totalPages > 0) {
        val frac = (novel.currentPage - 1).toFloat() / novel.totalPages.toFloat()
        val targetItem = (frac * novel.storyItems.size).toInt().coerceIn(0, novel.storyItems.size)
        lazyListState.scrollToItem((targetItem + 1).coerceIn(0, novel.storyItems.size), 0)
      }
      hasInitialSynced = true
    }
  }

  // Active chapter detection based on current scroll position
  val currentChapterIndex by remember(lazyListState.firstVisibleItemIndex, novel.chapters) {
    derivedStateOf {
      val currentItemIdx = (lazyListState.firstVisibleItemIndex - 1).coerceAtLeast(0)
      val ch = novel.chapters.find { currentItemIdx in it.startParagraphIndex..it.endParagraphIndex }
      ch?.index ?: novel.chapters.lastOrNull { it.startParagraphIndex <= currentItemIdx }?.index ?: 0
    }
  }

  val currentChapter = novel.chapters.getOrNull(currentChapterIndex) ?: novel.chapters.firstOrNull()

  // Track page progress
  val estimatedCurrentPage by remember(lazyListState.firstVisibleItemIndex, novel.storyItems.size, novel.totalPages) {
    derivedStateOf {
      if (novel.storyItems.isNotEmpty()) {
        val frac = (lazyListState.firstVisibleItemIndex - 1).coerceAtLeast(0).toFloat() / novel.storyItems.size.toFloat()
        val p = (frac * (novel.totalPages - 1)).toInt() + 1
        p.coerceIn(1, novel.totalPages)
      } else {
        novel.currentPage.coerceAtLeast(1)
      }
    }
  }

  // Auto-save reading progress (debounced to preserve smooth scroll performance)
  LaunchedEffect(estimatedCurrentPage, hasInitialSynced) {
    if (hasInitialSynced && estimatedCurrentPage != novel.currentPage) {
      delay(800L)
      onSaveProgress(estimatedCurrentPage)
    }
  }

  // Automatic completion detection: mark finished when reaching the end of the last chapter
  var hasMarkedFinished by remember(novel.id) { mutableStateOf(novel.isFinished) }
  val isAtEndPosition by remember(lazyListState) {
    derivedStateOf {
      val total = lazyListState.layoutInfo.totalItemsCount
      val lastVis = lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
      total > 1 && lastVis >= total - 2
    }
  }

  LaunchedEffect(isAtEndPosition, hasMarkedFinished) {
    if (isAtEndPosition && !hasMarkedFinished) {
      hasMarkedFinished = true
      viewModel?.markNovelAsFinished(novel.id)
      onSaveProgress(novel.totalPages)
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      if (hasInitialSynced) {
        onSaveProgress(estimatedCurrentPage)
      }
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

  LaunchedEffect(searchMatches.size) {
    if (currentSearchMatchIndex >= searchMatches.size) {
      currentSearchMatchIndex = 0
    }
  }

  fun jumpToSearchMatch(match: SearchMatch) {
    coroutineScope.launch {
      val targetIndex = (match.paragraphIndex + 1).coerceIn(0, novel.storyItems.size)
      lazyListState.scrollToItem(targetIndex, 0)
    }
  }

  // Immediate and precise chapter navigation in continuous scroll
  fun jumpToChapter(chapterIndex: Int, startParagraphIndex: Int) {
    coroutineScope.launch {
      // In LazyColumn, index 0 is reading_header, so item startParagraphIndex is at index (startParagraphIndex + 1)
      val targetIndex = (startParagraphIndex + 1).coerceIn(0, novel.storyItems.size)
      lazyListState.scrollToItem(targetIndex, 0)
    }
  }

  // Main Reader Canvas
  Box(
    modifier = modifier
      .fillMaxSize()
      .background(SoftCreamPaper)
      .testTag("reading_screen_container")
  ) {
    // Subtle Paper Ambient Lighting
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

    Column(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
    ) {
      // -------------------------------------------------------------
      // TOP HUD BAR: Back, Book Title, Search, Chapters, Typography, Bookmarks
      // -------------------------------------------------------------
      AnimatedVisibility(
        visible = isHudVisible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xF5F7F4EC))
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            // Left: Back button & Novel Title
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f, fill = false)
            ) {
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

              Spacer(modifier = Modifier.width(6.dp))

              Column {
                Text(
                  text = novel.title,
                  style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.5.sp
                  ),
                  color = CharcoalText,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = "Ch. ${toRomanNumeral(currentChapter?.number ?: (currentChapterIndex + 1))} • p. $estimatedCurrentPage of ${novel.totalPages} • ${currentChapter?.title ?: novel.chapterTitle}",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.5.sp,
                    color = AntiqueGold,
                    fontWeight = FontWeight.Medium
                  ),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right: In-Book Search, Chapters Modal Trigger, Typography, Bookmarks, Favorite
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
              // 1. Search in Book
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

              // 2. Chapters Table of Contents Button
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0x1AD4AF37),
                border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.7f)),
                modifier = Modifier
                  .clickable { isChapterModalOpen = true }
                  .testTag("chapters_button")
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                  Icon(
                    imageVector = Icons.Outlined.FormatListNumbered,
                    contentDescription = "Chapters Table of Contents",
                    tint = AntiqueGold,
                    modifier = Modifier.size(14.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = "Ch. ${toRomanNumeral(currentChapter?.number ?: (currentChapterIndex + 1))}",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontSize = 10.5.sp,
                      fontWeight = FontWeight.Bold,
                      color = CharcoalText
                    )
                  )
                  Spacer(modifier = Modifier.width(2.dp))
                  Text(
                    text = "▼",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontSize = 7.5.sp,
                      color = AntiqueGold
                    )
                  )
                }
              }

              // 2b. Quick Add Chapter Button (for Owner or assigned Translator)
              if (canAddChapter) {
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = AntiqueGold.copy(alpha = 0.16f),
                  border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.75f)),
                  modifier = Modifier
                    .clickable { isAddChapterModalOpen = true }
                    .testTag("quick_add_chapter_button")
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
                  ) {
                    Text(
                      text = "+ Ch.",
                      style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = CharcoalText
                      )
                    )
                  }
                }
              }

              // 3. Typography Customizer
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
                    contentDescription = "Reader typography",
                    tint = if (isTypographyCustomized) AntiqueGold else CharcoalSecondary,
                    modifier = Modifier.size(14.dp)
                  )
                  Spacer(modifier = Modifier.width(2.dp))
                  Text(
                    text = "Aa",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontSize = 10.sp,
                      fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold,
                      fontStyle = readerFontStyle,
                      color = if (isTypographyCustomized) AntiqueGold else CharcoalSecondary
                    )
                  )
                }
              }

              // 4. Bookmarks & Highlighted Lines Button
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
                  if (bookmarksList.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                      text = "${bookmarksList.size}",
                      style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = AntiqueGold
                      )
                    )
                  }
                }
              }

              // 5. Favorite Heart
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

          // In-Book Search Bar (if opened)
          if (isSearchOpen) {
            Surface(
              color = Color(0xF0FAF6EE),
              border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.35f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = AntiqueGold,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(8.dp))

                  BasicTextField(
                    value = searchQuery,
                    onValueChange = {
                      searchQuery = it
                      currentSearchMatchIndex = 0
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                      fontSize = 13.sp,
                      color = CharcoalText
                    ),
                    cursorBrush = SolidColor(AntiqueGold),
                    decorationBox = { innerTextField ->
                      Box(modifier = Modifier.fillMaxWidth()) {
                        if (searchQuery.isEmpty()) {
                          Text(
                            text = "Search lines, words, or character names...",
                            style = MaterialTheme.typography.bodyMedium.copy(
                              fontSize = 13.sp,
                              color = CharcoalTertiary
                            )
                          )
                        }
                        innerTextField()
                      }
                    },
                    modifier = Modifier
                      .weight(1f)
                      .testTag("in_book_search_input")
                  )

                  if (searchQuery.isNotBlank()) {
                    IconButton(
                      onClick = { searchQuery = "" },
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
                }

                if (searchMatches.isNotEmpty()) {
                  Spacer(modifier = Modifier.height(6.dp))
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = "${currentSearchMatchIndex + 1} of ${searchMatches.size} results",
                      style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AntiqueGold
                      )
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                      IconButton(
                        onClick = {
                          val next = (currentSearchMatchIndex - 1 + searchMatches.size) % searchMatches.size
                          currentSearchMatchIndex = next
                          jumpToSearchMatch(searchMatches[next])
                        },
                        modifier = Modifier.size(26.dp)
                      ) {
                        Icon(
                          imageVector = Icons.Outlined.KeyboardArrowUp,
                          contentDescription = "Previous Match",
                          tint = AntiqueGold,
                          modifier = Modifier.size(16.dp)
                        )
                      }

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
                          contentDescription = "Next Match",
                          tint = AntiqueGold,
                          modifier = Modifier.size(16.dp)
                        )
                      }

                      Spacer(modifier = Modifier.width(6.dp))

                      OutlinedButton(
                        onClick = { isSearchResultsListOpen = !isSearchResultsListOpen },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.5f))
                      ) {
                        Icon(
                          imageVector = Icons.Outlined.List,
                          contentDescription = null,
                          tint = AntiqueGold,
                          modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                          text = "All Matches",
                          style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.5.sp,
                            color = AntiqueGold
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

      // -------------------------------------------------------------
      // NOVEL VIEWPORT: PURE CONTINUOUS VERTICAL SCROLL
      // -------------------------------------------------------------
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
      ) {
        val baseFontSize = 17.5f * fontSizeScale
        val baseLineHeight = 30f * fontSizeScale * lineHeightScale
        val baseParagraphSpacing = (20f * paragraphSpacingScale).dp

        LazyColumn(
          state = lazyListState,
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 26.dp, vertical = 8.dp)
            .testTag("reading_lazy_column"),
          horizontalAlignment = Alignment.CenterHorizontally,
          contentPadding = PaddingValues(bottom = 36.dp)
        ) {
          item(key = "reading_header") {
            Column(
              modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth(),
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
                  lineHeight = 30.sp
                ),
                color = CharcoalText,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("reading_book_title")
              )

              Spacer(modifier = Modifier.height(8.dp))

              // Author Line and Clickable Translator Profile Badge
              val translatorSlot = remember(novel, authorSlots) {
                if (novel.authorSlot > 0) {
                  authorSlots.find { it.slotNumber == novel.authorSlot }
                } else {
                  authorSlots.find {
                    it.penName.equals(novel.author, ignoreCase = true) ||
                    it.authorName.equals(novel.author, ignoreCase = true)
                  }
                } ?: authorSlots.find { it.slotNumber == 0 }
              }

              if (novel.originalAuthor.isNotBlank()) {
                Text(
                  text = "BY ${novel.originalAuthor.uppercase()}",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.Bold
                  ),
                  color = CharcoalTertiary,
                  textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
              }

              val translatorLabel = when {
                novel.authorSlot > 0 -> "Translated by ${novel.author.ifBlank { "Translator Room ${novel.authorSlot}" }}"
                novel.author.isNotBlank() && !novel.author.equals(novel.originalAuthor, ignoreCase = true) -> "Curated by ${novel.author}"
                else -> "Curated by Strawberrycandy"
              }

              Surface(
                shape = RoundedCornerShape(18.dp),
                color = AntiqueGold.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.45f)),
                modifier = Modifier
                  .clip(RoundedCornerShape(18.dp))
                  .clickable {
                    viewingTranslatorSlot = translatorSlot
                  }
                  .testTag("novel_translator_profile_button")
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = "Translator Profile",
                    tint = AntiqueGold,
                    modifier = Modifier.size(14.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "$translatorLabel • View Profile →",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontSize = 10.5.sp,
                      fontWeight = FontWeight.Bold,
                      color = CharcoalText,
                      letterSpacing = 0.3.sp
                    )
                  )
                }
              }

              Spacer(modifier = Modifier.height(28.dp))
            }
          }

          itemsIndexed(
            items = novel.storyItems,
            key = { index, _ -> "story_item_$index" }
          ) { index, item ->
            Box(
              modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                .padding(bottom = baseParagraphSpacing)
            ) {
              when (item) {
                is StoryContentItem.ChapterBreak -> {
                  // Chapter Break Display
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
                        fontWeight = FontWeight.Bold,
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
                  val matchingHighlights = bookmarksList.filter { bmk ->
                    bmk.novelId == novel.id && (
                      bmk.paragraphIndex == index ||
                      paragraph.contains(bmk.quoteText, ignoreCase = true)
                    )
                  }
                  val hasHighlights = matchingHighlights.isNotEmpty()
                  val isSelectionActive = activeParagraphSelection?.paragraphIndex == index

                  Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                      modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                          if (isSelectionActive) Color(0x0E000000)
                          else if (hasHighlights) Color(matchingHighlights.firstOrNull()?.colorHex ?: 0xFFD4AF37).copy(alpha = 0.08f)
                          else Color.Transparent
                        )
                        .border(
                          border = if (isSelectionActive) {
                            BorderStroke(1.2.dp, AntiqueGold)
                          } else if (hasHighlights) {
                            BorderStroke(1.dp, Color(matchingHighlights.firstOrNull()?.colorHex ?: 0xFFD4AF37).copy(alpha = 0.45f))
                          } else {
                            BorderStroke(0.dp, Color.Transparent)
                          },
                          shape = RoundedCornerShape(12.dp)
                        )
                        .then(
                          if (isSelectionActive) {
                            Modifier.clickable {
                              activeParagraphSelection = null
                            }
                          } else {
                            Modifier.combinedClickable(
                              onLongClick = {
                                val sentences = splitParagraphIntoSentences(paragraph)
                                activeParagraphSelection = ActiveParagraphSelection(
                                  paragraphIndex = index,
                                  paragraphText = paragraph,
                                  sentences = sentences,
                                  selectedSentenceIndex = if (sentences.size > 1) 0 else -1
                                )
                              },
                              onClick = {
                                // Plain tap allows smooth scroll and doesn't interfere with gesture detection
                              }
                            )
                          }
                        )
                        .padding(
                          horizontal = if (isSelectionActive || hasHighlights) 12.dp else 0.dp,
                          vertical = if (isSelectionActive || hasHighlights) 8.dp else 0.dp
                        )
                    ) {
                      Column {
                        // If has highlight badges, show color ribbon on top
                        if (hasHighlights) {
                          Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 6.dp)
                          ) {
                            val firstHl = matchingHighlights.firstOrNull()
                            val hlColor = Color(firstHl?.colorHex ?: 0xFFD4AF37)
                            Box(
                              modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(hlColor)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                              text = "HIGHLIGHTED FAVORITE LINE",
                              style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.5.sp,
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.Bold
                              ),
                              color = hlColor
                            )
                          }
                        }

                        val formatRes = buildReadingParagraphAnnotatedString(
                          text = paragraph,
                          query = searchQuery,
                          isParagraphActive = searchMatches.getOrNull(currentSearchMatchIndex)?.paragraphIndex == index,
                          highlights = matchingHighlights
                        )

                        if (formatRes.isDivider) {
                          Box(
                            modifier = Modifier
                              .fillMaxWidth()
                              .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                          ) {
                            Text(
                              text = "❦",
                              style = MaterialTheme.typography.titleMedium,
                              color = AntiqueGold.copy(alpha = 0.7f)
                            )
                          }
                        } else {
                          val finalAlign = if (formatRes.alignment != TextAlign.Start) {
                            formatRes.alignment
                          } else if (isJustified) {
                            TextAlign.Justify
                          } else {
                            TextAlign.Start
                          }

                          val finalFontStyle = if (formatRes.isQuote) FontStyle.Italic else readerFontStyle

                          Text(
                            text = formatRes.annotatedString,
                            style = MaterialTheme.typography.bodyLarge.copy(
                              fontFamily = readerFontFamily,
                              fontWeight = readerFontWeight,
                              fontStyle = finalFontStyle,
                              color = CharcoalText,
                              lineHeight = baseLineHeight.sp,
                              fontSize = baseFontSize.sp,
                              textAlign = finalAlign,
                              textIndent = TextIndent(
                                firstLine = if (isFirstLineIndent && !formatRes.isQuote && formatRes.alignment == TextAlign.Start) 22.sp else 0.sp
                              )
                            ),
                            modifier = Modifier
                              .fillMaxWidth()
                              .padding(
                                start = if (formatRes.isQuote) 14.dp else 0.dp,
                                end = if (formatRes.isQuote) 10.dp else 0.dp
                              )
                          )
                        }
                      }
                    }

                    // -----------------------------------------------------------
                    // INTERACTIVE SENTENCE HIGHLIGHTING PALETTE & FAVORITE LINES
                    // -----------------------------------------------------------
                    AnimatedVisibility(
                      visible = isSelectionActive && activeParagraphSelection != null,
                      enter = fadeIn() + expandVertically(),
                      exit = fadeOut() + shrinkVertically()
                    ) {
                      val selection = activeParagraphSelection ?: return@AnimatedVisibility
                      val targetQuoteText = if (selection.selectedSentenceIndex in selection.sentences.indices) {
                        selection.sentences[selection.selectedSentenceIndex]
                      } else {
                        selection.paragraphText.trim()
                      }

                      val activeHighlight = bookmarksList.find {
                        it.novelId == novel.id && it.quoteText.trim() == targetQuoteText.trim()
                      }

                      Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF7F0)),
                        border = BorderStroke(1.2.dp, AntiqueGold.copy(alpha = 0.6f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        modifier = Modifier
                          .fillMaxWidth()
                          .padding(top = 8.dp)
                          .testTag("highlight_editor_card")
                      ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                          // Header
                          Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                          ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                              Icon(
                                imageVector = Icons.Filled.Bookmark,
                                contentDescription = null,
                                tint = AntiqueGold,
                                modifier = Modifier.size(16.dp)
                              )
                              Spacer(modifier = Modifier.width(6.dp))
                              Text(
                                text = "HIGHLIGHT FAVORITE LINE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                  fontSize = 9.sp,
                                  letterSpacing = 1.4.sp,
                                  fontWeight = FontWeight.Bold
                                ),
                                color = AntiqueGold
                              )
                            }
                            IconButton(
                              onClick = { activeParagraphSelection = null },
                              modifier = Modifier.size(24.dp)
                            ) {
                              Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Close",
                                tint = CharcoalSecondary,
                                modifier = Modifier.size(16.dp)
                              )
                            }
                          }

                          // Sentence selection chips (if multiple sentences exist)
                          if (selection.sentences.size > 1) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                              text = "Select a sentence or whole line:",
                              style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CharcoalSecondary
                              )
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            LazyRow(
                              horizontalArrangement = Arrangement.spacedBy(6.dp),
                              modifier = Modifier.fillMaxWidth()
                            ) {
                              itemsIndexed(selection.sentences) { sIdx, sText ->
                                val isSelected = selection.selectedSentenceIndex == sIdx
                                val isSentenceHighlighted = bookmarksList.find {
                                  it.novelId == novel.id && it.quoteText.trim() == sText.trim()
                                }

                                Surface(
                                  shape = RoundedCornerShape(12.dp),
                                  color = if (isSelected) AntiqueGold else if (isSentenceHighlighted != null) Color(isSentenceHighlighted.colorHex).copy(alpha = 0.2f) else Color(0x0C000000),
                                  border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) AntiqueGold else if (isSentenceHighlighted != null) Color(isSentenceHighlighted.colorHex) else SubtleBorder
                                  ),
                                  modifier = Modifier
                                    .clickable {
                                      activeParagraphSelection = selection.copy(selectedSentenceIndex = sIdx)
                                    }
                                    .testTag("sentence_chip_$sIdx")
                                ) {
                                  Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                  ) {
                                    if (isSentenceHighlighted != null) {
                                      Box(
                                        modifier = Modifier
                                          .size(6.dp)
                                          .clip(CircleShape)
                                          .background(if (isSelected) SoftCreamPaper else Color(isSentenceHighlighted.colorHex))
                                      )
                                      Spacer(modifier = Modifier.width(5.dp))
                                    }
                                    Text(
                                      text = "Sentence ${sIdx + 1}",
                                      style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) SoftCreamPaper else CharcoalText
                                      )
                                    )
                                  }
                                }
                              }

                              // Whole paragraph chip
                              item {
                                val isSelected = selection.selectedSentenceIndex == -1
                                Surface(
                                  shape = RoundedCornerShape(12.dp),
                                  color = if (isSelected) AntiqueGold else Color(0x0C000000),
                                  border = BorderStroke(1.dp, if (isSelected) AntiqueGold else SubtleBorder),
                                  modifier = Modifier
                                    .clickable {
                                      activeParagraphSelection = selection.copy(selectedSentenceIndex = -1)
                                    }
                                    .testTag("sentence_chip_all")
                                ) {
                                  Text(
                                    text = "Whole Paragraph",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                      fontSize = 10.sp,
                                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                      color = if (isSelected) SoftCreamPaper else CharcoalText
                                    ),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                  )
                                }
                              }
                            }
                          }

                          Spacer(modifier = Modifier.height(10.dp))

                          // Quote preview
                          Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (activeHighlight != null) Color(activeHighlight.colorHex).copy(alpha = 0.15f) else Color(0x08000000),
                            border = BorderStroke(
                              1.dp,
                              if (activeHighlight != null) Color(activeHighlight.colorHex).copy(alpha = 0.5f) else SubtleBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                          ) {
                            Text(
                              text = "“$targetQuoteText”",
                              style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = readerFontFamily,
                                fontStyle = FontStyle.Italic,
                                fontSize = 12.5.sp,
                                lineHeight = 18.sp,
                                color = CharcoalText
                              ),
                              modifier = Modifier.padding(10.dp),
                              maxLines = 4,
                              overflow = TextOverflow.Ellipsis
                            )
                          }

                          Spacer(modifier = Modifier.height(12.dp))

                          // Color Swatches Palette (Variety of highlight colors)
                          Text(
                            text = "Tap a color to highlight this sentence:",
                            style = MaterialTheme.typography.labelSmall.copy(
                              fontSize = 9.5.sp,
                              fontWeight = FontWeight.SemiBold,
                              color = CharcoalSecondary
                            )
                          )
                          Spacer(modifier = Modifier.height(8.dp))

                          Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                          ) {
                            ReaderHighlightColors.forEach { colorOpt ->
                              val isThisColorActive = activeHighlight?.colorHex == colorOpt.hex
                              Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                  .clickable {
                                    viewModel?.saveHighlight(
                                      novelId = novel.id,
                                      chapterTitle = currentChapter?.title ?: novel.chapterTitle,
                                      quoteText = targetQuoteText,
                                      paragraphIndex = selection.paragraphIndex,
                                      colorHex = colorOpt.hex
                                    )
                                  }
                                  .testTag("color_swatch_${colorOpt.name.lowercase().replace(" ", "_")}")
                              ) {
                                Box(
                                  modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(colorOpt.displayColor)
                                    .border(
                                      width = if (isThisColorActive) 2.5.dp else 1.dp,
                                      color = if (isThisColorActive) CharcoalText else Color(0x33000000),
                                      shape = CircleShape
                                    ),
                                  contentAlignment = Alignment.Center
                                ) {
                                  if (isThisColorActive) {
                                    Icon(
                                      imageVector = Icons.Outlined.Check,
                                      contentDescription = "Active color",
                                      tint = Color.White,
                                      modifier = Modifier.size(20.dp)
                                    )
                                  }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                  text = colorOpt.emoji,
                                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp)
                                )
                              }
                            }
                          }

                          // Bottom buttons
                          Spacer(modifier = Modifier.height(12.dp))
                          Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                          ) {
                            if (activeHighlight != null) {
                              OutlinedButton(
                                onClick = {
                                  viewModel?.deleteBookmark(activeHighlight.id)
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC74350)),
                                border = BorderStroke(1.dp, Color(0xFFC74350).copy(alpha = 0.5f)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("clear_highlight_button")
                              ) {
                                Icon(
                                  imageVector = Icons.Outlined.Delete,
                                  contentDescription = null,
                                  tint = Color(0xFFC74350),
                                  modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                  text = "Remove Highlight",
                                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                  color = Color(0xFFC74350)
                                )
                              }
                            } else {
                              Spacer(modifier = Modifier.width(1.dp))
                            }

                            OutlinedButton(
                              onClick = {
                                val targetEnd = currentChapter?.endParagraphIndex ?: index
                                activeParagraphSelection = null
                                coroutineScope.launch {
                                  try {
                                    val count = lazyListState.layoutInfo.totalItemsCount
                                    if (count > 0) {
                                      lazyListState.animateScrollToItem((targetEnd + 1).coerceIn(0, count - 1))
                                    }
                                  } catch (_: Exception) {}
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

              // Check if a chapter ends at this paragraph index (for multi-chapter novels)
              val chapterEndingHere = novel.chapters.find {
                it.endParagraphIndex == index && it.index < novel.chapters.lastIndex
              }
              if (chapterEndingHere != null) {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp, bottom = 16.dp),
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  Text(
                    text = "— END OF ${chapterEndingHere.title.uppercase()} —",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontSize = 10.sp,
                      letterSpacing = 1.4.sp,
                      fontWeight = FontWeight.Bold
                    ),
                    color = AntiqueGold
                  )
                  Spacer(modifier = Modifier.height(16.dp))

                  val thisChapterComments = remember(allNovelComments, chapterEndingHere.title) {
                    allNovelComments.filter { it.chapterTitle.equals(chapterEndingHere.title, ignoreCase = true) }
                  }

                  ChapterCommentsSection(
                    chapterTitle = chapterEndingHere.title,
                    comments = thisChapterComments,
                    activeReaderName = activeUser?.displayName,
                    activeReaderEmail = activeUser?.email,
                    isOwner = activeUser?.role == "OWNER" || activeUser?.authorSlot == 0,
                    onPostComment = { text, penName, parentCommentId, replyToReaderName ->
                      viewModel?.postComment(
                        novelId = novel.id,
                        chapterTitle = chapterEndingHere.title,
                        text = text,
                        penName = penName,
                        parentCommentId = parentCommentId,
                        replyToReaderName = replyToReaderName
                      )
                    },
                    onLikeComment = { commentId ->
                      viewModel?.likeComment(commentId)
                    },
                    onDeleteComment = { commentId ->
                      viewModel?.deleteComment(commentId)
                    },
                    onSavePenName = { newName ->
                      viewModel?.updateReaderName(newName)
                    }
                  )

                  Spacer(modifier = Modifier.height(28.dp))
                  Text(
                    text = "— ❦ —",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AntiqueGold.copy(alpha = 0.5f)
                  )
                }
              }
            }
          }

          item(key = "reading_footer") {
            Column(
              modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth(),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Spacer(modifier = Modifier.height(32.dp))

              // Celebratory Finished Novel Card
              Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0x1AD4AF37),
                border = BorderStroke(1.2.dp, AntiqueGold.copy(alpha = 0.7f)),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("novel_finished_celebration_card")
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Box(
                    modifier = Modifier
                      .size(44.dp)
                      .clip(CircleShape)
                      .background(AntiqueGold.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(
                      imageVector = Icons.Outlined.Check,
                      contentDescription = "Finished",
                      tint = AntiqueGold,
                      modifier = Modifier.size(24.dp)
                    )
                  }
                  Spacer(modifier = Modifier.width(14.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Text(
                        text = "MANUSCRIPT FINISHED",
                        style = MaterialTheme.typography.labelSmall.copy(
                          fontSize = 9.sp,
                          letterSpacing = 1.4.sp,
                          fontWeight = FontWeight.Bold
                        ),
                        color = AntiqueGold
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF2E7D32).copy(alpha = 0.15f)
                      ) {
                        Text(
                          text = "✓ Finished",
                          style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                          ),
                          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                      }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = "You reached the end of ${novel.title}",
                      style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold
                      ),
                      color = CharcoalText
                    )
                    Text(
                      text = if (activeUser != null) "Recorded in your library • +1 Pen Name Point earned" else "Recorded as Finished in your library",
                      style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                      color = CharcoalSecondary
                    )
                  }
                }
              }

              Spacer(modifier = Modifier.height(32.dp))

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

              // Final Chapter Comments Section
              val lastChapterTitle = novel.chapters.lastOrNull()?.title ?: novel.chapterTitle
              val lastChapterComments = remember(allNovelComments, lastChapterTitle) {
                allNovelComments.filter { it.chapterTitle.equals(lastChapterTitle, ignoreCase = true) }
              }

              ChapterCommentsSection(
                chapterTitle = lastChapterTitle,
                comments = lastChapterComments,
                activeReaderName = activeUser?.displayName,
                activeReaderEmail = activeUser?.email,
                isOwner = activeUser?.role == "OWNER" || activeUser?.authorSlot == 0,
                onPostComment = { text, penName, parentCommentId, replyToReaderName ->
                  viewModel?.postComment(
                    novelId = novel.id,
                    chapterTitle = lastChapterTitle,
                    text = text,
                    penName = penName,
                    parentCommentId = parentCommentId,
                    replyToReaderName = replyToReaderName
                  )
                },
                onLikeComment = { commentId ->
                  viewModel?.likeComment(commentId)
                },
                onDeleteComment = { commentId ->
                  viewModel?.deleteComment(commentId)
                },
                modifier = Modifier.widthIn(max = 640.dp)
              )

              Spacer(modifier = Modifier.height(48.dp))
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
        onAddChapterClick = if (canAddChapter) {
          {
            isChapterModalOpen = false
            isAddChapterModalOpen = true
          }
        } else null,
        onDismiss = { isChapterModalOpen = false }
      )
    }

    // 1b. Add Chapter Dialog (Curator & Translator Contribution)
    if (isAddChapterModalOpen) {
      AddChapterDialog(
        novel = novel,
        onDismiss = { isAddChapterModalOpen = false },
        onAddChapter = { _, newTitle, newContent ->
          viewModel?.addChapterToNovel(novel.id, newTitle, newContent)
          isAddChapterModalOpen = false
        }
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
        pageTurnMode = "scroll",
        paragraphSpacingScale = paragraphSpacingScale,
        isJustified = isJustified,
        isFirstLineIndent = isFirstLineIndent,
        isBold = isBold,
        isItalic = isItalic,
        onSelectFontType = { fontType ->
          selectedFontType = fontType
          typographyPrefs.edit().putString("font_family_type", fontType).apply()
        },
        onCustomFontUploaded = { filePath, fontName ->
          customFontPath = filePath
          customFontName = fontName
          selectedFontType = "custom"
          typographyPrefs.edit()
            .putString("custom_font_file_path", filePath)
            .putString("custom_font_name", fontName)
            .putString("font_family_type", "custom")
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
        onSelectPageTurnMode = {},
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
          typographyPrefs.edit().putBoolean("is_bold", bold).putBoolean("is_italic", italic).apply()
        },
        onResetDefaults = {
          selectedFontType = "serif"
          customFontPath = null
          customFontName = null
          fontSizeScale = 1.0f
          lineHeightScale = 1.0f
          paragraphSpacingScale = 1.0f
          isJustified = false
          isFirstLineIndent = false
          isBold = false
          isItalic = false
          typographyPrefs.edit().clear().apply()
        },
        onDismiss = { isTypographyModalOpen = false }
      )
    }

    // 4. Bookmarks & Highlighted Lines Modal
    if (isBookmarksModalOpen) {
      BookmarksHighlightsModal(
        bookmarks = bookmarksList,
        onJumpToParagraph = { paragraphIndex ->
          jumpToChapter(0, paragraphIndex)
        },
        onDeleteBookmark = { bookmarkId ->
          viewModel?.deleteBookmark(bookmarkId)
        },
        onDismiss = { isBookmarksModalOpen = false }
      )
    }

    // 5. Translator Profile Modal (Viewable by anyone, translators can only modify their own room)
    if (viewingTranslatorSlot != null) {
      val slot = viewingTranslatorSlot!!
      val user = activeUser
      val isSoleOwner = StrawberrycandyViewModel.isOwnerEmail(user?.email)
      val canModifyThisRoom = isSoleOwner || (
        user?.role == "TRANSLATOR" && (
          user.authorSlot == slot.slotNumber ||
          (user.email.isNotBlank() && slot.translatorEmail?.equals(user.email, ignoreCase = true) == true)
        )
      )
      TranslatorProfileModal(
        slot = slot,
        novels = allNovelsList,
        isOwner = isSoleOwner,
        isOwnerOrTranslator = canModifyThisRoom,
        onDismiss = { viewingTranslatorSlot = null },
        onSelectNovel = { newNovel ->
          viewingTranslatorSlot = null
          if (newNovel.id != novel.id) {
            onSelectNovel?.invoke(newNovel) ?: viewModel?.recordNovelRead(newNovel.id)
          }
        },
        onUpdateCoverImage = { slotNum, imagePath ->
          if (canModifyThisRoom) {
            viewModel?.updateAuthorSlotCover(slotNum, imagePath)
            viewingTranslatorSlot = slot.copy(coverImageUri = imagePath)
          }
        },
        onOpenUploadForSlot = null,
        onToggleSlotPermission = if (isSoleOwner) { slotNum, isGranted ->
          viewModel?.setSlotPermission(slotNum, isGranted)
        } else null,
        onAddChapterToNovel = if (canModifyThisRoom) { novelId, title, content ->
          viewModel?.addChapterToNovel(novelId, title, content)
        } else null,
        onEditNovel = null
      )
    }
  }
}

private fun splitParagraphIntoSentences(paragraph: String): List<String> {
  val clean = paragraph.trim()
  if (clean.isEmpty()) return emptyList()
  val regex = Regex("""(?<=[.!?…]["'”’]?)\s+(?=[A-Z0-9"“'‘—])""")
  val parts = clean.split(regex).map { it.trim() }.filter { it.isNotBlank() }
  return if (parts.size <= 1) listOf(clean) else parts
}

data class FormattedTextResult(
  val annotatedString: AnnotatedString,
  val alignment: TextAlign,
  val isQuote: Boolean,
  val isDivider: Boolean,
)

private fun buildReadingParagraphAnnotatedString(
  text: String,
  query: String,
  isParagraphActive: Boolean,
  highlights: List<BookmarkHighlightEntity> = emptyList(),
): FormattedTextResult {
  var cleanText = text.trim()

  if (cleanText == "❦" || cleanText == "• • •" || cleanText == "***" || cleanText == "---") {
    return FormattedTextResult(
      annotatedString = buildAnnotatedString { append(cleanText) },
      alignment = TextAlign.Center,
      isQuote = false,
      isDivider = true
    )
  }

  var isCenter = false
  var isRight = false
  var isQuote = false

  if (cleanText.startsWith("[align:center]") && cleanText.endsWith("[/align]")) {
    isCenter = true
    cleanText = cleanText.removePrefix("[align:center]").removeSuffix("[/align]").trim()
  } else if (cleanText.startsWith("[align:right]") && cleanText.endsWith("[/align]")) {
    isRight = true
    cleanText = cleanText.removePrefix("[align:right]").removeSuffix("[/align]").trim()
  }

  if (cleanText.startsWith("[quote]") && cleanText.endsWith("[/quote]")) {
    isQuote = true
    cleanText = cleanText.removePrefix("[quote]").removeSuffix("[/quote]").trim()
  }

  // Parse inline tags <b>, <strong>, <i>, <em>
  val styleSpans = mutableListOf<Triple<Int, Int, SpanStyle>>()
  val plainBuilder = StringBuilder()
  val tagRegex = Regex("""<(b|strong|i|em)>(.*?)</\1>""", RegexOption.IGNORE_CASE)
  var cursor = 0
  for (match in tagRegex.findAll(cleanText)) {
    if (match.range.first > cursor) {
      plainBuilder.append(cleanText.substring(cursor, match.range.first))
    }
    val tag = match.groupValues[1].lowercase()
    val inner = match.groupValues[2]
    val start = plainBuilder.length
    plainBuilder.append(inner)
    val end = plainBuilder.length
    if (tag == "b" || tag == "strong") {
      styleSpans.add(Triple(start, end, SpanStyle(fontWeight = FontWeight.Bold)))
    } else if (tag == "i" || tag == "em") {
      styleSpans.add(Triple(start, end, SpanStyle(fontStyle = FontStyle.Italic)))
    }
    cursor = match.range.last + 1
  }
  if (cursor < cleanText.length) {
    plainBuilder.append(cleanText.substring(cursor))
  }
  val plainText = plainBuilder.toString()

  val annotated = buildAnnotatedString {
    append(plainText)
    for ((start, end, style) in styleSpans) {
      if (start in 0..plainText.length && end in 0..plainText.length && start < end) {
        try {
          addStyle(style, start, end)
        } catch (_: Exception) {}
      }
    }

    // Apply color highlights from bookmarks / favorite lines
    for (hl in highlights) {
      val quote = hl.quoteText.trim()
      if (quote.isNotBlank()) {
        var hIdx = 0
        while (hIdx < plainText.length) {
          val found = plainText.indexOf(quote, hIdx, ignoreCase = true)
          if (found == -1) break
          val hlColor = Color(hl.colorHex)
          val safeStart = found.coerceIn(0, plainText.length)
          val safeEnd = (found + quote.length).coerceIn(0, plainText.length)
          if (safeStart < safeEnd) {
            try {
              addStyle(
                SpanStyle(
                  background = hlColor.copy(alpha = 0.35f),
                  fontWeight = FontWeight.Medium
                ),
                safeStart,
                safeEnd
              )
            } catch (_: Exception) {}
          }
          hIdx = found + maxOf(1, quote.length)
        }
      }
    }

    // Apply search query highlighting
    if (query.isNotBlank() && query.trim().length >= 2) {
      val q = query.trim()
      var sIdx = 0
      while (sIdx < plainText.length) {
        val found = plainText.indexOf(q, sIdx, ignoreCase = true)
        if (found == -1) break
        val safeStart = found.coerceIn(0, plainText.length)
        val safeEnd = (found + q.length).coerceIn(0, plainText.length)
        if (safeStart < safeEnd) {
          try {
            addStyle(
              SpanStyle(
                background = if (isParagraphActive) AntiqueGold else Color(0x66D4AF37),
                color = if (isParagraphActive) Color.White else CharcoalText,
                fontWeight = FontWeight.Bold
              ),
              safeStart,
              safeEnd
            )
          } catch (_: Exception) {}
        }
        sIdx = found + maxOf(1, q.length)
      }
    }
  }

  val align = when {
    isCenter -> TextAlign.Center
    isRight -> TextAlign.End
    else -> TextAlign.Start
  }

  return FormattedTextResult(
    annotatedString = annotated,
    alignment = align,
    isQuote = isQuote,
    isDivider = false
  )
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
