package com.foxium.browser.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val FirefoxDarkColorScheme = darkColorScheme(
    primary = FirefoxOrange,
    secondary = FirefoxPurple,
    tertiary = FirefoxCyan,
    background = FirefoxDeepBg,
    surface = FirefoxSurface,
    surfaceVariant = FirefoxSurfaceVariant,
    onPrimary = FirefoxTextPrimary,
    onSecondary = FirefoxTextPrimary,
    onBackground = FirefoxTextPrimary,
    onSurface = FirefoxTextPrimary,
    outline = FirefoxBorder
)

@Composable
fun FoxiumTheme(
    isPrivateMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (isPrivateMode) {
        FirefoxDarkColorScheme.copy(
            primary = FirefoxPrivatePurple,
            secondary = FirefoxFlame,
            background = FirefoxPrivateBg,
            surface = FirefoxSurfaceVariant,
            outline = FirefoxPrivatePurple.copy(alpha = 0.4f)
        )
    } else {
        FirefoxDarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
