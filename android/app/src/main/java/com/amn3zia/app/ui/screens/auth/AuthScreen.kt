package com.amn3zia.app.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amn3zia.app.core.tdlib.AuthState

/** Mirrors TDLib's authorization-state machine exactly — each state gets its own input step. */
@Composable
fun AuthScreen(onAuthenticated: () -> Unit) {
    val viewModel: AuthViewModel = viewModel()
    val state by viewModel.authState.collectAsState()
    val error by viewModel.error.collectAsState()
    val debugLog by viewModel.debugLog.collectAsState()

    LaunchedEffect(state) {
        if (state is AuthState.Ready) onAuthenticated()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("AMN3ZIA", fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, letterSpacing = 4.sp)
        Spacer(Modifier.height(8.dp))
        Text("Privacy-first Telegram client", fontSize = 13.sp)
        Spacer(Modifier.height(40.dp))

        when (val s = state) {
            is AuthState.WaitingForPhoneNumber -> PhoneStep(onSubmit = viewModel::submitPhoneNumber)
            is AuthState.WaitingForCode -> CodeStep(onSubmit = viewModel::submitCode)
            is AuthState.WaitingForPassword -> PasswordStep(hint = s.hint, onSubmit = viewModel::submitPassword)
            is AuthState.WaitingForRegistration -> RegistrationStep(onSubmit = viewModel::register)
            is AuthState.Initializing -> {
                CircularProgressIndicator()
                DebugTrace(debugLog)
            }
            else -> {
                CircularProgressIndicator()
                DebugTrace(debugLog)
            }
        }

        error?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }
    }
}

/**
 * TEMPORARY: shows TdClient's raw lifecycle trace directly on screen.
 * adb/logcat is unreliable on the test emulator, so this is the fastest way
 * to see exactly where TDLib initialization is stuck. Remove once the
 * launch-hang investigation is done.
 */
@Composable
private fun DebugTrace(lines: List<String>) {
    Spacer(Modifier.height(24.dp))
    Text("debug trace:", fontSize = 11.sp, color = Color.Gray)
    Spacer(Modifier.height(4.dp))
    LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
        items(lines) { line ->
            Text(line, fontSize = 10.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun PhoneStep(onSubmit: (String) -> Unit) {
    var phone by remember { mutableStateOf("") }
    StepColumn {
        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone number (+1...)") }, singleLine = true)
        Button(onClick = { onSubmit(phone.trim()) }, enabled = phone.isNotBlank()) { Text("Continue") }
    }
}

@Composable
private fun CodeStep(onSubmit: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    StepColumn {
        Text("Enter the code Telegram sent you", fontSize = 13.sp)
        OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Code") }, singleLine = true)
        Button(onClick = { onSubmit(code.trim()) }, enabled = code.isNotBlank()) { Text("Verify") }
    }
}

@Composable
private fun PasswordStep(hint: String?, onSubmit: (String) -> Unit) {
    var password by remember { mutableStateOf("") }
    StepColumn {
        Text("Two-step verification password" + (hint?.takeIf { it.isNotBlank() }?.let { " (hint: $it)" } ?: ""), fontSize = 13.sp)
        OutlinedTextField(
            value = password, onValueChange = { password = it }, label = { Text("Password") }, singleLine = true,
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
        )
        Button(onClick = { onSubmit(password) }, enabled = password.isNotBlank()) { Text("Unlock") }
    }
}

@Composable
private fun RegistrationStep(onSubmit: (String, String) -> Unit) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    StepColumn {
        Text("Looks like you're new — create your Telegram profile", fontSize = 13.sp)
        OutlinedTextField(value = firstName, onValueChange = { firstName = it }, label = { Text("First name") }, singleLine = true)
        OutlinedTextField(value = lastName, onValueChange = { lastName = it }, label = { Text("Last name") }, singleLine = true)
        Button(onClick = { onSubmit(firstName.trim(), lastName.trim()) }, enabled = firstName.isNotBlank()) { Text("Create account") }
    }
}

@Composable
private fun StepColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally, content = content)
}
