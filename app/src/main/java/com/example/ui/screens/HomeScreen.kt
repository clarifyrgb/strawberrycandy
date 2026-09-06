package com.example.ui.screens

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.BookmarkAdded
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
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
import com.example.ui.components.AuthModal
import com.example.ui.components.AuthorRoomsModal
import com.example.ui.components.EditNovelModal
import com.example.ui.components.NovelSearchBar
import com.example.ui.components.OwnerUploadDialog
import com.example.ui.components.ReaderProfileModal
import com.example.ui.components.TranslatorProfileModal
import com.example.ui.theme.AntiqueGold
import com.example.ui.theme.AntiqueGoldLight
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
        authorName = "Translator $slotNum",
        penName = "Translator $slotNum",
        bio = "Contributing translator at Strawberrycandy Archive",
        avatarColorHex = 0xFF353C48,
        accessCode = "AUTH-ROOM-$slotNum",
        isClaimed = false,
        isPermissionGranted = slotNum <= 4
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
        onOpenAuthorRooms = {
          if (activeUser != null) {
            isAuthorRoomsModalOpen = true
          }
        },
        onOpenUpload = {
          selectedUploadSlot = activeUser?.authorSlot ?: 0
          viewModel.openUploadDialog()
        },
        activeTranslatorsCount = activeTranslators.size
      )

      // 2. Novel Search Bar for Readers and Translators
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
        }

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

      // Translator Collective Archive (strawberrycandy + Permitted Translators)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState())
          .padding(horizontal = 24.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // "All Translators" opens the Translator Archive with live active count
        FilterChip(
          selected = true,
          enabled = activeUser != null,
          onClick = {
            if (activeUser != null) {
              isAuthorRoomsModalOpen = true
            }
          },
          leadingIcon = {
            Icon(
              imageVector = if (activeUser != null) Icons.Outlined.WorkspacePremium else Icons.Outlined.Lock,
              contentDescription = if (activeUser != null) null else "Log in required",
              tint = if (activeUser != null) AntiqueGold else CharcoalSecondary.copy(alpha = 0.5f),
              modifier = Modifier.size(13.dp)
            )
          },
          label = {
            Text(
              if (activeUser != null) "All Translators (${activeTranslators.size} Active)" else "Curators (Log in to view)",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
            )
          },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = CharcoalText,
            selectedLabelColor = SoftCreamPaper,
            containerColor = SoftCreamPaper,
            labelColor = CharcoalSecondary,
            disabledContainerColor = SoftCreamPaper.copy(alpha = 0.5f),
            disabledLabelColor = CharcoalSecondary.copy(alpha = 0.5f)
          ),
          border = BorderStroke(1.dp, if (activeUser != null) CharcoalText else SubtleBorder.copy(alpha = 0.5f)),
          shape = RoundedCornerShape(12.dp)
        )

        val ownerSlot = uiState.authorSlots.find { it.slotNumber == 0 } ?: AuthorSlotEntity(
          slotNumber = 0,
          authorName = "strawberrycandy",
          penName = "strawberrycandy",
          bio = "Founder & Curator at Strawberrycandy Archive. Permanent editorial administrator.",
          avatarColorHex = 0xFF8C2D48,
          accessCode = "ARCHIVE-OWNER-0",
          isClaimed = true,
          isPermissionGranted = true
        )

        FilterChip(
          selected = false,
          enabled = activeUser != null,
          onClick = {
            if (activeUser != null) {
              selectedTranslatorForDetail = ownerSlot
            }
          },
          leadingIcon = {
            Icon(
              imageVector = if (activeUser != null) Icons.Outlined.WorkspacePremium else Icons.Outlined.Lock,
              contentDescription = if (activeUser != null) null else "Log in required",
              tint = if (activeUser != null) AntiqueGold else CharcoalSecondary.copy(alpha = 0.5f),
              modifier = Modifier.size(12.dp)
            )
          },
          label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                "strawberrycandy",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 11.sp,
                  fontWeight = FontWeight.SemiBold
                )
              )
              val founderCount = novels.count { it.authorSlot == 0 }
              if (founderCount > 0) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  "($founderCount)",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.5.sp,
                    color = if (activeUser != null) AntiqueGold else CharcoalSecondary.copy(alpha = 0.5f)
                  )
                )
              }
            }
          },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = SoftCreamPaper,
            selectedLabelColor = CharcoalText,
            containerColor = SoftCreamPaper,
            labelColor = CharcoalSecondary,
            disabledContainerColor = SoftCreamPaper.copy(alpha = 0.5f),
            disabledLabelColor = CharcoalSecondary.copy(alpha = 0.5f)
          ),
          border = BorderStroke(1.dp, if (activeUser != null) AntiqueGold.copy(alpha = 0.5f) else SubtleBorder.copy(alpha = 0.4f)),
          shape = RoundedCornerShape(12.dp)
        )

        // Active Curators (Permitted Translators)
        activeTranslators.forEach { slot ->
          val safePenName = slot.penName.replace(Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"), "").trim().ifBlank {
            "Translator ${slot.slotNumber}"
          }
          FilterChip(
            selected = false,
            enabled = activeUser != null,
            onClick = {
              if (activeUser != null) {
                selectedTranslatorForDetail = slot
              }
            },
            leadingIcon = {
              if (activeUser != null) {
                Box(
                  modifier = Modifier
                    .size(7.dp)
                    .background(Color(0xFF2E7D32), CircleShape)
                )
              } else {
                Icon(
                  imageVector = Icons.Outlined.Lock,
                  contentDescription = "Log in required",
                  tint = CharcoalSecondary.copy(alpha = 0.5f),
                  modifier = Modifier.size(11.dp)
                )
              }
            },
            label = {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  safePenName,
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                  )
                )
                val count = novels.count { it.authorSlot == slot.slotNumber }
                if (count > 0) {
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    "($count)",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontSize = 9.5.sp,
                      color = CharcoalTertiary
                    )
                  )
                }
              }
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = SoftCreamPaper,
              selectedLabelColor = CharcoalText,
              containerColor = SoftCreamPaper,
              labelColor = CharcoalSecondary,
              disabledContainerColor = SoftCreamPaper.copy(alpha = 0.5f),
              disabledLabelColor = CharcoalSecondary.copy(alpha = 0.5f)
            ),
            border = BorderStroke(1.dp, SubtleBorder),
            shape = RoundedCornerShape(12.dp)
          )
        }

        // Translators who can be granted permission in the future (Only visible to Archive Sole Owner)
        if (isSoleOwner && futureGrantableTranslators.isNotEmpty()) {
          Box(
            modifier = Modifier
              .padding(horizontal = 4.dp)
              .width(1.dp)
              .height(18.dp)
              .background(SubtleBorder)
          )

          futureGrantableTranslators.forEach { slot ->
            FilterChip(
              selected = false,
              onClick = { slotToGrantPermission = slot },
              leadingIcon = {
                Icon(
                  imageVector = Icons.Outlined.PersonAdd,
                  contentDescription = "Grant Permission",
                  tint = AntiqueGold,
                  modifier = Modifier.size(12.dp)
                )
              },
              label = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    slot.penName,
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Normal
                    ),
                    color = CharcoalText.copy(alpha = 0.88f)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    "(+Grant)",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontSize = 9.sp,
                      fontWeight = FontWeight.Bold,
                      color = AntiqueGold
                    )
                  )
                }
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = AntiqueGold.copy(alpha = 0.08f),
                selectedLabelColor = CharcoalText,
                containerColor = AntiqueGold.copy(alpha = 0.04f),
                labelColor = CharcoalSecondary
              ),
              border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.45f)),
              shape = RoundedCornerShape(12.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // 4. "Continue Where You Stopped Reading" Card (if active reader has reading progress)
      if (continueReadingNovel != null) {
        ContinueReadingBanner(
          novel = continueReadingNovel,
          isTranslatorOrOwner = isTranslatorOrOwner,
          onResumeReading = { onSelectNovel(continueReadingNovel) },
          onEditNovel = { novelToEdit = continueReadingNovel },
          modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
      }

      // 5. Bookshelf: Displaying book cover cards in an elegant horizontal row
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

              HorizontalNovelCard(
                novel = novel,
                index = index,
                isFocused = isFocused,
                isTranslatorOrOwner = isTranslatorOrOwner,
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
          SelectedNovelSpotlight(
            novel = focusedNovel,
            onReadNovel = { onSelectNovel(focusedNovel) },
            onToggleFavorite = { viewModel.toggleFavorite(focusedNovel.id) },
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 24.dp, vertical = 12.dp)
          )

          // 5.2. Curated Cover Gallery Showcase
          CuratedCoverShowcase(
            novels = novels,
            selectedNovelId = focusedNovel.id,
            onSelectNovel = { novel ->
              val idx = novels.indexOfFirst { it.id == novel.id }
              if (idx >= 0) selectedIndex = idx
              onSelectNovel(novel)
            },
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 24.dp, vertical = 10.dp)
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
            val titleText = when {
              uiState.novelSearchQuery.isNotBlank() -> "No Matching Novels"
              uiState.activeFilter == ShelfFilter.FAVORITES -> "No Favorites Yet"
              else -> "Reading Shelf Empty"
            }
            val subtitleText = when {
              uiState.novelSearchQuery.isNotBlank() -> "We couldn't find any novels matching '${uiState.novelSearchQuery}'. Try searching by author, genre, or title."
              uiState.activeFilter == ShelfFilter.FAVORITES -> "Tap the heart icon on any novel to save your favorites here."
              else -> "Start reading or tap any novel from the archive to add it to this shelf."
            }

            Text(
              text = titleText,
              style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif),
              color = CharcoalText
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = subtitleText,
              style = MaterialTheme.typography.bodySmall,
              color = CharcoalSecondary,
              textAlign = TextAlign.Center
            )

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
    }

    // Auth Modal Dialog
    if (uiState.isAuthDialogOpen) {
      AuthModal(
        onDismiss = { viewModel.closeAuthDialog() },
        onSignInWithGoogle = { email, password, name, role, authorSlot ->
          viewModel.signInWithGoogle(email = email, password = password, displayName = name, role = role, authorSlot = authorSlot)
        },
        onSignInWithApple = { email, password, name, role, authorSlot ->
          viewModel.signInWithApple(email = email, password = password, displayName = name, role = role, authorSlot = authorSlot)
        },
        externalErrorMessage = uiState.authErrorMessage,
        onClearError = { viewModel.clearAuthError() }
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
        onSignOut = { viewModel.signOut() }
      )
    }

    // Author Rooms / Translator Collective Archive Modal (5 Curators)
    if (isAuthorRoomsModalOpen && activeUser != null) {
      AuthorRoomsModal(
        authorSlots = uiState.authorSlots,
        novels = allNovelsList,
        currentUser = activeUser,
        onDismiss = { isAuthorRoomsModalOpen = false },
        onOpenUploadForSlot = if (canUploadNovel) { slot ->
          selectedUploadSlot = slot
          viewModel.openUploadDialog()
        } else { _ -> },
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

    // Dedicated Personal Archive Page for a Translator
    if (selectedTranslatorForDetail != null && activeUser != null) {
      val activeSlot = uiState.authorSlots.find { it.slotNumber == selectedTranslatorForDetail!!.slotNumber }
        ?: selectedTranslatorForDetail!!
      TranslatorProfileModal(
        slot = activeSlot,
        novels = allNovelsList,
        isOwner = isSoleOwner,
        isOwnerOrTranslator = isTranslatorOrOwner,
        onDismiss = { selectedTranslatorForDetail = null },
        onSelectNovel = { novel ->
          selectedTranslatorForDetail = null
          onSelectNovel(novel)
        },
        onUpdateCoverImage = { slotNum, imagePath ->
          if (isSoleOwner || (activeUser?.role == "TRANSLATOR" && activeUser.authorSlot == slotNum)) {
            viewModel.updateAuthorSlotCover(slotNum, imagePath)
            selectedTranslatorForDetail = activeSlot.copy(coverImageUri = imagePath)
          }
        },
        onOpenUploadForSlot = if (canUploadNovel) { slotNum ->
          selectedUploadSlot = slotNum
          viewModel.openUploadDialog()
        } else null,
        onToggleSlotPermission = if (isSoleOwner) { slotNum, isGranted ->
          viewModel.setSlotPermission(slotNum, isGranted)
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
            text = "Grant Permission to ${slot.penName}?",
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
        onDismiss = { viewModel.closeUploadDialog() },
        onUpdateAuthorSlot = { slot, name, penName, bio ->
          viewModel.updateAuthorSlot(slot, name, penName, bio)
        },
        onPublishNovel = { title, subtitle, chapterTitle, excerpt, content, coverColor, author, authorSlot, coverImageUri, originalAuthor, novelStatus, releaseFormat ->
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
            releaseFormat = releaseFormat
          )
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
  onOpenAuthorRooms: () -> Unit,
  onOpenUpload: () -> Unit,
  activeTranslatorsCount: Int = 4,
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 6.dp)
  ) {
    // Primary Header Row: Brand Title on Left, Profile + Sign Out on Right
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Left: Brand Title STRAWBERRYCANDY
      Column {
        Text(
          text = "STRAWBERRYCANDY",
          style = MaterialTheme.typography.titleMedium.copy(
            letterSpacing = 2.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Serif,
            fontSize = 15.sp
          ),
          color = CharcoalText,
          modifier = Modifier.testTag("app_brand_title")
        )
        Text(
          text = "NOVEL ARCHIVE",
          style = MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 1.sp,
            fontSize = 7.5.sp,
            fontWeight = FontWeight.Bold
          ),
          color = AntiqueGold
        )
      }

      // Right: User Profile + Sign Out (or Sign In)
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        if (activeUser != null) {
          // Profile Pill
          Surface(
            onClick = onOpenProfile,
            shape = RoundedCornerShape(16.dp),
            color = SoftCreamPaper,
            border = BorderStroke(1.dp, if (isSoleOwner) AntiqueGold.copy(alpha = 0.6f) else SubtleBorder),
            modifier = Modifier.testTag("reader_profile_pill")
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(18.dp)
                  .clip(CircleShape)
                  .background(if (activeUser.provider == "GOOGLE") Color(0xFF4285F4) else Color(0xFF1E1D1B)),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = if (activeUser.provider == "GOOGLE") "G" else "",
                  color = Color.White,
                  fontSize = 9.sp,
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
                text = safeDisplayName.take(10),
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Medium
                ),
                color = CharcoalText
              )
              Spacer(modifier = Modifier.width(4.dp))
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

          // Direct, Prominent Sign Out Button (Always visible!)
          Surface(
            onClick = onSignOut,
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFFDEDEC),
            border = BorderStroke(1.dp, Color(0xFFEF9A9A)),
            modifier = Modifier.testTag("top_utility_sign_out_button")
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
              Icon(
                imageVector = Icons.Outlined.Logout,
                contentDescription = "Sign Out",
                tint = Color(0xFFC62828),
                modifier = Modifier.size(13.dp)
              )
              Spacer(modifier = Modifier.width(3.dp))
              Text(
                text = "Sign Out",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 9.5.sp,
                  color = Color(0xFFC62828)
                )
              )
            }
          }
        } else {
          // Clean Sign In button
          Surface(
            onClick = onOpenAuth,
            shape = RoundedCornerShape(16.dp),
            color = SoftCreamPaper,
            border = BorderStroke(1.dp, AntiqueGold),
            modifier = Modifier.testTag("sign_in_prompt_button")
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
              Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = null,
                tint = AntiqueGold,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Sign In",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 11.sp
                ),
                color = CharcoalText
              )
            }
          }
        }
      }
    }

    // Secondary Row: Curators & Upload buttons (only when logged in)
    if (activeUser != null) {
      Spacer(modifier = Modifier.height(6.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Curators button
        Surface(
          onClick = onOpenAuthorRooms,
          shape = RoundedCornerShape(14.dp),
          color = SoftCreamPaper,
          border = BorderStroke(1.dp, SubtleBorder),
          modifier = Modifier.testTag("author_rooms_button")
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Icon(
              imageVector = Icons.Outlined.WorkspacePremium,
              contentDescription = "Curators",
              tint = AntiqueGold,
              modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              text = "$activeTranslatorsCount Translators",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 9.5.sp
              ),
              color = CharcoalText
            )
          }
        }

        if (canUpload) {
          Spacer(modifier = Modifier.width(6.dp))
          Surface(
            onClick = onOpenUpload,
            shape = RoundedCornerShape(14.dp),
            color = AntiqueGoldLight.copy(alpha = 0.7f),
            border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.6f)),
            modifier = Modifier.testTag("owner_upload_button")
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Icon(
                imageVector = Icons.Outlined.Upload,
                contentDescription = null,
                tint = AntiqueGold,
                modifier = Modifier.size(12.dp)
              )
              Spacer(modifier = Modifier.width(3.dp))
              Text(
                text = "Upload",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 9.5.sp
                ),
                color = AntiqueGold
              )
            }
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

        Surface(
          shape = RoundedCornerShape(14.dp),
          color = CharcoalText,
          modifier = Modifier.padding(start = 8.dp)
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

      // 3-Second Hold progress overlay
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
            val remainingSec = maxOf(1, (3.2f * (1f - holdProgress)).toInt())
            Text(
              text = "Holding to Edit Novel (${remainingSec}s)...",
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
              novel.authorSlot > 0 -> "Translator ${novel.authorSlot}"
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
              text = if (novel.authorSlot > 0) "STRAWBERRYCANDY ARCHIVE" else "NOVEL ARCHIVE",
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

        // 3-Second Hold Feedback Overlay for Translators and Owner
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
              val remainingSec = maxOf(1, (2.2f * (1f - holdProgress)).toInt())
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
      maxLines = 1,
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
        val penName = cleanAuthor.ifBlank { "Translator ${novel.authorSlot}" }
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
private fun SelectedNovelSpotlight(
  novel: NovelWithState,
  onReadNovel: () -> Unit,
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
        Card(
          shape = RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp, topEnd = 8.dp, bottomEnd = 8.dp),
          colors = CardDefaults.cardColors(containerColor = Color(novel.coverColorHex)),
          elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
          border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.4f)),
          modifier = Modifier
            .width(80.dp)
            .height(115.dp)
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
            val genreTag = when (novel.id) {
              "nov_crimson" -> "Fantasy Romance"
              "nov_celestial" -> "Astral Sci-Fi"
              "nov_whispering_pines" -> "Nordic Mystery"
              "nov_moonlight" -> "Historical Romance"
              "nov_schema" -> "Design Monograph"
              else -> "Curated Novel"
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = AntiqueGold.copy(alpha = 0.12f),
                border = BorderStroke(0.6.dp, AntiqueGold.copy(alpha = 0.35f))
              ) {
                Text(
                  text = genreTag.uppercase(),
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp
                  ),
                  color = AntiqueGold,
                  modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
              }
              Spacer(modifier = Modifier.width(5.dp))
              // Publication Status Badge: Finished vs Ongoing
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (novel.isCompletedNovel) Color(0xFF2E7D32).copy(alpha = 0.15f) else Color(0xFFD87D2A).copy(alpha = 0.15f),
                border = BorderStroke(0.6.dp, if (novel.isCompletedNovel) Color(0xFF2E7D32).copy(alpha = 0.4f) else Color(0xFFD87D2A).copy(alpha = 0.4f))
              ) {
                Text(
                  text = if (novel.isCompletedNovel) "✓ FINISHED" else "• ONGOING",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                  ),
                  color = if (novel.isCompletedNovel) Color(0xFF2E7D32) else Color(0xFFD87D2A),
                  modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
              }
              Spacer(modifier = Modifier.width(5.dp))
              Text(
                text = "${novel.totalPages} pages",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                color = CharcoalTertiary
              )
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
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )

            val authorDisplay = if (novel.originalAuthor.isNotBlank()) {
              "${novel.originalAuthor} • ${novel.author}"
            } else {
              novel.author
            }
            Text(
              text = "By $authorDisplay",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.5.sp,
                color = AntiqueGold
              ),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
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

      // Excerpt Quote
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = CreamBackground.copy(alpha = 0.6f),
        border = BorderStroke(0.6.dp, SubtleBorder.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = "“${novel.excerpt}”",
          style = MaterialTheme.typography.bodySmall.copy(
            fontFamily = FontFamily.Serif,
            fontStyle = FontStyle.Italic,
            fontSize = 11.sp,
            lineHeight = 14.sp
          ),
          color = CharcoalSecondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
        )
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Action Buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
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
  onSelectNovel: (NovelWithState) -> Unit,
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
        CuratedCoverGridCard(
          novel = novel,
          isSelected = novel.id == selectedNovelId,
          onClick = { onSelectNovel(novel) },
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
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    onClick = onClick,
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = SoftCreamPaper),
    border = BorderStroke(
      width = if (isSelected) 1.5.dp else 0.8.dp,
      color = if (isSelected) AntiqueGold else SubtleBorder
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 2.dp),
    modifier = modifier.testTag("gallery_card_${novel.id}")
  ) {
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
          maxLines = 1,
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
  }
}
