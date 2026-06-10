package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = CyberCyan,
    secondary = CyberAmber,
    tertiary = CyberEmerald,
    background = CyberMidnight,
    surface = CyberSlate,
    onPrimary = CyberMidnight,
    onSecondary = CyberMidnight,
    onTertiary = CyberMidnight,
    onBackground = TextPrimary,
    onSurface = TextPrimary
  )

private val LightColorScheme = DarkColorScheme // Force dark cyberpunk aesthetic for cohesive, immersive UX

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark theme by default for the Cyberpunk feel
  dynamicColor: Boolean = false, // Disable dynamic colors to maintain high-contrast cyber theme integrity
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
