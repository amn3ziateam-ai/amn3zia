package com.amn3zia.app.ui.screens.settings

import android.app.Application
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.amn3zia.app.AmnApplication
import com.amn3zia.app.core.privacy.ScreenProtection
import com.amn3zia.app.core.settings.PrivacyPreferences
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PrivacySettingsState(
    val ghostModeEnabled: Boolean = false,
    val suppressTyping: Boolean = true,
    val controlReadReceipts: Boolean = true,
    val delayedReadMarking: Boolean = true,
    val disableLinkPreviews: Boolean = true,
    val blockExternalMedia: Boolean = true,
    val screenshotsBlocked: Boolean = true,
    val blurInRecents: Boolean = true,
    val mediaVisibilityBlur: Boolean = false,
    val appLockEnabled: Boolean = false,
    val appLockType: String = "pin",
    val autoLockTimeoutSec: Int = 60,
    val killSwitchEnabled: Boolean = true,
    val antiViewEnabled: Boolean = false,
    val silentModeEnabled: Boolean = false,
    val sendDelaySec: Int = 0,
    val fakeUiEnabled: Boolean = false,
    val selfDestructAttempts: Int = 10,
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app get() = AmnApplication.from(getApplication())
    private val prefs get() = app.prefs

    private val _state = MutableStateFlow(PrivacySettingsState())
    val state: StateFlow<PrivacySettingsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(prefs.ghostMode, prefs.suppressTyping, prefs.controlReadReceipts, prefs.delayedReadMarking)
            { gm, st, cr, dr -> _state.update { it.copy(ghostModeEnabled = gm, suppressTyping = st, controlReadReceipts = cr, delayedReadMarking = dr) } }.collect {}
        }
        viewModelScope.launch {
            combine(prefs.disableLinkPreviews, prefs.blockExternalMedia, prefs.screenshotsBlocked, prefs.blurInRecents)
            { dlp, bem, sb, bir -> _state.update { it.copy(disableLinkPreviews = dlp, blockExternalMedia = bem, screenshotsBlocked = sb, blurInRecents = bir) } }.collect {}
        }
        viewModelScope.launch {
            combine(prefs.appLockEnabled, prefs.appLockType, prefs.autoLockTimeoutSec, prefs.killSwitchEnabled)
            { ale, alt, alts, ks -> _state.update { it.copy(appLockEnabled = ale, appLockType = alt, autoLockTimeoutSec = alts, killSwitchEnabled = ks) } }.collect {}
        }
        viewModelScope.launch {
            combine(prefs.antiViewEnabled, prefs.silentModeEnabled, prefs.sendDelaySec, prefs.fakeUiEnabled, prefs.selfDestructAttempts)
            { av, sm, sd, fu, sda -> _state.update { it.copy(antiViewEnabled = av, silentModeEnabled = sm, sendDelaySec = sd, fakeUiEnabled = fu, selfDestructAttempts = sda) } }.collect {}
        }
    }

    fun toggle(key: Preferences.Key<Boolean>, current: Boolean) {
        viewModelScope.launch { prefs.set(key, !current) }
        when (key) {
            PrivacyPreferences.GHOST_MODE            -> app.ghostMode.isEnabled = !current
            PrivacyPreferences.SUPPRESS_TYPING       -> app.ghostMode.suppressTyping = !current
            PrivacyPreferences.CONTROL_READ_RECEIPTS -> app.ghostMode.controlReadReceipts = !current
            PrivacyPreferences.DELAYED_READ_MARKING  -> app.ghostMode.useDelayedReadMarking = !current
        }
    }

    fun setInt(key: Preferences.Key<Int>, value: Int) {
        viewModelScope.launch { prefs.set(key, value) }
    }

    fun toggleScreenshotsBlocked(activity: android.app.Activity) {
        val current = _state.value.screenshotsBlocked
        ScreenProtection.setScreenshotsBlocked(activity, !current)
        viewModelScope.launch { prefs.set(PrivacyPreferences.SCREENSHOTS_BLOCKED, !current) }
    }

    fun toggleGhostMode()          = toggle(PrivacyPreferences.GHOST_MODE, _state.value.ghostModeEnabled)
    fun toggleSuppressTyping()     = toggle(PrivacyPreferences.SUPPRESS_TYPING, _state.value.suppressTyping)
    fun toggleReadReceipts()       = toggle(PrivacyPreferences.CONTROL_READ_RECEIPTS, _state.value.controlReadReceipts)
    fun toggleDelayedReadMarking() = toggle(PrivacyPreferences.DELAYED_READ_MARKING, _state.value.delayedReadMarking)
    fun toggleAntiView()           = toggle(PrivacyPreferences.ANTI_VIEW_ENABLED, _state.value.antiViewEnabled)
    fun toggleSilentMode()         = toggle(PrivacyPreferences.SILENT_MODE_ENABLED, _state.value.silentModeEnabled)
    fun toggleMediaBlur()          = toggle(PrivacyPreferences.MEDIA_VISIBILITY_BLUR, _state.value.mediaVisibilityBlur)
    fun toggleBlurInRecents()      = toggle(PrivacyPreferences.BLUR_IN_RECENTS, _state.value.blurInRecents)
    fun toggleKillSwitch()         = toggle(PrivacyPreferences.KILL_SWITCH_ENABLED, _state.value.killSwitchEnabled)
    fun setSendDelay(sec: Int)     = setInt(PrivacyPreferences.SEND_DELAY_SEC, sec)
    fun setSelfDestructAttempts(n: Int) = setInt(PrivacyPreferences.SELF_DESTRUCT_ATTEMPTS, n)
}
