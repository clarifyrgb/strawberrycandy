package com.example.data.remote

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.local.NovelEntity
import com.google.firebase.FirebaseApp
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * Service for handling uploads and synchronization with Firebase Cloud Storage.
 * Stores novel metadata, full text manuscripts, chapter backups, and cover artwork
 * directly in the Firebase Cloud Storage bucket (e.g. strawberrycandy.firebasestorage.app).
 */
class FirebaseCloudStorageService(
  private val context: Context,
) {
  companion object {
    private const val TAG = "FirebaseCloudStorage"
    const val STORAGE_BUCKET = "strawberrycandy.firebasestorage.app"
  }

  private val storage: FirebaseStorage? by lazy {
    try {
      if (FirebaseApp.getApps(context).isEmpty()) {
        FirebaseApp.initializeApp(context)
      }
      FirebaseStorage.getInstance("gs://$STORAGE_BUCKET")
    } catch (e: Exception) {
      Log.w(TAG, "Failed to initialize FirebaseStorage for $STORAGE_BUCKET", e)
      try {
        FirebaseStorage.getInstance()
      } catch (ex: Exception) {
        Log.e(TAG, "Fallback to default FirebaseStorage also failed", ex)
        null
      }
    }
  }

  /**
   * Checks if Firebase Cloud Storage is initialized and accessible.
   */
  fun isAvailable(): Boolean = storage != null

  /**
   * Uploads novel manuscript and metadata JSON to Firebase Cloud Storage.
   * Path: /novels/{novelId}/metadata.json
   */
  suspend fun uploadNovelMetadata(novel: NovelEntity): Result<String> {
    val storageInstance = storage ?: return Result.failure(IllegalStateException("Firebase Storage is not initialized"))
    return try {
      val jsonContent = JSONObject().apply {
        put("id", novel.id)
        put("title", novel.title)
        put("subtitle", novel.subtitle)
        put("author", novel.author)
        put("originalAuthor", novel.originalAuthor)
        put("authorSlot", novel.authorSlot)
        put("year", novel.year)
        put("editionNumber", novel.editionNumber)
        put("chapterTitle", novel.chapterTitle)
        put("totalPages", novel.totalPages)
        put("excerpt", novel.excerpt)
        put("coverColorHex", novel.coverColorHex)
        put("coverImageUri", novel.coverImageUri ?: "")
        put("novelStatus", novel.novelStatus)
        put("releaseFormat", novel.releaseFormat)
        put("createdAt", novel.createdAt)
        put("contentLength", novel.contentText.length)
        put("storyImagesJson", novel.storyImagesJson)
      }.toString(2).toByteArray(StandardCharsets.UTF_8)

      val metadata = StorageMetadata.Builder()
        .setContentType("application/json")
        .setCustomMetadata("novelId", novel.id)
        .setCustomMetadata("title", novel.title)
        .setCustomMetadata("author", novel.author)
        .build()

      val ref = storageInstance.reference.child("novels/${novel.id}/metadata.json")
      ref.putBytes(jsonContent, metadata).await()
      val downloadUrl = ref.downloadUrl.await().toString()
      Result.success(downloadUrl)
    } catch (e: Exception) {
      Log.e(TAG, "Error uploading novel metadata for ${novel.id}", e)
      Result.failure(e)
    }
  }

  /**
   * Uploads the complete novel manuscript / chapter text to Firebase Cloud Storage.
   * Path: /novels/{novelId}/manuscript.txt
   */
  suspend fun uploadNovelContent(novel: NovelEntity): Result<String> {
    val storageInstance = storage ?: return Result.failure(IllegalStateException("Firebase Storage is not initialized"))
    return try {
      val textBytes = novel.contentText.toByteArray(StandardCharsets.UTF_8)
      val metadata = StorageMetadata.Builder()
        .setContentType("text/plain; charset=utf-8")
        .setCustomMetadata("novelId", novel.id)
        .setCustomMetadata("title", novel.title)
        .build()

      val ref = storageInstance.reference.child("novels/${novel.id}/manuscript.txt")
      ref.putBytes(textBytes, metadata).await()
      val downloadUrl = ref.downloadUrl.await().toString()
      Result.success(downloadUrl)
    } catch (e: Exception) {
      Log.e(TAG, "Error uploading manuscript text for ${novel.id}", e)
      Result.failure(e)
    }
  }

  /**
   * Uploads novel cover image file or uri to Firebase Cloud Storage.
   * Path: /novels/{novelId}/cover.jpg
   */
  suspend fun uploadNovelCover(novelId: String, imageUriString: String): Result<String> {
    val storageInstance = storage ?: return Result.failure(IllegalStateException("Firebase Storage is not initialized"))
    return try {
      val uri = Uri.parse(imageUriString)
      val ref = storageInstance.reference.child("novels/$novelId/cover.jpg")
      
      val uploadTask = if (uri.scheme == "file" || uri.scheme == null) {
        val file = File(uri.path ?: imageUriString)
        if (file.exists()) {
          ref.putFile(Uri.fromFile(file))
        } else {
          return Result.failure(IllegalArgumentException("Cover image file does not exist: $imageUriString"))
        }
      } else {
        ref.putFile(uri)
      }

      uploadTask.await()
      val downloadUrl = ref.downloadUrl.await().toString()
      Result.success(downloadUrl)
    } catch (e: Exception) {
      Log.e(TAG, "Error uploading novel cover for $novelId", e)
      Result.failure(e)
    }
  }

  /**
   * Uploads an entire novel package (Metadata JSON, Text Manuscript, and Cover if available)
   * into Firebase Cloud Storage under /novels/{novelId}/
   */
  suspend fun uploadNovelPackage(novel: NovelEntity): Result<FirebaseUploadSummary> {
    val metaResult = uploadNovelMetadata(novel)
    if (metaResult.isFailure) {
      return Result.failure(metaResult.exceptionOrNull() ?: Exception("Failed to upload metadata"))
    }

    val contentResult = uploadNovelContent(novel)
    if (contentResult.isFailure) {
      return Result.failure(contentResult.exceptionOrNull() ?: Exception("Failed to upload manuscript"))
    }

    var coverUrl: String? = null
    if (!novel.coverImageUri.isNullOrBlank()) {
      val coverRes = uploadNovelCover(novel.id, novel.coverImageUri)
      if (coverRes.isSuccess) {
        coverUrl = coverRes.getOrNull()
      }
    }

    return Result.success(
      FirebaseUploadSummary(
        novelId = novel.id,
        metadataUrl = metaResult.getOrNull() ?: "",
        contentUrl = contentResult.getOrNull() ?: "",
        coverUrl = coverUrl,
        storageBucket = STORAGE_BUCKET
      )
    )
  }
}

data class FirebaseUploadSummary(
  val novelId: String,
  val metadataUrl: String,
  val contentUrl: String,
  val coverUrl: String?,
  val storageBucket: String,
)
