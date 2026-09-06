package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_reading_state")
data class UserReadingStateEntity(
  @PrimaryKey val compositeId: String, // "${userId}_${novelId}"
  val userId: String,
  val novelId: String,
  val currentPage: Int = 1,
  val isFavorite: Boolean = false,
  val inReadingList: Boolean = false,
  val isFinished: Boolean = false,
  val inTbrList: Boolean = false,
  val pointsAwarded: Boolean = false,
  val lastReadTimestamp: Long = System.currentTimeMillis(),
)
