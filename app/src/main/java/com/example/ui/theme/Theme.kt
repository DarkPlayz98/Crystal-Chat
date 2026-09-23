package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
  primary = CrystalPrimaryDark,
  onPrimary = Color(0xFF0F172A),
  primaryContainer = CrystalPrimaryContainerDark,
  onPrimaryContainer = CrystalOnPrimaryContainerDark,
  secondary = CrystalSecondaryDark,
  onSecondary = Color(0xFF0F172A),
  secondaryContainer = Color(0xFF0C4A6E),
  onSecondaryContainer = Color(0xFFBAE6FD),
  background = CrystalDarkBackground,
  onBackground = CrystalDarkOnSurface,
  surface = CrystalDarkSurface,
  onSurface = CrystalDarkOnSurface,
  surfaceVariant = CrystalDarkSurfaceVariant,
  onSurfaceVariant = CrystalDarkOnSurfaceVariant,
  outline = CrystalDarkOutline
)

private val LightColorScheme = lightColorScheme(
  primary = CrystalPrimaryLight,
  onPrimary = Color.White,
  primaryContainer = CrystalPrimaryContainerLight,
  onPrimaryContainer = CrystalOnPrimaryContainerLight,
  secondary = CrystalSecondaryLight,
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFE0F2FE),
  onSecondaryContainer = Color(0xFF0369A1),
  background = CrystalLightBackground,
  onBackground = CrystalLightOnSurface,
  surface = CrystalLightSurface,
  onSurface = CrystalLightOnSurface,
  surfaceVariant = CrystalLightSurfaceVariant,
  onSurfaceVariant = CrystalLightOnSurfaceVariant,
  outline = CrystalLightOutline
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit
) {
  val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
  }

  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as? Activity)?.window
      if (window != null) {
        window.statusBarColor = colorScheme.surface.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
      }
    }
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}

@Composable
fun CrystalTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit
) = MyApplicationTheme(darkTheme = darkTheme, dynamicColor = true, content = content)
