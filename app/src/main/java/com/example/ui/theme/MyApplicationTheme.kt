package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme =
  darkColorScheme(
    primary = AntiqueGold,
    secondary = CharcoalSecondary,
    tertiary = AntiqueGoldLight,
    background = DarkObsidian,
    surface = DarkSurface,
    onPrimary = DarkObsidian,
    onSecondary = DarkOnSurface,
    onTertiary = DarkObsidian,
    onBackground = DarkOnSurface,
    onSurface = DarkOnSurface,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = AntiqueGold,
    secondary = CharcoalSecondary,
    tertiary = CharcoalTertiary,
    background = CreamBackground,
    surface = SoftCreamPaper,
    onPrimary = CardWarmWhite,
    onSecondary = CharcoalText,
    onTertiary = CharcoalText,
    onBackground = CharcoalText,
    onSurface = CharcoalText,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false, // Keep high-fidelity cream paper aesthetic by default
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

