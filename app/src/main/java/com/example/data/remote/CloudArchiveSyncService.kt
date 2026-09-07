package com.example.data.remote

import android.content.Context
import android.util.Base64
import com.example.data.local.AuthorSlotEntity
import com.example.data.local.ChapterCommentEntity
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

    const val DEFAULT_CLOUD_ARCHIVE_URL =
      "https://api.restful-api.dev/objects/ff808181a067127101a07b2efb503348"
    const val DEFAULT_READ_URL =
      "https://raw.githubusercontent.com/clarifyrgb/strawberrycandy/refs/heads/main/novels.json"
    const val FALLBACK_READ_URL =
      "https://raw.githubusercontent.com/clarifyrgb/strawberrycandy/main/novels.json"
    const val GITHUB_REPO_PATH = "clarifyrgb/strawberrycandy"
  }

  private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  fun getReadUrl(): String {
    return prefs.getString(KEY_READ_URL, DEFAULT_CLOUD_ARCHIVE_URL)?.trim()?.ifBlank { DEFAULT_CLOUD_ARCHIVE_URL } ?: DEFAULT_CLOUD_ARCHIVE_URL
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

    // 1. Try primary target URL first (default: DEFAULT_CLOUD_ARCHIVE_URL)
    val firstAttempt = tryFetchFromUrl(targetUrl)
    if (firstAttempt.isSuccess) {
      val novels = firstAttempt.getOrNull() ?: emptyList()
      if (novels.isNotEmpty()) {
        recordSyncSuccess(novels.size)
        return@withContext Result.success(novels)
      }
    }

    // 2. If primary target was custom or empty, fallback to DEFAULT_CLOUD_ARCHIVE_URL
    if (targetUrl != DEFAULT_CLOUD_ARCHIVE_URL) {
      val cloudAttempt = tryFetchFromUrl(DEFAULT_CLOUD_ARCHIVE_URL)
      if (cloudAttempt.isSuccess) {
        val novels = cloudAttempt.getOrNull() ?: emptyList()
        if (novels.isNotEmpty()) {
          recordSyncSuccess(novels.size)
          return@withContext Result.success(novels)
        }
      }
    }

    // 3. Fallback to GitHub raw main branch
    val githubAttempt = tryFetchFromUrl(DEFAULT_READ_URL)
    if (githubAttempt.isSuccess) {
      val novels = githubAttempt.getOrNull() ?: emptyList()
      if (novels.isNotEmpty()) {
        recordSyncSuccess(novels.size)
        return@withContext Result.success(novels)
      }
    }

    // 4. Try GitHub fallback branch
    val githubFallbackAttempt = tryFetchFromUrl(FALLBACK_READ_URL)
    if (githubFallbackAttempt.isSuccess) {
      val novels = githubFallbackAttempt.getOrNull() ?: emptyList()
      if (novels.isNotEmpty()) {
        recordSyncSuccess(novels.size)
        return@withContext Result.success(novels)
      }
    }

    // 5. If remote is unreachable, read bundled asset as offline fallback
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
        obj.has("data") -> {
          val dataVal = obj.opt("data")
          when (dataVal) {
            is JSONArray -> dataVal
            is JSONObject -> {
              if (dataVal.has("novels")) dataVal.getJSONArray("novels")
              else if (dataVal.has("items")) dataVal.getJSONArray("items")
              else JSONArray()
            }
            else -> JSONArray()
          }
        }
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
      
      // Strict filter: Never load sample novels
      val isSample = id in listOf("nov_1", "nov_2", "nov_3", "nov_cloud_1", "nov_cloud_2", "nov_schema", "nov_crimson", "nov_celestial", "nov_whispering_pines", "nov_moonlight") ||
        title.equals("The Starlight Chronicles", ignoreCase = true) ||
        title.equals("Dawn on the Canal", ignoreCase = true) ||
        title.equals("The Count of Monte Cristo", ignoreCase = true) ||
        title.equals("No Longer Human", ignoreCase = true)
      if (isSample) continue
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

    // Mode A: Firebase Realtime Database (if explicitly configured)
    if (writeUrl.isNotBlank() && writeUrl.contains("firebaseio.com", ignoreCase = true)) {
      return@withContext publishToFirebase(novel, writeUrl)
    }

    // Mode B: GitHub Contents API (if user explicitly provided a PAT)
    if (token.isNotBlank()) {
      return@withContext publishToGitHubApi(novel, allNovels, token)
    }

    // Mode C: Custom REST API Endpoint (if user explicitly provided a custom write URL)
    if (writeUrl.isNotBlank()) {
      return@withContext publishToCustomEndpoint(novel, writeUrl)
    }

    // Mode D: Wattpad-style Direct Global Cloud Archive (Zero-Token, Zero-HTTPS config required!)
    return@withContext publishToDefaultCloudArchive(novel, allNovels)
  }

  private fun publishToDefaultCloudArchive(
    novel: NovelEntity,
    allNovels: List<NovelEntity>
  ): Result<String> {
    var connection: HttpURLConnection? = null
    try {
      // 1. Fetch current remote novels from the global cloud archive to preserve other translators' manuscripts
      val existingRemoteNovels = tryFetchFromUrl(DEFAULT_CLOUD_ARCHIVE_URL).getOrNull() ?: emptyList()

      // 2. Merge all novels: remote + local + current new novel
      val mergedMap = LinkedHashMap<String, NovelEntity>()
      existingRemoteNovels.forEach { mergedMap[it.id] = it }
      allNovels.forEach { mergedMap[it.id] = it }
      mergedMap[novel.id] = novel

      val mergedList = mergedMap.values.toList()

      val array = JSONArray()
      mergedList.forEach { array.put(novelToJson(it)) }

      // Also preserve online comments and author slots stored in the cloud object
      var existingCommentsArray = JSONArray()
      var existingAuthorSlotsArray = JSONArray()
      try {
        val checkUrl = URL(DEFAULT_CLOUD_ARCHIVE_URL)
        val checkConn = (checkUrl.openConnection() as HttpURLConnection).apply {
          requestMethod = "GET"
          connectTimeout = 8000
          readTimeout = 8000
          setRequestProperty("Accept", "application/json")
          setRequestProperty("User-Agent", "Strawberrycandy-Android-APK")
        }
        if (checkConn.responseCode in 200..299) {
          val resText = checkConn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
          val root = JSONObject(resText)
          val dataObj = root.optJSONObject("data")
          if (dataObj != null) {
            if (dataObj.has("comments")) {
              existingCommentsArray = dataObj.optJSONArray("comments") ?: JSONArray()
            }
            if (dataObj.has("authorSlots")) {
              existingAuthorSlotsArray = dataObj.optJSONArray("authorSlots") ?: JSONArray()
            }
          }
        }
        checkConn.disconnect()
      } catch (_: Exception) {}

      val payload = JSONObject().apply {
        put("name", "Strawberrycandy Global Cloud Archive")
        put("data", JSONObject().apply {
          put("novels", array)
          put("comments", existingCommentsArray)
          put("authorSlots", existingAuthorSlotsArray)
        })
      }.toString()

      val url = URL(DEFAULT_CLOUD_ARCHIVE_URL)
      connection = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "PUT"
        connectTimeout = 12000
        readTimeout = 12000
        doOutput = true
        setRequestProperty("Content-Type", "application/json; charset=utf-8")
        setRequestProperty("Accept", "application/json")
        setRequestProperty("User-Agent", "Strawberrycandy-Android-APK")
      }

      OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use {
        it.write(payload)
        it.flush()
      }

      val code = connection.responseCode
      if (code in 200..299) {
        recordSyncSuccess(mergedList.size)
        return Result.success("✨ Successfully uploaded '${novel.title}' directly to the Global Cloud Archive! All APK readers can now read it.")
      } else {
        return Result.failure(Exception("Cloud archive server returned HTTP $code: ${connection.responseMessage}"))
      }
    } catch (e: Exception) {
      return Result.failure(e)
    } finally {
      connection?.disconnect()
    }
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

  suspend fun publishUpdateManifestToGitHub(
    versionName: String,
    versionCode: Int,
    title: String,
    changelog: String,
    apkUrl: String,
    releasePageUrl: String,
    token: String
  ): Result<String> = withContext(Dispatchers.IO) {
    var connection: HttpURLConnection? = null
    try {
      val apiUrl = "https://api.github.com/repos/$GITHUB_REPO_PATH/contents/update.json"
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

      val updateJson = JSONObject().apply {
        put("versionName", versionName)
        put("versionCode", versionCode)
        put("title", title)
        put("changelog", changelog)
        put("apkUrl", apkUrl)
        put("releasePageUrl", releasePageUrl)
        put("publishedDate", java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date()))
      }

      val jsonText = updateJson.toString(2)
      val base64Content = Base64.encodeToString(jsonText.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)

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
        put("message", "Release APK update manifest: $versionName (Build $versionCode)")
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
        Result.success("✨ Successfully updated update.json on GitHub! All installed APKs will now detect this update.")
      } else {
        val err = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
        Result.failure(Exception("GitHub API HTTP $code: $err"))
      }
    } catch (e: Exception) {
      Result.failure(e)
    } finally {
      connection?.disconnect()
    }
  }

  data class GitHubReleaseTagResult(
    val isSuccess: Boolean,
    val tagName: String,
    val htmlUrl: String,
    val message: String,
    val releaseId: Long? = null
  )

  suspend fun createGitHubReleaseTag(
    tagName: String,
    releaseTitle: String,
    releaseNotes: String,
    targetBranch: String = "main",
    isDraft: Boolean = false,
    isPrerelease: Boolean = false,
    token: String
  ): Result<GitHubReleaseTagResult> = withContext(Dispatchers.IO) {
    var connection: HttpURLConnection? = null
    try {
      var cleanTag = tagName.trim()
      if (cleanTag.isBlank()) {
        return@withContext Result.failure(IllegalArgumentException("Tag name cannot be empty (e.g. 'v1.0.1' or 'v1.0.2')"))
      }
      if (!cleanTag.startsWith("v", ignoreCase = true)) {
        cleanTag = "v$cleanTag"
      }

      val apiUrl = "https://api.github.com/repos/$GITHUB_REPO_PATH/releases"
      val releaseBody = JSONObject().apply {
        put("tag_name", cleanTag)
        put("target_commitish", targetBranch.trim().ifBlank { "main" })
        put("name", releaseTitle.trim().ifBlank { "Strawberrycandy $cleanTag" })
        put("body", releaseNotes.trim())
        put("draft", isDraft)
        put("prerelease", isPrerelease)
        put("generate_release_notes", false)
      }

      connection = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = 12000
        readTimeout = 12000
        doOutput = true
        setRequestProperty("Authorization", "Bearer $token")
        setRequestProperty("Content-Type", "application/json")
        setRequestProperty("Accept", "application/vnd.github.v3+json")
        setRequestProperty("User-Agent", "Strawberrycandy-Android-APK")
      }

      OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use {
        it.write(releaseBody.toString())
        it.flush()
      }

      val code = connection.responseCode
      if (code in 200..299) {
        val res = connection.inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(res)
        val htmlUrl = json.optString("html_url", "https://github.com/$GITHUB_REPO_PATH/releases/tag/$cleanTag")
        val releaseId = json.optLong("id")
        return@withContext Result.success(
          GitHubReleaseTagResult(
            isSuccess = true,
            tagName = cleanTag,
            htmlUrl = htmlUrl,
            message = "✨ GitHub release and git tag '$cleanTag' successfully created!",
            releaseId = releaseId
          )
        )
      } else {
        val errStream = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
        if (code == 422 && (errStream.contains("already_exists") || errStream.contains("already exists"))) {
          val existingUrl = "https://github.com/$GITHUB_REPO_PATH/releases/tag/$cleanTag"
          return@withContext Result.success(
            GitHubReleaseTagResult(
              isSuccess = true,
              tagName = cleanTag,
              htmlUrl = existingUrl,
              message = "ℹ️ Release tag '$cleanTag' already exists on GitHub. You can upload the APK directly to this release."
            )
          )
        }
        return@withContext Result.failure(Exception("GitHub API HTTP $code: $errStream"))
      }
    } catch (e: Exception) {
      return@withContext Result.failure(e)
    } finally {
      connection?.disconnect()
    }
  }

  /**
   * Online Comments Synchronization:
   * Enables all readers across different devices and APK installs to interact, post, reply, and like comments in real-time.
   */
  suspend fun fetchRemoteComments(novelId: String? = null): Result<List<ChapterCommentEntity>> = withContext(Dispatchers.IO) {
    var connection: HttpURLConnection? = null
    try {
      val url = URL(DEFAULT_CLOUD_ARCHIVE_URL)
      connection = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 8000
        readTimeout = 8000
        setRequestProperty("Accept", "application/json")
        setRequestProperty("User-Agent", "Strawberrycandy-Android-APK")
      }
      val code = connection.responseCode
      if (code !in 200..299) {
        return@withContext Result.failure(Exception("Cloud archive returned HTTP $code"))
      }
      val responseText = connection.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
      val rootJson = JSONObject(responseText)
      val dataObj = rootJson.optJSONObject("data") ?: return@withContext Result.success(emptyList())
      val commentsArray = dataObj.optJSONArray("comments") ?: return@withContext Result.success(emptyList())
      val commentsList = mutableListOf<ChapterCommentEntity>()
      for (i in 0 until commentsArray.length()) {
        val item = commentsArray.optJSONObject(i) ?: continue
        val cNovelId = item.optString("novelId")
        if (novelId != null && cNovelId != novelId) continue
        val cId = item.optString("id")
        if (cId.isBlank()) continue
        commentsList.add(
          ChapterCommentEntity(
            id = cId,
            novelId = cNovelId,
            chapterTitle = item.optString("chapterTitle", "Chapter I"),
            readerName = item.optString("readerName", "Reader • Literary Guest"),
            readerEmail = item.optString("readerEmail", ""),
            commentText = item.optString("commentText", ""),
            timestamp = item.optLong("timestamp", System.currentTimeMillis()),
            avatarColorHex = item.optLong("avatarColorHex", 0xFF5C2D3B),
            likesCount = item.optInt("likesCount", 0),
            isLikedByMe = false,
            parentCommentId = item.optString("parentCommentId").takeIf { it.isNotBlank() && it != "null" },
            replyToReaderName = item.optString("replyToReaderName").takeIf { it.isNotBlank() && it != "null" }
          )
        )
      }
      Result.success(commentsList)
    } catch (e: Exception) {
      Result.failure(e)
    } finally {
      connection?.disconnect()
    }
  }

  suspend fun pushCommentToCloud(comment: ChapterCommentEntity): Result<Unit> = withContext(Dispatchers.IO) {
    var connection: HttpURLConnection? = null
    try {
      // 1. Fetch current cloud state
      val fetchUrl = URL(DEFAULT_CLOUD_ARCHIVE_URL)
      val getConn = (fetchUrl.openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 8000
        readTimeout = 8000
        setRequestProperty("Accept", "application/json")
        setRequestProperty("User-Agent", "Strawberrycandy-Android-APK")
      }
      val getCode = getConn.responseCode
      val currentRoot = if (getCode in 200..299) {
        val text = getConn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        JSONObject(text)
      } else {
        JSONObject()
      }
      getConn.disconnect()

      val dataObj = currentRoot.optJSONObject("data") ?: JSONObject()
      val novelsArr = dataObj.optJSONArray("novels") ?: JSONArray()
      val existingCommentsArr = dataObj.optJSONArray("comments") ?: JSONArray()
      val existingAuthorSlotsArr = dataObj.optJSONArray("authorSlots") ?: JSONArray()

      // Merge comments by ID (update or append)
      val commentsMap = LinkedHashMap<String, JSONObject>()
      for (i in 0 until existingCommentsArr.length()) {
        val item = existingCommentsArr.optJSONObject(i) ?: continue
        val cId = item.optString("id")
        if (cId.isNotBlank()) commentsMap[cId] = item
      }

      val newObj = JSONObject().apply {
        put("id", comment.id)
        put("novelId", comment.novelId)
        put("chapterTitle", comment.chapterTitle)
        put("readerName", comment.readerName)
        put("readerEmail", comment.readerEmail)
        put("commentText", comment.commentText)
        put("timestamp", comment.timestamp)
        put("avatarColorHex", comment.avatarColorHex)
        put("likesCount", comment.likesCount)
        put("parentCommentId", comment.parentCommentId ?: JSONObject.NULL)
        put("replyToReaderName", comment.replyToReaderName ?: JSONObject.NULL)
      }
      commentsMap[comment.id] = newObj

      val updatedCommentsArr = JSONArray()
      commentsMap.values.forEach { updatedCommentsArr.put(it) }

      val payload = JSONObject().apply {
        put("name", "Strawberrycandy Global Cloud Archive")
        put("data", JSONObject().apply {
          put("novels", novelsArr)
          put("comments", updatedCommentsArr)
          put("authorSlots", existingAuthorSlotsArr)
        })
      }.toString()

      val putUrl = URL(DEFAULT_CLOUD_ARCHIVE_URL)
      connection = (putUrl.openConnection() as HttpURLConnection).apply {
        requestMethod = "PUT"
        connectTimeout = 10000
        readTimeout = 10000
        doOutput = true
        setRequestProperty("Content-Type", "application/json; charset=utf-8")
        setRequestProperty("Accept", "application/json")
        setRequestProperty("User-Agent", "Strawberrycandy-Android-APK")
      }
      OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use {
        it.write(payload)
        it.flush()
      }
      val putCode = connection.responseCode
      if (putCode in 200..299) {
        Result.success(Unit)
      } else {
        Result.failure(Exception("Failed to push comment online (HTTP $putCode)"))
      }
    } catch (e: Exception) {
      Result.failure(e)
    } finally {
      connection?.disconnect()
    }
  }

  suspend fun likeCommentInCloud(commentId: String): Result<Unit> = withContext(Dispatchers.IO) {
    var connection: HttpURLConnection? = null
    try {
      val fetchUrl = URL(DEFAULT_CLOUD_ARCHIVE_URL)
      val getConn = (fetchUrl.openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 8000
        readTimeout = 8000
        setRequestProperty("Accept", "application/json")
        setRequestProperty("User-Agent", "Strawberrycandy-Android-APK")
      }
      val getCode = getConn.responseCode
      val currentRoot = if (getCode in 200..299) {
        val text = getConn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        JSONObject(text)
      } else {
        JSONObject()
      }
      getConn.disconnect()

      val dataObj = currentRoot.optJSONObject("data") ?: return@withContext Result.failure(Exception("No data"))
      val novelsArr = dataObj.optJSONArray("novels") ?: JSONArray()
      val existingCommentsArr = dataObj.optJSONArray("comments") ?: JSONArray()
      val existingAuthorSlotsArr = dataObj.optJSONArray("authorSlots") ?: JSONArray()

      for (i in 0 until existingCommentsArr.length()) {
        val item = existingCommentsArr.optJSONObject(i) ?: continue
        if (item.optString("id") == commentId) {
          val currentLikes = item.optInt("likesCount", 0)
          item.put("likesCount", currentLikes + 1)
          break
        }
      }

      val payload = JSONObject().apply {
        put("name", "Strawberrycandy Global Cloud Archive")
        put("data", JSONObject().apply {
          put("novels", novelsArr)
          put("comments", existingCommentsArr)
          put("authorSlots", existingAuthorSlotsArr)
        })
      }.toString()

      val putUrl = URL(DEFAULT_CLOUD_ARCHIVE_URL)
      connection = (putUrl.openConnection() as HttpURLConnection).apply {
        requestMethod = "PUT"
        connectTimeout = 10000
        readTimeout = 10000
        doOutput = true
        setRequestProperty("Content-Type", "application/json; charset=utf-8")
        setRequestProperty("Accept", "application/json")
        setRequestProperty("User-Agent", "Strawberrycandy-Android-APK")
      }
      OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use {
        it.write(payload)
        it.flush()
      }
      val putCode = connection.responseCode
      if (putCode in 200..299) {
        Result.success(Unit)
      } else {
        Result.failure(Exception("HTTP $putCode"))
      }
    } catch (e: Exception) {
      Result.failure(e)
    } finally {
      connection?.disconnect()
    }
  }

  /**
   * Author Slots & Permissions Synchronization:
   * Enables owner to grant permissions to translators across different devices and APK installs.
   */
  fun authorSlotToJson(slot: AuthorSlotEntity): JSONObject {
    return JSONObject().apply {
      put("slotNumber", slot.slotNumber)
      put("authorName", slot.authorName)
      put("penName", slot.penName)
      put("bio", slot.bio)
      put("coverImageUri", slot.coverImageUri ?: JSONObject.NULL)
      put("translatorEmail", slot.translatorEmail ?: JSONObject.NULL)
      put("accessCode", slot.accessCode)
      put("isClaimed", slot.isClaimed)
      put("isPermissionGranted", slot.isPermissionGranted)
      put("lastActiveTimestamp", slot.lastActiveTimestamp)
    }
  }

  fun jsonToAuthorSlot(obj: JSONObject): AuthorSlotEntity {
    val slotNumber = obj.optInt("slotNumber", 1)
    val rawAuthor = obj.optString("authorName", "")
    val authorName = if (rawAuthor.startsWith("Translator ", ignoreCase = true) || rawAuthor.startsWith("Author ", ignoreCase = true)) "" else rawAuthor
    val rawPen = obj.optString("penName", "")
    val penName = if (rawPen.startsWith("Translator ", ignoreCase = true) || rawPen.startsWith("Author ", ignoreCase = true)) "" else rawPen
    val bio = obj.optString("bio", "")
    val coverImageUri = obj.optString("coverImageUri").takeIf { it.isNotBlank() && it != "null" }
    val translatorEmail = obj.optString("translatorEmail").takeIf { it.isNotBlank() && it != "null" }
    val accessCode = obj.optString("accessCode", "AUTH-ROOM-$slotNumber")
    val isClaimed = obj.optBoolean("isClaimed", false)
    val isPermissionGranted = obj.optBoolean("isPermissionGranted", false)
    val lastActiveTimestamp = obj.optLong("lastActiveTimestamp", System.currentTimeMillis())
    return AuthorSlotEntity(
      slotNumber = slotNumber,
      authorName = authorName,
      penName = penName,
      bio = bio,
      coverImageUri = coverImageUri,
      accessCode = accessCode,
      translatorEmail = translatorEmail,
      isClaimed = isClaimed,
      isPermissionGranted = isPermissionGranted,
      lastActiveTimestamp = lastActiveTimestamp
    )
  }

  suspend fun fetchRemoteAuthorSlots(): Result<List<AuthorSlotEntity>> = withContext(Dispatchers.IO) {
    var connection: HttpURLConnection? = null
    try {
      val url = URL(DEFAULT_CLOUD_ARCHIVE_URL)
      connection = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 8000
        readTimeout = 8000
        setRequestProperty("Accept", "application/json")
        setRequestProperty("User-Agent", "Strawberrycandy-Android-APK")
      }
      val code = connection.responseCode
      if (code !in 200..299) {
        return@withContext Result.failure(Exception("Cloud archive returned HTTP $code"))
      }
      val responseText = connection.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
      val rootJson = JSONObject(responseText)
      val dataObj = rootJson.optJSONObject("data") ?: return@withContext Result.success(emptyList())
      val slotsArray = dataObj.optJSONArray("authorSlots") ?: return@withContext Result.success(emptyList())
      val slotsList = mutableListOf<AuthorSlotEntity>()
      for (i in 0 until slotsArray.length()) {
        val item = slotsArray.optJSONObject(i) ?: continue
        slotsList.add(jsonToAuthorSlot(item))
      }
      Result.success(slotsList)
    } catch (e: Exception) {
      Result.failure(e)
    } finally {
      connection?.disconnect()
    }
  }

  suspend fun publishAuthorSlotsToRemote(slots: List<AuthorSlotEntity>): Result<String> = withContext(Dispatchers.IO) {
    var connection: HttpURLConnection? = null
    try {
      var existingNovelsArr = JSONArray()
      var existingCommentsArr = JSONArray()
      try {
        val checkUrl = URL(DEFAULT_CLOUD_ARCHIVE_URL)
        val checkConn = (checkUrl.openConnection() as HttpURLConnection).apply {
          requestMethod = "GET"
          connectTimeout = 8000
          readTimeout = 8000
          setRequestProperty("Accept", "application/json")
          setRequestProperty("User-Agent", "Strawberrycandy-Android-APK")
        }
        if (checkConn.responseCode in 200..299) {
          val resText = checkConn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
          val root = JSONObject(resText)
          val dataObj = root.optJSONObject("data")
          if (dataObj != null) {
            existingNovelsArr = dataObj.optJSONArray("novels") ?: JSONArray()
            existingCommentsArr = dataObj.optJSONArray("comments") ?: JSONArray()
          }
        }
        checkConn.disconnect()
      } catch (_: Exception) {}

      val slotsArr = JSONArray()
      slots.forEach { slot ->
        slotsArr.put(authorSlotToJson(slot))
      }

      val payload = JSONObject().apply {
        put("name", "Strawberrycandy Global Cloud Archive")
        put("data", JSONObject().apply {
          put("novels", existingNovelsArr)
          put("comments", existingCommentsArr)
          put("authorSlots", slotsArr)
        })
      }.toString()

      val url = URL(DEFAULT_CLOUD_ARCHIVE_URL)
      connection = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "PUT"
        connectTimeout = 12000
        readTimeout = 12000
        doOutput = true
        setRequestProperty("Content-Type", "application/json; charset=utf-8")
        setRequestProperty("Accept", "application/json")
        setRequestProperty("User-Agent", "Strawberrycandy-Android-APK")
      }

      OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use {
        it.write(payload)
        it.flush()
      }

      val code = connection.responseCode
      if (code in 200..299) {
        Result.success("Author slots synchronized to Global Cloud Archive")
      } else {
        Result.failure(Exception("Cloud archive returned HTTP $code"))
      }
    } catch (e: Exception) {
      Result.failure(e)
    } finally {
      connection?.disconnect()
    }
  }
}
