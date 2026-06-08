package com.amn3zia.app.core.privacy

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.amn3zia.app.core.account.AccountManager
import java.security.MessageDigest

/**
 * Self-Destruct: wipes the app on too many failed PIN attempts, or on manual
 * trigger from settings. Shares the same irreversible wipe path as the panic
 * button ([AccountManager.wipeAllAccountsLocally]) — the only difference is
 * what triggers it (failed-PIN counter vs. explicit confirmation code).
 */
class SelfDestructManager(
    context: Context,
    private val accounts: AccountManager,
) {
    @Volatile var maxFailedAttempts: Int = 5

    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(
            context, "amn3zia_lock_prefs", masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private fun hash(pin: String): String =
        MessageDigest.getInstance("SHA-256").digest(pin.toByteArray()).joinToString("") { "%02x".format(it) }

    fun setPin(pin: String) {
        prefs.edit().putString(KEY_PIN_HASH, hash(pin)).putInt(KEY_FAILED_COUNT, 0).apply()
    }

    fun hasPinSet(): Boolean = prefs.contains(KEY_PIN_HASH)

    /** Returns true if [pin] is correct. On failure, increments the counter and may trigger a wipe. */
    suspend fun verifyPin(pin: String): PinResult {
        val stored = prefs.getString(KEY_PIN_HASH, null) ?: return PinResult.NoPinConfigured
        if (hash(pin) == stored) {
            prefs.edit().putInt(KEY_FAILED_COUNT, 0).apply()
            return PinResult.Correct
        }
        val failed = prefs.getInt(KEY_FAILED_COUNT, 0) + 1
        prefs.edit().putInt(KEY_FAILED_COUNT, failed).apply()
        val remaining = maxFailedAttempts - failed
        if (remaining <= 0) {
            triggerWipe()
            return PinResult.WipeTriggered
        }
        return PinResult.Incorrect(remaining)
    }

    /** Manual trigger from Settings -> "Self-destruct now". Caller must already have confirmed with the user. */
    suspend fun manualTrigger() = triggerWipe()

    private suspend fun triggerWipe() {
        accounts.wipeAllAccountsLocally()
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_FAILED_COUNT = "failed_count"
    }
}

sealed interface PinResult {
    data object Correct : PinResult
    data class Incorrect(val attemptsRemaining: Int) : PinResult
    data object WipeTriggered : PinResult
    data object NoPinConfigured : PinResult
}
