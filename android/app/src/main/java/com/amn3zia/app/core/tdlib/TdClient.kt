package com.amn3zia.app.core.tdlib

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
        replay = 1,           // FIX: replay the latest update to late subscribers.
        // Without replay, UpdateAuthorizationState fires on TDLib's background
        // thread before AuthViewModel's stateIn collector has subscribed, and is
        // permanently lost → UI stays stuck on AuthState.Initializing forever.
        extraBufferCapacity = 256,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val updates: SharedFlow<TdApi.Object> = _updates.asSharedFlow()

    // --- TEMPORARY on-screen diagnostics -----------------------------------
    // adb/logcat is unreliable on the target test emulator, so surface what's
    // happening directly in the UI (AuthScreen shows this while initializing)
    // instead of relying on logcat. Safe to remove once the launch hang is
    // diagnosed and fixed.
    private val _debugLog = MutableStateFlow<List<String>>(emptyList())
    val debugLog: StateFlow<List<String>> = _debugLog.asStateFlow()

    private fun trace(msg: String) {
        android.util.Log.d("TdClient[$accountId]", msg)
        _debugLog.update { current ->
            // Collapse runs of identical messages (e.g. floods of UpdateOption)
            // into a single "msg (xN)" line so rare/important entries (auth
            // state transitions, errors) don't get pushed out of the window.
            val last = current.lastOrNull()
            val collapsedMsg = msg
            val updated = if (last != null && stripCount(last) == collapsedMsg) {
                current.dropLast(1) + "$collapsedMsg (x${countOf(last) + 1})"
            } else {
                current + collapsedMsg
            }
            updated.takeLast(40)
        }
    }

    private fun stripCount(line: String): String = line.replace(Regex(""" \(x\d+\)$"""), "")
    private fun countOf(line: String): Int =
        Regex(""" \(x(\d+)\)$""").find(line)?.groupValues?.get(1)?.toIntOrNull() ?: 1
    // ------------------------------------------------------------------------

    private val clientRef = AtomicReference<Client?>(null)

    /** Starts the TDLib client and begins receiving updates. Idempotent. */
    fun start() {
        if (clientRef.get() != null) return
        trace("start() called")
        val client = try {
            Client.create(
                { obj -> onUpdate(obj) },
                { ex -> onUnhandledException(ex) },
                { ex -> onUnhandledException(ex) },
            )
        } catch (t: Throwable) {
            trace("Client.create THREW: ${t::class.simpleName}: ${t.message}")
            throw t
        }
        trace("Client.create returned (native lib loaded OK)")
        clientRef.set(client)
        configureParameters(client)
        trace("configureParameters sent")
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
        trace("apiId=${params.apiId} apiHash.len=${params.apiHash?.length ?: -1} dbDir=${params.databaseDirectory}")
        client.send(params) { result ->
            // The authorization-state machine (observed via UpdateAuthorizationState)
            // is the source of truth for the UI. But if TDLib rejects the parameters
            // outright (e.g. invalid/missing api_id or api_hash), it never advances
            // past AuthorizationStateWaitTdlibParameters and the UI would otherwise
            // spin forever with no clue why — so at least surface it in logcat.
            if (result.constructor == TdApi.Error.CONSTRUCTOR) {
                val error = result as TdApi.Error
                trace("SetTdlibParameters REJECTED code=${error.code}: ${error.message}")
            } else {
                trace("SetTdlibParameters -> ${result::class.simpleName}")
            }
        }
    }

    private fun onUpdate(obj: TdApi.Object) {
        if (obj.constructor == TdApi.UpdateAuthorizationState.CONSTRUCTOR) {
            val st = (obj as TdApi.UpdateAuthorizationState).authorizationState
            trace("UpdateAuthorizationState -> ${st::class.simpleName}")
        } else {
            trace("update: ${obj::class.simpleName}")
        }
        // Privacy interceptor gets first look — it can suppress, delay, or rewrite
        // outgoing read-receipt/typing related side effects triggered by updates
        // (e.g. it decides whether viewMessages should actually be sent for Ghost Mode).
        interceptor.onIncomingUpdate(accountId, obj)
        _updates.tryEmit(obj)
    }

    private fun onUnhandledException(ex: Throwable) {
        // Never silently crash the whole app on a single account's TDLib error.
        trace("Unhandled TDLib exception: ${ex::class.simpleName}: ${ex.message}")
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
