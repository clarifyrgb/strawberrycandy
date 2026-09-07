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
import org.json.JSONArray
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
      val result = fetchAppUpdate(
        repo = "clarifyrgb/strawberrycandy",
        fallbackJsonUrl = updateJsonUrl,
        currentVersionName = currentVersionName,
        currentVersionCode = currentVersionCode
      )
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
            Icon(
              imageVector = Icons.Outlined.Upload,
              contentDescription = null,
              tint = AntiqueGold,
              modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
              text = "Tags Source:",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
              color = CharcoalTertiary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "github.com/clarifyrgb/strawberrycandy/tags",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
              ),
              color = AntiqueGold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }

          IconButton(
            onClick = {
              val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/clarifyrgb/strawberrycandy/tags"))
              try { context.startActivity(browserIntent) } catch (_: Exception) {}
            },
            modifier = Modifier.size(24.dp)
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
              contentDescription = "Open GitHub Tags",
              tint = AntiqueGold,
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
            text = "Always inspects latest APK releases and versions posted under github.com/clarifyrgb/strawberrycandy/tags.",
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
              text = "Looking for latest APK under GitHub tags...",
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

            // Quick link to GitHub tags page
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "🏷️ Posted at github.com/clarifyrgb/strawberrycandy/tags",
                fontSize = 8.5.sp,
                color = Color(0xFF2E7D32)
              )
              Text(
                text = "Open Tags",
                fontSize = 8.5.sp,
                color = Color(0xFF1B5E20),
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                  .clip(RoundedCornerShape(4.dp))
                  .clickable {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/clarifyrgb/strawberrycandy/tags"))
                    try { context.startActivity(intent) } catch (_: Exception) {}
                  }
                  .padding(horizontal = 4.dp, vertical = 2.dp)
              )
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
                  text = "v${upToDate.checkedVersion} is the latest version under GitHub tags • Checked ${upToDate.checkedTimestamp}",
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                  color = CharcoalSecondary
                )
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              OutlinedButton(
                onClick = {
                  val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/clarifyrgb/strawberrycandy/tags"))
                  try { context.startActivity(intent) } catch (_: Exception) {}
                },
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(0.8.dp, AntiqueGold.copy(alpha = 0.5f)),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(26.dp)
              ) {
                Text(
                  text = "Browse Tags (/tags)",
                  fontSize = 9.sp,
                  color = AntiqueGold,
                  fontWeight = FontWeight.Medium
                )
              }

              OutlinedButton(
                onClick = onSimulateUpdate,
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(0.8.dp, Color(0xFF388E3C).copy(alpha = 0.5f)),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(26.dp)
              ) {
                Text(
                  text = "Preview Update UI",
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
            OutlinedButton(
              onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/clarifyrgb/strawberrycandy/tags"))
                try { context.startActivity(intent) } catch (_: Exception) {}
              },
              shape = RoundedCornerShape(6.dp),
              border = BorderStroke(0.8.dp, AntiqueGold.copy(alpha = 0.5f)),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
              modifier = Modifier.height(26.dp)
            ) {
              Text("Open github.com/clarifyrgb/strawberrycandy/tags", fontSize = 9.sp, color = AntiqueGold)
            }
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
            OutlinedButton(
              onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/clarifyrgb/strawberrycandy/tags"))
                try { context.startActivity(intent) } catch (_: Exception) {}
              },
              shape = RoundedCornerShape(6.dp),
              border = BorderStroke(0.8.dp, Color(0xFFC62828).copy(alpha = 0.5f)),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
              modifier = Modifier.height(26.dp)
            ) {
              Text("Browse GitHub Tags Directly", fontSize = 9.sp, color = Color(0xFFC62828))
            }
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

      HorizontalDivider(thickness = 0.6.dp, color = SubtleBorder)

      DiagnosticRow(label = "Sync Architecture", value = "Remote Multi-User Shared Sync")
      DiagnosticRow(
        label = "Cloud Synced Manuscripts",
        value = if (cloudSyncedCount > 0) "$cloudSyncedCount novels synchronized" else "Ready to sync"
      )
      val timeFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
      val timeStr = if (lastSyncTime > 0) timeFormat.format(Date(lastSyncTime)) else "On startup"
      DiagnosticRow(label = "Last Cloud Sync", value = timeStr)

      Spacer(modifier = Modifier.height(2.dp))

      Button(
        onClick = onSyncNow,
        enabled = !isCloudSyncing,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = CharcoalText),
        modifier = Modifier
          .fillMaxWidth()
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
// NETWORK QUERY: ALWAYS CHECK GITHUB REPOSITORY TAGS (clarifyrgb/strawberrycandy/tags)
// ---------------------------------------------------------------------
data class TagReleaseInfo(
  val title: String,
  val notes: String,
  val htmlUrl: String,
  val apkUrl: String,
  val publishedDate: String
)

suspend fun fetchAppUpdate(
  repo: String = "clarifyrgb/strawberrycandy",
  fallbackJsonUrl: String = "https://raw.githubusercontent.com/clarifyrgb/strawberrycandy/refs/heads/main/update.json",
  currentVersionName: String,
  currentVersionCode: Int
): UpdateStatus = withContext(Dispatchers.IO) {
  val cleanRepo = repo.trim().removePrefix("https://github.com/").removePrefix("github.com/").trimEnd('/')
  val nowTimestamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

  // 1. ALWAYS query the GitHub repository tags first (https://github.com/clarifyrgb/strawberrycandy/tags)
  try {
    val tagsResult = queryGitHubTags(cleanRepo, currentVersionName, currentVersionCode, nowTimestamp)
    if (tagsResult != null) {
      return@withContext tagsResult
    }
  } catch (e: Exception) {
    android.util.Log.w("AppUpdate", "Tags query failed, trying fallback: ${e.message}")
  }

  // 2. Fallback: Query GitHub releases/latest
  try {
    val releaseResult = queryGitHubLatestRelease(cleanRepo, currentVersionName)
    if (releaseResult !is UpdateStatus.Error && releaseResult !is UpdateStatus.NoReleasesFound) {
      return@withContext releaseResult
    }
  } catch (_: Exception) {}

  // 3. Fallback: Query update.json if available
  try {
    val jsonResult = queryUpdateJson(fallbackJsonUrl, currentVersionName, currentVersionCode, nowTimestamp)
    if (jsonResult != null) {
      return@withContext jsonResult
    }
  } catch (_: Exception) {}

  // 4. If all fail or no releases/tags exist
  UpdateStatus.NoReleasesFound(
    repository = cleanRepo,
    message = "No release tags found under https://github.com/$cleanRepo/tags."
  )
}

// Backwards compatibility overload
suspend fun fetchAppUpdate(
  fallbackJsonUrl: String,
  currentVersionName: String,
  currentVersionCode: Int
): UpdateStatus = fetchAppUpdate(
  repo = "clarifyrgb/strawberrycandy",
  fallbackJsonUrl = fallbackJsonUrl,
  currentVersionName = currentVersionName,
  currentVersionCode = currentVersionCode
)

suspend fun queryGitHubTags(
  cleanRepo: String,
  currentVersionName: String,
  currentVersionCode: Int,
  nowTimestamp: String
): UpdateStatus? = withContext(Dispatchers.IO) {
  var connection: HttpURLConnection? = null
  try {
    val tagsApiUrl = "https://api.github.com/repos/$cleanRepo/tags?per_page=30&t=${System.currentTimeMillis()}"
    val url = URL(tagsApiUrl)
    connection = (url.openConnection() as HttpURLConnection).apply {
      requestMethod = "GET"
      connectTimeout = 8000
      readTimeout = 8000
      useCaches = false
      setRequestProperty("Accept", "application/vnd.github.v3+json")
      setRequestProperty("User-Agent", "Strawberrycandy-App-Update-Checker")
    }

    val responseCode = connection.responseCode
    if (responseCode == 404) {
      return@withContext UpdateStatus.NoReleasesFound(
        repository = cleanRepo,
        message = "No tags found under https://github.com/$cleanRepo/tags."
      )
    }
    if (responseCode !in 200..299) {
      return@withContext null // fallback
    }

    val responseText = connection.inputStream.bufferedReader().use { it.readText() }.trim()
    if (!responseText.startsWith("[")) {
      return@withContext null
    }

    val tagsArray = JSONArray(responseText)
    if (tagsArray.length() == 0) {
      return@withContext UpdateStatus.NoReleasesFound(
        repository = cleanRepo,
        message = "No tags found under https://github.com/$cleanRepo/tags."
      )
    }

    val tagNames = mutableListOf<String>()
    for (i in 0 until tagsArray.length()) {
      val tagObj = tagsArray.getJSONObject(i)
      val name = tagObj.optString("name", "").trim()
      if (name.isNotBlank()) {
        tagNames.add(name)
      }
    }

    if (tagNames.isEmpty()) {
      return@withContext null
    }

    // Find the latest semantic version tag
    val latestTagName = findLatestTag(tagNames) ?: tagNames.first()
    val cleanLatestVersion = latestTagName.removePrefix("v").removePrefix("V").trim()

    // Query release metadata for this tag from GitHub
    val releaseInfo = fetchReleaseForTag(cleanRepo, latestTagName)

    val releaseTitle = releaseInfo?.title ?: "Strawberrycandy $latestTagName"
    val releaseNotes = releaseInfo?.notes ?: "New version $latestTagName published under https://github.com/$cleanRepo/tags"
    val htmlUrl = releaseInfo?.htmlUrl ?: "https://github.com/$cleanRepo/releases/tag/$latestTagName"
    val apkUrl = releaseInfo?.apkUrl ?: "https://github.com/$cleanRepo/releases/download/$latestTagName/Strawberrycandy.apk"
    val publishedDate = releaseInfo?.publishedDate ?: "Tagged Release"

    val isNewer = isVersionNewer(cleanLatestVersion, currentVersionName)

    if (isNewer) {
      UpdateStatus.UpdateAvailable(
        latestVersion = cleanLatestVersion,
        releaseTitle = releaseTitle,
        releaseNotes = releaseNotes,
        apkDownloadUrl = apkUrl,
        releasePageUrl = htmlUrl,
        publishedDate = publishedDate
      )
    } else {
      UpdateStatus.UpToDate(
        checkedVersion = cleanLatestVersion.ifBlank { currentVersionName },
        checkedTimestamp = nowTimestamp
      )
    }
  } catch (e: Exception) {
    android.util.Log.e("AppUpdate", "Error in queryGitHubTags: ${e.message}")
    null
  } finally {
    connection?.disconnect()
  }
}

suspend fun fetchReleaseForTag(cleanRepo: String, tagName: String): TagReleaseInfo? = withContext(Dispatchers.IO) {
  var connection: HttpURLConnection? = null
  try {
    val releaseUrl = "https://api.github.com/repos/$cleanRepo/releases/tags/$tagName"
    val url = URL(releaseUrl)
    connection = (url.openConnection() as HttpURLConnection).apply {
      requestMethod = "GET"
      connectTimeout = 7000
      readTimeout = 7000
      setRequestProperty("Accept", "application/vnd.github.v3+json")
      setRequestProperty("User-Agent", "Strawberrycandy-App-Update-Checker")
    }

    if (connection.responseCode in 200..299) {
      val text = connection.inputStream.bufferedReader().use { it.readText() }
      val json = JSONObject(text)
      val name = json.optString("name", "Strawberrycandy $tagName")
      val body = json.optString("body", "")
      val htmlUrl = json.optString("html_url", "https://github.com/$cleanRepo/releases/tag/$tagName")
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

      var apkUrl: String? = null
      val assets = json.optJSONArray("assets")
      if (assets != null) {
        for (i in 0 until assets.length()) {
          val asset = assets.getJSONObject(i)
          val assetName = asset.optString("name", "")
          if (assetName.endsWith(".apk", ignoreCase = true)) {
            apkUrl = asset.optString("browser_download_url", null)
            break
          }
        }
      }

      val resolvedApkUrl = apkUrl ?: "https://github.com/$cleanRepo/releases/download/$tagName/Strawberrycandy.apk"

      return@withContext TagReleaseInfo(
        title = name,
        notes = body,
        htmlUrl = htmlUrl,
        apkUrl = resolvedApkUrl,
        publishedDate = formattedDate
      )
    }
  } catch (e: Exception) {
    android.util.Log.w("AppUpdate", "fetchReleaseForTag failed: ${e.message}")
  } finally {
    connection?.disconnect()
  }
  null
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
        message = "No releases found for '$cleanRepo'. Releases will appear here once an APK tag is posted."
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
        apkDownloadUrl = apkDownloadUrl ?: "https://github.com/$cleanRepo/releases/download/$tagName/Strawberrycandy.apk",
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

suspend fun queryUpdateJson(
  rawUrl: String,
  currentVersionName: String,
  currentVersionCode: Int,
  nowTimestamp: String
): UpdateStatus? = withContext(Dispatchers.IO) {
  var connection: HttpURLConnection? = null
  try {
    val fullUrl = if (rawUrl.contains("raw.githubusercontent.com") && !rawUrl.contains("?")) {
      "$rawUrl?t=${System.currentTimeMillis()}"
    } else rawUrl

    val url = URL(fullUrl)
    connection = (url.openConnection() as HttpURLConnection).apply {
      requestMethod = "GET"
      connectTimeout = 6000
      readTimeout = 6000
      useCaches = false
      setRequestProperty("Accept", "application/json, text/plain, */*")
      setRequestProperty("User-Agent", "Strawberrycandy-App-Update-Checker")
    }

    if (connection.responseCode in 200..299) {
      val text = connection.inputStream.bufferedReader().use { it.readText() }.trim()
      if (text.startsWith("{") && text.endsWith("}")) {
        val json = JSONObject(text)
        val remoteVersionName = json.optString("versionName", json.optString("version", "")).trim()
        val remoteVersionCode = json.optInt("versionCode", json.optInt("build", json.optInt("buildNumber", 0)))
        val title = json.optString("title", json.optString("name", "Strawberrycandy $remoteVersionName"))
        val changelog = json.optString("changelog", json.optString("notes", json.optString("description", "")))
        val apkUrl = json.optString("apkUrl", json.optString("downloadUrl", null))
        val releasePageUrl = json.optString("releasePageUrl", json.optString("githubUrl", "https://github.com/clarifyrgb/strawberrycandy/tags"))
        val publishedDate = json.optString("publishedDate", "")

        val hasNewCode = remoteVersionCode > currentVersionCode
        val hasNewName = isVersionNewer(remoteVersionName, currentVersionName)

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
            apkDownloadUrl = apkUrl ?: "https://github.com/clarifyrgb/strawberrycandy/releases/download/v$remoteVersionName/Strawberrycandy.apk",
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
  } catch (_: Exception) {}
  finally {
    connection?.disconnect()
  }
  null
}

/**
 * Compares two semantic version strings (e.g. "1.0.2" vs "1.0.1")
 */
fun compareSemVer(a: String, b: String): Int {
  val cleanA = a.trim().removePrefix("v").removePrefix("V").trim()
  val cleanB = b.trim().removePrefix("v").removePrefix("V").trim()
  val partsA = cleanA.split('.').mapNotNull { seg ->
    seg.takeWhile { it.isDigit() }.toIntOrNull()
  }
  val partsB = cleanB.split('.').mapNotNull { seg ->
    seg.takeWhile { it.isDigit() }.toIntOrNull()
  }
  val maxLen = maxOf(partsA.size, partsB.size)
  for (i in 0 until maxLen) {
    val pA = partsA.getOrElse(i) { 0 }
    val pB = partsB.getOrElse(i) { 0 }
    if (pA != pB) return pA.compareTo(pB)
  }
  return cleanA.compareTo(cleanB)
}

/**
 * Finds the tag with the latest semantic version from a list of tag names
 */
fun findLatestTag(tagNames: List<String>): String? {
  if (tagNames.isEmpty()) return null
  return tagNames.maxWithOrNull { a, b -> compareSemVer(a, b) }
}

/**
 * Returns true if remote version is strictly newer than local version
 */
fun isVersionNewer(remote: String, local: String): Boolean {
  val cleanRemote = remote.trim().removePrefix("v").removePrefix("V").trim()
  val cleanLocal = local.trim().removePrefix("v").removePrefix("V").trim()
  if (cleanRemote.isBlank()) return false
  if (cleanRemote.equals(cleanLocal, ignoreCase = true)) return false
  return compareSemVer(cleanRemote, cleanLocal) > 0
}
