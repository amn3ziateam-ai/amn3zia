package com.amn3zia.app.ui.screens.panic

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.amn3zia.app.AmnApplication
import com.amn3zia.app.core.privacy.PanicState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PanicViewModel(application: Application) : AndroidViewModel(application) {

    private val app get() = AmnApplication.from(getApplication())

    private val _state = MutableStateFlow<PanicState>(PanicState.Idle)
    val state: StateFlow<PanicState> = _state.asStateFlow()

    private val _input = MutableStateFlow("")
    val input: StateFlow<String> = _input.asStateFlow()

    /** Step 1: user tapped the big red button — generate and show the challenge code. */
    fun onPanicButtonTapped() {
        val code = app.panic.generateChallenge()
        _input.value = ""
        _state.value = PanicState.AwaitingConfirmation(code)
    }

    fun onInputChanged(value: String) {
        _input.value = value.uppercase()
    }

    fun cancel() {
        _state.value = PanicState.Idle
        _input.value = ""
    }

    /** Step 2: user submitted their typed code — verify exact match, then wipe irreversibly. */
    fun onConfirm() {
        val current = _state.value as? PanicState.AwaitingConfirmation ?: return
        if (!app.panic.verifyChallenge(current.expectedCode, _input.value)) {
            _state.value = PanicState.Failed("Code does not match. Nothing was deleted.")
            return
        }
        _state.value = PanicState.Wiping
        viewModelScope.launch {
            runCatching { app.panic.executeWipe() }
                .onSuccess { _state.value = PanicState.Done }
                .onFailure { _state.value = PanicState.Failed(it.message ?: "Wipe failed") }
        }
    }

    fun acknowledgeFailureAndRetry() {
        _state.value = PanicState.Idle
        _input.value = ""
    }
}
