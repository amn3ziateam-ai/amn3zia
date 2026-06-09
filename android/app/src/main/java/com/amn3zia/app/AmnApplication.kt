package com.amn3zia.app

import android.app.Application
import com.amn3zia.app.core.account.AccountManager
import com.amn3zia.app.core.privacy.*
import com.amn3zia.app.core.settings.PrivacyPreferences

/**
 * App-wide singletons. Kept minimal and explicit (no DI framework) so the
 * privacy layer's lifecycle is easy to reason about and audit.
 */
class AmnApplication : Application() {

    lateinit var prefs: PrivacyPreferences
        private set
    lateinit var encryption: EncryptionManager
        private set
    lateinit var accounts: AccountManager
        private set
    lateinit var ghostMode: GhostModeManager
        private set
    lateinit var autoClean: AutoCleanManager
        private set
    lateinit var panic: PanicController
        private set
    lateinit var selfDestruct: SelfDestructManager
        private set
    lateinit var appLock: AppLockManager
        private set
    lateinit var hiddenChats: HiddenChatManager
        private set
    lateinit var sendDelay: SendDelayManager
        private set
    lateinit var fakeUi: FakeUiManager
        private set

    override fun onCreate() {
        super.onCreate()
        prefs = PrivacyPreferences(this)
        encryption = EncryptionManager(this)
        accounts = AccountManager(this, encryption)
        ghostMode = GhostModeManager(this)
        selfDestruct = SelfDestructManager(this, accounts)
        appLock = AppLockManager(this, prefs, selfDestruct)
        hiddenChats = HiddenChatManager(this, prefs)
        sendDelay = SendDelayManager()
        fakeUi = FakeUiManager(prefs)
        autoClean = AutoCleanManager(this, accounts)
        panic = PanicController(accounts)

        accounts.restoreSavedAccounts()
        autoClean.scheduleTriggersOnAppStart()
    }

    companion object {
        fun from(context: android.content.Context): AmnApplication =
            context.applicationContext as AmnApplication
    }
}
