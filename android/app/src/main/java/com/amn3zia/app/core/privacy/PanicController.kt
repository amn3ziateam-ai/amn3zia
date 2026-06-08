package com.amn3zia.app.core.privacy

import com.amn3zia.app.core.account.AccountManager
import kotlin.random.Random

sealed interface PanicState {
    data object Idle : PanicState
    data class AwaitingConfirmation(val expectedCode: String) : PanicState
    data object Wiping : PanicState
    data object Done : PanicState
    data class Failed(val reason: String) : PanicState
}

/**
 * Panic Button business logic:
 *  1. generateChallenge() — produces a random 5-character code (e.g. "A7K92")
 *     the user must retype exactly to confirm — prevents accidental triggers.
 *  2. confirmAndWipe() — on exact match, irreversibly:
 *       - deletes all chats/messages/media/cache/session files (per account,
 *         via AccountManager.wipeAllAccountsLocally)
 *       - clears encryption keys & local databases
 *       - logs out of Telegram from THIS APP ONLY (TdApi.LogOut, not account deletion)
 *
 * This NEVER calls TdApi.DeleteAccount — the Telegram-side account is left intact,
 * per the hard requirement "DO NOT delete Telegram account from Telegram servers."
 */
class PanicController(private val accounts: AccountManager) {

    private val codeAlphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // no ambiguous chars (0/O, 1/I)

    fun generateChallenge(length: Int = 5): String =
        (1..length).map { codeAlphabet[Random.nextInt(codeAlphabet.length)] }.joinToString("")

    /**
     * Returns true only if [enteredCode] exactly matches [expectedCode]
     * (case-sensitive, no trimming beyond surrounding whitespace) — the
     * exact-match requirement is the deliberate friction that prevents
     * accidental wipes.
     */
    fun verifyChallenge(expectedCode: String, enteredCode: String): Boolean =
        expectedCode == enteredCode.trim()

    /**
     * Executes the irreversible wipe. Caller (ViewModel) is responsible for
     * driving [PanicState] transitions and surfacing the result to the UI.
     */
    suspend fun executeWipe() {
        accounts.wipeAllAccountsLocally()
    }
}
