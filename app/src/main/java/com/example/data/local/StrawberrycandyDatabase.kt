package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
  entities = [
    NovelEntity::class,
    ReaderProfileEntity::class,
    UserReadingStateEntity::class,
    AuthorSlotEntity::class,
    ChapterCommentEntity::class,
    BookmarkHighlightEntity::class
  ],
  version = 7,
  exportSchema = false
)
abstract class StrawberrycandyDatabase : RoomDatabase() {
  abstract fun strawberrycandyDao(): StrawberrycandyDao

  companion object {
    @Volatile
    private var INSTANCE: StrawberrycandyDatabase? = null

    fun getInstance(context: Context): StrawberrycandyDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          StrawberrycandyDatabase::class.java,
          "strawberrycandy.db"
        )
          .fallbackToDestructiveMigration()
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
