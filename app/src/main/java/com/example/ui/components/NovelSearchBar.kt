package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AntiqueGold
import com.example.ui.theme.CharcoalSecondary
import com.example.ui.theme.CharcoalTertiary
import com.example.ui.theme.CharcoalText
import com.example.ui.theme.SoftCreamPaper
import com.example.ui.theme.SubtleBorder

@Composable
fun NovelSearchBar(
  query: String,
  onQueryChange: (String) -> Unit,
  onClearQuery: () -> Unit,
  matchCount: Int,
  totalCount: Int,
  modifier: Modifier = Modifier,
) {
  val quickTags = listOf("Modern Romance", "Historical Romance", "Fantasy Romance", "R19")

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = SoftCreamPaper),
    border = BorderStroke(1.dp, if (query.isNotBlank()) AntiqueGold.copy(alpha = 0.6f) else SubtleBorder),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = modifier.testTag("novel_search_bar")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedTextField(
          value = query,
          onValueChange = onQueryChange,
          placeholder = {
            Text(
              text = "Search novels by title, author, genre, tags...",
              style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
              color = CharcoalTertiary
            )
          },
          leadingIcon = {
            Icon(
              imageVector = Icons.Outlined.Search,
              contentDescription = "Search",
              tint = AntiqueGold,
              modifier = Modifier.size(18.dp)
            )
          },
          trailingIcon = {
            if (query.isNotBlank()) {
              IconButton(
                onClick = onClearQuery,
                modifier = Modifier.size(28.dp).testTag("clear_novel_search_button")
              ) {
                Icon(
                  imageVector = Icons.Outlined.Close,
                  contentDescription = "Clear search",
                  tint = CharcoalSecondary,
                  modifier = Modifier.size(16.dp)
                )
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AntiqueGold,
            unfocusedBorderColor = SubtleBorder.copy(alpha = 0.6f),
            focusedContainerColor = SoftCreamPaper,
            unfocusedContainerColor = SoftCreamPaper
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("novel_search_input")
        )
      }

      // Search status & quick suggestion pills
      if (query.isNotBlank()) {
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = if (matchCount > 0) "Found $matchCount of $totalCount novels" else "No novels match '$query'",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 10.sp,
              fontWeight = FontWeight.Medium
            ),
            color = if (matchCount > 0) AntiqueGold else CharcoalSecondary
          )

          Surface(
            onClick = onClearQuery,
            shape = RoundedCornerShape(8.dp),
            color = SubtleBorder.copy(alpha = 0.3f),
            modifier = Modifier.clickable { onClearQuery() }
          ) {
            Text(
              text = "Clear filter",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold
              ),
              color = CharcoalText,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }
      } else {
        // Quick tags suggestion row
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Quick find:",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 9.sp,
              fontWeight = FontWeight.SemiBold
            ),
            color = CharcoalTertiary
          )
          quickTags.forEach { tag ->
            Surface(
              onClick = { onQueryChange(tag) },
              shape = RoundedCornerShape(10.dp),
              color = SubtleBorder.copy(alpha = 0.2f),
              border = BorderStroke(0.5.dp, SubtleBorder.copy(alpha = 0.4f)),
              modifier = Modifier.testTag("quick_search_tag_$tag")
            ) {
              Text(
                text = tag,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = CharcoalText,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
              )
            }
          }
        }
      }
    }
  }
}
