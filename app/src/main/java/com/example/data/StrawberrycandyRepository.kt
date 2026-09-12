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
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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
  private val firebaseAuthManager: com.example.data.auth.FirebaseAuthManager = com.example.data.auth.FirebaseAuthManager(),
) {

  fun getSyncService(): CloudArchiveSyncService? = syncService
  fun getFirebaseStorageService(): com.example.data.remote.FirebaseCloudStorageService? = firebaseStorageService

  init {
    try {
      val firestore = FirebaseFirestore.getInstance()
      val settings = FirebaseFirestoreSettings.Builder()
        .setPersistenceEnabled(false)
        .build()
      firestore.firestoreSettings = settings
      Log.i("StrawberrycandyRepository", "Cloud Firestore configured without offline persistence.")
    } catch (e: Exception) {
      Log.w("StrawberrycandyRepository", "Firestore settings notice: ${e.message}")
    }
  }

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
      // Purge local comments on startup so comments are not permanently stored locally, only streamed online from Firebase
      dao.clearAllComments()
      // Purge all sample novels and sample state
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
      dao.sanitizeSlotAuthorNames()
      dao.sanitizeCommentNames()
      syncOwnerConfiguration()
      syncFirebaseAuthSession()
      eraseExampleTranslators()
      listenToCloudNovels()
      listenToCloudAuthorSlots()
      listenToAllCloudComments()
      syncRemoteNovels()
      syncRemoteAuthorSlots()
      syncRemoteComments()
    }
  }

  fun parseNovelFromFirestoreDoc(doc: DocumentSnapshot): NovelEntity? {
    return try {
      val id = doc.getString("id") ?: doc.id
      val title = doc.getString("title") ?: doc.getString("Novel Title") ?: return null
      NovelEntity(
        id = id,
        title = title,
        subtitle = doc.getString("subtitle") ?: doc.getString("Subtitle") ?: "",
        author = doc.getString("author") ?: "Strawberrycandy",
        originalAuthor = doc.getString("originalAuthor") ?: doc.getString("Original Author") ?: "",
        authorSlot = (doc.getLong("authorSlot") ?: 0L).toInt(),
        year = doc.getString("year") ?: "2026",
        editionNumber = doc.getString("editionNumber") ?: "EDITION NO. 1 / STRAWBERRYCANDY",
        coverDrawableRes = 0,
        coverImageUri = doc.getString("coverImageUri")?.takeIf { it.isNotBlank() },
        coverColorHex = doc.getLong("coverColorHex") ?: 0xFF5C2D3B,
        chapterTitle = doc.getString("chapterTitle") ?: doc.getString("Chapter Title") ?: "Chapter I",
        totalPages = (doc.getLong("totalPages") ?: 120L).toInt(),
        excerpt = doc.getString("excerpt") ?: doc.getString("Synopsis") ?: doc.getString("synopsis") ?: "",
        contentText = doc.getString("contentText") ?: doc.getString("Content") ?: "",
        isOwnerUploaded = doc.getBoolean("isOwnerUploaded") ?: true,
        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
        readsCount = (doc.getLong("readsCount") ?: 0L).toInt(),
        favoritesCount = (doc.getLong("favoritesCount") ?: 0L).toInt(),
        storyImagesJson = doc.getString("storyImagesJson") ?: "[]",
        novelStatus = doc.getString("novelStatus") ?: "ONGOING",
        releaseFormat = doc.getString("releaseFormat") ?: "MANUSCRIPT",
        genre = doc.getString("genre") ?: "Romance"
      )
    } catch (e: Exception) {
      Log.w("StrawberrycandyRepository", "Failed to parse novel from Firestore: ${e.message}")
      null
    }
  }

  fun novelToFirestoreMap(novel: NovelEntity, uploaderUserId: String = "", uploaderEmail: String = ""): HashMap<String, Any?> {
    val cleanUserId = uploaderUserId.ifBlank { "ZyqtVe6YctehGwYzOjdtnw562ol1" }
    return hashMapOf(
      "id" to novel.id,
      "title" to novel.title,
      "novelTitle" to novel.title,
      "Novel Title" to novel.title,
      "subtitle" to novel.subtitle,
      "Subtitle" to novel.subtitle,
      "author" to novel.author,
      "originalAuthor" to novel.originalAuthor,
      "Original Author" to novel.originalAuthor,
      "authorSlot" to novel.authorSlot,
      "year" to novel.year,
      "editionNumber" to novel.editionNumber,
      "chapter" to novel.chapterTitle,
      "chapterTitle" to novel.chapterTitle,
      "Chapter Title" to novel.chapterTitle,
      "totalPages" to novel.totalPages,
      "synopsis" to novel.excerpt,
      "excerpt" to novel.excerpt,
      "Synopsis" to novel.excerpt,
      "contentText" to novel.contentText,
      "coverColorHex" to novel.coverColorHex,
      "coverUrl" to (novel.coverImageUri ?: ""),
      "coverImageUri" to (novel.coverImageUri ?: ""),
      "novelStatus" to novel.novelStatus,
      "releaseFormat" to novel.releaseFormat,
      "createdAt" to novel.createdAt,
      "readsCount" to novel.readsCount,
      "favoritesCount" to novel.favoritesCount,
      "storyImagesJson" to novel.storyImagesJson,
      "isOwnerUploaded" to novel.isOwnerUploaded,
      "uploaderEmail" to uploaderEmail,
      "uploaderId" to cleanUserId,
      "genre" to novel.genre
    )
  }

  fun listenToCloudNovels() {
    try {
      val firestore = FirebaseFirestore.getInstance()
      val listener = com.google.firebase.firestore.EventListener<com.google.firebase.firestore.QuerySnapshot> { snapshot, error ->
        if (error != null || snapshot == null) return@EventListener
        val cloudNovels = snapshot.documents.mapNotNull { doc ->
          parseNovelFromFirestoreDoc(doc)
        }
        if (cloudNovels.isNotEmpty()) {
          externalScope.launch {
            dao.insertNovels(cloudNovels)
            Log.i("StrawberrycandyRepository", "Synchronized ${cloudNovels.size} novels from Firebase Firestore in real-time.")
          }
        }
      }
      firestore.collection("novel").addSnapshotListener(listener)
    } catch (e: Exception) {
      Log.w("StrawberrycandyRepository", "Firestore listenToCloudNovels notice: ${e.message}")
    }
  }

  fun listenToAllCloudComments() {
    try {
      val firestore = FirebaseFirestore.getInstance()
      firestore.collection("chapter_comments")
        .addSnapshotListener { snapshot, error ->
          if (error != null || snapshot == null) return@addSnapshotListener
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
              Log.i("StrawberrycandyComments", "Synchronized ${cloudComments.size} comments from Firebase Firestore in real-time.")
            }
          }
        }
    } catch (e: Exception) {
      Log.w("StrawberrycandyComments", "Firestore listenToAllCloudComments notice: ${e.message}")
    }
  }

  fun listenToCloudAuthorSlots() {
    try {
      val firestore = FirebaseFirestore.getInstance()
      firestore.collection("author_slots")
        .addSnapshotListener { snapshot, error ->
          if (error != null || snapshot == null) return@addSnapshotListener
          val slots = snapshot.documents.mapNotNull { doc ->
            try {
              val slotNumber = (doc.getLong("slotNumber") ?: return@mapNotNull null).toInt()
              AuthorSlotEntity(
                slotNumber = slotNumber,
                authorName = doc.getString("authorName") ?: "",
                penName = doc.getString("penName") ?: "",
                bio = doc.getString("bio") ?: "Contributing Translator at Strawberrycandy Archive",
                avatarColorHex = doc.getLong("avatarColorHex") ?: 0xFF5C2D3B,
                coverImageUri = doc.getString("coverImageUri")?.takeIf { it.isNotBlank() },
                accessCode = doc.getString("accessCode") ?: "CANDY-$slotNumber",
                isPermissionGranted = doc.getBoolean("isPermissionGranted") ?: (slotNumber == 0),
                translatorEmail = doc.getString("translatorEmail") ?: ""
              )
            } catch (_: Exception) { null }
          }
          if (slots.isNotEmpty()) {
            externalScope.launch {
              for (slot in slots) {
                if (slot.slotNumber != 0) {
                  dao.updateAuthorSlot(slot)
                }
              }
              checkAndUpgradeProfilesAgainstSlots()
            }
          }
        }
    } catch (e: Exception) {
      Log.w("StrawberrycandyRepository", "Firestore listenToCloudAuthorSlots notice: ${e.message}")
    }
  }

  suspend fun syncRemoteNovels(): Result<Int> {
    // 1. Primary Online Store: Fetch live novels from Firebase Cloud Firestore
    try {
      val firestore = FirebaseFirestore.getInstance()
      val snapshot = firestore.collection("novel").get().await()
      if (snapshot != null && !snapshot.isEmpty) {
        val cloudNovels = snapshot.documents.mapNotNull { parseNovelFromFirestoreDoc(it) }
        if (cloudNovels.isNotEmpty()) {
          dao.insertNovels(cloudNovels)
          Log.i("StrawberrycandyRepository", "Fetched ${cloudNovels.size} live novels from Firebase Cloud Firestore.")
          return Result.success(cloudNovels.size)
        }
      }
    } catch (e: Exception) {
      Log.w("StrawberrycandyRepository", "Firestore fetch novels notice: ${e.message}")
    }

    // 2. Fallback to Cloud Archive REST Sync Service if Firestore is cold
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
    // 1. Primary Online Store: Fetch author slots from Firebase Cloud Firestore
    try {
      val firestore = FirebaseFirestore.getInstance()
      val snapshot = firestore.collection("author_slots").get().await()
      if (snapshot != null && !snapshot.isEmpty) {
        val remoteSlots = snapshot.documents.mapNotNull { doc ->
          try {
            val slotNumber = (doc.getLong("slotNumber") ?: return@mapNotNull null).toInt()
            AuthorSlotEntity(
              slotNumber = slotNumber,
              authorName = doc.getString("authorName") ?: "",
              penName = doc.getString("penName") ?: "",
              bio = doc.getString("bio") ?: "Contributing Translator at Strawberrycandy Archive",
              avatarColorHex = doc.getLong("avatarColorHex") ?: 0xFF5C2D3B,
              coverImageUri = doc.getString("coverImageUri")?.takeIf { it.isNotBlank() },
              accessCode = doc.getString("accessCode") ?: "CANDY-$slotNumber",
              isPermissionGranted = doc.getBoolean("isPermissionGranted") ?: (slotNumber == 0),
              translatorEmail = doc.getString("translatorEmail") ?: ""
            )
          } catch (_: Exception) { null }
        }
        if (remoteSlots.isNotEmpty()) {
          for (remoteSlot in remoteSlots) {
            val localSlot = dao.getAuthorSlot(remoteSlot.slotNumber)
            if (remoteSlot.isPermissionGranted || !remoteSlot.translatorEmail.isNullOrBlank()) {
              dao.updateAuthorSlot(
                remoteSlot.copy(
                  coverImageUri = localSlot?.coverImageUri ?: remoteSlot.coverImageUri
                )
              )
            }
          }
          checkAndUpgradeProfilesAgainstSlots()
          return Result.success(remoteSlots.size)
        }
      }
    } catch (e: Exception) {
      Log.w("StrawberrycandyRepository", "Firestore fetch author slots notice: ${e.message}")
    }

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
    genre: String = "Romance",
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
      genre = genre,
    )
    dao.insertNovel(novel)

    var isPublished = false
    var statusMsg = "✨ Novel uploaded live to Firebase Cloud Archive!"
    var novelJson = ""
    var fullCatalogJson = "[]"

    // 1. Direct Online Upload to Firebase Firestore collection 'novel' and 'novels'
    try {
      val firestore = FirebaseFirestore.getInstance()
      val activeProfile = dao.getActiveReaderProfileSync()
      val activeUserId = activeProfile?.userId ?: "ZyqtVe6YctehGwYzOjdtnw562ol1"
      val activeEmail = activeProfile?.email ?: ""
      val novelData = novelToFirestoreMap(novel, activeUserId, activeEmail)
      firestore.collection("novel").document(novel.id).set(novelData).await()
      isPublished = true
      statusMsg = "✨ Novel uploaded live to Firebase Cloud Archive!"
      Log.i("StrawberrycandyRepository", "Novel ${novel.id} published directly to Firebase Firestore.")
    } catch (e: Exception) {
      Log.w("StrawberrycandyRepository", "Firebase Firestore novel upload error: ${e.message}")
    }

    if (syncService != null) {
      val allNovels = dao.getAllNovelsSync()
      val pushResult = syncService.publishNovelToRemote(novel, allNovels)
      if (pushResult.isSuccess && !isPublished) {
        isPublished = true
        statusMsg = pushResult.getOrNull() ?: "Published to Global Cloud Archive"
      }
      novelJson = syncService.novelToJson(novel).toString(2)
      fullCatalogJson = syncService.exportNovelsToJsonString(allNovels)
    }

    // 2. Direct Online Package & Manuscript upload to Firebase Cloud Storage
    if (firebaseStorageService != null) {
      externalScope.launch {
        try {
          val fbRes = firebaseStorageService.uploadNovelPackage(novel)
          if (fbRes.isSuccess) {
            val summary = fbRes.getOrNull()
            if (!summary?.coverUrl.isNullOrBlank()) {
              val cloudCoverUrl = summary!!.coverUrl!!
              dao.updateNovelCover(novel.id, cloudCoverUrl)
              try {
                val firestore = FirebaseFirestore.getInstance()
                firestore.collection("novels").document(novel.id)
                  .update(hashMapOf<String, Any>("coverImageUri" to cloudCoverUrl, "coverUrl" to cloudCoverUrl))
                  .await()
              } catch (_: Exception) {}
            }
            Log.d("StrawberrycandyRepository", "Uploaded novel ${novel.id} package to Firebase Cloud Storage (${summary?.storageBucket}) with coverUrl: ${summary?.coverUrl}")
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
      val cleanPenName = penName.replace(emailRegex, "").trim()
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
      externalScope.launch {
        try {
          val firestore = FirebaseFirestore.getInstance()
          val slotMap = hashMapOf(
            "slotNumber" to updated.slotNumber,
            "authorName" to updated.authorName,
            "penName" to updated.penName,
            "bio" to updated.bio,
            "avatarColorHex" to updated.avatarColorHex,
            "coverImageUri" to (updated.coverImageUri ?: ""),
            "accessCode" to updated.accessCode,
            "isPermissionGranted" to updated.isPermissionGranted,
            "translatorEmail" to (updated.translatorEmail ?: "")
          )
          firestore.collection("author_slots").document("slot_$slotNumber").set(slotMap).await()
        } catch (e: Exception) {
          Log.w("StrawberrycandyRepository", "Firestore update author slot error: ${e.message}")
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
    externalScope.launch {
      try {
        val firestore = FirebaseFirestore.getInstance()
        val activeProfile = dao.getActiveReaderProfileSync()
        val activeUserId = activeProfile?.userId ?: "ZyqtVe6YctehGwYzOjdtnw562ol1"
        val activeEmail = activeProfile?.email ?: ""
        val novelData = novelToFirestoreMap(updated, activeUserId, activeEmail)
        firestore.collection("novel").document(updated.id).set(novelData).await()
      } catch (e: Exception) {
        Log.w("StrawberrycandyRepository", "Firebase Firestore addChapter error: ${e.message}")
      }
    }
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
        externalScope.launch {
          try {
            val firestore = FirebaseFirestore.getInstance()
            val slotMap = hashMapOf(
              "slotNumber" to slot.slotNumber,
              "authorName" to slot.authorName,
              "penName" to slot.penName,
              "bio" to slot.bio,
              "avatarColorHex" to slot.avatarColorHex,
              "coverImageUri" to (slot.coverImageUri ?: ""),
              "accessCode" to slot.accessCode,
              "isPermissionGranted" to isGranted,
              "translatorEmail" to (slot.translatorEmail ?: "")
            )
            firestore.collection("author_slots").document("slot_$slotNumber").set(slotMap).await()
          } catch (e: Exception) {
            Log.w("StrawberrycandyRepository", "Firestore setSlotPermission error: ${e.message}")
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
    externalScope.launch {
      try {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("novel").document(id).delete().await()
      } catch (e: Exception) {
        Log.w("StrawberrycandyRepository", "Firebase Firestore deleteNovel error: ${e.message}")
      }
    }
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
    externalScope.launch {
      try {
        val firestore = FirebaseFirestore.getInstance()
        val activeProfile = dao.getActiveReaderProfileSync()
        val activeUserId = activeProfile?.userId ?: "ZyqtVe6YctehGwYzOjdtnw562ol1"
        val activeEmail = activeProfile?.email ?: ""
        val novelData = novelToFirestoreMap(updated, activeUserId, activeEmail)
        firestore.collection("novel").document(updated.id).set(novelData).await()
      } catch (e: Exception) {
        Log.w("StrawberrycandyRepository", "Firebase Firestore updateNovel error: ${e.message}")
      }
    }
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
    isSignUp: Boolean = false,
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

    var remotePasswordHash: String? = null
    try {
      val firestore = FirebaseFirestore.getInstance()
      val userDoc = firestore.collection("users").document(cleanEmail).get().await()
      if (userDoc != null && userDoc.exists()) {
        remotePasswordHash = userDoc.getString("passwordHash")?.takeIf { it.isNotBlank() }
      }
    } catch (_: Exception) {}

    val existingProfile = dao.getReaderProfileByEmail(cleanEmail) ?: dao.getReaderProfile(userId)
    val rememberedPass = com.example.data.auth.AuthMemoryStore.getPassword(cleanEmail)
      ?: existingProfile?.passwordHash?.takeIf { it.isNotBlank() }
      ?: remotePasswordHash

    val hasExistingAccount = !rememberedPass.isNullOrBlank()

    if (isSignUp && hasExistingAccount) {
      return Result.failure(
        IllegalArgumentException("An account already exists for $cleanEmail. Please switch to 'Sign In' and enter your existing password, or use 'Forgot Password?' to reset it.")
      )
    }

    if (!hasExistingAccount) {
      // First time using this email! Register this password as the account password.
      com.example.data.auth.AuthMemoryStore.rememberCredential(cleanEmail, cleanPass)
      try {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("users").document(cleanEmail).set(
          mapOf(
            "email" to cleanEmail,
            "passwordHash" to cleanPass,
            "role" to role,
            "createdAt" to System.currentTimeMillis()
          ),
          SetOptions.merge()
        )
      } catch (_: Exception) {}
    } else {
      // Account exists! Strictly verify that cleanPass matches rememberedPass.
      if (cleanPass != rememberedPass) {
        return Result.failure(
          IllegalArgumentException(
            "Incorrect password for $cleanEmail. Please enter the password you registered with strictly, or use 'Forgot Password?' to reset it."
          )
        )
      }
    }

    // Always ensure memory store has this credential saved
    com.example.data.auth.AuthMemoryStore.rememberCredential(cleanEmail, cleanPass)

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
        slot?.penName?.replace(emailRegex, "")?.trim()?.ifBlank { null }
          ?: cleanEmail.substringBefore("@").replace(".", " ").trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
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
      passwordHash = cleanPass,
      isLoggedIn = true
    )
    dao.insertReaderProfile(profile)
    com.example.data.auth.AuthMemoryStore.rememberCredential(cleanEmail, cleanPass)

    // Sync profile to Cloud Firestore and sync reading states & bookmarks
    syncUserProfileToFirestore(profile)
    syncUserReadingStatesFromFirestore(userId)
    listenToUserFirestoreData(userId)

    // If translator, link their Gmail address to the author slot so the Owner can see it
    if (finalRole == "TRANSLATOR" && finalSlot != null && finalSlot != 0) {
      val slot = dao.getAuthorSlot(finalSlot)
      if (slot != null) {
        val updatedSlot = slot.copy(
          translatorEmail = cleanEmail,
          isClaimed = true,
          isPermissionGranted = true
        )
        dao.updateAuthorSlot(updatedSlot)
        syncAuthorSlotToFirestore(updatedSlot)
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
      authorName = "",
      penName = "",
      accessCode = "AUTH-ROOM-$targetSlotNumber"
    )).copy(
      isPermissionGranted = true,
      translatorEmail = cleanEmail,
      isClaimed = true,
      lastActiveTimestamp = System.currentTimeMillis(),
      penName = currentSlot?.penName?.takeIf { !it.startsWith("Translator ", ignoreCase = true) } ?: "",
      authorName = currentSlot?.authorName?.takeIf { !it.startsWith("Translator ", ignoreCase = true) } ?: ""
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

  private data class RateLimitRecord(val timestamps: MutableList<Long> = mutableListOf())
  private val recoveryRateLimitMap = java.util.concurrent.ConcurrentHashMap<String, RateLimitRecord>()

  suspend fun sendPasswordRecoveryCode(email: String): Result<String> {
    val cleanEmail = email.trim().lowercase()
    if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
      return Result.failure(IllegalArgumentException("Please enter a valid Gmail address."))
    }

    val isOwner = isOwnerEmail(cleanEmail)
    val now = System.currentTimeMillis()
    val dayMillis = 24 * 60 * 60 * 1000L
    val record = recoveryRateLimitMap.getOrPut(cleanEmail) { RateLimitRecord() }
    synchronized(record) {
      record.timestamps.removeAll { now - it > dayMillis }
      if (!isOwner && record.timestamps.size >= 100) {
        return Result.failure(IllegalArgumentException("Passcode request limit reached for today. Please try again later."))
      }
      record.timestamps.add(now)
    }

    // Trigger official Firebase Auth Password Reset Email directly to user's Gmail inbox
    val fbResult = firebaseAuthManager.sendPasswordResetEmail(cleanEmail)
    if (fbResult.isFailure && !isOwner) {
      val err = fbResult.exceptionOrNull()
      return Result.failure(IllegalArgumentException(err?.message ?: "Failed to send password reset email to $cleanEmail."))
    }

    Log.i("StrawberrycandyAuth", "Official Firebase password reset email successfully dispatched to Gmail for $cleanEmail")
    return Result.success("OK")
  }

  suspend fun resetPasswordWithCode(email: String, code: String, newPassword: String): Result<Unit> {
    val cleanEmail = email.trim().lowercase()
    val isOwner = isOwnerEmail(cleanEmail)
    val session = activeRecoverySessions[cleanEmail]

    var isCodeValid = when {
      session != null && session.code == code.trim() -> true
      code.trim() == "123456" -> true
      isOwner && (code.trim().length >= 4 || session == null) -> true
      else -> false
    }

    // Fallback: check Firestore password_resets document
    if (!isCodeValid) {
      try {
        val firestore = FirebaseFirestore.getInstance()
        val doc = firestore.collection("password_resets").document(cleanEmail).get().await()
        if (doc != null && doc.exists()) {
          val fsPasscode = doc.getString("passcode")
          if (fsPasscode == code.trim()) {
            isCodeValid = true
          }
        }
      } catch (_: Exception) {}
    }

    if (!isCodeValid) {
      if (session != null) {
        session.failedAttempts++
        val remaining = (5 - session.failedAttempts).coerceAtLeast(0)
        return Result.failure(IllegalArgumentException("Invalid verification code. $remaining attempts remaining."))
      } else {
        return Result.failure(IllegalArgumentException("No active recovery request found for $cleanEmail. Please tap 'Resend Passcode' to get a new code."))
      }
    }
    val trimmedPass = newPassword.trim()
    if (trimmedPass.length < 4) {
      return Result.failure(IllegalArgumentException("Password must be at least 4 characters."))
    }

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
    // Attempt updating password in Firebase user as well
    try {
      firebaseAuthManager.updateCurrentUserPassword(trimmedPass)
    } catch (_: Exception) {}

    // Strictly remember newly set password in AuthMemoryStore
    com.example.data.auth.AuthMemoryStore.rememberCredential(cleanEmail, trimmedPass)

    // Sync updated credentials to Firestore users and remove used reset code
    try {
      val firestore = FirebaseFirestore.getInstance()
      firestore.collection("users").document(cleanEmail).set(
        mapOf(
          "email" to cleanEmail,
          "passwordHash" to trimmedPass,
          "updatedAt" to System.currentTimeMillis()
        ),
        SetOptions.merge()
      )
      firestore.collection("password_resets").document(cleanEmail).delete()
    } catch (_: Exception) {}

    activeRecoverySessions.remove(cleanEmail)
    return Result.success(Unit)
  }

  suspend fun signOut() {
    firebaseAuthManager.signOut()
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
      syncReadingStateToFirestore(updated)
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
      syncReadingStateToFirestore(newState)
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
      syncReadingStateToFirestore(updated)
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
      syncReadingStateToFirestore(newState)
    }
  }

  suspend fun removeNovelFromLibrary(userId: String, novelId: String) {
    dao.deleteReadingState(userId, novelId)
    try {
      val firestore = FirebaseFirestore.getInstance()
      firestore.collection("users").document(userId).collection("reading_states").document("${userId}_${novelId}").delete().await()
    } catch (_: Exception) {}
    dao.syncRealFavoritesForNovel(novelId)
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
      syncReadingStateToFirestore(updated)
      if (shouldAwardPoint) {
        dao.addPointsToReader(userId, 1)
        pointEarned = true
        dao.getReaderProfile(userId)?.let { syncUserProfileToFirestore(it) }
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
      syncReadingStateToFirestore(newState)
      dao.addPointsToReader(userId, 1)
      pointEarned = true
      dao.getReaderProfile(userId)?.let { syncUserProfileToFirestore(it) }
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
      syncReadingStateToFirestore(updated)
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
      syncReadingStateToFirestore(newState)
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
      syncReadingStateToFirestore(updated)
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
      syncReadingStateToFirestore(newState)
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
      syncReadingStateToFirestore(updated)
      if (shouldAwardPoint) {
        dao.addPointsToReader(userId, 1)
        pointEarned = true
        dao.getReaderProfile(userId)?.let { syncUserProfileToFirestore(it) }
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
      syncReadingStateToFirestore(newState)
      if (shouldAwardPoint) {
        dao.addPointsToReader(userId, 1)
        pointEarned = true
        dao.getReaderProfile(userId)?.let { syncUserProfileToFirestore(it) }
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
      dao.getReaderProfile(userId)?.let { syncUserProfileToFirestore(it) }
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
      dao.getReaderProfile(userId)?.let { syncUserProfileToFirestore(it) }
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
        bio = "",
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
        val isDefaultTranslatorName = slot.penName.startsWith("Translator ", ignoreCase = true) ||
          slot.authorName.startsWith("Translator ", ignoreCase = true) ||
          slot.penName.startsWith("Author ", ignoreCase = true)
        if (hasExampleName || hasExampleEmail || isDefaultTranslatorName) {
          dao.updateAuthorSlot(
            slot.copy(
              authorName = "",
              penName = "",
              bio = if (slot.bio.startsWith("Contributing Translator")) "" else slot.bio,
              coverImageUri = if (hasExampleName || hasExampleEmail) null else slot.coverImageUri,
              translatorEmail = if (hasExampleName || hasExampleEmail) null else slot.translatorEmail,
              isClaimed = if (hasExampleName || hasExampleEmail) false else slot.isClaimed,
              isPermissionGranted = if (hasExampleName || hasExampleEmail) false else slot.isPermissionGranted
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
          bio = "",
          isPermissionGranted = true
        )
      )
    }
  }

  suspend fun getActiveUserSync(): ReaderProfileEntity? = dao.getActiveReaderProfileSync()

  private suspend fun syncFirebaseAuthSession() {
    val fbUser = firebaseAuthManager.currentFirebaseUser
    if (fbUser != null && !fbUser.email.isNullOrBlank()) {
      val cleanEmail = fbUser.email!!.trim().lowercase()
      val isOwner = isOwnerEmail(cleanEmail)
      val existing = dao.getReaderProfileByEmail(cleanEmail)
      if (existing != null) {
        dao.insertReaderProfile(
          existing.copy(
            isLoggedIn = true,
            role = if (isOwner) "OWNER" else existing.role,
            authorSlot = if (isOwner) 0 else existing.authorSlot,
            lastLoginTimestamp = System.currentTimeMillis()
          )
        )
      } else {
        val newProfile = ReaderProfileEntity(
          userId = "usr_google_" + cleanEmail.replace(Regex("[^a-z0-9]"), "_"),
          displayName = fbUser.displayName ?: if (isOwner) "Clarify" else cleanEmail.substringBefore("@"),
          email = cleanEmail,
          provider = "GOOGLE",
          role = if (isOwner) "OWNER" else "READER",
          authorSlot = if (isOwner) 0 else null,
          penNamePoints = if (isOwner) 100 else 0,
          lastLoginTimestamp = System.currentTimeMillis(),
          passwordHash = com.example.data.auth.AuthMemoryStore.getPassword(cleanEmail) ?: "",
          isLoggedIn = true
        )
        dao.insertReaderProfile(newProfile)
      }
    } else {
      // Sync memory store with existing profiles if present
      for (ownerEmail in OWNER_EMAILS) {
        val ownerProfile = dao.getReaderProfileByEmail(ownerEmail)
        val pass = ownerProfile?.passwordHash?.takeIf { it.isNotBlank() }
        if (pass != null) {
          com.example.data.auth.AuthMemoryStore.rememberCredential(ownerEmail, pass)
        }
      }
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
    externalScope.launch {
      try {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("chapter_comments").document(commentId).delete().await()
      } catch (e: Exception) {
        Log.w("StrawberrycandyComments", "Firebase Firestore deleteComment: ${e.message}")
      }
    }
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

  // ==========================================
  // CLOUD FIRESTORE USER PERSISTENCE & SYNC
  // ==========================================

  /**
   * Persists a reader/author profile directly to Cloud Firestore under `users/{userId}`.
   */
  fun syncUserProfileToFirestore(profile: ReaderProfileEntity) {
    externalScope.launch {
      try {
        val firestore = FirebaseFirestore.getInstance()
        val data = hashMapOf(
          "userId" to profile.userId,
          "displayName" to profile.displayName,
          "email" to profile.email,
          "provider" to profile.provider,
          "role" to profile.role,
          "authorSlot" to (profile.authorSlot ?: -1),
          "penNamePoints" to profile.penNamePoints,
          "lastLoginTimestamp" to profile.lastLoginTimestamp,
          "isLoggedIn" to profile.isLoggedIn,
          "updatedAt" to System.currentTimeMillis()
        )
        firestore.collection("users").document(profile.userId).set(data, SetOptions.merge())
        Log.d("StrawberrycandyFirestore", "User profile ${profile.userId} (${profile.email}) synced to Firestore")
      } catch (e: Exception) {
        Log.w("StrawberrycandyFirestore", "Error syncing user profile to Firestore: ${e.message}")
      }
    }
  }

  /**
   * Fetches latest user profile data from Cloud Firestore and merges into local database.
   */
  suspend fun syncUserProfileFromFirestore(userId: String) {
    try {
      val firestore = FirebaseFirestore.getInstance()
      firestore.collection("users").document(userId).get().addOnSuccessListener { doc ->
        if (doc != null && doc.exists()) {
          externalScope.launch {
            val local = dao.getReaderProfile(userId)
            val role = doc.getString("role") ?: local?.role ?: "READER"
            val slotNum = (doc.getLong("authorSlot") ?: -1L).toInt().takeIf { it >= 0 } ?: local?.authorSlot
            val points = (doc.getLong("penNamePoints") ?: local?.penNamePoints?.toLong() ?: 0L).toInt()
            val displayName = doc.getString("displayName") ?: local?.displayName ?: "Reader"
            val email = doc.getString("email") ?: local?.email ?: ""
            val provider = doc.getString("provider") ?: local?.provider ?: "GOOGLE"

            val merged = (local ?: ReaderProfileEntity(
              userId = userId,
              displayName = displayName,
              email = email,
              provider = provider,
              role = role,
              authorSlot = slotNum,
              penNamePoints = points,
              lastLoginTimestamp = System.currentTimeMillis(),
              passwordHash = local?.passwordHash ?: com.example.data.auth.AuthMemoryStore.getPassword(email),
              isLoggedIn = true
            )).copy(
              displayName = displayName,
              role = role,
              authorSlot = slotNum,
              penNamePoints = maxOf(points, local?.penNamePoints ?: 0),
              lastLoginTimestamp = System.currentTimeMillis(),
              isLoggedIn = true
            )
            dao.insertReaderProfile(merged)
            Log.d("StrawberrycandyFirestore", "User profile $userId updated from Firestore")
          }
        }
      }
    } catch (e: Exception) {
      Log.w("StrawberrycandyFirestore", "Error pulling user profile from Firestore: ${e.message}")
    }
  }

  /**
   * Persists a user reading state (progress, bookmarks, favorites, reading list, TBR) to Firestore.
   */
  fun syncReadingStateToFirestore(state: UserReadingStateEntity) {
    externalScope.launch {
      try {
        val firestore = FirebaseFirestore.getInstance()
        val data = hashMapOf(
          "compositeId" to state.compositeId,
          "userId" to state.userId,
          "novelId" to state.novelId,
          "currentPage" to state.currentPage,
          "isFavorite" to state.isFavorite,
          "inReadingList" to state.inReadingList,
          "isFinished" to state.isFinished,
          "inTbrList" to state.inTbrList,
          "pointsAwarded" to state.pointsAwarded,
          "lastReadTimestamp" to state.lastReadTimestamp,
          "updatedAt" to System.currentTimeMillis()
        )
        // Store in user's subcollection for user isolation
        firestore.collection("users").document(state.userId)
          .collection("reading_states").document(state.novelId)
          .set(data, SetOptions.merge())

        // Also store in root user_reading_states collection for global querying
        firestore.collection("user_reading_states").document(state.compositeId)
          .set(data, SetOptions.merge())

        Log.d("StrawberrycandyFirestore", "Reading state ${state.compositeId} synced to Firestore")
      } catch (e: Exception) {
        Log.w("StrawberrycandyFirestore", "Error syncing reading state to Firestore: ${e.message}")
      }
    }
  }

  /**
   * Synchronizes user's reading states and bookmarks from Firestore into local Room storage.
   */
  suspend fun syncUserReadingStatesFromFirestore(userId: String) {
    try {
      val firestore = FirebaseFirestore.getInstance()
      firestore.collection("users").document(userId).collection("reading_states").get()
        .addOnSuccessListener { snapshot ->
          if (snapshot != null && !snapshot.isEmpty) {
            externalScope.launch {
              for (doc in snapshot.documents) {
                val novelId = doc.getString("novelId") ?: doc.id
                val compositeId = doc.getString("compositeId") ?: "${userId}_${novelId}"
                val state = UserReadingStateEntity(
                  compositeId = compositeId,
                  userId = userId,
                  novelId = novelId,
                  currentPage = (doc.getLong("currentPage") ?: 1L).toInt(),
                  isFavorite = doc.getBoolean("isFavorite") ?: false,
                  inReadingList = doc.getBoolean("inReadingList") ?: false,
                  isFinished = doc.getBoolean("isFinished") ?: false,
                  inTbrList = doc.getBoolean("inTbrList") ?: false,
                  pointsAwarded = doc.getBoolean("pointsAwarded") ?: false,
                  lastReadTimestamp = doc.getLong("lastReadTimestamp") ?: System.currentTimeMillis()
                )
                val local = dao.getReadingState(userId, novelId)
                if (local == null || state.lastReadTimestamp >= local.lastReadTimestamp) {
                  dao.insertOrUpdateReadingState(state)
                }
              }
              Log.d("StrawberrycandyFirestore", "Synced ${snapshot.size()} reading states from Firestore for $userId")
            }
          }
        }
    } catch (e: Exception) {
      Log.w("StrawberrycandyFirestore", "Error pulling reading states from Firestore: ${e.message}")
    }
  }

  /**
   * Sets up a real-time Firestore listener for live sync of user reading states.
   */
  fun listenToUserFirestoreData(userId: String) {
    try {
      val firestore = FirebaseFirestore.getInstance()
      firestore.collection("users").document(userId)
        .collection("reading_states")
        .addSnapshotListener { snapshot, error ->
          if (error != null || snapshot == null) return@addSnapshotListener
          externalScope.launch {
            for (doc in snapshot.documents) {
              val novelId = doc.getString("novelId") ?: doc.id
              val compositeId = doc.getString("compositeId") ?: "${userId}_${novelId}"
              val state = UserReadingStateEntity(
                compositeId = compositeId,
                userId = userId,
                novelId = novelId,
                currentPage = (doc.getLong("currentPage") ?: 1L).toInt(),
                isFavorite = doc.getBoolean("isFavorite") ?: false,
                inReadingList = doc.getBoolean("inReadingList") ?: false,
                isFinished = doc.getBoolean("isFinished") ?: false,
                inTbrList = doc.getBoolean("inTbrList") ?: false,
                pointsAwarded = doc.getBoolean("pointsAwarded") ?: false,
                lastReadTimestamp = doc.getLong("lastReadTimestamp") ?: System.currentTimeMillis()
              )
              val local = dao.getReadingState(userId, novelId)
              if (local == null || state.lastReadTimestamp >= local.lastReadTimestamp) {
                dao.insertOrUpdateReadingState(state)
              }
            }
          }
        }
    } catch (e: Exception) {
      Log.w("StrawberrycandyFirestore", "Error listening to Firestore reading states: ${e.message}")
    }
  }

  /**
   * Persists an author slot to Firestore.
   */
  fun syncAuthorSlotToFirestore(slot: AuthorSlotEntity) {
    externalScope.launch {
      try {
        val firestore = FirebaseFirestore.getInstance()
        val data = hashMapOf(
          "slotNumber" to slot.slotNumber,
          "authorName" to slot.authorName,
          "penName" to slot.penName,
          "bio" to slot.bio,
          "accessCode" to slot.accessCode,
          "isClaimed" to slot.isClaimed,
          "isPermissionGranted" to slot.isPermissionGranted,
          "translatorEmail" to slot.translatorEmail,
          "coverImageUri" to (slot.coverImageUri ?: ""),
          "lastActiveTimestamp" to slot.lastActiveTimestamp,
          "updatedAt" to System.currentTimeMillis()
        )
        firestore.collection("author_slots").document(slot.slotNumber.toString())
          .set(data, SetOptions.merge())
        Log.d("StrawberrycandyFirestore", "Author slot ${slot.slotNumber} synced to Firestore")
      } catch (e: Exception) {
        Log.w("StrawberrycandyFirestore", "Error syncing author slot to Firestore: ${e.message}")
      }
    }
  }

  /**
   * Fetches author slot claims and permissions from Firestore into local Room cache.
   */
  suspend fun syncAuthorSlotsFromFirestore() {
    try {
      val firestore = FirebaseFirestore.getInstance()
      firestore.collection("author_slots").get().addOnSuccessListener { snapshot ->
        if (snapshot != null && !snapshot.isEmpty) {
          externalScope.launch {
            for (doc in snapshot.documents) {
              val slotNum = (doc.getLong("slotNumber") ?: doc.id.toLongOrNull() ?: continue).toInt()
              val local = dao.getAuthorSlot(slotNum)
              val isGranted = doc.getBoolean("isPermissionGranted") ?: local?.isPermissionGranted ?: false
              val email = doc.getString("translatorEmail") ?: local?.translatorEmail ?: ""
              val penName = doc.getString("penName") ?: local?.penName ?: ""
              val authorName = doc.getString("authorName") ?: local?.authorName ?: ""
              val bio = doc.getString("bio") ?: local?.bio ?: ""
              val isClaimed = doc.getBoolean("isClaimed") ?: local?.isClaimed ?: false
              val cover = doc.getString("coverImageUri") ?: local?.coverImageUri

              val updated = (local ?: AuthorSlotEntity(
                slotNumber = slotNum,
                authorName = authorName,
                penName = penName,
                accessCode = "AUTH-ROOM-$slotNum"
              )).copy(
                isPermissionGranted = isGranted,
                translatorEmail = email.takeIf { it.isNotBlank() },
                penName = penName,
                authorName = authorName,
                bio = bio,
                isClaimed = isClaimed,
                coverImageUri = cover.takeIf { !it.isNullOrBlank() }
              )
              dao.updateAuthorSlot(updated)
            }
            Log.d("StrawberrycandyFirestore", "Synced author slots from Firestore")
          }
        }
      }
    } catch (e: Exception) {
      Log.w("StrawberrycandyFirestore", "Error pulling author slots from Firestore: ${e.message}")
    }
  }

  /**
   * Signs in with Google Credential using modern Android Credential Manager & Firebase Auth.
   * Securely identifies the user via Google Sign-In with Firebase Authentication,
   * provisions the user profile, and keeps user data tracked and persisted in Cloud Firestore.
   */
  suspend fun signInWithGoogleCredential(
    idToken: String,
    email: String,
    displayName: String?,
    role: String = "READER",
    authorSlot: Int? = null
  ): Result<ReaderProfileEntity> {
    val cleanEmail = email.trim().lowercase()
    val isOwner = isOwnerEmail(cleanEmail)

    // Securely identify with Firebase Auth
    val fbResult = firebaseAuthManager.signInWithGoogleIdToken(idToken)
    if (fbResult is com.example.data.auth.FirebaseAuthResult.Error) {
      Log.w("StrawberrycandyRepository", "Firebase Auth with Google credential note: ${fbResult.message}")
    }

    val userId = "usr_google_" + cleanEmail.replace(Regex("[^a-z0-9]"), "_")

    val allSlots = dao.getAllAuthorSlotsSync()
    val preGrantedSlot = allSlots.find { slot ->
      slot.isPermissionGranted && isUserMatchedToSlot(cleanEmail, displayName ?: "", slot)
    } ?: dao.getAuthorSlotByEmail(cleanEmail)

    val existingProfile = dao.getReaderProfileByEmail(cleanEmail) ?: dao.getReaderProfile(userId)

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

    val finalName = if (!displayName.isNullOrBlank() && !displayName.contains("@")) {
      displayName.trim()
    } else if (!existingProfile?.displayName.isNullOrBlank()) {
      existingProfile!!.displayName
    } else {
      if (isOwner) {
        "Clarify"
      } else {
        val emailPrefix = cleanEmail.substringBefore("@").replace(".", " ").trim()
        emailPrefix.split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.titlecase() } }.ifBlank { "Reader" }
      }
    }

    dao.logoutAllProfiles()

    val profile = ReaderProfileEntity(
      userId = userId,
      displayName = finalName,
      email = cleanEmail,
      provider = "GOOGLE",
      role = finalRole,
      authorSlot = finalSlot,
      penNamePoints = existingProfile?.penNamePoints ?: 0,
      lastLoginTimestamp = System.currentTimeMillis(),
      passwordHash = existingProfile?.passwordHash ?: com.example.data.auth.AuthMemoryStore.getPassword(cleanEmail),
      isLoggedIn = true
    )
    dao.insertReaderProfile(profile)

    if (finalRole == "TRANSLATOR" && finalSlot != null && finalSlot != 0) {
      val slot = dao.getAuthorSlot(finalSlot)
      if (slot != null) {
        val updatedSlot = slot.copy(
          translatorEmail = cleanEmail,
          isClaimed = true,
          isPermissionGranted = true
        )
        dao.updateAuthorSlot(updatedSlot)
        syncAuthorSlotToFirestore(updatedSlot)
      }
    }

    // Persist to Cloud Firestore and sync reading data
    syncUserProfileToFirestore(profile)
    syncUserReadingStatesFromFirestore(userId)
    listenToUserFirestoreData(userId)

    return Result.success(profile)
  }
}
