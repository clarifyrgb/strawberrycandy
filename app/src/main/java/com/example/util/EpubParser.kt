package com.example.util

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

data class ExtractedStoryImage(
  val filePath: String,
  val caption: String,
)

sealed class EpubParseResult {
  data class Success(
    val title: String,
    val author: String,
    val subtitle: String,
    val excerpt: String,
    val chapterTitle: String,
    val fullContent: String,
    val coverImageUri: String?,
    val storyImages: List<ExtractedStoryImage>,
    val totalChapters: Int,
    val estimatedPages: Int,
  ) : EpubParseResult()

  data class Error(val message: String) : EpubParseResult()
}

object EpubParser {
  private const val TAG = "EpubParser"

  fun parse(context: Context, uri: Uri): EpubParseResult {
    val tempDir = File(context.cacheDir, "epub_extract_" + UUID.randomUUID().toString().take(8))
    tempDir.mkdirs()

    return try {
      // 1. Unpack ZIP contents into tempDir
      val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
      if (inputStream == null) {
        return EpubParseResult.Error("Could not open EPUB file stream.")
      }

      unzip(inputStream, tempDir)

      // 2. Locate OPF file via META-INF/container.xml
      val containerXmlFile = File(tempDir, "META-INF/container.xml")
      var opfFile: File? = null

      if (containerXmlFile.exists()) {
        val containerText = containerXmlFile.readText()
        val fullPathMatch = Regex("""full-path\s*=\s*["']([^"']+\.opf)["']""", RegexOption.IGNORE_CASE)
          .find(containerText)
        if (fullPathMatch != null) {
          val opfRelPath = fullPathMatch.groupValues[1]
          opfFile = File(tempDir, opfRelPath)
        }
      }

      // Fallback: search for any .opf file in the directory tree
      if (opfFile == null || !opfFile.exists()) {
        opfFile = tempDir.walkTopDown().firstOrNull { it.extension.equals("opf", ignoreCase = true) }
      }

      val opfDir = opfFile?.parentFile ?: tempDir

      var bookTitle = "Untitled Manuscript"
      var bookAuthor = "Translator Room"
      var bookDescription = ""
      var coverPath: String? = null
      val manifest = mutableMapOf<String, Pair<String, String>>() // id -> (href, mediaType)
      val spineItemRefs = mutableListOf<String>()

      if (opfFile != null && opfFile.exists()) {
        val opfContent = opfFile.readText()

        // Extract metadata: title, creator/author, description
        val titleMatch = Regex("""<dc:title[^>]*>(.*?)</dc:title>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
          .find(opfContent)
        if (titleMatch != null) {
          bookTitle = cleanHtmlEntities(titleMatch.groupValues[1].trim())
        }

        val authorMatch = Regex("""<dc:creator[^>]*>(.*?)</dc:creator>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
          .find(opfContent)
        if (authorMatch != null) {
          bookAuthor = cleanHtmlEntities(authorMatch.groupValues[1].trim())
        }

        val descMatch = Regex("""<dc:description[^>]*>(.*?)</dc:description>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
          .find(opfContent)
        if (descMatch != null) {
          bookDescription = stripHtmlTags(cleanHtmlEntities(descMatch.groupValues[1].trim()))
        }

        // Parse Manifest items
        val itemPattern = Regex("""<item\s+([^>]+)/?>""", RegexOption.IGNORE_CASE)
        for (match in itemPattern.findAll(opfContent)) {
          val attrs = match.groupValues[1]
          val id = Regex("""id\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(attrs)?.groupValues?.get(1) ?: continue
          val href = Regex("""href\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(attrs)?.groupValues?.get(1) ?: continue
          val mediaType = Regex("""media-type\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(attrs)?.groupValues?.get(1) ?: ""
          val properties = Regex("""properties\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(attrs)?.groupValues?.get(1) ?: ""

          manifest[id] = Pair(href, mediaType)

          // Check if marked as cover image
          if (properties.contains("cover-image", ignoreCase = true) || id.contains("cover", ignoreCase = true)) {
            if (mediaType.startsWith("image/")) {
              val imageFile = File(opfDir, href)
              if (imageFile.exists()) {
                coverPath = copyToCoversDir(context, imageFile)
              }
            }
          }
        }

        // If cover still not found, check <meta name="cover" content="cover-id" />
        if (coverPath == null) {
          val metaCoverMatch = Regex("""<meta\s+name\s*=\s*["']cover["']\s+content\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            .find(opfContent)
          val coverId = metaCoverMatch?.groupValues?.get(1)
          if (coverId != null && manifest.containsKey(coverId)) {
            val (href, _) = manifest[coverId]!!
            val imageFile = File(opfDir, href)
            if (imageFile.exists()) {
              coverPath = copyToCoversDir(context, imageFile)
            }
          }
        }

        // Parse Spine item order
        val itemRefPattern = Regex("""<itemref\s+[^>]*idref\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        for (match in itemRefPattern.findAll(opfContent)) {
          spineItemRefs.add(match.groupValues[1])
        }
      }

      // Collect HTML/XHTML chapter files in spine order or filesystem order
      val chapterFiles = mutableListOf<File>()
      for (id in spineItemRefs) {
        val href = manifest[id]?.first
        if (href != null) {
          val f = File(opfDir, href)
          if (f.exists() && (f.extension.equals("xhtml", true) || f.extension.equals("html", true) || f.extension.equals("htm", true))) {
            chapterFiles.add(f)
          }
        }
      }

      // Fallback: if spine is empty, take all xhtml/html files sorted
      if (chapterFiles.isEmpty()) {
        val found = tempDir.walkTopDown()
          .filter { it.isFile && (it.extension.equals("xhtml", true) || it.extension.equals("html", true) || it.extension.equals("htm", true)) }
          .sortedBy { it.name }
          .toList()
        chapterFiles.addAll(found)
      }

      if (chapterFiles.isEmpty()) {
        return EpubParseResult.Error("No readable text chapters found in this EPUB file.")
      }

      // 3. Process each chapter: extract text and embedded images
      val storyImages = mutableListOf<ExtractedStoryImage>()
      val storyDir = File(context.filesDir, "story_images").apply { mkdirs() }
      val paragraphsList = mutableListOf<String>()
      var firstChapterHeader = ""

      var imageCounter = 1

      for (chapterFile in chapterFiles) {
        val chapterHtml = chapterFile.readText()
        val chapterDir = chapterFile.parentFile ?: opfDir

        // Extract title of this chapter if available
        val hTagMatch = Regex("""<h[1-3][^>]*>(.*?)</h[1-3]>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
          .find(chapterHtml)
        val candidateHeader = hTagMatch?.let { stripHtmlTags(cleanHtmlEntities(it.groupValues[1])).trim() }
        if (candidateHeader.isNullOrBlank().not() && firstChapterHeader.isEmpty()) {
          firstChapterHeader = candidateHeader!!
        }

        // Extract internal images inside this chapter and convert to [image:uri:caption] markers
        val imgPattern = Regex("""<(?:img\s+[^>]*src|image\s+[^>]*xlink:href)\s*=\s*["']([^"']+)["'][^>]*>""", RegexOption.IGNORE_CASE)
        val transformedHtml = imgPattern.replace(chapterHtml) { match ->
          val rawSrc = match.groupValues[1]
          val fullTag = match.value
          // Extract alt text or caption
          val altMatch = Regex("""alt\s*=\s*["']([^"']*)["']""", RegexOption.IGNORE_CASE).find(fullTag)
          val titleMatchImg = Regex("""title\s*=\s*["']([^"']*)["']""", RegexOption.IGNORE_CASE).find(fullTag)
          val captionText = altMatch?.groupValues?.get(1)?.ifEmpty { null }
            ?: titleMatchImg?.groupValues?.get(1)?.ifEmpty { null }
            ?: "Illustration ${imageCounter}"

          // Resolve image path relative to chapter file or opf dir
          var imgFile = File(chapterDir, rawSrc)
          if (!imgFile.exists()) {
            imgFile = File(opfDir, rawSrc)
          }
          if (!imgFile.exists()) {
            val fileNameOnly = File(rawSrc).name
            imgFile = tempDir.walkTopDown().firstOrNull { it.name.equals(fileNameOnly, ignoreCase = true) } ?: imgFile
          }

          if (imgFile.exists() && imgFile.isFile) {
            val destImage = File(storyDir, "story_${System.currentTimeMillis()}_${imageCounter++}.${imgFile.extension}")
            try {
              imgFile.copyTo(destImage, overwrite = true)
              storyImages.add(ExtractedStoryImage(destImage.absolutePath, captionText))
              "\n\n[image:${destImage.absolutePath}:${captionText}]\n\n"
            } catch (e: Exception) {
              ""
            }
          } else {
            ""
          }
        }

        // Convert HTML elements to paragraphs
        val formattedText = htmlToParagraphs(transformedHtml)
        if (formattedText.isNotBlank()) {
          paragraphsList.add(formattedText)
        }
      }

      val fullContent = paragraphsList.joinToString("\n\n")
      if (fullContent.isBlank()) {
        return EpubParseResult.Error("EPUB chapters were empty or contained no text.")
      }

      // If cover is still null, look for any image in tempDir ending with cover.jpg/png or first extracted image
      if (coverPath == null) {
        val anyCover = tempDir.walkTopDown()
          .firstOrNull { it.isFile && it.name.contains("cover", ignoreCase = true) && isImageFile(it) }
        if (anyCover != null) {
          coverPath = copyToCoversDir(context, anyCover)
        } else if (storyImages.isNotEmpty()) {
          coverPath = storyImages.first().filePath
        }
      }

      // If bookTitle wasn't set or is generic, use filename or first chapter header
      if (bookTitle == "Untitled Manuscript" && firstChapterHeader.isNotEmpty()) {
        bookTitle = firstChapterHeader
      }

      val excerpt = if (bookDescription.isNotBlank()) {
        bookDescription.take(280)
      } else {
        fullContent.replace(Regex("""\[image:[^\]]+\]"""), "").trim().take(280) + "..."
      }

      val totalWords = fullContent.split(Regex("""\s+""")).size
      val estimatedPages = (totalWords / 250).coerceAtLeast(18)

      EpubParseResult.Success(
        title = bookTitle,
        author = bookAuthor,
        subtitle = if (bookAuthor.isNotEmpty()) "Translated by $bookAuthor" else "Strawberrycandy Collective Edition",
        excerpt = excerpt,
        chapterTitle = if (firstChapterHeader.isNotEmpty()) firstChapterHeader else "Chapter I • Opening Folio",
        fullContent = fullContent,
        coverImageUri = coverPath,
        storyImages = storyImages,
        totalChapters = chapterFiles.size,
        estimatedPages = estimatedPages
      )
    } catch (e: Exception) {
      Log.e(TAG, "Failed to parse EPUB", e)
      EpubParseResult.Error("Error parsing EPUB: ${e.localizedMessage ?: "Unknown format"}")
    } finally {
      // Clean up temporary extracted folder
      try {
        tempDir.deleteRecursively()
      } catch (_: Exception) {}
    }
  }

  private fun unzip(inputStream: InputStream, targetDir: File) {
    ZipInputStream(inputStream).use { zis ->
      var entry: ZipEntry? = zis.nextEntry
      while (entry != null) {
        val newFile = File(targetDir, entry.name)
        // Prevent path traversal vulnerability
        if (!newFile.canonicalPath.startsWith(targetDir.canonicalPath)) {
          zis.closeEntry()
          entry = zis.nextEntry
          continue
        }

        if (entry.isDirectory) {
          newFile.mkdirs()
        } else {
          newFile.parentFile?.mkdirs()
          FileOutputStream(newFile).use { fos ->
            zis.copyTo(fos)
          }
        }
        zis.closeEntry()
        entry = zis.nextEntry
      }
    }
  }

  private fun isImageFile(file: File): Boolean {
    val ext = file.extension.lowercase()
    return ext == "jpg" || ext == "jpeg" || ext == "png" || ext == "webp" || ext == "gif"
  }

  private fun copyToCoversDir(context: Context, source: File): String {
    val coversDir = File(context.filesDir, "covers").apply { mkdirs() }
    val dest = File(coversDir, "epub_cover_${System.currentTimeMillis()}.${source.extension}")
    source.copyTo(dest, overwrite = true)
    return dest.absolutePath
  }

  private fun stripHtmlTags(html: String): String {
    return html.replace(Regex("""<[^>]+>"""), "").trim()
  }

  private fun cleanHtmlEntities(text: String): String {
    var result = text
      .replace("&nbsp;", " ")
      .replace("&amp;", "&")
      .replace("&lt;", "<")
      .replace("&gt;", ">")
      .replace("&quot;", "\"")
      .replace("&#39;", "'")
      .replace("&apos;", "'")
      .replace("&mdash;", "—")
      .replace("&ndash;", "–")
      .replace("&hellip;", "…")
      .replace("&rsquo;", "’")
      .replace("&lsquo;", "‘")
      .replace("&rdquo;", "”")
      .replace("&ldquo;", "“")
    result = Regex("""&#(\d+);""").replace(result) { match ->
      try {
        match.groupValues[1].toInt().toChar().toString()
      } catch (_: Exception) {
        match.value
      }
    }
    return result
  }

  private fun htmlToParagraphs(html: String): String {
    var text = html
    // Preserve custom [image:...] markers
    text = text.replace(Regex("""<style[^>]*>.*?</style>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
    text = text.replace(Regex("""<script[^>]*>.*?</script>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
    text = text.replace(Regex("""<head[^>]*>.*?</head>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")

    // Line breaks
    text = text.replace(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE), "\n")
    text = text.replace(Regex("""</?(?:p|div|h[1-6]|blockquote|li|tr)[^>]*>""", RegexOption.IGNORE_CASE), "\n\n")

    // Strip remaining tags
    text = text.replace(Regex("""<[^>]+>"""), "")

    text = cleanHtmlEntities(text)

    // Normalize multiple consecutive blank lines into double newline
    val paragraphs = text.split("\n")
      .map { it.trim() }
      .filter { it.isNotEmpty() }

    return paragraphs.joinToString("\n\n")
  }
}
