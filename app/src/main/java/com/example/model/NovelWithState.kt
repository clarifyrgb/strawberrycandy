package com.example.model

import com.example.data.local.NovelEntity
import com.example.data.local.UserReadingStateEntity

sealed class StoryContentItem {
  data class Text(val paragraph: String) : StoryContentItem()
  data class Photo(val imageUri: String, val caption: String) : StoryContentItem()
}

data class NovelWithState(
  val novel: NovelEntity,
  val userState: UserReadingStateEntity?,
) {
  val id: String get() = novel.id
  val title: String get() = novel.title
  val subtitle: String get() = novel.subtitle
  val author: String get() = novel.author
  val originalAuthor: String get() = novel.originalAuthor
  val authorSlot: Int get() = novel.authorSlot
  val year: String get() = novel.year
  val editionNumber: String get() = novel.editionNumber
  val coverDrawableRes: Int get() = novel.coverDrawableRes
  val coverImageUri: String? get() = novel.coverImageUri
  val coverColorHex: Long get() = novel.coverColorHex
  val chapterTitle: String get() = novel.chapterTitle
  val totalPages: Int get() = novel.totalPages
  val excerpt: String get() = novel.excerpt
  val paragraphs: List<String> get() = novel.contentText.split("\n\n").filter { it.isNotBlank() }

  val storyItems: List<StoryContentItem> get() {
    return paragraphs.map { p ->
      val trimmed = p.trim()
      if (trimmed.startsWith("[image:") && trimmed.endsWith("]")) {
        val inner = trimmed.removePrefix("[image:").removeSuffix("]")
        val parts = inner.split(":", limit = 2)
        val uri = parts.getOrNull(0)?.trim() ?: ""
        val caption = parts.getOrNull(1)?.trim() ?: "Story Illustration"
        StoryContentItem.Photo(imageUri = uri, caption = caption)
      } else if (trimmed.startsWith("[photo:") && trimmed.endsWith("]")) {
        val inner = trimmed.removePrefix("[photo:").removeSuffix("]")
        val parts = inner.split(":", limit = 2)
        val uri = parts.getOrNull(0)?.trim() ?: ""
        val caption = parts.getOrNull(1)?.trim() ?: "Story Illustration"
        StoryContentItem.Photo(imageUri = uri, caption = caption)
      } else {
        StoryContentItem.Text(paragraph = p)
      }
    }
  }

  val storyPhotos: List<StoryContentItem.Photo> get() = storyItems.filterIsInstance<StoryContentItem.Photo>()

  val currentPage: Int get() = userState?.currentPage ?: 1
  val isFavorite: Boolean get() = userState?.isFavorite ?: false
  val inReadingList: Boolean get() = userState?.inReadingList ?: false
  val progressFraction: Float get() = if (totalPages > 0) (currentPage.toFloat() / totalPages.toFloat()).coerceIn(0f, 1f) else 0f
  val lastReadTimestamp: Long get() = userState?.lastReadTimestamp ?: 0L
  val createdAt: Long get() = novel.createdAt
  val readsCount: Int get() = novel.readsCount
  val favoritesCount: Int get() = novel.favoritesCount
  val storyImagesJson: String get() = novel.storyImagesJson
}
