package com.amn3zia.app.ui.screens.chats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amn3zia.app.core.tdlib.ChatSummary
import com.amn3zia.app.ui.components.TgAvatar
import com.amn3zia.app.ui.theme.TgColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    onOpenChat: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPanic: () -> Unit,
    onOpenHidden: () -> Unit = {},
) {
    val viewModel: ChatListViewModel = viewModel()
    val chats by viewModel.chats.collectAsState()
    val ghostMode by viewModel.ghostModeEnabled.collectAsState()
    val accountIds by viewModel.accountIds.collectAsState()
    val activeAccountId by viewModel.activeAccountId.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var searchActive by remember { mutableStateOf(false) }

    val filtered = if (searchQuery.isBlank()) chats
    else chats.filter { it.title.contains(searchQuery, ignoreCase = true) }

    Scaffold(
        containerColor = TgColors.Bg,
        topBar = {
            if (searchActive) {
                TgSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onClose = { searchActive = false; searchQuery = "" },
                )
            } else {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = TgColors.Bg,
                        titleContentColor = TgColors.TextPrimary,
                        actionIconContentColor = TgColors.TextSecondary,
                    ),
                    title = {
                        Column {
                            Text("AMN3ZIA", fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = TgColors.TextPrimary)
                            if (ghostMode) Text("Ghost Mode", fontSize = 11.sp, color = TgColors.Blue)
                        }
                    },
                    actions = {
                        IconButton(onClick = { searchActive = true }) {
                            Icon(Icons.Filled.Search, contentDescription = "Search", tint = TgColors.TextSecondary)
                        }
                        IconButton(onClick = onOpenHidden) {
                            Icon(Icons.Filled.Lock, contentDescription = "Hidden chats", tint = TgColors.TextSecondary)
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = TgColors.TextSecondary)
                        }
                    },
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenPanic,
                containerColor = TgColors.PanicRed,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(56.dp),
            ) {
                Icon(Icons.Filled.Warning, contentDescription = "Panic", modifier = Modifier.size(24.dp))
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (accountIds.size > 1) {
                AccountSwitcherRow(
                    accountIds = accountIds,
                    activeAccountId = activeAccountId,
                    onSwitch = viewModel::switchAccount,
                    onAddAccount = viewModel::addAccount,
                )
                HorizontalDivider(color = TgColors.Divider, thickness = 0.5.dp)
            }
            if (ghostMode) GhostModeBanner()

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filtered, key = { it.id }) { chat ->
                    ChatRow(chat = chat, onClick = { onOpenChat(chat.id) })
                    HorizontalDivider(
                        color = TgColors.Divider,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(start = 80.dp),
                    )
                }
                if (filtered.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(top = 80.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                if (searchQuery.isNotBlank()) "Nothing found" else "No chats yet",
                                color = TgColors.TextHint,
                                fontSize = 15.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatRow(chat: ChatSummary, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TgAvatar(title = chat.title, size = 52.dp)
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = chat.title,
                    color = TgColors.TextPrimary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp),
                )
                Text("now", color = if (chat.unreadCount > 0) TgColors.Blue else TgColors.TextHint, fontSize = 12.sp)
            }
            Spacer(Modifier.height(3.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = chat.lastMessagePreview,
                    color = TgColors.TextSecondary,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                )
                if (chat.unreadCount > 0) {
                    Badge(
                        containerColor = TgColors.Badge,
                        contentColor = Color.White,
                        modifier = Modifier.defaultMinSize(minWidth = 20.dp, minHeight = 20.dp),
                    ) {
                        Text(if (chat.unreadCount > 99) "99+" else chat.unreadCount.toString(), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountSwitcherRow(
    accountIds: List<String>,
    activeAccountId: String?,
    onSwitch: (String) -> Unit,
    onAddAccount: () -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().background(TgColors.BgSecondary),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(accountIds) { id ->
            FilterChip(
                selected = id == activeAccountId,
                onClick = { onSwitch(id) },
                label = { Text(id.take(6), fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = TgColors.Blue,
                    selectedLabelColor = Color.White,
                    containerColor = TgColors.BgTertiary,
                    labelColor = TgColors.TextSecondary,
                ),
                border = null,
            )
        }
        item {
            AssistChip(
                onClick = onAddAccount,
                label = { Text("+ Add", fontSize = 12.sp) },
                colors = AssistChipDefaults.assistChipColors(containerColor = TgColors.BgTertiary, labelColor = TgColors.Blue),
                border = null,
            )
        }
    }
}

@Composable
private fun TgSearchBar(query: String, onQueryChange: (String) -> Unit, onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(TgColors.Bg).padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close", tint = TgColors.Blue)
        }
        TextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search", color = TgColors.TextHint) },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = TgColors.TextPrimary,
                unfocusedTextColor = TgColors.TextPrimary,
                cursorColor = TgColors.Blue,
            ),
            modifier = Modifier.weight(1f),
        )
        if (query.isNotEmpty()) {
            IconButton(onClick = { onQueryChange("") }) {
                Icon(Icons.Filled.Close, contentDescription = "Clear", tint = TgColors.TextSecondary)
            }
        }
    }
}

@Composable
private fun GhostModeBanner() {
    Row(
        modifier = Modifier.fillMaxWidth().background(Color(0xFF1A2533)).padding(horizontal = 16.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Filled.VisibilityOff, contentDescription = null, tint = TgColors.Blue, modifier = Modifier.size(16.dp))
        Text("Ghost Mode — typing & read receipts hidden", color = TgColors.Blue, fontSize = 12.sp)
    }
}
