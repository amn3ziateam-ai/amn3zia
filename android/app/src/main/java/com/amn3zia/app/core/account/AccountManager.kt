package com.amn3zia.app.core.account

import android.content.Context
import com.amn3zia.app.core.privacy.AntiTrackingPolicy
import com.amn3zia.app.core.privacy.EncryptionManager
import com.amn3zia.app.core.privacy.GhostModeManager
import com.amn3zia.app.core.privacy.PrivacyInterceptorImpl
import com.amn3zia.app.core.proxy.ProxyEngine
import com.amn3zia.app.core.tdlib.AuthRepository
import com.amn3zia.app.core.tdlib.ChatRepository
import com.amn3zia.app.core.tdlib.TdClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class AccountSession(
    val accountId: String,
    val client: TdClient,
    val auth: AuthRepository,
    val chats: ChatRepository,
    val proxy: ProxyEngine,
    val displayLabel: String,
)

/**
 * Multi-Account System (unlimited accounts):
 *  - each account gets a UUID, its own storage directory tree
 *    (`/files/accounts/<uuid>/{db,files}`), its own randomly-generated
 *    encryption key from [EncryptionManager], its own [TdClient]/[ProxyEngine].
 *  - nothing is shared between accounts except the master Keystore key that
 *    wraps each account's individual database key.
 */
class AccountManager(
    private val context: Context,
    private val encryption: EncryptionManager,
) {
    private val sessions = ConcurrentHashMap<String, AccountSession>()

    private val _accountIds = MutableStateFlow<List<String>>(emptyList())
    val accountIds: StateFlow<List<String>> = _accountIds

    private val _activeAccountId = MutableStateFlow<String?>(null)
    val activeAccountId: StateFlow<String?> = _activeAccountId

    private val registryFile: File
        get() = File(context.filesDir, "accounts/registry.txt")

    fun restoreSavedAccounts() {
        val file = registryFile
        if (!file.exists()) return
        file.readLines().map { it.trim() }.filter { it.isNotEmpty() }.forEach { id ->
            createSession(id, persistToRegistry = false)
        }
        if (_activeAccountId.value == null) {
            _activeAccountId.value = _accountIds.value.firstOrNull()
        }
    }

    /** Adds a brand-new, fully isolated account and starts its TDLib session (begins login flow). */
    fun addAccount(): AccountSession {
        val id = UUID.randomUUID().toString()
        val session = createSession(id, persistToRegistry = true)
        if (_activeAccountId.value == null) switchTo(id)
        return session
    }

    fun switchTo(accountId: String) {
        require(sessions.containsKey(accountId)) { "Unknown account $accountId" }
        _activeAccountId.value = accountId
    }

    fun sessionFor(accountId: String): AccountSession? = sessions[accountId]
    fun clientFor(accountId: String): TdClient? = sessions[accountId]?.client
    fun activeSession(): AccountSession? = _activeAccountId.value?.let { sessions[it] }

    /**
     * Permanently removes an account from THIS APP only:
     *  - local logout via TDLib (does not touch the Telegram-side account)
     *  - deletes its isolated storage directory and destroys its encryption key
     * Used by both normal "remove account" UI and as a building block of the panic button.
     */
    suspend fun removeAccountLocally(accountId: String) {
        val session = sessions.remove(accountId) ?: return
        runCatching { session.auth.logOut() }
        session.client.stop()
        encryption.destroyKeyFor(accountId)
        directoryFor(accountId).deleteRecursively()
        _accountIds.update { it - accountId }
        if (_activeAccountId.value == accountId) {
            _activeAccountId.value = _accountIds.value.firstOrNull()
        }
        persistRegistry()
    }

    private fun createSession(accountId: String, persistToRegistry: Boolean): AccountSession {
        val baseDir = directoryFor(accountId)
        val dbDir = File(baseDir, "db").apply { mkdirs() }
        val filesDir = File(baseDir, "files").apply { mkdirs() }
        val key = encryption.databaseKeyFor(accountId)

        val ghostMode = GhostModeManager(context)
        val antiTracking = AntiTrackingPolicy()
        val interceptor = PrivacyInterceptorImpl(ghostMode, antiTracking).also { it.accounts = this }

        val client = TdClient(accountId, dbDir, filesDir, key, interceptor)
        client.start()

        val session = AccountSession(
            accountId = accountId,
            client = client,
            auth = AuthRepository(client),
            chats = ChatRepository(client),
            proxy = ProxyEngine(accountId, client),
            displayLabel = "Account ${accountId.take(8)}",
        )
        sessions[accountId] = session
        _accountIds.update { (it + accountId).distinct() }
        if (persistToRegistry) persistRegistry()
        return session
    }

    private fun directoryFor(accountId: String): File =
        File(context.filesDir, "accounts/$accountId")

    private fun persistRegistry() {
        val file = registryFile
        file.parentFile?.mkdirs()
        file.writeText(_accountIds.value.joinToString("\n"))
    }

    /**
     * Used by the Panic Button / Self-Destruct: removes every account locally
     * and wipes all shared key material. Irreversible.
     */
    suspend fun wipeAllAccountsLocally() {
        val ids = _accountIds.value.toList()
        ids.forEach { removeAccountLocally(it) }
        encryption.destroyAllKeys()
        File(context.filesDir, "accounts").deleteRecursively()
        context.cacheDir.deleteRecursively()
    }
}
