package com.amn3zia.app.ui.screens.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.amn3zia.app.AmnApplication
import com.amn3zia.app.core.tdlib.AuthState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val app get() = AmnApplication.from(getApplication())

    /** Ensures at least one (isolated) account/session exists, then exposes its auth state. */
    private val session by lazy {
        app.accounts.activeSession() ?: app.accounts.addAccount()
    }

    val authState: StateFlow<AuthState> by lazy {
        session.auth.state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AuthState.Initializing)
    }

    /** TEMPORARY: raw TdClient lifecycle trace, shown on-screen while debugging the launch hang. */
    val debugLog: StateFlow<List<String>> by lazy { session.client.debugLog }

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun submitPhoneNumber(phone: String) = launchGuarded { session.auth.submitPhoneNumber(phone) }
    fun submitCode(code: String) = launchGuarded { session.auth.submitCode(code) }
    fun submitPassword(password: String) = launchGuarded { session.auth.submitPassword(password) }
    fun register(firstName: String, lastName: String) = launchGuarded { session.auth.registerNewUser(firstName, lastName) }

    private fun launchGuarded(block: suspend () -> Unit) {
        _error.value = null
        viewModelScope.launch {
            runCatching { block() }.onFailure { _error.value = it.message ?: "Unknown error" }
        }
    }
}
