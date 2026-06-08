package com.amn3zia.app.ui.screens.chats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.amn3zia.app.AmnApplication
import com.amn3zia.app.core.account.AccountSession
import com.amn3zia.app.core.tdlib.ChatSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.drinkless.td.libcore.telegram.TdApi

class ChatListViewModel(application: Application) : AndroidViewModel(application) {

    private val app get() = AmnApplication.from(getApplication())
    private val session: AccountSession get() = requireNotNull(app.accounts.activeSession())

    private val _chats = MutableStateFlow<List<ChatSummary>>(emptyList())
    val chats: StateFlow<List<ChatSummary>> = _chats.asStateFlow()

    val accountIds: StateFlow<List<String>> = app.accounts.accountIds
    val activeAccountId: StateFlow<String?> = app.accounts.activeAccountId

    private val _ghostModeEnabled = MutableStateFlow(app.ghostMode.isEnabled)
    val ghostModeEnabled: StateFlow<Boolean> = _ghostModeEnabled.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            session.chats.chatUpdates.collect { update ->
                if (update is TdApi.UpdateNewMessage || update is TdApi.UpdateChatLastMessage || update is TdApi.UpdateChatReadInbox) {
                    refresh()
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            runCatching { session.chats.loadChatList() }
                .onSuccess { _chats.value = it }
        }
    }

    fun toggleGhostMode() {
        app.ghostMode.isEnabled = !app.ghostMode.isEnabled
        _ghostModeEnabled.value = app.ghostMode.isEnabled
    }

    fun switchAccount(accountId: String) {
        app.accounts.switchTo(accountId)
        refresh()
    }

    fun addAccount() {
        app.accounts.addAccount()
    }
}
