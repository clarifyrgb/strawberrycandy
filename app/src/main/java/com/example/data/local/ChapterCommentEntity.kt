package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chapter_comments")
data class ChapterCommentEntity(
  @PrimaryKey val id: String,
  val novelId: String,
  val chapterTitle: String,
  val readerName: String,
  val readerEmail: String = "",
  val commentText: String,
  val timestamp: Long = System.currentTimeMillis(),
  val avatarColorHex: Long = 0xFF5C2D3B,
  val likesCount: Int = 0,
  val isLikedByMe: Boolean = false,
)
