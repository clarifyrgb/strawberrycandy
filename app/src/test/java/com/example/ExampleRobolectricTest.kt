package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.viewmodel.StrawberrycandyViewModel
import com.example.data.StrawberrycandyRepository
import com.example.data.local.NovelEntity
import com.example.data.local.ReaderProfileEntity
import com.example.data.local.StrawberrycandyDatabase
import com.example.data.local.UserReadingStateEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Before
  fun setUp() = runBlocking(Dispatchers.IO) {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = StrawberrycandyDatabase.getInstance(context)
    db.clearAllTables()
  }

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
    val uploadResult = repo.uploadNovel(
      title = "The Whispering Pines",
      subtitle = "A Northern Solitude",
      chapterTitle = "Chapter I • Needle & Frost",
      excerpt = "The forest gave no quarter to the restless.",
      content = "The forest gave no quarter to the restless. Snow drifted across the granite outcrop in quiet waves.",
      coverColorHex = 0xFF28362D
    )
    val uploadedId = uploadResult.novelId

    val novel = dao.getNovelById(uploadedId)
    assertNotNull(novel)
    assertEquals("Strawberrycandy", novel?.author)
    assertEquals("The Whispering Pines", novel?.title)

    // 2. Verify Reader Sign-In with Google and matching password check (letters-only password)
    val initialSignInResult = repo.signIn(
      provider = "GOOGLE",
      email = "reader.alex@gmail.com",
      password = "mypasswordonlyletters",
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
      password = "differentpassword",
      displayName = "Alex Vance"
    )
    assertTrue(wrongPassResult.isFailure)

    // Verify Archive Owner (clarifymanga@gmail.com) signs in with letters-only password
    val ownerSignInResult = repo.signIn(
      provider = "GOOGLE",
      email = "clarifymanga@gmail.com",
      password = "ownerpasswordwithoutnumbers",
      displayName = "Clarify"
    )
    assertTrue(ownerSignInResult.isSuccess)
    val ownerUser = repo.activeUser.first()
    assertNotNull(ownerUser)
    assertEquals("OWNER", ownerUser?.role)
    assertEquals("clarifymanga@gmail.com", ownerUser?.email)

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

  @Test
  fun `translators and readers easily log in with same password and gmail`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = StrawberrycandyDatabase.getInstance(context)
    val dao = db.strawberrycandyDao()
    val repo = StrawberrycandyRepository(dao)

    // Reader logs in with simple letters-only password
    val readerResult = repo.signIn(
      provider = "GOOGLE",
      email = "testreader@gmail.com",
      password = "simplepassword",
      displayName = "Reader Test"
    )
    assertTrue(readerResult.isSuccess)
    val readerUser = repo.activeUser.first()
    assertNotNull(readerUser)
    assertEquals("READER", readerUser?.role)

    // Translator logs in with simple password and role is preserved
    val translatorResult = repo.signIn(
      provider = "GOOGLE",
      email = "translator1@gmail.com",
      password = "translatorpass",
      displayName = "Translator One",
      role = "TRANSLATOR",
      authorSlot = 1
    )
    assertTrue(translatorResult.isSuccess)
    val translatorUser = repo.activeUser.first()
    assertNotNull(translatorUser)
    assertEquals("TRANSLATOR", translatorUser?.role)
    assertEquals(1, translatorUser?.authorSlot)

    // Returning Translator logs in again with just email and same password
    val returningTranslatorResult = repo.signIn(
      provider = "GOOGLE",
      email = "translator1@gmail.com",
      password = "translatorpass",
      displayName = ""
    )
    assertTrue(returningTranslatorResult.isSuccess)
    val returningUser = repo.activeUser.first()
    assertNotNull(returningUser)
    assertEquals("TRANSLATOR", returningUser?.role)
    assertEquals(1, returningUser?.authorSlot)

    // Sign out should NOT erase remembered accounts
    repo.signOut()
    assertNull(repo.activeUser.first())
    val rememberedList = repo.rememberedAccounts.first()
    assertTrue(rememberedList.any { it.email == "translator1@gmail.com" })

    // Forgot password flow: request 6-digit code for Gmail
    val codeResult = repo.sendPasswordRecoveryCode("translator1@gmail.com")
    assertTrue(codeResult.isSuccess)
    val recoveryCode = codeResult.getOrNull()
    assertNotNull(recoveryCode)
    assertEquals(6, recoveryCode?.length)

    // Reset password using recovery code
    val resetResult = repo.resetPasswordWithCode(
      email = "translator1@gmail.com",
      code = recoveryCode!!,
      newPassword = "newsecretpassword123"
    )
    assertTrue(resetResult.isSuccess)

    // Verify user is automatically signed in with updated credentials
    val recoveredUser = repo.activeUser.first()
    assertNotNull(recoveredUser)
    assertEquals("translator1@gmail.com", recoveredUser?.email)

    // Sign in with the new password succeeds
    repo.signOut()
    val newSignInResult = repo.signIn(
      provider = "GOOGLE",
      email = "translator1@gmail.com",
      password = "newsecretpassword123",
      displayName = ""
    )
    assertTrue(newSignInResult.isSuccess)
  }

  @Test
  fun `upload novel permission is strictly restricted to translators and owner`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = StrawberrycandyDatabase.getInstance(context)
    val dao = db.strawberrycandyDao()
    val repo = StrawberrycandyRepository(dao)

    // Initially sign out (guest)
    repo.signOut()
    assertNull(repo.activeUser.first())

    val app = ApplicationProvider.getApplicationContext<Application>()
    val vm = StrawberrycandyViewModel(app)
    assertFalse(vm.canUploadNovel(null))

    // Sign in as Reader (non-translator)
    repo.signIn(
      provider = "GOOGLE",
      email = "regularreader@gmail.com",
      password = "password123",
      displayName = "Casual Reader"
    )
    val readerUser = repo.activeUser.first()
    assertNotNull(readerUser)
    assertEquals("READER", readerUser?.role)
    assertFalse(vm.canUploadNovel(readerUser))

    // Sign in as Owner
    repo.signOut()
    val ownerSignInResult = repo.signIn(
      provider = "GOOGLE",
      email = "clarifymanga@gmail.com",
      password = "clarify123",
      displayName = "Clarify"
    )
    assertTrue(ownerSignInResult.isSuccess)
    val ownerUser = repo.activeUser.first()
    assertNotNull(ownerUser)
    assertTrue(vm.canUploadNovel(ownerUser))
    assertTrue(vm.isOwner(ownerUser))

    // Owner grants access permission to Translator
    repo.grantPermissionByEmail("author.translator@gmail.com", 1)

    // Sign in as Translator
    repo.signOut()
    repo.signIn(
      provider = "GOOGLE",
      email = "author.translator@gmail.com",
      password = "newsecretpassword123",
      displayName = "Translator One",
      role = "TRANSLATOR",
      authorSlot = 1
    )
    val translatorUser = repo.activeUser.first()
    assertNotNull(translatorUser)
    assertEquals("TRANSLATOR", translatorUser?.role)
    assertTrue(vm.canUploadNovel(translatorUser))
  }
}
