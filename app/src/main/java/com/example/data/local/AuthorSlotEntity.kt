package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "author_slots")
data class AuthorSlotEntity(
  @PrimaryKey val slotNumber: Int, // 0 = Strawberrycandy (Owner), 1..10 = Permitted Translators
  val authorName: String,
  val penName: String,
  val bio: String = "Contributing Translator at Strawberrycandy Archive",
  val avatarColorHex: Long = 0xFF5C2D3B,
  val coverImageUri: String? = null,
  val accessCode: String,
  val isClaimed: Boolean = true,
  val isPermissionGranted: Boolean = true,
  val lastActiveTimestamp: Long = System.currentTimeMillis(),
  val translatorEmail: String? = null,
)
