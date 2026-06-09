package com.amn3zia.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────────────────────
// AMN3ZIA Premium colour palette
// Design language: Clean · Modern · Apple-like · Light
// ─────────────────────────────────────────────────────────────────────────────

object AmnColors {
    // Brand
    val Primary          = Color(0xFF4F7CFF)   // AMN3ZIA blue
    val PrimaryHover     = Color(0xFF628BFF)
    val PrimaryLight     = Color(0xFFE8EFFF)   // 10% primary tint

    // Backgrounds
    val Background       = Color(0xFFF7F9FC)   // Off-white canvas
    val Surface          = Color(0xFFFFFFFF)   // Pure white
    val SurfaceAlt       = Color(0xFFF2F5FA)   // Cards, hover states
    val SurfaceElevated  = Color(0xFFFFFFFF)

    // Text
    val TextPrimary      = Color(0xFF111827)   // Near-black
    val TextSecondary    = Color(0xFF6B7280)   // Gray
    val TextTertiary     = Color(0xFF9CA3AF)   // Hints
    val TextDisabled     = Color(0xFFD1D5DB)

    // Borders
    val Border           = Color(0xFFE5E7EB)
    val BorderLight      = Color(0xFFF3F4F6)

    // Semantic
    val Success          = Color(0xFF34C759)
    val Warning          = Color(0xFFFF9F0A)
    val Error            = Color(0xFFFF3B30)
    val Online           = Color(0xFF34C759)

    // Chat bubbles
    val BubbleMine       = Color(0xFF4F7CFF)   // Blue outgoing
    val BubbleMineGrad   = Color(0xFF628BFF)
    val BubbleOther      = Color(0xFFFFFFFF)   // White incoming
    val BubbleMineShadow = Color(0x1A4F7CFF)
    val BubbleOtherShadow= Color(0x14111827)

    // Chat text
    val TextBubbleMine   = Color(0xFFFFFFFF)
    val TextBubbleOther  = Color(0xFF111827)

    // Ticks / timestamps
    val SentTick         = Color(0xCCFFFFFF)
    val ReadTick         = Color(0xFFFFFFFF)
    val TimestampMine    = Color(0xCCFFFFFF)
    val TimestampOther   = Color(0xFF9CA3AF)

    // Badge
    val Badge            = Color(0xFF4F7CFF)
    val BadgeMuted       = Color(0xFFD1D5DB)

    // Input
    val InputBackground  = Color(0xFFF7F9FC)
    val InputBorder      = Color(0xFFE5E7EB)
    val InputBorderFocused = Color(0xFF4F7CFF)

    // Action bar (top bar)
    val ActionBar        = Color(0xFFFFFFFF)
    val ActionBarIcon    = Color(0xFF4F7CFF)
    val ActionBarTitle   = Color(0xFF111827)
    val ActionBarSubtitle= Color(0xFF6B7280)

    // Misc
    val Divider          = Color(0xFFE5E7EB)
    val Scrim            = Color(0x66111827)

    // Legacy alias so old code compiles (will migrate gradually)
    @Deprecated("Use AmnColors", replaceWith = ReplaceWith("Primary"))
    val Blue = Primary
    @Deprecated("Use AmnColors", replaceWith = ReplaceWith("Background"))
    val Bg = Background
    @Deprecated("Use AmnColors", replaceWith = ReplaceWith("Surface"))
    val BgSecondary = Surface
    @Deprecated("Use AmnColors", replaceWith = ReplaceWith("SurfaceAlt"))
    val BgTertiary = SurfaceAlt
}

// ─────────────────────────────────────────────────────────────────────────────
// Backward-compat alias used by existing screens (migrates in place)
// ─────────────────────────────────────────────────────────────────────────────
@Suppress("unused")
object TgColors {
    val Blue         get() = AmnColors.Primary
    val Bg           get() = AmnColors.Background
    val BgSecondary  get() = AmnColors.Surface
    val BgTertiary   get() = AmnColors.SurfaceAlt
    val TextPrimary  get() = AmnColors.TextPrimary
    val TextSecondary get() = AmnColors.TextSecondary
    val TextHint     get() = AmnColors.TextTertiary
    val Divider      get() = AmnColors.Border
    val BubbleMine   get() = AmnColors.BubbleMine
    val BubbleOther  get() = AmnColors.BubbleOther
    val Online       get() = AmnColors.Online
    val Red          get() = AmnColors.Error
    val PanicRed     get() = AmnColors.Error
    val Badge        get() = AmnColors.Badge
    val BadgeMuted   get() = AmnColors.BadgeMuted
    val SentTick     get() = AmnColors.SentTick
    val BlueLight    get() = AmnColors.PrimaryHover
}

// ─────────────────────────────────────────────────────────────────────────────
// Material 3 colour scheme
// ─────────────────────────────────────────────────────────────────────────────

private val amnColorScheme = lightColorScheme(
    primary              = AmnColors.Primary,
    onPrimary            = Color.White,
    primaryContainer     = AmnColors.PrimaryLight,
    onPrimaryContainer   = AmnColors.Primary,
    secondary            = AmnColors.TextSecondary,
    onSecondary          = Color.White,
    secondaryContainer   = AmnColors.SurfaceAlt,
    onSecondaryContainer = AmnColors.TextPrimary,
    tertiary             = AmnColors.Success,
    onTertiary           = Color.White,
    background           = AmnColors.Background,
    onBackground         = AmnColors.TextPrimary,
    surface              = AmnColors.Surface,
    onSurface            = AmnColors.TextPrimary,
    surfaceVariant       = AmnColors.SurfaceAlt,
    onSurfaceVariant     = AmnColors.TextSecondary,
    outline              = AmnColors.Border,
    outlineVariant       = AmnColors.BorderLight,
    error                = AmnColors.Error,
    onError              = Color.White,
    inverseSurface       = AmnColors.TextPrimary,
    inverseOnSurface     = Color.White,
    inversePrimary       = AmnColors.PrimaryHover,
    scrim                = AmnColors.Scrim,
)

// ─────────────────────────────────────────────────────────────────────────────
// Typography
// ─────────────────────────────────────────────────────────────────────────────

private val amnTypography = Typography(
    headlineLarge   = TextStyle(fontWeight = FontWeight.Bold,     fontSize = 28.sp),
    headlineMedium  = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    headlineSmall   = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    titleLarge      = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
    titleMedium     = TextStyle(fontWeight = FontWeight.Medium,   fontSize = 15.sp),
    bodyLarge       = TextStyle(fontWeight = FontWeight.Normal,   fontSize = 15.sp),
    bodyMedium      = TextStyle(fontWeight = FontWeight.Normal,   fontSize = 13.sp),
    bodySmall       = TextStyle(fontWeight = FontWeight.Normal,   fontSize = 11.sp),
    labelLarge      = TextStyle(fontWeight = FontWeight.Medium,   fontSize = 13.sp),
    labelMedium     = TextStyle(fontWeight = FontWeight.Medium,   fontSize = 11.sp),
    labelSmall      = TextStyle(fontWeight = FontWeight.Normal,   fontSize = 10.sp),
)

// ─────────────────────────────────────────────────────────────────────────────
// Theme entry point
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AmnTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = amnColorScheme,
        typography  = amnTypography,
        content     = content,
    )
}
