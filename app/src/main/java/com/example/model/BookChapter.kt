package com.example.model

data class BookChapter(
  val id: String,
  val index: Int, // 0-based index
  val number: Int, // 1-based number (1, 2, 3...)
  val title: String, // e.g. "Chapter I • The Acoustics of Stillness"
  val subtitle: String = "",
  val startParagraphIndex: Int,
  val endParagraphIndex: Int,
  val startPageIndex: Int = 0,
  val previewSnippet: String = "",
)

data class SearchMatch(
  val globalIndex: Int,
  val paragraphIndex: Int,
  val chapterIndex: Int,
  val chapterTitle: String,
  val startCharIndex: Int,
  val endCharIndex: Int,
  val surroundingSnippet: String,
)
