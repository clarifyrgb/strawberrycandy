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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.BookmarkAdded
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
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
import com.example.ui.components.OwnerUploadDialog
import com.example.ui.components.TranslatorProfileModal
import com.example.ui.theme.AntiqueGold
import com.example.ui.theme.AntiqueGoldLight
import com.example.ui.theme.CharcoalSecondary
import com.example.ui.theme.CharcoalTertiary
import com.example.ui.theme.CharcoalText
import com.example.ui.theme.CreamBackground
import com.example.ui.theme.SoftCreamPaper
import com.example.ui.theme.SubtleBorder
import com.example.viewmodel.ShelfFilter
import com.example.viewmodel.StrawberrycandyViewModel
import com.example.util.formatStatCount
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
  val novels = uiState.novels
  val activeUser = uiState.activeUser
  var selectedIndex by remember { mutableIntStateOf(0) }
  var isAuthorRoomsModalOpen by remember { mutableStateOf(false) }
  var selectedUploadSlot by remember { mutableIntStateOf(0) }
  var selectedTranslatorForDetail by remember { mutableStateOf<AuthorSlotEntity?>(null) }
  var slotToGrantPermission by remember { mutableStateOf<AuthorSlotEntity?>(null) }

  val context = LocalContext.current
  var coverPickerTargetNovelId by remember { mutableStateOf<String?>(null) }
  val singleNovelCoverPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    val targetId = coverPickerTargetNovelId
    if (uri != null && targetId != null) {
      try {
        val coversDir = File(context.filesDir, "covers").apply { mkdirs() }
        val destFile = File(coversDir, "cover_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
          FileOutputStream(destFile).use { output ->
            input.copyTo(output)
          }
        }
        viewModel.updateNovelCover(targetId, destFile.absolutePath)
      } catch (e: Exception) {
        viewModel.updateNovelCover(targetId, uri.toString())
      }
    }
    coverPickerTargetNovelId = null
  }

  val safeIndex = if (novels.isNotEmpty()) selectedIndex.coerceIn(0, novels.size - 1) else 0
  val selectedNovel = novels.getOrNull(safeIndex)

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
        onOpenAuth = { viewModel.openAuthDialog() },
        onSignOut = { viewModel.signOut() },
        onOpenAuthorRooms = { isAuthorRoomsModalOpen = true },
        onOpenUpload = {
          selectedUploadSlot = 0
          viewModel.openUploadDialog()
        },
        activeTranslatorsCount = activeTranslators.size
      )

      // 2. Main Brand Header: Strawberrycandy
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 12.dp, bottom = 16.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = "STRAWBERRYCANDY",
          style = MaterialTheme.typography.displayMedium.copy(
            letterSpacing = 3.sp,
            fontWeight = FontWeight.Light,
            fontFamily = FontFamily.Serif
          ),
          color = CharcoalText,
          textAlign = TextAlign.Center,
          modifier = Modifier.testTag("app_brand_title")
        )
      }

      // 3. Reader Filter Chips: All Novels, My Library, Favorites
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 24.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
      ) {
        ShelfFilterChip(
          label = "Curated Shelf",
          selected = uiState.activeFilter == ShelfFilter.ALL,
          onClick = { viewModel.setFilter(ShelfFilter.ALL) }
        )
        Spacer(modifier = Modifier.width(8.dp))
        ShelfFilterChip(
          label = "My Reading (${novels.count { it.inReadingList }})",
          selected = uiState.activeFilter == ShelfFilter.MY_LIBRARY,
          onClick = {
            viewModel.setFilter(ShelfFilter.MY_LIBRARY)
          }
        )
        Spacer(modifier = Modifier.width(8.dp))
        ShelfFilterChip(
          label = "Favorites (${novels.count { it.isFavorite }})",
          selected = uiState.activeFilter == ShelfFilter.FAVORITES,
          onClick = {
            viewModel.setFilter(ShelfFilter.FAVORITES)
          }
        )
      }

      // 3.5. Dynamic Active Curator Status Header
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "CURATORS (${activeTranslators.size + 1} ACTIVE)",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 9.5.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.2.sp
            ),
            color = CharcoalTertiary
          )
          Spacer(modifier = Modifier.width(6.dp))
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF2E7D32).copy(alpha = 0.12f),
            border = BorderStroke(0.8.dp, Color(0xFF2E7D32).copy(alpha = 0.35f))
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(5.dp)
                  .background(Color(0xFF2E7D32), CircleShape)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "${activeTranslators.size} Active Translators",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 8.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF2E7D32)
                )
              )
            }
          }
        }

        if (futureGrantableTranslators.isNotEmpty()) {
          Text(
            text = "${futureGrantableTranslators.size} grantable",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 9.sp,
              color = AntiqueGold,
              fontWeight = FontWeight.Medium
            )
          )
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
          onClick = { isAuthorRoomsModalOpen = true },
          leadingIcon = {
            Icon(
              imageVector = Icons.Outlined.WorkspacePremium,
              contentDescription = null,
              tint = AntiqueGold,
              modifier = Modifier.size(13.dp)
            )
          },
          label = {
            Text(
              "All Translators (${activeTranslators.size} Active)",
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
            labelColor = CharcoalSecondary
          ),
          border = BorderStroke(1.dp, CharcoalText),
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
          onClick = { selectedTranslatorForDetail = ownerSlot },
          leadingIcon = {
            Icon(
              imageVector = Icons.Outlined.WorkspacePremium,
              contentDescription = null,
              tint = AntiqueGold,
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
                    color = AntiqueGold
                  )
                )
              }
            }
          },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = SoftCreamPaper,
            selectedLabelColor = CharcoalText,
            containerColor = SoftCreamPaper,
            labelColor = CharcoalSecondary
          ),
          border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.5f)),
          shape = RoundedCornerShape(12.dp)
        )

        // Active Curators (Permitted Translators)
        activeTranslators.forEach { slot ->
          FilterChip(
            selected = false,
            onClick = { selectedTranslatorForDetail = slot },
            leadingIcon = {
              Box(
                modifier = Modifier
                  .size(7.dp)
                  .background(Color(0xFF2E7D32), CircleShape)
              )
            },
            label = {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  slot.penName,
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
              labelColor = CharcoalSecondary
            ),
            border = BorderStroke(1.dp, SubtleBorder),
            shape = RoundedCornerShape(12.dp)
          )
        }

        // Translators who can be granted permission in the future
        if (futureGrantableTranslators.isNotEmpty()) {
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
          onResumeReading = { onSelectNovel(continueReadingNovel) },
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
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom
          ) {
            novels.forEachIndexed { index, novel ->
              val isFocused = safeIndex == index

              HorizontalNovelCard(
                novel = novel,
                index = index,
                isFocused = isFocused,
                onCardClick = {
                  selectedIndex = index
                  onSelectNovel(novel)
                },
                onFocusClick = {
                  selectedIndex = index
                },
                onToggleFavorite = {
                  viewModel.toggleFavorite(novel.id)
                }
              )

              if (index < novels.size - 1) {
                Spacer(modifier = Modifier.width(16.dp))
              }
            }
          }

          // Minimalist architectural shelf line beneath the row
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 24.dp)
              .height(2.dp)
              .background(
                brush = Brush.horizontalGradient(
                  listOf(
                    Color.Transparent,
                    AntiqueGold.copy(alpha = 0.35f),
                    AntiqueGold.copy(alpha = 0.35f),
                    Color.Transparent
                  )
                )
              )
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
            Text(
              text = if (uiState.activeFilter == ShelfFilter.FAVORITES) "No Favorites Yet" else "Reading Shelf Empty",
              style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif),
              color = CharcoalText
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Tap the heart icon or start reading any novel to add it here.",
              style = MaterialTheme.typography.bodySmall,
              color = CharcoalSecondary,
              textAlign = TextAlign.Center
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // 6. Selected Novel Details Card
      if (selectedNovel != null) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = SoftCreamPaper),
            border = BorderStroke(1.dp, SubtleBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("focused_book_card")
          ) {
            Column(
              modifier = Modifier.padding(20.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                val authorCredit = if (selectedNovel.authorSlot > 0) {
                  "BY ${selectedNovel.author.uppercase()} (ROOM ${selectedNovel.authorSlot}) • ${selectedNovel.editionNumber}"
                } else {
                  "BY STRAWBERRYCANDY • ${selectedNovel.editionNumber}"
                }
                Text(
                  text = authorCredit,
                  style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.4.sp,
                    fontWeight = FontWeight.SemiBold
                  ),
                  color = AntiqueGold
                )

                IconButton(
                  onClick = { viewModel.toggleFavorite(selectedNovel.id) },
                  modifier = Modifier.size(28.dp)
                ) {
                  Icon(
                    imageVector = if (selectedNovel.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (selectedNovel.isFavorite) Color(0xFFC74350) else CharcoalTertiary,
                    modifier = Modifier.size(18.dp)
                  )
                }
              }

              Spacer(modifier = Modifier.height(6.dp))

              Text(
                text = selectedNovel.title,
                style = MaterialTheme.typography.headlineMedium.copy(
                  fontFamily = FontFamily.Serif,
                  fontWeight = FontWeight.Normal
                ),
                color = CharcoalText,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("active_book_title")
              )

              Spacer(modifier = Modifier.height(4.dp))

              Text(
                text = selectedNovel.subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                  fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                ),
                color = CharcoalSecondary,
                textAlign = TextAlign.Center
              )

              Spacer(modifier = Modifier.height(10.dp))

              // Reads and Favorites Metrics Row
              Row(
                modifier = Modifier
                  .clip(RoundedCornerShape(16.dp))
                  .background(SoftCreamPaper)
                  .border(1.dp, SubtleBorder, RoundedCornerShape(16.dp))
                  .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
              ) {
                // Reads Count
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Outlined.Visibility,
                    contentDescription = null,
                    tint = AntiqueGold,
                    modifier = Modifier.size(13.dp)
                  )
                  Spacer(modifier = Modifier.width(5.dp))
                  Text(
                    text = "${formatStatCount(selectedNovel.readsCount)} Readers",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = FontWeight.SemiBold,
                      fontSize = 11.sp
                    ),
                    color = CharcoalText
                  )
                }

                Box(
                  modifier = Modifier
                    .width(1.dp)
                    .height(11.dp)
                    .background(SubtleBorder)
                )

                // Favorites Count
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.clickable { viewModel.toggleFavorite(selectedNovel.id) }
                ) {
                  Icon(
                    imageVector = if (selectedNovel.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (selectedNovel.isFavorite) Color(0xFFEF5350) else AntiqueGold,
                    modifier = Modifier.size(13.dp)
                  )
                  Spacer(modifier = Modifier.width(5.dp))
                  Text(
                    text = "${formatStatCount(selectedNovel.favoritesCount)} Favorited",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = FontWeight.SemiBold,
                      fontSize = 11.sp
                    ),
                    color = CharcoalText
                  )
                }

                if (selectedNovel.storyPhotos.isNotEmpty()) {
                  Box(
                    modifier = Modifier
                      .width(1.dp)
                      .height(11.dp)
                      .background(SubtleBorder)
                  )

                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = Icons.Outlined.PhotoLibrary,
                      contentDescription = null,
                      tint = AntiqueGold,
                      modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = "${selectedNovel.storyPhotos.size} Photos",
                      style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                      ),
                      color = CharcoalSecondary
                    )
                  }
                }
              }

              Spacer(modifier = Modifier.height(12.dp))

              Text(
                text = "“${selectedNovel.excerpt}”",
                style = MaterialTheme.typography.bodyMedium.copy(
                  fontFamily = FontFamily.Serif,
                  lineHeight = 22.sp
                ),
                color = CharcoalText.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
              )

              Spacer(modifier = Modifier.height(18.dp))

              // Read / Continue button and Translator Cover Upload
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Surface(
                  onClick = {
                    viewModel.recordNovelRead(selectedNovel.id)
                    onSelectNovel(selectedNovel)
                  },
                  shape = RoundedCornerShape(24.dp),
                  color = CharcoalText,
                  modifier = Modifier.testTag("open_book_button")
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                  ) {
                    Icon(
                      imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                      contentDescription = null,
                      tint = SoftCreamPaper,
                      modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = if (selectedNovel.currentPage > 1) "Continue (${selectedNovel.currentPage})" else "Read Novel",
                      style = MaterialTheme.typography.labelLarge.copy(
                        letterSpacing = 0.8.sp
                      ),
                      color = SoftCreamPaper
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                      imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                      contentDescription = null,
                      tint = AntiqueGold,
                      modifier = Modifier.size(13.dp)
                    )
                  }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Upload / Change Cover Button for Room Translators & Owner
                OutlinedButton(
                  onClick = {
                    coverPickerTargetNovelId = selectedNovel.id
                    singleNovelCoverPickerLauncher.launch(
                      PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                  },
                  shape = RoundedCornerShape(24.dp),
                  colors = ButtonDefaults.outlinedButtonColors(contentColor = CharcoalText),
                  border = BorderStroke(1.dp, SubtleBorder),
                  contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp),
                  modifier = Modifier.testTag("upload_cover_button_shelf")
                ) {
                  Icon(
                    imageVector = Icons.Outlined.AddPhotoAlternate,
                    contentDescription = null,
                    tint = AntiqueGold,
                    modifier = Modifier.size(15.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = if (selectedNovel.coverImageUri != null) "Change Cover" else "Upload Cover",
                    style = MaterialTheme.typography.labelMedium.copy(
                      fontWeight = FontWeight.Medium,
                      letterSpacing = 0.4.sp
                    ),
                    color = CharcoalText
                  )
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      // 7. Provenance security footer
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.padding(horizontal = 24.dp)
      ) {
        Icon(
          imageVector = Icons.Outlined.Lock,
          contentDescription = null,
          tint = CharcoalTertiary,
          modifier = Modifier.size(12.dp)
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
        onSignInWithGoogle = { email, name ->
          viewModel.signInWithGoogle(email, name)
        },
        onSignInWithApple = { email, name ->
          viewModel.signInWithApple(email, name)
        }
      )
    }

    // Author Rooms / Translator Collective Archive Modal (5 Curators)
    if (isAuthorRoomsModalOpen) {
      AuthorRoomsModal(
        authorSlots = uiState.authorSlots,
        novels = allNovelsList,
        onDismiss = { isAuthorRoomsModalOpen = false },
        onOpenUploadForSlot = { slot ->
          selectedUploadSlot = slot
          viewModel.openUploadDialog()
        },
        onUpdateSlot = { slot, name, penName, bio ->
          viewModel.updateAuthorSlot(slot, name, penName, bio)
        },
        onUpdateSlotCover = { slot, imagePath ->
          viewModel.updateAuthorSlotCover(slot, imagePath)
        },
        onToggleSlotPermission = { slot, isGranted ->
          viewModel.setSlotPermission(slot, isGranted)
        },
        onViewTranslatorArchive = { slot ->
          selectedTranslatorForDetail = slot
        }
      )
    }

    // Dedicated Personal Archive Page for a Translator
    if (selectedTranslatorForDetail != null) {
      val activeSlot = uiState.authorSlots.find { it.slotNumber == selectedTranslatorForDetail!!.slotNumber }
        ?: selectedTranslatorForDetail!!
      TranslatorProfileModal(
        slot = activeSlot,
        novels = allNovelsList,
        isOwnerOrTranslator = true,
        onDismiss = { selectedTranslatorForDetail = null },
        onSelectNovel = { novel ->
          selectedTranslatorForDetail = null
          onSelectNovel(novel)
        },
        onUpdateCoverImage = { slotNum, imagePath ->
          viewModel.updateAuthorSlotCover(slotNum, imagePath)
          selectedTranslatorForDetail = activeSlot.copy(coverImageUri = imagePath)
        },
        onOpenUploadForSlot = { slotNum ->
          selectedUploadSlot = slotNum
          viewModel.openUploadDialog()
        },
        onToggleSlotPermission = { slotNum, isGranted ->
          viewModel.setSlotPermission(slotNum, isGranted)
        }
      )
    }

    // Quick Permission Grant Dialog for Future Translator Slots
    if (slotToGrantPermission != null) {
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
        onPublishNovel = { title, subtitle, chapterTitle, excerpt, content, coverColor, author, authorSlot, coverImageUri, originalAuthor ->
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
            originalAuthor = originalAuthor
          )
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
  onOpenAuth: () -> Unit,
  onSignOut: () -> Unit,
  onOpenAuthorRooms: () -> Unit,
  onOpenUpload: () -> Unit,
  activeTranslatorsCount: Int = 4,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp, vertical = 10.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Reader Profile / Sign-in Pill
    if (activeUser != null) {
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = SoftCreamPaper,
        border = BorderStroke(1.dp, SubtleBorder),
        modifier = Modifier.testTag("reader_profile_pill")
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(start = 6.dp, end = 10.dp, top = 4.dp, bottom = 4.dp)
        ) {
          // Provider badge circle
          Box(
            modifier = Modifier
              .size(22.dp)
              .clip(CircleShape)
              .background(if (activeUser.provider == "GOOGLE") Color(0xFF4285F4) else Color(0xFF1E1D1B)),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = if (activeUser.provider == "GOOGLE") "G" else "",
              color = Color.White,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }

          Spacer(modifier = Modifier.width(6.dp))

          Text(
            text = activeUser.displayName.take(14),
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Medium
            ),
            color = CharcoalText
          )

          Spacer(modifier = Modifier.width(6.dp))

          IconButton(
            onClick = onSignOut,
            modifier = Modifier.size(20.dp)
          ) {
            Icon(
              imageVector = Icons.Outlined.Logout,
              contentDescription = "Sign Out",
              tint = CharcoalSecondary,
              modifier = Modifier.size(13.dp)
            )
          }
        }
      }
    } else {
      Surface(
        onClick = onOpenAuth,
        shape = RoundedCornerShape(20.dp),
        color = SoftCreamPaper,
        border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.5f)),
        modifier = Modifier.testTag("sign_in_prompt_button")
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
          Icon(
            imageVector = Icons.Outlined.Person,
            contentDescription = null,
            tint = AntiqueGold,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Sign In",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.SemiBold,
              letterSpacing = 0.4.sp
            ),
            color = CharcoalText
          )
        }
      }
    }

    // Right action buttons: 4 Author Rooms & Upload
    Row(verticalAlignment = Alignment.CenterVertically) {
      Surface(
        onClick = onOpenAuthorRooms,
        shape = RoundedCornerShape(20.dp),
        color = SoftCreamPaper,
        border = BorderStroke(1.dp, SubtleBorder),
        modifier = Modifier.testTag("author_rooms_button")
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
          Icon(
            imageVector = Icons.Outlined.WorkspacePremium,
            contentDescription = null,
            tint = AntiqueGold,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(5.dp))
          Text(
            text = "$activeTranslatorsCount Translators",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.SemiBold,
              letterSpacing = 0.4.sp
            ),
            color = CharcoalText
          )
        }
      }

      Spacer(modifier = Modifier.width(8.dp))

      Surface(
        onClick = onOpenUpload,
        shape = RoundedCornerShape(20.dp),
        color = AntiqueGoldLight.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.5f)),
        modifier = Modifier.testTag("owner_upload_button")
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
          Icon(
            imageVector = Icons.Outlined.Upload,
            contentDescription = null,
            tint = AntiqueGold,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(5.dp))
          Text(
            text = "Upload",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.6.sp
            ),
            color = AntiqueGold
          )
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
) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(16.dp),
    color = if (selected) CharcoalText else SoftCreamPaper,
    border = BorderStroke(1.dp, if (selected) CharcoalText else SubtleBorder),
    modifier = Modifier.height(32.dp)
  ) {
    Box(
      modifier = Modifier.padding(horizontal = 12.dp),
      contentAlignment = Alignment.Center
    ) {
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
 */
@Composable
private fun ContinueReadingBanner(
  novel: NovelWithState,
  onResumeReading: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = AntiqueGoldLight.copy(alpha = 0.45f)),
    border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.4f)),
    modifier = modifier
      .fillMaxWidth()
      .clickable { onResumeReading() }
      .testTag("continue_reading_banner")
  ) {
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
            text = "Page ${novel.currentPage} of ${novel.totalPages} • Progress saved",
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
  }
}

/**
 * Individual Novel Card in the horizontal row
 */
@Composable
private fun HorizontalNovelCard(
  novel: NovelWithState,
  index: Int,
  isFocused: Boolean,
  onCardClick: () -> Unit,
  onFocusClick: () -> Unit,
  onToggleFavorite: () -> Unit,
) {
  val scale by animateFloatAsState(targetValue = if (isFocused) 1.04f else 0.96f, label = "card_scale")
  val elevation by animateFloatAsState(targetValue = if (isFocused) 14f else 6f, label = "card_elevation")
  val borderColor by animateColorAsState(
    targetValue = if (isFocused) AntiqueGold else Color.Transparent,
    label = "border_color"
  )

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .width(136.dp)
      .graphicsLayer {
        scaleX = scale
        scaleY = scale
      }
      .clickable {
        onFocusClick()
        onCardClick()
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
            val authorLabel = if (novel.originalAuthor.isNotBlank()) novel.originalAuthor else novel.author
            val headerText = if (novel.authorSlot > 0) {
              "ROOM ${novel.authorSlot} • ${authorLabel.uppercase()}"
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
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = novel.title,
      style = MaterialTheme.typography.labelMedium.copy(
        fontFamily = FontFamily.Serif,
        fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
      ),
      color = if (isFocused) CharcoalText else CharcoalSecondary,
      textAlign = TextAlign.Center,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis
    )

    Spacer(modifier = Modifier.height(2.dp))

    val authorSubtitle = buildString {
      if (novel.originalAuthor.isNotBlank()) {
        append("By ")
        append(novel.originalAuthor)
      } else if (novel.authorSlot > 0) {
        append("Room ")
        append(novel.authorSlot)
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
  }
}
