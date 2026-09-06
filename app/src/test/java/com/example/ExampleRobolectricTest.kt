package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.StrawberrycandyRepository
import com.example.data.local.NovelEntity
import com.example.data.local.ReaderProfileEntity
import com.example.data.local.StrawberrycandyDatabase
import com.example.data.local.UserReadingStateEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Strawberrycandy", appName)
  }

  @Test
  fun `activity launches successfully`() {
    val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
    assertNotNull(controller.get())
  }

  @Test
  fun `owner can upload novel under author Strawberrycandy and reader can save favorites and progress`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = StrawberrycandyDatabase.getInstance(context)
    val dao = db.strawberrycandyDao()
    val repo = StrawberrycandyRepository(dao)

    // 1. Verify Owner Novel Upload under "Strawberrycandy"
    val uploadedId = repo.uploadNovel(
      title = "The Whispering Pines",
      subtitle = "A Northern Solitude",
      chapterTitle = "Chapter I • Needle & Frost",
      excerpt = "The forest gave no quarter to the restless.",
      content = "The forest gave no quarter to the restless. Snow drifted across the granite outcrop in quiet waves.",
      coverColorHex = 0xFF28362D
    )

    val novel = dao.getNovelById(uploadedId)
    assertNotNull(novel)
    assertEquals("Strawberrycandy", novel?.author)
    assertEquals("The Whispering Pines", novel?.title)

    // 2. Verify Reader Sign-In with Google and matching password check
    val initialSignInResult = repo.signIn(
      provider = "GOOGLE",
      email = "reader.alex@gmail.com",
      password = "GooglePassword123!",
      displayName = "Alex Vance"
    )
    assertTrue(initialSignInResult.isSuccess)
    val googleUser = repo.activeUser.first()
    assertNotNull(googleUser)
    assertEquals("GOOGLE", googleUser?.provider)
    assertEquals("reader.alex@gmail.com", googleUser?.email)

    // Verify rejection if mismatched password is used
    val wrongPassResult = repo.signIn(
      provider = "GOOGLE",
      email = "reader.alex@gmail.com",
      password = "WrongPassword999!",
      displayName = "Alex Vance"
    )
    assertTrue(wrongPassResult.isFailure)

    // 3. Verify Reader adding Favorite and Storing Reading Novel
    repo.toggleFavorite(googleUser!!.userId, uploadedId)
    val state = dao.getReadingState(googleUser.userId, uploadedId)
    assertNotNull(state)
    assertTrue(state!!.isFavorite)
    assertTrue(state.inReadingList)

    // 4. Verify Continuing where reader stopped reading (Page Progress Sync)
    repo.saveReadingProgress(googleUser.userId, uploadedId, page = 42)
    val updatedState = dao.getReadingState(googleUser.userId, uploadedId)
    assertEquals(42, updatedState?.currentPage)

    // 5. Verify Sign in with Apple
    val appleResult = repo.signIn(
      provider = "APPLE",
      email = "reader.user@privaterelay.appleid.com",
      password = "ApplePassword123!",
      displayName = "Apple Reader"
    )
    assertTrue(appleResult.isSuccess)
    val appleUser = repo.activeUser.first()
    assertNotNull(appleUser)
    assertEquals("APPLE", appleUser?.provider)
  }
}
