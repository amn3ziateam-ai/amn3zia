package com.amn3zia.app.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import com.amn3zia.app.ui.theme.AmnColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(chatId: Long, onBack: () -> Unit) {
    val viewModel: ChatViewModel = viewModel()
    LaunchedEffect(chatId) { viewModel.open(chatId) }

    val messages  by viewModel.messages.collectAsState()
    val draft     by viewModel.draft.collectAsState()
    val chatTitle by viewModel.chatTitle.collectAsState()

    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Scaffold(
        containerColor = AmnColors.Background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor         = AmnColors.Surface,
                    titleContentColor      = AmnColors.TextPrimary,
                    navigationIconContentColor = AmnColors.Primary,
                    actionIconContentColor = AmnColors.TextSecondary,
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = AmnColors.Primary)
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        TgAvatar(title = chatTitle, size = 36.dp)
                        Column {
                            Text(chatTitle, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = AmnColors.TextPrimary)
                            Text("last seen recently", fontSize = 12.sp, color = AmnColors.Primary)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Filled.Phone, "Call", tint = AmnColors.Primary)
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Filled.MoreVert, "More", tint = AmnColors.TextSecondary)
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
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(AmnColors.Background)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            items(messages, key = { it.id }) { message ->
                MessageBubble(message = message)
                Spacer(Modifier.height(2.dp))
            }
        }
    }
}

// ── Message bubble ─────────────────────────────────────────────────────────────

@Composable
private fun MessageBubble(message: MessageItem) {
    val isOut = message.isOutgoing
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
        horizontalArrangement = if (isOut) Arrangement.End else Arrangement.Start,
    ) {
        if (!isOut) Spacer(Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart    = if (isOut) 16.dp else 4.dp,
                        topEnd      = if (isOut) 4.dp else 16.dp,
                        bottomStart = 16.dp,
                        bottomEnd   = 16.dp,
                    )
                )
                .background(if (isOut) AmnColors.BubbleMine else AmnColors.BubbleOther)
                .then(
                    if (!isOut) Modifier.padding(0.dp)  // shadow via elevation
                    else Modifier
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Column {
                Text(
                    text = message.text,
                    color = if (isOut) AmnColors.TextBubbleMine else AmnColors.TextBubbleOther,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                )
                Spacer(Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(
                        text = "now",
                        color = if (isOut) AmnColors.TimestampMine else AmnColors.TimestampOther,
                        fontSize = 11.sp,
                    )
                    if (isOut) {
                        Spacer(Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.Filled.DoneAll,
                            contentDescription = null,
                            tint = AmnColors.ReadTick,
                            modifier = Modifier.size(14.dp).align(Alignment.CenterVertically),
                        )
                    }
                }
            }
        }

        if (isOut) Spacer(Modifier.width(8.dp))
    }
}

// ── Composer ───────────────────────────────────────────────────────────────────

@Composable
private fun ChatComposer(draft: String, onDraftChanged: (String) -> Unit, onSend: () -> Unit) {
    Surface(
        color = AmnColors.Surface,
        tonalElevation = 0.dp,
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Attach / emoji
            IconButton(onClick = {}) {
                Icon(Icons.Filled.EmojiEmotions, "Emoji", tint = AmnColors.TextSecondary)
            }

            // Text field
            TextField(
                value = draft,
                onValueChange = onDraftChanged,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp)),
                placeholder = { Text("Message", color = AmnColors.TextTertiary, fontSize = 15.sp) },
                maxLines = 6,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor   = AmnColors.InputBackground,
                    unfocusedContainerColor = AmnColors.InputBackground,
                    focusedIndicatorColor   = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor        = AmnColors.TextPrimary,
                    unfocusedTextColor      = AmnColors.TextPrimary,
                    cursorColor             = AmnColors.Primary,
                ),
            )

            // Send / mic button
            val canSend = draft.isNotBlank()
            IconButton(
                onClick = { if (canSend) onSend() },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (canSend) AmnColors.Primary else Color.Transparent),
            ) {
                Icon(
                    imageVector = if (canSend) Icons.AutoMirrored.Filled.Send else Icons.Filled.Mic,
                    contentDescription = "Send",
                    tint = if (canSend) Color.White else AmnColors.TextSecondary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}
