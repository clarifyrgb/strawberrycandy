package com.example.data.remote

import android.content.Context
import android.util.Base64
import com.example.data.local.NovelEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

class CloudArchiveSyncService(private val context: Context) {

  companion object {
    const val PREFS_NAME = "strawberrycandy_cloud_sync"
    const val KEY_READ_URL = "cloud_archive_read_url"
    const val KEY_WRITE_URL = "cloud_archive_write_url"
    const val KEY_GITHUB_TOKEN = "github_pat_token"
    const val KEY_LAST_SYNC_TIME = "last_sync_timestamp"
    const val KEY_LAST_SYNC_COUNT = "last_sync_count"
    const val KEY_LAST_SYNC_ERROR = "last_sync_error"

    const val DEFAULT_READ_URL =
      "https://raw.githubusercontent.com/clarifyrgb/strawberrycandy/refs/heads/main/novels.json"
    const val FALLBACK_READ_URL =
      "https://raw.githubusercontent.com/clarifyrgb/strawberrycandy/main/novels.json"
    const val GITHUB_REPO_PATH = "clarifyrgb/strawberrycandy"
  }

  private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  fun getReadUrl(): String {
    return prefs.getString(KEY_READ_URL, DEFAULT_READ_URL)?.trim()?.ifBlank { DEFAULT_READ_URL } ?: DEFAULT_READ_URL
  }

  fun setReadUrl(url: String) {
    prefs.edit().putString(KEY_READ_URL, url.trim()).apply()
  }

  fun getWriteUrl(): String {
    return prefs.getString(KEY_WRITE_URL, "")?.trim() ?: ""
  }

  fun setWriteUrl(url: String) {
    prefs.edit().putString(KEY_WRITE_URL, url.trim()).apply()
  }

  fun getGitHubToken(): String {
    return prefs.getString(KEY_GITHUB_TOKEN, "")?.trim() ?: ""
  }

  fun setGitHubToken(token: String) {
    prefs.edit().putString(KEY_GITHUB_TOKEN, token.trim()).apply()
  }

  fun getLastSyncTime(): Long = prefs.getLong(KEY_LAST_SYNC_TIME, 0L)
  fun getLastSyncCount(): Int = prefs.getInt(KEY_LAST_SYNC_COUNT, 0)
  fun getLastSyncError(): String? = prefs.getString(KEY_LAST_SYNC_ERROR, null)

  private fun recordSyncSuccess(count: Int) {
    prefs.edit()
      .putLong(KEY_LAST_SYNC_TIME, System.currentTimeMillis())
      .putInt(KEY_LAST_SYNC_COUNT, count)
      .remove(KEY_LAST_SYNC_ERROR)
      .apply()
  }

  private fun recordSyncError(error: String) {
    prefs.edit()
      .putString(KEY_LAST_SYNC_ERROR, error)
      .apply()
  }

  /**
   * Fetch all novels from the remote Cloud Archive.
   * Anyone who installed the APK calls this on app launch and refresh to sync the latest uploaded novels.
   */
  suspend fun fetchRemoteNovels(customUrl: String? = null): Result<List<NovelEntity>> = withContext(Dispatchers.IO) {
    val targetUrl = customUrl?.trim()?.ifBlank { null } ?: getReadUrl()

    // Try primary target URL first
    val firstAttempt = tryFetchFromUrl(targetUrl)
    if (firstAttempt.isSuccess) {
      val novels = firstAttempt.getOrNull() ?: emptyList()
      recordSyncSuccess(novels.size)
      return@withContext Result.success(novels)
    }

    // If primary failed and it was the default URL, try the fallback raw branch
    if (targetUrl == DEFAULT_READ_URL) {
      val secondAttempt = tryFetchFromUrl(FALLBACK_READ_URL)
      if (secondAttempt.isSuccess) {
        val novels = secondAttempt.getOrNull() ?: emptyList()
        recordSyncSuccess(novels.size)
        return@withContext Result.success(novels)
      }
    }

    // If remote is unreachable, attempt to read bundled asset as offline fallback
    val assetNovels = readBundledAssetNovels()
    if (assetNovels.isNotEmpty()) {
      return@withContext Result.success(assetNovels)
    }

    val errorMsg = firstAttempt.exceptionOrNull()?.message ?: "Unable to connect to Cloud Archive ($targetUrl)"
    recordSyncError(errorMsg)
    Result.failure(Exception(errorMsg, firstAttempt.exceptionOrNull()))
  }

  private fun tryFetchFromUrl(urlString: String): Result<List<NovelEntity>> {
    var connection: HttpURLConnection? = null
    try {
      val url = URL(urlString.trim())
      connection = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 8000
        readTimeout = 8000
        useCaches = false
        setRequestProperty("Accept", "application/json, text/plain, */*")
        setRequestProperty("User-Agent", "Strawberrycandy-Android-APK")
      }

      val code = connection.responseCode
      if (code in 200..299) {
        val jsonText = connection.inputStream.bufferedReader().use { it.readText() }.trim()
        val parsed = parseNovelsFromJsonString(jsonText)
        return Result.success(parsed)
      } else {
        return Result.failure(Exception("HTTP $code: ${connection.responseMessage}"))
      }
    } catch (e: Exception) {
      return Result.failure(e)
    } finally {
      connection?.disconnect()
    }
  }

  fun readBundledAssetNovels(): List<NovelEntity> {
    return try {
      val json = context.assets.open("novels.json").bufferedReader().use { it.readText() }
      parseNovelsFromJsonString(json)
    } catch (e: Exception) {
      emptyList()
    }
  }

  fun parseNovelsFromJsonString(jsonString: String): List<NovelEntity> {
    val text = jsonString.trim()
    if (text.isEmpty()) return emptyList()

    val novelsList = mutableListOf<NovelEntity>()

    val jsonArray: JSONArray = if (text.startsWith("[")) {
      JSONArray(text)
    } else if (text.startsWith("{")) {
      val obj = JSONObject(text)
      when {
        obj.has("novels") -> obj.getJSONArray("novels")
        obj.has("data") -> obj.getJSONArray("data")
        obj.has("items") -> obj.getJSONArray("items")
        else -> {
          // Check if it's a Firebase RTDB map of ID -> Object
          val keys = obj.keys()
          val array = JSONArray()
          while (keys.hasNext()) {
            val key = keys.next()
            val child = obj.optJSONObject(key)
            if (child != null) {
              if (!child.has("id")) child.put("id", key)
              array.put(child)
            }
          }
          array
        }
      }
    } else {
      return emptyList()
    }

    for (i in 0 until jsonArray.length()) {
      val item = jsonArray.optJSONObject(i) ?: continue
      val id = item.optString("id").ifBlank { "nov_cloud_$i" }
      val title = item.optString("title").ifBlank { "Untitled Manuscript" }
      val subtitle = item.optString("subtitle", "")
      val author = item.optString("author", "Strawberrycandy")
      val originalAuthor = item.optString("originalAuthor", "")
      val authorSlot = item.optInt("authorSlot", 0)
      val year = item.optString("year", "2026")
      val editionNumber = item.optString("editionNumber", "GLOBAL CLOUD ARCHIVE")
      val coverDrawableRes = item.optInt("coverDrawableRes", 0)
      val coverImageUri = item.optString("coverImageUri").takeIf { it.isNotBlank() && it != "null" }
      val coverColorHex = item.optLong("coverColorHex", 0xFF5C2D3B)
      val chapterTitle = item.optString("chapterTitle", "Chapter I")
      val contentText = item.optString("contentText", "")
      val totalPages = item.optInt("totalPages", maxOf(1, contentText.split("\n\n").count { it.isNotBlank() } * 2))
      val excerpt = item.optString("excerpt", contentText.take(150))
      val isOwnerUploaded = item.optBoolean("isOwnerUploaded", true)
      val createdAt = item.optLong("createdAt", System.currentTimeMillis())
      val readsCount = item.optInt("readsCount", 0)
      val favoritesCount = item.optInt("favoritesCount", 0)
      val storyImagesJson = item.optString("storyImagesJson", "")
      val novelStatus = item.optString("novelStatus", "ONGOING")
      val releaseFormat = item.optString("releaseFormat", "CHAPTER")

      novelsList.add(
        NovelEntity(
          id = id,
          title = title,
          subtitle = subtitle,
          author = author,
          originalAuthor = originalAuthor,
          authorSlot = authorSlot,
          year = year,
          editionNumber = editionNumber,
          coverDrawableRes = coverDrawableRes,
          coverImageUri = coverImageUri,
          coverColorHex = coverColorHex,
          chapterTitle = chapterTitle,
          totalPages = totalPages,
          excerpt = excerpt,
          contentText = contentText,
          isOwnerUploaded = isOwnerUploaded,
          createdAt = createdAt,
          readsCount = readsCount,
          favoritesCount = favoritesCount,
          storyImagesJson = storyImagesJson,
          novelStatus = novelStatus,
          releaseFormat = releaseFormat
        )
      )
    }

    return novelsList
  }

  fun novelToJson(novel: NovelEntity): JSONObject {
    return JSONObject().apply {
      put("id", novel.id)
      put("title", novel.title)
      put("subtitle", novel.subtitle)
      put("author", novel.author)
      put("originalAuthor", novel.originalAuthor)
      put("authorSlot", novel.authorSlot)
      put("year", novel.year)
      put("editionNumber", novel.editionNumber)
      put("coverDrawableRes", novel.coverDrawableRes)
      if (novel.coverImageUri != null) put("coverImageUri", novel.coverImageUri)
      put("coverColorHex", novel.coverColorHex)
      put("chapterTitle", novel.chapterTitle)
      put("totalPages", novel.totalPages)
      put("excerpt", novel.excerpt)
      put("contentText", novel.contentText)
      put("isOwnerUploaded", novel.isOwnerUploaded)
      put("createdAt", novel.createdAt)
      put("readsCount", novel.readsCount)
      put("favoritesCount", novel.favoritesCount)
      put("storyImagesJson", novel.storyImagesJson)
      put("novelStatus", novel.novelStatus)
      put("releaseFormat", novel.releaseFormat)
    }
  }

  fun exportNovelsToJsonString(novels: List<NovelEntity>): String {
    val array = JSONArray()
    novels.forEach { array.put(novelToJson(it)) }
    return array.toString(2)
  }

  /**
   * Publish a novel to the remote cloud so all installed APKs see it.
   */
  suspend fun publishNovelToRemote(
    novel: NovelEntity,
    allNovels: List<NovelEntity>
  ): Result<String> = withContext(Dispatchers.IO) {
    val writeUrl = getWriteUrl()
    val token = getGitHubToken()

    // Mode A: Firebase Realtime Database
    if (writeUrl.isNotBlank() && writeUrl.contains("firebaseio.com", ignoreCase = true)) {
      return@withContext publishToFirebase(novel, writeUrl)
    }

    // Mode B: GitHub Contents API (Direct commit to clarifyrgb/strawberrycandy/novels.json)
    if (token.isNotBlank()) {
      return@withContext publishToGitHubApi(novel, allNovels, token)
    }

    // Mode C: Custom REST API Endpoint
    if (writeUrl.isNotBlank()) {
      return@withContext publishToCustomEndpoint(novel, writeUrl)
    }

    Result.failure(
      IllegalStateException(
        "No remote cloud write destination configured yet. Use GitHub Token, Firebase URL, or export novels.json to publish globally."
      )
    )
  }

  private fun publishToFirebase(novel: NovelEntity, baseUrl: String): Result<String> {
    var connection: HttpURLConnection? = null
    try {
      val cleanBase = baseUrl.trim().removeSuffix(".json").removeSuffix("/")
      val targetUrl = "$cleanBase/novels/${novel.id}.json"
      val url = URL(targetUrl)

      connection = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "PUT"
        connectTimeout = 10000
        readTimeout = 10000
        doOutput = true
        setRequestProperty("Content-Type", "application/json; charset=utf-8")
        setRequestProperty("Accept", "application/json")
      }

      val payload = novelToJson(novel).toString()
      OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use {
        it.write(payload)
        it.flush()
      }

      val code = connection.responseCode
      if (code in 200..299) {
        return Result.success("Successfully published '${novel.title}' to Firebase Cloud Archive!")
      } else {
        return Result.failure(Exception("Firebase returned HTTP $code: ${connection.responseMessage}"))
      }
    } catch (e: Exception) {
      return Result.failure(e)
    } finally {
      connection?.disconnect()
    }
  }

  private fun publishToCustomEndpoint(novel: NovelEntity, writeUrl: String): Result<String> {
    var connection: HttpURLConnection? = null
    try {
      val url = URL(writeUrl.trim())
      connection = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = 10000
        readTimeout = 10000
        doOutput = true
        setRequestProperty("Content-Type", "application/json; charset=utf-8")
        setRequestProperty("Accept", "application/json")
        setRequestProperty("User-Agent", "Strawberrycandy-Android-APK")
      }

      val payload = novelToJson(novel).toString()
      OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use {
        it.write(payload)
        it.flush()
      }

      val code = connection.responseCode
      if (code in 200..299) {
        return Result.success("Successfully uploaded '${novel.title}' to custom Cloud Server!")
      } else {
        return Result.failure(Exception("Cloud server returned HTTP $code: ${connection.responseMessage}"))
      }
    } catch (e: Exception) {
      return Result.failure(e)
    } finally {
      connection?.disconnect()
    }
  }

  private fun publishToGitHubApi(
    novel: NovelEntity,
    allNovels: List<NovelEntity>,
    token: String
  ): Result<String> {
    var connection: HttpURLConnection? = null
    try {
      // 1. Get current SHA of novels.json from GitHub
      val apiUrl = "https://api.github.com/repos/$GITHUB_REPO_PATH/contents/novels.json"
      var existingSha: String? = null

      try {
        val checkConn = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
          requestMethod = "GET"
          connectTimeout = 8000
          readTimeout = 8000
          setRequestProperty("Authorization", "Bearer $token")
          setRequestProperty("Accept", "application/vnd.github.v3+json")
          setRequestProperty("User-Agent", "Strawberrycandy-Android-APK")
        }
        if (checkConn.responseCode == 200) {
          val res = checkConn.inputStream.bufferedReader().use { it.readText() }
          val json = JSONObject(res)
          existingSha = json.optString("sha")
        }
        checkConn.disconnect()
      } catch (_: Exception) {}

      // 2. Build updated catalog (replace if id matches or prepend)
      val updatedList = mutableListOf<NovelEntity>()
      updatedList.add(novel)
      allNovels.filter { it.id != novel.id }.forEach { updatedList.add(it) }

      val updatedJsonText = exportNovelsToJsonString(updatedList)
      val base64Content = Base64.encodeToString(updatedJsonText.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)

      // 3. Commit updated novels.json to GitHub repository
      connection = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
        requestMethod = "PUT"
        connectTimeout = 12000
        readTimeout = 12000
        doOutput = true
        setRequestProperty("Authorization", "Bearer $token")
        setRequestProperty("Content-Type", "application/json")
        setRequestProperty("Accept", "application/vnd.github.v3+json")
        setRequestProperty("User-Agent", "Strawberrycandy-Android-APK")
      }

      val requestBody = JSONObject().apply {
        put("message", "Publish novel: '${novel.title}' by ${novel.author} via Strawberrycandy APK")
        put("content", base64Content)
        if (existingSha != null && existingSha.isNotBlank()) {
          put("sha", existingSha)
        }
      }

      OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use {
        it.write(requestBody.toString())
        it.flush()
      }

      val code = connection.responseCode
      if (code in 200..299) {
        return Result.success("✨ Successfully committed to GitHub repository! Live for all APK readers globally.")
      } else {
        val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
        return Result.failure(Exception("GitHub API HTTP $code: $errorStream"))
      }
    } catch (e: Exception) {
      return Result.failure(e)
    } finally {
      connection?.disconnect()
    }
  }
}
