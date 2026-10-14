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
    primary = PrimaryViolet,
    secondary = CyberCyan,
    tertiary = PrimaryLightViolet,
    background = DeepIndigoBg,
    surface = CardSurfaceDark,
    surfaceVariant = Color(0xFF23223A),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = Color(0xFF373554)
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryViolet,
    secondary = CyberCyan,
    tertiary = PrimaryLightViolet,
    background = Color(0xFFF8FAFC),
    surface = CardSurfaceLight,
    surfaceVariant = Color(0xFFF1F5F9),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = Color(0xFFCBD5E1)
)

@Composable
fun LifeMateTheme(
    themeMode: String = "system",
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

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
