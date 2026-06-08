package com.amn3zia.app.core.tdlib

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import org.drinkless.td.libcore.telegram.TdApi

/**
 * Adapts TDLib's authorization-state machine to a simple sealed [AuthState]
 * stream + suspend functions for each step. Mirrors the official TDLib auth
 * flow exactly (we never bypass or shortcut Telegram's login protocol).
 */
class AuthRepository(private val client: TdClient) {

    val state: Flow<AuthState> = client.updates
        .filterIsInstance<TdApi.UpdateAuthorizationState>()
        .map { it.authorizationState.toAuthState() }

    suspend fun submitPhoneNumber(phoneNumber: String) {
        client.send(TdApi.SetAuthenticationPhoneNumber(phoneNumber, null))
    }

    suspend fun submitCode(code: String) {
        client.send(TdApi.CheckAuthenticationCode(code))
    }

    suspend fun submitPassword(password: String) {
        client.send(TdApi.CheckAuthenticationPassword(password))
    }

    suspend fun registerNewUser(firstName: String, lastName: String) {
        client.send(TdApi.RegisterUser(firstName, lastName))
    }

    /** Local logout only — never deletes the account from Telegram's servers. */
    suspend fun logOut() {
        client.send(TdApi.LogOut())
    }

    private fun TdApi.AuthorizationState.toAuthState(): AuthState = when (this) {
        is TdApi.AuthorizationStateWaitTdlibParameters -> AuthState.Initializing
        is TdApi.AuthorizationStateWaitPhoneNumber -> AuthState.WaitingForPhoneNumber
        is TdApi.AuthorizationStateWaitCode -> AuthState.WaitingForCode
        is TdApi.AuthorizationStateWaitPassword -> AuthState.WaitingForPassword(passwordHint)
        is TdApi.AuthorizationStateWaitRegistration -> AuthState.WaitingForRegistration
        is TdApi.AuthorizationStateReady -> AuthState.Ready
        is TdApi.AuthorizationStateLoggingOut -> AuthState.LoggingOut
        is TdApi.AuthorizationStateClosing -> AuthState.Closing
        is TdApi.AuthorizationStateClosed -> AuthState.Closed
        else -> AuthState.Unknown
    }
}

sealed interface AuthState {
    data object Initializing : AuthState
    data object WaitingForPhoneNumber : AuthState
    data object WaitingForCode : AuthState
    data class WaitingForPassword(val hint: String?) : AuthState
    data object WaitingForRegistration : AuthState
    data object Ready : AuthState
    data object LoggingOut : AuthState
    data object Closing : AuthState
    data object Closed : AuthState
    data object Unknown : AuthState
}
