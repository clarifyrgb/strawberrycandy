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

  companion object {
    val OWNER_EMAILS = setOf(
      "clarifymanga@gmail.com",
      "clarissamendoza241@gmail.com"
    )

    fun isOwnerEmail(email: String?): Boolean {
      if (email.isNullOrBlank()) return false
      val normalized = email.trim().lowercase()
      return normalized in OWNER_EMAILS ||
             normalized == "clarifymanga@gmail.com" ||
             normalized == "clarissamendoza241@gmail.com"
    }
  }

  init {
    externalScope.launch {
      seedDefaultNovelsIfEmpty()
      seedDefaultAuthorSlotsIfEmpty()
      seedDefaultCommentsIfEmpty()
      ensureSchemaMonographExists()
      ensureFeaturedNovelsWithCoversExist()
      upgradeDefaultNovelsWithChapters()
      removePhotosUnderThoughtIfPresent()
      dao.removeFakeSeededReadingState()
      dao.resetArtificialReads()
      dao.syncAllRealFavorites()
      dao.sanitizeSlotBios()
      dao.sanitizeSlotPenNames()
      dao.sanitizeCommentNames()
      syncOwnerConfiguration()
      ensureQuickFindCategoriesExist()
    }
  }

  val activeUser: Flow<ReaderProfileEntity?> = dao.getActiveReaderProfile()

  val authorSlots: Flow<List<AuthorSlotEntity>> = dao.getAllAuthorSlots()

  val allNovelsWithState: Flow<List<NovelWithState>> =
    activeUser.flatMapLatest { user ->
      val effectiveUserId = user?.userId ?: "guest_reader"
      val readingStatesFlow = dao.getUserReadingStates(effectiveUserId)

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
    novelStatus: String = "ONGOING",
    releaseFormat: String = "CHAPTER",
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
      readsCount = 0,
      favoritesCount = 0,
      storyImagesJson = storyImagesJson,
      novelStatus = novelStatus,
      releaseFormat = releaseFormat,
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
      val emailRegex = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
      val cleanPenName = penName.replace(emailRegex, "").trim().ifBlank {
        existing.penName.replace(emailRegex, "").trim().ifBlank { "Translator $slotNumber" }
      }
      val cleanAuthorName = authorName.replace(emailRegex, "").trim().ifBlank { cleanPenName }
      val cleanBio = bio.replace(emailRegex, "").trim()
      val updated = existing.copy(
        authorName = cleanAuthorName,
        penName = cleanPenName,
        bio = cleanBio,
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

  suspend fun updateNovel(
    novelId: String,
    title: String,
    subtitle: String,
    originalAuthor: String,
    synopsis: String,
    chapterTitle: String,
    contentText: String,
    coverColorHex: Long? = null,
    coverImageUri: String? = null,
    novelStatus: String? = null,
    releaseFormat: String? = null,
  ) {
    val existing = dao.getNovelById(novelId) ?: return
    val cleanContent = contentText.trim()
    val updated = existing.copy(
      title = title.trim(),
      subtitle = subtitle.trim(),
      originalAuthor = originalAuthor.trim(),
      excerpt = synopsis.trim(),
      chapterTitle = chapterTitle.trim(),
      contentText = cleanContent,
      coverColorHex = coverColorHex ?: existing.coverColorHex,
      coverImageUri = coverImageUri ?: existing.coverImageUri,
      novelStatus = novelStatus ?: existing.novelStatus,
      releaseFormat = releaseFormat ?: existing.releaseFormat,
      totalPages = maxOf(1, cleanContent.split("\n\n").count { it.isNotBlank() } * 2)
    )
    dao.insertNovel(updated)
  }

  suspend fun updateNovelCover(id: String, coverImageUri: String) {
    dao.updateNovelCover(id, coverImageUri)
  }

  suspend fun signIn(
    provider: String, // "GOOGLE" or "APPLE"
    email: String,
    password: String = "",
    displayName: String,
    role: String = "READER",
    authorSlot: Int? = null,
  ): Result<Unit> {
    val cleanEmail = email.trim().lowercase()
    if (provider == "GOOGLE") {
      if (!cleanEmail.endsWith("@gmail.com") && !cleanEmail.endsWith("@googlemail.com")) {
        return Result.failure(IllegalArgumentException("For Google sign in, you must enter a valid Gmail address (@gmail.com)."))
      }
    }
    if (password.isBlank()) {
      return Result.failure(IllegalArgumentException("Please enter your account password."))
    }

    val userId = "usr_" + provider.lowercase() + "_" + cleanEmail.replace(Regex("[^a-z0-9]"), "_")
    val isOwner = isOwnerEmail(cleanEmail)

    // Check existing profile
    val existingProfile = dao.getReaderProfileByEmail(cleanEmail) ?: dao.getReaderProfile(userId)
    if (!isOwner && existingProfile != null && !existingProfile.passwordHash.isNullOrBlank() && password.isNotBlank()) {
      if (existingProfile.passwordHash != password) {
        return Result.failure(
          IllegalArgumentException(
            "Incorrect password for Google Account $cleanEmail. The password entered must match your account password."
          )
        )
      }
    }

    // Check if owner has pre-granted this email to an author slot
    val preGrantedSlot = dao.getAuthorSlotByEmail(cleanEmail)

    val finalRole = if (isOwner) {
      "OWNER"
    } else if (preGrantedSlot != null && preGrantedSlot.isPermissionGranted) {
      "TRANSLATOR"
    } else if (role == "TRANSLATOR" || existingProfile?.role == "TRANSLATOR") {
      "TRANSLATOR"
    } else {
      "READER"
    }

    val finalSlot = if (finalRole == "OWNER") {
      0
    } else if (preGrantedSlot != null) {
      preGrantedSlot.slotNumber
    } else {
      authorSlot ?: existingProfile?.authorSlot
    }

    val emailRegex = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
    val finalName = if (displayName.isNotBlank() && !displayName.contains("@")) {
      displayName.trim()
    } else if (!existingProfile?.displayName.isNullOrBlank()) {
      existingProfile!!.displayName
    } else {
      if (finalRole == "OWNER") {
        "Clarify"
      } else if (finalRole == "TRANSLATOR") {
        val slot = finalSlot?.let { dao.getAuthorSlot(it) }
        slot?.penName?.replace(emailRegex, "")?.trim()?.ifBlank { "Translator $finalSlot" } ?: "Translator"
      } else {
        val emailPrefix = cleanEmail.substringBefore("@").replace(".", " ").trim()
        if (emailPrefix.isNotBlank()) {
          emailPrefix.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        } else {
          "Reader"
        }
      }
    }

    val existingPoints = existingProfile?.penNamePoints ?: 0

    val profile = ReaderProfileEntity(
      userId = userId,
      displayName = finalName,
      email = cleanEmail,
      provider = provider,
      role = finalRole,
      authorSlot = finalSlot,
      penNamePoints = existingPoints,
      lastLoginTimestamp = System.currentTimeMillis(),
      passwordHash = password.ifBlank { existingProfile?.passwordHash ?: "account_pass" }
    )
    dao.insertReaderProfile(profile)

    // If translator, link their Gmail address to the author slot so the Owner can see it
    if (finalRole == "TRANSLATOR" && finalSlot != null && finalSlot != 0) {
      val slot = dao.getAuthorSlot(finalSlot)
      if (slot != null) {
        dao.updateAuthorSlotEmail(finalSlot, cleanEmail)
      }
    }

    return Result.success(Unit)
  }

  suspend fun grantPermissionByEmail(email: String, slotNumber: Int? = null) {
    val cleanEmail = email.trim().lowercase()
    val existingSlot = dao.getAuthorSlotByEmail(cleanEmail)
    if (existingSlot != null) {
      dao.updateAuthorSlotPermission(existingSlot.slotNumber, true)
    } else {
      val target = slotNumber ?: (1..10).firstOrNull { slotNum ->
        val s = dao.getAuthorSlot(slotNum)
        s == null || !s.isPermissionGranted
      } ?: 5
      dao.updateAuthorSlotPermissionAndEmail(target, true, cleanEmail)
    }
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
    }
    dao.syncRealFavoritesForNovel(novelId)
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

  suspend fun markNovelAsFinished(userId: String, novelId: String): Boolean {
    val existing = dao.getReadingState(userId, novelId)
    val novel = dao.getNovelById(novelId)
    val totalPages = novel?.totalPages ?: 100
    var pointEarned = false

    if (existing != null) {
      val shouldAwardPoint = !existing.pointsAwarded
      val updated = existing.copy(
        currentPage = totalPages,
        isFinished = true,
        inReadingList = true,
        inTbrList = false,
        pointsAwarded = true,
        lastReadTimestamp = System.currentTimeMillis()
      )
      dao.insertOrUpdateReadingState(updated)
      if (shouldAwardPoint) {
        dao.addPointsToReader(userId, 1)
        pointEarned = true
      }
    } else {
      val newState = UserReadingStateEntity(
        compositeId = "${userId}_${novelId}",
        userId = userId,
        novelId = novelId,
        currentPage = totalPages,
        isFavorite = false,
        inReadingList = true,
        isFinished = true,
        inTbrList = false,
        pointsAwarded = true,
        lastReadTimestamp = System.currentTimeMillis()
      )
      dao.insertOrUpdateReadingState(newState)
      dao.addPointsToReader(userId, 1)
      pointEarned = true
    }
    return pointEarned
  }

  suspend fun markNovelAsToBeRead(userId: String, novelId: String) {
    val existing = dao.getReadingState(userId, novelId)
    if (existing != null) {
      val updated = existing.copy(
        inTbrList = true,
        isFinished = false,
        inReadingList = true,
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
        isFinished = false,
        inTbrList = true,
        pointsAwarded = false,
        lastReadTimestamp = System.currentTimeMillis()
      )
      dao.insertOrUpdateReadingState(newState)
    }
  }

  suspend fun markNovelAsReading(userId: String, novelId: String) {
    val existing = dao.getReadingState(userId, novelId)
    if (existing != null) {
      val updated = existing.copy(
        inTbrList = false,
        isFinished = false,
        inReadingList = true,
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
        isFinished = false,
        inTbrList = false,
        pointsAwarded = false,
        lastReadTimestamp = System.currentTimeMillis()
      )
      dao.insertOrUpdateReadingState(newState)
    }
  }

  suspend fun saveReadingProgress(userId: String, novelId: String, page: Int): Boolean {
    val existing = dao.getReadingState(userId, novelId)
    val novel = dao.getNovelById(novelId)
    val totalPages = novel?.totalPages ?: 100
    val isCompleted = page >= totalPages
    var pointEarned = false

    if (existing != null) {
      val shouldAwardPoint = isCompleted && !existing.pointsAwarded
      val updated = existing.copy(
        currentPage = page,
        inReadingList = true,
        inTbrList = false,
        isFinished = if (isCompleted) true else existing.isFinished,
        pointsAwarded = if (isCompleted) true else existing.pointsAwarded,
        lastReadTimestamp = System.currentTimeMillis()
      )
      dao.insertOrUpdateReadingState(updated)
      if (shouldAwardPoint) {
        dao.addPointsToReader(userId, 1)
        pointEarned = true
      }
    } else {
      val shouldAwardPoint = isCompleted
      val newState = UserReadingStateEntity(
        compositeId = "${userId}_${novelId}",
        userId = userId,
        novelId = novelId,
        currentPage = page,
        isFavorite = false,
        inReadingList = true,
        isFinished = isCompleted,
        inTbrList = false,
        pointsAwarded = isCompleted,
        lastReadTimestamp = System.currentTimeMillis()
      )
      dao.insertOrUpdateReadingState(newState)
      if (shouldAwardPoint) {
        dao.addPointsToReader(userId, 1)
        pointEarned = true
      }
    }
    return pointEarned
  }

  suspend fun changePenNameWithPoint(userId: String, newPenName: String): Boolean {
    val emailRegex = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
    val cleanPenName = newPenName.replace(emailRegex, "").trim()
    if (cleanPenName.isBlank()) return false

    val profile = dao.getReaderProfile(userId) ?: return false
    if (profile.penNamePoints < 1) {
      return false
    }

    val updatedCount = dao.deductPointAndSetDisplayName(userId, cleanPenName)
    if (updatedCount > 0) {
      val slotNum = profile.authorSlot
      if (slotNum != null && slotNum >= 0) {
        dao.updateSlotPenName(slotNum, cleanPenName)
      }
      return true
    }
    return false
  }

  suspend fun updateReaderDisplayName(userId: String, newDisplayName: String): Boolean {
    val emailRegex = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
    val cleanName = newDisplayName.replace(emailRegex, "").trim()
    if (cleanName.isBlank()) return false

    val profile = dao.getReaderProfile(userId) ?: return false
    val updatedCount = dao.updateReaderDisplayName(userId, cleanName)
    if (updatedCount > 0) {
      val slotNum = profile.authorSlot
      if (slotNum != null && slotNum >= 0) {
        dao.updateSlotPenName(slotNum, cleanName)
      }
      return true
    }
    return false
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
          chapterTitle = "Chapter I • The Cloistered Arcades of Thoronet",
          totalPages = 312,
          excerpt = "To construct a room for silence is not to subtract sound, but to tune the resonance of what remains.",
          contentText = listOf(
            "[chapter:Chapter I • The Cloistered Arcades of Thoronet]",
            "To construct a room for silence is not merely to subtract sound, but to tune the subtle resonance of what remains. In the cloistered arcades of Thoronet and the vaulted corridors of Sénanque, stone does not absorb speech; it receives it as a transient vibration, smoothing harshness into an echo that returns only as ambient presence.",
            "When an author steps into such enclosures, the cadence of thought slows to match the thermal mass of granite. We are accustomed in contemporary life to an architecture of velocity—glass surfaces that reflect only haste, partitions that transmit anxiety.",
            "[chapter:Chapter II • Deep Limestone Splays & Morning Light]",
            "Here, conversely, the wall possesses gravity. A single window, cut deep into limestone with a 45-degree splay, gathers the morning light and diffuses it across whitewashed lime plaster with an evenness that renders artificial illumination unnecessary. In this sanctuary, the page becomes the floorplan of an inner chamber.",
            "Consider the margin of the printed page. It is not wasted paper; it is the physical moat that shields the text from the noise of the surrounding world. Just as a Japanese teahouse requires an entry crawl-space (nijiriguchi) to humble the visitor, a worthy book demands an expanse of blank cream paper before the first sentence may begin.",
            "[chapter:Chapter III • The Acoustics of Stillness & Sacred Margins]",
            "We read, ultimately, not to consume data, but to occupy a space designed by an architect of language. The sentences must carry structural integrity: verbs acting as load-bearing columns, subordinate clauses as cantilevered balconies overlooking quiet courtyards of reflection.",
            "In this proprietary edition, the text is sealed against casual dissemination. It exists only for the deliberate eye, held within an authenticated viewport where words retain their weight, unclouded by the frantic mechanisms of the digital crowd."
          ).joinToString("\n\n"),
          isOwnerUploaded = false,
          createdAt = 1000L,
          readsCount = 0,
          favoritesCount = 0
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
            "[chapter:Chapter I • The Geometry of Solitude]",
            "Form is what happens when solitude is granted sufficient time to harden into outline. Left to itself, human contemplation is diffuse, mist-like, drifting across memories and unfinished gestures without settling into durable shape.",
            "Only under the deliberate pressure of seclusion does the material crystallize. The sculptor knows that marble does not yield to haste; every chisel blow is a subtraction that reveals an inevitable contour already dormant within the quarry.",
            "[photo:drawable:img_book_2:Plate I • Contemplation of the Stone Quarry & Solitary Form]",
            "[chapter:Chapter II • The Chisel and the Marble Contour]",
            "In our era of hyper-connectivity, solitude is often treated as an error code—a malfunction to be corrected by immediate notification. Yet throughout intellectual history, every profound contribution to philosophy and literature arose from sustained, uninterrupted seclusion.",
            "The mind needs borders just as a painting requires a frame. Without limits, imagination dissipates into ambient noise. By establishing rigorous artistic constraints—restricting our shelf to three works, purifying our canvas of extraneous buttons—we return dignity to the reading act.",
            "[chapter:Chapter III • The Contractual Stillness of Reading]",
            "The reader who opens this volume enters a contractual stillness. No alerts will arrive; no algorithms will measure your scroll speed to optimize engagement. There is only the charcoal ligature upon cream fiber, and the silent covenant between author and witness."
          ).joinToString("\n\n"),
          isOwnerUploaded = false,
          createdAt = 2000L,
          readsCount = 0,
          favoritesCount = 0
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
          chapterTitle = "Chapter I • Slant Light at Dawn",
          totalPages = 196,
          excerpt = "The most enduring ideas are those written with the lightness of dust floating across a sunbeam.",
          contentText = listOf(
            "[chapter:Chapter I • Slant Light at Dawn]",
            "The most enduring ideas are those written with the lightness of dust floating across a sunbeam. They do not demand assent with theatrical rhetoric; they illuminate quietly, allowing the reader to discover the truth as if remembering a long-forgotten dream.",
            "[photo:drawable:img_book_3:Plate I • Slant Light across the Ancient Library Gallery at Dawn]",
            "[chapter:Chapter II • The Golden Foil of Forgotten Folios]",
            "At dawn, before the city awakens, light enters sideways. It strikes the spine of a book shelved three decades ago, gilding the gold foil embossing with a momentary flash of gold. For five minutes, that forgotten object becomes the brightest thing in the library.",
            "[chapter:Chapter III • Translucence and Memory]",
            "Literature is that slant light. It illuminates what was already present in the reader's spirit, giving names to sensations that were previously mute. In designing these digital manuscripts, we sought the tactile tranquility of that early morning study.",
            "The subtle grain of paper, the restraint of charcoal typography, and the absence of intrusive interface machinery all conspire toward one end: that nothing shall stand between the radiance of the sentence and the stillness of the reader's mind."
          ).joinToString("\n\n"),
          isOwnerUploaded = false,
          createdAt = 3000L,
          readsCount = 0,
          favoritesCount = 0
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
        readsCount = 0,
        favoritesCount = 0
      )
      dao.insertNovel(schemaMonograph)
    }
  }

  private suspend fun ensureFeaturedNovelsWithCoversExist() {
    val featuredNovels = listOf(
      NovelEntity(
        id = "nov_crimson",
        title = "The Crimson Bloom",
        subtitle = "Chronicles of the Spider Lily & Midnight Courtyard",
        author = "Strawberrycandy",
        originalAuthor = "Yukiko Murasaki",
        authorSlot = 0,
        year = "2026",
        editionNumber = "ARCHIVE NO. 063 / 100",
        coverDrawableRes = R.drawable.img_novel_crimson_bloom,
        coverColorHex = 0xFF381519,
        chapterTitle = "Chapter I • The Garden of Wandering Spirits",
        totalPages = 340,
        excerpt = "Beneath the blood-red petals of the midnight lily, secrets long buried in the imperial gardens whisper to those who dare listen.",
        contentText = listOf(
          "[chapter:Chapter I • The Garden of Wandering Spirits]",
          "Beneath the blood-red petals of the midnight lily, secrets long buried in the imperial gardens whisper to those who dare listen. The stone path, polished by centuries of silent footsteps, glowed under the harvest moon with a pale vermilion luminescence.",
          "Ren had traveled across the northern mountains seeking the courtyard of the forgotten empress. Legends spoke of a spring that flowed only during the equinox, its waters reflecting not the face of the living, but the unfinished dreams of those who came before.",
          "[photo:drawable:img_novel_crimson_bloom:Plate I • Crimson Spider Lilies along the Midnight Courtyard]",
          "[chapter:Chapter II • The Song of the Midnight Flute]",
          "A low melody drifted through the weeping willow branches—a tune composed on bamboo so aged it resonated with the tone of falling leaves. It was not sorrowful, yet every note stirred an ache in Ren's chest, as if recalling a promise spoken in another life.",
          "She appeared near the stone lantern, her silk robes dark as obsidian, edged with thread of spun gold. 'You returned,' she whispered, though Ren was certain they had never met. 'The lilies remember every debt.'",
          "[chapter:Chapter III • The Pact Written in Starlight]",
          "To step into the inner sanctum was to surrender the armor of the daylight world. Here, where shadows folded into velvet silence, words became binding vows.",
          "'If you take the bloom,' she warned, holding forth a single crimson stem whose petals trembled with inner fire, 'your heart will never again belong to the mundane world. You will hear the murmurs of the ancient empire in every gust of autumn wind.'"
        ).joinToString("\n\n"),
        isOwnerUploaded = false,
        createdAt = 5000L,
        readsCount = 0,
        favoritesCount = 0
      ),
      NovelEntity(
        id = "nov_celestial",
        title = "The Celestial Observatory",
        subtitle = "Voyages Beyond the Astral Meridian",
        author = "Strawberrycandy",
        originalAuthor = "Arthur Sterling",
        authorSlot = 0,
        year = "2026",
        editionNumber = "ARCHIVE NO. 077 / 100",
        coverDrawableRes = R.drawable.img_novel_celestial,
        coverColorHex = 0xFF161936,
        chapterTitle = "Chapter I • The Stardust Astrolabe",
        totalPages = 412,
        excerpt = "When the brass gears of the great telescope aligned with the violet nebula, the universe ceased to be silent.",
        contentText = listOf(
          "[chapter:Chapter I • The Stardust Astrolabe]",
          "When the brass gears of the great telescope aligned with the violet nebula, the universe ceased to be silent. High above the cloudline, atop the crags of Mount Alcor, the Great Astrolabe of the Guild rotated with a steady, hydraulic hum.",
          "Professor Vane wiped condensation from the ocular lens. For forty-three years he had mapped the wandering stars, recording planetary transits with iron ink on vellum folios. But tonight, a new spectrum was bleeding into the constellation of Cygnus—a pulse of azure light that defied astronomical theory.",
          "[photo:drawable:img_novel_celestial:Plate I • The Great Astrolabe and the Swirling Sapphire Nebula]",
          "[chapter:Chapter II • The Meridian Telegraph]",
          "The mechanical ticker in the observatory corner sprang to life, its needle punching perforated tape with frantic rhythm. Signals from the southern listening posts in Patagonia and the deep desert of Atacama were converging on the exact same frequency.",
          "'It is not an optical anomaly,' whispered his apprentice Lyra, her fingers trembling as she measured the spectral lines with a bronze caliper. 'The meridian itself is shifting. The stars are sending coordinates.'",
          "[chapter:Chapter III • Beyond the Threshold of Orion]",
          "To look deeply into the cosmos is to realize that humanity's triumphs and tragedies are cast upon a very small, fragile stage. Yet within that tiny theater lies the audacious power of the human mind: to measure infinity from a stone tower, guided only by brass, mirrors, and curiosity."
        ).joinToString("\n\n"),
        isOwnerUploaded = false,
        createdAt = 6000L,
        readsCount = 0,
        favoritesCount = 0
      ),
      NovelEntity(
        id = "nov_whispering_pines",
        title = "Whispering Pines",
        subtitle = "Echoes Across the Mist of the Northern Fjord",
        author = "Strawberrycandy",
        originalAuthor = "Astrid Lindqvist",
        authorSlot = 0,
        year = "2026",
        editionNumber = "ARCHIVE NO. 052 / 100",
        coverDrawableRes = R.drawable.img_novel_whispering_pines,
        coverColorHex = 0xFF152A24,
        chapterTitle = "Chapter I • The Solitary Lantern at Fog's Edge",
        totalPages = 295,
        excerpt = "In the quiet heart of the evergreen wilderness, the water remembers every traveler who never returned.",
        contentText = listOf(
          "[chapter:Chapter I • The Solitary Lantern at Fog's Edge]",
          "In the quiet heart of the evergreen wilderness, the water remembers every traveler who never returned. At twilight, when the fog curls like smoke off the black surface of Lake Siljan, the spruce trees along the ridge begin their patient dialogue.",
          "Maren pushed her wooden skiff off the shingle. Her grandfather had kept the timber cabin on the western island for sixty winters, leaving behind only a cedar chest of notebooks and a brass nautical lantern that burned with a stubborn emerald flame.",
          "[photo:drawable:img_novel_whispering_pines:Plate I • The Solitary Dock and Misty Emerald Pines]",
          "[chapter:Chapter II • Footprints on Granite]",
          "The scent of cedar bark and damp lichen was intoxicating. She tied the boat to the mossy post and lit the lantern. Across the water, the aurora began to flicker—a ribbon of ghostly jade and violet undulating between the craggy peaks.",
          "She opened the first journal. In meticulous cursive, her grandfather had recorded not water levels or timber yields, but songs overheard when no one was singing: 'The forest does not keep secrets; it simply waits for listeners who are quiet enough to hear.'",
          "[chapter:Chapter III • The Island of Still Waters]",
          "Here, beyond cell towers and asphalt highways, time ceased its frantic countdown. Each breath filled the lungs with the pure chill of ancient glaciers, anchoring the soul in the enduring permanence of rock and pine."
        ).joinToString("\n\n"),
        isOwnerUploaded = false,
        createdAt = 7000L,
        readsCount = 0,
        favoritesCount = 0
      ),
      NovelEntity(
        id = "nov_moonlight",
        title = "The Moonlight Pavilion",
        subtitle = "Letters Written Beneath the Lotus Bower",
        author = "Strawberrycandy",
        originalAuthor = "Lin Shen-Yue",
        authorSlot = 0,
        year = "2026",
        editionNumber = "ARCHIVE NO. 091 / 100",
        coverDrawableRes = R.drawable.img_novel_moonlight,
        coverColorHex = 0xFF2A1C30,
        chapterTitle = "Chapter I • Floating Lanterns and Unspoken Vows",
        totalPages = 268,
        excerpt = "Each lantern set adrift upon the lake bore a single character—a promise carried toward dawn.",
        contentText = listOf(
          "[chapter:Chapter I • Floating Lanterns and Unspoken Vows]",
          "Each lantern set adrift upon the lake bore a single character—a promise carried toward dawn. In the fourteenth year of the Emperor's reign, the pavilion on the western causeway was the only sanctuary where scholars dared speak without masks.",
          "Mei-Ling arranged the ink stone, grinding pine soot with rainwater gathered from the eaves of the temple. Across the wooden railing, the lotus blossoms were closing for the night, their pink tips folding into serene contemplation.",
          "[photo:drawable:img_novel_moonlight:Plate I • Ornate Pavilion and Drifting Lanterns on Lotus Water]",
          "[chapter:Chapter II • The Calligraphy of Longing]",
          "'True words,' Master Wen had taught her, 'must be brushed with the weight of mountains and the lightness of plum blossoms.' But tonight her brush hesitated over the mulberry paper. How could five characters capture ten years of absence?",
          "A warm night breeze carried the scent of gardenia and wet stone. From across the dark water, the gentle dip of an oar broke the stillness. Someone was rowing toward the pavilion under cover of the crescent moon.",
          "[chapter:Chapter III • The Dawn of Quiet Resolve]",
          "As the first blush of apricot spread along the horizon, the lantern fire flickered out. What remained was the clarity of parchment, the scent of fresh ink, and the realization that some stories are written not to be remembered by history, but to keep the human spirit alive."
        ).joinToString("\n\n"),
        isOwnerUploaded = false,
        createdAt = 8000L,
        readsCount = 0,
        favoritesCount = 0
      )
    )

    for (novel in featuredNovels) {
      val existing = dao.getNovelById(novel.id)
      if (existing == null) {
        dao.insertNovel(novel)
      } else if (existing.coverDrawableRes != novel.coverDrawableRes) {
        dao.insertNovel(existing.copy(coverDrawableRes = novel.coverDrawableRes, coverColorHex = novel.coverColorHex))
      }
    }
  }

  private suspend fun upgradeDefaultNovelsWithChapters() {
    val nov1 = dao.getNovelById("nov_1")
    if (nov1 != null && !nov1.contentText.contains("[chapter:")) {
      val updatedNov1 = nov1.copy(
        chapterTitle = "Chapter I • The Cloistered Arcades of Thoronet",
        contentText = listOf(
          "[chapter:Chapter I • The Cloistered Arcades of Thoronet]",
          "To construct a room for silence is not merely to subtract sound, but to tune the subtle resonance of what remains. In the cloistered arcades of Thoronet and the vaulted corridors of Sénanque, stone does not absorb speech; it receives it as a transient vibration, smoothing harshness into an echo that returns only as ambient presence.",
          "When an author steps into such enclosures, the cadence of thought slows to match the thermal mass of granite. We are accustomed in contemporary life to an architecture of velocity—glass surfaces that reflect only haste, partitions that transmit anxiety.",
          "[chapter:Chapter II • Deep Limestone Splays & Morning Light]",
          "Here, conversely, the wall possesses gravity. A single window, cut deep into limestone with a 45-degree splay, gathers the morning light and diffuses it across whitewashed lime plaster with an evenness that renders artificial illumination unnecessary. In this sanctuary, the page becomes the floorplan of an inner chamber.",
          "Consider the margin of the printed page. It is not wasted paper; it is the physical moat that shields the text from the noise of the surrounding world. Just as a Japanese teahouse requires an entry crawl-space (nijiriguchi) to humble the visitor, a worthy book demands an expanse of blank cream paper before the first sentence may begin.",
          "[chapter:Chapter III • The Acoustics of Stillness & Sacred Margins]",
          "We read, ultimately, not to consume data, but to occupy a space designed by an architect of language. The sentences must carry structural integrity: verbs acting as load-bearing columns, subordinate clauses as cantilevered balconies overlooking quiet courtyards of reflection.",
          "In this proprietary edition, the text is sealed against casual dissemination. It exists only for the deliberate eye, held within an authenticated viewport where words retain their weight, unclouded by the frantic mechanisms of the digital crowd."
        ).joinToString("\n\n")
      )
      dao.insertNovel(updatedNov1)
    }

    val nov2 = dao.getNovelById("nov_2")
    if (nov2 != null && !nov2.contentText.contains("[chapter:")) {
      val updatedNov2 = nov2.copy(
        chapterTitle = "Chapter I • The Geometry of Solitude",
        contentText = listOf(
          "[chapter:Chapter I • The Geometry of Solitude]",
          "Form is what happens when solitude is granted sufficient time to harden into outline. Left to itself, human contemplation is diffuse, mist-like, drifting across memories and unfinished gestures without settling into durable shape.",
          "Only under the deliberate pressure of seclusion does the material crystallize. The sculptor knows that marble does not yield to haste; every chisel blow is a subtraction that reveals an inevitable contour already dormant within the quarry.",
          "[photo:drawable:img_book_2:Plate I • Contemplation of the Stone Quarry & Solitary Form]",
          "[chapter:Chapter II • The Chisel and the Marble Contour]",
          "In our era of hyper-connectivity, solitude is often treated as an error code—a malfunction to be corrected by immediate notification. Yet throughout intellectual history, every profound contribution to philosophy and literature arose from sustained, uninterrupted seclusion.",
          "The mind needs borders just as a painting requires a frame. Without limits, imagination dissipates into ambient noise. By establishing rigorous artistic constraints—restricting our shelf to three works, purifying our canvas of extraneous buttons—we return dignity to the reading act.",
          "[chapter:Chapter III • The Contractual Stillness of Reading]",
          "The reader who opens this volume enters a contractual stillness. No alerts will arrive; no algorithms will measure your scroll speed to optimize engagement. There is only the charcoal ligature upon cream fiber, and the silent covenant between author and witness."
        ).joinToString("\n\n")
      )
      dao.insertNovel(updatedNov2)
    }

    val nov3 = dao.getNovelById("nov_3")
    if (nov3 != null && !nov3.contentText.contains("[chapter:")) {
      val updatedNov3 = nov3.copy(
        chapterTitle = "Chapter I • Slant Light at Dawn",
        contentText = listOf(
          "[chapter:Chapter I • Slant Light at Dawn]",
          "The most enduring ideas are those written with the lightness of dust floating across a sunbeam. They do not demand assent with theatrical rhetoric; they illuminate quietly, allowing the reader to discover the truth as if remembering a long-forgotten dream.",
          "[photo:drawable:img_book_3:Plate I • Slant Light across the Ancient Library Gallery at Dawn]",
          "[chapter:Chapter II • The Golden Foil of Forgotten Folios]",
          "At dawn, before the city awakens, light enters sideways. It strikes the spine of a book shelved three decades ago, gilding the gold foil embossing with a momentary flash of gold. For five minutes, that forgotten object becomes the brightest thing in the library.",
          "[chapter:Chapter III • Translucence and Memory]",
          "Literature is that slant light. It illuminates what was already present in the reader's spirit, giving names to sensations that were previously mute. In designing these digital manuscripts, we sought the tactile tranquility of that early morning study.",
          "The subtle grain of paper, the restraint of charcoal typography, and the absence of intrusive interface machinery all conspire toward one end: that nothing shall stand between the radiance of the sentence and the stillness of the reader's mind."
        ).joinToString("\n\n")
      )
      dao.insertNovel(updatedNov3)
    }

    val novSchema = dao.getNovelById("nov_schema")
    if (novSchema != null && !novSchema.contentText.contains("[chapter:")) {
      val updatedNovSchema = novSchema.copy(
        chapterTitle = "Chapter I • Physicality in Software",
        contentText = listOf(
          "[chapter:Chapter I • Physicality in Software & Ephemeral UI]",
          "Substance over surface: The decline of ephemeral UI.",
          "An exploration into durable software tools, tactile digital interfaces, and how the pursuit of hyper-velocity eroded deliberate human-computer ergonomics.",
          "[photo:drawable:img_book_1:Plate I • Monolith Architecture Form & Spatial Persistence in Software]",
          "[chapter:Chapter II • Determinism & Ambient Calmness]",
          "Why predicting state mutation visually creates calmness: in deterministic interfaces, the ambient cognitive overhead of web applications drops to zero.",
          "Opticals, Weights, and the Modern Sans: Tracing font rasterization behaviors from high-DPI glass screens back to Swiss modernism's raw strictness.",
          "[chapter:Chapter III • Local-First Sovereignty & Tactile Friction]",
          "Physical Levers in an Intangible Medium: The tactile feedback loops that mechanical craftsmen understood decades before mobile micro-haptics attempted replication.",
          "Local-First Software and Sovereignty: Ownership paradigms in an era where network rent-seeking threatens the longevity of personal intellectual work.",
          "Designing for Deliberate Friction: How frictionless flows encourage thoughtless actions, and where modern product designers must reintroduce hesitation.",
          "The Monochrome Paradigm in App Design: Restricting color palette usage exclusively to semantic states as a discipline to uncover poor visual hierarchies."
        ).joinToString("\n\n")
      )
      dao.insertNovel(updatedNovSchema)
    }
  }

  private suspend fun removePhotosUnderThoughtIfPresent() {
    val nov1 = dao.getNovelById("nov_1")
    if (nov1 != null && (nov1.contentText.contains("Monastic Stone Arcades") || nov1.contentText.contains("Deep Limestone Splay"))) {
      val cleanedContent = nov1.contentText
        .replace("[photo:drawable:img_book_1:Plate I • Monastic Stone Arcades & The Acoustics of Thoronet]\n\n", "")
        .replace("[photo:drawable:img_book_1:Plate I • Monastic Stone Arcades & The Acoustics of Thoronet]", "")
        .replace("[photo:drawable:img_book_2:Plate II • Deep Limestone Splay Diffusing Morning Light]\n\n", "")
        .replace("[photo:drawable:img_book_2:Plate II • Deep Limestone Splay Diffusing Morning Light]", "")
      dao.insertNovel(nov1.copy(contentText = cleanedContent))
    }
  }

  private suspend fun ensureQuickFindCategoriesExist() {
    val quickFindMappings = mapOf(
      "nov_crimson" to "Historical Romance • Chronicles of the Spider Lily & Midnight Courtyard",
      "nov_moonlight" to "Modern Romance • Letters Written Beneath the Lotus Bower",
      "nov_celestial" to "Fantasy Romance • Voyages Beyond the Astral Meridian",
      "nov_whispering_pines" to "Historical Romance • Echoes Across the Mist of the Northern Fjord",
      "nov_2" to "R19 • Studies in Void, Weight, and Proportion",
      "nov_3" to "Modern Romance • Notes on Radiance, Dawn, and Impermanence",
      "nov_1" to "Fantasy Romance • Monastic Spacing & Solitary Thought"
    )
    for ((id, subtitle) in quickFindMappings) {
      val novel = dao.getNovelById(id)
      if (novel != null && !novel.subtitle.contains("Romance") && !novel.subtitle.contains("R19")) {
        dao.insertNovel(novel.copy(subtitle = subtitle))
      }
    }
  }

  private suspend fun seedDefaultAuthorSlotsIfEmpty() {
    val defaultSlots = listOf(
      AuthorSlotEntity(
        slotNumber = 0,
        authorName = "Clarify",
        penName = "Strawberrycandy",
        bio = "Founder & Sole Owner at Strawberrycandy Archive. Oversees manuscript acquisitions, translator permissions, and curation.",
        avatarColorHex = 0xFF8C2D48,
        accessCode = "ARCHIVE-OWNER-0",
        isClaimed = true,
        isPermissionGranted = true,
        translatorEmail = "clarifymanga@gmail.com"
      ),
      AuthorSlotEntity(
        slotNumber = 1,
        authorName = "Aria Thorne",
        penName = "Aria Thorne",
        bio = "Atmospheric gothic fiction, liminal romances, and cathedral solitude.",
        avatarColorHex = 0xFF5C2D3B,
        accessCode = "AUTH-ROOM-1",
        isClaimed = true,
        isPermissionGranted = true,
        translatorEmail = "aria.thorne@gmail.com"
      ),
      AuthorSlotEntity(
        slotNumber = 2,
        authorName = "Felix Moreau",
        penName = "Felix Moreau",
        bio = "Speculative architectural essays, existential monographs, and twilight studies.",
        avatarColorHex = 0xFF28362D,
        accessCode = "AUTH-ROOM-2",
        isClaimed = true,
        isPermissionGranted = true,
        translatorEmail = "felix.moreau@gmail.com"
      ),
      AuthorSlotEntity(
        slotNumber = 3,
        authorName = "Clara O'Connor",
        penName = "Clara O'Connor",
        bio = "Chronicles of quiet coastal landscapes, mist-shrouded poetry, and dawn light.",
        avatarColorHex = 0xFF21252D,
        accessCode = "AUTH-ROOM-3",
        isClaimed = true,
        isPermissionGranted = true,
        translatorEmail = "clara.oconnor@gmail.com"
      ),
      AuthorSlotEntity(
        slotNumber = 4,
        authorName = "Dante Valeri",
        penName = "Dante Valeri",
        bio = "Modernist psychological realism, chamber memoirs, and classical dialogue.",
        avatarColorHex = 0xFF4A3428,
        accessCode = "AUTH-ROOM-4",
        isClaimed = true,
        isPermissionGranted = true,
        translatorEmail = "dante.valeri@gmail.com"
      ),
      AuthorSlotEntity(
        slotNumber = 5,
        authorName = "Evelyn Vance",
        penName = "Evelyn Vance",
        bio = "Mythic folklore adaptations, lyrical historical drama, and translated ballads.",
        avatarColorHex = 0xFF3D405B,
        accessCode = "AUTH-ROOM-5",
        isClaimed = false,
        isPermissionGranted = false,
        translatorEmail = "evelyn.vance@gmail.com"
      ),
      AuthorSlotEntity(
        slotNumber = 6,
        authorName = "Julian Croft",
        penName = "Julian Croft",
        bio = "Philosophical noir novellas, urban rain memoirs, and midnight translations.",
        avatarColorHex = 0xFF4A5859,
        accessCode = "AUTH-ROOM-6",
        isClaimed = false,
        isPermissionGranted = false,
        translatorEmail = "julian.croft@gmail.com"
      ),
      AuthorSlotEntity(
        slotNumber = 7,
        authorName = "Mira Hashimoto",
        penName = "Mira Hashimoto",
        bio = "Quiet magical realism, tea garden parables, and celestial vignettes.",
        avatarColorHex = 0xFF6B4E71,
        accessCode = "AUTH-ROOM-7",
        isClaimed = false,
        isPermissionGranted = false,
        translatorEmail = "mira.hashimoto@gmail.com"
      ),
      AuthorSlotEntity(
        slotNumber = 8,
        authorName = "Lucian Bell",
        penName = "Lucian Bell",
        bio = "Neo-Victorian detective mysteries and classical gothic archives.",
        avatarColorHex = 0xFF583E26,
        accessCode = "AUTH-ROOM-8",
        isClaimed = false,
        isPermissionGranted = false,
        translatorEmail = "lucian.bell@gmail.com"
      ),
      AuthorSlotEntity(
        slotNumber = 9,
        authorName = "Sophie Lind",
        penName = "Sophie Lind",
        bio = "Nordic minimalist fiction, winter journals, and island solitude.",
        avatarColorHex = 0xFF2E4057,
        accessCode = "AUTH-ROOM-9",
        isClaimed = false,
        isPermissionGranted = false,
        translatorEmail = "sophie.lind@gmail.com"
      ),
      AuthorSlotEntity(
        slotNumber = 10,
        authorName = "Rowan Mercer",
        penName = "Rowan Mercer",
        bio = "Antiquity chronicles, lost library manuscripts, and epistolary tragedies.",
        avatarColorHex = 0xFF4B3832,
        accessCode = "AUTH-ROOM-10",
        isClaimed = false,
        isPermissionGranted = false,
        translatorEmail = "rowan.mercer@gmail.com"
      )
    )

    if (dao.getAuthorSlotCount() == 0) {
      dao.insertAuthorSlots(defaultSlots)
    } else {
      // Ensure all 10 translator slots plus slot 0 exist and have emails
      for (slot in defaultSlots) {
        val current = dao.getAuthorSlot(slot.slotNumber)
        if (current == null) {
          dao.updateAuthorSlot(slot)
        } else if (current.translatorEmail.isNullOrBlank()) {
          dao.updateAuthorSlot(current.copy(translatorEmail = slot.translatorEmail))
        }
      }
    }
  }

  private suspend fun syncOwnerConfiguration() {
    val currentSlot0 = dao.getAuthorSlot(0)
    if (currentSlot0 != null) {
      dao.updateAuthorSlot(
        currentSlot0.copy(
          authorName = "Clarify",
          penName = "Strawberrycandy",
          bio = "Founder & Sole Owner at Strawberrycandy Archive. Oversees manuscript acquisitions, translator permissions, and curation.",
          isPermissionGranted = true
        )
      )
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
    val cleanReaderName = if (readerName.contains("@")) {
      val prefix = readerName.substringBefore("@").replace(".", " ").trim()
      prefix.ifBlank { "Literary Reader" }
    } else {
      readerName.trim().ifEmpty { "Literary Reader" }
    }
    val comment = ChapterCommentEntity(
      id = "cmt_" + UUID.randomUUID().toString().take(8),
      novelId = novelId,
      chapterTitle = chapterTitle,
      readerName = cleanReaderName,
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

  fun getCommentsForReader(email: String, name: String): Flow<List<ChapterCommentEntity>> {
    return dao.getCommentsForReader(email, name)
  }

  suspend fun deleteComment(commentId: String) {
    dao.deleteComment(commentId)
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
