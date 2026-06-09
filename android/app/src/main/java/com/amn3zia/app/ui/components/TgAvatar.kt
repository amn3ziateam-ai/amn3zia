package com.amn3zia.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Deterministic Telegram-style avatar colours (light-friendly, saturated)
private val AVATAR_COLORS = listOf(
    Color(0xFF4F7CFF),  // AMN3ZIA blue
    Color(0xFF34C9B7),  // teal
    Color(0xFF34C759),  // green
    Color(0xFFFF9F0A),  // orange
    Color(0xFF7C4DFF),  // violet
    Color(0xFFFF4FA3),  // pink
    Color(0xFFFF5722),  // deep orange
    Color(0xFF00BCD4),  // cyan
    Color(0xFF4CAF50),  // mid-green
    Color(0xFF9C27B0),  // purple
)

private fun avatarColor(title: String): Color =
    AVATAR_COLORS[(title.firstOrNull()?.code ?: 0) % AVATAR_COLORS.size]

private fun initials(title: String): String {
    val words = title.trim().split(" ").filter { it.isNotEmpty() }
    return when {
        words.size >= 2       -> "${words[0].first().uppercaseChar()}${words[1].first().uppercaseChar()}"
        words.size == 1 && words[0].length >= 2 -> words[0].take(2).uppercase()
        words.size == 1       -> words[0].first().uppercaseChar().toString()
        else                  -> "?"
    }
}

@Composable
fun TgAvatar(title: String, size: Dp = 52.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(avatarColor(title)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials(title),
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = (size.value * 0.34f).sp,
        )
    }
}
