package com.amn3zia.app.core.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "amn3zia_privacy")

/**
 * Central persistent privacy settings backed by DataStore Preferences.
 * All privacy toggles survive app restarts and are stored encrypted at rest
 * (the DataStore file lives in the app's private files dir, protected by
 * Android's per-app filesystem sandbox + EncryptedSharedPreferences layer).
 */
class PrivacyPreferences(private val context: Context) {

    // ── Keys ────────────────────────────────────────────────────────────────
    companion object Keys {
        val GHOST_MODE             = booleanPreferencesKey("ghost_mode")
        val SUPPRESS_TYPING        = booleanPreferencesKey("suppress_typing")
        val CONTROL_READ_RECEIPTS  = booleanPreferencesKey("control_read_receipts")
        val DELAYED_READ_MARKING   = booleanPreferencesKey("delayed_read_marking")
        val DISABLE_LINK_PREVIEWS  = booleanPreferencesKey("disable_link_previews")
        val BLOCK_EXTERNAL_MEDIA   = booleanPreferencesKey("block_external_media")
        val SCREENSHOTS_BLOCKED    = booleanPreferencesKey("screenshots_blocked")
        val APP_LOCK_ENABLED       = booleanPreferencesKey("app_lock_enabled")
        val APP_LOCK_TYPE          = stringPreferencesKey("app_lock_type")  // "pin" | "biometric"
        val APP_LOCK_PIN_HASH      = stringPreferencesKey("app_lock_pin_hash")
        val AUTO_LOCK_TIMEOUT_SEC  = intPreferencesKey("auto_lock_timeout_sec")
        val KILL_SWITCH_ENABLED    = booleanPreferencesKey("kill_switch")
        val PANIC_CODE_ENABLED     = booleanPreferencesKey("panic_code_enabled")
        val HIDDEN_CHATS_PIN_HASH  = stringPreferencesKey("hidden_chats_pin_hash")
        val FAKE_UI_ENABLED        = booleanPreferencesKey("fake_ui_enabled")
        val FAKE_UI_UNLOCK_GESTURE = stringPreferencesKey("fake_ui_gesture")  // e.g. "666="
        val SEND_DELAY_SEC         = intPreferencesKey("send_delay_sec")
        val ANTI_VIEW_ENABLED      = booleanPreferencesKey("anti_view_enabled")
        val SILENT_MODE_ENABLED    = booleanPreferencesKey("silent_mode_enabled")
        val SELF_DESTRUCT_ATTEMPTS = intPreferencesKey("self_destruct_attempts")
        val BLUR_IN_RECENTS        = booleanPreferencesKey("blur_in_recents")
        val MEDIA_VISIBILITY_BLUR  = booleanPreferencesKey("media_visibility_blur")
    }

    // ── Flows ────────────────────────────────────────────────────────────────
    val ghostMode: Flow<Boolean>            = pref(GHOST_MODE, false)
    val suppressTyping: Flow<Boolean>       = pref(SUPPRESS_TYPING, true)
    val controlReadReceipts: Flow<Boolean>  = pref(CONTROL_READ_RECEIPTS, true)
    val delayedReadMarking: Flow<Boolean>   = pref(DELAYED_READ_MARKING, true)
    val disableLinkPreviews: Flow<Boolean>  = pref(DISABLE_LINK_PREVIEWS, true)
    val blockExternalMedia: Flow<Boolean>   = pref(BLOCK_EXTERNAL_MEDIA, true)
    val screenshotsBlocked: Flow<Boolean>   = pref(SCREENSHOTS_BLOCKED, true)
    val appLockEnabled: Flow<Boolean>       = pref(APP_LOCK_ENABLED, false)
    val appLockType: Flow<String>           = pref(APP_LOCK_TYPE, "pin")
    val appLockPinHash: Flow<String>        = pref(APP_LOCK_PIN_HASH, "")
    val autoLockTimeoutSec: Flow<Int>       = pref(AUTO_LOCK_TIMEOUT_SEC, 60)
    val killSwitchEnabled: Flow<Boolean>    = pref(KILL_SWITCH_ENABLED, true)
    val hiddenChatsPinHash: Flow<String>    = pref(HIDDEN_CHATS_PIN_HASH, "")
    val fakeUiEnabled: Flow<Boolean>        = pref(FAKE_UI_ENABLED, false)
    val fakeUiGesture: Flow<String>         = pref(FAKE_UI_UNLOCK_GESTURE, "1337=")
    val sendDelaySec: Flow<Int>             = pref(SEND_DELAY_SEC, 0)
    val antiViewEnabled: Flow<Boolean>      = pref(ANTI_VIEW_ENABLED, false)
    val silentModeEnabled: Flow<Boolean>    = pref(SILENT_MODE_ENABLED, false)
    val selfDestructAttempts: Flow<Int>     = pref(SELF_DESTRUCT_ATTEMPTS, 10)
    val blurInRecents: Flow<Boolean>        = pref(BLUR_IN_RECENTS, true)
    val mediaVisibilityBlur: Flow<Boolean>  = pref(MEDIA_VISIBILITY_BLUR, false)

    // ── Write helpers ────────────────────────────────────────────────────────
    suspend fun set(key: Preferences.Key<Boolean>, value: Boolean) {
        context.dataStore.edit { it[key] = value }
    }
    suspend fun set(key: Preferences.Key<String>, value: String) {
        context.dataStore.edit { it[key] = value }
    }
    suspend fun set(key: Preferences.Key<Int>, value: Int) {
        context.dataStore.edit { it[key] = value }
    }

    private fun <T> pref(key: Preferences.Key<T>, default: T): Flow<T> =
        context.dataStore.data.map { it[key] ?: default }
}
