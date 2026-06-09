package com.amn3zia.app.core.privacy

import com.amn3zia.app.core.settings.PrivacyPreferences
import kotlinx.coroutines.flow.first

/**
 * Fake UI — when enabled the app launches as a calculator.
 * Entering the unlock gesture (default: "1337=") in the calculator
 * transitions to the real AMN3ZIA UI.
 *
 * The actual fake UI screen is [com.amn3zia.app.ui.screens.fakeui.CalculatorScreen].
 * [MainActivity] checks [shouldShowFakeUi] on start and routes accordingly.
 */
class FakeUiManager(private val prefs: PrivacyPreferences) {

    suspend fun isEnabled(): Boolean = prefs.fakeUiEnabled.first()

    suspend fun unlockGesture(): String = prefs.fakeUiGesture.first()

    /** Returns true if [input] matches the secret gesture. */
    suspend fun isUnlockGesture(input: String): Boolean = input == prefs.fakeUiGesture.first()

    suspend fun enable(gesture: String) {
        prefs.set(PrivacyPreferences.FAKE_UI_ENABLED, true)
        prefs.set(PrivacyPreferences.FAKE_UI_UNLOCK_GESTURE, gesture)
    }

    suspend fun disable() {
        prefs.set(PrivacyPreferences.FAKE_UI_ENABLED, false)
    }
}
