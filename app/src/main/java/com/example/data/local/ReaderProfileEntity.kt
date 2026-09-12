package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reader_profiles")
data class ReaderProfileEntity(
  @PrimaryKey val userId: String,
  val displayName: String,
  val email: String,
  val provider: String, // "GOOGLE" or "APPLE"
  val avatarInitial: String = displayName.take(1).uppercase(),
  val avatarUri: String? = null,
  val role: String = "TRANSLATOR", // "OWNER", "TRANSLATOR", or "READER"
  val authorSlot: Int? = null, // Optional slot number (0 for owner, 1..10 for translator)
  val lastLoginTimestamp: Long = System.currentTimeMillis(),
  val penNamePoints: Int = 0,
  val passwordHash: String? = null,
  val isLoggedIn: Boolean = true,
)
