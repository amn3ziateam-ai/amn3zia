package com.amn3zia.app.core.privacy

import com.amn3zia.app.core.account.AccountManager
import com.amn3zia.app.core.tdlib.PrivacyInterceptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi

/**
 * Concrete chokepoint implementation wiring [GhostModeManager] and
 * [AntiTrackingPolicy] into every [TdClient]'s outgoing/incoming traffic.
 *
 * `accounts` is supplied lazily because [AccountManager] itself constructs
 * [TdClient]s that need this interceptor — see the lateinit wiring in
 * AccountManager.attach(this).
 */
class PrivacyInterceptorImpl(
    val ghostMode: GhostModeManager,
    val antiTracking: AntiTrackingPolicy,
) : PrivacyInterceptor {

    lateinit var accounts: AccountManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun beforeOutgoingRequest(accountId: String, function: TdApi.Function<*>): Boolean {
        // Anti-tracking: drop link previews / external media downloads outright
        if (antiTracking.filterOutgoing(function) == null) return false

        when (function) {
            is TdApi.SendChatAction -> {
                if (!ghostMode.shouldAllowTypingAction()) return false
            }
            is TdApi.ViewMessages -> {
                if (!ghostMode.shouldSendReadReceiptNow()) {
                    if (ghostMode.useDelayedReadMarking) {
                        ghostMode.scheduleDelayedRead(accountId, function.chatId, function.messageIds) { chatId, ids ->
                            accounts.clientFor(accountId)?.send(TdApi.ViewMessages(chatId, ids, null, true))
                        }
                    }
                    return false
                }
            }
            else -> Unit
        }
        return true
    }

    override fun onIncomingUpdate(accountId: String, update: TdApi.Object) {
        if (update is TdApi.UpdateAuthorizationState &&
            update.authorizationState is TdApi.AuthorizationStateReady
        ) {
            // Apply anti-tracking auto-download policy as soon as the session is usable.
            scope.launch {
                runCatching {
                    accounts.clientFor(accountId)?.send(
                        TdApi.SetAutoDownloadSettings(antiTracking.autoDownloadSettings(), TdApi.AutoDownloadSettingsPresetLow())
                    )
                }
            }
        }
    }
}
