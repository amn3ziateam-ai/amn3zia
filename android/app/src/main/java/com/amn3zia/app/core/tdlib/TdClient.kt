package com.amn3zia.app.core.tdlib

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.io.File
import java.util.concurrent.atomic.AtomicReference

/**
 * Thin coroutine-friendly wrapper around a single TDLib [Client] instance.
 *
 * One [TdClient] == one isolated Telegram account/session. The [AccountManager]
 * owns the mapping from accountId -> TdClient and ensures each gets its own
 * database/files directory and encryption key (see [databaseEncryptionKey]).
 *
 * All Telegram network/protocol behavior is delegated verbatim to TDLib —
 * we do not modify the protocol. This wrapper only adapts TDLib's
 * callback-based Client.send API to coroutines/Flow for the UI layer, and is
 * the single chokepoint the Privacy Layer hooks into (see [PrivacyInterceptor]).
 */
class TdClient(
    val accountId: String,
    private val databaseDirectory: File,
    private val filesDirectory: File,
    private val databaseEncryptionKey: ByteArray,
    private val interceptor: PrivacyInterceptor,
) {
    private val _updates = MutableSharedFlow<TdApi.Object>(
        extraBufferCapacity = 256,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val updates: SharedFlow<TdApi.Object> = _updates.asSharedFlow()

    private val clientRef = AtomicReference<Client?>(null)

    /** Starts the TDLib client and begins receiving updates. Idempotent. */
    fun start() {
        if (clientRef.get() != null) return
        val client = Client.create(
            { obj -> onUpdate(obj) },
            { ex -> onUnhandledException(ex) },
            { ex -> onUnhandledException(ex) },
        )
        clientRef.set(client)
        configureParameters(client)
    }

    fun stop() {
        clientRef.getAndSet(null)?.send(TdApi.Close()) {}
    }

    private fun configureParameters(client: Client) {
        val params = TdApi.SetTdlibParameters().apply {
            databaseDirectory = this@TdClient.databaseDirectory.absolutePath
            filesDirectory = this@TdClient.filesDirectory.absolutePath
            databaseEncryptionKey = this@TdClient.databaseEncryptionKey
            useFileDatabase = true
            useChatInfoDatabase = true
            useMessageDatabase = true
            useSecretChats = true
            apiId = com.amn3zia.app.BuildConfig.TELEGRAM_API_ID
            apiHash = com.amn3zia.app.BuildConfig.TELEGRAM_API_HASH
            systemLanguageCode = "en"
            deviceModel = "AMN3ZIA Client"
            applicationVersion = "0.1.0"
            // Anti-tracking: do not let TDLib auto-download anything by default;
            // the privacy layer sets per-chat/per-network download rules explicitly.
        }
        client.send(params) { /* result observed via UpdateAuthorizationState */ }
    }

    private fun onUpdate(obj: TdApi.Object) {
        // Privacy interceptor gets first look — it can suppress, delay, or rewrite
        // outgoing read-receipt/typing related side effects triggered by updates
        // (e.g. it decides whether viewMessages should actually be sent for Ghost Mode).
        interceptor.onIncomingUpdate(accountId, obj)
        _updates.tryEmit(obj)
    }

    private fun onUnhandledException(ex: Throwable) {
        // Never silently crash the whole app on a single account's TDLib error.
        android.util.Log.e("TdClient[$accountId]", "Unhandled TDLib exception", ex)
    }

    /**
     * Sends a TdApi.Function and suspends until the result arrives.
     * The Privacy Layer can veto/modify outgoing requests here (Ghost Mode,
     * Anti-Tracking link-preview suppression, etc.) before they reach TDLib.
     */
    suspend fun <R : TdApi.Object> send(function: TdApi.Function<R>): R {
        val allowed = interceptor.beforeOutgoingRequest(accountId, function)
        if (!allowed) {
            @Suppress("UNCHECKED_CAST")
            return TdApi.Ok() as R
        }
        val deferred = CompletableDeferred<R>()
        val client = clientRef.get() ?: error("TdClient[$accountId] not started")
        client.send(function) { result ->
            @Suppress("UNCHECKED_CAST")
            when (result.constructor) {
                TdApi.Error.CONSTRUCTOR -> deferred.completeExceptionally(
                    TdLibException(result as TdApi.Error)
                )
                else -> deferred.complete(result as R)
            }
        }
        return deferred.await()
    }
}

class TdLibException(val error: TdApi.Error) : Exception("TDLib error ${error.code}: ${error.message}")
