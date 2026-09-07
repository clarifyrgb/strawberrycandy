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

      // Collect CSS files and parse global styling rules
      val globalCssRules = mutableMapOf<String, CssStyleRule>()
      val cssFiles = tempDir.walkTopDown()
        .filter { it.isFile && it.extension.equals("css", ignoreCase = true) }
        .toList()
      for (cssFile in cssFiles) {
        try {
          parseCssTextIntoRules(cssFile.readText(), globalCssRules)
        } catch (_: Exception) {}
      }

      // 3. Process each chapter: extract text and embedded images
      val storyImages = mutableListOf<ExtractedStoryImage>()
      val storyDir = File(context.filesDir, "story_images").apply { mkdirs() }
      val paragraphsList = mutableListOf<String>()
      var firstChapterHeader = ""

      var imageCounter = 1
      var chapterIndexCounter = 1

      for (chapterFile in chapterFiles) {
        val chapterHtml = chapterFile.readText()
        val chapterDir = chapterFile.parentFile ?: opfDir

        // Extract title of this chapter with comprehensive heuristics
        var candidateHeader: String? = null
        
        // 1. Check h1 to h6 tags
        val hTagMatch = Regex("""<h[1-6][^>]*>(.*?)</h[1-6]>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
          .find(chapterHtml)
        if (hTagMatch != null) {
          val clean = stripHtmlTags(cleanHtmlEntities(hTagMatch.groupValues[1])).trim()
          if (clean.isNotBlank()) candidateHeader = clean
        }

        // 2. Check elements with class or id referencing chapter/title
        if (candidateHeader.isNullOrBlank()) {
          val classTitleMatch = Regex("""<(?:p|div|span)[^>]*(?:class|id)=["'][^"']*(?:chapter|title|heading|subhead)[^"']*["'][^>]*>(.*?)</(?:p|div|span)>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            .find(chapterHtml)
          if (classTitleMatch != null) {
            val clean = stripHtmlTags(cleanHtmlEntities(classTitleMatch.groupValues[1])).trim()
            if (clean.isNotBlank()) candidateHeader = clean
          }
        }

        // 3. Check HTML <title> tag
        if (candidateHeader.isNullOrBlank()) {
          val titleMatch = Regex("""<title[^>]*>(.*?)</title>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            .find(chapterHtml)
          if (titleMatch != null) {
            val clean = stripHtmlTags(cleanHtmlEntities(titleMatch.groupValues[1])).trim()
            if (clean.isNotBlank() && !clean.equals(bookTitle, ignoreCase = true)) {
              candidateHeader = clean
            }
          }
        }

        // 4. Check file name for chapter indicators
        if (candidateHeader.isNullOrBlank()) {
          val nameWithoutExt = chapterFile.nameWithoutExtension
          val chNumMatch = Regex("""(?:chapter|ch)[-_]?(\d+)""", RegexOption.IGNORE_CASE).find(nameWithoutExt)
          if (chNumMatch != null) {
            candidateHeader = "Chapter ${chNumMatch.groupValues[1]}"
          }
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

        // Convert HTML elements to paragraphs preserving built-in CSS and styling
        val formattedText = htmlToParagraphs(transformedHtml, globalCssRules)
        if (formattedText.isNotBlank()) {
          val finalChapterTitle = if (!candidateHeader.isNullOrBlank()) {
            candidateHeader
          } else {
            "Chapter $chapterIndexCounter"
          }
          if (firstChapterHeader.isEmpty()) {
            firstChapterHeader = finalChapterTitle
          }
          paragraphsList.add("[chapter:$finalChapterTitle]")
          chapterIndexCounter++
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

  data class CssStyleRule(
    val textAlign: String? = null, // "center", "right", "justify"
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val isLineThrough: Boolean = false,
    val isQuote: Boolean = false,
    val isSceneBreak: Boolean = false,
    val isHidden: Boolean = false
  )

  private fun parseCssTextIntoRules(cssText: String, rulesMap: MutableMap<String, CssStyleRule>) {
    // Matches: selector { properties }
    val blockRegex = Regex("""([^{]+)\{([^}]+)\}""", RegexOption.DOT_MATCHES_ALL)
    for (match in blockRegex.findAll(cssText)) {
      val rawSelectors = match.groupValues[1]
      val rawProps = match.groupValues[2]

      var align: String? = null
      var bold = false
      var italic = false
      var underline = false
      var lineThrough = false
      var quote = false
      var sceneBreak = false
      var hidden = false

      val lowerProps = rawProps.lowercase()
      if (lowerProps.contains("text-align") || lowerProps.contains("text-align:")) {
        if (Regex("""text-align\s*:\s*center""").containsMatchIn(lowerProps)) align = "center"
        else if (Regex("""text-align\s*:\s*right""").containsMatchIn(lowerProps)) align = "right"
        else if (Regex("""text-align\s*:\s*justify""").containsMatchIn(lowerProps)) align = "justify"
      }

      if (Regex("""font-weight\s*:\s*(bold|bolder|[6-9]00)""").containsMatchIn(lowerProps)) bold = true
      if (Regex("""font-style\s*:\s*(italic|oblique)""").containsMatchIn(lowerProps)) italic = true
      if (Regex("""text-decoration\s*:[^;]*underline""").containsMatchIn(lowerProps)) underline = true
      if (Regex("""text-decoration\s*:[^;]*line-through""").containsMatchIn(lowerProps)) lineThrough = true
      if (Regex("""display\s*:\s*none""").containsMatchIn(lowerProps)) hidden = true
      if (lowerProps.contains("border-left") || lowerProps.contains("margin-left: 2") || lowerProps.contains("margin-left: 3")) quote = true

      val selectors = rawSelectors.split(",")
      for (sel in selectors) {
        val trimmed = sel.trim()
        val classMatches = Regex("""\.([a-zA-Z0-9_-]+)""").findAll(trimmed)
        for (cm in classMatches) {
          val className = cm.groupValues[1].lowercase()
          val current = rulesMap[className] ?: CssStyleRule()
          rulesMap[className] = current.copy(
            textAlign = align ?: current.textAlign,
            isBold = bold || current.isBold,
            isItalic = italic || current.isItalic,
            isUnderline = underline || current.isUnderline,
            isLineThrough = lineThrough || current.isLineThrough,
            isQuote = quote || current.isQuote,
            isSceneBreak = sceneBreak || current.isSceneBreak,
            isHidden = hidden || current.isHidden
          )
        }
      }
    }
  }

  private fun htmlToParagraphs(html: String, globalCssRules: Map<String, CssStyleRule> = emptyMap()): String {
    val activeRules = globalCssRules.toMutableMap()
    var text = html

    // Parse any inline <style> blocks before stripping them
    val styleBlockRegex = Regex("""<style[^>]*>(.*?)</style>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    for (match in styleBlockRegex.findAll(text)) {
      try {
        parseCssTextIntoRules(match.groupValues[1], activeRules)
      } catch (_: Exception) {}
    }

    // Strip styles, scripts, head, and HTML comments
    text = text.replace(styleBlockRegex, "")
    text = text.replace(Regex("""<!--.*?-->""", RegexOption.DOT_MATCHES_ALL), "")
    text = text.replace(Regex("""<script[^>]*>.*?</script>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
    text = text.replace(Regex("""<head[^>]*>.*?</head>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")

    // Strip elements marked with display: none
    text = text.replace(Regex("""<(?:p|div|span)[^>]*style=["'][^"']*display\s*:\s*none[^"']*["'][^>]*>.*?</(?:p|div|span)>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")

    // Convert decorative horizontal rules and scene breaks
    text = text.replace(Regex("""<hr\s*/?>""", RegexOption.IGNORE_CASE), "\n\n❦\n\n")

    // Preserve blockquotes and quote classes
    text = text.replace(Regex("""<blockquote[^>]*>(.*?)</blockquote>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))) {
      "\n\n[quote]${it.groupValues[1].trim()}[/quote]\n\n"
    }

    // Preserve headings h1-h6 as bold section breaks
    text = text.replace(Regex("""<h[1-6][^>]*>(.*?)</h[1-6]>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))) {
      val content = it.groupValues[1].trim()
      "\n\n<b>$content</b>\n\n"
    }

    // Preserve CSS text alignment and formatting from class attributes and style attributes
    val blockPattern = Regex("""<(p|div|center)[^>]*>(.*?)</\1>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    text = blockPattern.replace(text) { match ->
      val tagContent = match.value
      val innerContent = match.groupValues[2].trim()

      val isCenter = tagContent.contains("text-align: center", ignoreCase = true) ||
          tagContent.contains("text-align:center", ignoreCase = true) ||
          tagContent.startsWith("<center", ignoreCase = true) ||
          Regex("""class=["'][^"']*(?:center|align-center|aligncenter|caption|centered)[^"']*["']""", RegexOption.IGNORE_CASE).containsMatchIn(tagContent) ||
          hasMatchingRule(tagContent, activeRules) { it.textAlign == "center" }

      val isRight = tagContent.contains("text-align: right", ignoreCase = true) ||
          tagContent.contains("text-align:right", ignoreCase = true) ||
          Regex("""class=["'][^"']*(?:right|align-right|alignright)[^"']*["']""", RegexOption.IGNORE_CASE).containsMatchIn(tagContent) ||
          hasMatchingRule(tagContent, activeRules) { it.textAlign == "right" }

      val isQuote = Regex("""class=["'][^"']*(?:quote|epigraph|verse|stanza|poem|letter|telegram)[^"']*["']""", RegexOption.IGNORE_CASE).containsMatchIn(tagContent) ||
          hasMatchingRule(tagContent, activeRules) { it.isQuote }

      val isSceneBreak = Regex("""class=["'][^"']*(?:scene-break|separator|fleuron|divider|ornament|asterisk)[^"']*["']""", RegexOption.IGNORE_CASE).containsMatchIn(tagContent) ||
          hasMatchingRule(tagContent, activeRules) { it.isSceneBreak }

      if (isSceneBreak) {
        "\n\n❦\n\n"
      } else if (isQuote) {
        "\n\n[quote]$innerContent[/quote]\n\n"
      } else if (isCenter) {
        "\n\n[align:center]$innerContent[/align]\n\n"
      } else if (isRight) {
        "\n\n[align:right]$innerContent[/align]\n\n"
      } else {
        "\n\n$innerContent\n\n"
      }
    }

    // Handle spans with bold, italic, underline, or strikethrough classes and styles
    val spanPattern = Regex("""<span[^>]*>(.*?)</span>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    text = spanPattern.replace(text) { match ->
      val fullTag = match.value
      var inner = match.groupValues[1]

      val isBold = fullTag.contains("font-weight: bold", ignoreCase = true) ||
          fullTag.contains("font-weight:bold", ignoreCase = true) ||
          Regex("""class=["'][^"']*(?:bold|strong|heavy)[^"']*["']""", RegexOption.IGNORE_CASE).containsMatchIn(fullTag) ||
          hasMatchingRule(fullTag, activeRules) { it.isBold }

      val isItalic = fullTag.contains("font-style: italic", ignoreCase = true) ||
          fullTag.contains("font-style:italic", ignoreCase = true) ||
          fullTag.contains("font-style: oblique", ignoreCase = true) ||
          Regex("""class=["'][^"']*(?:italic|italics|emphasis|emph)[^"']*["']""", RegexOption.IGNORE_CASE).containsMatchIn(fullTag) ||
          hasMatchingRule(fullTag, activeRules) { it.isItalic }

      val isUnderline = fullTag.contains("text-decoration: underline", ignoreCase = true) ||
          hasMatchingRule(fullTag, activeRules) { it.isUnderline }

      val isLineThrough = fullTag.contains("text-decoration: line-through", ignoreCase = true) ||
          hasMatchingRule(fullTag, activeRules) { it.isLineThrough }

      if (isBold) inner = "<b>$inner</b>"
      if (isItalic) inner = "<i>$inner</i>"
      if (isUnderline) inner = "<u>$inner</u>"
      if (isLineThrough) inner = "<s>$inner</s>"
      inner
    }

    // Normalize semantic tags
    text = text.replace(Regex("""<strong[^>]*>(.*?)</strong>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "<b>$1</b>")
    text = text.replace(Regex("""<em[^>]*>(.*?)</em>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "<i>$1</i>")
    text = text.replace(Regex("""<ins[^>]*>(.*?)</ins>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "<u>$1</u>")
    text = text.replace(Regex("""<(?:del|strike)[^>]*>(.*?)</(?:del|strike)>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "<s>$1</s>")

    // Line breaks inside stanzas or paragraphs
    text = text.replace(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE), "\n")
    text = text.replace(Regex("""</?(?:p|div|li|tr|ul|ol|table)[^>]*>""", RegexOption.IGNORE_CASE), "\n\n")

    // Strip remaining tags except our preserved formatting tags: <b>, </b>, <i>, </i>, <u>, </u>, <s>, </s>
    text = text.replace(Regex("""<(?!/?(?:b|i|u|s)\b)[^>]+>""", RegexOption.IGNORE_CASE), "")

    text = cleanHtmlEntities(text)

    // Normalize multiple consecutive blank lines into double newline
    val rawParagraphs = text.split("\n\n")
    val resultList = mutableListOf<String>()
    for (p in rawParagraphs) {
      val trimmed = p.trim()
      if (trimmed.isNotEmpty()) {
        resultList.add(trimmed)
      }
    }

    return resultList.joinToString("\n\n")
  }

  private fun hasMatchingRule(tagSnippet: String, rules: Map<String, CssStyleRule>, predicate: (CssStyleRule) -> Boolean): Boolean {
    val classAttr = Regex("""class=["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(tagSnippet)?.groupValues?.get(1) ?: return false
    val classes = classAttr.split(Regex("""\s+"""))
    return classes.any { c ->
      val rule = rules[c.lowercase()]
      rule != null && predicate(rule)
    }
  }
}
