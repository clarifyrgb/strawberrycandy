package com.example.ui.screens

import com.example.R
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.BookmarkAdded
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.AuthorSlotEntity
import com.example.data.local.ReaderProfileEntity
import com.example.model.NovelWithState
import com.example.ui.components.AboutModal
import com.example.ui.components.AuthModal
import com.example.ui.components.AuthorRoomsModal
import com.example.ui.components.CloudPublishModal
import com.example.ui.components.EditNovelModal
import com.example.ui.components.NovelSearchBar
import com.example.ui.components.OwnerUploadDialog
import com.example.ui.components.RealtimeNewNovelBanner
import com.example.ui.components.ReaderProfileModal
import com.example.ui.components.TranslatorProfileModal
import com.example.ui.theme.AntiqueGold
import com.example.ui.theme.AntiqueGoldLight
import com.example.ui.theme.CardWarmWhite
import com.example.ui.theme.DeepBurgundy
import com.example.ui.theme.CharcoalSecondary
import com.example.ui.theme.CharcoalTertiary
import com.example.ui.theme.CharcoalText
import com.example.ui.theme.CreamBackground
import com.example.ui.theme.SoftCreamPaper
import com.example.ui.theme.SubtleBorder
import com.example.viewmodel.NovelSortOption
import com.example.viewmodel.ShelfFilter
import com.example.viewmodel.StrawberrycandyViewModel
import com.example.util.formatStatCount
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@Composable
fun HomeScreen(
  viewModel: StrawberrycandyViewModel,
  onSelectNovel: (NovelWithState) -> Unit,
  modifier: Modifier = Modifier,
) {
  val uiState by viewModel.uiState.collectAsState()
  val allNovelsList by viewModel.allNovels.collectAsState()
  val myCommentsHistory by viewModel.myCommentsHistory.collectAsState(initial = emptyList())
  val novels = uiState.novels
  val activeUser = uiState.activeUser
  var selectedIndex by remember { mutableIntStateOf(0) }
  var isAuthorRoomsModalOpen by remember { mutableStateOf(false) }
  var isAboutModalOpen by remember { mutableStateOf(false) }
  var selectedUploadSlot by remember { mutableIntStateOf(0) }
  var selectedTranslatorForDetail by remember { mutableStateOf<AuthorSlotEntity?>(null) }
  var slotToGrantPermission by remember { mutableStateOf<AuthorSlotEntity?>(null) }
  var novelToEdit by remember { mutableStateOf<NovelWithState?>(null) }
  var isCoverGalleryMode by remember { mutableStateOf(false) }
  var isSortMenuOpen by remember { mutableStateOf(false) }

  val safeIndex = if (novels.isNotEmpty()) selectedIndex.coerceIn(0, novels.size - 1) else 0

  // Find a novel currently in progress to feature in "Continue Reading"
  val continueReadingNovel = novels.firstOrNull { it.inReadingList && it.currentPage > 1 }
    ?: novels.firstOrNull { it.inReadingList }

  val allTranslatorSlots = remember(uiState.authorSlots) {
    (1..10).map { slotNum ->
      uiState.authorSlots.find { it.slotNumber == slotNum } ?: AuthorSlotEntity(
        slotNumber = slotNum,
        authorName = "",
        penName = "",
        bio = "",
        avatarColorHex = 0xFF353C48,
        accessCode = "AUTH-ROOM-$slotNum",
        isClaimed = false,
        isPermissionGranted = false
      )
    }
  }
  val activeTranslators = remember(allTranslatorSlots) {
    allTranslatorSlots.filter { it.isPermissionGranted }
  }
  val futureGrantableTranslators = remember(allTranslatorSlots) {
    allTranslatorSlots.filter { !it.isPermissionGranted }
  }

  val isSoleOwner = viewModel.isOwner(activeUser)
  val isTranslatorOrOwner = viewModel.canEditNovel(activeUser, uiState.authorSlots)
  val canUploadNovel = viewModel.canUploadNovel(activeUser, uiState.authorSlots)

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(CreamBackground)
      .statusBarsPadding()
      .testTag("home_screen_container")
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .navigationBarsPadding()
        .padding(bottom = 70.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // 1. Top Utility Header: Reader Sign-in, Author Rooms & Upload actions
      TopUtilityBar(
        activeUser = activeUser,
        isSoleOwner = isSoleOwner,
        canUpload = canUploadNovel,
        onOpenAuth = { viewModel.openAuthDialog() },
        onSignOut = { viewModel.signOut() },
        onOpenProfile = { viewModel.openProfileDialog() },
        onOpenAbout = { isAboutModalOpen = true },
        onOpenAuthorRooms = {
          isAuthorRoomsModalOpen = true
        },
        onOpenUpload = {
          if (isSoleOwner) {
            selectedUploadSlot = 0
            viewModel.openUploadDialog()
          } else if (activeUser != null && (activeUser.role.equals("TRANSLATOR", ignoreCase = true) || canUploadNovel)) {
            selectedUploadSlot = activeUser.authorSlot ?: 1
            viewModel.openUploadDialog()
          } else if (activeUser == null) {
            viewModel.openAuthDialog()
            viewModel.showSnackbar("Please sign in as Translator or Archive Owner to publish manuscripts")
          } else {
            viewModel.showSnackbar("Publishing manuscripts is reserved for the Archive Owner and Translators")
          }
        },
        activeTranslatorsCount = activeTranslators.size
      )

      // 2. Literary Hero Masthead removed per user request


      if (activeUser == null) {
        // Guest mode: Novels are hidden until user logs in
        GuestArchiveLockedView(
          rememberedAccounts = uiState.rememberedAccounts,
          onOpenAuth = { email -> viewModel.openAuthDialog(email) }
        )
      } else {
        // Logged-in mode
        // 1. Novel Search Bar for Readers and Translators
        NovelSearchBar(
          query = uiState.novelSearchQuery,
          onQueryChange = { viewModel.setNovelSearchQuery(it) },
          onClearQuery = { viewModel.clearNovelSearchQuery() },
          matchCount = novels.size,
          totalCount = allNovelsList.size,
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
        )

        val genres = listOf("All", "Romance", "Fantasy", "Mystery", "Sci-Fi", "Historical", "Monograph")
        val currentGenre = remember(uiState.novelSearchQuery) {
          val q = uiState.novelSearchQuery.trim()
          if (q.isBlank()) "All" else genres.firstOrNull { it.equals(q, ignoreCase = true) } ?: q
        }

        GenreFilterRow(
          genres = genres,
          selectedGenre = currentGenre,
          onSelectGenre = { genre ->
            if (genre.equals("All", ignoreCase = true)) {
              viewModel.clearNovelSearchQuery()
            } else {
              viewModel.setNovelSearchQuery(genre)
            }
          }
        )

      // Real-Time Alert for newly posted novels in APK
      val alertNovel = uiState.newlyPostedNovelAlert
      if (alertNovel != null) {
        RealtimeNewNovelBanner(
          novel = alertNovel,
          onReadNow = {
            onSelectNovel(alertNovel)
            viewModel.dismissNewNovelAlert()
          },
          onDismiss = {
            viewModel.dismissNewNovelAlert()
          },
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
        )
      }

      // 3. Clean Archive Header & Sort Bar (Reading, Finished, TBR & Shelf moved to Reader and Translator Profiles for clean aesthetic)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 24.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(6.dp)
              .clip(CircleShape)
              .background(AntiqueGold)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "LITERARY COLLECTION (${novels.size})",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 9.5.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.2.sp,
              color = CharcoalTertiary
            )
          )
          Spacer(modifier = Modifier.width(8.dp))
          // Real-time live status chip
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF2E7D32).copy(alpha = 0.12f),
            border = BorderStroke(0.6.dp, Color(0xFF2E7D32).copy(alpha = 0.35f)),
            onClick = {
              if (uiState.hasNewReleases) {
                viewModel.showNewReleasesOnly()
              }
            }
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(5.dp)
                  .clip(CircleShape)
                  .background(Color(0xFF2E7D32))
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = if (uiState.activeFilter == ShelfFilter.NEW_RELEASES) "NEW RELEASES" else "LIVE ARCHIVE",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 8.sp,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.6.sp,
                  color = Color(0xFF2E7D32)
                )
              )
            }
          }

          if (uiState.activeFilter == ShelfFilter.NEW_RELEASES) {
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = SoftCreamPaper,
              border = BorderStroke(0.6.dp, SubtleBorder),
              onClick = { viewModel.setFilter(ShelfFilter.ALL) }
            ) {
              Text(
                text = "Show All ✕",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 8.sp,
                  fontWeight = FontWeight.Bold,
                  color = CharcoalSecondary
                ),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
              )
            }
          }
        }

        // Novel Actions: Sort Menu
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          // Sort Dropdown Button
          Box {
            Surface(
              onClick = { isSortMenuOpen = true },
              shape = RoundedCornerShape(14.dp),
              color = SoftCreamPaper,
              border = BorderStroke(1.dp, SubtleBorder),
              modifier = Modifier.height(28.dp)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp)
              ) {
                Icon(
                  imageVector = Icons.Outlined.Sort,
                  contentDescription = "Sort novels",
                  tint = AntiqueGold,
                  modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = uiState.activeSort.label,
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                  color = CharcoalText
                )
              }
            }

            DropdownMenu(
              expanded = isSortMenuOpen,
              onDismissRequest = { isSortMenuOpen = false }
            ) {
              NovelSortOption.entries.forEach { option ->
                DropdownMenuItem(
                text = {
                  Text(
                    text = option.label,
                    style = MaterialTheme.typography.bodyMedium.copy(
                      fontWeight = if (uiState.activeSort == option) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = if (uiState.activeSort == option) AntiqueGold else CharcoalText
                  )
                },
                onClick = {
                  viewModel.setSort(option)
                  isSortMenuOpen = false
                }
              )
            }
          }
        }
      }
    }

      Spacer(modifier = Modifier.height(10.dp))

      // 4. "Continue Where You Stopped Reading" Card (if active reader has reading progress)
      if (continueReadingNovel != null) {
        val canEditContinue = viewModel.canEditSpecificNovel(continueReadingNovel, activeUser, uiState.authorSlots)
        ContinueReadingBanner(
          novel = continueReadingNovel,
          isTranslatorOrOwner = canEditContinue,
          onResumeReading = { onSelectNovel(continueReadingNovel) },
          onEditNovel = { novelToEdit = continueReadingNovel },
          onRemoveFromReading = { viewModel.toggleReadingList(continueReadingNovel.id) },
          modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
      }

      if (uiState.recentlyReadNovels.isNotEmpty() && uiState.novelSearchQuery.isBlank()) {
        RecentlyReadSection(
          recentlyReadNovels = uiState.recentlyReadNovels,
          onSelectNovel = onSelectNovel
        )
      }

      // 4.1. New Release Novels Shelf on Homepage
      val newReleaseNovels = remember(allNovelsList) {
        val recent = allNovelsList.filter { it.isNewRelease }.sortedByDescending { it.createdAt }
        if (recent.isNotEmpty()) recent else allNovelsList.sortedByDescending { it.createdAt }.take(5)
      }
      if (newReleaseNovels.isNotEmpty() && uiState.novelSearchQuery.isBlank()) {
        NewReleasesShowcase(
          novels = newReleaseNovels,
          authorSlots = uiState.authorSlots,
          onViewTranslator = { slot -> selectedTranslatorForDetail = slot },
          onSelectNovel = onSelectNovel,
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp)
        )
      }

      // 5. If actively searching, show a clear Search Results section so readers can find the novel immediately
      if (uiState.novelSearchQuery.isNotBlank() && novels.isNotEmpty()) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp)
            .testTag("search_results_list_section"),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "MATCHING MANUSCRIPTS (${novels.size})",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
              ),
              color = AntiqueGold
            )
            Text(
              text = "Tap to open and read",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
              color = CharcoalTertiary
            )
          }

          novels.forEach { matchedNovel ->
            SearchResultNovelCard(
              novel = matchedNovel,
              query = uiState.novelSearchQuery,
              onSelect = { onSelectNovel(matchedNovel) },
              onToggleFavorite = { viewModel.toggleFavorite(matchedNovel.id) }
            )
          }

          Spacer(modifier = Modifier.height(10.dp))
        }
      }

      // 6. Bookshelf: Displaying book cover cards in an elegant horizontal row
      if (novels.isNotEmpty()) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .testTag("bookshelf_section"),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState())
              .padding(horizontal = 24.dp, vertical = 12.dp)
              .testTag("bookshelf_horizontal_row"),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.Bottom
          ) {
            novels.forEachIndexed { index, novel ->
              val isFocused = safeIndex == index
              val canEditThisNovel = viewModel.canEditSpecificNovel(novel, activeUser, uiState.authorSlots)

              HorizontalNovelCard(
                novel = novel,
                index = index,
                isFocused = isFocused,
                isTranslatorOrOwner = canEditThisNovel,
                onCardClick = {
                  selectedIndex = index
                  onSelectNovel(novel)
                },
                onFocusClick = {
                  selectedIndex = index
                },
                onToggleFavorite = {
                  viewModel.toggleFavorite(novel.id)
                },
                onEditNovel = {
                  selectedIndex = index
                  novelToEdit = novel
                },
                onMarkReading = {
                  viewModel.markNovelAsReading(novel.id)
                },
                onMarkFinished = {
                  viewModel.markNovelAsFinished(novel.id)
                },
                onMarkToBeRead = {
                  viewModel.markNovelAsToBeRead(novel.id)
                }
              )
            }
          }

          // Refined architectural shelf plinth beneath the row
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 24.dp)
              .height(5.dp)
              .clip(RoundedCornerShape(2.5.dp))
              .background(
                brush = Brush.horizontalGradient(
                  listOf(
                    Color.Transparent,
                    AntiqueGold.copy(alpha = 0.45f),
                    AntiqueGold.copy(alpha = 0.45f),
                    Color.Transparent
                  )
                )
              )
          )

          // 5.1. Selected Novel Spotlight Feature
          val focusedNovel = novels.getOrNull(safeIndex) ?: novels.first()
          val canEditFocusedNovel = viewModel.canEditSpecificNovel(focusedNovel, activeUser, uiState.authorSlots)
          SelectedNovelSpotlight(
            novel = focusedNovel,
            canEditNovel = canEditFocusedNovel,
            authorSlots = uiState.authorSlots,
            onViewTranslator = { slot -> selectedTranslatorForDetail = slot },
            onReadNovel = { onSelectNovel(focusedNovel) },
            onEditNovel = { novelToEdit = focusedNovel },
            onToggleFavorite = { viewModel.toggleFavorite(focusedNovel.id) },
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 24.dp, vertical = 12.dp)
          )
        }
      } else {
        // Empty state for filters
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = SoftCreamPaper),
          border = BorderStroke(1.dp, SubtleBorder),
          modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
        ) {
          Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            val isGlobalArchiveEmpty = allNovelsList.isEmpty()
            val titleText = when {
              uiState.novelSearchQuery.isNotBlank() -> "No Matching Novels"
              uiState.activeFilter == ShelfFilter.FAVORITES -> "No Favorites Yet"
              uiState.activeFilter == ShelfFilter.NEW_RELEASES -> "No New Releases"
              isGlobalArchiveEmpty -> "Archive Ready for Manuscripts"
              else -> "Reading Shelf Empty"
            }
            val subtitleText = when {
              uiState.novelSearchQuery.isNotBlank() -> "We couldn't find any novels matching '${uiState.novelSearchQuery}'. Try searching by author, genre, or title."
              uiState.activeFilter == ShelfFilter.FAVORITES -> "Tap the heart icon on any novel to save your favorites here."
              uiState.activeFilter == ShelfFilter.NEW_RELEASES -> "Novels posted in the archive will appear here in real-time."
              isGlobalArchiveEmpty -> ""
              else -> "Start reading or tap any novel from the archive to add it to this shelf."
            }

            Text(
              text = titleText,
              style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif),
              color = CharcoalText
            )
            if (subtitleText.isNotBlank()) {
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = subtitleText,
                style = MaterialTheme.typography.bodySmall,
                color = CharcoalSecondary,
                textAlign = TextAlign.Center
              )
            }

            if (isGlobalArchiveEmpty && canUploadNovel) {
              Spacer(modifier = Modifier.height(14.dp))
              Button(
                onClick = {
                  selectedUploadSlot = activeUser?.authorSlot ?: 0
                  viewModel.openUploadDialog()
                },
                colors = ButtonDefaults.buttonColors(
                  containerColor = AntiqueGold,
                  contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("empty_state_post_novel_button")
              ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  "Post New Novel",
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
              }
            }

            if (uiState.novelSearchQuery.isNotBlank()) {
              Spacer(modifier = Modifier.height(10.dp))
              TextButton(
                onClick = { viewModel.clearNovelSearchQuery() },
                modifier = Modifier.testTag("empty_state_clear_search_button")
              ) {
                Text("Clear Search Query", color = AntiqueGold, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Subtle Archive Colophon / Shelf Summary Plate so the bottom has balanced presence
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = SoftCreamPaper.copy(alpha = 0.75f),
        border = BorderStroke(1.dp, SubtleBorder.copy(alpha = 0.65f)),
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 24.dp)
      ) {
        Column(
          modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Outlined.MenuBook,
              contentDescription = null,
              tint = AntiqueGold,
              modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "COLLECTIVE ARCHIVE",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp
              ),
              color = CharcoalText
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "•",
              style = MaterialTheme.typography.labelSmall,
              color = CharcoalTertiary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.testTag("user_favorited_count_indicator")
            ) {
              Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = "Favorited novels",
                tint = if (uiState.totalFavoriteNovelsCount > 0) Color(0xFFE53935) else AntiqueGold,
                modifier = Modifier.size(11.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "${uiState.totalFavoriteNovelsCount} Favorited",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 9.5.sp,
                  fontWeight = FontWeight.SemiBold,
                  letterSpacing = 0.6.sp
                ),
                color = AntiqueGold
              )
            }
            if (activeUser != null) {
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "•",
                style = MaterialTheme.typography.labelSmall,
                color = CharcoalTertiary
              )
              Spacer(modifier = Modifier.width(8.dp))
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.testTag("user_points_indicator")
              ) {
                Icon(
                  imageVector = Icons.Filled.Star,
                  contentDescription = "Points",
                  tint = AntiqueGold,
                  modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                  text = "${activeUser.penNamePoints} pt${if (activeUser.penNamePoints != 1) "s" else ""}",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.6.sp
                  ),
                  color = AntiqueGold
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            text = "Handcrafted translations formatted for quiet, distraction-free reading.",
            style = MaterialTheme.typography.bodySmall.copy(
              fontFamily = FontFamily.Serif,
              fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
              fontSize = 11.5.sp
            ),
            color = CharcoalSecondary,
            textAlign = TextAlign.Center
          )
        }
      }
    } // End of activeUser != null

      Spacer(modifier = Modifier.height(18.dp))

      // Provenance security footer
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.padding(horizontal = 24.dp)
      ) {
        Icon(
          imageVector = Icons.Outlined.Lock,
          contentDescription = null,
          tint = CharcoalTertiary,
          modifier = Modifier.size(11.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "STRAWBERRYCANDY SECURE VAULT • GOOGLE & APPLE AUTH",
          style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 8.5.sp,
            letterSpacing = 1.4.sp
          ),
          color = CharcoalTertiary
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Footer About & Check Updates Button
      Surface(
        onClick = { isAboutModalOpen = true },
        shape = RoundedCornerShape(12.dp),
        color = SoftCreamPaper,
        border = BorderStroke(1.dp, SubtleBorder),
        modifier = Modifier.testTag("footer_about_app_button")
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
          Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = AntiqueGold,
            modifier = Modifier.size(13.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "About Strawberrycandy • v${com.example.BuildConfig.VERSION_NAME} • Check for Updates",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 9.5.sp,
              fontWeight = FontWeight.Medium,
              color = CharcoalSecondary
            )
          )
        }
      }
    }

    // Auth Modal Dialog
    if (uiState.isAuthDialogOpen) {
      AuthModal(
        onDismiss = { viewModel.closeAuthDialog() },
        initialEmail = uiState.authInitialEmail ?: "",
        onSignInWithGoogle = { email, password, name, role, authorSlot, isSignUp ->
          viewModel.signInWithGoogle(email = email, password = password, displayName = name, role = role, authorSlot = authorSlot, isSignUp = isSignUp)
        },
        onSignInWithApple = { email, password, name, role, authorSlot, isSignUp ->
          viewModel.signInWithApple(email = email, password = password, displayName = name, role = role, authorSlot = authorSlot, isSignUp = isSignUp)
        },
        rememberedAccounts = uiState.rememberedAccounts,
        onRequestPasswordResetCode = { email, onResult ->
          viewModel.requestPasswordResetCode(email, onResult)
        },
        onResetPasswordWithCode = { email, code, newPassword, onResult ->
          viewModel.resetPasswordWithCode(email, code, newPassword, onResult)
        },
        externalErrorMessage = uiState.authErrorMessage,
        onClearError = { viewModel.clearAuthError() },
        onSignInWithGoogleCredential = { idToken, email, name, role, authorSlot ->
          viewModel.signInWithGoogleCredential(
            idToken = idToken,
            email = email,
            displayName = name,
            role = role,
            authorSlot = authorSlot
          )
        }
      )
    }

    // Reader Profile Modal (Shows points, stats, freely editable name, and comment history)
    if (uiState.isProfileDialogOpen && activeUser != null) {
      ReaderProfileModal(
        activeUser = activeUser,
        readingCount = uiState.readingCount,
        finishedCount = uiState.finishedCount,
        toBeReadCount = uiState.toBeReadCount,
        isSoleOwner = isSoleOwner,
        canUpload = canUploadNovel,
        commentsHistory = myCommentsHistory,
        novelsList = allNovelsList,
        onDismiss = { viewModel.closeProfileDialog() },
        onChangePenName = { newName ->
          viewModel.updateReaderName(newName)
        },
        onDeleteComment = { commentId ->
          viewModel.deleteComment(commentId)
        },
        onSelectNovel = { novel ->
          viewModel.closeProfileDialog()
          onSelectNovel(novel)
        },
        onSwitchAccount = { viewModel.openAuthDialog() },
        onSignOut = { viewModel.signOut() },
        onOpenAbout = {
          viewModel.closeProfileDialog()
          isAboutModalOpen = true
        },
        onOpenUpload = {
          viewModel.closeProfileDialog()
          selectedUploadSlot = if (isSoleOwner) 0 else (activeUser.authorSlot ?: 1)
          viewModel.openUploadDialog()
        },
        onOpenAuthorRooms = {
          viewModel.closeProfileDialog()
          isAuthorRoomsModalOpen = true
        }
      )
    }

    // About Strawberrycandy & APK Update Modal
    if (isAboutModalOpen) {
      AboutModal(
        novelsCount = allNovelsList.size,
        isCloudSyncing = uiState.isCloudSyncing,
        lastCloudSyncTime = uiState.lastCloudSyncTime,
        cloudSyncedNovelsCount = uiState.cloudSyncedNovelsCount,
        cloudReadUrl = viewModel.getCloudReadUrl(),
        cloudWriteUrl = viewModel.getCloudWriteUrl(),
        gitHubToken = viewModel.getGitHubToken(),
        onSyncCloudArchive = { viewModel.refreshCloudArchive() },
        onSaveCloudSettings = { readUrl, writeUrl, token ->
          viewModel.setCloudReadUrl(readUrl)
          viewModel.setCloudWriteUrl(writeUrl)
          viewModel.setGitHubToken(token)
        },
        onPublishUpdateManifest = { versionName, versionCode, title, changelog, apkUrl, releasePageUrl, onComplete ->
          viewModel.publishUpdateManifest(versionName, versionCode, title, changelog, apkUrl, releasePageUrl, onComplete)
        },
        onCreateGitHubReleaseTag = { tagName, releaseTitle, releaseNotes, targetBranch, isDraft, alsoUpdateManifest, versionCode, onComplete ->
          viewModel.createGitHubReleaseTag(tagName, releaseTitle, releaseNotes, targetBranch, isDraft, alsoUpdateManifest, versionCode, onComplete)
        },
        onDismiss = { isAboutModalOpen = false }
      )
    }

    // Author Rooms / Translator Collective Archive Modal (5 Curators)
    if (isAuthorRoomsModalOpen) {
      AuthorRoomsModal(
        authorSlots = uiState.authorSlots,
        novels = allNovelsList,
        currentUser = activeUser,
        onDismiss = { isAuthorRoomsModalOpen = false },
        onOpenUploadForSlot = { slot ->
          selectedUploadSlot = slot
          isAuthorRoomsModalOpen = false
          viewModel.openUploadDialog()
        },
        onOpenAuth = { viewModel.openAuthDialog() },
        onUpdateSlot = { slot, name, penName, bio ->
          if (isSoleOwner || (activeUser?.role == "TRANSLATOR" && activeUser.authorSlot == slot)) {
            viewModel.updateAuthorSlotWithPoint(slot, name, penName, bio)
          }
        },
        onUpdateSlotCover = { slot, imagePath ->
          if (isSoleOwner || (activeUser?.role == "TRANSLATOR" && activeUser.authorSlot == slot)) {
            viewModel.updateAuthorSlotCover(slot, imagePath)
          }
        },
        onToggleSlotPermission = { slot, isGranted ->
          if (isSoleOwner) {
            viewModel.setSlotPermission(slot, isGranted)
          }
        },
        onSelectNovel = { novel ->
          isAuthorRoomsModalOpen = false
          onSelectNovel(novel)
        },
        onEditNovel = { novel ->
          isAuthorRoomsModalOpen = false
          novelToEdit = novel
        },
        onViewTranslatorArchive = { slot ->
          selectedTranslatorForDetail = slot
        },
        onGrantPermissionByEmail = if (isSoleOwner) { email, slot ->
          viewModel.grantPermissionByEmail(email, slot)
        } else null,
        onUpdateSlotByOwner = if (isSoleOwner) { slot, email, penName, bio, isGranted ->
          viewModel.updateAuthorSlotByOwner(slot, email, penName, bio, isGranted)
        } else null
      )
    }

    // Dedicated Personal Archive Page for a Translator (accessible to anyone)
    if (selectedTranslatorForDetail != null) {
      val activeSlot = uiState.authorSlots.find { it.slotNumber == selectedTranslatorForDetail!!.slotNumber }
        ?: selectedTranslatorForDetail!!
      val canModifyThisRoom = isSoleOwner || (
        activeUser?.role == "TRANSLATOR" && (
          activeUser.authorSlot == activeSlot.slotNumber ||
          (activeUser.email.isNotBlank() && activeSlot.translatorEmail?.equals(activeUser.email, ignoreCase = true) == true)
        )
      )
      TranslatorProfileModal(
        slot = activeSlot,
        novels = allNovelsList,
        isOwner = isSoleOwner,
        isOwnerOrTranslator = canModifyThisRoom,
        onDismiss = { selectedTranslatorForDetail = null },
        onSelectNovel = { novel ->
          selectedTranslatorForDetail = null
          onSelectNovel(novel)
        },
        onUpdateCoverImage = { slotNum, imagePath ->
          if (canModifyThisRoom) {
            viewModel.updateAuthorSlotCover(slotNum, imagePath)
            selectedTranslatorForDetail = activeSlot.copy(coverImageUri = imagePath)
          }
        },
        onOpenUploadForSlot = if (canModifyThisRoom) { slotNum ->
          selectedUploadSlot = slotNum
          viewModel.openUploadDialog()
        } else null,
        onToggleSlotPermission = if (isSoleOwner) { slotNum, isGranted ->
          viewModel.setSlotPermission(slotNum, isGranted)
        } else null,
        onAddChapterToNovel = if (canModifyThisRoom) { novelId, title, content ->
          viewModel.addChapterToNovel(novelId, title, content)
        } else null,
        onEditNovel = if (canModifyThisRoom) { novel ->
          selectedTranslatorForDetail = null
          novelToEdit = novel
        } else null
      )
    }

    // Quick Permission Grant Dialog for Future Translator Slots (Only for Archive Owner)
    if (isSoleOwner && slotToGrantPermission != null) {
      val slot = slotToGrantPermission!!
      AlertDialog(
        onDismissRequest = { slotToGrantPermission = null },
        icon = {
          Icon(
            imageVector = Icons.Outlined.PersonAdd,
            contentDescription = null,
            tint = AntiqueGold,
            modifier = Modifier.size(28.dp)
          )
        },
        title = {
          Text(
            text = "Grant Permission to ${slot.penName.ifBlank { "Writer's Room #${slot.slotNumber}" }}?",
            style = MaterialTheme.typography.titleMedium.copy(
              fontFamily = FontFamily.Serif,
              fontWeight = FontWeight.Bold
            ),
            color = CharcoalText,
            textAlign = TextAlign.Center
          )
        },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              text = "SEAT ${slot.slotNumber} • FUTURE TRANSLATOR SLOT",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.Bold
              ),
              color = AntiqueGold
            )
            Text(
              text = slot.bio,
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
              color = CharcoalSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (slot.translatorEmail != null) {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = AntiqueGold.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Mail,
                    contentDescription = null,
                    tint = AntiqueGold,
                    modifier = Modifier.size(14.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "Translator Gmail: ${slot.translatorEmail}",
                    style = MaterialTheme.typography.bodySmall.copy(
                      fontSize = 11.5.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = CharcoalText
                    )
                  )
                }
              }
              Spacer(modifier = Modifier.height(4.dp))
            }
            Text(
              text = "Granting permission will activate this translator slot, allowing them to publish translated manuscripts and appear as an active curator in the archive.",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, lineHeight = 15.sp),
              color = CharcoalText
            )
          }
        },
        confirmButton = {
          Button(
            onClick = {
              viewModel.setSlotPermission(slot.slotNumber, true)
              slotToGrantPermission = null
            },
            colors = ButtonDefaults.buttonColors(containerColor = CharcoalText),
            shape = RoundedCornerShape(10.dp)
          ) {
            Text("Grant Permission", color = SoftCreamPaper)
          }
        },
        dismissButton = {
          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(
              onClick = {
                val target = slotToGrantPermission
                slotToGrantPermission = null
                selectedTranslatorForDetail = target
              }
            ) {
              Text("View Profile", color = AntiqueGold)
            }
            TextButton(
              onClick = { slotToGrantPermission = null }
            ) {
              Text("Cancel", color = CharcoalSecondary)
            }
          }
        },
        containerColor = SoftCreamPaper,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 6.dp
      )
    }

    // Owner / Author Room Upload Dialog
    if (uiState.isUploadDialogOpen) {
      OwnerUploadDialog(
        authorSlots = uiState.authorSlots,
        initialSlot = selectedUploadSlot,
        activeUser = activeUser,
        isOwner = isSoleOwner,
        onDismiss = { viewModel.closeUploadDialog() },
        onUpdateAuthorSlot = { slot, name, penName, bio ->
          viewModel.updateAuthorSlot(slot, name, penName, bio)
        },
        onPublishNovel = { title, subtitle, chapterTitle, excerpt, content, coverColor, author, authorSlot, coverImageUri, originalAuthor, novelStatus, releaseFormat, genre, onDone ->
          viewModel.uploadNovel(
            title = title,
            subtitle = subtitle,
            chapterTitle = chapterTitle,
            excerpt = excerpt,
            content = content,
            coverColorHex = coverColor,
            author = author,
            authorSlot = authorSlot,
            coverImageUri = coverImageUri,
            originalAuthor = originalAuthor,
            novelStatus = novelStatus,
            releaseFormat = releaseFormat,
            genre = genre,
            onResult = onDone
          )
        }
      )
    }

    // Global Cloud Publishing Hub Modal
    if (uiState.cloudPublishModalNovel != null) {
      CloudPublishModal(
        novel = uiState.cloudPublishModalNovel!!,
        novelJson = uiState.cloudPublishModalNovelJson ?: "",
        fullCatalogJson = uiState.cloudPublishModalCatalogJson ?: "[]",
        isAlreadyCloudPublished = uiState.isAlreadyCloudPublished,
        initialGitHubToken = viewModel.getGitHubToken(),
        initialWriteUrl = viewModel.getCloudWriteUrl(),
        onDismiss = { viewModel.dismissCloudPublishModal() },
        onPublishToCloud = { token, writeUrl, onDone ->
          viewModel.publishPendingNovelToCloud(token, writeUrl, onDone)
        },
        onUploadToFirebaseStorage = { onDone ->
          viewModel.uploadNovelToFirebaseStorage(onDone)
        }
      )
    }

    // Translator Editorial Studio (Editing title, subtitle, author, synopsis, content, cover, or deleting novel)
    if (novelToEdit != null) {
      EditNovelModal(
        novel = novelToEdit!!,
        onDismiss = { novelToEdit = null },
        onUpdateNovel = { novelId, title, subtitle, originalAuthor, synopsis, chapterTitle, contentText, coverColorHex, coverImageUri, novelStatus, releaseFormat ->
          viewModel.updateNovel(
            novelId = novelId,
            title = title,
            subtitle = subtitle,
            originalAuthor = originalAuthor,
            synopsis = synopsis,
            chapterTitle = chapterTitle,
            contentText = contentText,
            coverColorHex = coverColorHex,
            coverImageUri = coverImageUri,
            novelStatus = novelStatus,
            releaseFormat = releaseFormat
          )
        },
        onDeleteNovel = { novelId ->
          viewModel.deleteNovel(novelId)
          if (safeIndex >= novels.size - 1 && selectedIndex > 0) {
            selectedIndex--
          }
        }
      )
    }
  }
}

/**
 * Top Utility Bar with Reader Account Profile / Sign In & Owner Upload Studio
 */
@Composable
private fun TopUtilityBar(
  activeUser: ReaderProfileEntity?,
  isSoleOwner: Boolean = false,
  canUpload: Boolean = false,
  onOpenAuth: () -> Unit,
  onSignOut: () -> Unit,
  onOpenProfile: () -> Unit = {},
  onOpenAbout: () -> Unit = {},
  onOpenAuthorRooms: () -> Unit,
  onOpenUpload: () -> Unit,
  activeTranslatorsCount: Int = 4,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 8.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Left: Strawberrycandy Novel Archive Logo Lockup
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.testTag("app_brand_logo_lockup")
    ) {
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = SoftCreamPaper,
        border = BorderStroke(1.2.dp, AntiqueGold.copy(alpha = 0.75f)),
        shadowElevation = 2.dp,
        modifier = Modifier.size(36.dp)
      ) {
        Image(
          painter = painterResource(id = R.drawable.img_strawberrycandy_launcher),
          contentDescription = "Strawberrycandy Logo",
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      Column {
        Text(
          text = "Strawberrycandy",
          style = MaterialTheme.typography.titleMedium.copy(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            letterSpacing = 0.2.sp
          ),
          color = CharcoalText,
          maxLines = 1,
          modifier = Modifier.testTag("app_brand_title")
        )
        Text(
          text = "NOVEL STUDIO",
          style = MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 1.4.sp,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold
          ),
          color = AntiqueGold
        )
      }
    }

    // Right: Action Buttons + User Profile
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      if (activeUser != null) {
        // Translators & Owner Profiles Button (Visible only when signed in)
        Surface(
          onClick = onOpenAuthorRooms,
          shape = RoundedCornerShape(16.dp),
          color = SoftCreamPaper,
          border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.5f)),
          modifier = Modifier.testTag("top_utility_translators_button")
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
          ) {
            Icon(
              imageVector = Icons.Outlined.WorkspacePremium,
              contentDescription = "Translators & Owners",
              tint = AntiqueGold,
              modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
              text = "Translators",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 9.5.sp,
                color = CharcoalText
              )
            )
          }
        }

        // Profile Pill (Tapping opens Profile modal with reading history, account stats, pen name, and Sign Out)
        Surface(
          onClick = onOpenProfile,
          shape = RoundedCornerShape(16.dp),
          color = SoftCreamPaper,
          border = BorderStroke(1.dp, if (isSoleOwner) AntiqueGold.copy(alpha = 0.7f) else SubtleBorder),
          modifier = Modifier.testTag("reader_profile_pill")
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
          ) {
            Box(
              modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(if (activeUser.provider == "GOOGLE") Color(0xFF4285F4) else Color(0xFF1E1D1B)),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = if (activeUser.provider == "GOOGLE") "G" else "",
                color = Color.White,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
              )
            }
            Spacer(modifier = Modifier.width(4.dp))
            val safeDisplayName = activeUser.displayName
              .replace(Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"), "")
              .substringBefore("@")
              .trim()
              .ifBlank { if (activeUser.role == "TRANSLATOR") "Translator" else "Reader" }
            Text(
              text = safeDisplayName,
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Medium
              ),
              color = CharcoalText,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.widthIn(max = 65.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = AntiqueGold.copy(alpha = 0.15f),
              border = BorderStroke(0.5.dp, AntiqueGold.copy(alpha = 0.5f)),
              modifier = Modifier.testTag("user_points_indicator")
            ) {
              Text(
                text = "${activeUser.penNamePoints}pt",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 8.sp,
                  fontWeight = FontWeight.Bold
                ),
                color = AntiqueGold,
                modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
              )
            }
          }
        }
      } else {
        // Guest mode: About + Sign In (Translators room hidden when not signed in)
        Surface(
          onClick = onOpenAbout,
          shape = RoundedCornerShape(16.dp),
          color = SoftCreamPaper,
          border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.5f)),
          modifier = Modifier.testTag("top_utility_about_button")
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
          ) {
            Icon(
              imageVector = Icons.Outlined.Info,
              contentDescription = "About",
              tint = AntiqueGold,
              modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
              text = "About",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 9.5.sp,
                color = CharcoalText
              )
            )
          }
        }


      }
    }
  }
}

/**
 * Filter Chip for Curated Shelf, My Reading Library, and Favorites
 */
@Composable
private fun ShelfFilterChip(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
  modifier: Modifier = Modifier,
) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(16.dp),
    color = if (selected) CharcoalText else SoftCreamPaper,
    border = BorderStroke(1.dp, if (selected) CharcoalText else SubtleBorder),
    modifier = modifier.height(32.dp)
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      if (icon != null) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = if (selected) SoftCreamPaper else CharcoalSecondary,
          modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
      }
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
          fontSize = 11.sp
        ),
        color = if (selected) SoftCreamPaper else CharcoalSecondary
      )
    }
  }
}

/**
 * "Continue where you stopped reading" Banner
 * Pressing for 3 seconds opens the novel editor; tapping resumes reading immediately.
 */
@Composable
private fun ContinueReadingBanner(
  novel: NovelWithState,
  isTranslatorOrOwner: Boolean = false,
  onResumeReading: () -> Unit,
  onEditNovel: () -> Unit,
  onRemoveFromReading: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val haptic = LocalHapticFeedback.current
  val coroutineScope = rememberCoroutineScope()
  var holdProgress by remember { mutableFloatStateOf(0f) }

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = AntiqueGoldLight.copy(alpha = 0.45f)),
    border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.4f)),
    modifier = modifier
      .fillMaxWidth()
      .pointerInput(novel.id, isTranslatorOrOwner) {
        if (isTranslatorOrOwner) {
          detectTapGestures(
            onPress = {
              val startTime = System.currentTimeMillis()
              val totalMs = 2000L
              val interval = 40L
              val job = coroutineScope.launch {
                var elapsed = 0L
                while (elapsed < totalMs) {
                  kotlinx.coroutines.delay(interval)
                  elapsed += interval
                  holdProgress = (elapsed.toFloat() / totalMs).coerceIn(0f, 1f)
                  if (elapsed >= totalMs) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onEditNovel()
                    holdProgress = 0f
                    return@launch
                  }
                }
              }
              try {
                val released = tryAwaitRelease()
                if (released) {
                  val elapsed = System.currentTimeMillis() - startTime
                  if (elapsed < 500L) {
                    onResumeReading()
                  }
                }
              } finally {
                job.cancel()
                holdProgress = 0f
              }
            }
          )
        } else {
          detectTapGestures(
            onTap = { onResumeReading() }
          )
        }
      }
      .testTag("continue_reading_banner")
  ) {
    Box(modifier = Modifier.fillMaxWidth()) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          Icon(
            imageVector = Icons.Outlined.History,
            contentDescription = null,
            tint = AntiqueGold,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = "CONTINUE READING",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
              ),
              color = AntiqueGold
            )
            Text(
              text = novel.title,
              style = MaterialTheme.typography.titleSmall.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Medium
              ),
              color = CharcoalText,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = if (isTranslatorOrOwner) {
                "Page ${novel.currentPage} of ${novel.totalPages} • Progress saved • Hold 2s to Edit"
              } else {
                "Page ${novel.currentPage} of ${novel.totalPages} • Progress saved"
              },
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
              color = CharcoalSecondary
            )
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          modifier = Modifier.padding(start = 8.dp)
        ) {
          IconButton(
            onClick = onRemoveFromReading,
            modifier = Modifier.size(32.dp).testTag("delete_continue_reading_button")
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Remove from Continue Reading",
              tint = CharcoalSecondary,
              modifier = Modifier.size(16.dp)
            )
          }

          Surface(
            shape = RoundedCornerShape(14.dp),
            color = CharcoalText
          ) {
            Text(
              text = "Resume",
              style = MaterialTheme.typography.labelSmall.copy(
                color = SoftCreamPaper,
                fontWeight = FontWeight.SemiBold
              ),
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
          }
        }
      }

      // 2-Second Hold progress overlay
      if (isTranslatorOrOwner && holdProgress > 0f) {
        Box(
          modifier = Modifier
            .matchParentSize()
            .background(Color(0xEE1E1815))
            .padding(horizontal = 16.dp),
          contentAlignment = Alignment.CenterStart
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            CircularProgressIndicator(
              progress = { holdProgress },
              modifier = Modifier.size(22.dp),
              color = AntiqueGold,
              trackColor = AntiqueGold.copy(alpha = 0.25f),
              strokeWidth = 2.5.dp
            )
            val remainingSec = ((2000L - (holdProgress * 2000L).toLong() + 900L) / 1000L).coerceIn(1L, 2L)
            Text(
              text = "Holding to Edit Manuscript (${remainingSec}s)...",
              style = MaterialTheme.typography.labelMedium.copy(
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

/**
 * Individual Novel Card in the horizontal row
 * Hold for 3 seconds to open the translator edit/delete modal; tap to select or read.
 */
@Composable
private fun HorizontalNovelCard(
  novel: NovelWithState,
  index: Int,
  isFocused: Boolean,
  isTranslatorOrOwner: Boolean = false,
  onCardClick: () -> Unit,
  onFocusClick: () -> Unit,
  onToggleFavorite: () -> Unit,
  onEditNovel: () -> Unit,
  onMarkReading: () -> Unit = {},
  onMarkFinished: () -> Unit = {},
  onMarkToBeRead: () -> Unit = {},
) {
  val haptic = LocalHapticFeedback.current
  val coroutineScope = rememberCoroutineScope()
  var holdProgress by remember { mutableFloatStateOf(0f) }
  var isStatusMenuOpen by remember { mutableStateOf(false) }

  val scale by animateFloatAsState(targetValue = if (isFocused) 1.04f else 0.96f, label = "card_scale")
  val elevation by animateFloatAsState(targetValue = if (isFocused) 14f else 6f, label = "card_elevation")
  val borderColor by animateColorAsState(
    targetValue = if (isFocused) AntiqueGold else Color.Transparent,
    label = "border_color"
  )

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .width(118.dp)
      .graphicsLayer {
        scaleX = scale
        scaleY = scale
      }
      .pointerInput(novel.id, isTranslatorOrOwner) {
        if (isTranslatorOrOwner) {
          detectTapGestures(
            onPress = {
              onFocusClick()
              val startTime = System.currentTimeMillis()
              val totalMs = 2000L
              val interval = 40L
              val job = coroutineScope.launch {
                var elapsed = 0L
                while (elapsed < totalMs) {
                  kotlinx.coroutines.delay(interval)
                  elapsed += interval
                  holdProgress = (elapsed.toFloat() / totalMs).coerceIn(0f, 1f)
                  if (elapsed >= totalMs) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onEditNovel()
                    holdProgress = 0f
                    return@launch
                  }
                }
              }
              try {
                val released = tryAwaitRelease()
                if (released) {
                  val elapsed = System.currentTimeMillis() - startTime
                  if (elapsed < 500L) {
                    onCardClick()
                  }
                }
              } finally {
                job.cancel()
                holdProgress = 0f
              }
            }
          )
        } else {
          detectTapGestures(
            onTap = {
              onFocusClick()
              onCardClick()
            }
          )
        }
      }
      .testTag("novel_card_${novel.id}")
  ) {
    Card(
      shape = RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp, topEnd = 10.dp, bottomEnd = 10.dp),
      colors = CardDefaults.cardColors(containerColor = Color(novel.coverColorHex)),
      elevation = CardDefaults.cardElevation(defaultElevation = elevation.dp),
      border = if (isFocused) BorderStroke(1.5.dp, borderColor) else null,
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(0.68f)
        .shadow(
          elevation = elevation.dp,
          shape = RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp, topEnd = 10.dp, bottomEnd = 10.dp),
          ambientColor = Color(0x35000000),
          spotColor = Color(0x25000000)
        )
    ) {
      Box(modifier = Modifier.fillMaxSize()) {
        if (novel.coverImageUri != null) {
          AsyncImage(
            model = File(novel.coverImageUri!!).takeIf { it.exists() } ?: novel.coverImageUri,
            contentDescription = novel.title,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
          )
        } else if (novel.coverDrawableRes != 0) {
          Image(
            painter = painterResource(id = novel.coverDrawableRes),
            contentDescription = novel.title,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
          )
        } else {
          // Custom hardcover design for owner-uploaded novels
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            val emailRegex = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
            val cleanOriginalAuthor = novel.originalAuthor.replace(emailRegex, "").trim()
            val cleanAuthor = novel.author.replace(emailRegex, "").trim()
            val authorLabel = when {
              cleanOriginalAuthor.isNotBlank() -> cleanOriginalAuthor
              cleanAuthor.isNotBlank() -> cleanAuthor
              novel.authorSlot > 0 -> "Room ${novel.authorSlot}"
              else -> "Strawberrycandy"
            }
            val headerText = if (novel.authorSlot > 0) {
              "TRANSLATOR • ${authorLabel.uppercase()}"
            } else {
              authorLabel.uppercase()
            }
            Text(
              text = headerText,
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 7.sp,
                letterSpacing = 1.4.sp,
                fontWeight = FontWeight.Bold
              ),
              color = Color(0xD0E5C17D),
              textAlign = TextAlign.Center,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis
            )

            Text(
              text = novel.title,
              style = MaterialTheme.typography.titleSmall.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Medium,
                color = Color.White
              ),
              textAlign = TextAlign.Center,
              maxLines = 3,
              overflow = TextOverflow.Ellipsis
            )

            Text(
              text = if (novel.authorSlot > 0) "STRAWBERRYCANDY STUDIO" else "NOVEL STUDIO",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 6.5.sp,
                letterSpacing = 0.8.sp
              ),
              color = Color(0xB0FFFFFF)
            )
          }
        }

        // Spine highlight
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              brush = Brush.horizontalGradient(
                0.0f to Color(0x40000000),
                0.025f to Color(0x15000000),
                0.05f to Color(0x30FFFFFF),
                0.09f to Color(0x00000000)
              )
            )
        )

        // Reading Status Badge (Top-Left of card)
        val statusBadge = when {
          novel.isFinished -> "✓ Finished" to Color(0xDD1B5E20)
          novel.isReading -> "📖 Reading" to Color(0xDD795548)
          novel.isToBeRead -> "🔖 To Read" to Color(0xDD37474F)
          else -> null
        }
        if (statusBadge != null) {
          Surface(
            shape = RoundedCornerShape(bottomEnd = 6.dp),
            color = statusBadge.second,
            modifier = Modifier.align(Alignment.TopStart)
          ) {
            Text(
              text = statusBadge.first,
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.4.sp
              ),
              color = Color.White,
              modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
            )
          }
        } else if (novel.isNewRelease) {
          Surface(
            shape = RoundedCornerShape(bottomEnd = 6.dp),
            color = AntiqueGold,
            modifier = Modifier.align(Alignment.TopStart)
          ) {
            Text(
              text = "✨ NEW",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 7.5.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.4.sp
              ),
              color = DeepBurgundy,
              modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
            )
          }
        }

        // Favorite icon button (Top Right of card)
        Box(
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(4.dp)
            .size(26.dp)
            .clip(CircleShape)
            .background(Color(0x75000000))
            .clickable { onToggleFavorite() },
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = if (novel.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = "Favorite",
            tint = if (novel.isFavorite) Color(0xFFEF5350) else Color.White,
            modifier = Modifier.size(14.dp)
          )
        }

        // In Progress indicator bar at bottom
        if (novel.currentPage > 1) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(3.dp)
              .background(Color(0x50000000))
              .align(Alignment.BottomCenter)
          ) {
            Box(
              modifier = Modifier
                .fillMaxWidth(novel.progressFraction)
                .height(3.dp)
                .background(AntiqueGold)
            )
          }
        }

        // Editable Novel indicator badge for translators / owner
        if (isTranslatorOrOwner && holdProgress == 0f) {
          Surface(
            shape = RoundedCornerShape(topStart = 6.dp),
            color = AntiqueGold.copy(alpha = 0.92f),
            modifier = Modifier
              .align(Alignment.BottomEnd)
              .padding(bottom = if (novel.currentPage > 1) 3.dp else 0.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
              Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = "Hold 2s to edit",
                tint = CharcoalText,
                modifier = Modifier.size(8.dp)
              )
              Spacer(modifier = Modifier.width(2.dp))
              Text(
                text = "Hold 2s",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 7.sp,
                  fontWeight = FontWeight.Bold
                ),
                color = CharcoalText
              )
            }
          }
        }

        // 2-Second Hold Feedback Overlay for Translators and Owner
        if (isTranslatorOrOwner && holdProgress > 0f) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(Color(0xEE1E1815))
              .padding(8.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center
            ) {
              CircularProgressIndicator(
                progress = { holdProgress },
                modifier = Modifier.size(38.dp),
                color = AntiqueGold,
                trackColor = AntiqueGold.copy(alpha = 0.25f),
                strokeWidth = 3.5.dp
              )
              Spacer(modifier = Modifier.height(6.dp))
              val remainingSec = ((2000L - (holdProgress * 2000L).toLong() + 900L) / 1000L).coerceIn(1L, 2L)
              Text(
                text = "Hold ${remainingSec}s\nto Edit",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 9.5.sp,
                  fontWeight = FontWeight.Bold,
                  textAlign = TextAlign.Center,
                  lineHeight = 12.sp
                ),
                color = SoftCreamPaper
              )
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(4.dp))

    Text(
      text = novel.title,
      style = MaterialTheme.typography.labelMedium.copy(
        fontFamily = FontFamily.Serif,
        fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 14.sp
      ),
      color = if (isFocused) CharcoalText else CharcoalSecondary,
      textAlign = TextAlign.Center,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis
    )

    Spacer(modifier = Modifier.height(1.dp))

    val authorSubtitle = buildString {
      val emailRegex = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
      val cleanOriginalAuthor = novel.originalAuthor.replace(emailRegex, "").trim()
      val cleanAuthor = novel.author.replace(emailRegex, "").trim()
      if (cleanOriginalAuthor.isNotBlank()) {
        append("By ")
        append(cleanOriginalAuthor)
      } else if (novel.authorSlot > 0) {
        val penName = cleanAuthor.ifBlank { "Room ${novel.authorSlot}" }
        append(penName)
      } else {
        append("Strawberrycandy")
      }
      if (novel.currentPage > 1) {
        append(" • Pg. ")
        append(novel.currentPage)
      }
    }

    Text(
      text = authorSubtitle,
      style = MaterialTheme.typography.labelSmall.copy(
        fontSize = 9.sp,
        letterSpacing = 0.8.sp
      ),
      color = if (isFocused) AntiqueGold else CharcoalTertiary,
      textAlign = TextAlign.Center,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )

    // Subtle readership and favorites metrics
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      modifier = Modifier.padding(top = 2.dp)
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Outlined.Visibility,
          contentDescription = null,
          tint = CharcoalTertiary,
          modifier = Modifier.size(10.dp)
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
          text = formatStatCount(novel.readsCount),
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
          color = CharcoalTertiary
        )
      }
      Text(text = "•", fontSize = 8.sp, color = CharcoalTertiary.copy(alpha = 0.5f))
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Filled.Favorite,
          contentDescription = null,
          tint = if (novel.favoritesCount > 0) Color(0xFFEF5350) else CharcoalTertiary,
          modifier = Modifier.size(9.dp)
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
          text = formatStatCount(novel.favoritesCount),
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
          color = CharcoalTertiary
        )
      }
    }

    // Quick status selector pill
    Box(
      modifier = Modifier.padding(top = 4.dp),
      contentAlignment = Alignment.Center
    ) {
      Surface(
        onClick = { isStatusMenuOpen = true },
        shape = RoundedCornerShape(10.dp),
        color = when {
          novel.isFinished -> Color(0xFF2E7D32).copy(alpha = 0.12f)
          novel.isReading -> AntiqueGold.copy(alpha = 0.12f)
          novel.isToBeRead -> CharcoalSecondary.copy(alpha = 0.1f)
          else -> Color.Transparent
        },
        border = BorderStroke(
          0.8.dp,
          when {
            novel.isFinished -> Color(0xFF2E7D32).copy(alpha = 0.4f)
            novel.isReading -> AntiqueGold.copy(alpha = 0.45f)
            novel.isToBeRead -> CharcoalSecondary.copy(alpha = 0.35f)
            else -> SubtleBorder
          }
        ),
        modifier = Modifier.testTag("novel_status_selector_${novel.id}")
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
        ) {
          val (icon, labelText) = when {
            novel.isFinished -> Icons.Filled.CheckCircle to "Finished"
            novel.isReading -> Icons.Filled.MenuBook to "Reading"
            novel.isToBeRead -> Icons.Filled.Bookmark to "To Read"
            else -> Icons.Outlined.BookmarkAdd to "Set Status"
          }
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (novel.isFinished) Color(0xFF2E7D32) else AntiqueGold,
            modifier = Modifier.size(10.dp)
          )
          Spacer(modifier = Modifier.width(3.dp))
          Text(
            text = labelText,
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 8.5.sp,
              fontWeight = FontWeight.Medium
            ),
            color = if (novel.isFinished) Color(0xFF2E7D32) else CharcoalText
          )
        }
      }

      DropdownMenu(
        expanded = isStatusMenuOpen,
        onDismissRequest = { isStatusMenuOpen = false }
      ) {
        DropdownMenuItem(
          text = { Text("Currently Reading", fontSize = 11.sp) },
          leadingIcon = {
            Icon(Icons.Filled.MenuBook, contentDescription = null, tint = AntiqueGold, modifier = Modifier.size(14.dp))
          },
          onClick = {
            isStatusMenuOpen = false
            onMarkReading()
          }
        )
        DropdownMenuItem(
          text = { Text("Finished (+1 Point)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
          leadingIcon = {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(14.dp))
          },
          onClick = {
            isStatusMenuOpen = false
            onMarkFinished()
          }
        )
        DropdownMenuItem(
          text = { Text("To Be Read (TBR)", fontSize = 11.sp) },
          leadingIcon = {
            Icon(Icons.Filled.Bookmark, contentDescription = null, tint = CharcoalSecondary, modifier = Modifier.size(14.dp))
          },
          onClick = {
            isStatusMenuOpen = false
            onMarkToBeRead()
          }
        )
      }
    }
  }
}

@Composable
private fun SearchResultNovelCard(
  novel: NovelWithState,
  query: String,
  onSelect: () -> Unit,
  onToggleFavorite: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = SoftCreamPaper),
    border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.5f)),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = modifier
      .fillMaxWidth()
      .clickable { onSelect() }
      .testTag("search_result_novel_${novel.id}")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Cover Thumbnail
      Card(
        shape = RoundedCornerShape(topStart = 2.dp, bottomStart = 2.dp, topEnd = 6.dp, bottomEnd = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color(novel.coverColorHex)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
          .width(54.dp)
          .height(76.dp)
      ) {
        Box(modifier = Modifier.fillMaxSize()) {
          if (novel.coverImageUri != null) {
            AsyncImage(
              model = File(novel.coverImageUri!!).takeIf { it.exists() } ?: novel.coverImageUri,
              contentDescription = novel.title,
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize()
            )
          } else {
            Column(
              modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
              verticalArrangement = Arrangement.Center,
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(
                text = novel.title.take(12),
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 7.5.sp,
                  fontWeight = FontWeight.Bold
                ),
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.width(12.dp))

      // Novel Info & Match Details
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = novel.title,
            style = MaterialTheme.typography.titleSmall.copy(
              fontFamily = FontFamily.Serif,
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp
            ),
            color = CharcoalText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
          )
          if (novel.isNewRelease) {
            Spacer(modifier = Modifier.width(4.dp))
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = AntiqueGold.copy(alpha = 0.2f)
            ) {
              Text(
                text = "NEW",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 7.5.sp,
                  fontWeight = FontWeight.Bold
                ),
                color = AntiqueGold,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
          text = buildString {
            if (novel.author.isNotBlank()) append(novel.author) else append("Strawberrycandy")
            if (novel.chapterTitle.isNotBlank()) {
              append(" • ")
              append(novel.chapterTitle)
            }
          },
          style = MaterialTheme.typography.bodySmall.copy(
            fontSize = 10.5.sp,
            color = AntiqueGold
          ),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )

        if (novel.excerpt.isNotBlank()) {
          Spacer(modifier = Modifier.height(3.dp))
          Text(
            text = novel.excerpt,
            style = MaterialTheme.typography.bodySmall.copy(
              fontSize = 10.sp,
              color = CharcoalSecondary,
              fontStyle = FontStyle.Italic
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      Spacer(modifier = Modifier.width(8.dp))

      // Action Button
      Button(
        onClick = onSelect,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = CharcoalText),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
        modifier = Modifier.height(32.dp)
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Outlined.MenuBook,
          contentDescription = null,
          tint = AntiqueGold,
          modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = "Read",
          style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
          ),
          color = SoftCreamPaper
        )
      }
    }
  }
}

@Composable
private fun SelectedNovelSpotlight(
  novel: NovelWithState,
  canEditNovel: Boolean = false,
  authorSlots: List<AuthorSlotEntity> = emptyList(),
  onViewTranslator: ((AuthorSlotEntity) -> Unit)? = null,
  onReadNovel: () -> Unit,
  onEditNovel: (() -> Unit)? = null,
  onToggleFavorite: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = SoftCreamPaper),
    border = BorderStroke(1.dp, SubtleBorder),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = modifier.testTag("selected_novel_spotlight")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
      ) {
        // Book Cover Art with elegant border and shadow
        Box(
          modifier = Modifier
            .width(80.dp)
            .height(115.dp)
        ) {
          Card(
            shape = RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp, topEnd = 8.dp, bottomEnd = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(novel.coverColorHex)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.4f)),
            modifier = Modifier
              .fillMaxSize()
              .shadow(4.dp, RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp, topEnd = 8.dp, bottomEnd = 8.dp))
          ) {
            Box(modifier = Modifier.fillMaxSize()) {
              if (novel.coverImageUri != null) {
                AsyncImage(
                  model = File(novel.coverImageUri!!).takeIf { it.exists() } ?: novel.coverImageUri,
                  contentDescription = novel.title,
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxSize()
                )
              } else if (novel.coverDrawableRes != 0) {
                Image(
                  painter = painterResource(id = novel.coverDrawableRes),
                  contentDescription = novel.title,
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxSize()
                )
              } else {
                Box(
                  modifier = Modifier
                    .fillMaxSize()
                    .background(Color(novel.coverColorHex)),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = novel.title.take(18),
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontSize = 9.sp,
                      fontWeight = FontWeight.Bold,
                      fontFamily = FontFamily.Serif
                    ),
                    color = AntiqueGold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(6.dp)
                  )
                }
              }

              // Spine shadow overlay
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .background(
                    brush = Brush.horizontalGradient(
                      0.0f to Color(0x35000000),
                      0.03f to Color(0x10000000),
                      0.06f to Color(0x20FFFFFF),
                      0.10f to Color(0x00000000)
                    )
                  )
              )
            }
          }
        }

        // Right-side vertical New Release Ribbon
        if (novel.isNewRelease) {
          Spacer(modifier = Modifier.width(8.dp))
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = AntiqueGold,
            modifier = Modifier
              .width(22.dp)
              .height(115.dp)
          ) {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 6.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "✨ NEW RELEASE",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 7.5.sp,
                  fontWeight = FontWeight.ExtraBold,
                  letterSpacing = 1.sp,
                  color = DeepBurgundy
                ),
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                  .graphicsLayer(rotationZ = 90f)
            )
          }
        }
      }

        Spacer(modifier = Modifier.width(12.dp))

        // Novel Meta details
        Column(
          modifier = Modifier
            .weight(1f)
            .height(115.dp),
          verticalArrangement = Arrangement.SpaceBetween
        ) {
          Column {
            // Edition and genre tag
            val genreTag = if (novel.novel.genre.isNotBlank() && novel.novel.genre != "Curated Novel") {
              novel.novel.genre
            } else {
              when (novel.id) {
                "nov_crimson" -> "Fantasy Romance"
                "nov_celestial" -> "Astral Sci-Fi"
                "nov_whispering_pines" -> "Nordic Mystery"
                "nov_moonlight" -> "Historical Romance"
                "nov_schema" -> "Design Monograph"
                else -> "Romance"
              }
            }

            Column(
              verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = AntiqueGold.copy(alpha = 0.12f),
                  border = BorderStroke(0.6.dp, AntiqueGold.copy(alpha = 0.35f))
                ) {
                  Text(
                    text = genreTag.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontSize = 7.sp,
                      fontWeight = FontWeight.Bold,
                      letterSpacing = 0.5.sp
                    ),
                    color = AntiqueGold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
                  )
                }
                // Publication Status Badge: Finished vs Ongoing
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = if (novel.isCompletedNovel) Color(0xFF2E7D32).copy(alpha = 0.15f) else Color(0xFFD87D2A).copy(alpha = 0.15f),
                  border = BorderStroke(0.6.dp, if (novel.isCompletedNovel) Color(0xFF2E7D32).copy(alpha = 0.4f) else Color(0xFFD87D2A).copy(alpha = 0.4f))
                ) {
                  Text(
                    text = if (novel.isCompletedNovel) "✓ FINISHED" else "• ONGOING",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontSize = 7.sp,
                      fontWeight = FontWeight.Bold,
                      letterSpacing = 0.4.sp
                    ),
                    color = if (novel.isCompletedNovel) Color(0xFF2E7D32) else Color(0xFFD87D2A),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
                  )
                }
                if (novel.isR19 || genreTag.contains("R19", ignoreCase = true)) {
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFC62828).copy(alpha = 0.15f),
                    border = BorderStroke(0.6.dp, Color(0xFFC62828).copy(alpha = 0.4f))
                  ) {
                    Text(
                      text = "R19",
                      style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.4.sp
                      ),
                      color = Color(0xFFC62828),
                      modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
                    )
                  }
                }
              }

              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Text(
                  text = "${novel.totalPages} pages",
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                  color = CharcoalTertiary
                )
                if (novel.isNewRelease) {
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = AntiqueGold,
                  ) {
                    Text(
                      text = "✨ NEW",
                      style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 7.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = DeepBurgundy
                      ),
                      modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
                    )
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(3.dp))

            Text(
              text = novel.title,
              style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                lineHeight = 17.sp
              ),
              color = CharcoalText,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis
            )

            val authorDisplay = if (novel.originalAuthor.isNotBlank()) {
              "${novel.originalAuthor} • ${novel.author}"
            } else {
              novel.author
            }
            val targetSlot = if (novel.authorSlot > 0) {
              authorSlots.find { it.slotNumber == novel.authorSlot }
            } else {
              authorSlots.find { it.penName.equals(novel.author, ignoreCase = true) }
            } ?: authorSlots.find { it.slotNumber == 0 }

            val isTargetSlotPermitted = targetSlot?.slotNumber == 0 || targetSlot?.isPermissionGranted == true

            if (isTargetSlotPermitted) {
              Text(
                text = "By $authorDisplay • Profile →",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 8.5.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = AntiqueGold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                  .clip(RoundedCornerShape(4.dp))
                  .clickable(enabled = targetSlot != null && onViewTranslator != null) {
                    targetSlot?.let { onViewTranslator?.invoke(it) }
                  }
              )
            } else {
              Text(
                text = "By $authorDisplay",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 8.5.sp,
                  fontWeight = FontWeight.Normal,
                  color = CharcoalSecondary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }

          // Progress indicator if reader started it
          if (novel.inReadingList && novel.progressFraction > 0f) {
            Column(modifier = Modifier.testTag("continue_reading_banner")) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = "Reading Progress",
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                  color = CharcoalTertiary
                )
                Text(
                  text = "${(novel.progressFraction * 100).toInt()}%",
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                  color = AntiqueGold
                )
              }
              Spacer(modifier = Modifier.height(2.dp))
              LinearProgressIndicator(
                progress = { novel.progressFraction },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(3.dp)
                  .clip(RoundedCornerShape(1.5.dp)),
                color = AntiqueGold,
                trackColor = SubtleBorder
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Action Buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (canEditNovel && onEditNovel != null) {
          OutlinedButton(
            onClick = onEditNovel,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.6f)),
            colors = ButtonDefaults.outlinedButtonColors(
              containerColor = AntiqueGoldLight.copy(alpha = 0.3f),
              contentColor = CharcoalText
            ),
            modifier = Modifier
              .height(36.dp)
              .testTag("spotlight_edit_button")
          ) {
            Icon(
              imageVector = Icons.Outlined.Edit,
              contentDescription = "Edit Novel",
              tint = AntiqueGold,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Edit",
              style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
              )
            )
          }

          Spacer(modifier = Modifier.width(8.dp))
        }

        Button(
          onClick = onReadNovel,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = CharcoalText),
          modifier = Modifier
            .weight(1f)
            .height(36.dp)
            .testTag("spotlight_read_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Outlined.MenuBook,
            contentDescription = null,
            tint = AntiqueGold,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = novel.readButtonLabel,
            style = MaterialTheme.typography.labelMedium.copy(
              fontWeight = FontWeight.SemiBold,
              fontSize = 11.sp,
              letterSpacing = 0.4.sp
            ),
            color = SoftCreamPaper
          )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Favorite Button
        Surface(
          onClick = onToggleFavorite,
          shape = RoundedCornerShape(12.dp),
          color = if (novel.isFavorite) AntiqueGold.copy(alpha = 0.15f) else SoftCreamPaper,
          border = BorderStroke(1.dp, if (novel.isFavorite) AntiqueGold else SubtleBorder),
          modifier = Modifier
            .height(36.dp)
            .testTag("spotlight_favorite_button")
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp)
          ) {
            Icon(
              imageVector = if (novel.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
              contentDescription = "Favorite",
              tint = if (novel.isFavorite) AntiqueGold else CharcoalSecondary,
              modifier = Modifier.size(15.dp)
            )
            if (novel.favoritesCount > 0) {
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "${novel.favoritesCount}",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold
                ),
                color = if (novel.isFavorite) AntiqueGold else CharcoalSecondary
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CuratedCoverShowcase(
  novels: List<NovelWithState>,
  selectedNovelId: String,
  canEditSpecificNovel: (NovelWithState) -> Boolean = { false },
  onSelectNovel: (NovelWithState) -> Unit,
  onEditNovel: ((NovelWithState) -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 6.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.AutoMirrored.Outlined.MenuBook,
          contentDescription = null,
          tint = AntiqueGold,
          modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "CURATED COVER FLOW",
          style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
          ),
          color = CharcoalText
        )
      }
      Text(
        text = "${novels.size} Volumes • Swipe to Browse",
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
        color = AntiqueGold
      )
    }

    // Horizontal scrolling row of cover cards - ZERO VERTICAL SCROLLING
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
        .padding(vertical = 4.dp),
      horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
      verticalAlignment = Alignment.CenterVertically
    ) {
      novels.forEach { novel ->
        val canEdit = canEditSpecificNovel(novel)
        CuratedCoverGridCard(
          novel = novel,
          isSelected = novel.id == selectedNovelId,
          canEdit = canEdit,
          onClick = { onSelectNovel(novel) },
          onEdit = if (canEdit && onEditNovel != null) { { onEditNovel(novel) } } else null,
          modifier = Modifier.width(172.dp)
        )
      }
    }
  }
}

@Composable
private fun CuratedCoverGridCard(
  novel: NovelWithState,
  isSelected: Boolean,
  canEdit: Boolean = false,
  onClick: () -> Unit,
  onEdit: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val haptic = LocalHapticFeedback.current
  val coroutineScope = rememberCoroutineScope()
  var holdProgress by remember { mutableFloatStateOf(0f) }

  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = SoftCreamPaper),
    border = BorderStroke(
      width = if (isSelected) 1.5.dp else 0.8.dp,
      color = if (isSelected) AntiqueGold else SubtleBorder
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 2.dp),
    modifier = modifier
      .testTag("gallery_card_${novel.id}")
      .pointerInput(novel.id, canEdit) {
        if (canEdit && onEdit != null) {
          detectTapGestures(
            onPress = {
              val startTime = System.currentTimeMillis()
              val totalMs = 2000L
              val interval = 40L
              val job = coroutineScope.launch {
                var elapsed = 0L
                while (elapsed < totalMs) {
                  kotlinx.coroutines.delay(interval)
                  elapsed += interval
                  holdProgress = (elapsed.toFloat() / totalMs).coerceIn(0f, 1f)
                  if (elapsed >= totalMs) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onEdit()
                    holdProgress = 0f
                    return@launch
                  }
                }
              }
              try {
                val released = tryAwaitRelease()
                if (released) {
                  val elapsed = System.currentTimeMillis() - startTime
                  if (elapsed < 500L) {
                    onClick()
                  }
                }
              } finally {
                job.cancel()
                holdProgress = 0f
              }
            }
          )
        } else {
          detectTapGestures(onTap = { onClick() })
        }
      }
  ) {
    Box(modifier = Modifier.fillMaxWidth()) {
      Column(modifier = Modifier.fillMaxWidth()) {
        // Cover Image Box (aspectRatio ~0.75f)
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.75f)
            .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
            .background(Color(novel.coverColorHex))
        ) {
        if (novel.coverImageUri != null) {
          AsyncImage(
            model = File(novel.coverImageUri!!).takeIf { it.exists() } ?: novel.coverImageUri,
            contentDescription = novel.title,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
          )
        } else if (novel.coverDrawableRes != 0) {
          Image(
            painter = painterResource(id = novel.coverDrawableRes),
            contentDescription = novel.title,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
          )
        } else {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(Color(novel.coverColorHex)),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = novel.title,
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif
              ),
              color = AntiqueGold,
              textAlign = TextAlign.Center,
              modifier = Modifier.padding(10.dp)
            )
          }
        }

        // Reading status tag overlay (Top Left)
        val statusLabel = when {
          novel.isFinished -> "✓ Finished"
          novel.isReading -> "📖 Reading"
          novel.isToBeRead -> "🔖 To Read"
          else -> null
        }
        if (statusLabel != null) {
          Surface(
            shape = RoundedCornerShape(bottomEnd = 6.dp),
            color = if (novel.isFinished) Color(0xDD1B5E20) else Color(0xDD2A2825),
            modifier = Modifier.align(Alignment.TopStart)
          ) {
            Text(
              text = statusLabel,
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Bold
              ),
              color = Color.White,
              modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
            )
          }
        } else if (novel.isNewRelease) {
          Surface(
            shape = RoundedCornerShape(bottomEnd = 6.dp),
            color = AntiqueGold,
            modifier = Modifier.align(Alignment.TopStart)
          ) {
            Text(
              text = "✨ NEW",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 7.5.sp,
                fontWeight = FontWeight.ExtraBold
              ),
              color = DeepBurgundy,
              modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
            )
          }
        }

        // Bottom gradient overlay for title legibility
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .align(Alignment.BottomCenter)
            .background(
              brush = Brush.verticalGradient(
                listOf(Color.Transparent, Color(0xCC000000))
              )
            )
        )

        // Floating action or page count at bottom of cover
        Text(
          text = "${novel.totalPages} pgs",
          style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 7.5.sp,
            fontWeight = FontWeight.Medium
          ),
          color = Color.White.copy(alpha = 0.9f),
          modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(6.dp)
        )
      }

      // Title & metadata below cover
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(8.dp)
      ) {
        Text(
          text = novel.title,
          style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Serif,
            fontSize = 12.sp,
            lineHeight = 15.sp
          ),
          color = CharcoalText,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(1.dp))
        Text(
          text = novel.author,
          style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 9.sp
          ),
          color = AntiqueGold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }

    // 2-Second Hold Feedback Overlay for Translators and Owner
    if (canEdit && holdProgress > 0f) {
      Box(
        modifier = Modifier
          .matchParentSize()
          .background(Color(0xEE1E1815))
          .padding(8.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          CircularProgressIndicator(
            progress = { holdProgress },
            modifier = Modifier.size(36.dp),
            color = AntiqueGold,
            trackColor = AntiqueGold.copy(alpha = 0.25f),
            strokeWidth = 3.dp
          )
          Spacer(modifier = Modifier.height(6.dp))
          val remainingSec = ((2000L - (holdProgress * 2000L).toLong() + 900L) / 1000L).coerceIn(1L, 2L)
          Text(
            text = "Hold ${remainingSec}s\nto Edit",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 9.5.sp,
              fontWeight = FontWeight.Bold,
              textAlign = TextAlign.Center,
              lineHeight = 12.sp
            ),
            color = SoftCreamPaper
          )
        }
      }
    }
  }
}
}

@Composable
private fun NewReleasesShowcase(
  novels: List<NovelWithState>,
  authorSlots: List<AuthorSlotEntity> = emptyList(),
  onViewTranslator: ((AuthorSlotEntity) -> Unit)? = null,
  onSelectNovel: (NovelWithState) -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier.testTag("homepage_new_releases_section")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 4.dp, vertical = 4.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(7.dp)
            .clip(CircleShape)
            .background(AntiqueGold)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "NEW RELEASES",
          style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.3.sp,
            color = AntiqueGold
          )
        )
        Spacer(modifier = Modifier.width(6.dp))
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = AntiqueGold.copy(alpha = 0.15f),
          border = BorderStroke(0.6.dp, AntiqueGold.copy(alpha = 0.4f))
        ) {
          Text(
            text = "FRESH RELEASES",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 7.5.sp,
              fontWeight = FontWeight.ExtraBold,
              letterSpacing = 0.5.sp,
              color = DeepBurgundy
            ),
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
          )
        }
      }
      Text(
        text = "Recently Added",
        style = MaterialTheme.typography.labelSmall.copy(
          fontSize = 8.5.sp,
          color = CharcoalTertiary
        )
      )
    }

    Spacer(modifier = Modifier.height(6.dp))

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
        .padding(vertical = 4.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      novels.forEach { novel ->
        NewReleaseCard(
          novel = novel,
          authorSlots = authorSlots,
          onViewTranslator = onViewTranslator,
          onCardClick = { onSelectNovel(novel) }
        )
      }
    }
  }
}

@Composable
private fun NewReleaseCard(
  novel: NovelWithState,
  authorSlots: List<AuthorSlotEntity> = emptyList(),
  onViewTranslator: ((AuthorSlotEntity) -> Unit)? = null,
  onCardClick: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = SoftCreamPaper),
    border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.45f)),
    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    onClick = onCardClick,
    modifier = Modifier
      .width(135.dp)
      .testTag("new_release_card_${novel.id}")
  ) {
    Column(modifier = Modifier.padding(8.dp)) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(130.dp)
          .clip(RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp, topEnd = 8.dp, bottomEnd = 8.dp))
          .background(Color(novel.coverColorHex))
      ) {
        if (novel.coverImageUri != null) {
          AsyncImage(
            model = File(novel.coverImageUri!!).takeIf { it.exists() } ?: novel.coverImageUri,
            contentDescription = novel.title,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
          )
        } else if (novel.coverDrawableRes != 0) {
          Image(
            painter = painterResource(id = novel.coverDrawableRes),
            contentDescription = novel.title,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
          )
        } else {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
              text = novel.title.take(16),
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif
              ),
              color = AntiqueGold,
              textAlign = TextAlign.Center,
              modifier = Modifier.padding(6.dp)
            )
          }
        }

        // Spine highlight
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              brush = Brush.horizontalGradient(
                0.0f to Color(0x35000000),
                0.04f to Color(0x10000000),
                0.08f to Color(0x20FFFFFF),
                0.12f to Color(0x00000000)
              )
            )
        )

        // ✨ NEW Badge
        Surface(
          shape = RoundedCornerShape(bottomEnd = 6.dp),
          color = AntiqueGold,
          modifier = Modifier.align(Alignment.TopStart)
        ) {
          Text(
            text = "✨ NEW",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 7.sp,
              fontWeight = FontWeight.ExtraBold,
              letterSpacing = 0.4.sp,
              color = DeepBurgundy
            ),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = novel.title,
        style = MaterialTheme.typography.titleSmall.copy(
          fontSize = 11.5.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Serif
        ),
        color = CharcoalText,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )

      val authorText = when {
        novel.originalAuthor.isNotBlank() -> novel.originalAuthor
        novel.author.isNotBlank() -> novel.author
        novel.authorSlot > 0 -> "Translator Room ${novel.authorSlot}"
        else -> "Strawberrycandy"
      }
      val targetSlot = if (novel.authorSlot > 0) {
        authorSlots.find { it.slotNumber == novel.authorSlot }
      } else {
        authorSlots.find { it.penName.equals(novel.author, ignoreCase = true) }
      } ?: authorSlots.find { it.slotNumber == 0 }

      val isTargetSlotPermitted = targetSlot?.slotNumber == 0 || targetSlot?.isPermissionGranted == true

      Text(
        text = authorText,
        style = MaterialTheme.typography.labelSmall.copy(
          fontSize = 9.sp,
          color = if (isTargetSlotPermitted) AntiqueGold else CharcoalSecondary,
          fontWeight = FontWeight.Medium
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
          .clip(RoundedCornerShape(4.dp))
          .clickable(enabled = isTargetSlotPermitted && targetSlot != null && onViewTranslator != null) {
            targetSlot?.let { onViewTranslator?.invoke(it) }
          }
      )

      Spacer(modifier = Modifier.height(6.dp))

      Surface(
        shape = RoundedCornerShape(8.dp),
        color = CharcoalText,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = "Read Now →",
          style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            color = SoftCreamPaper
          ),
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(vertical = 4.dp)
        )
      }
    }
  }
}

@Composable
private fun GuestArchiveLockedView(
  rememberedAccounts: List<ReaderProfileEntity>,
  onOpenAuth: (String?) -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp, vertical = 20.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Surface(
      shape = RoundedCornerShape(22.dp),
      color = CardWarmWhite,
      border = BorderStroke(1.2.dp, AntiqueGold.copy(alpha = 0.45f)),
      shadowElevation = 2.dp,
      modifier = Modifier
        .fillMaxWidth()
        .testTag("guest_archive_locked_card")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Icon Crest
        Box(
          modifier = Modifier
            .size(54.dp)
            .clip(CircleShape)
            .background(AntiqueGold.copy(alpha = 0.12f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Outlined.MenuBook,
            contentDescription = "Archive Portal",
            tint = AntiqueGold,
            modifier = Modifier.size(28.dp)
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
          text = "STRAWBERRYCANDY STUDIO",
          style = MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 2.sp,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
          ),
          color = AntiqueGold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = "Sign In to Access Novels",
          style = MaterialTheme.typography.headlineSmall.copy(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
          ),
          color = CharcoalText,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = "To protect translated manuscripts and synchronize your reading progress, please sign in with your account. Returning readers and curators can sign in to resume reading.",
          style = MaterialTheme.typography.bodySmall.copy(
            fontSize = 12.sp,
            lineHeight = 17.sp
          ),
          color = CharcoalSecondary,
          textAlign = TextAlign.Center
        )

        // Saved accounts quick access
        if (rememberedAccounts.isNotEmpty()) {
          Spacer(modifier = Modifier.height(16.dp))
          HorizontalDivider(color = SubtleBorder.copy(alpha = 0.8f), thickness = 0.8.dp)
          Spacer(modifier = Modifier.height(12.dp))

          Text(
            text = "SAVED ACCOUNTS ON THIS DEVICE",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.8.sp
            ),
            color = CharcoalSecondary
          )

          Spacer(modifier = Modifier.height(8.dp))

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.Center
          ) {
            rememberedAccounts.forEach { acc ->
              Surface(
                onClick = { onOpenAuth(acc.email) },
                shape = RoundedCornerShape(14.dp),
                color = SoftCreamPaper,
                border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.5f)),
                modifier = Modifier
                  .padding(horizontal = 4.dp)
                  .testTag("guest_saved_account_${acc.email}")
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                  Box(
                    modifier = Modifier
                      .size(16.dp)
                      .clip(CircleShape)
                      .background(AntiqueGold),
                    contentAlignment = Alignment.Center
                  ) {
                    Text(
                      text = acc.avatarInitial.take(1),
                      color = Color.White,
                      fontSize = 9.sp,
                      fontWeight = FontWeight.Bold
                    )
                  }
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = acc.displayName.ifBlank { acc.email.substringBefore("@") },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = CharcoalText
                  )
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Large Sign In Button
        Button(
          onClick = { onOpenAuth(null) },
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(containerColor = AntiqueGold),
          elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("guest_sign_in_button")
        ) {
          Icon(
            imageVector = Icons.Outlined.Person,
            contentDescription = null,
            tint = SoftCreamPaper,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Sign In / Register Account",
            style = MaterialTheme.typography.labelLarge.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 13.5.sp
            ),
            color = SoftCreamPaper
          )
        }
      }
    }
  }
}

@Composable
private fun HomeHeroMasthead(
  novelCount: Int,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = SoftCreamPaper),
    border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.45f)),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 6.dp)
      .testTag("home_hero_masthead")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 14.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Decorative fleuron ribbon
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.padding(bottom = 4.dp)
      ) {
        Box(
          modifier = Modifier
            .width(22.dp)
            .height(1.dp)
            .background(AntiqueGold.copy(alpha = 0.5f))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "❦  PRIVATE LITERARY SALON  ❦",
          style = MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 1.8.sp,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
          ),
          color = AntiqueGold
        )
        Spacer(modifier = Modifier.width(6.dp))
        Box(
          modifier = Modifier
            .width(22.dp)
            .height(1.dp)
            .background(AntiqueGold.copy(alpha = 0.5f))
        )
      }

      Text(
        text = "Curated Web & Light Novels",
        style = MaterialTheme.typography.titleLarge.copy(
          fontFamily = FontFamily.Serif,
          fontWeight = FontWeight.Bold,
          fontSize = 19.sp,
          letterSpacing = 0.2.sp
        ),
        color = CharcoalText,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(2.dp))

      Text(
        text = "Quiet literary archive, handcrafted translations & chapter reflections",
        style = MaterialTheme.typography.bodySmall.copy(
          fontStyle = FontStyle.Italic,
          fontSize = 11.5.sp
        ),
        color = CharcoalText.copy(alpha = 0.7f),
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(10.dp))

      // Refined literary badge pills
      Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        LiteraryFeaturePill(text = "✦ Cloud Synchronized")
        LiteraryFeaturePill(text = "✦ Reader Discussions")
      }
    }
  }
}

@Composable
private fun LiteraryFeaturePill(text: String) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = Color.White.copy(alpha = 0.75f),
    border = BorderStroke(0.8.dp, AntiqueGold.copy(alpha = 0.35f))
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall.copy(
        fontSize = 9.5.sp,
        fontWeight = FontWeight.Medium
      ),
      color = AntiqueGold,
      modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
    )
  }
}

@Composable
private fun RecentlyReadSection(
  recentlyReadNovels: List<NovelWithState>,
  onSelectNovel: (NovelWithState) -> Unit,
  modifier: Modifier = Modifier
) {
  if (recentlyReadNovels.isNotEmpty()) {
    Column(
      modifier = modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp, vertical = 6.dp)
        .testTag("recently_read_section"),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(6.dp)
              .clip(CircleShape)
              .background(AntiqueGold)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "RECENTLY READ (${recentlyReadNovels.size})",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 9.5.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.2.sp,
              color = CharcoalTertiary
            )
          )
        }
      }

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        recentlyReadNovels.forEach { novel ->
          RecentlyReadCard(
            novel = novel,
            onClick = { onSelectNovel(novel) }
          )
        }
      }
    }
  }
}

@Composable
private fun RecentlyReadCard(
  novel: NovelWithState,
  onClick: () -> Unit
) {
  Card(
    onClick = onClick,
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = SoftCreamPaper),
    border = BorderStroke(1.dp, SubtleBorder),
    modifier = Modifier
      .width(180.dp)
      .height(72.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxSize()
        .padding(8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Box(
        modifier = Modifier
          .width(42.dp)
          .height(56.dp)
          .clip(RoundedCornerShape(6.dp))
          .background(Color(novel.coverColorHex))
      ) {
        if (novel.coverImageUri != null) {
          AsyncImage(
            model = File(novel.coverImageUri!!).takeIf { it.exists() } ?: novel.coverImageUri,
            contentDescription = novel.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
        } else if (novel.coverDrawableRes != 0) {
          Image(
            painter = painterResource(id = novel.coverDrawableRes),
            contentDescription = novel.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
        } else {
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = novel.title.take(3),
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
              color = AntiqueGold
            )
          }
        }
      }

      Column(
        modifier = Modifier
          .weight(1f)
          .fillMaxHeight(),
        verticalArrangement = Arrangement.Center
      ) {
        Text(
          text = novel.title,
          style = MaterialTheme.typography.titleSmall.copy(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            lineHeight = 14.sp
          ),
          color = CharcoalText,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = "Page ${novel.currentPage} of ${novel.totalPages}",
          style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 9.sp,
            color = CharcoalSecondary
          ),
          maxLines = 1
        )
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
          progress = { novel.progressFraction },
          modifier = Modifier
            .fillMaxWidth()
            .height(3.dp)
            .clip(RoundedCornerShape(1.5.dp)),
          color = AntiqueGold,
          trackColor = SubtleBorder
        )
      }
    }
  }
}

@Composable
private fun GenreFilterRow(
  genres: List<String>,
  selectedGenre: String,
  onSelectGenre: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .horizontalScroll(rememberScrollState())
      .padding(horizontal = 16.dp, vertical = 4.dp)
      .testTag("genre_filter_row"),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    genres.forEach { genre ->
      val isSelected = selectedGenre.equals(genre, ignoreCase = true)
      Surface(
        onClick = { onSelectGenre(genre) },
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) AntiqueGold else SoftCreamPaper,
        border = BorderStroke(1.dp, if (isSelected) AntiqueGold else SubtleBorder),
        modifier = Modifier.testTag("genre_chip_${genre.lowercase()}")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = genre,
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
              fontSize = 11.sp,
              color = if (isSelected) Color.White else CharcoalText
            )
          )
        }
      }
    }
  }
}


