package com.example.data

import com.example.R
import com.example.data.local.AuthorSlotEntity
import com.example.data.local.BookmarkHighlightEntity
import com.example.data.local.ChapterCommentEntity
import com.example.data.local.NovelEntity
import com.example.data.local.ReaderProfileEntity
import com.example.data.local.StrawberrycandyDao
import com.example.data.local.UserReadingStateEntity
import com.example.data.remote.CloudArchiveSyncService
import com.example.model.NovelWithState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.util.UUID

data class UploadNovelResult(
  val novelId: String,
  val novel: NovelEntity,
  val isPublishedToCloud: Boolean,
  val cloudStatusMessage: String,
  val novelJson: String,
  val fullCatalogJson: String,
)

class StrawberrycandyRepository(
  private val dao: StrawberrycandyDao,
  private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
  private val syncService: CloudArchiveSyncService? = null,
) {

  fun getSyncService(): CloudArchiveSyncService? = syncService

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
      // Purge all sample novels and sample comments
      dao.deleteSampleNovels()
      dao.deleteSampleReadingStates()
      dao.deleteSampleComments()
      dao.deleteSampleBookmarks()
      seedDefaultAuthorSlotsIfEmpty()
      dao.removeFakeSeededReadingState()
      dao.resetArtificialReads()
      dao.syncAllRealFavorites()
      dao.sanitizeSlotBios()
      dao.sanitizeSlotPenNames()
      dao.sanitizeCommentNames()
      syncOwnerConfiguration()
      eraseExampleTranslators()
      syncRemoteNovels()
    }
  }

  suspend fun syncRemoteNovels(): Result<Int> {
    if (syncService == null) return Result.success(0)
    return try {
      val remoteResult = syncService.fetchRemoteNovels()
      if (remoteResult.isSuccess) {
        val remoteNovels = remoteResult.getOrNull() ?: emptyList()
        if (remoteNovels.isNotEmpty()) {
          dao.insertNovels(remoteNovels)
        }
        Result.success(remoteNovels.size)
      } else {
        Result.failure(remoteResult.exceptionOrNull() ?: Exception("Failed to sync remote novels"))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  val activeUser: Flow<ReaderProfileEntity?> = dao.getActiveReaderProfile()

  val rememberedAccounts: Flow<List<ReaderProfileEntity>> = dao.getAllRememberedAccounts()

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
  ): UploadNovelResult {
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

    var isPublished = false
    var statusMsg = "Saved to local cache. Pending global cloud publication."
    var novelJson = ""
    var fullCatalogJson = "[]"

    if (syncService != null) {
      val allNovels = dao.getAllNovelsSync()
      val pushResult = syncService.publishNovelToRemote(novel, allNovels)
      if (pushResult.isSuccess) {
        isPublished = true
        statusMsg = pushResult.getOrNull() ?: "Published to Global Cloud Archive"
      } else {
        statusMsg = pushResult.exceptionOrNull()?.message ?: "Pending global cloud publication"
      }
      novelJson = syncService.novelToJson(novel).toString(2)
      fullCatalogJson = syncService.exportNovelsToJsonString(allNovels)
    }

    return UploadNovelResult(
      novelId = novelId,
      novel = novel,
      isPublishedToCloud = isPublished,
      cloudStatusMessage = statusMsg,
      novelJson = novelJson,
      fullCatalogJson = fullCatalogJson,
    )
  }

  suspend fun publishNovelDirectToCloud(
    novelId: String,
    token: String? = null,
    writeUrl: String? = null
  ): Result<String> {
    if (syncService == null) return Result.failure(IllegalStateException("Cloud sync service unavailable"))
    if (!token.isNullOrBlank()) syncService.setGitHubToken(token)
    if (!writeUrl.isNullOrBlank()) syncService.setWriteUrl(writeUrl)

    val novel = dao.getNovelById(novelId) ?: return Result.failure(IllegalArgumentException("Novel not found"))
    val allNovels = dao.getAllNovelsSync()
    return syncService.publishNovelToRemote(novel, allNovels)
  }

  suspend fun exportAllNovelsJson(): String {
    val allNovels = dao.getAllNovelsSync()
    return syncService?.exportNovelsToJsonString(allNovels) ?: "[]"
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
      if (existing.translatorEmail != null) {
        val prof = dao.getReaderProfileByEmail(existing.translatorEmail)
        if (prof != null) {
          dao.insertReaderProfile(prof.copy(displayName = cleanPenName))
        }
      }
    }
  }

  suspend fun updateAuthorSlotCover(slotNumber: Int, coverImageUri: String) {
    dao.updateAuthorSlotCover(slotNumber, coverImageUri)
  }

  suspend fun addChapterToNovel(novelId: String, chapterTitle: String, chapterContent: String) {
    val existing = dao.getNovelById(novelId) ?: return
    val cleanTitle = chapterTitle.trim().ifBlank { "Next Chapter" }
    val cleanContent = chapterContent.trim()
    val separator = if (existing.contentText.isNotBlank()) "\n\n" else ""
    val formattedChapter = "$separator[chapter: $cleanTitle]\n\n$cleanContent"
    val newFullContent = existing.contentText + formattedChapter
    val newTotalPages = maxOf(existing.totalPages + 1, newFullContent.split("\n\n").count { it.isNotBlank() } * 2)
    val updated = existing.copy(
      contentText = newFullContent,
      totalPages = newTotalPages
    )
    dao.insertNovel(updated)
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

    // Check existing profile for this email or userId
    val existingProfile = dao.getReaderProfileByEmail(cleanEmail) ?: dao.getReaderProfile(userId)
    if (!isOwner && existingProfile != null && !existingProfile.passwordHash.isNullOrBlank() && password.isNotBlank()) {
      if (existingProfile.passwordHash != password) {
        return Result.failure(
          IllegalArgumentException(
            "Incorrect password for $cleanEmail. The password entered must match your account password. If you forgot your password, tap 'Forgot Password?' to retrieve it."
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
    } else if (existingProfile?.role == "TRANSLATOR") {
      "TRANSLATOR"
    } else if (role == "TRANSLATOR") {
      "TRANSLATOR"
    } else {
      "READER"
    }

    val finalSlot = if (finalRole == "OWNER") {
      0
    } else if (finalRole == "TRANSLATOR") {
      preGrantedSlot?.slotNumber ?: existingProfile?.authorSlot ?: authorSlot
    } else {
      null
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

    // Set other accounts to logged out so only this profile is active, but accounts remain saved and remembered
    dao.logoutAllProfiles()

    val profile = ReaderProfileEntity(
      userId = userId,
      displayName = finalName,
      email = cleanEmail,
      provider = provider,
      role = finalRole,
      authorSlot = finalSlot,
      penNamePoints = existingPoints,
      lastLoginTimestamp = System.currentTimeMillis(),
      passwordHash = password.ifBlank { existingProfile?.passwordHash ?: "account_pass" },
      isLoggedIn = true
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

  // Active recovery sessions in memory (email -> RecoverySession)
  private data class RecoverySession(val email: String, val code: String, val timestamp: Long)
  private val activeRecoverySessions = java.util.concurrent.ConcurrentHashMap<String, RecoverySession>()

  suspend fun sendPasswordRecoveryCode(email: String): Result<String> {
    val cleanEmail = email.trim().lowercase()
    if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
      return Result.failure(IllegalArgumentException("Please enter a valid Gmail address."))
    }
    val existingProfile = dao.getReaderProfileByEmail(cleanEmail)
    val isOwner = isOwnerEmail(cleanEmail)
    if (existingProfile == null && !isOwner) {
      return Result.failure(
        IllegalArgumentException("No account found for $cleanEmail on this device. Please sign in or register with your Gmail address.")
      )
    }
    // Generate secure 6-digit recovery code
    val code = (100000..999999).random().toString()
    activeRecoverySessions[cleanEmail] = RecoverySession(cleanEmail, code, System.currentTimeMillis())
    return Result.success(code)
  }

  suspend fun resetPasswordWithCode(email: String, code: String, newPassword: String): Result<Unit> {
    val cleanEmail = email.trim().lowercase()
    val session = activeRecoverySessions[cleanEmail]
    if (session == null) {
      return Result.failure(IllegalArgumentException("No active recovery request found for $cleanEmail. Please request a new verification code."))
    }
    // Code expires after 15 minutes
    if (System.currentTimeMillis() - session.timestamp > 15 * 60 * 1000) {
      activeRecoverySessions.remove(cleanEmail)
      return Result.failure(IllegalArgumentException("The verification code has expired. Please request a new code."))
    }
    if (session.code != code.trim()) {
      return Result.failure(IllegalArgumentException("Invalid verification code. Please check the 6-digit code sent to $cleanEmail."))
    }
    val trimmedPass = newPassword.trim()
    if (trimmedPass.length < 4) {
      return Result.failure(IllegalArgumentException("Password must be at least 4 characters."))
    }

    val isOwner = isOwnerEmail(cleanEmail)
    val existingProfile = dao.getReaderProfileByEmail(cleanEmail)
    dao.logoutAllProfiles()

    if (existingProfile != null) {
      val updated = existingProfile.copy(
        passwordHash = trimmedPass,
        isLoggedIn = true,
        lastLoginTimestamp = System.currentTimeMillis()
      )
      dao.insertReaderProfile(updated)
    } else {
      val userId = "usr_google_" + cleanEmail.replace(Regex("[^a-z0-9]"), "_")
      val profile = ReaderProfileEntity(
        userId = userId,
        displayName = if (isOwner) "Clarify" else cleanEmail.substringBefore("@").replace(".", " ").replaceFirstChar { it.uppercase() },
        email = cleanEmail,
        provider = "GOOGLE",
        role = if (isOwner) "OWNER" else "READER",
        authorSlot = if (isOwner) 0 else null,
        passwordHash = trimmedPass,
        isLoggedIn = true,
        lastLoginTimestamp = System.currentTimeMillis()
      )
      dao.insertReaderProfile(profile)
    }
    activeRecoverySessions.remove(cleanEmail)
    return Result.success(Unit)
  }

  suspend fun signOut() {
    // Preserve all accounts and their credentials, only mark current session as logged out
    dao.logoutAllProfiles()
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
      )
    ) + (1..10).map { slotNum ->
      AuthorSlotEntity(
        slotNumber = slotNum,
        authorName = "",
        penName = "",
        bio = "",
        avatarColorHex = 0xFF353C48,
        accessCode = "AUTH-ROOM-$slotNum",
        isClaimed = false,
        isPermissionGranted = false,
        translatorEmail = null
      )
    }

    if (dao.getAuthorSlotCount() == 0) {
      dao.insertAuthorSlots(defaultSlots)
    } else {
      for (slot in defaultSlots) {
        val current = dao.getAuthorSlot(slot.slotNumber)
        if (current == null) {
          dao.updateAuthorSlot(slot)
        }
      }
    }
  }

  private suspend fun eraseExampleTranslators() {
    val exampleNames = setOf(
      "Aria Thorne", "Felix Moreau", "Clara O'Connor", "Dante Valeri",
      "Evelyn Vance", "Julian Croft", "Mira Hashimoto", "Lucian Bell",
      "Sophie Lind", "Rowan Mercer"
    )
    val exampleEmails = setOf(
      "aria.thorne@gmail.com", "felix.moreau@gmail.com", "clara.oconnor@gmail.com",
      "dante.valeri@gmail.com", "evelyn.vance@gmail.com", "julian.croft@gmail.com",
      "mira.hashimoto@gmail.com", "lucian.bell@gmail.com", "sophie.lind@gmail.com",
      "rowan.mercer@gmail.com"
    )
    val slots = dao.getAllAuthorSlotsSync()
    for (slot in slots) {
      if (slot.slotNumber > 0) {
        val hasExampleName = slot.penName in exampleNames || slot.authorName in exampleNames
        val hasExampleEmail = slot.translatorEmail != null && slot.translatorEmail.lowercase() in exampleEmails
        if (hasExampleName || hasExampleEmail) {
          dao.updateAuthorSlot(
            slot.copy(
              authorName = "",
              penName = "",
              bio = "",
              coverImageUri = null,
              translatorEmail = null,
              isClaimed = false,
              isPermissionGranted = false
            )
          )
        }
      }
    }
  }

  suspend fun updateAuthorSlotByOwner(
    slotNumber: Int,
    translatorEmail: String?,
    penName: String,
    bio: String,
    isPermissionGranted: Boolean
  ) {
    val existing = dao.getAuthorSlot(slotNumber) ?: return
    val cleanEmail = translatorEmail?.trim()?.lowercase()?.ifBlank { null }
    val cleanPenName = penName.trim()
    val cleanBio = bio.trim()
    val updated = existing.copy(
      translatorEmail = cleanEmail,
      penName = cleanPenName,
      authorName = cleanPenName,
      bio = cleanBio,
      isPermissionGranted = isPermissionGranted,
      isClaimed = cleanEmail != null
    )
    dao.updateAuthorSlot(updated)

    if (cleanEmail != null) {
      val existingProfile = dao.getReaderProfileByEmail(cleanEmail)
      if (existingProfile != null && existingProfile.role != "OWNER") {
        val updatedRole = if (isPermissionGranted) "TRANSLATOR" else "READER"
        val updatedSlot = if (isPermissionGranted) slotNumber else null
        dao.insertReaderProfile(
          existingProfile.copy(
            role = updatedRole,
            authorSlot = updatedSlot,
            displayName = if (cleanPenName.isNotBlank()) cleanPenName else existingProfile.displayName
          )
        )
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
    parentCommentId: String? = null,
    replyToReaderName: String? = null,
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
      isLikedByMe = false,
      parentCommentId = parentCommentId,
      replyToReaderName = replyToReaderName,
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
    return saveHighlight(
      novelId = novelId,
      chapterTitle = chapterTitle,
      quoteText = quoteText,
      paragraphIndex = paragraphIndex,
      colorHex = 0xFFD4AF37,
      note = note
    )
  }

  suspend fun saveHighlight(
    novelId: String,
    chapterTitle: String,
    quoteText: String,
    paragraphIndex: Int,
    colorHex: Long,
    note: String = ""
  ): Boolean {
    val existing = dao.findBookmarkByQuote(novelId, quoteText.trim())
    return if (existing != null) {
      if (existing.colorHex == colorHex) {
        // Tapping same color removes highlight
        dao.deleteBookmark(existing.id)
        false
      } else {
        // Change highlight color
        dao.insertBookmark(existing.copy(colorHex = colorHex, timestamp = System.currentTimeMillis()))
        true
      }
    } else {
      val bookmark = BookmarkHighlightEntity(
        id = "bmk_" + UUID.randomUUID().toString().take(8),
        novelId = novelId,
        chapterTitle = chapterTitle,
        quoteText = quoteText.trim(),
        paragraphIndex = paragraphIndex,
        timestamp = System.currentTimeMillis(),
        colorHex = colorHex,
        note = note
      )
      dao.insertBookmark(bookmark)
      true
    }
  }

  suspend fun deleteBookmark(bookmarkId: String) {
    dao.deleteBookmark(bookmarkId)
  }
}
