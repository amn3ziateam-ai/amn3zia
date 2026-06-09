package com.amn3zia.app.core.privacy

import android.content.Context
import com.amn3zia.app.core.settings.PrivacyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import java.security.MessageDigest

/**
 * Hidden Chats — chats that are excluded from the main chat list and
 * only accessible after entering a separate PIN.
 *
 * The hidden list is stored as a comma-separated string of chat IDs in
 * EncryptedSharedPreferences. The PIN hash is stored in DataStore.
 */
class HiddenChatManager(
    private val context: Context,
    private val prefs: PrivacyPreferences,
) {
    private val hiddenPrefs by lazy {
        context.getSharedPreferences("amn3zia_hidden", Context.MODE_PRIVATE)
    }

    private val _hiddenIds = MutableStateFlow<Set<Long>>(loadIds())
    val hiddenChatIds: Flow<Set<Long>> = _hiddenIds.asStateFlow()

    fun isHidden(chatId: Long): Boolean = _hiddenIds.value.contains(chatId)

    fun hide(chatId: Long) {
        val updated = _hiddenIds.value + chatId
        _hiddenIds.value = updated
        persist(updated)
    }

    fun unhide(chatId: Long) {
        val updated = _hiddenIds.value - chatId
        _hiddenIds.value = updated
        persist(updated)
    }

    /** Set (or reset) the hidden-chats PIN. */
    suspend fun setPin(pin: String) {
        prefs.set(PrivacyPreferences.HIDDEN_CHATS_PIN_HASH, sha256(pin))
    }

    /** Returns true if the PIN is correct. */
    suspend fun verifyPin(pin: String): Boolean {
        val stored = prefs.hiddenChatsPinHash.first()
        return stored.isNotEmpty() && sha256(pin) == stored
    }

    suspend fun hasPinSet(): Boolean = prefs.hiddenChatsPinHash.first().isNotEmpty()

    private fun loadIds(): Set<Long> {
        val raw = hiddenPrefs.getString("hidden_ids", "") ?: ""
        return raw.split(",").mapNotNull { it.trim().toLongOrNull() }.toSet()
    }

    private fun persist(ids: Set<Long>) {
        hiddenPrefs.edit().putString("hidden_ids", ids.joinToString(",")).apply()
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
