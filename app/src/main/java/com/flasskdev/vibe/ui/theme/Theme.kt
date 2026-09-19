package com.flasskdev.vibe.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = VibePrimary,
    secondary = VibeSecondary,
    background = VibeBackgroundLight,
    surface = VibeSurfaceLight,
    surfaceVariant = VibeSurfaceVariantLight,
    surfaceTint = VibeGlassTint,
    onBackground = VibeOnBackgroundLight,
    onSurface = VibeOnSurfaceLight,
    inverseSurface = Color(0xFF2C2C2E),
    inversePrimary = Color(0xFF64B5F6),
    error = VibeError
)

private val DarkColorScheme = darkColorScheme(
    primary = VibePrimary,
    secondary = VibeSecondary,
    background = VibeBackgroundDark,
    surface = VibeSurfaceDark,
    surfaceVariant = VibeSurfaceVariantDark,
    surfaceTint = Color(0xFF3A3A3C).copy(alpha = 0.35f),
    onBackground = VibeOnBackgroundDark,
    onSurface = VibeOnSurfaceDark,
    inverseSurface = Color(0xFFE5E5EA),
    inversePrimary = Color(0xFF007AFF),
    error = VibeError
)

@Composable
fun VibeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }

            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    val typography = remember(view) {
        if (!view.isInEditMode) {
            AppleFontProvider.getTypography(view.context)
        } else {
            Typography
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
