package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldLight,
    onPrimary = EmeraldDark,
    primaryContainer = EmeraldPrimary,
    onPrimaryContainer = EmeraldContainer,
    secondary = SlateLight,
    onSecondary = NavyDark,
    tertiary = GoldAccent,
    background = NavyDark,
    surface = NavySecondary,
    onBackground = SlateLight,
    onSurface = SlateLight
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = EmeraldDark,
    secondary = NavySecondary,
    onSecondary = Color.White,
    secondaryContainer = SlateLight,
    onSecondaryContainer = NavyDark,
    tertiary = GoldAccent,
    tertiaryContainer = GoldContainer,
    onTertiaryContainer = Color(0xFF78350F),
    background = NeutralBackground,
    onBackground = NavyDark,
    surface = SurfacePure,
    onSurface = NavyDark,
    surfaceVariant = SlateLight,
    onSurfaceVariant = SlateTextSecondary,
    outline = SlateBorder,
    error = RedAlert,
    errorContainer = RedContainer
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our handcrafted Lebanese POS palette
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
