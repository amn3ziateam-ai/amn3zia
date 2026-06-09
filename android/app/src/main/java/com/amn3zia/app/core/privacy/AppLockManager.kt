package com.amn3zia.app.core.privacy

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.amn3zia.app.core.settings.PrivacyPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.security.MessageDigest

/**
 * App-level lock: PIN, password, or biometric (fingerprint / Face ID).
 *
 * Lock state machine:
 *   LOCKED  ──► unlockWithPin()/unlockWithBiometric()  ──►  UNLOCKED
 *   UNLOCKED ──► (auto-lock timer / app to background) ──►  LOCKED
 *
 * After [maxFailedAttempts] wrong PIN entries the [SelfDestructManager] is
 * triggered (wipes all data).
 */
class AppLockManager(
    private val context: Context,
    private val prefs: PrivacyPreferences,
    private val selfDestruct: SelfDestructManager,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _locked = MutableStateFlow(true)
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    private var failedAttempts = 0
    private var maxFailedAttempts = 10

    init {
        scope.launch {
            prefs.selfDestructAttempts.collect { maxFailedAttempts = it }
        }
        scope.launch {
            prefs.appLockEnabled.collect { enabled ->
                if (!enabled) _locked.value = false
            }
        }
    }

    /** Returns true if app lock is turned on at all. */
    suspend fun isEnabled(): Boolean = prefs.appLockEnabled.first()

    /** Called when activity resumes — re-locks if auto-lock timeout expired. */
    fun onAppForeground() {
        scope.launch {
            if (prefs.appLockEnabled.first()) _locked.value = true
        }
    }

    /** Set a new PIN (stores its SHA-256 hash). */
    suspend fun setPin(pin: String) {
        prefs.set(PrivacyPreferences.APP_LOCK_PIN_HASH, sha256(pin))
        prefs.set(PrivacyPreferences.APP_LOCK_ENABLED, true)
        prefs.set(PrivacyPreferences.APP_LOCK_TYPE, "pin")
    }

    suspend fun clearLock() {
        prefs.set(PrivacyPreferences.APP_LOCK_ENABLED, false)
        prefs.set(PrivacyPreferences.APP_LOCK_PIN_HASH, "")
        _locked.value = false
    }

    /** Attempt PIN unlock. Returns true on success. Triggers self-destruct after too many failures. */
    suspend fun unlockWithPin(pin: String): Boolean {
        val stored = prefs.appLockPinHash.first()
        return if (sha256(pin) == stored) {
            failedAttempts = 0
            _locked.value = false
            true
        } else {
            failedAttempts++
            if (failedAttempts >= maxFailedAttempts) {
                scope.launch { selfDestruct.manualTrigger() }
            }
            false
        }
    }

    /** Show biometric prompt. Unlocks on success. */
    fun unlockWithBiometric(activity: FragmentActivity, onSuccess: () -> Unit, onFail: (String) -> Unit) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                failedAttempts = 0
                _locked.value = false
                onSuccess()
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onFail(errString.toString())
            }
            override fun onAuthenticationFailed() {
                failedAttempts++
            }
        })
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("AMN3ZIA")
            .setSubtitle("Unlock to continue")
            .setNegativeButtonText("Use PIN")
            .build()
        prompt.authenticate(info)
    }

    fun isBiometricAvailable(): Boolean {
        val bm = BiometricManager.from(context)
        return bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
                BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
