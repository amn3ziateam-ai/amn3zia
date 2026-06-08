package com.amn3zia.app.core.privacy

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/**
 * Ghost Mode — global privacy toggle that controls outgoing presence signals.
 *
 * Implemented as the decision authority that [PrivacyInterceptor] consults:
 *  - suppressTyping     -> drop TdApi.SendChatAction before it reaches TDLib
 *  - controlReadReceipts -> drop/allow TdApi.ViewMessages
 *  - delayedReadMarking  -> instead of dropping, re-issue ViewMessages after a
 *                           randomized human-like delay so contacts still see
 *                           messages "read" eventually but not instantly
 *
 * Settings persist in [EncryptionManager]-backed encrypted prefs via the
 * settings store (kept here as in-memory + simple flags for clarity; a real
 * build wires this to DataStore/EncryptedSharedPreferences).
 */
class GhostModeManager(context: Context) {

    @Volatile var isEnabled: Boolean = false
    @Volatile var suppressTyping: Boolean = true
    @Volatile var controlReadReceipts: Boolean = true
    @Volatile var useDelayedReadMarking: Boolean = true
    @Volatile var minDelaySeconds: Int = 15
    @Volatile var maxDelaySeconds: Int = 90

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val pendingReads = ConcurrentHashMap<String, MutableSet<Long>>()

    /** Called by PrivacyInterceptor.beforeOutgoingRequest. Returns true = allow request through. */
    fun shouldAllowTypingAction(): Boolean = !(isEnabled && suppressTyping)

    /**
     * Returns true if a ViewMessages call should be sent to TDLib immediately.
     * If false, the caller is expected to have already been queued via
     * [scheduleDelayedRead] (when delayed marking is on) or the read is fully
     * suppressed (when it's off).
     */
    fun shouldSendReadReceiptNow(): Boolean = !(isEnabled && controlReadReceipts)

    /**
     * Queues a delayed "mark as read" for [messageIds] in [chatId]. After a
     * random human-like delay, [onFire] is invoked to actually issue the
     * TdApi.ViewMessages call (bypassing this gate, since it's the delayed one).
     */
    fun scheduleDelayedRead(accountId: String, chatId: Long, messageIds: LongArray, onFire: suspend (Long, LongArray) -> Unit) {
        if (!useDelayedReadMarking) return
        val key = "$accountId:$chatId"
        val set = pendingReads.getOrPut(key) { ConcurrentHashMap.newKeySet() }
        val newIds = messageIds.filter { set.add(it) }
        if (newIds.isEmpty()) return

        val delaySeconds = Random.nextInt(minDelaySeconds, maxDelaySeconds + 1)
        scope.launch {
            delay(delaySeconds * 1000L)
            val toSend = newIds.toLongArray()
            newIds.forEach { set.remove(it) }
            runCatching { onFire(chatId, toSend) }
        }
    }

    /** Builds the chat-action that's actually allowed through (cancel instead of typing) when suppressing. */
    fun sanitizeChatAction(action: TdApi.ChatAction): TdApi.ChatAction =
        if (isEnabled && suppressTyping) TdApi.ChatActionCancel() else action
}
