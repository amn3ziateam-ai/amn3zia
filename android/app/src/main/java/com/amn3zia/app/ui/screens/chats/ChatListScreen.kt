package com.amn3zia.app.ui.screens.chats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amn3zia.app.core.tdlib.ChatSummary
import com.amn3zia.app.ui.components.TgAvatar
import com.amn3zia.app.ui.theme.AmnColors

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
        containerColor = AmnColors.Background,
        topBar = {
            if (searchActive) {
                AmnSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onClose = { searchActive = false; searchQuery = "" },
                )
            } else {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = AmnColors.Surface,
                        titleContentColor = AmnColors.TextPrimary,
                        actionIconContentColor = AmnColors.ActionBarIcon,
                    ),
                    title = {
                        Column {
                            Text(
                                "AMN3ZIA",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = AmnColors.TextPrimary,
                            )
                            if (ghostMode) {
                                Text(
                                    "Ghost Mode",
                                    fontSize = 11.sp,
                                    color = AmnColors.Primary,
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { searchActive = true }) {
                            Icon(Icons.Filled.Search, "Search", tint = AmnColors.ActionBarIcon)
                        }
                        IconButton(onClick = onOpenHidden) {
                            Icon(Icons.Filled.Lock, "Hidden chats", tint = AmnColors.ActionBarIcon)
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Filled.Settings, "Settings", tint = AmnColors.ActionBarIcon)
                        }
                    },
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenPanic,
                containerColor = AmnColors.Error,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(56.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
            ) {
                Icon(Icons.Filled.Warning, "Panic", modifier = Modifier.size(22.dp))
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // Account switcher (multi-account)
            if (accountIds.size > 1) {
                AccountSwitcherRow(
                    accountIds = accountIds,
                    activeAccountId = activeAccountId,
                    onSwitch = viewModel::switchAccount,
                    onAddAccount = viewModel::addAccount,
                )
                HorizontalDivider(color = AmnColors.Divider, thickness = 0.5.dp)
            }

            // Chat list
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filtered, key = { it.id }) { chat ->
                    ChatRow(chat = chat, onClick = { onOpenChat(chat.id) })
                    HorizontalDivider(
                        color = AmnColors.Divider,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(start = 80.dp),
                    )
                }
                if (filtered.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 80.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                if (searchQuery.isNotBlank()) "Nothing found" else "No chats yet",
                                color = AmnColors.TextTertiary,
                                fontSize = 15.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Chat row ──────────────────────────────────────────────────────────────────

@Composable
private fun ChatRow(chat: ChatSummary, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AmnColors.Surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Avatar
        TgAvatar(title = chat.title, size = 52.dp)

        // Content
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = chat.title,
                    color = AmnColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp),
                )
                Text(
                    text = "now",
                    color = if (chat.unreadCount > 0) AmnColors.Primary else AmnColors.TextTertiary,
                    fontSize = 12.sp,
                )
            }
            Spacer(Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = chat.lastMessagePreview,
                    color = AmnColors.TextSecondary,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                )
                if (chat.unreadCount > 0) {
                    UnreadBadge(count = chat.unreadCount)
                }
            }
        }
    }
}

@Composable
private fun UnreadBadge(count: Int) {
    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(AmnColors.Badge)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

// ── Account switcher ──────────────────────────────────────────────────────────

@Composable
private fun AccountSwitcherRow(
    accountIds: List<String>,
    activeAccountId: String?,
    onSwitch: (String) -> Unit,
    onAddAccount: () -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().background(AmnColors.Surface),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(accountIds) { id ->
            FilterChip(
                selected = id == activeAccountId,
                onClick = { onSwitch(id) },
                label = { Text(id.take(8), fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AmnColors.Primary,
                    selectedLabelColor = Color.White,
                    containerColor = AmnColors.SurfaceAlt,
                    labelColor = AmnColors.TextSecondary,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = id == activeAccountId,
                    borderColor = AmnColors.Border,
                    selectedBorderColor = AmnColors.Primary,
                ),
            )
        }
        item {
            AssistChip(
                onClick = onAddAccount,
                label = { Text("+ Add account", fontSize = 12.sp) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = AmnColors.PrimaryLight,
                    labelColor = AmnColors.Primary,
                ),
                border = AssistChipDefaults.assistChipBorder(
                    enabled = true,
                    borderColor = AmnColors.Primary,
                ),
            )
        }
    }
}

// ── Search bar ────────────────────────────────────────────────────────────────

@Composable
private fun AmnSearchBar(query: String, onQueryChange: (String) -> Unit, onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AmnColors.Surface)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Close", tint = AmnColors.Primary)
        }
        TextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search", color = AmnColors.TextTertiary) },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = AmnColors.TextPrimary,
                unfocusedTextColor = AmnColors.TextPrimary,
                cursorColor = AmnColors.Primary,
            ),
            modifier = Modifier.weight(1f),
        )
        if (query.isNotEmpty()) {
            IconButton(onClick = { onQueryChange("") }) {
                Icon(Icons.Filled.Close, "Clear", tint = AmnColors.TextSecondary)
            }
        }
    }
}
