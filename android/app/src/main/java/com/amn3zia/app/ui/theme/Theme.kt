package com.amn3zia.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Background = Color(0xFF0A0A0F)
private val Surface = Color(0xFF15151C)
private val Accent = Color(0xFF7C5CFF)
private val PanicRed = Color(0xFFE6294B)

private val AmnColorScheme = darkColorScheme(
    background = Background,
    surface = Surface,
    primary = Accent,
    secondary = Accent,
    error = PanicRed,
    onBackground = Color(0xFFEDEDF2),
    onSurface = Color(0xFFEDEDF2),
)

/** Dark theme is the only theme — privacy-first apps default to dark, no light variant offered. */
@Composable
fun AmnTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AmnColorScheme, content = content)
}
