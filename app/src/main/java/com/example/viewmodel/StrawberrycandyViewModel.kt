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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ShelfFilter {
  ALL,
  MY_LIBRARY,
  FAVORITES
}

data class StrawberrycandyUiState(
  val activeUser: ReaderProfileEntity? = null,
  val novels: List<NovelWithState> = emptyList(),
  val authorSlots: List<AuthorSlotEntity> = emptyList(),
  val activeFilter: ShelfFilter = ShelfFilter.ALL,
  val selectedAuthorFilter: Int? = null, // null = all, 0 = Owner Strawberrycandy, 1..4 = Author Room 1..4
  val isAuthDialogOpen: Boolean = false,
  val isUploadDialogOpen: Boolean = false,
  val message: String? = null,
)

class StrawberrycandyViewModel(application: Application) : AndroidViewModel(application) {
  private val database = StrawberrycandyDatabase.getInstance(application)
  private val repository = StrawberrycandyRepository(database.strawberrycandyDao())

  private val _activeFilter = MutableStateFlow(ShelfFilter.ALL)
  private val _selectedAuthorFilter = MutableStateFlow<Int?>(null)
  private val _isAuthDialogOpen = MutableStateFlow(false)
  private val _isUploadDialogOpen = MutableStateFlow(false)
  private val _snackbarMessage = MutableStateFlow<String?>(null)

  val activeUser: StateFlow<ReaderProfileEntity?> = repository.activeUser
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  val allNovels: StateFlow<List<NovelWithState>> = repository.allNovelsWithState
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val authorSlots: StateFlow<List<AuthorSlotEntity>> = repository.authorSlots
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  private val _filterState = combine(_activeFilter, _selectedAuthorFilter) { filter, authorSlot ->
    Pair(filter, authorSlot)
  }

  private val _dialogState = combine(
    _isAuthDialogOpen,
    _isUploadDialogOpen,
    _snackbarMessage
  ) { isAuth, isUpload, msg ->
    Triple(isAuth, isUpload, msg)
  }

  val uiState: StateFlow<StrawberrycandyUiState> = combine(
    repository.activeUser,
    repository.allNovelsWithState,
    repository.authorSlots,
    _filterState,
    _dialogState
  ) { user, novels, slots, (filter, authorSlotFilter), (isAuthOpen, isUploadOpen, msg) ->
    val filteredNovels = novels
      .filter { novel ->
        when (filter) {
          ShelfFilter.ALL -> true
          ShelfFilter.MY_LIBRARY -> novel.inReadingList
          ShelfFilter.FAVORITES -> novel.isFavorite
        }
      }
      .filter { novel ->
        if (authorSlotFilter == null) true else novel.authorSlot == authorSlotFilter
      }

    StrawberrycandyUiState(
      activeUser = user,
      novels = filteredNovels,
      authorSlots = slots,
      activeFilter = filter,
      selectedAuthorFilter = authorSlotFilter,
      isAuthDialogOpen = isAuthOpen,
      isUploadDialogOpen = isUploadOpen,
      message = msg
    )
  }.stateIn(
    viewModelScope,
    SharingStarted.WhileSubscribed(5000),
    StrawberrycandyUiState()
  )

  fun setFilter(filter: ShelfFilter) {
    _activeFilter.value = filter
  }

  fun setAuthorFilter(slot: Int?) {
    _selectedAuthorFilter.value = slot
  }

  fun openAuthDialog() {
    _isAuthDialogOpen.value = true
  }

  fun closeAuthDialog() {
    _isAuthDialogOpen.value = false
  }

  fun openUploadDialog() {
    _isUploadDialogOpen.value = true
  }

  fun closeUploadDialog() {
    _isUploadDialogOpen.value = false
  }

  fun isTranslatorOrOwner(user: ReaderProfileEntity? = activeUser.value): Boolean {
    if (user == null) return false
    return user.role == "OWNER" || user.role == "TRANSLATOR"
  }

  fun canUploadNovel(user: ReaderProfileEntity? = activeUser.value, slots: List<AuthorSlotEntity> = authorSlots.value): Boolean {
    if (user == null) return false
    if (user.role == "OWNER") return true
    if (user.role == "TRANSLATOR") {
      // Find matching slot or check if user's slot is permitted
      val slot = if (user.authorSlot != null) {
        slots.find { it.slotNumber == user.authorSlot }
      } else {
        slots.find {
          it.penName.equals(user.displayName, ignoreCase = true) ||
          it.authorName.equals(user.displayName, ignoreCase = true)
        } ?: slots.filter { it.slotNumber > 0 }.firstOrNull { it.isPermissionGranted }
      }
      return slot?.isPermissionGranted ?: false
    }
    return false
  }

  fun signInWithGoogle(
    email: String,
    displayName: String = "",
    role: String = "TRANSLATOR",
    authorSlot: Int? = null,
  ) {
    viewModelScope.launch {
      repository.signIn(provider = "GOOGLE", email = email, displayName = displayName, role = role, authorSlot = authorSlot)
      _isAuthDialogOpen.value = false
      val roleLabel = if (role == "OWNER") "Archive Owner" else if (role == "TRANSLATOR") "Translator" else "Reader"
      _snackbarMessage.value = "Signed in as $roleLabel"
    }
  }

  fun signInWithApple(
    email: String,
    displayName: String = "",
    role: String = "TRANSLATOR",
    authorSlot: Int? = null,
  ) {
    viewModelScope.launch {
      repository.signIn(provider = "APPLE", email = email, displayName = displayName, role = role, authorSlot = authorSlot)
      _isAuthDialogOpen.value = false
      val roleLabel = if (role == "OWNER") "Archive Owner" else if (role == "TRANSLATOR") "Translator" else "Reader"
      _snackbarMessage.value = "Signed in as $roleLabel"
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
      repository.saveReadingProgress(user.userId, novelId, page)
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
