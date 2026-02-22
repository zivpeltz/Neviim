package com.neviim.market.ui.theme

import android.app.Activity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.neviim.market.data.repository.SettingsRepository
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// ── Default dark / light schemes ─────────────────────────────────────

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

// ── Jewish theme schemes (Israeli blue & white) ───────────────────────

private val JewishLightColorScheme = lightColorScheme(
    primary = IsraeliBlue,
    onPrimary = Color.White,
    primaryContainer = IsraeliBluePale,
    onPrimaryContainer = IsraeliBlueDeep,
    secondary = IsraeliBlueLight,
    onSecondary = Color.White,
    secondaryContainer = IsraeliBluePale,
    onSecondaryContainer = IsraeliBlueDeep,
    background = IsraeliWhite,
    onBackground = Color(0xFF0A0A2E),
    surface = Color.White,
    onSurface = Color(0xFF0A0A2E),
    surfaceVariant = IsraeliBluePale,
    onSurfaceVariant = IsraeliBlueDeep,
    error = RedLoss,
    onError = Color.White,
    outline = Color(0xFFB0C4FF)
)

private val JewishDarkColorScheme = darkColorScheme(
    primary = IsraeliBlueLight,
    onPrimary = Color.White,
    primaryContainer = IsraeliBlueDeep,
    onPrimaryContainer = IsraeliBluePale,
    secondary = IsraeliBlueLight,
    onSecondary = Color.White,
    secondaryContainer = IsraeliBlueDeep,
    onSecondaryContainer = IsraeliBluePale,
    background = IsraeliNavy,
    onBackground = Color(0xFFE8EEFF),
    surface = IsraeliNavySurface,
    onSurface = Color(0xFFE8EEFF),
    surfaceVariant = Color(0xFF142070),
    onSurfaceVariant = Color(0xFFB0C4FF),
    error = RedLoss,
    onError = Color.White,
    outline = Color(0xFF2A4099)
)

// ── Theme composable ─────────────────────────────────────────────────

@Composable
fun NeviimTheme(content: @Composable () -> Unit) {
    val themeMode by SettingsRepository.themeMode.collectAsState()

    val isJewish = themeMode == SettingsRepository.ThemeMode.JEWISH
    val darkTheme = when (themeMode) {
        SettingsRepository.ThemeMode.SYSTEM -> isSystemInDarkTheme()
        SettingsRepository.ThemeMode.LIGHT  -> false
        SettingsRepository.ThemeMode.DARK   -> true
        SettingsRepository.ThemeMode.JEWISH -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        isJewish && darkTheme  -> JewishDarkColorScheme
        isJewish && !darkTheme -> JewishLightColorScheme
        darkTheme              -> DarkColorScheme
        else                   -> LightColorScheme
    }

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

// ── Star of David decorative pattern ─────────────────────────────────
// Use this composable as a background layer in screens when Jewish theme is active.

@Composable
fun StarOfDavidPattern(
    color: Color = IsraeliBlue.copy(alpha = 0.06f),
    modifier: Modifier = Modifier.fillMaxSize()
) {
    Canvas(modifier = modifier) {
        val starSize = 60.dp.toPx()
        val spacingX = starSize * 2.2f
        val spacingY = starSize * 1.9f
        val cols = (size.width / spacingX).toInt() + 2
        val rows = (size.height / spacingY).toInt() + 2

        for (row in -1..rows) {
            for (col in -1..cols) {
                val offsetX = if (row % 2 == 0) 0f else spacingX / 2f
                val cx = col * spacingX + offsetX
                val cy = row * spacingY
                drawStarOfDavid(cx, cy, starSize / 2f, color)
            }
        }
    }
}

/** Draws a single Star of David (two overlapping triangles) at [cx],[cy] with [radius]. */
private fun DrawScope.drawStarOfDavid(cx: Float, cy: Float, radius: Float, color: Color) {
    val stroke = Stroke(width = 1.5.dp.toPx())

    // Upward-pointing triangle
    drawPath(
        path = equilateralTriangle(cx, cy, radius, pointUp = true),
        color = color,
        style = stroke
    )
    // Downward-pointing triangle
    drawPath(
        path = equilateralTriangle(cx, cy, radius, pointUp = false),
        color = color,
        style = stroke
    )
}

private fun equilateralTriangle(cx: Float, cy: Float, radius: Float, pointUp: Boolean): Path {
    val path = Path()
    val startAngle = if (pointUp) -90.0 else 90.0
    for (i in 0..2) {
        val angleDeg = startAngle + i * 120.0
        val angleRad = Math.toRadians(angleDeg)
        val x = cx + radius * cos(angleRad).toFloat()
        val y = cy + radius * sin(angleRad).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}
