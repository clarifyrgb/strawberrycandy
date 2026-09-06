package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "novels")
data class NovelEntity(
  @PrimaryKey val id: String,
  val title: String,
  val subtitle: String,
  val author: String = "Strawberrycandy",
  val originalAuthor: String = "", // Original author who wrote the novel (e.g., Alexandre Dumas, Osamu Dazai)
  val authorSlot: Int = 0, // 0 = Strawberrycandy (Owner), 1..10 = 10 Contributor Author Rooms
  val year: String,
  val editionNumber: String,
  val coverDrawableRes: Int = 0,
  val coverImageUri: String? = null,
  val coverColorHex: Long = 0xFF2A2825,
  val chapterTitle: String,
  val totalPages: Int,
  val excerpt: String,
  val contentText: String, // Full text joined with double newline
  val isOwnerUploaded: Boolean = false,
  val createdAt: Long = System.currentTimeMillis(),
  val readsCount: Int = 0,
  val favoritesCount: Int = 0,
  val storyImagesJson: String = "",
  val novelStatus: String = "ONGOING", // "ONGOING" or "FINISHED"
  val releaseFormat: String = "CHAPTER", // "CHAPTER" or "VOLUME"
)
