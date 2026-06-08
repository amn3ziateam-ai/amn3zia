package com.amn3zia.app.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.amn3zia.app.AmnApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PrivacySettingsState(
    val ghostModeEnabled: Boolean,
    val suppressTyping: Boolean,
    val controlReadReceipts: Boolean,
    val delayedReadMarking: Boolean,
    val disableLinkPreviews: Boolean,
    val blockExternalMedia: Boolean,
    val screenshotsBlocked: Boolean,
)

/** Single source of truth feeding the Privacy Dashboard — "all controls in one place". */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app get() = AmnApplication.from(getApplication())

    private val _state = MutableStateFlow(snapshot())
    val state: StateFlow<PrivacySettingsState> = _state.asStateFlow()

    private fun snapshot() = PrivacySettingsState(
        ghostModeEnabled = app.ghostMode.isEnabled,
        suppressTyping = app.ghostMode.suppressTyping,
        controlReadReceipts = app.ghostMode.controlReadReceipts,
        delayedReadMarking = app.ghostMode.useDelayedReadMarking,
        disableLinkPreviews = true,
        blockExternalMedia = true,
        screenshotsBlocked = com.amn3zia.app.core.privacy.ScreenProtection.screenshotsBlocked,
    )

    private fun refresh() { _state.value = snapshot() }

    fun toggleGhostMode() { app.ghostMode.isEnabled = !app.ghostMode.isEnabled; refresh() }
    fun toggleSuppressTyping() { app.ghostMode.suppressTyping = !app.ghostMode.suppressTyping; refresh() }
    fun toggleReadReceipts() { app.ghostMode.controlReadReceipts = !app.ghostMode.controlReadReceipts; refresh() }
    fun toggleDelayedReadMarking() { app.ghostMode.useDelayedReadMarking = !app.ghostMode.useDelayedReadMarking; refresh() }

    fun toggleScreenshotsBlocked(activity: android.app.Activity) {
        com.amn3zia.app.core.privacy.ScreenProtection.setScreenshotsBlocked(
            activity, !com.amn3zia.app.core.privacy.ScreenProtection.screenshotsBlocked,
        )
        refresh()
    }
}
