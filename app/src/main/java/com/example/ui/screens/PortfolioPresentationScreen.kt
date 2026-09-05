package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NoPhotography
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.NovelEntity
import com.example.data.local.UserReadingStateEntity
import com.example.model.NovelWithState
import com.example.ui.theme.AntiqueGold
import com.example.ui.theme.CharcoalSecondary
import com.example.ui.theme.CharcoalText
import com.example.ui.theme.SoftCreamPaper
import com.example.viewmodel.StrawberrycandyViewModel

@Composable
fun PortfolioPresentationScreen(
  viewModel: StrawberrycandyViewModel,
  onOpenNovel: (NovelWithState) -> Unit,
  modifier: Modifier = Modifier,
) {
  val novels by viewModel.allNovels.collectAsState()
  val sampleNovel = novels.firstOrNull() ?: NovelWithState(
    novel = NovelEntity(
      id = "nov_sample",
      title = "The Architecture of Silence",
      subtitle = "Monastic Spacing & Solitary Thought",
      author = "Strawberrycandy",
      year = "2026",
      editionNumber = "ARCHIVE NO. 042 / 100",
      coverDrawableRes = R.drawable.img_book_1,
      chapterTitle = "Chapter III • The Acoustics of Stillness",
      totalPages = 312,
      excerpt = "To construct a room for silence is not to subtract sound, but to tune the resonance of what remains.",
      contentText = "To construct a room for silence is not merely to subtract sound, but to tune the subtle resonance of what remains. In the cloistered arcades of Thoronet and the vaulted corridors of Sénanque, stone does not absorb speech; it receives it as a transient vibration, smoothing harshness into an echo that returns only as ambient presence."
    ),
    userState = UserReadingStateEntity(
      compositeId = "demo_1",
      userId = "demo",
      novelId = "nov_sample",
      currentPage = 48,
      isFavorite = true,
      inReadingList = true
    )
  )

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(
        brush = Brush.radialGradient(
          colors = listOf(
            Color(0xFFEDE9DF),
            Color(0xFFE2DDD2),
            Color(0xFFD7D1C5)
          ),
          radius = 1800f
        )
      )
      .statusBarsPadding()
      .testTag("portfolio_presentation_screen")
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 20.dp, vertical = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Portfolio Presentation Header
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center
        ) {
          Box(
            modifier = Modifier
              .size(6.dp)
              .clip(CircleShape)
              .background(AntiqueGold)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "UX/UI DESIGN SYSTEM & PORTFOLIO PRESENTATION",
            style = MaterialTheme.typography.labelSmall.copy(
              letterSpacing = 2.sp,
              fontWeight = FontWeight.SemiBold
            ),
            color = AntiqueGold
          )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = "Strawberrycandy — Author Publishing Architecture",
          style = MaterialTheme.typography.headlineMedium.copy(
            fontFamily = FontFamily.Serif,
            letterSpacing = (-0.2).sp
          ),
          color = CharcoalText,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
          text = "Screen 1: Curated Shelf & Owner Studio  •  Screen 2: Distraction-Free Reader & Sync",
          style = MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 1.2.sp
          ),
          color = CharcoalSecondary,
          textAlign = TextAlign.Center
        )
      }

      // Side-by-Side High-Fidelity Mockups
      Row(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Device Mockup 1: Home Dashboard
        PortfolioDeviceMockup(
          titleLabel = "SCREEN 1 : HOME DASHBOARD",
          subtitleLabel = "Curated 3-Book Shelf • Owner Publishing • Google/Apple Auth"
        ) {
          HomeScreen(
            viewModel = viewModel,
            onSelectNovel = onOpenNovel,
            modifier = Modifier.fillMaxSize()
          )
        }

        Spacer(modifier = Modifier.width(28.dp))

        // Device Mockup 2: Reading View
        PortfolioDeviceMockup(
          titleLabel = "SCREEN 2 : THE READING VIEW",
          subtitleLabel = "Distraction-Free • Resume Page Sync • Ghosted DRM"
        ) {
          ReadingScreen(
            novel = sampleNovel,
            onBack = {},
            modifier = Modifier.fillMaxSize()
          )
        }
      }

      // Feature Specifications Bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 10.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
      ) {
        FeatureSpecBadge(
          icon = Icons.Outlined.AutoStories,
          label = "Owner Studio Uploads"
        )
        FeatureSpecBadge(
          icon = Icons.Outlined.Sync,
          label = "Continue Where You Stopped"
        )
        FeatureSpecBadge(
          icon = Icons.Outlined.Shield,
          label = "Google & Apple Reader Sync"
        )
      }
    }
  }
}

@Composable
private fun PortfolioDeviceMockup(
  titleLabel: String,
  subtitleLabel: String,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier.width(320.dp)
  ) {
    Text(
      text = titleLabel,
      style = MaterialTheme.typography.labelMedium.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.4.sp
      ),
      color = CharcoalText,
      textAlign = TextAlign.Center
    )
    Text(
      text = subtitleLabel,
      style = MaterialTheme.typography.labelSmall.copy(
        fontSize = 8.5.sp,
        letterSpacing = 0.8.sp
      ),
      color = CharcoalSecondary,
      textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(8.dp))

    Card(
      shape = RoundedCornerShape(32.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1D1B)),
      elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
      modifier = Modifier
        .fillMaxWidth()
        .height(560.dp)
        .shadow(
          elevation = 20.dp,
          shape = RoundedCornerShape(32.dp),
          ambientColor = Color(0x35000000),
          spotColor = Color(0x25000000)
        )
        .border(
          width = 3.dp,
          color = Color(0xFF2C2B29),
          shape = RoundedCornerShape(32.dp)
        )
    ) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(6.dp)
          .clip(RoundedCornerShape(26.dp))
          .background(SoftCreamPaper)
      ) {
        // Dynamic Island / Speaker notch simulation
        Box(
          modifier = Modifier
            .padding(top = 8.dp)
            .width(80.dp)
            .height(18.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF121212))
            .align(Alignment.TopCenter)
        )

        content()
      }
    }
  }
}

@Composable
private fun FeatureSpecBadge(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.padding(horizontal = 6.dp)
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = AntiqueGold,
      modifier = Modifier.size(13.dp)
    )
    Spacer(modifier = Modifier.width(5.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall.copy(
        fontSize = 8.5.sp,
        letterSpacing = 0.8.sp
      ),
      color = CharcoalSecondary
    )
  }
}
