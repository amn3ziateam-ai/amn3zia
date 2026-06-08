package com.amn3zia.app.core.tdlib

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import org.drinkless.tdlib.TdApi

data class ChatSummary(
    val id: Long,
    val title: String,
    val lastMessagePreview: String,
    val unreadCount: Int,
    val isMuted: Boolean,
    val lastMessageDate: Int,
)

data class MessageItem(
    val id: Long,
    val chatId: Long,
    val senderName: String,
    val text: String,
    val date: Int,
    val isOutgoing: Boolean,
)

/**
 * Real chat-list and messaging operations against TDLib for one account.
 * UI never talks to [TdClient] directly — it goes through repositories so the
 * Privacy Layer's interception (link previews, read receipts, typing) is
 * always in the path.
 */
class ChatRepository(private val client: TdClient) {

    val newMessages: Flow<TdApi.UpdateNewMessage> =
        client.updates.filterIsInstance()

    val chatUpdates: Flow<TdApi.Object> = client.updates

    suspend fun loadChatList(limit: Int = 50): List<ChatSummary> {
        client.send(TdApi.LoadChats(TdApi.ChatListMain(), limit))
        val chats = client.send(TdApi.GetChats(TdApi.ChatListMain(), limit))
        return chats.chatIds.toList().mapNotNull { id ->
            runCatching { fetchChatSummary(id) }.getOrNull()
        }
    }

    private suspend fun fetchChatSummary(chatId: Long): ChatSummary {
        val chat = client.send(TdApi.GetChat(chatId))
        val preview = (chat.lastMessage?.content as? TdApi.MessageText)?.text?.text
            ?: chat.lastMessage?.content?.let { contentPreview(it) }
            ?: ""
        return ChatSummary(
            id = chat.id,
            title = chat.title,
            lastMessagePreview = preview,
            unreadCount = chat.unreadCount,
            isMuted = chat.notificationSettings.muteFor > 0,
            lastMessageDate = chat.lastMessage?.date ?: 0,
        )
    }

    private fun contentPreview(content: TdApi.MessageContent): String = when (content) {
        is TdApi.MessagePhoto -> "📷 Photo"
        is TdApi.MessageVideo -> "🎬 Video"
        is TdApi.MessageVoiceNote -> "🎤 Voice message"
        is TdApi.MessageDocument -> "📄 ${content.document.fileName}"
        is TdApi.MessageSticker -> "${content.sticker.emoji} Sticker"
        else -> ""
    }

    suspend fun loadHistory(chatId: Long, fromMessageId: Long = 0, limit: Int = 50): List<MessageItem> {
        val result = client.send(
            TdApi.GetChatHistory(chatId, fromMessageId, 0, limit, false)
        )
        return result.messages.map { it.toMessageItem() }
    }

    suspend fun sendTextMessage(chatId: Long, text: String) {
        val formattedText = TdApi.FormattedText(text, emptyArray())
        val content = TdApi.InputMessageText(formattedText, null, true)
        client.send(TdApi.SendMessage(chatId, null, null, null, null, content))
    }

    /**
     * Marks messages as read. Routed through here (not called directly from UI)
     * so [GhostModeManager] / the [PrivacyInterceptor] can delay or suppress it
     * when Ghost Mode is active — see `beforeOutgoingRequest` for ViewMessages.
     */
    suspend fun markMessagesRead(chatId: Long, messageIds: LongArray) {
        client.send(TdApi.ViewMessages(chatId, messageIds, null, true))
    }

    suspend fun setTyping(chatId: Long, isTyping: Boolean) {
        val action: TdApi.ChatAction = if (isTyping) TdApi.ChatActionTyping() else TdApi.ChatActionCancel()
        client.send(TdApi.SendChatAction(chatId, null, null, action))
    }
}

private fun TdApi.Message.toMessageItem(): MessageItem {
    val text = when (val c = content) {
        is TdApi.MessageText -> c.text.text
        else -> ""
    }
    return MessageItem(
        id = id,
        chatId = chatId,
        senderName = "", // resolved via GetUser/GetChat in the UI layer as needed
        text = text,
        date = date,
        isOutgoing = isOutgoing,
    )
}
