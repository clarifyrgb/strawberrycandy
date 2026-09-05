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
  val lastLoginTimestamp: Long = System.currentTimeMillis(),
)
