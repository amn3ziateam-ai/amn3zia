package com.amn3zia.app

import android.app.Application
import com.amn3zia.app.core.account.AccountManager
import com.amn3zia.app.core.privacy.AutoCleanManager
import com.amn3zia.app.core.privacy.EncryptionManager
import com.amn3zia.app.core.privacy.GhostModeManager
import com.amn3zia.app.core.privacy.PanicController
import com.amn3zia.app.core.privacy.SelfDestructManager

/**
 * App-wide singletons. Kept minimal and explicit (no DI framework) so the
 * privacy layer's lifecycle is easy to reason about and audit.
 */
class AmnApplication : Application() {

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

    override fun onCreate() {
        super.onCreate()
        encryption = EncryptionManager(this)
        accounts = AccountManager(this, encryption)
        ghostMode = GhostModeManager(this)
        autoClean = AutoCleanManager(this, accounts)
        panic = PanicController(accounts)
        selfDestruct = SelfDestructManager(this, accounts)

        accounts.restoreSavedAccounts()
        autoClean.scheduleTriggersOnAppStart()
    }

    companion object {
        fun from(context: android.content.Context): AmnApplication =
            context.applicationContext as AmnApplication
    }
}
