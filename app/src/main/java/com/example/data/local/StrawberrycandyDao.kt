package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StrawberrycandyDao {
  // Novels
  @Query("SELECT * FROM novels ORDER BY createdAt ASC")
  fun getAllNovels(): Flow<List<NovelEntity>>

  @Query("SELECT * FROM novels WHERE id = :id LIMIT 1")
  suspend fun getNovelById(id: String): NovelEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertNovel(novel: NovelEntity)

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertNovels(novels: List<NovelEntity>)

  @Query("DELETE FROM novels WHERE id = :id")
  suspend fun deleteNovelById(id: String)

  @Query("UPDATE novels SET coverImageUri = :coverImageUri WHERE id = :novelId")
  suspend fun updateNovelCover(novelId: String, coverImageUri: String)

  @Query("UPDATE novels SET readsCount = readsCount + 1 WHERE id = :novelId")
  suspend fun incrementReadsCount(novelId: String)

  @Query("UPDATE novels SET favoritesCount = CASE WHEN :increment = 1 THEN favoritesCount + 1 ELSE CASE WHEN favoritesCount > 0 THEN favoritesCount - 1 ELSE 0 END END WHERE id = :novelId")
  suspend fun updateFavoritesCount(novelId: String, increment: Int)

  @Query("SELECT COUNT(*) FROM novels")
  suspend fun getNovelCount(): Int

  // Reader Profiles (Google / Apple)
  @Query("SELECT * FROM reader_profiles ORDER BY lastLoginTimestamp DESC LIMIT 1")
  fun getActiveReaderProfile(): Flow<ReaderProfileEntity?>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertReaderProfile(profile: ReaderProfileEntity)

  @Query("DELETE FROM reader_profiles")
  suspend fun clearReaderProfiles()

  // Reading States (Favorites, Reading Novels, Resume Position)
  @Query("SELECT * FROM user_reading_state WHERE userId = :userId")
  fun getUserReadingStates(userId: String): Flow<List<UserReadingStateEntity>>

  @Query("SELECT * FROM user_reading_state WHERE userId = :userId AND novelId = :novelId LIMIT 1")
  suspend fun getReadingState(userId: String, novelId: String): UserReadingStateEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdateReadingState(state: UserReadingStateEntity)

  // 4-Author Rooms & Access Slots
  @Query("SELECT * FROM author_slots ORDER BY slotNumber ASC")
  fun getAllAuthorSlots(): Flow<List<AuthorSlotEntity>>

  @Query("SELECT * FROM author_slots WHERE slotNumber = :slotNumber LIMIT 1")
  suspend fun getAuthorSlot(slotNumber: Int): AuthorSlotEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAuthorSlots(slots: List<AuthorSlotEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun updateAuthorSlot(slot: AuthorSlotEntity)

  @Query("UPDATE author_slots SET coverImageUri = :coverImageUri WHERE slotNumber = :slotNumber")
  suspend fun updateAuthorSlotCover(slotNumber: Int, coverImageUri: String?)

  @Query("UPDATE author_slots SET isPermissionGranted = :isGranted WHERE slotNumber = :slotNumber")
  suspend fun updateAuthorSlotPermission(slotNumber: Int, isGranted: Boolean)

  @Query("SELECT COUNT(*) FROM author_slots")
  suspend fun getAuthorSlotCount(): Int

  // Chapter Comments per Chapter
  @Query("SELECT * FROM chapter_comments WHERE novelId = :novelId AND chapterTitle = :chapterTitle ORDER BY timestamp DESC")
  fun getCommentsForChapter(novelId: String, chapterTitle: String): Flow<List<ChapterCommentEntity>>

  @Query("SELECT COUNT(*) FROM chapter_comments WHERE novelId = :novelId AND chapterTitle = :chapterTitle")
  fun getCommentCountForChapter(novelId: String, chapterTitle: String): Flow<Int>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertComment(comment: ChapterCommentEntity)

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertComments(comments: List<ChapterCommentEntity>)

  @Query("UPDATE chapter_comments SET likesCount = likesCount + 1, isLikedByMe = 1 WHERE id = :commentId")
  suspend fun likeComment(commentId: String)

  @Query("SELECT COUNT(*) FROM chapter_comments")
  suspend fun getCommentCount(): Int

  // Favorite Lines & Bookmarks per Novel
  @Query("SELECT * FROM chapter_bookmarks WHERE novelId = :novelId ORDER BY timestamp DESC")
  fun getBookmarksForNovel(novelId: String): Flow<List<BookmarkHighlightEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertBookmark(bookmark: BookmarkHighlightEntity)

  @Query("DELETE FROM chapter_bookmarks WHERE id = :id")
  suspend fun deleteBookmark(id: String)

  @Query("DELETE FROM chapter_bookmarks WHERE novelId = :novelId AND quoteText = :quoteText")
  suspend fun deleteBookmarkByQuote(novelId: String, quoteText: String)

  @Query("SELECT * FROM chapter_bookmarks WHERE novelId = :novelId AND quoteText = :quoteText LIMIT 1")
  suspend fun findBookmarkByQuote(novelId: String, quoteText: String): BookmarkHighlightEntity?
}
