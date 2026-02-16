package com.neviim.market.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.neviim.market.data.repository.SettingsRepository

private val DarkColorScheme = darkColorScheme(
    primary = Teal40,
    onPrimary = Color.White,
    primaryContainer = TealDark,
    onPrimaryContainer = Teal80,
    secondary = Amber40,
    onSecondary = Color.Black,
    secondaryContainer = Amber20,
    onSecondaryContainer = Amber80,
    background = DarkSurface,
    onBackground = Color(0xFFE0E0E0),
    surface = DarkSurfaceVariant,
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = DarkCard,
    onSurfaceVariant = Color(0xFFBDBDBD),
    error = RedLoss,
    onError = Color.White,
    outline = Color(0xFF444466)
)

private val LightColorScheme = lightColorScheme(
    primary = Teal40,
    onPrimary = Color.White,
    primaryContainer = Teal80,
    onPrimaryContainer = TealDark,
    secondary = Amber40,
    onSecondary = Color.Black,
    secondaryContainer = Amber80,
    onSecondaryContainer = Amber20,
    background = Color(0xFFF5F5F5),
    onBackground = Color(0xFF1C1B1F),
    surface = Color.White,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE8E8EE),
    onSurfaceVariant = Color(0xFF49454F),
    error = RedLoss,
    onError = Color.White,
    outline = Color(0xFFCCCCDD)
)

@Composable
fun NeviimTheme(
    content: @Composable () -> Unit
) {
    val themeMode by SettingsRepository.themeMode.collectAsState()
    val darkTheme = when (themeMode) {
        SettingsRepository.ThemeMode.SYSTEM -> isSystemInDarkTheme()
        SettingsRepository.ThemeMode.LIGHT -> false
        SettingsRepository.ThemeMode.DARK -> true
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NeviimTypography,
        content = content
    )
}
