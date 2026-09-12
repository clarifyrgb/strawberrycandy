package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StrawberrycandyDao {
  // Novels
  @Query("SELECT * FROM novels ORDER BY createdAt DESC")
  fun getAllNovels(): Flow<List<NovelEntity>>

  @Query("DELETE FROM novels WHERE id IN ('nov_1', 'nov_2', 'nov_3', 'nov_cloud_1', 'nov_cloud_2', 'nov_schema', 'nov_crimson', 'nov_celestial', 'nov_whispering_pines', 'nov_moonlight') OR LOWER(title) IN ('the starlight chronicles', 'dawn on the canal', 'the count of monte cristo', 'no longer human') OR (isOwnerUploaded = 0 AND author = 'Strawberrycandy')")
  suspend fun deleteSampleNovels()

  @Query("DELETE FROM user_reading_state WHERE novelId IN ('nov_1', 'nov_2', 'nov_3', 'nov_cloud_1', 'nov_cloud_2', 'nov_schema', 'nov_crimson', 'nov_celestial', 'nov_whispering_pines', 'nov_moonlight')")
  suspend fun deleteSampleReadingStates()

  @Query("DELETE FROM chapter_comments WHERE novelId IN ('nov_1', 'nov_2', 'nov_3', 'nov_cloud_1', 'nov_cloud_2', 'nov_schema', 'nov_crimson', 'nov_celestial', 'nov_whispering_pines', 'nov_moonlight') OR id LIKE 'cmt_init_%'")
  suspend fun deleteSampleComments()

  @Query("DELETE FROM chapter_bookmarks WHERE novelId IN ('nov_1', 'nov_2', 'nov_3', 'nov_cloud_1', 'nov_cloud_2', 'nov_schema', 'nov_crimson', 'nov_celestial', 'nov_whispering_pines', 'nov_moonlight')")
  suspend fun deleteSampleBookmarks()

  @Query("SELECT * FROM novels WHERE id = :id LIMIT 1")
  suspend fun getNovelById(id: String): NovelEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertNovel(novel: NovelEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertNovels(novels: List<NovelEntity>)

  @Query("SELECT * FROM novels ORDER BY createdAt DESC")
  suspend fun getAllNovelsSync(): List<NovelEntity>

  @Query("DELETE FROM novels WHERE id = :id")
  suspend fun deleteNovelById(id: String)

  @Query("UPDATE novels SET coverImageUri = :coverImageUri WHERE id = :novelId")
  suspend fun updateNovelCover(novelId: String, coverImageUri: String)

  @Query("UPDATE novels SET readsCount = readsCount + 1 WHERE id = :novelId")
  suspend fun incrementReadsCount(novelId: String)

  @Query("UPDATE novels SET favoritesCount = (SELECT COUNT(*) FROM user_reading_state WHERE user_reading_state.novelId = novels.id AND user_reading_state.isFavorite = 1) WHERE id = :novelId")
  suspend fun syncRealFavoritesForNovel(novelId: String)

  @Query("UPDATE novels SET favoritesCount = (SELECT COUNT(*) FROM user_reading_state WHERE user_reading_state.novelId = novels.id AND user_reading_state.isFavorite = 1)")
  suspend fun syncAllRealFavorites()

  @Query("UPDATE novels SET readsCount = 0 WHERE readsCount >= 500")
  suspend fun resetArtificialReads()

  @Query("DELETE FROM user_reading_state WHERE compositeId = 'guest_reader_nov_1' AND currentPage = 48")
  suspend fun removeFakeSeededReadingState()

  @Query("UPDATE author_slots SET bio = 'Contributing Translator at Strawberrycandy Archive' WHERE bio LIKE '%@%'")
  suspend fun sanitizeSlotBios()

  @Query("UPDATE author_slots SET penName = '' WHERE slotNumber > 0 AND (penName LIKE '%@%' OR penName LIKE 'Translator %' OR penName LIKE 'Author %')")
  suspend fun sanitizeSlotPenNames()

  @Query("UPDATE author_slots SET authorName = '' WHERE slotNumber > 0 AND (authorName LIKE '%@%' OR authorName LIKE 'Translator %' OR authorName LIKE 'Author %')")
  suspend fun sanitizeSlotAuthorNames()

  @Query("UPDATE chapter_comments SET readerName = substr(readerName, 1, instr(readerName, '@') - 1) WHERE readerName LIKE '%@%'")
  suspend fun sanitizeCommentNames()

  @Query("UPDATE novels SET favoritesCount = CASE WHEN :increment = 1 THEN favoritesCount + 1 ELSE CASE WHEN favoritesCount > 0 THEN favoritesCount - 1 ELSE 0 END END WHERE id = :novelId")
  suspend fun updateFavoritesCount(novelId: String, increment: Int)

  @Query("SELECT COUNT(*) FROM novels")
  suspend fun getNovelCount(): Int

  // Reader Profiles (Google / Apple)
  @Query("SELECT * FROM reader_profiles WHERE isLoggedIn = 1 ORDER BY lastLoginTimestamp DESC LIMIT 1")
  fun getActiveReaderProfile(): Flow<ReaderProfileEntity?>

  @Query("SELECT * FROM reader_profiles WHERE isLoggedIn = 1 ORDER BY lastLoginTimestamp DESC LIMIT 1")
  suspend fun getActiveReaderProfileSync(): ReaderProfileEntity?

  @Query("SELECT * FROM reader_profiles ORDER BY lastLoginTimestamp DESC")
  fun getAllRememberedAccounts(): Flow<List<ReaderProfileEntity>>

  @Query("SELECT * FROM reader_profiles ORDER BY lastLoginTimestamp DESC")
  suspend fun getAllProfilesList(): List<ReaderProfileEntity>

  @Query("SELECT * FROM reader_profiles WHERE userId = :userId LIMIT 1")
  suspend fun getReaderProfile(userId: String): ReaderProfileEntity?

  @Query("SELECT * FROM reader_profiles WHERE LOWER(email) = LOWER(:email) LIMIT 1")
  suspend fun getReaderProfileByEmail(email: String): ReaderProfileEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertReaderProfile(profile: ReaderProfileEntity)

  @Query("UPDATE reader_profiles SET isLoggedIn = 0")
  suspend fun logoutAllProfiles()

  @Query("UPDATE reader_profiles SET passwordHash = :newPassword WHERE LOWER(email) = LOWER(:email)")
  suspend fun updatePasswordByEmail(email: String, newPassword: String): Int

  @Query("DELETE FROM reader_profiles WHERE userId = :userId")
  suspend fun deleteProfile(userId: String)

  @Query("DELETE FROM reader_profiles")
  suspend fun clearReaderProfiles()

  @Query("UPDATE reader_profiles SET penNamePoints = penNamePoints + :delta WHERE userId = :userId")
  suspend fun addPointsToReader(userId: String, delta: Int)

  @Query("UPDATE reader_profiles SET role = 'TRANSLATOR', authorSlot = :slotNumber WHERE userId = :userId")
  suspend fun upgradeProfileToTranslator(userId: String, slotNumber: Int)

  @Query("UPDATE reader_profiles SET role = 'TRANSLATOR', authorSlot = :slotNumber WHERE LOWER(email) = LOWER(:email)")
  suspend fun upgradeProfileToTranslatorByEmail(email: String, slotNumber: Int)

  @Query("UPDATE reader_profiles SET displayName = :newDisplayName, penNamePoints = penNamePoints - 1 WHERE userId = :userId AND penNamePoints >= 1")
  suspend fun deductPointAndSetDisplayName(userId: String, newDisplayName: String): Int

  @Query("UPDATE reader_profiles SET displayName = :newDisplayName WHERE userId = :userId")
  suspend fun updateReaderDisplayName(userId: String, newDisplayName: String): Int

  @Query("UPDATE author_slots SET penName = :penName WHERE slotNumber = :slotNumber")
  suspend fun updateSlotPenName(slotNumber: Int, penName: String)

  // Reading States (Favorites, Reading Novels, Resume Position)
  @Query("SELECT * FROM user_reading_state WHERE userId = :userId")
  fun getUserReadingStates(userId: String): Flow<List<UserReadingStateEntity>>

  @Query("SELECT * FROM user_reading_state WHERE userId = :userId AND novelId = :novelId LIMIT 1")
  suspend fun getReadingState(userId: String, novelId: String): UserReadingStateEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdateReadingState(state: UserReadingStateEntity)

  @Query("DELETE FROM user_reading_state WHERE userId = :userId AND novelId = :novelId")
  suspend fun deleteReadingState(userId: String, novelId: String)

  @Query("DELETE FROM user_reading_state WHERE novelId = :novelId")
  suspend fun deleteAllReadingStatesForNovel(novelId: String)

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

  @Query("UPDATE author_slots SET translatorEmail = :email WHERE slotNumber = :slotNumber")
  suspend fun updateAuthorSlotEmail(slotNumber: Int, email: String)

  @Query("UPDATE author_slots SET isPermissionGranted = :isGranted, translatorEmail = :email, isClaimed = 1 WHERE slotNumber = :slotNumber")
  suspend fun updateAuthorSlotPermissionAndEmail(slotNumber: Int, isGranted: Boolean, email: String)

  @Query("SELECT * FROM author_slots WHERE LOWER(translatorEmail) = LOWER(:email) LIMIT 1")
  suspend fun getAuthorSlotByEmail(email: String): AuthorSlotEntity?

  @Query("SELECT COUNT(*) FROM author_slots")
  suspend fun getAuthorSlotCount(): Int

  @Query("SELECT * FROM author_slots ORDER BY slotNumber ASC")
  suspend fun getAllAuthorSlotsSync(): List<AuthorSlotEntity>

  // Chapter Comments per Chapter
  @Query("SELECT * FROM chapter_comments WHERE novelId = :novelId AND chapterTitle = :chapterTitle ORDER BY timestamp DESC")
  fun getCommentsForChapter(novelId: String, chapterTitle: String): Flow<List<ChapterCommentEntity>>

  @Query("SELECT * FROM chapter_comments WHERE novelId = :novelId ORDER BY timestamp DESC")
  fun getAllCommentsForNovel(novelId: String): Flow<List<ChapterCommentEntity>>

  @Query("SELECT * FROM chapter_comments WHERE novelId = :novelId ORDER BY timestamp DESC")
  suspend fun getAllCommentsForNovelSync(novelId: String): List<ChapterCommentEntity>

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

  @Query("SELECT * FROM chapter_comments WHERE (readerEmail != '' AND LOWER(readerEmail) = LOWER(:email)) OR LOWER(readerName) = LOWER(:name) ORDER BY timestamp DESC")
  fun getCommentsForReader(email: String, name: String): Flow<List<ChapterCommentEntity>>

  @Query("DELETE FROM chapter_comments WHERE id = :commentId OR parentCommentId = :commentId")
  suspend fun deleteComment(commentId: String)

  @Query("DELETE FROM chapter_comments WHERE novelId = :novelId")
  suspend fun deleteCommentsForNovel(novelId: String)

  @Query("DELETE FROM chapter_comments")
  suspend fun clearAllComments()

  // Favorite Lines & Bookmarks per Novel
  @Query("SELECT * FROM chapter_bookmarks WHERE novelId = :novelId ORDER BY timestamp DESC")
  fun getBookmarksForNovel(novelId: String): Flow<List<BookmarkHighlightEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertBookmark(bookmark: BookmarkHighlightEntity)

  @Query("DELETE FROM chapter_bookmarks WHERE id = :id")
  suspend fun deleteBookmark(id: String)

  @Query("DELETE FROM chapter_bookmarks WHERE novelId = :novelId")
  suspend fun deleteBookmarksForNovel(novelId: String)

  @Query("DELETE FROM chapter_bookmarks WHERE novelId = :novelId AND quoteText = :quoteText")
  suspend fun deleteBookmarkByQuote(novelId: String, quoteText: String)

  @Query("SELECT * FROM chapter_bookmarks WHERE novelId = :novelId AND quoteText = :quoteText LIMIT 1")
  suspend fun findBookmarkByQuote(novelId: String, quoteText: String): BookmarkHighlightEntity?
}
