package com.amn3zia.app.ui.screens.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amn3zia.app.core.tdlib.MessageItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(chatId: Long, onBack: () -> Unit) {
    val viewModel: ChatViewModel = viewModel()
    LaunchedEffect(chatId) { viewModel.open(chatId) }

    val messages by viewModel.messages.collectAsState()
    val draft by viewModel.draft.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
        bottomBar = { Composer(draft = draft, onDraftChanged = viewModel::onDraftChanged, onSend = viewModel::send) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 12.dp),
            reverseLayout = false,
        ) {
            items(messages, key = { it.id }) { message ->
                MessageBubble(message)
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun MessageBubble(message: MessageItem) {
    val alignment = if (message.isOutgoing) Alignment.CenterEnd else Alignment.CenterStart
    val color = if (message.isOutgoing) Color(0xFF7C5CFF) else Color(0xFF1F1F28)
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Surface(color = color, shape = RoundedCornerShape(14.dp)) {
            Text(message.text, color = Color.White, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
        }
    }
}

@Composable
private fun Composer(draft: String, onDraftChanged: (String) -> Unit, onSend: () -> Unit) {
    Surface(color = Color(0xFF101016)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChanged,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message") },
                maxLines = 4,
            )
            IconButton(onClick = onSend, enabled = draft.isNotBlank()) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
            }
        }
    }
}
