package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.NovelEntity
import com.example.ui.theme.AntiqueGold
import com.example.ui.theme.CharcoalSecondary
import com.example.ui.theme.CharcoalTertiary
import com.example.ui.theme.CharcoalText
import com.example.ui.theme.DeepBurgundy
import com.example.ui.theme.SoftCreamPaper
import com.example.ui.theme.SubtleBorder

@Composable
fun CloudPublishModal(
  novel: NovelEntity,
  novelJson: String,
  fullCatalogJson: String,
  isAlreadyCloudPublished: Boolean = false,
  initialGitHubToken: String = "",
  initialWriteUrl: String = "",
  onDismiss: () -> Unit,
  onPublishToCloud: (token: String?, writeUrl: String?, onDone: (success: Boolean, message: String) -> Unit) -> Unit,
  onUploadToFirebaseStorage: ((onDone: (success: Boolean, message: String) -> Unit) -> Unit)? = null,
) {
  val context = LocalContext.current
  var gitHubToken by remember { mutableStateOf(initialGitHubToken) }
  var writeUrl by remember { mutableStateOf(initialWriteUrl) }
  var isPublishing by remember { mutableStateOf(false) }
  var statusMessage by remember {
    mutableStateOf(
      if (isAlreadyCloudPublished) "✨ Successfully published to Global Cloud Archive! All APK readers can now read this manuscript."
      else null
    )
  }
  var isSuccess by remember { mutableStateOf(isAlreadyCloudPublished) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = SoftCreamPaper,
      tonalElevation = 6.dp,
      modifier = Modifier
        .fillMaxWidth(0.92f)
        .widthIn(max = 480.dp)
        .heightIn(max = 720.dp)
        .padding(vertical = 16.dp)
        .testTag("cloud_publish_modal")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(24.dp)
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(DeepBurgundy.copy(alpha = 0.12f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = if (isSuccess) Icons.Outlined.CloudDone else Icons.Outlined.CloudUpload,
                contentDescription = null,
                tint = DeepBurgundy,
                modifier = Modifier.size(22.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "GLOBAL CLOUD ARCHIVE",
                style = MaterialTheme.typography.labelMedium.copy(
                  letterSpacing = 1.2.sp,
                  fontWeight = FontWeight.Bold
                ),
                color = DeepBurgundy
              )
              Text(
                text = if (isSuccess) "Published Globally" else "Global Distribution Hub",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.SemiBold
                ),
                color = CharcoalText
              )
            }
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("close_cloud_publish_modal")
          ) {
            Icon(
              imageVector = Icons.Outlined.Close,
              contentDescription = "Close",
              tint = CharcoalSecondary
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Manuscript summary card
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = Color.White),
          border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SubtleBorder)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Text(
              text = novel.title,
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = CharcoalText
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "By ${novel.author} • Room ${novel.authorSlot} • ${novel.novelStatus}",
              style = MaterialTheme.typography.bodySmall,
              color = CharcoalSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "All users who installed Strawberrycandy will see this novel once synchronized with the global cloud archive.",
              style = MaterialTheme.typography.labelSmall.copy(lineHeight = 16.sp),
              color = CharcoalTertiary
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Status banner
        if (statusMessage != null) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isSuccess) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
            border = androidx.compose.foundation.BorderStroke(
              1.dp,
              if (isSuccess) Color(0xFF81C784) else Color(0xFFE57373)
            ),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = if (isSuccess) Icons.Outlined.Check else Icons.Outlined.CloudUpload,
                contentDescription = null,
                tint = if (isSuccess) Color(0xFF2E7D32) else Color(0xFFC62828),
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = statusMessage!!,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = if (isSuccess) Color(0xFF1B5E20) else Color(0xFFB71C1C)
              )
            }
          }
          Spacer(modifier = Modifier.height(16.dp))
        }

        // Action Option 0: Instant 1-Click Direct Cloud Upload (Recommended / Wattpad style)
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = AntiqueGold.copy(alpha = 0.08f),
          border = androidx.compose.foundation.BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.4f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Outlined.Public,
                contentDescription = null,
                tint = AntiqueGold,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "WATTPAD-STYLE DIRECT CLOUD UPLOAD (RECOMMENDED)",
                style = MaterialTheme.typography.labelSmall.copy(
                  letterSpacing = 0.8.sp,
                  fontWeight = FontWeight.Bold
                ),
                color = CharcoalText
              )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Upload directly to the Strawberrycandy Global Cloud Archive. Zero tokens, zero configuration — instant sync for all APK readers worldwide.",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, lineHeight = 16.sp),
              color = CharcoalSecondary
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
              onClick = {
                isPublishing = true
                statusMessage = null
                onPublishToCloud(null, null) { success, msg ->
                  isPublishing = false
                  isSuccess = success
                  statusMessage = msg
                }
              },
              enabled = !isPublishing,
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(containerColor = DeepBurgundy),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("publish_direct_cloud_button")
            ) {
              if (isPublishing) {
                CircularProgressIndicator(
                  color = SoftCreamPaper,
                  strokeWidth = 2.dp,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Publishing to Cloud...", color = SoftCreamPaper, fontSize = 12.sp)
              } else {
                Icon(
                  imageVector = Icons.Outlined.CloudUpload,
                  contentDescription = null,
                  tint = SoftCreamPaper,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Publish to Cloud (No Token Needed)",
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                  color = SoftCreamPaper
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Action Option 1: Direct Push to GitHub repository (novels.json)
        Text(
          text = "OPTION 1: DIRECT COMMIT TO GITHUB REPO",
          style = MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 1.0.sp,
            fontWeight = FontWeight.Bold
          ),
          color = CharcoalSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "Commits directly to 'clarifyrgb/strawberrycandy/novels.json' using a GitHub Personal Access Token. Instantly live worldwide.",
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 15.sp),
          color = CharcoalTertiary
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = gitHubToken,
          onValueChange = { gitHubToken = it },
          label = { Text("GitHub Token (PAT with repo scope)", fontSize = 11.sp) },
          placeholder = { Text("ghp_xxxxxxxxxxxxxxxxxxxx", fontSize = 11.sp) },
          leadingIcon = {
            Icon(
              imageVector = Icons.Outlined.Key,
              contentDescription = null,
              modifier = Modifier.size(16.dp)
            )
          },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("github_token_input"),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = DeepBurgundy,
            unfocusedBorderColor = SubtleBorder,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
          ),
          shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
          onClick = {
            if (gitHubToken.isBlank()) {
              statusMessage = "Please enter your GitHub Personal Access Token."
              isSuccess = false
              return@Button
            }
            isPublishing = true
            statusMessage = null
            onPublishToCloud(gitHubToken, null) { success, msg ->
              isPublishing = false
              isSuccess = success
              statusMessage = msg
            }
          },
          enabled = !isPublishing,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = CharcoalText),
          modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .testTag("commit_github_button")
        ) {
          if (isPublishing) {
            CircularProgressIndicator(
              modifier = Modifier.size(16.dp),
              color = SoftCreamPaper,
              strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Publishing to GitHub...", fontSize = 12.sp, color = SoftCreamPaper)
          } else {
            Icon(
              imageVector = Icons.Outlined.Send,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
              tint = SoftCreamPaper
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              "Commit to novels.json on GitHub",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
              color = SoftCreamPaper
            )
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Action Option 2: Firebase Realtime DB / REST API
        Text(
          text = "OPTION 2: FIREBASE REALTIME DB / CUSTOM REST",
          style = MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 1.0.sp,
            fontWeight = FontWeight.Bold
          ),
          color = CharcoalSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
          value = writeUrl,
          onValueChange = { writeUrl = it },
          label = { Text("Cloud REST / Firebase DB URL", fontSize = 11.sp) },
          placeholder = { Text("https://<project>-default-rtdb.firebaseio.com", fontSize = 11.sp) },
          leadingIcon = {
            Icon(
              imageVector = Icons.Outlined.Public,
              contentDescription = null,
              modifier = Modifier.size(16.dp)
            )
          },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("write_url_input"),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = DeepBurgundy,
            unfocusedBorderColor = SubtleBorder,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
          ),
          shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
          onClick = {
            if (writeUrl.isBlank()) {
              statusMessage = "Please enter a Firebase or REST API URL."
              isSuccess = false
              return@Button
            }
            isPublishing = true
            statusMessage = null
            onPublishToCloud(null, writeUrl) { success, msg ->
              isPublishing = false
              isSuccess = success
              statusMessage = msg
            }
          },
          enabled = !isPublishing,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = DeepBurgundy),
          modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .testTag("publish_rest_button")
        ) {
          Icon(
            imageVector = Icons.Outlined.CloudUpload,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = SoftCreamPaper
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            "Publish to Cloud Server / Firebase",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = SoftCreamPaper
          )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Action Option 2B: Firebase Cloud Storage Bucket
        if (onUploadToFirebaseStorage != null) {
          Text(
            text = "FIREBASE CLOUD STORAGE (BUCKET BLOB)",
            style = MaterialTheme.typography.labelSmall.copy(
              letterSpacing = 1.0.sp,
              fontWeight = FontWeight.Bold
            ),
            color = CharcoalSecondary
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Back up this novel's full manuscript, metadata JSON, and cover to your Firebase Storage bucket (gs://strawberrycandy.firebasestorage.app).",
            style = MaterialTheme.typography.bodySmall,
            color = CharcoalTertiary
          )
          Spacer(modifier = Modifier.height(8.dp))

          OutlinedButton(
            onClick = {
              isPublishing = true
              statusMessage = null
              onUploadToFirebaseStorage { success, msg ->
                isPublishing = false
                isSuccess = success
                statusMessage = msg
              }
            },
            enabled = !isPublishing,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(44.dp)
              .testTag("upload_firebase_storage_button"),
            border = androidx.compose.foundation.BorderStroke(1.dp, DeepBurgundy)
          ) {
            Icon(
              imageVector = Icons.Outlined.CloudUpload,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
              tint = DeepBurgundy
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              "Upload to Firebase Storage Bucket",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
              color = DeepBurgundy
            )
          }

          Spacer(modifier = Modifier.height(20.dp))
        }

        // Action Option 3: Copy JSON to clipboard
        Text(
          text = "OPTION 3: MANUAL GITHUB / JSON UPDATE",
          style = MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 1.0.sp,
            fontWeight = FontWeight.Bold
          ),
          color = CharcoalSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "Copy the formatted JSON entry and paste it into novels.json in the GitHub repository. All APKs fetch it automatically on launch.",
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 15.sp),
          color = CharcoalTertiary
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = {
              val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
              clipboard.setPrimaryClip(ClipData.newPlainText("Novel JSON", novelJson))
              Toast.makeText(context, "Copied manuscript JSON to clipboard!", Toast.LENGTH_SHORT).show()
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .weight(1f)
              .height(44.dp)
              .testTag("copy_novel_json_button")
          ) {
            Icon(
              imageVector = Icons.Outlined.ContentCopy,
              contentDescription = null,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Copy Novel JSON", fontSize = 11.sp)
          }

          OutlinedButton(
            onClick = {
              val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
              clipboard.setPrimaryClip(ClipData.newPlainText("Full novels.json Catalog", fullCatalogJson))
              Toast.makeText(context, "Copied full novels.json archive to clipboard!", Toast.LENGTH_SHORT).show()
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .weight(1f)
              .height(44.dp)
              .testTag("copy_full_json_button")
          ) {
            Icon(
              imageVector = Icons.Outlined.ContentCopy,
              contentDescription = null,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Copy Full Catalog", fontSize = 11.sp)
          }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Done button
        Button(
          onClick = onDismiss,
          shape = RoundedCornerShape(16.dp),
          colors = ButtonDefaults.buttonColors(containerColor = AntiqueGold),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("done_publish_modal_button")
        ) {
          Text(
            text = "Done & Return to Library",
            style = MaterialTheme.typography.labelLarge.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.5.sp
            ),
            color = CharcoalText
          )
        }
      }
    }
  }
}
