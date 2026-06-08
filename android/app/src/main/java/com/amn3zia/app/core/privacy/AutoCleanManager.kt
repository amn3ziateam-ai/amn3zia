package com.amn3zia.app.core.privacy

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.amn3zia.app.core.account.AccountManager
import kotlinx.coroutines.launch
import org.drinkless.td.libcore.telegram.TdApi
import java.util.concurrent.TimeUnit

enum class AutoCleanTrigger { TIMER, INACTIVITY, APP_CLOSE }

data class AutoCleanRule(
    val accountId: String,
    val chatId: Long?,           // null = applies to all chats
    val trigger: AutoCleanTrigger,
    val olderThanHours: Int,     // delete messages/media older than this
    val inactivityThresholdMinutes: Int = 0, // for INACTIVITY trigger
    val wipeMediaOnly: Boolean = false,
)

/**
 * Advanced Auto-Clean: deletes chats/messages/media/cache on a schedule,
 * after a period of inactivity, or when the app is closed.
 *
 * - TIMER       -> WorkManager periodic job (survives process death, battery-aware)
 * - INACTIVITY  -> checked on app foreground/background transitions, persisted
 *                  "lastActiveAt" compared against threshold
 * - APP_CLOSE   -> run from ProcessLifecycleOwner.onStop hook (see MainActivity)
 */
class AutoCleanManager(
    private val context: Context,
    private val accounts: AccountManager,
) {
    private val workManager get() = WorkManager.getInstance(context)
    private val cleanupScope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO
    )
    private val rules = mutableListOf<AutoCleanRule>()
    @Volatile private var lastActiveAtMillis: Long = System.currentTimeMillis()

    fun addRule(rule: AutoCleanRule) {
        rules.add(rule)
        if (rule.trigger == AutoCleanTrigger.TIMER) scheduleTimer(rule)
    }

    fun removeRulesFor(accountId: String) {
        rules.removeAll { it.accountId == accountId }
        workManager.cancelUniqueWork(timerWorkName(accountId))
    }

    fun scheduleTriggersOnAppStart() {
        rules.filter { it.trigger == AutoCleanTrigger.TIMER }.forEach(::scheduleTimer)
    }

    private fun scheduleTimer(rule: AutoCleanRule) {
        val request = PeriodicWorkRequestBuilder<AutoCleanWorker>(
            repeatInterval = maxOf(rule.olderThanHours.toLong(), 1L), TimeUnit.HOURS,
        )
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build())
            .setInputData(workDataOf(
                AutoCleanWorker.KEY_ACCOUNT_ID to rule.accountId,
                AutoCleanWorker.KEY_CHAT_ID to (rule.chatId ?: -1L),
                AutoCleanWorker.KEY_OLDER_THAN_HOURS to rule.olderThanHours,
                AutoCleanWorker.KEY_MEDIA_ONLY to rule.wipeMediaOnly,
            ))
            .build()
        workManager.enqueueUniquePeriodicWork(
            timerWorkName(rule.accountId), ExistingPeriodicWorkPolicy.UPDATE, request,
        )
    }

    private fun timerWorkName(accountId: String) = "autoclean_timer_$accountId"

    /** Call from Activity lifecycle: every time the app comes to foreground. */
    fun onAppForegrounded() {
        val now = System.currentTimeMillis()
        val idleMinutes = (now - lastActiveAtMillis) / 60_000
        rules.filter { it.trigger == AutoCleanTrigger.INACTIVITY }
            .filter { idleMinutes >= it.inactivityThresholdMinutes }
            .forEach { runCleanForRule(it) }
        lastActiveAtMillis = now
    }

    /** Call from ProcessLifecycleOwner.onStop / Activity.onDestroy (best-effort). */
    fun onAppClosed() {
        lastActiveAtMillis = System.currentTimeMillis()
        rules.filter { it.trigger == AutoCleanTrigger.APP_CLOSE }
            .forEach { runCleanForRule(it) }
    }

    internal fun runCleanForRule(rule: AutoCleanRule) {
        val client = accounts.clientFor(rule.accountId) ?: return
        val cutoff = (System.currentTimeMillis() / 1000L) - rule.olderThanHours * 3600L

        cleanupScope.launch {
            val chatIds: List<Long> = if (rule.chatId != null) listOf(rule.chatId) else {
                runCatching { client.send(TdApi.GetChats(TdApi.ChatListMain(), 200)).chatIds.toList() }
                    .getOrDefault(emptyList())
            }
            for (chatId in chatIds) {
                cleanChat(rule, chatId, cutoff)
            }
        }
    }

    private suspend fun cleanChat(rule: AutoCleanRule, chatId: Long, cutoffUnixSeconds: Long) {
        val client = accounts.clientFor(rule.accountId) ?: return
        runCatching {
            var fromMessageId = 0L
            while (true) {
                val history = client.send(TdApi.GetChatHistory(chatId, fromMessageId, 0, 100, false))
                if (history.messages.isEmpty()) break
                val toDelete = history.messages.filter { it.date <= cutoffUnixSeconds }
                    .filter { msg -> !rule.wipeMediaOnly || msg.content.isMediaContent() }
                if (toDelete.isNotEmpty()) {
                    client.send(TdApi.DeleteMessages(chatId, toDelete.map { it.id }.toLongArray(), true))
                }
                val oldest = history.messages.minByOrNull { it.id } ?: break
                if (oldest.date > cutoffUnixSeconds) break
                fromMessageId = oldest.id
            }
            // Cache trim — local files for this chat that TDLib has already downloaded
            client.send(TdApi.OptimizeStorage(0L, 0, 0, 0, arrayOf(), longArrayOf(chatId), longArrayOf(), true, 0))
        }
    }

    private fun TdApi.MessageContent.isMediaContent(): Boolean = when (this) {
        is TdApi.MessagePhoto, is TdApi.MessageVideo, is TdApi.MessageDocument,
        is TdApi.MessageVoiceNote, is TdApi.MessageAnimation, is TdApi.MessageAudio -> true
        else -> false
    }
}
