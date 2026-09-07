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
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
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
  private val firebaseStorageService: com.example.data.remote.FirebaseCloudStorageService? = null,
) {

  fun getSyncService(): CloudArchiveSyncService? = syncService
  fun getFirebaseStorageService(): com.example.data.remote.FirebaseCloudStorageService? = firebaseStorageService

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

    fun normalizeEmailOrName(input: String?): String {
      if (input.isNullOrBlank()) return ""
      return input.trim().lowercase()
    }

    fun extractEmailPrefix(input: String?): String {
      if (input.isNullOrBlank()) return ""
      val clean = input.trim().lowercase()
      return if (clean.contains("@")) clean.substringBefore("@") else clean
    }

    fun isUserMatchedToSlot(
      userEmail: String?,
      userDisplayName: String?,
      slot: AuthorSlotEntity
    ): Boolean {
      val cleanUserEmail = normalizeEmailOrName(userEmail)
      val userPrefix = extractEmailPrefix(userEmail)
      val userSimplifiedPrefix = userPrefix.replace(".", "").replace("_", "").replace("-", "")
      val cleanDisplayName = normalizeEmailOrName(userDisplayName)
      val simplifiedDisplayName = cleanDisplayName.replace(" ", "").replace(".", "").replace("_", "").replace("-", "")

      val slotEmail = normalizeEmailOrName(slot.translatorEmail)
      val slotPrefix = extractEmailPrefix(slot.translatorEmail)
      val slotSimplifiedPrefix = slotPrefix.replace(".", "").replace("_", "").replace("-", "")

      val slotPen = normalizeEmailOrName(slot.penName)
      val slotSimplifiedPen = slotPen.replace(" ", "").replace(".", "").replace("_", "").replace("-", "")

      val slotAuthor = normalizeEmailOrName(slot.authorName)
      val slotSimplifiedAuthor = slotAuthor.replace(" ", "").replace(".", "").replace("_", "").replace("-", "")

      // 1. Direct email and prefix matches
      if (slotEmail.isNotBlank()) {
        if (slotEmail == cleanUserEmail) return true
        val strippedSlot = slotEmail.removeSuffix("@gmail.com").removeSuffix("@googlemail.com")
        val strippedUser = cleanUserEmail.removeSuffix("@gmail.com").removeSuffix("@googlemail.com")
        if (strippedSlot == strippedUser) return true

        if (slotPrefix.isNotBlank() && slotPrefix == userPrefix) return true
        if (slotSimplifiedPrefix.isNotBlank() && slotSimplifiedPrefix == userSimplifiedPrefix) return true

        if (cleanDisplayName.isNotBlank()) {
          if (slotEmail == cleanDisplayName || slotPrefix == cleanDisplayName) return true
          if (slotSimplifiedPrefix.isNotBlank() && slotSimplifiedPrefix == simplifiedDisplayName) return true
        }
      }

      // 2. Pen name match (e.g. Owner set pen name as translator's Gmail name or display name)
      if (slotPen.isNotBlank() && !slotPen.startsWith("translator ", ignoreCase = true)) {
        if (slotPen == cleanUserEmail || slotPen == userPrefix || slotSimplifiedPen == userSimplifiedPrefix) return true
        if (cleanDisplayName.isNotBlank() && (slotPen == cleanDisplayName || slotSimplifiedPen == simplifiedDisplayName)) return true
      }

      // 3. Author name match
      if (slotAuthor.isNotBlank() && !slotAuthor.startsWith("translator ", ignoreCase = true)) {
        if (slotAuthor == cleanUserEmail || slotAuthor == userPrefix || slotSimplifiedAuthor == userSimplifiedPrefix) return true
        if (cleanDisplayName.isNotBlank() && (slotAuthor == cleanDisplayName || slotSimplifiedAuthor == simplifiedDisplayName)) return true
      }

      return false
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
      syncRemoteAuthorSlots()
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

  suspend fun syncRemoteAuthorSlots(): Result<Int> {
    if (syncService == null) return Result.success(0)
    return try {
      val remoteResult = syncService.fetchRemoteAuthorSlots()
      if (remoteResult.isSuccess) {
        val remoteSlots = remoteResult.getOrNull() ?: emptyList()
        if (remoteSlots.isNotEmpty()) {
          for (remoteSlot in remoteSlots) {
            val localSlot = dao.getAuthorSlot(remoteSlot.slotNumber)
            // If remote has permission or translator email assigned, merge it locally
            if (remoteSlot.isPermissionGranted || !remoteSlot.translatorEmail.isNullOrBlank()) {
              dao.updateAuthorSlot(
                remoteSlot.copy(
                  coverImageUri = localSlot?.coverImageUri ?: remoteSlot.coverImageUri
                )
              )
            }
          }
          checkAndUpgradeProfilesAgainstSlots()
        }
        Result.success(remoteSlots.size)
      } else {
        Result.failure(remoteResult.exceptionOrNull() ?: Exception("Failed to sync remote author slots"))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun checkAndUpgradeProfilesAgainstSlots() {
    val allSlots = dao.getAllAuthorSlotsSync()
    val profiles = dao.getAllProfilesList()
    for (profile in profiles) {
      if (isOwnerEmail(profile.email)) continue
      val matchedSlot = allSlots.find { slot ->
        slot.isPermissionGranted && isUserMatchedToSlot(profile.email, profile.displayName, slot)
      }
      if (matchedSlot != null) {
        if (profile.role != "TRANSLATOR" || profile.authorSlot != matchedSlot.slotNumber) {
          dao.upgradeProfileToTranslator(profile.userId, matchedSlot.slotNumber)
        }
      }
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

    // Automatically backup novel package (metadata & manuscript) to Firebase Cloud Storage bucket if available
    if (firebaseStorageService != null) {
      externalScope.launch {
        try {
          val fbRes = firebaseStorageService.uploadNovelPackage(novel)
          if (fbRes.isSuccess) {
            Log.d("StrawberrycandyRepository", "Uploaded novel ${novel.id} to Firebase Cloud Storage (${fbRes.getOrNull()?.storageBucket})")
          } else {
            Log.w("StrawberrycandyRepository", "Firebase Cloud Storage backup note: ${fbRes.exceptionOrNull()?.message}")
          }
        } catch (e: Exception) {
          Log.w("StrawberrycandyRepository", "Firebase Cloud Storage upload error: ${e.message}")
        }
      }
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

  suspend fun publishNovelToFirebaseStorage(novelId: String): Result<String> {
    val storage = firebaseStorageService ?: return Result.failure(IllegalStateException("Firebase Cloud Storage service unavailable"))
    val novel = dao.getNovelById(novelId) ?: return Result.failure(IllegalArgumentException("Novel not found"))
    val res = storage.uploadNovelPackage(novel)
    return if (res.isSuccess) {
      val summary = res.getOrNull()
      Result.success("✨ Novel manuscript & metadata uploaded to Firebase Storage (gs://${summary?.storageBucket}/novels/$novelId/)")
    } else {
      Result.failure(res.exceptionOrNull() ?: Exception("Firebase Storage upload failed"))
    }
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
      val slot = dao.getAuthorSlot(slotNumber)
      if (slot != null) {
        val profiles = dao.getAllProfilesList()
        for (profile in profiles) {
          if (isOwnerEmail(profile.email)) continue
          if (profile.authorSlot == slotNumber || isUserMatchedToSlot(profile.email, profile.displayName, slot)) {
            val newRole = if (isGranted) "TRANSLATOR" else "READER"
            val newSlot = if (isGranted) slotNumber else null
            dao.insertReaderProfile(profile.copy(role = newRole, authorSlot = newSlot))
          }
        }
      }
      if (syncService != null) {
        try {
          syncService.publishAuthorSlotsToRemote(dao.getAllAuthorSlotsSync())
        } catch (_: Exception) {}
      }
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
    val cleanPass = password.trim()
    if (cleanPass.isBlank()) {
      return Result.failure(IllegalArgumentException("Please enter your account password."))
    }
    if (cleanPass.length < 4) {
      return Result.failure(IllegalArgumentException("Password must be at least 4 characters."))
    }

    val userId = "usr_" + provider.lowercase() + "_" + cleanEmail.replace(Regex("[^a-z0-9]"), "_")
    val isOwner = isOwnerEmail(cleanEmail)

    // Strict password verification for all accounts (Owner, Guest, and Readers alike)
    val existingProfile = dao.getReaderProfileByEmail(cleanEmail) ?: dao.getReaderProfile(userId)
    if (existingProfile != null && !existingProfile.passwordHash.isNullOrBlank()) {
      if (existingProfile.passwordHash != cleanPass) {
        return Result.failure(
          IllegalArgumentException(
            "Incorrect password for $cleanEmail. The password entered must match your account password. If you forgot your password, tap 'Forgot Password?' to retrieve it."
          )
        )
      }
    }

    // Check if owner has pre-granted this email or name to an author slot
    val allSlots = dao.getAllAuthorSlotsSync()
    val preGrantedSlot = allSlots.find { slot ->
      slot.isPermissionGranted && isUserMatchedToSlot(cleanEmail, displayName, slot)
    } ?: dao.getAuthorSlotByEmail(cleanEmail)

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
      passwordHash = cleanPass.ifBlank { existingProfile?.passwordHash ?: cleanPass },
      isLoggedIn = true
    )
    dao.insertReaderProfile(profile)

    // If translator, link their Gmail address to the author slot so the Owner can see it
    if (finalRole == "TRANSLATOR" && finalSlot != null && finalSlot != 0) {
      val slot = dao.getAuthorSlot(finalSlot)
      if (slot != null) {
        dao.updateAuthorSlot(
          slot.copy(
            translatorEmail = cleanEmail,
            isClaimed = true,
            isPermissionGranted = true
          )
        )
      }
    }

    return Result.success(Unit)
  }

  suspend fun grantPermissionByEmail(email: String, slotNumber: Int? = null) {
    val cleanInput = email.trim()
    val cleanEmail = if (cleanInput.contains("@")) cleanInput.lowercase() else "${cleanInput.lowercase()}@gmail.com"
    val allSlots = dao.getAllAuthorSlotsSync()
    val existingSlot = allSlots.find { isUserMatchedToSlot(cleanEmail, cleanInput, it) }

    val targetSlotNumber = if (existingSlot != null) {
      existingSlot.slotNumber
    } else {
      slotNumber ?: (1..10).firstOrNull { slotNum ->
        val s = dao.getAuthorSlot(slotNum)
        s == null || !s.isPermissionGranted
      } ?: 1
    }

    val currentSlot = dao.getAuthorSlot(targetSlotNumber)
    val defaultPen = run {
      val prefix = cleanEmail.substringBefore("@").replace(".", " ")
      prefix.split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.titlecase() } }
    }
    val updatedSlot = (currentSlot ?: AuthorSlotEntity(
      slotNumber = targetSlotNumber,
      authorName = defaultPen,
      penName = defaultPen,
      accessCode = "AUTH-ROOM-$targetSlotNumber"
    )).copy(
      isPermissionGranted = true,
      translatorEmail = cleanEmail,
      isClaimed = true,
      lastActiveTimestamp = System.currentTimeMillis(),
      penName = if (currentSlot == null || currentSlot.penName.isBlank() || currentSlot.penName.startsWith("Translator ", ignoreCase = true)) {
        defaultPen
      } else {
        currentSlot.penName
      },
      authorName = if (currentSlot == null || currentSlot.authorName.isBlank() || currentSlot.authorName.startsWith("Translator ", ignoreCase = true)) {
        defaultPen
      } else {
        currentSlot.authorName
      }
    )
    dao.updateAuthorSlot(updatedSlot)

    // Upgrade existing reader profile if one already exists
    val profiles = dao.getAllProfilesList()
    for (p in profiles) {
      if (!isOwnerEmail(p.email) && isUserMatchedToSlot(p.email, p.displayName, updatedSlot)) {
        dao.upgradeProfileToTranslator(p.userId, targetSlotNumber)
      }
    }

    // Sync author slots to Cloud Archive so other devices receive permission immediately!
    if (syncService != null) {
      try {
        syncService.publishAuthorSlotsToRemote(dao.getAllAuthorSlotsSync())
      } catch (_: Exception) {}
    }
  }

  // Active recovery sessions in memory (email -> RecoverySession) with firewall protection
  private data class RecoverySession(val email: String, val code: String, val timestamp: Long, var failedAttempts: Int = 0)
  private val activeRecoverySessions = java.util.concurrent.ConcurrentHashMap<String, RecoverySession>()

  suspend fun sendPasswordRecoveryCode(email: String): Result<String> {
    val cleanEmail = email.trim().lowercase()
    if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
      return Result.failure(IllegalArgumentException("Please enter a valid Gmail address."))
    }

    // 1. Generate secure 6-digit recovery code for in-app verification
    val code = (100000..999999).random().toString()
    activeRecoverySessions[cleanEmail] = RecoverySession(cleanEmail, code, System.currentTimeMillis(), 0)

    // 2. Dispatch official password reset email directly via Firebase Authentication
    try {
      val auth = FirebaseAuth.getInstance()
      auth.sendPasswordResetEmail(cleanEmail).addOnCompleteListener { task ->
        if (task.isSuccessful) {
          Log.d("StrawberrycandyAuth", "Firebase Authentication password reset email dispatched for $cleanEmail")
        } else {
          val ex = task.exception
          Log.w("StrawberrycandyAuth", "Firebase Auth reset dispatch error: ${ex?.message}")
          try {
            val tempPass = "Reset_" + java.util.UUID.randomUUID().toString().take(8) + "!"
            auth.createUserWithEmailAndPassword(cleanEmail, tempPass).addOnCompleteListener { createRes ->
              if (createRes.isSuccessful) {
                auth.sendPasswordResetEmail(cleanEmail)
              }
            }
          } catch (createEx: Exception) {
            Log.w("StrawberrycandyAuth", "Firebase Auth auto-provision failed: ${createEx.message}")
          }
        }
      }
    } catch (e: Exception) {
      Log.w("StrawberrycandyAuth", "Firebase Auth reset error: ${e.message}")
    }

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
    // Firewall protection against brute-force
    if (session.failedAttempts >= 5) {
      activeRecoverySessions.remove(cleanEmail)
      return Result.failure(IllegalArgumentException("Security Firewall: Maximum verification attempts exceeded. Please request a new 2FA code."))
    }
    if (session.code != code.trim()) {
      session.failedAttempts++
      val remaining = 5 - session.failedAttempts
      return Result.failure(IllegalArgumentException("Invalid verification code. Firewall: $remaining attempt${if (remaining == 1) "" else "s"} remaining."))
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
    val rawEmail = translatorEmail?.trim()?.ifBlank { null }
    val cleanEmail = if (rawEmail != null) {
      if (rawEmail.contains("@")) rawEmail.lowercase() else "${rawEmail.lowercase()}@gmail.com"
    } else null
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

    val profiles = dao.getAllProfilesList()
    for (profile in profiles) {
      if (isOwnerEmail(profile.email)) continue
      if (profile.authorSlot == slotNumber || (cleanEmail != null && isUserMatchedToSlot(profile.email, profile.displayName, updated))) {
        val updatedRole = if (isPermissionGranted) "TRANSLATOR" else "READER"
        val updatedSlot = if (isPermissionGranted) slotNumber else null
        dao.insertReaderProfile(
          profile.copy(
            role = updatedRole,
            authorSlot = updatedSlot,
            displayName = if (cleanPenName.isNotBlank() && !cleanPenName.startsWith("Translator ", ignoreCase = true)) cleanPenName else profile.displayName
          )
        )
      }
    }

    if (syncService != null) {
      try {
        syncService.publishAuthorSlotsToRemote(dao.getAllAuthorSlotsSync())
      } catch (_: Exception) {}
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

  fun getAllCommentsForNovel(novelId: String): Flow<List<ChapterCommentEntity>> {
    return dao.getAllCommentsForNovel(novelId)
  }

  fun getCommentCountForChapter(novelId: String, chapterTitle: String): Flow<Int> {
    return dao.getCommentCountForChapter(novelId, chapterTitle)
  }

  fun listenToCloudComments(novelId: String) {
    try {
      val firestore = FirebaseFirestore.getInstance()
      firestore.collection("chapter_comments")
        .whereEqualTo("novelId", novelId)
        .addSnapshotListener { snapshot, error ->
          if (error != null || snapshot == null) return@addSnapshotListener
          val cloudComments = snapshot.documents.mapNotNull { doc ->
            try {
              val cId = doc.getString("id") ?: doc.id
              val cNovelId = doc.getString("novelId") ?: novelId
              if (cId.isBlank()) null
              else ChapterCommentEntity(
                id = cId,
                novelId = cNovelId,
                chapterTitle = doc.getString("chapterTitle") ?: "Chapter I",
                readerName = doc.getString("readerName") ?: "Literary Reader",
                readerEmail = doc.getString("readerEmail") ?: "",
                commentText = doc.getString("commentText") ?: "",
                timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                avatarColorHex = doc.getLong("avatarColorHex") ?: 0xFF5C2D3B,
                likesCount = (doc.getLong("likesCount") ?: 0L).toInt(),
                isLikedByMe = false,
                parentCommentId = doc.getString("parentCommentId").takeIf { !it.isNullOrBlank() },
                replyToReaderName = doc.getString("replyToReaderName").takeIf { !it.isNullOrBlank() }
              )
            } catch (_: Exception) { null }
          }
          if (cloudComments.isNotEmpty()) {
            externalScope.launch {
              dao.insertComments(cloudComments)
            }
          }
        }
    } catch (e: Exception) {
      Log.w("StrawberrycandyComments", "Firestore listenToCloudComments error: ${e.message}")
    }
  }

  suspend fun syncRemoteComments(novelId: String? = null) {
    // 1. Fetch from Firebase Firestore Cloud
    try {
      val firestore = FirebaseFirestore.getInstance()
      val query = if (novelId != null) {
        firestore.collection("chapter_comments").whereEqualTo("novelId", novelId)
      } else {
        firestore.collection("chapter_comments")
      }
      query.get().addOnSuccessListener { snapshot ->
        if (snapshot != null && !snapshot.isEmpty) {
          val cloudComments = snapshot.documents.mapNotNull { doc ->
            try {
              val cId = doc.getString("id") ?: doc.id
              val cNovelId = doc.getString("novelId") ?: ""
              if (cId.isBlank() || cNovelId.isBlank()) null
              else ChapterCommentEntity(
                id = cId,
                novelId = cNovelId,
                chapterTitle = doc.getString("chapterTitle") ?: "Chapter I",
                readerName = doc.getString("readerName") ?: "Literary Reader",
                readerEmail = doc.getString("readerEmail") ?: "",
                commentText = doc.getString("commentText") ?: "",
                timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                avatarColorHex = doc.getLong("avatarColorHex") ?: 0xFF5C2D3B,
                likesCount = (doc.getLong("likesCount") ?: 0L).toInt(),
                isLikedByMe = false,
                parentCommentId = doc.getString("parentCommentId").takeIf { !it.isNullOrBlank() },
                replyToReaderName = doc.getString("replyToReaderName").takeIf { !it.isNullOrBlank() }
              )
            } catch (_: Exception) { null }
          }
          if (cloudComments.isNotEmpty()) {
            externalScope.launch {
              dao.insertComments(cloudComments)
            }
          }
        }
      }
    } catch (e: Exception) {
      Log.w("StrawberrycandyComments", "Firestore syncRemoteComments error: ${e.message}")
    }

    // 2. Fetch from Cloud Archive REST
    val service = syncService ?: return
    try {
      val remoteResult = service.fetchRemoteComments(novelId)
      if (remoteResult.isSuccess) {
        val comments = remoteResult.getOrNull() ?: emptyList()
        if (comments.isNotEmpty()) {
          dao.insertComments(comments)
        }
      }
    } catch (_: Exception) {}
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
    
    // Post to Cloud: Firebase Firestore & Cloud Archive REST
    externalScope.launch {
      try {
        val firestore = FirebaseFirestore.getInstance()
        val docData = hashMapOf(
          "id" to comment.id,
          "novelId" to comment.novelId,
          "chapterTitle" to comment.chapterTitle,
          "readerName" to comment.readerName,
          "readerEmail" to comment.readerEmail,
          "commentText" to comment.commentText,
          "timestamp" to comment.timestamp,
          "avatarColorHex" to comment.avatarColorHex,
          "likesCount" to comment.likesCount,
          "parentCommentId" to (comment.parentCommentId ?: ""),
          "replyToReaderName" to (comment.replyToReaderName ?: "")
        )
        firestore.collection("chapter_comments").document(comment.id).set(docData)
        Log.d("StrawberrycandyComments", "Comment ${comment.id} posted directly to Firebase Firestore Cloud")
      } catch (e: Exception) {
        Log.w("StrawberrycandyComments", "Firebase Firestore comment upload: ${e.message}")
      }

      try {
        syncService?.pushCommentToCloud(comment)
      } catch (_: Exception) {}
    }
  }

  suspend fun likeComment(commentId: String) {
    dao.likeComment(commentId)
    externalScope.launch {
      try {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("chapter_comments").document(commentId)
          .update("likesCount", FieldValue.increment(1))
      } catch (e: Exception) {
        Log.w("StrawberrycandyComments", "Firebase Firestore likeComment error: ${e.message}")
      }

      try {
        syncService?.likeCommentInCloud(commentId)
      } catch (_: Exception) {}
    }
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
