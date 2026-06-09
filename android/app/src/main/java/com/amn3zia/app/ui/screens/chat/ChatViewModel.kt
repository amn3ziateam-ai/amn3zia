package com.amn3zia.app.ui.screens.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.amn3zia.app.AmnApplication
import com.amn3zia.app.core.account.AccountSession
import com.amn3zia.app.core.tdlib.MessageItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val app get() = AmnApplication.from(getApplication())
    private val session: AccountSession get() = requireNotNull(app.accounts.activeSession())

    private var chatId: Long = 0L

    private val _messages = MutableStateFlow<List<MessageItem>>(emptyList())
    val messages: StateFlow<List<MessageItem>> = _messages.asStateFlow()

    private val _draft = MutableStateFlow("")
    val draft: StateFlow<String> = _draft.asStateFlow()

    private val _chatTitle = MutableStateFlow("Chat")
    val chatTitle: StateFlow<String> = _chatTitle.asStateFlow()

    fun open(chatId: Long) {
        if (this.chatId == chatId) return
        this.chatId = chatId
        loadHistory()
        observeIncoming()
    }

    private fun loadHistory() = viewModelScope.launch {
        runCatching { session.chats.loadHistory(chatId) }
            .onSuccess { history ->
                _messages.value = history.sortedBy { it.date }
                scheduleReadMarking(history)
            }
    }

    private fun observeIncoming() = viewModelScope.launch {
        session.chats.newMessages.collect { update ->
            if (update.message.chatId == chatId) {
                _messages.value = (_messages.value + update.message.toItem()).sortedBy { it.date }
                scheduleReadMarking(_messages.value.takeLast(1))
            }
        }
    }

    /**
     * Read receipts always go through GhostModeManager via the PrivacyInterceptor —
     * we just call markMessagesRead and the interceptor decides whether to send it
     * now, delay it (Ghost Mode "delayed read marking"), or suppress it entirely.
     */
    private fun scheduleReadMarking(items: List<MessageItem>) {
        val incomingIds = items.filter { !it.isOutgoing }.map { it.id }.toLongArray()
        if (incomingIds.isEmpty()) return
        viewModelScope.launch {
            runCatching { session.chats.markMessagesRead(chatId, incomingIds) }
        }
    }

    fun onDraftChanged(text: String) {
        _draft.value = text
        viewModelScope.launch { runCatching { session.chats.setTyping(chatId, text.isNotBlank()) } }
    }

    fun send() {
        val text = _draft.value.trim()
        if (text.isEmpty()) return
        _draft.value = ""
        viewModelScope.launch {
            runCatching { session.chats.sendTextMessage(chatId, text) }
            runCatching { session.chats.setTyping(chatId, false) }
        }
    }
}

private fun TdApi.Message.toItem(): MessageItem {
    val text = (content as? TdApi.MessageText)?.text?.text ?: ""
    return MessageItem(id = id, chatId = chatId, senderName = "", text = text, date = date, isOutgoing = isOutgoing)
}
