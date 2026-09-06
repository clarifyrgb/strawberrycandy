package com.example.model

import com.example.data.local.NovelEntity
import com.example.data.local.UserReadingStateEntity

sealed class StoryContentItem {
  data class Text(val paragraph: String) : StoryContentItem()
  data class Photo(val imageUri: String, val caption: String) : StoryContentItem()
  data class ChapterBreak(val chapterNumber: Int, val title: String, val subtitle: String = "") : StoryContentItem()
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
    var detectedChapterCount = 1
    return paragraphs.map { p ->
      val trimmed = p.trim()
      if (trimmed.startsWith("[chapter:") && trimmed.endsWith("]")) {
        val title = trimmed.removePrefix("[chapter:").removeSuffix("]").trim()
        val num = detectedChapterCount++
        StoryContentItem.ChapterBreak(chapterNumber = num, title = title)
      } else if (trimmed.startsWith("### ") || trimmed.startsWith("## ") || trimmed.startsWith("# ")) {
        val title = trimmed.replace(Regex("""^#+\s*"""), "").trim()
        val num = detectedChapterCount++
        StoryContentItem.ChapterBreak(chapterNumber = num, title = title)
      } else if (trimmed.startsWith("[image:") && trimmed.endsWith("]")) {
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

  val chapters: List<BookChapter> get() {
    val items = storyItems
    val chapterList = mutableListOf<BookChapter>()
    var currentChapterStart = 0
    var currentTitle = chapterTitle.ifBlank { "Chapter I • Opening Folio" }
    var chapterCounter = 1

    items.forEachIndexed { index, item ->
      if (item is StoryContentItem.ChapterBreak) {
        if (index > currentChapterStart) {
          chapterList.add(
            BookChapter(
              id = "${id}_ch_${chapterCounter}",
              index = chapterCounter - 1,
              number = chapterCounter,
              title = currentTitle,
              startParagraphIndex = currentChapterStart,
              endParagraphIndex = index - 1,
              previewSnippet = getSnippetForRange(items, currentChapterStart, index - 1)
            )
          )
          chapterCounter++
        }
        currentChapterStart = index
        currentTitle = item.title
      }
    }

    if (currentChapterStart < items.size) {
      chapterList.add(
        BookChapter(
          id = "${id}_ch_${chapterCounter}",
          index = chapterCounter - 1,
          number = chapterCounter,
          title = currentTitle,
          startParagraphIndex = currentChapterStart,
          endParagraphIndex = (items.size - 1).coerceAtLeast(currentChapterStart),
          previewSnippet = getSnippetForRange(items, currentChapterStart, items.size - 1)
        )
      )
    }

    // If only 1 chapter was detected and there are multiple paragraphs, create logical chapters
    if (chapterList.size <= 1 && items.size >= 4) {
      val total = items.size
      val part1End = (total / 3).coerceAtLeast(1)
      val part2End = (2 * total / 3).coerceAtLeast(part1End + 1)
      val baseTitle = chapterTitle.ifBlank { "The Monograph" }
      val prefix = if (baseTitle.contains("•")) baseTitle.substringBefore("•").trim() else "Chapter I"
      val rest = if (baseTitle.contains("•")) baseTitle.substringAfter("•").trim() else baseTitle

      return listOf(
        BookChapter(
          id = "${id}_ch_1",
          index = 0,
          number = 1,
          title = if (baseTitle.contains("Chapter", ignoreCase = true)) baseTitle else "Chapter I • $rest",
          startParagraphIndex = 0,
          endParagraphIndex = part1End - 1,
          previewSnippet = getSnippetForRange(items, 0, part1End - 1)
        ),
        BookChapter(
          id = "${id}_ch_2",
          index = 1,
          number = 2,
          title = "Chapter II • The Resonance of Stone & Shadow",
          startParagraphIndex = part1End,
          endParagraphIndex = part2End - 1,
          previewSnippet = getSnippetForRange(items, part1End, part2End - 1)
        ),
        BookChapter(
          id = "${id}_ch_3",
          index = 2,
          number = 3,
          title = "Chapter III • The Tactile Covenant of Reading",
          startParagraphIndex = part2End,
          endParagraphIndex = total - 1,
          previewSnippet = getSnippetForRange(items, part2End, total - 1)
        )
      )
    }

    return chapterList
  }

  private fun getSnippetForRange(items: List<StoryContentItem>, start: Int, end: Int): String {
    for (i in start..end.coerceAtMost(items.size - 1)) {
      val item = items[i]
      if (item is StoryContentItem.Text && item.paragraph.isNotBlank()) {
        val clean = item.paragraph.trim()
        return clean.take(140) + if (clean.length > 140) "…" else ""
      }
    }
    return "Chapter folio and editorial illustrations"
  }

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

