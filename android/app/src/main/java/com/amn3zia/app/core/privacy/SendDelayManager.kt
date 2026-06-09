package com.amn3zia.app.core.privacy

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Send Delay — queues outgoing messages and dispatches them after a configurable
 * delay, with a cancel window (tap to undo before the timer fires).
 */
class SendDelayManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    data class PendingMessage(
        val id: String = UUID.randomUUID().toString(),
        val chatId: Long,
        val text: String,
        val scheduledAt: Long,   // System.currentTimeMillis() + delaySec * 1000
    )

    private val pending = mutableMapOf<String, Job>()
    val queue = kotlinx.coroutines.flow.MutableStateFlow<List<PendingMessage>>(emptyList())

    /**
     * Schedule [send] to run after [delaySec] seconds.
     * Returns the message ID that can be passed to [cancel].
     */
    fun schedule(chatId: Long, text: String, delaySec: Int, send: suspend () -> Unit): String {
        val msg = PendingMessage(chatId = chatId, text = text, scheduledAt = System.currentTimeMillis() + delaySec * 1000L)
        queue.value = queue.value + msg
        val job = scope.launch {
            delay(delaySec * 1000L)
            queue.value = queue.value.filter { it.id != msg.id }
            send()
        }
        pending[msg.id] = job
        return msg.id
    }

    /** Cancel a pending message (within the delay window). */
    fun cancel(id: String) {
        pending.remove(id)?.cancel()
        queue.value = queue.value.filter { it.id != id }
    }
}
