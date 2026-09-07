package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.StrawberrycandyRepository
import com.example.data.local.AuthorSlotEntity
import com.example.data.local.BookmarkHighlightEntity
import com.example.data.local.ChapterCommentEntity
import com.example.data.local.NovelEntity
import com.example.data.local.ReaderProfileEntity
import com.example.data.local.StrawberrycandyDatabase
import com.example.data.remote.CloudArchiveSyncService
import com.example.model.NovelWithState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ShelfFilter {
  ALL,
  NEW_RELEASES,
  READING,
  FINISHED,
  TO_BE_READ,
  FAVORITES
}

enum class NovelSortOption(val label: String) {
  RECENTLY_READ("Recently Read"),
  TITLE("Title (A–Z)"),
  PROGRESS("Progress %"),
  NEWEST("Newest Additions")
}

private data class SearchFilterParams(
  val filter: ShelfFilter,
  val sort: NovelSortOption,
  val authorSlotFilter: Int?,
  val query: String
)

data class StrawberrycandyUiState(
  val activeUser: ReaderProfileEntity? = null,
  val novels: List<NovelWithState> = emptyList(),
  val allNovels: List<NovelWithState> = emptyList(),
  val authorSlots: List<AuthorSlotEntity> = emptyList(),
  val activeFilter: ShelfFilter = ShelfFilter.ALL,
  val activeSort: NovelSortOption = NovelSortOption.RECENTLY_READ,
  val selectedAuthorFilter: Int? = null, // null = all, 0 = Owner Strawberrycandy, 1..4 = Author Room 1..4
  val novelSearchQuery: String = "",
  val isAuthDialogOpen: Boolean = false,
  val isUploadDialogOpen: Boolean = false,
  val isProfileDialogOpen: Boolean = false,
  val message: String? = null,
  val authErrorMessage: String? = null,
  val totalFavoriteNovelsCount: Int = 0,
  val totalNovelsCount: Int = 0,
  val readingCount: Int = 0,
  val finishedCount: Int = 0,
  val toBeReadCount: Int = 0,
  val newlyPostedNovelAlert: NovelWithState? = null,
  val hasNewReleases: Boolean = false,
  val isRealtimeConnected: Boolean = true,
  val isCloudSyncing: Boolean = false,
  val lastCloudSyncTime: Long = 0L,
  val cloudSyncedNovelsCount: Int = 0,
  val cloudSyncError: String? = null,
  val cloudPublishModalNovel: NovelEntity? = null,
  val cloudPublishModalNovelJson: String? = null,
  val cloudPublishModalCatalogJson: String? = null,
  val isAlreadyCloudPublished: Boolean = false,
  val rememberedAccounts: List<ReaderProfileEntity> = emptyList(),
)

class StrawberrycandyViewModel(application: Application) : AndroidViewModel(application) {
  private val database = StrawberrycandyDatabase.getInstance(application)
  private val syncService = CloudArchiveSyncService(application)
  private val repository = StrawberrycandyRepository(database.strawberrycandyDao(), syncService = syncService)

  private val _activeFilter = MutableStateFlow(ShelfFilter.ALL)
  private val _activeSort = MutableStateFlow(NovelSortOption.NEWEST)
  private val _selectedAuthorFilter = MutableStateFlow<Int?>(null)
  private val _novelSearchQuery = MutableStateFlow("")
  val novelSearchQuery: StateFlow<String> = _novelSearchQuery.asStateFlow()

  private val _isAuthDialogOpen = MutableStateFlow(false)
  private val _authErrorMessage = MutableStateFlow<String?>(null)
  private val _isUploadDialogOpen = MutableStateFlow(false)
  private val _isProfileDialogOpen = MutableStateFlow(false)
  private val _snackbarMessage = MutableStateFlow<String?>(null)
  private val _dismissedAlertNovelId = MutableStateFlow<String?>(null)

  private val _isCloudSyncing = MutableStateFlow(false)
  private val _lastCloudSyncTime = MutableStateFlow(syncService.getLastSyncTime())
  private val _cloudSyncedNovelsCount = MutableStateFlow(syncService.getLastSyncCount())
  private val _cloudSyncError = MutableStateFlow(syncService.getLastSyncError())
  private val _cloudPublishModalNovel = MutableStateFlow<NovelEntity?>(null)
  private val _cloudPublishModalNovelJson = MutableStateFlow<String?>(null)
  private val _cloudPublishModalCatalogJson = MutableStateFlow<String?>(null)
  private val _isAlreadyCloudPublished = MutableStateFlow(false)

  val activeUser: StateFlow<ReaderProfileEntity?> = repository.activeUser
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  init {
    viewModelScope.launch {
      repository.syncRemoteNovels()
      repository.syncRemoteAuthorSlots()
      repository.checkAndUpgradeProfilesAgainstSlots()
    }
  }

  val allNovels: StateFlow<List<NovelWithState>> = repository.allNovelsWithState
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val authorSlots: StateFlow<List<AuthorSlotEntity>> = repository.authorSlots
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  @OptIn(ExperimentalCoroutinesApi::class)
  val myCommentsHistory: StateFlow<List<ChapterCommentEntity>> = repository.activeUser
    .flatMapLatest { user ->
      if (user != null) {
        repository.getCommentsForReader(user.email, user.displayName)
      } else {
        flowOf(emptyList())
      }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  private val _filterState = combine(
    _activeFilter,
    _activeSort,
    _selectedAuthorFilter,
    _novelSearchQuery
  ) { filter, sort, authorSlot, query ->
    SearchFilterParams(filter, sort, authorSlot, query)
  }

  private val _dialogState = combine(
    _isAuthDialogOpen,
    _isUploadDialogOpen,
    _isProfileDialogOpen,
    _snackbarMessage,
    combine(_authErrorMessage, _dismissedAlertNovelId) { authErr, dismissedId ->
      Pair(authErr, dismissedId)
    }
  ) { isAuth, isUpload, isProfile, msg, extra ->
    listOf(isAuth, isUpload, isProfile, msg, extra.first, extra.second)
  }

  private val _cloudState = combine(
    _isCloudSyncing,
    _lastCloudSyncTime,
    _cloudSyncedNovelsCount,
    _cloudSyncError,
    combine(
      _cloudPublishModalNovel,
      _cloudPublishModalNovelJson,
      _cloudPublishModalCatalogJson,
      _isAlreadyCloudPublished
    ) { novel, json, catJson, isPub ->
      listOf(novel, json, catJson, isPub)
    }
  ) { syncing, time, count, err, modalExtra ->
    listOf(syncing, time, count, err, modalExtra[0], modalExtra[1], modalExtra[2], modalExtra[3])
  }

  private val _dialogAndCloudState = combine(_dialogState, _cloudState) { dialogs, cloud ->
    Pair(dialogs, cloud)
  }

  private val _userAndAccountsState = combine(repository.activeUser, repository.rememberedAccounts) { user, accounts ->
    Pair(user, accounts)
  }

  val uiState: StateFlow<StrawberrycandyUiState> = combine(
    _userAndAccountsState,
    repository.allNovelsWithState,
    repository.authorSlots,
    _filterState,
    _dialogAndCloudState
  ) { userAccounts, novels, slots, searchParams, combinedState ->
    val user = userAccounts.first
    val remembered = userAccounts.second
    val dialogList = combinedState.first
    val cloudList = combinedState.second
    val isAuthOpen = dialogList[0] as Boolean
    val isUploadOpen = dialogList[1] as Boolean
    val isProfileOpen = dialogList[2] as Boolean
    val msg = dialogList[3] as String?
    val authErr = dialogList[4] as String?
    val dismissedId = dialogList[5] as String?

    val isCloudSyncing = cloudList[0] as Boolean
    val lastCloudSyncTime = cloudList[1] as Long
    val cloudSyncedCount = cloudList[2] as Int
    val cloudSyncErr = cloudList[3] as String?
    val cloudModalNovel = cloudList[4] as NovelEntity?
    val cloudModalNovelJson = cloudList[5] as String?
    val cloudModalCatalogJson = cloudList[6] as String?
    val isAlreadyPublished = cloudList[7] as Boolean

    val newestNovel = novels.maxByOrNull { it.novel.createdAt }
    val isAlertVisible = newestNovel != null && newestNovel.isNewRelease && newestNovel.id != dismissedId
    val alertNovel = if (isAlertVisible) newestNovel else null
    val hasNewReleases = novels.any { it.isNewRelease }

    val readingCount = novels.count { it.isReading }
    val finishedCount = novels.count { it.isFinished }
    val toBeReadCount = novels.count { it.isToBeRead }
    val totalFavorites = novels.count { it.isFavorite }
    val totalCount = novels.size

    val query = searchParams.query.trim().lowercase()
    val searchFiltered = if (query.isEmpty()) {
      novels
    } else {
      val tokens = query.split(Regex("""\s+""")).filter { it.isNotBlank() }
      novels.filter { novel ->
        val textToSearch = listOf(
          novel.title,
          novel.subtitle,
          novel.author,
          novel.originalAuthor,
          novel.excerpt,
          novel.chapterTitle,
          novel.contentText
        ).joinToString(" ").lowercase()

        textToSearch.contains(query) || (tokens.isNotEmpty() && tokens.all { textToSearch.contains(it) })
      }.sortedWith(
        compareByDescending<NovelWithState> { it.title.lowercase().contains(query) }
          .thenByDescending { it.author.lowercase().contains(query) }
          .thenByDescending { it.createdAt }
      )
    }

    val filteredNovels = if (query.isNotEmpty()) {
      // When searching via search bar, search across the entire library so users can always find their novel
      searchFiltered
    } else {
      searchFiltered
        .filter { novel ->
          when (searchParams.filter) {
            ShelfFilter.ALL -> true
            ShelfFilter.NEW_RELEASES -> novel.isNewRelease || (newestNovel != null && novel.id == newestNovel.id)
            ShelfFilter.READING -> novel.isReading
            ShelfFilter.FINISHED -> novel.isFinished
            ShelfFilter.TO_BE_READ -> novel.isToBeRead
            ShelfFilter.FAVORITES -> novel.isFavorite
          }
        }
        .filter { novel ->
          if (searchParams.authorSlotFilter == null) true else novel.authorSlot == searchParams.authorSlotFilter
        }
    }

    val sortedNovels = when (searchParams.sort) {
      NovelSortOption.RECENTLY_READ -> filteredNovels.sortedWith(
        compareByDescending<NovelWithState> { it.isNewRelease }
          .thenByDescending { it.lastReadTimestamp }
          .thenByDescending { it.currentPage > 1 }
          .thenByDescending { it.createdAt }
      )
      NovelSortOption.TITLE -> filteredNovels.sortedBy { it.title.lowercase() }
      NovelSortOption.PROGRESS -> filteredNovels.sortedByDescending { it.progressFraction }
      NovelSortOption.NEWEST -> filteredNovels.sortedByDescending { it.createdAt }
    }

    StrawberrycandyUiState(
      activeUser = user,
      novels = sortedNovels,
      allNovels = novels,
      authorSlots = slots,
      activeFilter = searchParams.filter,
      activeSort = searchParams.sort,
      selectedAuthorFilter = searchParams.authorSlotFilter,
      novelSearchQuery = searchParams.query,
      isAuthDialogOpen = isAuthOpen,
      isUploadDialogOpen = isUploadOpen,
      isProfileDialogOpen = isProfileOpen,
      message = msg,
      authErrorMessage = authErr,
      totalFavoriteNovelsCount = totalFavorites,
      totalNovelsCount = totalCount,
      readingCount = readingCount,
      finishedCount = finishedCount,
      toBeReadCount = toBeReadCount,
      newlyPostedNovelAlert = alertNovel,
      hasNewReleases = hasNewReleases,
      isRealtimeConnected = true,
      isCloudSyncing = isCloudSyncing,
      lastCloudSyncTime = lastCloudSyncTime,
      cloudSyncedNovelsCount = cloudSyncedCount,
      cloudSyncError = cloudSyncErr,
      cloudPublishModalNovel = cloudModalNovel,
      cloudPublishModalNovelJson = cloudModalNovelJson,
      cloudPublishModalCatalogJson = cloudModalCatalogJson,
      isAlreadyCloudPublished = isAlreadyPublished,
      rememberedAccounts = remembered,
    )
  }.stateIn(
    viewModelScope,
    SharingStarted.WhileSubscribed(5000),
    StrawberrycandyUiState()
  )

  fun setNovelSearchQuery(query: String) {
    _novelSearchQuery.value = query
  }

  fun clearNovelSearchQuery() {
    _novelSearchQuery.value = ""
  }

  fun setFilter(filter: ShelfFilter) {
    _activeFilter.value = filter
  }

  fun dismissNewNovelAlert() {
    val currentAlert = uiState.value.newlyPostedNovelAlert
    if (currentAlert != null) {
      _dismissedAlertNovelId.value = currentAlert.id
    }
  }

  fun showNewReleasesOnly() {
    _activeFilter.value = ShelfFilter.NEW_RELEASES
  }

  fun setSort(sort: NovelSortOption) {
    _activeSort.value = sort
  }

  fun openProfileDialog() {
    _isProfileDialogOpen.value = true
  }

  fun closeProfileDialog() {
    _isProfileDialogOpen.value = false
  }

  fun setAuthorFilter(slot: Int?) {
    _selectedAuthorFilter.value = slot
  }

  fun openAuthDialog() {
    _authErrorMessage.value = null
    _isAuthDialogOpen.value = true
  }

  fun closeAuthDialog() {
    _authErrorMessage.value = null
    _isAuthDialogOpen.value = false
  }

  fun clearAuthError() {
    _authErrorMessage.value = null
  }

  fun openUploadDialog() {
    _isUploadDialogOpen.value = true
  }

  fun closeUploadDialog() {
    _isUploadDialogOpen.value = false
  }

  companion object {
    val OWNER_EMAILS = StrawberrycandyRepository.OWNER_EMAILS

    fun isOwnerEmail(email: String?): Boolean {
      return StrawberrycandyRepository.isOwnerEmail(email)
    }
  }

  fun isOwner(user: ReaderProfileEntity? = activeUser.value): Boolean {
    if (user == null) return false
    return isOwnerEmail(user.email) || (user.role == "OWNER" && isOwnerEmail(user.email))
  }

  fun isPermittedTranslator(
    user: ReaderProfileEntity? = activeUser.value,
    slots: List<AuthorSlotEntity> = authorSlots.value
  ): Boolean {
    if (user == null) return false
    if (isOwner(user)) return true
    if (user.role.equals("TRANSLATOR", ignoreCase = true)) return true
    val matchedSlot = slots.find { slot ->
      slot.isPermissionGranted && StrawberrycandyRepository.isUserMatchedToSlot(user.email, user.displayName, slot)
    }
    return matchedSlot != null
  }

  fun canEditNovel(
    user: ReaderProfileEntity? = activeUser.value,
    slots: List<AuthorSlotEntity> = authorSlots.value
  ): Boolean {
    if (user == null) return false
    if (isOwner(user)) return true
    if (user.role == "TRANSLATOR") return true
    return isPermittedTranslator(user, slots)
  }

  fun canEditSpecificNovel(
    novel: NovelWithState?,
    user: ReaderProfileEntity? = activeUser.value,
    slots: List<AuthorSlotEntity> = authorSlots.value
  ): Boolean {
    if (novel == null) return false
    if (user == null) return false
    // 1. Archive owner has universal edit access across all novels
    if (isOwner(user)) return true

    // 2. Translators can only edit novels in their own room
    if (user.role == "TRANSLATOR" || user.authorSlot != null || isPermittedTranslator(user, slots)) {
      val userSlot = user.authorSlot ?: slots.find {
        it.translatorEmail?.equals(user.email, ignoreCase = true) == true
      }?.slotNumber

      if (userSlot != null && userSlot > 0 && novel.authorSlot == userSlot) {
        return true
      }
    }
    return false
  }

  fun isTranslatorOrOwner(user: ReaderProfileEntity? = activeUser.value, slots: List<AuthorSlotEntity> = authorSlots.value): Boolean {
    if (user == null) return false
    if (user.role.equals("OWNER", ignoreCase = true) || isOwnerEmail(user.email)) return true
    if (user.role.equals("TRANSLATOR", ignoreCase = true)) return true
    if (isPermittedTranslator(user, slots)) return true
    return false
  }

  fun canUploadNovel(user: ReaderProfileEntity? = activeUser.value, slots: List<AuthorSlotEntity> = authorSlots.value): Boolean {
    if (user == null) return false
    if (user.role.equals("READER", ignoreCase = true)) return false
    if (user.role.equals("OWNER", ignoreCase = true) || isOwnerEmail(user.email)) return true
    if (user.role.equals("TRANSLATOR", ignoreCase = true)) {
      if (user.authorSlot != null && user.authorSlot > 0) return true
      if (isPermittedTranslator(user, slots)) return true
    }
    return false
  }

  fun signInWithGoogle(
    email: String,
    password: String = "",
    displayName: String = "",
    role: String = "READER",
    authorSlot: Int? = null,
    onError: ((String) -> Unit)? = null
  ) {
    viewModelScope.launch {
      val result = repository.signIn(
        provider = "GOOGLE",
        email = email,
        password = password,
        displayName = displayName,
        role = role,
        authorSlot = authorSlot
      )
      if (result.isSuccess) {
        _isAuthDialogOpen.value = false
        _authErrorMessage.value = null
        val isOwnerUser = isOwnerEmail(email)
        val roleLabel = if (isOwnerUser) "Sole Archive Owner" else if (role == "TRANSLATOR") "Translator" else "Reader"
        _snackbarMessage.value = "Signed in as $roleLabel ($email)"
      } else {
        val error = result.exceptionOrNull()?.message ?: "Sign in failed"
        _authErrorMessage.value = error
        _snackbarMessage.value = error
        onError?.invoke(error)
      }
    }
  }

  fun signInWithApple(
    email: String,
    password: String = "",
    displayName: String = "",
    role: String = "READER",
    authorSlot: Int? = null,
    onError: ((String) -> Unit)? = null
  ) {
    viewModelScope.launch {
      val result = repository.signIn(
        provider = "APPLE",
        email = email,
        password = password,
        displayName = displayName,
        role = role,
        authorSlot = authorSlot
      )
      if (result.isSuccess) {
        _isAuthDialogOpen.value = false
        _authErrorMessage.value = null
        val isOwnerUser = isOwnerEmail(email)
        val roleLabel = if (isOwnerUser) "Sole Archive Owner" else if (role == "TRANSLATOR") "Translator" else "Reader"
        _snackbarMessage.value = "Signed in as $roleLabel ($email)"
      } else {
        val error = result.exceptionOrNull()?.message ?: "Sign in failed"
        _authErrorMessage.value = error
        _snackbarMessage.value = error
        onError?.invoke(error)
      }
    }
  }

  fun requestPasswordResetCode(email: String, onResult: (Result<String>) -> Unit) {
    viewModelScope.launch {
      val result = repository.sendPasswordRecoveryCode(email)
      onResult(result)
    }
  }

  fun resetPasswordWithCode(
    email: String,
    code: String,
    newPassword: String,
    onResult: (Result<Unit>) -> Unit
  ) {
    viewModelScope.launch {
      val result = repository.resetPasswordWithCode(email, code, newPassword)
      if (result.isSuccess) {
        _isAuthDialogOpen.value = false
        _authErrorMessage.value = null
        _snackbarMessage.value = "Password reset successfully! Logged in as $email"
      }
      onResult(result)
    }
  }

  fun grantPermissionByEmail(email: String, slotNumber: Int? = null) {
    viewModelScope.launch {
      repository.grantPermissionByEmail(email, slotNumber)
      _snackbarMessage.value = "✨ Translator permission granted to $email! They can now sign in to post and edit novels."
    }
  }

  fun updateAuthorSlotByOwner(
    slotNumber: Int,
    translatorEmail: String?,
    penName: String,
    bio: String,
    isPermissionGranted: Boolean
  ) {
    viewModelScope.launch {
      repository.updateAuthorSlotByOwner(slotNumber, translatorEmail, penName, bio, isPermissionGranted)
      _snackbarMessage.value = if (isPermissionGranted) {
        "Translator permission granted to ${translatorEmail ?: "Seat $slotNumber"}"
      } else {
        "Updated Seat $slotNumber"
      }
    }
  }

  fun signOut() {
    viewModelScope.launch {
      repository.signOut()
      _snackbarMessage.value = "Signed out"
    }
  }

  fun recordNovelRead(novelId: String) {
    viewModelScope.launch {
      repository.recordNovelRead(novelId)
    }
  }

  fun toggleFavorite(novelId: String) {
    val userId = activeUser.value?.userId ?: "guest_reader"
    viewModelScope.launch {
      repository.toggleFavorite(userId, novelId)
    }
  }

  fun toggleReadingList(novelId: String) {
    val user = activeUser.value
    if (user == null) {
      _isAuthDialogOpen.value = true
      _snackbarMessage.value = "Please sign in to save novels to your reading list"
      return
    }
    viewModelScope.launch {
      repository.toggleReadingList(user.userId, novelId)
    }
  }

  fun saveReadingProgress(novelId: String, page: Int) {
    val user = activeUser.value ?: return
    viewModelScope.launch {
      val earnedPoint = repository.saveReadingProgress(user.userId, novelId, page)
      if (earnedPoint) {
        _snackbarMessage.value = "Novel Completed! +1 Pen Name Point earned! ⭐"
      }
    }
  }

  fun markNovelAsFinished(novelId: String) {
    val user = activeUser.value
    val effectiveUserId = user?.userId ?: "guest_reader"
    viewModelScope.launch {
      val earnedPoint = repository.markNovelAsFinished(effectiveUserId, novelId)
      if (earnedPoint && user != null) {
        _snackbarMessage.value = "🏆 Novel Finished! +1 Pen Name Point earned! ⭐"
      } else {
        _snackbarMessage.value = "🏆 Novel Finished! Marked as Completed in your library ✓"
      }
    }
  }

  fun markNovelAsToBeRead(novelId: String) {
    val user = activeUser.value
    if (user == null) {
      _isAuthDialogOpen.value = true
      _snackbarMessage.value = "Please sign in to add to your To-Be-Read list"
      return
    }
    viewModelScope.launch {
      repository.markNovelAsToBeRead(user.userId, novelId)
      _snackbarMessage.value = "Added to To-Be-Read list 🔖"
    }
  }

  fun markNovelAsReading(novelId: String) {
    val user = activeUser.value
    if (user == null) {
      _isAuthDialogOpen.value = true
      _snackbarMessage.value = "Please sign in to track your reading shelf"
      return
    }
    viewModelScope.launch {
      repository.markNovelAsReading(user.userId, novelId)
      _snackbarMessage.value = "Moved to Reading shelf 📖"
    }
  }

  fun changePenNameWithPoint(newPenName: String) {
    val user = activeUser.value
    if (user == null) {
      _snackbarMessage.value = "Sign in to your account to redeem points for pen name updates."
      return
    }
    viewModelScope.launch {
      repository.updateReaderDisplayName(user.userId, newPenName)
      if (user.authorSlot != null) {
        val slot = uiState.value.authorSlots.find { it.slotNumber == user.authorSlot }
        if (slot != null) {
          repository.updateAuthorSlot(user.authorSlot, newPenName, newPenName, slot.bio)
        }
      }
      _snackbarMessage.value = "Pen name updated to '$newPenName' ✨"
    }
  }

  fun updateReaderName(newName: String) {
    val cleanName = newName.trim()
    if (cleanName.isBlank()) {
      _snackbarMessage.value = "Please enter a valid display name."
      return
    }
    val user = activeUser.value
    if (user == null) {
      // Guest reader updating their pen name: do not open auth modal, confirm directly
      _snackbarMessage.value = "Reader pen name set to '$cleanName' ✓"
      return
    }
    viewModelScope.launch {
      val success = repository.updateReaderDisplayName(user.userId, cleanName)
      if (success) {
        _snackbarMessage.value = "Reader name updated to '$cleanName' ✨"
      } else {
        _snackbarMessage.value = "Please enter a valid display name."
      }
    }
  }

  fun updateAuthorSlotWithPoint(slotNumber: Int, authorName: String, penName: String, bio: String) {
    updateAuthorSlot(slotNumber, authorName, penName, bio)
  }

  fun uploadNovel(
    title: String,
    subtitle: String,
    chapterTitle: String,
    excerpt: String,
    content: String,
    coverColorHex: Long,
    author: String = "Strawberrycandy",
    authorSlot: Int = 0,
    coverImageUri: String? = null,
    storyImagesJson: String = "",
    originalAuthor: String = "",
    novelStatus: String = "ONGOING",
    releaseFormat: String = "CHAPTER",
  ) {
    val currentUser = activeUser.value
    if (currentUser == null || !canUploadNovel(currentUser)) {
      _snackbarMessage.value = "Publishing is restricted to Archive Translators and the Owner."
      return
    }
    val isOwnerUser = isOwner(currentUser)
    val effectiveSlot = if (isOwnerUser) {
      authorSlot
    } else {
      if (authorSlot == 0) (currentUser.authorSlot ?: 1) else authorSlot
    }
    val effectiveAuthor = if (effectiveSlot == 0 && isOwnerUser) {
      "Strawberrycandy"
    } else {
      author.trim().ifBlank { currentUser.displayName.ifBlank { "Translator $effectiveSlot" } }
    }

    viewModelScope.launch {
      if (!isOwnerUser && (currentUser.authorSlot == null || currentUser.role != "TRANSLATOR")) {
        repository.grantPermissionByEmail(currentUser.email, effectiveSlot)
      }

      val uploadResult = repository.uploadNovel(
        title = title,
        subtitle = subtitle,
        chapterTitle = chapterTitle,
        excerpt = excerpt,
        content = content,
        coverColorHex = coverColorHex,
        author = effectiveAuthor,
        authorSlot = effectiveSlot,
        coverImageUri = coverImageUri,
        storyImagesJson = storyImagesJson,
        originalAuthor = originalAuthor,
        novelStatus = novelStatus,
        releaseFormat = releaseFormat,
      )
      _isUploadDialogOpen.value = false
      _dismissedAlertNovelId.value = null
      val authorLabel = if (effectiveSlot == 0 && isOwnerUser) "Strawberrycandy" else "$effectiveAuthor (Translator Room $effectiveSlot)"

      if (uploadResult.isPublishedToCloud) {
        _snackbarMessage.value = "✨ Novel '$title' by $authorLabel is now live on the backend server & Cloud Archive!"
        refreshCloudArchive(silent = true)
      } else {
        // Saved locally in SQLite
        _snackbarMessage.value = "✨ Novel '$title' published to your library! Ready to read offline & queued for cloud sync."
      }
    }
  }

  fun dismissCloudPublishModal() {
    _cloudPublishModalNovel.value = null
    _cloudPublishModalNovelJson.value = null
    _cloudPublishModalCatalogJson.value = null
  }

  fun publishPendingNovelToCloud(
    token: String?,
    writeUrl: String?,
    onDone: (success: Boolean, message: String) -> Unit
  ) {
    val novel = _cloudPublishModalNovel.value ?: return
    viewModelScope.launch {
      val res = repository.publishNovelDirectToCloud(novel.id, token, writeUrl)
      if (res.isSuccess) {
        _isAlreadyCloudPublished.value = true
        _snackbarMessage.value = "✨ Global Archive: '${novel.title}' is now live for all APK readers worldwide!"
        onDone(true, res.getOrNull() ?: "Successfully published to Global Cloud Archive!")
        refreshCloudArchive(silent = true)
      } else {
        val err = res.exceptionOrNull()?.message ?: "Publication failed"
        onDone(false, err)
      }
    }
  }

  fun refreshCloudArchive(silent: Boolean = false) {
    viewModelScope.launch {
      _isCloudSyncing.value = true
      val res = repository.syncRemoteNovels()
      repository.syncRemoteAuthorSlots()
      repository.checkAndUpgradeProfilesAgainstSlots()
      _isCloudSyncing.value = false
      _lastCloudSyncTime.value = System.currentTimeMillis()
      if (res.isSuccess) {
        val count = res.getOrNull() ?: 0
        _cloudSyncedNovelsCount.value = count
        _cloudSyncError.value = null
        if (!silent) {
          _snackbarMessage.value = "✨ Cloud Archive Synced: $count novels live across all APKs"
        }
      } else {
        val err = res.exceptionOrNull()?.message ?: "Unable to sync cloud archive"
        _cloudSyncError.value = err
        if (!silent) {
          _snackbarMessage.value = "Cloud Archive sync issue: $err"
        }
      }
    }
  }

  fun getCloudReadUrl(): String = syncService.getReadUrl()
  fun setCloudReadUrl(url: String) = syncService.setReadUrl(url)
  fun getCloudWriteUrl(): String = syncService.getWriteUrl()
  fun setCloudWriteUrl(url: String) = syncService.setWriteUrl(url)
  fun getGitHubToken(): String = syncService.getGitHubToken()
  fun setGitHubToken(token: String) = syncService.setGitHubToken(token)

  fun publishUpdateManifest(
    versionName: String,
    versionCode: Int,
    title: String,
    changelog: String,
    apkUrl: String,
    releasePageUrl: String,
    onComplete: (Result<String>) -> Unit
  ) {
    viewModelScope.launch {
      val token = syncService.getGitHubToken()
      if (token.isBlank()) {
        onComplete(Result.failure(Exception("GitHub token is not configured. Please enter your GitHub Personal Access Token in the settings tab.")))
        return@launch
      }
      val res = syncService.publishUpdateManifestToGitHub(
        versionName = versionName,
        versionCode = versionCode,
        title = title,
        changelog = changelog,
        apkUrl = apkUrl,
        releasePageUrl = releasePageUrl,
        token = token
      )
      if (res.isSuccess) {
        _snackbarMessage.value = "✨ Update manifest published to GitHub!"
      }
      onComplete(res)
    }
  }

  fun createGitHubReleaseTag(
    tagName: String,
    releaseTitle: String,
    releaseNotes: String,
    targetBranch: String = "main",
    isDraft: Boolean = false,
    alsoUpdateManifest: Boolean = true,
    versionCode: Int = 2,
    onComplete: (Result<CloudArchiveSyncService.GitHubReleaseTagResult>) -> Unit
  ) {
    viewModelScope.launch {
      val token = syncService.getGitHubToken()
      if (token.isBlank()) {
        onComplete(Result.failure(Exception("GitHub token is not configured. Please enter your GitHub Personal Access Token in the settings tab, or use 'Open GitHub Web' to create the release tag.")))
        return@launch
      }

      val result = syncService.createGitHubReleaseTag(
        tagName = tagName,
        releaseTitle = releaseTitle,
        releaseNotes = releaseNotes,
        targetBranch = targetBranch,
        isDraft = isDraft,
        token = token
      )

      if (result.isSuccess) {
        val data = result.getOrNull()
        if (alsoUpdateManifest && data != null) {
          val cleanVersion = data.tagName.removePrefix("v").removePrefix("V")
          syncService.publishUpdateManifestToGitHub(
            versionName = cleanVersion,
            versionCode = versionCode,
            title = releaseTitle.ifBlank { "Strawberrycandy ${data.tagName}" },
            changelog = releaseNotes,
            apkUrl = "https://github.com/clarifyrgb/strawberrycandy/releases/download/${data.tagName}/Strawberrycandy.apk",
            releasePageUrl = data.htmlUrl,
            token = token
          )
        }
        _snackbarMessage.value = "🏷️ Release tag '${data?.tagName}' created on GitHub!"
      }
      onComplete(result)
    }
  }

  // Chapter Comments
  fun getCommentsForChapter(novelId: String, chapterTitle: String): Flow<List<ChapterCommentEntity>> {
    return repository.getCommentsForChapter(novelId, chapterTitle)
  }

  fun getAllCommentsForNovel(novelId: String): Flow<List<ChapterCommentEntity>> {
    return repository.getAllCommentsForNovel(novelId)
  }

  fun listenToCloudComments(novelId: String) {
    repository.listenToCloudComments(novelId)
  }

  fun syncRemoteComments(novelId: String? = null) {
    viewModelScope.launch {
      repository.syncRemoteComments(novelId)
    }
  }

  fun getCommentCountForChapter(novelId: String, chapterTitle: String): Flow<Int> {
    return repository.getCommentCountForChapter(novelId, chapterTitle)
  }

  fun postComment(
    novelId: String,
    chapterTitle: String,
    text: String,
    penName: String? = null,
    parentCommentId: String? = null,
    replyToReaderName: String? = null,
  ) {
    val user = activeUser.value
    val readerName = penName?.trim()?.takeIf { it.isNotBlank() }
      ?: user?.displayName
      ?: "Reader • Literary Guest"
    val readerEmail = user?.email ?: ""
    val avatarHex = when {
      user?.provider == "GOOGLE" -> 0xFF2A52BE
      user?.provider == "APPLE" -> 0xFF333333
      else -> 0xFF5C2D3B
    }

    viewModelScope.launch {
      repository.addComment(
        novelId = novelId,
        chapterTitle = chapterTitle,
        readerName = readerName,
        readerEmail = readerEmail,
        commentText = text,
        avatarColorHex = avatarHex,
        parentCommentId = parentCommentId,
        replyToReaderName = replyToReaderName,
      )
      _snackbarMessage.value = if (replyToReaderName != null) {
        "☁️ Reply posted online to @$replyToReaderName"
      } else {
        "☁️ Comment posted online to Cloud Archive"
      }
    }
  }

  fun likeComment(commentId: String) {
    viewModelScope.launch {
      repository.likeComment(commentId)
    }
  }

  fun deleteComment(commentId: String) {
    viewModelScope.launch {
      repository.deleteComment(commentId)
      _snackbarMessage.value = "Reflection removed from archive"
    }
  }

  // Bookmarks & Highlighting
  fun getBookmarksForNovel(novelId: String): Flow<List<BookmarkHighlightEntity>> {
    return repository.getBookmarksForNovel(novelId)
  }

  fun toggleBookmarkLine(novelId: String, chapterTitle: String, quoteText: String, paragraphIndex: Int) {
    viewModelScope.launch {
      val added = repository.toggleBookmark(novelId, chapterTitle, quoteText, paragraphIndex)
      _snackbarMessage.value = if (added) "Line bookmarked to your collection" else "Bookmark removed"
    }
  }

  fun saveHighlight(
    novelId: String,
    chapterTitle: String,
    quoteText: String,
    paragraphIndex: Int,
    colorHex: Long,
    note: String = ""
  ) {
    viewModelScope.launch {
      val added = repository.saveHighlight(novelId, chapterTitle, quoteText, paragraphIndex, colorHex, note)
      _snackbarMessage.value = if (added) "Line highlighted in your favorite colors" else "Highlight removed"
    }
  }

  fun deleteBookmark(bookmarkId: String) {
    viewModelScope.launch {
      repository.deleteBookmark(bookmarkId)
      _snackbarMessage.value = "Bookmark removed"
    }
  }

  fun updateAuthorSlot(slotNumber: Int, authorName: String, penName: String, bio: String) {
    val user = activeUser.value
    if (!isOwner(user)) {
      val userSlot = user?.authorSlot ?: authorSlots.value.find {
        it.translatorEmail?.equals(user?.email, ignoreCase = true) == true
      }?.slotNumber
      if (userSlot == null || userSlot != slotNumber) {
        _snackbarMessage.value = "Translators can only modify their own room"
        return
      }
    }
    viewModelScope.launch {
      repository.updateAuthorSlot(slotNumber, authorName, penName, bio)
      val user = activeUser.value
      if (user != null && user.authorSlot == slotNumber) {
        repository.updateReaderDisplayName(user.userId, penName)
      }
      _snackbarMessage.value = "Translator Profile updated ($penName)"
    }
  }

  fun addChapterToNovel(novelId: String, chapterTitle: String, chapterContent: String) {
    viewModelScope.launch {
      repository.addChapterToNovel(novelId, chapterTitle, chapterContent)
      _snackbarMessage.value = "Chapter '$chapterTitle' added successfully!"
    }
  }

  fun updateAuthorSlotCover(slotNumber: Int, coverImageUri: String) {
    val user = activeUser.value
    if (!isOwner(user)) {
      val userSlot = user?.authorSlot ?: authorSlots.value.find {
        it.translatorEmail?.equals(user?.email, ignoreCase = true) == true
      }?.slotNumber
      if (userSlot == null || userSlot != slotNumber) {
        _snackbarMessage.value = "Translators can only modify their own room"
        return
      }
    }
    viewModelScope.launch {
      repository.updateAuthorSlotCover(slotNumber, coverImageUri)
      _snackbarMessage.value = "Translator photo updated!"
    }
  }

  fun setSlotPermission(slotNumber: Int, isGranted: Boolean) {
    viewModelScope.launch {
      repository.setSlotPermission(slotNumber, isGranted)
      _snackbarMessage.value = if (isGranted) "Permission granted for Slot $slotNumber" else "Permission revoked for Slot $slotNumber"
    }
  }

  fun deleteNovel(novelId: String) {
    viewModelScope.launch {
      repository.deleteNovel(novelId)
      _snackbarMessage.value = "Novel deleted from archive"
    }
  }

  fun updateNovel(
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
    viewModelScope.launch {
      repository.updateNovel(
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
        releaseFormat = releaseFormat,
      )
      _snackbarMessage.value = "Novel '$title' updated successfully"
    }
  }

  fun updateNovelCover(novelId: String, coverImageUri: String) {
    viewModelScope.launch {
      repository.updateNovelCover(novelId, coverImageUri)
      _snackbarMessage.value = "Novel cover updated!"
    }
  }

  fun clearSnackbarMessage() {
    _snackbarMessage.value = null
  }

  fun showSnackbar(message: String) {
    _snackbarMessage.value = message
  }
}
