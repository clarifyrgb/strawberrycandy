package com.example.util

import java.io.File
import java.util.Locale

fun formatStatCount(count: Int): String {
  return when {
    count >= 1_000_000 -> String.format(Locale.US, "%.1fM", count / 1_000_000.0)
    count >= 1_000 -> String.format(Locale.US, "%.1fk", count / 1_000.0)
    else -> count.toString()
  }
}

fun resolveCoverModel(coverImageUri: String?, coverDrawableRes: Int = 0): Any? {
  val uriStr = coverImageUri?.trim()
  if (!uriStr.isNullOrBlank() && uriStr != "null" && uriStr != "undefined") {
    if (uriStr.startsWith("http://") || uriStr.startsWith("https://") || uriStr.startsWith("content://") || uriStr.startsWith("file://") || uriStr.startsWith("android.resource://")) {
      return uriStr
    }
    val file = File(uriStr)
    if (file.exists()) {
      return file
    }
    try {
      val app = com.example.StrawberrycandyApplication.instance
      val coversDir = File(app.filesDir, "novel_covers")
      val resolvedFile = File(coversDir, File(uriStr).name)
      if (resolvedFile.exists()) {
        return resolvedFile
      }
      val directFile = File(app.filesDir, uriStr)
      if (directFile.exists()) {
        return directFile
      }
    } catch (_: Exception) {}
  }
  if (coverDrawableRes != 0) {
    return coverDrawableRes
  }
  return null
}

