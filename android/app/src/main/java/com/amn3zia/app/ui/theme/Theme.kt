package com.amn3zia.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ─── Telegram / AyuGram colour palette ───────────────────────────────────────
object TgColors {
    val Bg            = Color(0xFF17212B)   // main background
    val BgSecondary   = Color(0xFF232E3C)   // cards, sheets, input fields
    val BgTertiary    = Color(0xFF1C2733)   // subtle separators
    val Blue          = Color(0xFF2AABEE)   // primary accent / links
    val BlueLight     = Color(0xFF40B3F0)
    val TextPrimary   = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFF8B9EAB)   // timestamps, subtitles
    val TextHint      = Color(0xFF566677)   // placeholder
    val Divider       = Color(0xFF0D1117)
    val BubbleMine    = Color(0xFF2B5278)   // outgoing message bubble
    val BubbleOther   = Color(0xFF182533)   // incoming message bubble
    val Online        = Color(0xFF4DCD5E)
    val Red           = Color(0xFFE53935)
    val PanicRed      = Color(0xFFE6294B)
    val Badge         = Color(0xFF2AABEE)
    val BadgeMuted    = Color(0xFF4A5568)
    val SentTick      = Color(0xFF40B3F0)   // ✓ / ✓✓ ticks
}

private val TelegramDarkScheme = darkColorScheme(
    background       = TgColors.Bg,
    surface          = TgColors.BgSecondary,
    surfaceVariant   = TgColors.BgTertiary,
    primary          = TgColors.Blue,
    primaryContainer = TgColors.BgSecondary,
    secondary        = TgColors.TextSecondary,
    error            = TgColors.Red,
    onBackground     = TgColors.TextPrimary,
    onSurface        = TgColors.TextPrimary,
    onSurfaceVariant = TgColors.TextSecondary,
    onPrimary        = TgColors.TextPrimary,
    outline          = TgColors.TextHint,
)

@Composable
fun AmnTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = TelegramDarkScheme, content = content)
}
