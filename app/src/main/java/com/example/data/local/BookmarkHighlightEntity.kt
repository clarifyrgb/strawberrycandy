package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chapter_bookmarks")
data class BookmarkHighlightEntity(
  @PrimaryKey val id: String,
  val novelId: String,
  val chapterTitle: String,
  val quoteText: String,
  val paragraphIndex: Int,
  val timestamp: Long = System.currentTimeMillis(),
  val colorHex: Long = 0xFFD4AF37,
  val note: String = "",
)
