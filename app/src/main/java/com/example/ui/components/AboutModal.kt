package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.BuildConfig
import com.example.data.remote.CloudArchiveSyncService
import com.example.ui.theme.AntiqueGold
import com.example.ui.theme.AntiqueGoldLight
import com.example.ui.theme.CharcoalSecondary
import com.example.ui.theme.CharcoalTertiary
import com.example.ui.theme.CharcoalText
import com.example.ui.theme.CreamBackground
import com.example.ui.theme.DeepBurgundy
import com.example.ui.theme.SoftCreamPaper
import com.example.ui.theme.SubtleBorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Update check outcome states
 */
sealed class UpdateStatus {
  object Idle : UpdateStatus()
  object Checking : UpdateStatus()
  data class UpdateAvailable(
    val latestVersion: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val apkDownloadUrl: String?,
    val releasePageUrl: String,
    val publishedDate: String
  ) : UpdateStatus()
  data class UpToDate(
    val checkedVersion: String,
    val checkedTimestamp: String
  ) : UpdateStatus()
  data class NoReleasesFound(
    val repository: String,
    val message: String
  ) : UpdateStatus()
  data class Error(
    val errorMessage: String
  ) : UpdateStatus()
}

@Composable
fun AboutModal(
  novelsCount: Int = 0,
  isCloudSyncing: Boolean = false,
  lastCloudSyncTime: Long = 0L,
  cloudSyncedNovelsCount: Int = 0,
  cloudReadUrl: String = "",
  cloudWriteUrl: String = "",
  gitHubToken: String = "",
  onSyncCloudArchive: () -> Unit = {},
  onSaveCloudSettings: (readUrl: String, writeUrl: String, token: String) -> Unit = { _, _, _ -> },
  onPublishUpdateManifest: (versionName: String, versionCode: Int, title: String, changelog: String, apkUrl: String, releasePageUrl: String, onComplete: (Result<String>) -> Unit) -> Unit = { _, _, _, _, _, _, _ -> },
  onCreateGitHubReleaseTag: (tagName: String, releaseTitle: String, releaseNotes: String, targetBranch: String, isDraft: Boolean, alsoUpdateManifest: Boolean, versionCode: Int, onComplete: (Result<CloudArchiveSyncService.GitHubReleaseTagResult>) -> Unit) -> Unit = { _, _, _, _, _, _, _, _ -> },
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  // App version information
  val currentVersionName = BuildConfig.VERSION_NAME.ifBlank { "1.0.1" }
  val currentVersionCode = BuildConfig.VERSION_CODE

  // Default update endpoint from repository
  val defaultJsonUrl = "https://raw.githubusercontent.com/clarifyrgb/strawberrycandy/refs/heads/main/update.json"
  val sharedPrefs = remember { context.getSharedPreferences("strawberrycandy_prefs", Context.MODE_PRIVATE) }
  var updateJsonUrl by remember {
    mutableStateOf(sharedPrefs.getString("update_json_url", defaultJsonUrl) ?: defaultJsonUrl)
  }
  var isEditingUrl by remember { mutableStateOf(false) }
  var urlInput by remember { mutableStateOf(updateJsonUrl) }

  // Update check status
  var updateStatus by remember { mutableStateOf<UpdateStatus>(UpdateStatus.Idle) }

  // Selected tab for deep details
  var selectedTab by remember { mutableStateOf(0) } // 0: App & Update, 1: Capabilities, 2: System Info

  fun triggerUpdateCheck() {
    updateStatus = UpdateStatus.Checking
    coroutineScope.launch {
      val result = fetchAppUpdate(updateJsonUrl, currentVersionName, currentVersionCode)
      updateStatus = result
    }
  }

  fun simulateUpdateAvailable() {
    updateStatus = UpdateStatus.UpdateAvailable(
      latestVersion = "1.0.2 (Build 3)",
      releaseTitle = "Strawberrycandy v1.0.2 - Live Update",
      releaseNotes = "• Instant APK update detection with direct browser download\n• Preserved signing keys: Update installs seamlessly over your existing app without data loss\n• Global Cloud Archive manuscript sync\n• Reading mode typography & smooth scroll optimizations",
      apkDownloadUrl = "https://github.com/clarifyrgb/strawberrycandy/releases/download/v1.0.2/Strawberrycandy.apk",
      releasePageUrl = "https://github.com/clarifyrgb/strawberrycandy/releases",
      publishedDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    )
  }

  // Auto-check for updates on modal open
  LaunchedEffect(Unit) {
    triggerUpdateCheck()
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth(0.94f)
        .widthIn(max = 560.dp)
        .fillMaxHeight(0.92f)
        .padding(vertical = 16.dp)
        .testTag("about_strawberrycandy_modal"),
      shape = RoundedCornerShape(20.dp),
      color = CreamBackground,
      border = BorderStroke(1.dp, SubtleBorder),
      shadowElevation = 14.dp
    ) {
      Column(
        modifier = Modifier.fillMaxHeight()
      ) {
        // -------------------------------------------------------------
        // HEADER: BRAND EMBLEM & TITLE
        // -------------------------------------------------------------
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(SoftCreamPaper)
            .padding(horizontal = 20.dp, vertical = 14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically
          ) {
            // App Emblem
            Surface(
              modifier = Modifier.size(40.dp),
              shape = RoundedCornerShape(10.dp),
              color = AntiqueGold.copy(alpha = 0.12f),
              border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.4f))
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text(
                  text = "❦",
                  fontSize = 20.sp,
                  color = AntiqueGold
                )
              }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
              Text(
                text = "STRAWBERRYCANDY",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontFamily = FontFamily.Serif,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 1.4.sp,
                  fontSize = 15.sp
                ),
                color = CharcoalText
              )
              Text(
                text = "LITERARY ARCHIVE & TRANSLATION STUDIO",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 8.sp,
                  fontWeight = FontWeight.SemiBold,
                  letterSpacing = 1.2.sp,
                  color = AntiqueGold
                )
              )
            }
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier
              .size(36.dp)
              .testTag("about_modal_close_button")
          ) {
            Icon(
              imageVector = Icons.Outlined.Close,
              contentDescription = "Close About",
              tint = CharcoalSecondary,
              modifier = Modifier.size(20.dp)
            )
          }
        }

        HorizontalDivider(color = SubtleBorder, thickness = 1.dp)

        // -------------------------------------------------------------
        // NAVIGATION SEGMENT TABS
        // -------------------------------------------------------------
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(SoftCreamPaper.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          val tabs = listOf("Version & Updates", "Features", "System Diagnostics")
          tabs.forEachIndexed { index, title ->
            val isSelected = selectedTab == index
            Surface(
              modifier = Modifier
                .weight(1f)
                .clickable { selectedTab = index }
                .testTag("about_tab_$index"),
              shape = RoundedCornerShape(10.dp),
              color = if (isSelected) AntiqueGold else Color.Transparent,
              border = if (isSelected) null else BorderStroke(1.dp, SubtleBorder.copy(alpha = 0.6f))
            ) {
              Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 10.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (isSelected) Color.White else CharcoalSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 6.dp)
              )
            }
          }
        }

        // -------------------------------------------------------------
        // SCROLLABLE CONTENT BODY
        // -------------------------------------------------------------
        Column(
          modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          when (selectedTab) {
            0 -> {
              // TAB 0: VERSION & APK UPDATE CHECKER
              VersionOverviewCard(
                versionName = currentVersionName,
                versionCode = currentVersionCode
              )

              UpdateCheckerCard(
                currentVersion = currentVersionName,
                updateJsonUrl = updateJsonUrl,
                isEditingUrl = isEditingUrl,
                urlInput = urlInput,
                onUrlInputChange = { urlInput = it },
                onSaveUrl = {
                  val cleaned = urlInput.trim()
                  updateJsonUrl = cleaned
                  sharedPrefs.edit().putString("update_json_url", cleaned).apply()
                  isEditingUrl = false
                },
                onToggleEditUrl = {
                  isEditingUrl = !isEditingUrl
                  if (isEditingUrl) urlInput = updateJsonUrl
                },
                updateStatus = updateStatus,
                onCheckUpdates = { triggerUpdateCheck() },
                onSimulateUpdate = { simulateUpdateAvailable() },
                onOpenReleaseManager = { selectedTab = 2 }
              )
            }
            1 -> {
              // TAB 1: FEATURES & ARCHITECTURE
              FeaturesShowcaseCard(novelsCount = novelsCount)
            }
            2 -> {
              // TAB 2: SYSTEM & DEVICE DIAGNOSTICS & GLOBAL CLOUD ARCHIVE
              GlobalCloudArchiveCard(
                isCloudSyncing = isCloudSyncing,
                lastSyncTime = lastCloudSyncTime,
                cloudSyncedCount = cloudSyncedNovelsCount,
                initialReadUrl = cloudReadUrl,
                initialWriteUrl = cloudWriteUrl,
                initialGitHubToken = gitHubToken,
                onSyncNow = onSyncCloudArchive,
                onSaveSettings = onSaveCloudSettings
              )
              Spacer(modifier = Modifier.height(12.dp))
              GitHubReleaseAndTagManagerCard(
                currentVersionName = currentVersionName,
                currentVersionCode = currentVersionCode,
                gitHubToken = gitHubToken,
                onPublishUpdateManifest = onPublishUpdateManifest,
                onCreateGitHubReleaseTag = onCreateGitHubReleaseTag
              )
              Spacer(modifier = Modifier.height(12.dp))
              SystemDiagnosticsCard(
                currentVersion = currentVersionName,
                buildNumber = currentVersionCode,
                novelsCount = novelsCount
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Colophon
          Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = "— ❦ —",
              color = AntiqueGold.copy(alpha = 0.6f),
              fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Crafted for readers, translators, and curators of fine literature.",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontFamily = FontFamily.Serif
              ),
              color = CharcoalTertiary,
              textAlign = TextAlign.Center
            )
          }
        }
      }
    }
  }
}

// ---------------------------------------------------------------------
// 1. CURRENT VERSION OVERVIEW CARD
// ---------------------------------------------------------------------
@Composable
private fun VersionOverviewCard(
  versionName: String,
  versionCode: Int
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("version_overview_card"),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = SoftCreamPaper),
    border = BorderStroke(1.dp, SubtleBorder)
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "CURRENT APPLICATION VERSION",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 8.5.sp,
              letterSpacing = 1.2.sp,
              fontWeight = FontWeight.Bold
            ),
            color = AntiqueGold
          )
          Row(
            verticalAlignment = Alignment.Bottom
          ) {
            Text(
              text = "v$versionName",
              style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold
              ),
              color = CharcoalText
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "(Build $versionCode)",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
              color = CharcoalSecondary
            )
          }
        }

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = AntiqueGold.copy(alpha = 0.12f),
          border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.3f))
        ) {
          Text(
            text = "OFFICIAL APK",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 8.5.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.8.sp
            ),
            color = AntiqueGold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }
    }
  }
}

// ---------------------------------------------------------------------
// 2. INTERACTIVE APK UPDATE CHECKER CARD
// ---------------------------------------------------------------------
@Composable
private fun UpdateCheckerCard(
  currentVersion: String,
  updateJsonUrl: String,
  isEditingUrl: Boolean,
  urlInput: String,
  onUrlInputChange: (String) -> Unit,
  onSaveUrl: () -> Unit,
  onToggleEditUrl: () -> Unit,
  updateStatus: UpdateStatus,
  onCheckUpdates: () -> Unit,
  onSimulateUpdate: () -> Unit,
  onOpenReleaseManager: () -> Unit = {}
) {
  val context = LocalContext.current

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .animateContentSize()
      .testTag("update_checker_card"),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = SoftCreamPaper),
    border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.4f))
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Outlined.SystemUpdate,
            contentDescription = null,
            tint = AntiqueGold,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "APK UPDATE CHECKER",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 9.sp,
              letterSpacing = 1.2.sp,
              fontWeight = FontWeight.Bold
            ),
            color = AntiqueGold
          )
        }

        // Edit URL toggle
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable { onToggleEditUrl() }
            .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
          Icon(
            imageVector = if (isEditingUrl) Icons.Outlined.Check else Icons.Outlined.Edit,
            contentDescription = "Edit Update URL",
            tint = AntiqueGold,
            modifier = Modifier.size(12.dp)
          )
          Spacer(modifier = Modifier.width(3.dp))
          Text(
            text = if (isEditingUrl) "Done" else "Edit URL",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 9.sp,
              fontWeight = FontWeight.SemiBold
            ),
            color = AntiqueGold
          )
        }
      }

      // Update Endpoint Indicator / Editor
      if (isEditingUrl) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = "Update Manifest JSON URL:",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = CharcoalSecondary
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            OutlinedTextField(
              value = urlInput,
              onValueChange = onUrlInputChange,
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("update_json_url_input"),
              textStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
              placeholder = { Text("https://raw.githubusercontent.com/.../update.json", fontSize = 10.sp) },
              singleLine = true,
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AntiqueGold,
                unfocusedBorderColor = SubtleBorder
              ),
              shape = RoundedCornerShape(8.dp)
            )
            Button(
              onClick = onSaveUrl,
              shape = RoundedCornerShape(8.dp),
              colors = ButtonDefaults.buttonColors(containerColor = AntiqueGold),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
              Text("Save", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      } else {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.7f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            Text(
              text = "Endpoint:",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
              color = CharcoalTertiary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = updateJsonUrl.substringAfter("raw.githubusercontent.com/").ifBlank { updateJsonUrl },
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
              ),
              color = CharcoalText,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }

          IconButton(
            onClick = {
              val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(updateJsonUrl))
              try { context.startActivity(browserIntent) } catch (_: Exception) {}
            },
            modifier = Modifier.size(24.dp)
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
              contentDescription = "Open update.json URL",
              tint = CharcoalSecondary,
              modifier = Modifier.size(13.dp)
            )
          }
        }
      }

      // ---------------------------------------------------------------
      // DYNAMIC UPDATE STATUS DISPLAY
      // ---------------------------------------------------------------
      when (updateStatus) {
        is UpdateStatus.Idle -> {
          Text(
            text = "Check whether a newer APK release with bug fixes, new features, or reader enhancements is published on GitHub.",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
            color = CharcoalSecondary
          )
        }
        is UpdateStatus.Checking -> {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(AntiqueGoldLight.copy(alpha = 0.5f))
              .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            CircularProgressIndicator(
              modifier = Modifier.size(16.dp),
              color = AntiqueGold,
              strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "Querying GitHub Releases API...",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
              ),
              color = AntiqueGold
            )
          }
        }
        is UpdateStatus.UpdateAvailable -> {
          val update = updateStatus
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(Color(0xFFE8F5E9))
              .border(1.dp, Color(0xFFA5D6A7), RoundedCornerShape(10.dp))
              .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.NewReleases,
                contentDescription = null,
                tint = Color(0xFF2E7D32),
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "New APK Update Available: v${update.latestVersion}!",
                style = MaterialTheme.typography.titleSmall.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp
                ),
                color = Color(0xFF1B5E20)
              )
            }

            if (update.releaseTitle.isNotBlank()) {
              Text(
                text = update.releaseTitle,
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 10.5.sp,
                  fontWeight = FontWeight.SemiBold
                ),
                color = CharcoalText
              )
            }

            if (update.publishedDate.isNotBlank()) {
              Text(
                text = "Published: ${update.publishedDate}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = CharcoalTertiary
              )
            }

            if (update.releaseNotes.isNotBlank()) {
              Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                color = Color.White.copy(alpha = 0.8f)
              ) {
                Text(
                  text = update.releaseNotes.take(280) + if (update.releaseNotes.length > 280) "..." else "",
                  style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                  ),
                  color = CharcoalSecondary,
                  modifier = Modifier.padding(8.dp)
                )
              }
            }

            // Direct Download & Release Action Buttons
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              if (update.apkDownloadUrl != null) {
                Button(
                  onClick = {
                    val downloadIntent = Intent(Intent.ACTION_VIEW, Uri.parse(update.apkDownloadUrl))
                    try { context.startActivity(downloadIntent) } catch (_: Exception) {}
                  },
                  modifier = Modifier
                    .weight(1.2f)
                    .testTag("download_apk_button"),
                  shape = RoundedCornerShape(8.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Download,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "Download APK",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                  )
                }

                IconButton(
                  onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    val clip = ClipData.newPlainText("Strawberrycandy APK URL", update.apkDownloadUrl)
                    clipboard?.setPrimaryClip(clip)
                    Toast.makeText(context, "APK download link copied to clipboard!", Toast.LENGTH_SHORT).show()
                  },
                  modifier = Modifier
                    .size(38.dp)
                    .border(1.dp, Color(0xFF2E7D32).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                ) {
                  Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = "Copy APK Link",
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(16.dp)
                  )
                }
              }

              OutlinedButton(
                onClick = {
                  val releaseIntent = Intent(Intent.ACTION_VIEW, Uri.parse(update.releasePageUrl))
                  try { context.startActivity(releaseIntent) } catch (_: Exception) {}
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF2E7D32))
              ) {
                Text(
                  text = "View Release",
                  fontSize = 11.sp,
                  color = Color(0xFF2E7D32)
                )
              }
            }

            // Seamless update hint
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = Color(0xFFE8F5E9).copy(alpha = 0.6f),
              border = BorderStroke(0.6.dp, Color(0xFF81C784).copy(alpha = 0.4f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = "✨ In-Place Upgrade: Install this APK directly over your current app. All books, bookmarks, reader settings, and progress will be preserved automatically.",
                fontSize = 9.sp,
                color = Color(0xFF1B5E20),
                lineHeight = 13.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
              )
            }
          }
        }
        is UpdateStatus.UpToDate -> {
          val upToDate = updateStatus
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(Color(0xFFF1F8E9))
              .border(1.dp, Color(0xFFC5E1A5), RoundedCornerShape(10.dp))
              .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF388E3C),
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "Strawberrycandy is up to date!",
                  style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                  ),
                  color = Color(0xFF2E7D32)
                )
                Text(
                  text = "v${upToDate.checkedVersion} is the latest release • Checked ${upToDate.checkedTimestamp}",
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                  color = CharcoalSecondary
                )
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.End
            ) {
              OutlinedButton(
                onClick = onSimulateUpdate,
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(0.8.dp, Color(0xFF388E3C).copy(alpha = 0.5f)),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(26.dp)
              ) {
                Text(
                  text = "Preview Update Available UI",
                  fontSize = 9.sp,
                  color = Color(0xFF2E7D32),
                  fontWeight = FontWeight.Medium
                )
              }
            }
          }
        }
        is UpdateStatus.NoReleasesFound -> {
          val noReleases = updateStatus
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(SoftCreamPaper)
              .border(1.dp, SubtleBorder, RoundedCornerShape(10.dp))
              .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = AntiqueGold,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "No Releases Found Yet",
                style = MaterialTheme.typography.labelMedium.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 11.5.sp
                ),
                color = CharcoalText
              )
            }
            Text(
              text = noReleases.message,
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
              color = CharcoalSecondary
            )
            Text(
              text = "When your GitHub Actions 'release.yml' completes, releases tagged here will automatically be recognized.",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.5.sp,
                fontFamily = FontFamily.Serif
              ),
              color = AntiqueGold
            )
          }
        }
        is UpdateStatus.Error -> {
          val err = updateStatus
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(Color(0xFFFFEBEE))
              .border(1.dp, Color(0xFFFFCDD2), RoundedCornerShape(10.dp))
              .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text(
              text = "Could not check updates",
              style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color(0xFFC62828),
                fontSize = 11.5.sp
              )
            )
            Text(
              text = err.errorMessage,
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
              color = CharcoalSecondary
            )
          }
        }
      }

      // Check Button
      Button(
        onClick = onCheckUpdates,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("check_for_updates_button"),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AntiqueGold),
        enabled = updateStatus !is UpdateStatus.Checking
      ) {
        Icon(
          imageVector = Icons.Outlined.Refresh,
          contentDescription = null,
          tint = Color.White,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (updateStatus is UpdateStatus.Checking) "Checking GitHub..." else "Check for Updates",
          style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
          )
        )
      }

      OutlinedButton(
        onClick = onOpenReleaseManager,
        modifier = Modifier
          .fillMaxWidth()
          .height(36.dp)
          .testTag("create_release_tag_quick_button"),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.6f))
      ) {
        Icon(
          imageVector = Icons.Outlined.Upload,
          contentDescription = null,
          tint = AntiqueGold,
          modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "🏷️ Create GitHub Release Tag (v$currentVersion or Future)",
          style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.5.sp
          ),
          color = AntiqueGold
        )
      }
    }
  }
}

// ---------------------------------------------------------------------
// 3. FEATURES & ARCHITECTURE CARD
// ---------------------------------------------------------------------
@Composable
private fun FeaturesShowcaseCard(novelsCount: Int) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("features_showcase_card"),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = SoftCreamPaper),
    border = BorderStroke(1.dp, SubtleBorder)
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      Text(
        text = "LITERARY PLATFORM CAPABILITIES",
        style = MaterialTheme.typography.labelSmall.copy(
          fontSize = 8.5.sp,
          letterSpacing = 1.2.sp,
          fontWeight = FontWeight.Bold
        ),
        color = AntiqueGold
      )

      FeatureItem(
        icon = Icons.Outlined.MenuBook,
        title = "Continuous Vertical Reading Canvas",
        description = "Unencumbered, pure vertical scroll with zero layout shifts. Custom serif typography, line-height scaling, and folio tracking."
      )

      FeatureItem(
        icon = Icons.Outlined.Storage,
        title = "Unlimited Novels & Offline Persistence",
        description = "Readers can read unlimited novels posted by translators. Currently holding $novelsCount live manuscripts, with infinite capacity for new novels, chapters, bookmarks, and reader marginalia in Room SQLite."
      )

      FeatureItem(
        icon = Icons.Outlined.Edit,
        title = "Translator Studio & Author Rooms",
        description = "Collaborative multi-slot translation archives with rich text chapter authoring, Markdown support, and custom curator pen names."
      )

      FeatureItem(
        icon = Icons.Outlined.History,
        title = "Community Reader Reflections",
        description = "Threaded per-chapter reader commentaries and sentence-level marginalia notes without disruptive popups."
      )
    }
  }
}

@Composable
private fun FeatureItem(
  icon: ImageVector,
  title: String,
  description: String
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.Top
  ) {
    Surface(
      modifier = Modifier.size(32.dp),
      shape = RoundedCornerShape(8.dp),
      color = AntiqueGold.copy(alpha = 0.12f)
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = AntiqueGold,
          modifier = Modifier.size(16.dp)
        )
      }
    }

    Spacer(modifier = Modifier.width(12.dp))

    Column {
      Text(
        text = title,
        style = MaterialTheme.typography.labelMedium.copy(
          fontWeight = FontWeight.SemiBold,
          fontSize = 11.5.sp
        ),
        color = CharcoalText
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = description,
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
        color = CharcoalSecondary,
        lineHeight = 14.sp
      )
    }
  }
}

// ---------------------------------------------------------------------
// 4. GLOBAL CLOUD ARCHIVE CARD
// ---------------------------------------------------------------------
@Composable
private fun GlobalCloudArchiveCard(
  isCloudSyncing: Boolean,
  lastSyncTime: Long,
  cloudSyncedCount: Int,
  initialReadUrl: String,
  initialWriteUrl: String,
  initialGitHubToken: String,
  onSyncNow: () -> Unit,
  onSaveSettings: (readUrl: String, writeUrl: String, token: String) -> Unit,
) {
  var isEditingSettings by remember { mutableStateOf(false) }
  var readUrlInput by remember { mutableStateOf(initialReadUrl) }
  var writeUrlInput by remember { mutableStateOf(initialWriteUrl) }
  var tokenInput by remember { mutableStateOf(initialGitHubToken) }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("global_cloud_archive_card"),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = SoftCreamPaper),
    border = BorderStroke(1.dp, SubtleBorder)
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Outlined.CloudDone,
            contentDescription = null,
            tint = Color(0xFF2E7D32),
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "GLOBAL CLOUD ARCHIVE",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 8.5.sp,
              letterSpacing = 1.2.sp,
              fontWeight = FontWeight.Bold
            ),
            color = AntiqueGold
          )
        }

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = Color(0xFFE8F5E9),
          border = BorderStroke(0.6.dp, Color(0xFF81C784))
        ) {
          Text(
            text = "SHARED LIVE ARCHIVE",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 8.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.5.sp,
              color = Color(0xFF2E7D32)
            ),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
          )
        }
      }

      Text(
        text = "When translators or owners upload a novel, it pushes to the Global Archive so anyone who installed this APK can read the published manuscripts.",
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 15.sp),
        color = CharcoalSecondary
      )

      HorizontalDivider(thickness = 0.6.dp, color = SubtleBorder)

      DiagnosticRow(label = "Sync Architecture", value = "Remote Multi-User Shared Sync")
      DiagnosticRow(
        label = "Cloud Synced Manuscripts",
        value = if (cloudSyncedCount > 0) "$cloudSyncedCount novels synchronized" else "Ready to sync"
      )
      val timeFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
      val timeStr = if (lastSyncTime > 0) timeFormat.format(Date(lastSyncTime)) else "On startup"
      DiagnosticRow(label = "Last Cloud Sync", value = timeStr)
      DiagnosticRow(
        label = "Primary Cloud Endpoint",
        value = "clarifyrgb/strawberrycandy"
      )

      Spacer(modifier = Modifier.height(2.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Button(
          onClick = onSyncNow,
          enabled = !isCloudSyncing,
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.buttonColors(containerColor = CharcoalText),
          modifier = Modifier
            .weight(1f)
            .height(38.dp)
            .testTag("sync_cloud_archive_button")
        ) {
          if (isCloudSyncing) {
            CircularProgressIndicator(
              modifier = Modifier.size(14.dp),
              strokeWidth = 1.5.dp,
              color = SoftCreamPaper
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Syncing...", fontSize = 11.sp, color = SoftCreamPaper)
          } else {
            Icon(
              imageVector = Icons.Outlined.Refresh,
              contentDescription = null,
              modifier = Modifier.size(14.dp),
              tint = SoftCreamPaper
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Sync Now", fontSize = 11.sp, color = SoftCreamPaper)
          }
        }

        OutlinedButton(
          onClick = { isEditingSettings = !isEditingSettings },
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .height(38.dp)
            .testTag("cloud_settings_toggle_button")
        ) {
          Icon(
            imageVector = Icons.Outlined.Edit,
            contentDescription = null,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(if (isEditingSettings) "Hide Config" else "Endpoints", fontSize = 11.sp)
        }
      }

      if (isEditingSettings) {
        Spacer(modifier = Modifier.height(4.dp))
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(10.dp))
            .border(BorderStroke(0.6.dp, SubtleBorder), RoundedCornerShape(10.dp))
            .padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            text = "CLOUD ARCHIVE ENDPOINTS",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 8.5.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp
            ),
            color = CharcoalSecondary
          )

          OutlinedTextField(
            value = readUrlInput,
            onValueChange = { readUrlInput = it },
            label = { Text("Read URL (novels.json)", fontSize = 10.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = DeepBurgundy,
              unfocusedBorderColor = SubtleBorder
            ),
            shape = RoundedCornerShape(8.dp)
          )

          OutlinedTextField(
            value = writeUrlInput,
            onValueChange = { writeUrlInput = it },
            label = { Text("Write Endpoint (Firebase RTDB / Custom REST)", fontSize = 10.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = DeepBurgundy,
              unfocusedBorderColor = SubtleBorder
            ),
            shape = RoundedCornerShape(8.dp)
          )

          OutlinedTextField(
            value = tokenInput,
            onValueChange = { tokenInput = it },
            label = { Text("GitHub Token (PAT with repo scope)", fontSize = 10.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = DeepBurgundy,
              unfocusedBorderColor = SubtleBorder
            ),
            shape = RoundedCornerShape(8.dp)
          )

          Button(
            onClick = {
              onSaveSettings(readUrlInput, writeUrlInput, tokenInput)
              isEditingSettings = false
            },
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AntiqueGold),
            modifier = Modifier
              .fillMaxWidth()
              .height(36.dp)
          ) {
            Text("Save Cloud Endpoints", fontSize = 11.sp, color = CharcoalText, fontWeight = FontWeight.SemiBold)
          }
        }
      }
    }
  }
}

// ---------------------------------------------------------------------
// 4B. GITHUB RELEASE & TAG CREATOR CARD
// ---------------------------------------------------------------------
@Composable
private fun GitHubReleaseAndTagManagerCard(
  currentVersionName: String,
  currentVersionCode: Int,
  gitHubToken: String,
  onPublishUpdateManifest: (versionName: String, versionCode: Int, title: String, changelog: String, apkUrl: String, releasePageUrl: String, onComplete: (Result<String>) -> Unit) -> Unit,
  onCreateGitHubReleaseTag: (tagName: String, releaseTitle: String, releaseNotes: String, targetBranch: String, isDraft: Boolean, alsoUpdateManifest: Boolean, versionCode: Int, onComplete: (Result<CloudArchiveSyncService.GitHubReleaseTagResult>) -> Unit) -> Unit
) {
  val context = LocalContext.current
  var isExpanded by remember { mutableStateOf(false) }

  // Tag & Release details
  var tagNameInput by remember { mutableStateOf("v$currentVersionName") }
  var targetBranchInput by remember { mutableStateOf("main") }
  var versionCodeInput by remember { mutableStateOf((currentVersionCode).toString()) }
  var titleInput by remember { mutableStateOf("Strawberrycandy v$currentVersionName") }
  var changelogInput by remember {
    mutableStateOf(
      "• In-app update system with direct APK download\n• Preserved reading progress & local bookmarks\n• Cloud Archive manuscript synchronization across all devices"
    )
  }
  var alsoUpdateManifest by remember { mutableStateOf(true) }
  var isDraft by remember { mutableStateOf(false) }

  var isCreatingTag by remember { mutableStateOf(false) }
  var isPublishingManifestOnly by remember { mutableStateOf(false) }
  var tagCreationResult by remember { mutableStateOf<CloudArchiveSyncService.GitHubReleaseTagResult?>(null) }
  var manifestOnlyResult by remember { mutableStateOf<String?>(null) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("github_release_tag_manager_card"),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = SoftCreamPaper),
    border = BorderStroke(1.dp, SubtleBorder)
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Outlined.SystemUpdate,
            contentDescription = null,
            tint = AntiqueGold,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "GITHUB RELEASE & TAG CREATOR",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 8.5.sp,
              letterSpacing = 1.2.sp,
              fontWeight = FontWeight.Bold
            ),
            color = AntiqueGold
          )
        }

        OutlinedButton(
          onClick = { isExpanded = !isExpanded },
          shape = RoundedCornerShape(6.dp),
          border = BorderStroke(0.8.dp, AntiqueGold.copy(alpha = 0.5f)),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
          modifier = Modifier.height(26.dp)
        ) {
          Text(
            text = if (isExpanded) "Hide" else "Create Tag / Release",
            fontSize = 9.sp,
            color = AntiqueGold,
            fontWeight = FontWeight.SemiBold
          )
        }
      }

      Text(
        text = "Installed build is v$currentVersionName (Build $currentVersionCode). Create Git tags (e.g. v$currentVersionName, v1.0.2) and release pages on GitHub so all installed APKs can detect and install updates.",
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
        color = CharcoalSecondary,
        lineHeight = 14.sp
      )

      AnimatedVisibility(visible = isExpanded) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Version presets chips
          Text(
            text = "PRESET TAG SUGGESTIONS",
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = CharcoalTertiary,
            letterSpacing = 1.sp
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            val suggestions = listOf(
              "v$currentVersionName" to currentVersionCode,
              "v1.0.2" to (currentVersionCode + 1),
              "v1.1.0" to (currentVersionCode + 2),
              "v2.0.0" to (currentVersionCode + 10)
            )
            suggestions.forEach { (tag, code) ->
              Surface(
                onClick = {
                  tagNameInput = tag
                  versionCodeInput = code.toString()
                  titleInput = "Strawberrycandy $tag"
                },
                shape = RoundedCornerShape(6.dp),
                color = if (tagNameInput == tag) AntiqueGold.copy(alpha = 0.15f) else CreamBackground,
                border = BorderStroke(0.7.dp, if (tagNameInput == tag) AntiqueGold else SubtleBorder)
              ) {
                Text(
                  text = tag,
                  fontSize = 9.5.sp,
                  fontWeight = if (tagNameInput == tag) FontWeight.Bold else FontWeight.Normal,
                  color = if (tagNameInput == tag) AntiqueGold else CharcoalSecondary,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedTextField(
              value = tagNameInput,
              onValueChange = {
                tagNameInput = it
                val clean = if (it.startsWith("v", ignoreCase = true)) it else "v$it"
                titleInput = "Strawberrycandy $clean"
              },
              label = { Text("Git Tag (e.g. v1.0.1)", fontSize = 10.sp) },
              singleLine = true,
              modifier = Modifier.weight(1.2f),
              colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepBurgundy, unfocusedBorderColor = SubtleBorder),
              shape = RoundedCornerShape(8.dp)
            )

            OutlinedTextField(
              value = targetBranchInput,
              onValueChange = { targetBranchInput = it },
              label = { Text("Branch", fontSize = 10.sp) },
              singleLine = true,
              modifier = Modifier.weight(0.8f),
              colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepBurgundy, unfocusedBorderColor = SubtleBorder),
              shape = RoundedCornerShape(8.dp)
            )

            OutlinedTextField(
              value = versionCodeInput,
              onValueChange = { versionCodeInput = it },
              label = { Text("Build Code", fontSize = 10.sp) },
              singleLine = true,
              modifier = Modifier.weight(0.8f),
              colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepBurgundy, unfocusedBorderColor = SubtleBorder),
              shape = RoundedCornerShape(8.dp)
            )
          }

          OutlinedTextField(
            value = titleInput,
            onValueChange = { titleInput = it },
            label = { Text("Release Title", fontSize = 10.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepBurgundy, unfocusedBorderColor = SubtleBorder),
            shape = RoundedCornerShape(8.dp)
          )

          OutlinedTextField(
            value = changelogInput,
            onValueChange = { changelogInput = it },
            label = { Text("Release Notes & Changelog", fontSize = 10.sp) },
            minLines = 3,
            maxLines = 5,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepBurgundy, unfocusedBorderColor = SubtleBorder),
            shape = RoundedCornerShape(8.dp)
          )

          // Options: also update manifest and draft
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Checkbox(
              checked = alsoUpdateManifest,
              onCheckedChange = { alsoUpdateManifest = it },
              colors = CheckboxDefaults.colors(checkedColor = AntiqueGold)
            )
            Text(
              text = "Also update update.json on GitHub so installed APKs get notified",
              fontSize = 9.5.sp,
              color = CharcoalText
            )
          }

          // Result notification
          tagCreationResult?.let { result ->
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = Color(0xFFE8F5E9),
              border = BorderStroke(1.dp, Color(0xFF81C784)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = result.message,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20)
                  )
                }
                Text(
                  text = "Release Tag URL: ${result.htmlUrl}",
                  fontSize = 9.sp,
                  color = Color(0xFF2E7D32)
                )

                Row(
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  modifier = Modifier.padding(top = 2.dp)
                ) {
                  OutlinedButton(
                    onClick = {
                      val intent = Intent(Intent.ACTION_VIEW, Uri.parse(result.htmlUrl))
                      context.startActivity(intent)
                    },
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(0.8.dp, Color(0xFF2E7D32)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(26.dp)
                  ) {
                    Icon(
                      imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                      contentDescription = null,
                      modifier = Modifier.size(12.dp),
                      tint = Color(0xFF2E7D32)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Open on GitHub", fontSize = 9.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.SemiBold)
                  }

                  OutlinedButton(
                    onClick = {
                      val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                      val clip = ClipData.newPlainText("Release URL", result.htmlUrl)
                      clipboard?.setPrimaryClip(clip)
                      Toast.makeText(context, "URL copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(0.8.dp, SubtleBorder),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(26.dp)
                  ) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp), tint = CharcoalText)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy Link", fontSize = 9.sp, color = CharcoalText)
                  }
                }

                Text(
                  text = "📦 Next step: Export the APK from AI Studio settings (Export / Download APK), then open the release page above and attach 'Strawberrycandy.apk' as a release binary.",
                  fontSize = 8.5.sp,
                  color = Color(0xFF33691E),
                  lineHeight = 12.sp
                )
              }
            }
          }

          manifestOnlyResult?.let { msg ->
            Text(
              text = msg,
              fontSize = 10.sp,
              color = if (msg.startsWith("✨")) Color(0xFF2E7D32) else Color(0xFFC2185B),
              lineHeight = 13.sp
            )
          }

          errorMessage?.let { err ->
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = Color(0xFFFFEBEE),
              border = BorderStroke(1.dp, Color(0xFFEF9A9A)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.Info,
                  contentDescription = null,
                  tint = Color(0xFFC2185B),
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = err,
                  fontSize = 9.5.sp,
                  color = Color(0xFFB71C1C),
                  lineHeight = 13.sp
                )
              }
            }
          }

          // Action buttons: Create Tag on GitHub, Open in Web, Copy JSON
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = {
                val code = versionCodeInput.toIntOrNull() ?: currentVersionCode
                isCreatingTag = true
                tagCreationResult = null
                errorMessage = null
                onCreateGitHubReleaseTag(
                  tagNameInput.trim(),
                  titleInput.trim(),
                  changelogInput.trim(),
                  targetBranchInput.trim(),
                  isDraft,
                  alsoUpdateManifest,
                  code
                ) { res ->
                  isCreatingTag = false
                  if (res.isSuccess) {
                    tagCreationResult = res.getOrNull()
                  } else {
                    errorMessage = res.exceptionOrNull()?.localizedMessage ?: "Failed to create tag on GitHub"
                  }
                }
              },
              enabled = !isCreatingTag,
              shape = RoundedCornerShape(8.dp),
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
              modifier = Modifier
                .weight(1.3f)
                .height(38.dp)
            ) {
              if (isCreatingTag) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color.White)
              } else {
                Icon(Icons.Outlined.Upload, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Create Tag on GitHub", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
              }
            }

            OutlinedButton(
              onClick = {
                try {
                  val cleanTag = if (tagNameInput.trim().startsWith("v", ignoreCase = true)) tagNameInput.trim() else "v${tagNameInput.trim()}"
                  val encodedTitle = URLEncoder.encode(titleInput.trim(), "UTF-8")
                  val encodedBody = URLEncoder.encode(changelogInput.trim(), "UTF-8")
                  val webUrl = "https://github.com/clarifyrgb/strawberrycandy/releases/new?tag=$cleanTag&title=$encodedTitle&body=$encodedBody&target=${targetBranchInput.trim()}"
                  val intent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl))
                  context.startActivity(intent)
                } catch (e: Exception) {
                  Toast.makeText(context, "Could not open browser: ${e.message}", Toast.LENGTH_SHORT).show()
                }
              },
              shape = RoundedCornerShape(8.dp),
              border = BorderStroke(1.dp, SubtleBorder),
              modifier = Modifier
                .weight(1f)
                .height(38.dp)
            ) {
              Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(13.dp), tint = CharcoalText)
              Spacer(modifier = Modifier.width(4.dp))
              Text("Open Web", fontSize = 10.5.sp, color = CharcoalText)
            }
          }

          // Secondary row: Update update.json only & Copy JSON
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedButton(
              onClick = {
                val code = versionCodeInput.toIntOrNull() ?: currentVersionCode
                val cleanTag = if (tagNameInput.trim().startsWith("v", ignoreCase = true)) tagNameInput.trim() else "v${tagNameInput.trim()}"
                val cleanVer = cleanTag.removePrefix("v").removePrefix("V")
                val apkUrl = "https://github.com/clarifyrgb/strawberrycandy/releases/download/$cleanTag/Strawberrycandy.apk"
                val releaseUrl = "https://github.com/clarifyrgb/strawberrycandy/releases/tag/$cleanTag"
                isPublishingManifestOnly = true
                manifestOnlyResult = null
                onPublishUpdateManifest(
                  cleanVer,
                  code,
                  titleInput.trim(),
                  changelogInput.trim(),
                  apkUrl,
                  releaseUrl
                ) { result ->
                  isPublishingManifestOnly = false
                  manifestOnlyResult = if (result.isSuccess) {
                    result.getOrNull() ?: "✨ update.json published to GitHub!"
                  } else {
                    result.exceptionOrNull()?.localizedMessage ?: "Failed to publish manifest"
                  }
                }
              },
              enabled = !isPublishingManifestOnly,
              shape = RoundedCornerShape(8.dp),
              border = BorderStroke(1.dp, SubtleBorder),
              modifier = Modifier
                .weight(1f)
                .height(34.dp)
            ) {
              if (isPublishingManifestOnly) {
                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = CharcoalText)
              } else {
                Text("Publish update.json Only", fontSize = 10.sp, color = CharcoalText)
              }
            }

            OutlinedButton(
              onClick = {
                val cleanTag = if (tagNameInput.trim().startsWith("v", ignoreCase = true)) tagNameInput.trim() else "v${tagNameInput.trim()}"
                val cleanVer = cleanTag.removePrefix("v").removePrefix("V")
                val apkUrl = "https://github.com/clarifyrgb/strawberrycandy/releases/download/$cleanTag/Strawberrycandy.apk"
                val releaseUrl = "https://github.com/clarifyrgb/strawberrycandy/releases/tag/$cleanTag"
                val json = """
{
  "versionName": "$cleanVer",
  "versionCode": ${versionCodeInput.toIntOrNull() ?: currentVersionCode},
  "title": "${titleInput.trim()}",
  "changelog": "${changelogInput.trim().replace("\n", "\\n")}",
  "apkUrl": "$apkUrl",
  "releasePageUrl": "$releaseUrl",
  "publishedDate": "${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())}"
}
                """.trimIndent()
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                val clip = ClipData.newPlainText("update.json", json)
                clipboard?.setPrimaryClip(clip)
                Toast.makeText(context, "update.json copied to clipboard!", Toast.LENGTH_SHORT).show()
              },
              shape = RoundedCornerShape(8.dp),
              border = BorderStroke(1.dp, SubtleBorder),
              modifier = Modifier
                .weight(0.7f)
                .height(34.dp)
            ) {
              Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(13.dp), tint = CharcoalText)
              Spacer(modifier = Modifier.width(4.dp))
              Text("Copy JSON", fontSize = 10.sp, color = CharcoalText)
            }
          }
        }
      }
    }
  }
}

// ---------------------------------------------------------------------
// 5. SYSTEM & DEVICE DIAGNOSTICS CARD
// ---------------------------------------------------------------------
@Composable
private fun SystemDiagnosticsCard(
  currentVersion: String,
  buildNumber: Int,
  novelsCount: Int
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("system_diagnostics_card"),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = SoftCreamPaper),
    border = BorderStroke(1.dp, SubtleBorder)
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Text(
        text = "SYSTEM & RUNTIME DIAGNOSTICS",
        style = MaterialTheme.typography.labelSmall.copy(
          fontSize = 8.5.sp,
          letterSpacing = 1.2.sp,
          fontWeight = FontWeight.Bold
        ),
        color = AntiqueGold
      )

      DiagnosticRow(label = "Application Version", value = "v$currentVersion (Build $buildNumber)")
      DiagnosticRow(label = "Archive Novel Capacity", value = "Unlimited (No Cap)")
      DiagnosticRow(label = "Currently Loaded Novels", value = "$novelsCount books available now")
      DiagnosticRow(label = "Android Operating System", value = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
      DiagnosticRow(label = "Device Hardware", value = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}")
      DiagnosticRow(label = "Local Storage Engine", value = "Room Database (SQLite 3.x)")
      DiagnosticRow(label = "Reading & Publishing Limit", value = "None • Infinite Novels Supported")
      DiagnosticRow(label = "UI Toolkit", value = "Jetpack Compose (Material 3)")
      DiagnosticRow(label = "Security / Sandboxing", value = "App Sandboxed • Offline Safe")
    }
  }
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
      color = CharcoalTertiary
    )
    Text(
      text = value,
      style = MaterialTheme.typography.labelSmall.copy(
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium
      ),
      color = CharcoalText,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}

// ---------------------------------------------------------------------
// NETWORK QUERY: FETCH UPDATE.JSON AND GITHUB RELEASES
// ---------------------------------------------------------------------
suspend fun fetchAppUpdate(
  jsonUrl: String,
  currentVersionName: String,
  currentVersionCode: Int
): UpdateStatus = withContext(Dispatchers.IO) {
  var connection: HttpURLConnection? = null

  try {
    val rawUrl = jsonUrl.trim()
    val fullUrl = if (rawUrl.contains("raw.githubusercontent.com") && !rawUrl.contains("?")) {
      "$rawUrl?t=${System.currentTimeMillis()}"
    } else {
      rawUrl
    }
    val url = URL(fullUrl)
    connection = (url.openConnection() as HttpURLConnection).apply {
      requestMethod = "GET"
      connectTimeout = 8000
      readTimeout = 8000
      useCaches = false
      setRequestProperty("Accept", "application/json, text/plain, */*")
      setRequestProperty("User-Agent", "Strawberrycandy-App-Update-Checker")
    }

    val responseCode = connection.responseCode

    if (responseCode in 200..299) {
      val responseText = connection.inputStream.bufferedReader().use { it.readText() }.trim()

      if (responseText.isNotEmpty() && responseText.startsWith("{") && responseText.endsWith("}")) {
        val json = JSONObject(responseText)

        val remoteVersionName = json.optString("versionName", json.optString("version", "")).trim()
        val remoteVersionCode = json.optInt("versionCode", json.optInt("build", json.optInt("buildNumber", 0)))
        val title = json.optString("title", json.optString("name", "Strawberrycandy $remoteVersionName"))
        val changelog = json.optString("changelog", json.optString("notes", json.optString("description", "")))
        val apkUrl = json.optString("apkUrl", json.optString("downloadUrl", json.optString("url", null)))
        val releasePageUrl = json.optString("releasePageUrl", json.optString("githubUrl", "https://github.com/clarifyrgb/strawberrycandy/releases"))
        val publishedDate = json.optString("publishedDate", json.optString("date", ""))

        val hasNewCode = remoteVersionCode > currentVersionCode
        val hasNewName = isVersionNewer(remoteVersionName, currentVersionName)
        val nowTimestamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

        if (hasNewCode || hasNewName) {
          val displayVersion = if (remoteVersionCode > 0 && remoteVersionName.isNotBlank()) {
            "$remoteVersionName (Build $remoteVersionCode)"
          } else if (remoteVersionName.isNotBlank()) {
            remoteVersionName
          } else {
            "Build $remoteVersionCode"
          }

          return@withContext UpdateStatus.UpdateAvailable(
            latestVersion = displayVersion,
            releaseTitle = title,
            releaseNotes = changelog,
            apkDownloadUrl = apkUrl,
            releasePageUrl = releasePageUrl,
            publishedDate = publishedDate
          )
        } else if (remoteVersionName.isNotBlank() || remoteVersionCode > 0) {
          return@withContext UpdateStatus.UpToDate(
            checkedVersion = remoteVersionName.ifBlank { currentVersionName },
            checkedTimestamp = nowTimestamp
          )
        }
      }
    }

    // If update.json is empty or not yet published, fall back to GitHub Releases API
    queryGitHubLatestRelease("clarifyrgb/strawberrycandy", currentVersionName)
  } catch (e: Exception) {
    // Fall back to GitHub Releases API if raw json fails
    try {
      queryGitHubLatestRelease("clarifyrgb/strawberrycandy", currentVersionName)
    } catch (_: Exception) {
      UpdateStatus.Error("Error checking update: ${e.localizedMessage ?: e.javaClass.simpleName}")
    }
  } finally {
    connection?.disconnect()
  }
}

suspend fun queryGitHubLatestRelease(repo: String, currentVersion: String): UpdateStatus = withContext(Dispatchers.IO) {
  val cleanRepo = repo.trim().removePrefix("https://github.com/").removePrefix("github.com/").trimEnd('/')
  if (!cleanRepo.contains("/")) {
    return@withContext UpdateStatus.Error("Invalid GitHub repository format. Use 'owner/repository'.")
  }

  val apiUrl = "https://api.github.com/repos/$cleanRepo/releases/latest"
  var connection: HttpURLConnection? = null

  try {
    val url = URL(apiUrl)
    connection = (url.openConnection() as HttpURLConnection).apply {
      requestMethod = "GET"
      connectTimeout = 8000
      readTimeout = 8000
      setRequestProperty("Accept", "application/vnd.github.v3+json")
      setRequestProperty("User-Agent", "Strawberrycandy-App-Update-Checker")
    }

    val responseCode = connection.responseCode
    if (responseCode == 404) {
      return@withContext UpdateStatus.NoReleasesFound(
        repository = cleanRepo,
        message = "No releases found for '$cleanRepo'. Releases will appear here once your release workflow generates an APK tag."
      )
    }

    if (responseCode !in 200..299) {
      val errorStream = connection.errorStream
      val errBody = errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
      val errMsg = try {
        JSONObject(errBody).optString("message", "HTTP Error $responseCode")
      } catch (_: Exception) {
        "HTTP Error $responseCode"
      }
      return@withContext UpdateStatus.Error("GitHub API returned: $errMsg")
    }

    val responseText = connection.inputStream.bufferedReader().use { it.readText() }
    val json = JSONObject(responseText)

    val tagName = json.optString("tag_name", "")
    val remoteVersion = tagName.removePrefix("v").trim()
    val releaseTitle = json.optString("name", "Release $tagName")
    val releaseNotes = json.optString("body", "")
    val htmlUrl = json.optString("html_url", "https://github.com/$cleanRepo/releases")
    val publishedAtRaw = json.optString("published_at", "")

    val formattedDate = try {
      if (publishedAtRaw.isNotBlank()) {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        val date = parser.parse(publishedAtRaw)
        if (date != null) {
          SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
        } else publishedAtRaw.take(10)
      } else ""
    } catch (_: Exception) {
      publishedAtRaw.take(10)
    }

    // Look for APK download asset
    var apkDownloadUrl: String? = null
    val assetsArray = json.optJSONArray("assets")
    if (assetsArray != null) {
      for (i in 0 until assetsArray.length()) {
        val assetObj = assetsArray.getJSONObject(i)
        val name = assetObj.optString("name", "")
        if (name.endsWith(".apk", ignoreCase = true)) {
          apkDownloadUrl = assetObj.optString("browser_download_url", null)
          break
        }
      }
    }

    val hasNewerVersion = isVersionNewer(remoteVersion, currentVersion)
    val nowTimestamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

    if (hasNewerVersion) {
      UpdateStatus.UpdateAvailable(
        latestVersion = remoteVersion,
        releaseTitle = releaseTitle,
        releaseNotes = releaseNotes,
        apkDownloadUrl = apkDownloadUrl,
        releasePageUrl = htmlUrl,
        publishedDate = formattedDate
      )
    } else {
      UpdateStatus.UpToDate(
        checkedVersion = remoteVersion.ifBlank { currentVersion },
        checkedTimestamp = nowTimestamp
      )
    }
  } catch (e: Exception) {
    UpdateStatus.Error("Network error checking releases: ${e.localizedMessage ?: e.javaClass.simpleName}")
  } finally {
    connection?.disconnect()
  }
}

/**
 * Compares two semantic version strings (e.g. "1.1.0" vs "1.0.0")
 */
fun isVersionNewer(remote: String, local: String): Boolean {
  val cleanRemote = remote.trim().removePrefix("v").removePrefix("V").trim()
  val cleanLocal = local.trim().removePrefix("v").removePrefix("V").trim()
  if (cleanRemote.isBlank()) return false
  if (cleanRemote.equals(cleanLocal, ignoreCase = true)) return false

  val remoteParts = cleanRemote.split('.').mapNotNull { it.toIntOrNull() }
  val localParts = cleanLocal.split('.').mapNotNull { it.toIntOrNull() }

  val maxLen = maxOf(remoteParts.size, localParts.size)
  for (i in 0 until maxLen) {
    val r = remoteParts.getOrElse(i) { 0 }
    val l = localParts.getOrElse(i) { 0 }
    if (r > l) return true
    if (r < l) return false
  }
  return false
}
