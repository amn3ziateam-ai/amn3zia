package com.amn3zia.app.ui.screens.chats

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amn3zia.app.core.tdlib.ChatSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(onOpenChat: (Long) -> Unit, onOpenSettings: () -> Unit, onOpenPanic: () -> Unit) {
    val viewModel: ChatListViewModel = viewModel()
    val chats by viewModel.chats.collectAsState()
    val ghostMode by viewModel.ghostModeEnabled.collectAsState()
    val accountIds by viewModel.accountIds.collectAsState()
    val activeAccountId by viewModel.activeAccountId.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AMN3ZIA") },
                actions = {
                    IconButton(onClick = viewModel::toggleGhostMode) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Filled.Visibility,
                            contentDescription = "Ghost mode",
                            tint = if (ghostMode) Color(0xFF7C5CFF) else Color.Gray,
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        floatingActionButton = {
            // Bottom-left placement requirement is honored via FabPosition.Start
            ExtendedFloatingActionButton(
                onClick = onOpenPanic,
                containerColor = Color(0xFFE6294B),
                contentColor = Color.White,
                icon = { Icon(Icons.Filled.Warning, contentDescription = null) },
                text = { Text("Panic") },
            )
        },
        floatingActionButtonPosition = FabPosition.Start,
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            AccountSwitcherRow(
                accountIds = accountIds,
                activeAccountId = activeAccountId,
                onSwitch = viewModel::switchAccount,
                onAddAccount = viewModel::addAccount,
            )
            if (ghostMode) GhostModeBanner()
            LazyColumn {
                items(chats, key = { it.id }) { chat ->
                    ChatRow(chat = chat, onClick = { onOpenChat(chat.id) })
                    HorizontalDivider(color = Color(0xFF1F1F28))
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
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        accountIds.forEach { id ->
            FilterChip(
                selected = id == activeAccountId,
                onClick = { onSwitch(id) },
                label = { Text(id.take(6)) },
            )
        }
        AssistChip(onClick = onAddAccount, label = { Text("+ Add account") })
    }
}

@Composable
private fun GhostModeBanner() {
    Surface(color = Color(0xFF231F36), modifier = Modifier.fillMaxWidth()) {
        Text(
            "👻 Ghost Mode active — typing & read receipts are hidden/delayed",
            color = Color(0xFFB9A6FF),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun ChatRow(chat: ChatSummary, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(chat.title, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Text(chat.lastMessagePreview, color = Color.Gray, maxLines = 1, style = MaterialTheme.typography.bodySmall)
        }
        if (chat.unreadCount > 0) {
            Badge(containerColor = Color(0xFF7C5CFF)) { Text(chat.unreadCount.toString()) }
        }
    }
}
