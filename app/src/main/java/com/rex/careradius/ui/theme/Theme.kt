package com.rex.careradius.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat


private val LightColorScheme = lightColorScheme(
    primary = UtilityBlueLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1E4FF), // Standard generated container
    onPrimaryContainer = Color(0xFF001D36),
    secondary = UtilityBlueLight, // Monochromatic accent
    onSecondary = Color.White,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    error = ErrorColor,
    onError = OnErrorColor
)

private val DarkColorScheme = darkColorScheme(
    primary = UtilityBlueDark,
    onPrimary = Color(0xFF002F52),
    primaryContainer = Color(0xFF004481),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = UtilityBlueDark, // Monochromatic accent
    onSecondary = Color(0xFF002F52),
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant, // Used for dialogs/modals in this system
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    error = DarkErrorColor,
    onError = DarkOnErrorColor
)

@Composable
fun CareRadiusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Nordic Utility: Strickly enforce our Neutral + Blue palette.
    // Dynamic Scalable Color is DISABLED to ensure the specific aesthetic.
    
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Ensure status bar icons contrast with our neutral backgrounds
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = Shapes, // 8dp global
        typography = Typography,
        content = content
    )
}