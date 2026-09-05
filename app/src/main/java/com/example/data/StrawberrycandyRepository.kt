package com.example.data

import com.example.R
import com.example.data.local.AuthorSlotEntity
import com.example.data.local.BookmarkHighlightEntity
import com.example.data.local.ChapterCommentEntity
import com.example.data.local.NovelEntity
import com.example.data.local.ReaderProfileEntity
import com.example.data.local.StrawberrycandyDao
import com.example.data.local.UserReadingStateEntity
import com.example.model.NovelWithState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.util.UUID

class StrawberrycandyRepository(
  private val dao: StrawberrycandyDao,
  private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
) {

  init {
    externalScope.launch {
      seedDefaultNovelsIfEmpty()
      seedDefaultAuthorSlotsIfEmpty()
      seedDefaultCommentsIfEmpty()
      ensureSchemaMonographExists()
    }
  }

  val activeUser: Flow<ReaderProfileEntity?> = dao.getActiveReaderProfile()

  val authorSlots: Flow<List<AuthorSlotEntity>> = dao.getAllAuthorSlots()

  val allNovelsWithState: Flow<List<NovelWithState>> =
    activeUser.flatMapLatest { user ->
      val readingStatesFlow = if (user != null) {
        dao.getUserReadingStates(user.userId)
      } else {
        flowOf(emptyList())
      }

      combine(dao.getAllNovels(), readingStatesFlow) { novels, states ->
        val stateMap = states.associateBy { it.novelId }
        novels.map { novel ->
          NovelWithState(
            novel = novel,
            userState = stateMap[novel.id]
          )
        }
      }
    }

  suspend fun uploadNovel(
    title: String,
    subtitle: String,
    chapterTitle: String,
    excerpt: String,
    content: String,
    coverColorHex: Long = 0xFF5C2D3B,
    author: String = "Strawberrycandy",
    authorSlot: Int = 0,
    coverImageUri: String? = null,
    storyImagesJson: String = "",
    originalAuthor: String = "",
  ): String {
    val novelId = "nov_" + UUID.randomUUID().toString().take(8)
    val novel = NovelEntity(
      id = novelId,
      title = title.trim(),
      subtitle = subtitle.trim().ifEmpty { if (authorSlot == 0) "Published by Strawberrycandy" else "Author Room $authorSlot • Strawberrycandy Archive" },
      author = author.trim().ifEmpty { "Strawberrycandy" },
      originalAuthor = originalAuthor.trim(),
      authorSlot = authorSlot,
      year = "2026",
      editionNumber = "EDITION NO. " + (10 + (System.currentTimeMillis() % 90)) + " / STRAWBERRYCANDY",
      coverDrawableRes = 0,
      coverImageUri = coverImageUri,
      coverColorHex = coverColorHex,
      chapterTitle = chapterTitle.trim().ifEmpty { "Chapter I" },
      totalPages = 120 + (content.length / 400).coerceAtLeast(10),
      excerpt = excerpt.trim().ifEmpty { content.take(150) + "..." },
      contentText = content.trim(),
      isOwnerUploaded = true,
      createdAt = System.currentTimeMillis(),
      readsCount = 1,
      favoritesCount = 0,
      storyImagesJson = storyImagesJson,
    )
    dao.insertNovel(novel)
    return novelId
  }

  suspend fun recordNovelRead(novelId: String) {
    dao.incrementReadsCount(novelId)
  }

  suspend fun updateAuthorSlot(slotNumber: Int, authorName: String, penName: String, bio: String) {
    val existing = dao.getAuthorSlot(slotNumber)
    if (existing != null) {
      val updated = existing.copy(
        authorName = authorName.trim(),
        penName = penName.trim(),
        bio = bio.trim(),
        lastActiveTimestamp = System.currentTimeMillis()
      )
      dao.updateAuthorSlot(updated)
    }
  }

  suspend fun updateAuthorSlotCover(slotNumber: Int, coverImageUri: String) {
    dao.updateAuthorSlotCover(slotNumber, coverImageUri)
  }

  suspend fun setSlotPermission(slotNumber: Int, isGranted: Boolean) {
    if (slotNumber != 0) { // Slot 0 is Strawberrycandy (Owner), always granted
      dao.updateAuthorSlotPermission(slotNumber, isGranted)
    }
  }

  suspend fun deleteNovel(id: String) {
    dao.deleteNovelById(id)
  }

  suspend fun updateNovelCover(id: String, coverImageUri: String) {
    dao.updateNovelCover(id, coverImageUri)
  }

  suspend fun signIn(
    provider: String, // "GOOGLE" or "APPLE"
    email: String,
    displayName: String,
  ) {
    val userId = "usr_" + provider.lowercase() + "_" + email.replace(Regex("[^a-zA-Z0-9]"), "").take(12)
    val profile = ReaderProfileEntity(
      userId = userId,
      displayName = displayName.ifEmpty { if (provider == "GOOGLE") "Google Reader" else "Apple Reader" },
      email = email,
      provider = provider,
      lastLoginTimestamp = System.currentTimeMillis()
    )
    dao.insertReaderProfile(profile)
  }

  suspend fun signOut() {
    dao.clearReaderProfiles()
  }

  suspend fun toggleFavorite(userId: String, novelId: String) {
    val existing = dao.getReadingState(userId, novelId)
    if (existing != null) {
      val newFav = !existing.isFavorite
      val updated = existing.copy(
        isFavorite = newFav,
        lastReadTimestamp = System.currentTimeMillis()
      )
      dao.insertOrUpdateReadingState(updated)
      dao.updateFavoritesCount(novelId, if (newFav) 1 else 0)
    } else {
      val newState = UserReadingStateEntity(
        compositeId = "${userId}_${novelId}",
        userId = userId,
        novelId = novelId,
        currentPage = 1,
        isFavorite = true,
        inReadingList = true,
        lastReadTimestamp = System.currentTimeMillis()
      )
      dao.insertOrUpdateReadingState(newState)
      dao.updateFavoritesCount(novelId, 1)
    }
  }

  suspend fun toggleReadingList(userId: String, novelId: String) {
    val existing = dao.getReadingState(userId, novelId)
    if (existing != null) {
      val updated = existing.copy(
        inReadingList = !existing.inReadingList,
        lastReadTimestamp = System.currentTimeMillis()
      )
      dao.insertOrUpdateReadingState(updated)
    } else {
      val newState = UserReadingStateEntity(
        compositeId = "${userId}_${novelId}",
        userId = userId,
        novelId = novelId,
        currentPage = 1,
        isFavorite = false,
        inReadingList = true,
        lastReadTimestamp = System.currentTimeMillis()
      )
      dao.insertOrUpdateReadingState(newState)
    }
  }

  suspend fun saveReadingProgress(userId: String, novelId: String, page: Int) {
    val existing = dao.getReadingState(userId, novelId)
    if (existing != null) {
      val updated = existing.copy(
        currentPage = page,
        inReadingList = true,
        lastReadTimestamp = System.currentTimeMillis()
      )
      dao.insertOrUpdateReadingState(updated)
    } else {
      val newState = UserReadingStateEntity(
        compositeId = "${userId}_${novelId}",
        userId = userId,
        novelId = novelId,
        currentPage = page,
        isFavorite = false,
        inReadingList = true,
        lastReadTimestamp = System.currentTimeMillis()
      )
      dao.insertOrUpdateReadingState(newState)
    }
  }

  private suspend fun seedDefaultNovelsIfEmpty() {
    if (dao.getNovelCount() == 0) {
      val defaultNovels = listOf(
        NovelEntity(
          id = "nov_1",
          title = "The Architecture of Silence",
          subtitle = "Monastic Spacing & Solitary Thought",
          author = "Strawberrycandy",
          originalAuthor = "Henri Focillon",
          year = "2026",
          editionNumber = "ARCHIVE NO. 042 / 100",
          coverDrawableRes = R.drawable.img_book_1,
          coverColorHex = 0xFF2A2825,
          chapterTitle = "Chapter III • The Acoustics of Stillness",
          totalPages = 312,
          excerpt = "To construct a room for silence is not to subtract sound, but to tune the resonance of what remains.",
          contentText = listOf(
            "To construct a room for silence is not merely to subtract sound, but to tune the subtle resonance of what remains. In the cloistered arcades of Thoronet and the vaulted corridors of Sénanque, stone does not absorb speech; it receives it as a transient vibration, smoothing harshness into an echo that returns only as ambient presence.",
            "[photo:drawable:img_book_1:Plate I • Monastic Stone Arcades & The Acoustics of Thoronet]",
            "When an author steps into such enclosures, the cadence of thought slows to match the thermal mass of granite. We are accustomed in contemporary life to an architecture of velocity—glass surfaces that reflect only haste, partitions that transmit anxiety.",
            "Here, conversely, the wall possesses gravity. A single window, cut deep into limestone with a 45-degree splay, gathers the morning light and diffuses it across whitewashed lime plaster with an evenness that renders artificial illumination unnecessary. In this sanctuary, the page becomes the floorplan of an inner chamber.",
            "[photo:drawable:img_book_2:Plate II • Deep Limestone Splay Diffusing Morning Light]",
            "Consider the margin of the printed page. It is not wasted paper; it is the physical moat that shields the text from the noise of the surrounding world. Just as a Japanese teahouse requires an entry crawl-space (nijiriguchi) to humble the visitor, a worthy book demands an expanse of blank cream paper before the first sentence may begin.",
            "We read, ultimately, not to consume data, but to occupy a space designed by an architect of language. The sentences must carry structural integrity: verbs acting as load-bearing columns, subordinate clauses as cantilevered balconies overlooking quiet courtyards of reflection.",
            "In this proprietary edition, the text is sealed against casual dissemination. It exists only for the deliberate eye, held within an authenticated viewport where words retain their weight, unclouded by the frantic mechanisms of the digital crowd."
          ).joinToString("\n\n"),
          isOwnerUploaded = false,
          createdAt = 1000L,
          readsCount = 1420,
          favoritesCount = 348
        ),
        NovelEntity(
          id = "nov_2",
          title = "Solitude & Form",
          subtitle = "Studies in Void, Weight, and Proportion",
          author = "Strawberrycandy",
          originalAuthor = "Rainer Maria Rilke",
          year = "2025",
          editionNumber = "ARCHIVE NO. 018 / 100",
          coverDrawableRes = R.drawable.img_book_2,
          coverColorHex = 0xFF352B28,
          chapterTitle = "Chapter I • The Geometry of Solitude",
          totalPages = 284,
          excerpt = "Form is what happens when solitude is granted sufficient time to harden into outline.",
          contentText = listOf(
            "Form is what happens when solitude is granted sufficient time to harden into outline. Left to itself, human contemplation is diffuse, mist-like, drifting across memories and unfinished gestures without settling into durable shape.",
            "Only under the deliberate pressure of seclusion does the material crystallize. The sculptor knows that marble does not yield to haste; every chisel blow is a subtraction that reveals an inevitable contour already dormant within the quarry.",
            "[photo:drawable:img_book_2:Plate I • Contemplation of the Stone Quarry & Solitary Form]",
            "In our era of hyper-connectivity, solitude is often treated as an error code—a malfunction to be corrected by immediate notification. Yet throughout intellectual history, every profound contribution to philosophy and literature arose from sustained, uninterrupted seclusion.",
            "The mind needs borders just as a painting requires a frame. Without limits, imagination dissipates into ambient noise. By establishing rigorous artistic constraints—restricting our shelf to three works, purifying our canvas of extraneous buttons—we return dignity to the reading act.",
            "The reader who opens this volume enters a contractual stillness. No alerts will arrive; no algorithms will measure your scroll speed to optimize engagement. There is only the charcoal ligature upon cream fiber, and the silent covenant between author and witness."
          ).joinToString("\n\n"),
          isOwnerUploaded = false,
          createdAt = 2000L,
          readsCount = 890,
          favoritesCount = 215
        ),
        NovelEntity(
          id = "nov_3",
          title = "Ephemeral Light",
          subtitle = "Notes on Radiance, Dawn, and Impermanence",
          author = "Strawberrycandy",
          originalAuthor = "Jun'ichirō Tanizaki",
          year = "2024",
          editionNumber = "ARCHIVE NO. 087 / 100",
          coverDrawableRes = R.drawable.img_book_3,
          coverColorHex = 0xFF2F322B,
          chapterTitle = "Chapter V • Translucence and Memory",
          totalPages = 196,
          excerpt = "The most enduring ideas are those written with the lightness of dust floating across a sunbeam.",
          contentText = listOf(
            "The most enduring ideas are those written with the lightness of dust floating across a sunbeam. They do not demand assent with theatrical rhetoric; they illuminate quietly, allowing the reader to discover the truth as if remembering a long-forgotten dream.",
            "[photo:drawable:img_book_3:Plate I • Slant Light across the Ancient Library Gallery at Dawn]",
            "At dawn, before the city awakens, light enters sideways. It strikes the spine of a book shelved three decades ago, gilding the gold foil embossing with a momentary flash of gold. For five minutes, that forgotten object becomes the brightest thing in the library.",
            "Literature is that slant light. It illuminates what was already present in the reader's spirit, giving names to sensations that were previously mute. In designing these digital manuscripts, we sought the tactile tranquility of that early morning study.",
            "The subtle grain of paper, the restraint of charcoal typography, and the absence of intrusive interface machinery all conspire toward one end: that nothing shall stand between the radiance of the sentence and the stillness of the reader's mind."
          ).joinToString("\n\n"),
          isOwnerUploaded = false,
          createdAt = 3000L,
          readsCount = 612,
          favoritesCount = 149
        )
      )
      dao.insertNovels(defaultNovels)
    }
  }

  private suspend fun ensureSchemaMonographExists() {
    if (dao.getNovelById("nov_schema") == null) {
      val schemaMonograph = NovelEntity(
        id = "nov_schema",
        title = "Substance Over Surface",
        subtitle = "SCHEMA Issue No. 28 • The Decline of Ephemeral UI",
        author = "SCHEMA",
        originalAuthor = "Paul Aris",
        authorSlot = 0,
        year = "2026",
        editionNumber = "EDITION NO. 28 / FALL 2026",
        coverDrawableRes = R.drawable.img_book_1,
        coverColorHex = 0xFF141414,
        chapterTitle = "Monograph • Physicality in Software & Spatial Persistence",
        totalPages = 148,
        excerpt = "An exploration into durable software tools, tactile digital interfaces, and how the pursuit of hyper-velocity eroded deliberate human-computer ergonomics.",
        contentText = listOf(
          "Substance over surface: The decline of ephemeral UI.",
          "An exploration into durable software tools, tactile digital interfaces, and how the pursuit of hyper-velocity eroded deliberate human-computer ergonomics.",
          "[photo:drawable:img_book_1:Plate I • Monolith Architecture Form & Spatial Persistence in Software]",
          "Why predicting state mutation visually creates calmness: in deterministic interfaces, the ambient cognitive overhead of web applications drops to zero.",
          "Opticals, Weights, and the Modern Sans: Tracing font rasterization behaviors from high-DPI glass screens back to Swiss modernism's raw strictness.",
          "Physical Levers in an Intangible Medium: The tactile feedback loops that mechanical craftsmen understood decades before mobile micro-haptics attempted replication.",
          "Local-First Software and Sovereignty: Ownership paradigms in an era where network rent-seeking threatens the longevity of personal intellectual work.",
          "Designing for Deliberate Friction: How frictionless flows encourage thoughtless actions, and where modern product designers must reintroduce hesitation.",
          "The Monochrome Paradigm in App Design: Restricting color palette usage exclusively to semantic states as a discipline to uncover poor visual hierarchies."
        ).joinToString("\n\n"),
        isOwnerUploaded = false,
        createdAt = 4000L,
        readsCount = 3840,
        favoritesCount = 920
      )
      dao.insertNovel(schemaMonograph)
    }
  }

  private suspend fun seedDefaultAuthorSlotsIfEmpty() {
    val defaultSlots = listOf(
      AuthorSlotEntity(
        slotNumber = 0,
        authorName = "strawberrycandy",
        penName = "strawberrycandy",
        bio = "Founder & Curator at Strawberrycandy Archive. Oversees manuscript acquisitions and literary curation.",
        avatarColorHex = 0xFF8C2D48,
        accessCode = "ARCHIVE-OWNER-0",
        isClaimed = true,
        isPermissionGranted = true
      ),
      AuthorSlotEntity(
        slotNumber = 1,
        authorName = "Aria Thorne",
        penName = "Aria Thorne",
        bio = "Atmospheric gothic fiction, liminal romances, and cathedral solitude.",
        avatarColorHex = 0xFF5C2D3B,
        accessCode = "AUTH-ROOM-1",
        isClaimed = true,
        isPermissionGranted = true
      ),
      AuthorSlotEntity(
        slotNumber = 2,
        authorName = "Felix Moreau",
        penName = "Felix Moreau",
        bio = "Speculative architectural essays, existential monographs, and twilight studies.",
        avatarColorHex = 0xFF28362D,
        accessCode = "AUTH-ROOM-2",
        isClaimed = true,
        isPermissionGranted = true
      ),
      AuthorSlotEntity(
        slotNumber = 3,
        authorName = "Clara O'Connor",
        penName = "Clara O'Connor",
        bio = "Chronicles of quiet coastal landscapes, mist-shrouded poetry, and dawn light.",
        avatarColorHex = 0xFF21252D,
        accessCode = "AUTH-ROOM-3",
        isClaimed = true,
        isPermissionGranted = true
      ),
      AuthorSlotEntity(
        slotNumber = 4,
        authorName = "Dante Valeri",
        penName = "Dante Valeri",
        bio = "Modernist psychological realism, chamber memoirs, and classical dialogue.",
        avatarColorHex = 0xFF4A3428,
        accessCode = "AUTH-ROOM-4",
        isClaimed = true,
        isPermissionGranted = true
      ),
      AuthorSlotEntity(
        slotNumber = 5,
        authorName = "Evelyn Vance",
        penName = "Evelyn Vance",
        bio = "Mythic folklore adaptations, lyrical historical drama, and translated ballads.",
        avatarColorHex = 0xFF3D405B,
        accessCode = "AUTH-ROOM-5",
        isClaimed = false,
        isPermissionGranted = false
      ),
      AuthorSlotEntity(
        slotNumber = 6,
        authorName = "Julian Croft",
        penName = "Julian Croft",
        bio = "Philosophical noir novellas, urban rain memoirs, and midnight translations.",
        avatarColorHex = 0xFF4A5859,
        accessCode = "AUTH-ROOM-6",
        isClaimed = false,
        isPermissionGranted = false
      ),
      AuthorSlotEntity(
        slotNumber = 7,
        authorName = "Mira Hashimoto",
        penName = "Mira Hashimoto",
        bio = "Quiet magical realism, tea garden parables, and celestial vignettes.",
        avatarColorHex = 0xFF6B4E71,
        accessCode = "AUTH-ROOM-7",
        isClaimed = false,
        isPermissionGranted = false
      ),
      AuthorSlotEntity(
        slotNumber = 8,
        authorName = "Lucian Bell",
        penName = "Lucian Bell",
        bio = "Neo-Victorian detective mysteries and classical gothic archives.",
        avatarColorHex = 0xFF583E26,
        accessCode = "AUTH-ROOM-8",
        isClaimed = false,
        isPermissionGranted = false
      ),
      AuthorSlotEntity(
        slotNumber = 9,
        authorName = "Sophie Lind",
        penName = "Sophie Lind",
        bio = "Nordic minimalist fiction, winter journals, and island solitude.",
        avatarColorHex = 0xFF2E4057,
        accessCode = "AUTH-ROOM-9",
        isClaimed = false,
        isPermissionGranted = false
      ),
      AuthorSlotEntity(
        slotNumber = 10,
        authorName = "Rowan Mercer",
        penName = "Rowan Mercer",
        bio = "Antiquity chronicles, lost library manuscripts, and epistolary tragedies.",
        avatarColorHex = 0xFF4B3832,
        accessCode = "AUTH-ROOM-10",
        isClaimed = false,
        isPermissionGranted = false
      )
    )

    if (dao.getAuthorSlotCount() == 0) {
      dao.insertAuthorSlots(defaultSlots)
    } else {
      // Ensure all 10 translator slots plus slot 0 exist
      for (slot in defaultSlots) {
        if (dao.getAuthorSlot(slot.slotNumber) == null) {
          dao.updateAuthorSlot(slot)
        }
      }
    }
  }

  // Chapter Comments per Chapter
  fun getCommentsForChapter(novelId: String, chapterTitle: String): Flow<List<ChapterCommentEntity>> {
    return dao.getCommentsForChapter(novelId, chapterTitle)
  }

  fun getCommentCountForChapter(novelId: String, chapterTitle: String): Flow<Int> {
    return dao.getCommentCountForChapter(novelId, chapterTitle)
  }

  suspend fun addComment(
    novelId: String,
    chapterTitle: String,
    readerName: String,
    readerEmail: String = "",
    commentText: String,
    avatarColorHex: Long = 0xFF5C2D3B,
  ) {
    val comment = ChapterCommentEntity(
      id = "cmt_" + UUID.randomUUID().toString().take(8),
      novelId = novelId,
      chapterTitle = chapterTitle,
      readerName = readerName.trim().ifEmpty { "Literary Reader" },
      readerEmail = readerEmail,
      commentText = commentText.trim(),
      timestamp = System.currentTimeMillis(),
      avatarColorHex = avatarColorHex,
      likesCount = 0,
      isLikedByMe = false
    )
    dao.insertComment(comment)
  }

  suspend fun likeComment(commentId: String) {
    dao.likeComment(commentId)
  }

  // Favorite Lines & Bookmarks
  fun getBookmarksForNovel(novelId: String): Flow<List<BookmarkHighlightEntity>> {
    return dao.getBookmarksForNovel(novelId)
  }

  suspend fun toggleBookmark(
    novelId: String,
    chapterTitle: String,
    quoteText: String,
    paragraphIndex: Int,
    note: String = ""
  ): Boolean {
    val existing = dao.findBookmarkByQuote(novelId, quoteText)
    return if (existing != null) {
      dao.deleteBookmark(existing.id)
      false // removed
    } else {
      val bookmark = BookmarkHighlightEntity(
        id = "bmk_" + UUID.randomUUID().toString().take(8),
        novelId = novelId,
        chapterTitle = chapterTitle,
        quoteText = quoteText.trim(),
        paragraphIndex = paragraphIndex,
        timestamp = System.currentTimeMillis(),
        colorHex = 0xFFD4AF37,
        note = note
      )
      dao.insertBookmark(bookmark)
      true // added
    }
  }

  suspend fun deleteBookmark(bookmarkId: String) {
    dao.deleteBookmark(bookmarkId)
  }

  private suspend fun seedDefaultCommentsIfEmpty() {
    if (dao.getCommentCount() == 0) {
      val defaultComments = listOf(
        ChapterCommentEntity(
          id = "cmt_init_1",
          novelId = "nov_1",
          chapterTitle = "Chapter III • The Acoustics of Stillness",
          readerName = "Camille Laurent",
          readerEmail = "c.laurent@sorbonne.lit",
          commentText = "The observation about stone arcades smoothing harshness into transient presence is breathtaking. Reading this in total silence feels like wandering into Sénanque at dusk.",
          timestamp = System.currentTimeMillis() - 86400000L * 2,
          avatarColorHex = 0xFF4A3E3D,
          likesCount = 14,
          isLikedByMe = false
        ),
        ChapterCommentEntity(
          id = "cmt_init_2",
          novelId = "nov_1",
          chapterTitle = "Chapter III • The Acoustics of Stillness",
          readerName = "Julian Croft",
          readerEmail = "j.croft@oxford.archive",
          commentText = "The metaphor of the page margin as an architectural moat against external noise—this is why typography matters. Sublime prose.",
          timestamp = System.currentTimeMillis() - 3600000L * 18,
          avatarColorHex = 0xFF2F3E46,
          likesCount = 9,
          isLikedByMe = false
        ),
        ChapterCommentEntity(
          id = "cmt_init_3",
          novelId = "nov_2",
          chapterTitle = "Chapter I • The Geometry of Solitude",
          readerName = "Elena Rostova",
          readerEmail = "elena.rostova@literary.cafe",
          commentText = "'Form is what happens when solitude is granted sufficient time to harden into outline.' Bookmarking this sentence forever.",
          timestamp = System.currentTimeMillis() - 3600000L * 30,
          avatarColorHex = 0xFF5C2D3B,
          likesCount = 21,
          isLikedByMe = false
        )
      )
      dao.insertComments(defaultComments)
    }
  }
}
