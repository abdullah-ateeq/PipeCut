package com.example.pipecut.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.pipecut.data.model.AppThemeMode

private val IndustrialDarkColorScheme = darkColorScheme(
    primary = IndustrialBluePrimaryDark,
    onPrimary = IndustrialBlueOnPrimaryDark,
    primaryContainer = IndustrialBlueContainerDark,
    onPrimaryContainer = IndustrialBlueOnContainerDark,
    background = CharcoalDarkBg,
    onBackground = CharcoalDarkOnSurface,
    surface = CharcoalDarkSurface,
    onSurface = CharcoalDarkOnSurface,
    surfaceVariant = CharcoalDarkSurfaceVariant,
    onSurfaceVariant = CharcoalDarkOnSurfaceVariant,
    outline = CharcoalDarkOutline,
    tertiary = AmberSafety,
    error = RedTrim
)

private val IndustrialLightColorScheme = lightColorScheme(
    primary = IndustrialBluePrimaryLight,
    onPrimary = IndustrialBlueOnPrimaryLight,
    primaryContainer = IndustrialBlueContainerLight,
    onPrimaryContainer = IndustrialBlueOnContainerLight,
    background = LightBg,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    tertiary = AmberSafety,
    error = RedTrim
)

@Composable
fun PipeCutTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    val colorScheme = if (darkTheme) IndustrialDarkColorScheme else IndustrialLightColorScheme

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
