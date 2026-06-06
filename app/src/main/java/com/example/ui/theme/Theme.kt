package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = SunsetOrange80,
    onPrimary = Color(0xFF3E1700),
    primaryContainer = Color(0xFF5C2D00),
    onPrimaryContainer = SunsetOrange80,
    secondary = SunsetDeepBlue80,
    onSecondary = Color(0xFF0D1B3E),
    secondaryContainer = Color(0xFF1A2F5A),
    onSecondaryContainer = SunsetDeepBlue80,
    tertiary = SunsetCoral80,
    onTertiary = Color(0xFF3E0A00),
    tertiaryContainer = Color(0xFF5C1A0A),
    onTertiaryContainer = SunsetCoral80,
    background = DarkBackground,
    onBackground = Color(0xFFE6E1E5),
    surface = DarkSurface,
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    inverseSurface = Color(0xFFE6E1E5),
    inverseOnSurface = Color(0xFF1C1B1F),
    inversePrimary = SunsetOrange40,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = SunsetOrange40,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE0B2),
    onPrimaryContainer = Color(0xFF3E1700),
    secondary = SunsetDeepBlue40,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC5CAE9),
    onSecondaryContainer = Color(0xFF0D1B3E),
    tertiary = SunsetCoral40,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFCDD2),
    onTertiaryContainer = Color(0xFF3E0A00),
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFF5EDE7),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E),
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disable dynamic color to always use our branded sunset palette
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
