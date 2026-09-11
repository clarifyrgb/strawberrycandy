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
  val contentText: String get() = novel.contentText
  val totalPages: Int get() = novel.totalPages
  val excerpt: String get() = novel.excerpt
  val genre: String get() = novel.genre
  val synopsis: String get() = novel.subtitle
  val paragraphs: List<String> get() = novel.contentText.split("\n\n").filter {
    it.isNotBlank() && !it.contains("Monastic Stone Arcades") && !it.contains("Deep Limestone Splay")
  }

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
    if (items.isEmpty()) return emptyList()

    val breakIndices = items.mapIndexedNotNull { index, item ->
      if (item is StoryContentItem.ChapterBreak) Pair(index, item) else null
    }

    if (breakIndices.isNotEmpty()) {
      val chapterList = mutableListOf<BookChapter>()
      val firstBreakIndex = breakIndices.first().first

      // If there are paragraphs preceding the first explicit chapter break
      if (firstBreakIndex > 0) {
        chapterList.add(
          BookChapter(
            id = "${id}_ch_0",
            index = 0,
            number = 1,
            title = chapterTitle.ifBlank { "Prologue • Opening Folio" },
            startParagraphIndex = 0,
            endParagraphIndex = firstBreakIndex - 1,
            previewSnippet = getSnippetForRange(items, 0, firstBreakIndex - 1)
          )
        )
      }

      breakIndices.forEachIndexed { i, (bIndex, bItem) ->
        val nextBreakIndex = breakIndices.getOrNull(i + 1)?.first ?: items.size
        val chIdx = chapterList.size
        val chNum = if (firstBreakIndex > 0) chIdx + 1 else bItem.chapterNumber
        chapterList.add(
          BookChapter(
            id = "${id}_ch_${chIdx}_${bItem.chapterNumber}",
            index = chIdx,
            number = chNum,
            title = bItem.title,
            startParagraphIndex = bIndex,
            endParagraphIndex = (nextBreakIndex - 1).coerceAtLeast(bIndex),
            previewSnippet = getSnippetForRange(items, bIndex, nextBreakIndex - 1)
          )
        )
      }
      return chapterList
    }

    // If only 1 chapter was detected and there are multiple paragraphs, create logical chapters
    if (items.size >= 4) {
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

    return listOf(
      BookChapter(
        id = "${id}_ch_1",
        index = 0,
        number = 1,
        title = chapterTitle.ifBlank { "Chapter I • Opening Folio" },
        startParagraphIndex = 0,
        endParagraphIndex = (items.size - 1).coerceAtLeast(0),
        previewSnippet = getSnippetForRange(items, 0, items.size - 1)
      )
    )
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
  val isFinished: Boolean get() = userState?.isFinished == true || (totalPages > 0 && currentPage >= totalPages)
  val inTbrList: Boolean get() = userState?.inTbrList == true
  val isReading: Boolean get() = !isFinished && (currentPage > 1 || (inReadingList && !inTbrList))
  val isToBeRead: Boolean get() = !isFinished && (inTbrList || (inReadingList && currentPage <= 1))
  val pointsAwarded: Boolean get() = userState?.pointsAwarded == true
  val progressFraction: Float get() = if (totalPages > 0) (currentPage.toFloat() / totalPages.toFloat()).coerceIn(0f, 1f) else 0f
  val lastReadTimestamp: Long get() = userState?.lastReadTimestamp ?: 0L
  val createdAt: Long get() = novel.createdAt
  val readsCount: Int get() = novel.readsCount
  val favoritesCount: Int get() = novel.favoritesCount
  val storyImagesJson: String get() = novel.storyImagesJson
  val novelStatus: String get() = novel.novelStatus
  val releaseFormat: String get() = novel.releaseFormat
  val isR19: Boolean get() = novel.isR19

  val isCompletedNovel: Boolean get() = novel.novelStatus.equals("FINISHED", ignoreCase = true)
  val novelStatusLabel: String get() = if (isCompletedNovel) "Finished" else "Ongoing"

  val authorLabel: String get() {
    return if (originalAuthor.isNotBlank() && !originalAuthor.equals(author, ignoreCase = true)) {
      "$originalAuthor (Trans. $author)"
    } else {
      author
    }
  }

  val isPerVolume: Boolean get() {
    if (novel.releaseFormat.equals("VOLUME", ignoreCase = true)) return true
    if (novel.releaseFormat.equals("CHAPTER", ignoreCase = true)) return false
    val combined = "${novel.title} ${novel.subtitle} ${novel.chapterTitle}".lowercase()
    return combined.contains("volume") || combined.contains("vol.") || combined.contains("vol ")
  }

  val isNewRelease: Boolean get() {
    val diff = System.currentTimeMillis() - novel.createdAt
    return diff in 0..(7L * 24 * 3600 * 1000L) // Active new release for 7 days
  }

  val readButtonLabel: String get() {
    return if (inReadingList && progressFraction > 0f) {
      "Continue (Pg. $currentPage)"
    } else if (isPerVolume) {
      "Read Volume"
    } else {
      "Read Chapters"
    }
  }
}

