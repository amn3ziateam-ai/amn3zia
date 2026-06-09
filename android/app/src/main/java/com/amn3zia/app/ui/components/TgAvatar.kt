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

// Telegram-style avatar palette — each chat/user gets a deterministic color
private val avatarColors = listOf(
    Color(0xFF2CA5E0), Color(0xFF45B2C4), Color(0xFF7B6FCF),
    Color(0xFF3D9F69), Color(0xFFD46B7D), Color(0xFF5B7EBF),
    Color(0xFFE07B39), Color(0xFF9E6BAD),
)

private fun avatarColor(title: String): Color =
    avatarColors[(title.firstOrNull()?.code ?: 0) % avatarColors.size]

private fun initials(title: String): String {
    val words = title.trim().split(" ").filter { it.isNotEmpty() }
    return when {
        words.size >= 2 -> "${words[0].first().uppercaseChar()}${words[1].first().uppercaseChar()}"
        words.size == 1 && words[0].length >= 2 -> words[0].take(2).uppercase()
        words.size == 1 -> words[0].first().uppercaseChar().toString()
        else -> "?"
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
