package com.neviim.market.ui.theme

import android.app.Activity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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

// ── Jewish / Israeli theme schemes ───────────────────────────────────
//
// Israeli flag colours (exact PMS 286 C):
//   White  = #FFFFFF  (flag background)
//   Blue   = #0038A8  (flag stripe / Star of David)
//
// Light mode:  pure white surfaces, Israeli blue primary
// Dark  mode:  deep navy background, Israeli blue primary (lightened for contrast)

private val JewishLightColorScheme = lightColorScheme(
    primary               = IsraeliBlue,          // #0038A8
    onPrimary             = Color.White,
    primaryContainer      = IsraeliBluePale,       // very pale blue tint
    onPrimaryContainer    = IsraeliBlueDeep,
    secondary             = IsraeliBlueLight,
    onSecondary           = Color.White,
    secondaryContainer    = IsraeliBluePale,
    onSecondaryContainer  = IsraeliBlueDeep,
    background            = Color.White,           // pure flag white
    onBackground          = Color(0xFF001050),     // very dark navy text
    surface               = Color.White,
    onSurface             = Color(0xFF001050),
    surfaceVariant        = Color(0xFFE8EEFF),     // subtle blue-white tint
    onSurfaceVariant      = Color(0xFF2040A0),
    error                 = RedLoss,
    onError               = Color.White,
    outline               = Color(0xFFB0C4FF)
)

private val JewishDarkColorScheme = darkColorScheme(
    primary               = Color(0xFF6699FF),     // lightened Israeli blue for dark contrast
    onPrimary             = Color.White,
    primaryContainer      = IsraeliBlueDeep,
    onPrimaryContainer    = IsraeliBluePale,
    secondary             = Color(0xFF6699FF),
    onSecondary           = Color.White,
    secondaryContainer    = IsraeliBlueDeep,
    onSecondaryContainer  = IsraeliBluePale,
    background            = Color(0xFF000D2E),     // very dark navy — feel of night sky on flag
    onBackground          = Color(0xFFE8EEFF),
    surface               = Color(0xFF001550),     // deep navy surface
    onSurface             = Color(0xFFE8EEFF),
    surfaceVariant        = Color(0xFF0A2070),
    onSurfaceVariant      = Color(0xFFB0C4FF),
    error                 = RedLoss,
    onError               = Color.White,
    outline               = Color(0xFF1A3080)
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
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                !darkTheme || (isJewish && !darkTheme)
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NeviimTypography,
        content = content
    )
}

// ── Star of David decorative pattern ─────────────────────────────────
//
// IMPORTANT: This composable uses Box + fillMaxSize to overlay behind content.
// Always wrap the call-site in a Box, or use the JewishThemedBackground
// helper below which does this correctly.
//
// Usage:
//   Box(Modifier.fillMaxSize()) {
//       YourContent()
//       if (isJewish) StarOfDavidPattern()
//   }
//
// Or use JewishThemedBackground { YourContent() }

@Composable
fun StarOfDavidPattern(
    color: Color = IsraeliBlue.copy(alpha = 0.05f),
    modifier: Modifier = Modifier.fillMaxSize()
) {
    Canvas(modifier = modifier) {
        val starRadius = 28.dp.toPx()
        val spacingX   = starRadius * 4.0f
        val spacingY   = starRadius * 3.5f
        val cols = (size.width  / spacingX).toInt() + 2
        val rows = (size.height / spacingY).toInt() + 2

        for (row in -1..rows) {
            for (col in -1..cols) {
                val offsetX = if (row % 2 == 0) 0f else spacingX / 2f
                val cx = col * spacingX + offsetX
                val cy = row * spacingY
                drawStarOfDavid(cx, cy, starRadius, color)
            }
        }
    }
}

/**
 * Wraps [content] in a Box with a Star of David pattern behind it.
 * Only renders the pattern when the Jewish theme is active — safe to
 * call unconditionally (it's a no-op for other themes).
 */
@Composable
fun JewishThemedBackground(modifier: Modifier = Modifier.fillMaxSize(), content: @Composable () -> Unit) {
    val themeMode by SettingsRepository.themeMode.collectAsState()
    val isJewish = themeMode == SettingsRepository.ThemeMode.JEWISH

    if (isJewish) {
        Box(modifier = modifier) {
            content()
            // Pattern drawn on top at very low alpha — visually behind interactive elements
            // because it's non-clickable and drawn over a solid background.
            StarOfDavidPattern(color = IsraeliBlue.copy(alpha = 0.045f))
        }
    } else {
        Box(modifier = modifier) { content() }
    }
}

// ── Private drawing helpers ───────────────────────────────────────────

private fun DrawScope.drawStarOfDavid(cx: Float, cy: Float, radius: Float, color: Color) {
    val strokeWidth = Stroke(width = 1.2.dp.toPx())
    drawPath(equilateralTriangle(cx, cy, radius, pointUp = true),  color = color, style = strokeWidth)
    drawPath(equilateralTriangle(cx, cy, radius, pointUp = false), color = color, style = strokeWidth)
}

private fun equilateralTriangle(cx: Float, cy: Float, radius: Float, pointUp: Boolean): Path {
    val path = Path()
    val startAngle = if (pointUp) -90.0 else 90.0
    for (i in 0..2) {
        val rad = Math.toRadians(startAngle + i * 120.0)
        val x = cx + radius * cos(rad).toFloat()
        val y = cy + radius * sin(rad).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}
