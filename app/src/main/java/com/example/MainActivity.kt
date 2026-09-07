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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.NovelWithState
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ReadingScreen
import com.example.ui.theme.CreamBackground
import com.example.ui.theme.MyApplicationTheme
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

@Composable
fun StrawberrycandyApp(
  viewModel: StrawberrycandyViewModel = viewModel(),
) {
  val uiState by viewModel.uiState.collectAsState()
  var selectedNovelId by remember { mutableStateOf<String?>(null) }
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
      targetState = selectedNovelId,
      transitionSpec = { fadeIn() togetherWith fadeOut() },
      label = "screen_transition"
    ) { currentNovelId ->
      val currentNovel = uiState.novels.find { it.id == currentNovelId }
      if (currentNovel != null) {
        ReadingScreen(
          novel = currentNovel,
          onBack = { selectedNovelId = null },
          onSelectNovel = { newNovel ->
            selectedNovelId = newNovel.id
          },
          onSaveProgress = { page ->
            viewModel.saveReadingProgress(currentNovel.id, page)
          },
          onToggleFavorite = {
            viewModel.toggleFavorite(currentNovel.id)
          },
          viewModel = viewModel
        )
      } else {
        HomeScreen(
          viewModel = viewModel,
          onSelectNovel = { novel ->
            viewModel.recordNovelRead(novel.id)
            selectedNovelId = novel.id
          }
        )
      }
    }

    SnackbarHost(
      hostState = snackbarHostState,
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .navigationBarsPadding()
        .padding(bottom = 16.dp)
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
