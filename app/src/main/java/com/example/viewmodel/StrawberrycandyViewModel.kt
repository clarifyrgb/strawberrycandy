package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.StrawberrycandyRepository
import com.example.data.local.AuthorSlotEntity
import com.example.data.local.BookmarkHighlightEntity
import com.example.data.local.ChapterCommentEntity
import com.example.data.local.ReaderProfileEntity
import com.example.data.local.StrawberrycandyDatabase
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
)

class StrawberrycandyViewModel(application: Application) : AndroidViewModel(application) {
  private val database = StrawberrycandyDatabase.getInstance(application)
  private val repository = StrawberrycandyRepository(database.strawberrycandyDao())

  private val _activeFilter = MutableStateFlow(ShelfFilter.ALL)
  private val _activeSort = MutableStateFlow(NovelSortOption.RECENTLY_READ)
  private val _selectedAuthorFilter = MutableStateFlow<Int?>(null)
  private val _novelSearchQuery = MutableStateFlow("")
  val novelSearchQuery: StateFlow<String> = _novelSearchQuery.asStateFlow()

  private val _isAuthDialogOpen = MutableStateFlow(false)
  private val _authErrorMessage = MutableStateFlow<String?>(null)
  private val _isUploadDialogOpen = MutableStateFlow(false)
  private val _isProfileDialogOpen = MutableStateFlow(false)
  private val _snackbarMessage = MutableStateFlow<String?>(null)

  val activeUser: StateFlow<ReaderProfileEntity?> = repository.activeUser
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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
    _authErrorMessage
  ) { isAuth, isUpload, isProfile, msg, authErr ->
    listOf(isAuth, isUpload, isProfile, msg, authErr)
  }

  val uiState: StateFlow<StrawberrycandyUiState> = combine(
    repository.activeUser,
    repository.allNovelsWithState,
    repository.authorSlots,
    _filterState,
    _dialogState
  ) { user, novels, slots, searchParams, dialogList ->
    val isAuthOpen = dialogList[0] as Boolean
    val isUploadOpen = dialogList[1] as Boolean
    val isProfileOpen = dialogList[2] as Boolean
    val msg = dialogList[3] as String?
    val authErr = dialogList[4] as String?

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
      }
    }

    val filteredNovels = searchFiltered
      .filter { novel ->
        when (searchParams.filter) {
          ShelfFilter.ALL -> true
          ShelfFilter.READING -> novel.isReading
          ShelfFilter.FINISHED -> novel.isFinished
          ShelfFilter.TO_BE_READ -> novel.isToBeRead
          ShelfFilter.FAVORITES -> novel.isFavorite
        }
      }
      .filter { novel ->
        if (searchParams.authorSlotFilter == null) true else novel.authorSlot == searchParams.authorSlotFilter
      }

    val sortedNovels = when (searchParams.sort) {
      NovelSortOption.RECENTLY_READ -> filteredNovels.sortedWith(
        compareByDescending<NovelWithState> { it.lastReadTimestamp }
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
    if (user.role != "TRANSLATOR") return false
    val slot = if (user.authorSlot != null) {
      slots.find { it.slotNumber == user.authorSlot }
    } else {
      slots.find {
        it.penName.equals(user.displayName, ignoreCase = true) ||
        it.authorName.equals(user.displayName, ignoreCase = true)
      }
    }
    return slot?.isPermissionGranted == true
  }

  fun canEditNovel(
    user: ReaderProfileEntity? = activeUser.value,
    slots: List<AuthorSlotEntity> = authorSlots.value
  ): Boolean {
    if (user == null) return false
    if (isOwner(user)) return true
    return isPermittedTranslator(user, slots)
  }

  fun isTranslatorOrOwner(user: ReaderProfileEntity? = activeUser.value): Boolean {
    return canEditNovel(user, authorSlots.value)
  }

  fun canUploadNovel(user: ReaderProfileEntity? = activeUser.value, slots: List<AuthorSlotEntity> = authorSlots.value): Boolean {
    if (user == null) return false
    if (isOwner(user)) return true
    return isPermittedTranslator(user, slots)
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

  fun grantPermissionByEmail(email: String, slotNumber: Int? = null) {
    viewModelScope.launch {
      repository.grantPermissionByEmail(email, slotNumber)
      _snackbarMessage.value = "Granted translator permission to $email"
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
    if (user == null) {
      _isAuthDialogOpen.value = true
      _snackbarMessage.value = "Please sign in to record finished novels and earn points"
      return
    }
    viewModelScope.launch {
      val earnedPoint = repository.markNovelAsFinished(user.userId, novelId)
      if (earnedPoint) {
        _snackbarMessage.value = "Novel Finished! +1 Pen Name Point earned! ⭐"
      } else {
        _snackbarMessage.value = "Novel marked as Finished ✓"
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
      _isAuthDialogOpen.value = true
      return
    }
    if (user.penNamePoints < 1 && !isOwner(user)) {
      _snackbarMessage.value = "🔒 Changing your pen name requires 1 point. Finish a novel to earn 1 point!"
      return
    }
    viewModelScope.launch {
      val success = repository.changePenNameWithPoint(user.userId, newPenName)
      if (success) {
        _snackbarMessage.value = "Pen name changed successfully! 1 point used. ⭐"
        _isProfileDialogOpen.value = false
      } else {
        _snackbarMessage.value = "Unable to change pen name. 1 novel completion point required."
      }
    }
  }

  fun updateReaderName(newName: String) {
    val user = activeUser.value
    if (user == null) {
      _isAuthDialogOpen.value = true
      return
    }
    viewModelScope.launch {
      val success = repository.updateReaderDisplayName(user.userId, newName)
      if (success) {
        _snackbarMessage.value = "Reader name updated to '$newName' ✨"
      } else {
        _snackbarMessage.value = "Please enter a valid display name."
      }
    }
  }

  fun updateAuthorSlotWithPoint(slotNumber: Int, authorName: String, penName: String, bio: String) {
    val user = activeUser.value ?: return
    val isSoleOwner = isOwner(user)

    if (isSoleOwner) {
      viewModelScope.launch {
        repository.updateAuthorSlot(slotNumber, authorName, penName, bio)
        _snackbarMessage.value = "Curator profile updated"
      }
      return
    }

    val currentSlot = user.authorSlot
    if (currentSlot == slotNumber) {
      val existingSlot = uiState.value.authorSlots.find { it.slotNumber == slotNumber }
      val isChangingPenName = existingSlot != null && existingSlot.penName != penName.trim()

      if (isChangingPenName) {
        if (user.penNamePoints < 1) {
          _snackbarMessage.value = "🔒 Changing your pen name requires 1 point earned from finishing a novel!"
          return
        }
        viewModelScope.launch {
          val success = repository.changePenNameWithPoint(user.userId, penName)
          if (success) {
            repository.updateAuthorSlot(slotNumber, penName, penName, bio)
            _snackbarMessage.value = "Pen name updated! 1 point deducted. ⭐"
          } else {
            _snackbarMessage.value = "Insufficient points to change pen name."
          }
        }
      } else {
        // Only bio or photo updated
        viewModelScope.launch {
          repository.updateAuthorSlot(slotNumber, penName, penName, bio)
          _snackbarMessage.value = "Profile bio updated"
        }
      }
    }
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
    viewModelScope.launch {
      repository.uploadNovel(
        title = title,
        subtitle = subtitle,
        chapterTitle = chapterTitle,
        excerpt = excerpt,
        content = content,
        coverColorHex = coverColorHex,
        author = author,
        authorSlot = authorSlot,
        coverImageUri = coverImageUri,
        storyImagesJson = storyImagesJson,
        originalAuthor = originalAuthor,
        novelStatus = novelStatus,
        releaseFormat = releaseFormat,
      )
      _isUploadDialogOpen.value = false
      val authorLabel = if (authorSlot == 0) "Strawberrycandy" else "$author (Room $authorSlot)"
      _snackbarMessage.value = "Novel '$title' published by $authorLabel!"
    }
  }

  // Chapter Comments
  fun getCommentsForChapter(novelId: String, chapterTitle: String): Flow<List<ChapterCommentEntity>> {
    return repository.getCommentsForChapter(novelId, chapterTitle)
  }

  fun getCommentCountForChapter(novelId: String, chapterTitle: String): Flow<Int> {
    return repository.getCommentCountForChapter(novelId, chapterTitle)
  }

  fun postComment(novelId: String, chapterTitle: String, text: String, penName: String? = null) {
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
        avatarColorHex = avatarHex
      )
      _snackbarMessage.value = "Thought posted on $chapterTitle"
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

  fun deleteBookmark(bookmarkId: String) {
    viewModelScope.launch {
      repository.deleteBookmark(bookmarkId)
      _snackbarMessage.value = "Bookmark removed"
    }
  }

  fun updateAuthorSlot(slotNumber: Int, authorName: String, penName: String, bio: String) {
    viewModelScope.launch {
      repository.updateAuthorSlot(slotNumber, authorName, penName, bio)
      _snackbarMessage.value = "Translator Profile updated ($penName)"
    }
  }

  fun updateAuthorSlotCover(slotNumber: Int, coverImageUri: String) {
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
}
