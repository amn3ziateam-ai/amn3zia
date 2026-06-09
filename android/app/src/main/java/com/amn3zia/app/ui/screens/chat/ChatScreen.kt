package com.amn3zia.app.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amn3zia.app.core.tdlib.MessageItem
import com.amn3zia.app.ui.components.TgAvatar
import com.amn3zia.app.ui.theme.TgColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(chatId: Long, onBack: () -> Unit) {
    val viewModel: ChatViewModel = viewModel()
    LaunchedEffect(chatId) { viewModel.open(chatId) }

    val messages by viewModel.messages.collectAsState()
    val draft by viewModel.draft.collectAsState()
    val chatTitle by viewModel.chatTitle.collectAsState()

    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Scaffold(
        containerColor = TgColors.Bg,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TgColors.BgSecondary,
                    titleContentColor = TgColors.TextPrimary,
                    navigationIconContentColor = TgColors.Blue,
                    actionIconContentColor = TgColors.TextSecondary,
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TgColors.Blue)
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TgAvatar(title = chatTitle, size = 36.dp)
                        Column {
                            Text(chatTitle, fontWeight = FontWeight.Medium, fontSize = 16.sp, color = TgColors.TextPrimary)
                            Text("last seen recently", fontSize = 12.sp, color = TgColors.TextSecondary)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Filled.Search, contentDescription = "Search", tint = TgColors.TextSecondary)
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = TgColors.TextSecondary)
                    }
                },
            )
        },
        bottomBar = {
            ChatComposer(
                draft = draft,
                onDraftChanged = viewModel::onDraftChanged,
                onSend = viewModel::send,
            )
        },
    ) { padding ->
        // Chat wallpaper-style background
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(TgColors.Bg),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                items(messages, key = { it.id }) { message ->
                    MessageBubble(message = message)
                    Spacer(Modifier.height(2.dp))
                }
            }
        }
    }
}

// ── Message bubble ────────────────────────────────────────────────────────────

@Composable
private fun MessageBubble(message: MessageItem) {
    val isOut = message.isOutgoing
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
        horizontalArrangement = if (isOut) Arrangement.End else Arrangement.Start,
    ) {
        if (!isOut) {
            Spacer(Modifier.width(8.dp))
        }
        Column(
            horizontalAlignment = if (isOut) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp),
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = if (isOut) 16.dp else 4.dp,
                            topEnd = if (isOut) 4.dp else 16.dp,
                            bottomStart = 16.dp,
                            bottomEnd = 16.dp,
                        )
                    )
                    .background(if (isOut) TgColors.BubbleMine else TgColors.BubbleOther)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Column {
                    Text(
                        text = message.text,
                        color = TgColors.TextPrimary,
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text("now", color = TgColors.TextSecondary, fontSize = 11.sp)
                        if (isOut) {
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Filled.DoneAll,
                                contentDescription = null,
                                tint = TgColors.SentTick,
                                modifier = Modifier.size(14.dp).align(Alignment.CenterVertically),
                            )
                        }
                    }
                }
            }
        }
        if (isOut) {
            Spacer(Modifier.width(8.dp))
        }
    }
}

// ── Composer ──────────────────────────────────────────────────────────────────

@Composable
private fun ChatComposer(draft: String, onDraftChanged: (String) -> Unit, onSend: () -> Unit) {
    Surface(color = TgColors.BgSecondary, tonalElevation = 0.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Emoji / attach
            IconButton(onClick = {}) {
                Icon(Icons.Filled.EmojiEmotions, contentDescription = "Emoji", tint = TgColors.TextSecondary)
            }

            // Text field
            TextField(
                value = draft,
                onValueChange = onDraftChanged,
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(24.dp)),
                placeholder = { Text("Message", color = TgColors.TextHint, fontSize = 15.sp) },
                maxLines = 6,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = TgColors.BgTertiary,
                    unfocusedContainerColor = TgColors.BgTertiary,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = TgColors.TextPrimary,
                    unfocusedTextColor = TgColors.TextPrimary,
                    cursorColor = TgColors.Blue,
                ),
            )

            // Send / microphone
            val canSend = draft.isNotBlank()
            IconButton(
                onClick = { if (canSend) onSend() },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (canSend) TgColors.Blue else Color.Transparent),
            ) {
                Icon(
                    imageVector = if (canSend) Icons.AutoMirrored.Filled.Send else Icons.Filled.Mic,
                    contentDescription = "Send",
                    tint = if (canSend) Color.White else TgColors.TextSecondary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}
