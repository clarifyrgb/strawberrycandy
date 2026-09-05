package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.NovelWithState
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PortfolioPresentationScreen
import com.example.ui.screens.ReadingScreen
import com.example.ui.theme.AntiqueGold
import com.example.ui.theme.CharcoalSecondary
import com.example.ui.theme.CharcoalText
import com.example.ui.theme.CreamBackground
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SubtleBorder
import com.example.viewmodel.StrawberrycandyViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        StrawberrycandyApp()
      }
    }
  }
}

enum class ViewMode {
  DEVICE_FLOW,
  PORTFOLIO_PRESENTATION
}

@Composable
fun StrawberrycandyApp(
  viewModel: StrawberrycandyViewModel = viewModel(),
) {
  val uiState by viewModel.uiState.collectAsState()
  var selectedNovelId by remember { mutableStateOf<String?>(null) }
  var viewMode by remember { mutableStateOf(ViewMode.DEVICE_FLOW) }
  val snackbarHostState = remember { SnackbarHostState() }

  val selectedNovel = uiState.novels.find { it.id == selectedNovelId }

  LaunchedEffect(uiState.message) {
    uiState.message?.let { msg ->
      snackbarHostState.showSnackbar(msg)
      viewModel.clearSnackbarMessage()
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(CreamBackground)
      .testTag("app_root_container")
  ) {
    AnimatedContent(
      targetState = Pair(viewMode, selectedNovel),
      transitionSpec = { fadeIn() togetherWith fadeOut() },
      label = "screen_transition"
    ) { (mode, currentNovel) ->
      when {
        mode == ViewMode.PORTFOLIO_PRESENTATION -> {
          PortfolioPresentationScreen(
            viewModel = viewModel,
            onOpenNovel = { novel ->
              selectedNovelId = novel.id
              viewMode = ViewMode.DEVICE_FLOW
            }
          )
        }
        currentNovel != null -> {
          ReadingScreen(
            novel = currentNovel,
            onBack = { selectedNovelId = null },
            onSaveProgress = { page ->
              viewModel.saveReadingProgress(currentNovel.id, page)
            },
            onToggleFavorite = {
              viewModel.toggleFavorite(currentNovel.id)
            },
            viewModel = viewModel
          )
        }
        else -> {
          HomeScreen(
            viewModel = viewModel,
            onSelectNovel = { novel -> selectedNovelId = novel.id }
          )
        }
      }
    }

    // Discreet portfolio presentation switcher pill
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = Color(0xF5FAF7F2),
      shadowElevation = 6.dp,
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .navigationBarsPadding()
        .padding(bottom = 12.dp)
        .clip(RoundedCornerShape(20.dp))
        .border(1.dp, SubtleBorder, RoundedCornerShape(20.dp))
        .testTag("portfolio_toggle_bar")
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        val isDeviceFlow = viewMode == ViewMode.DEVICE_FLOW
        Row(
          modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDeviceFlow) AntiqueGold.copy(alpha = 0.15f) else Color.Transparent)
            .clickable { viewMode = ViewMode.DEVICE_FLOW }
            .padding(horizontal = 8.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Outlined.Smartphone,
            contentDescription = "Interactive App Mode",
            tint = if (isDeviceFlow) AntiqueGold else CharcoalSecondary,
            modifier = Modifier.size(13.dp)
          )
          Text(
            text = if (selectedNovel == null) "Screen 1: Bookshelf" else "Screen 2: Reader",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 9.sp,
              letterSpacing = 0.6.sp
            ),
            color = if (isDeviceFlow) CharcoalText else CharcoalSecondary,
            modifier = Modifier.padding(start = 4.dp)
          )
        }

        val isPortfolio = viewMode == ViewMode.PORTFOLIO_PRESENTATION
        Row(
          modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isPortfolio) AntiqueGold.copy(alpha = 0.15f) else Color.Transparent)
            .clickable { viewMode = ViewMode.PORTFOLIO_PRESENTATION }
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("portfolio_mode_button"),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Outlined.Devices,
            contentDescription = "UX/UI Portfolio 16:9 Showcase",
            tint = if (isPortfolio) AntiqueGold else CharcoalSecondary,
            modifier = Modifier.size(13.dp)
          )
          Text(
            text = "Portfolio 16:9",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 9.sp,
              letterSpacing = 0.6.sp
            ),
            color = if (isPortfolio) CharcoalText else CharcoalSecondary,
            modifier = Modifier.padding(start = 4.dp)
          )
        }
      }
    }

    SnackbarHost(
      hostState = snackbarHostState,
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(bottom = 70.dp)
    )
  }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
  MyApplicationTheme { Greeting("Android") }
}
