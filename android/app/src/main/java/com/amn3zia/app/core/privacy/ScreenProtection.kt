package com.amn3zia.app.core.privacy

import android.app.Activity
import android.view.WindowManager

/**
 * Screen Protection:
 *  - blockScreenshots: FLAG_SECURE blocks screenshots/screen recording AND
 *    automatically blurs the activity in the recent-apps switcher (Android's
 *    OS-level behavior for FLAG_SECURE windows) — covers two requirements at once.
 *  - blurSensitiveContent: an additional in-UI blur overlay (see ChatScreen)
 *    for "blur until tapped" on individual messages/media, independent of FLAG_SECURE.
 */
object ScreenProtection {

    @Volatile var screenshotsBlocked: Boolean = true

    fun applyTo(activity: Activity) {
        if (screenshotsBlocked) {
            activity.window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    fun setScreenshotsBlocked(activity: Activity, blocked: Boolean) {
        screenshotsBlocked = blocked
        applyTo(activity)
    }
}
