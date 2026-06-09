package com.amn3zia.app.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amn3zia.app.core.tdlib.AuthState
import com.amn3zia.app.ui.theme.TgColors

@Composable
fun AuthScreen(onAuthenticated: () -> Unit) {
    val viewModel: AuthViewModel = viewModel()
    val state by viewModel.authState.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(state) {
        if (state is AuthState.Ready) onAuthenticated()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TgColors.Bg),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            // Logo
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(TgColors.Blue),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "AMN3ZIA",
                color = TgColors.TextPrimary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp,
                letterSpacing = 4.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text("Privacy-first Telegram client", color = TgColors.TextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(40.dp))

            when (val s = state) {
                is AuthState.WaitingForPhoneNumber -> PhoneStep(onSubmit = viewModel::submitPhoneNumber)
                is AuthState.WaitingForCode        -> CodeStep(onSubmit = viewModel::submitCode)
                is AuthState.WaitingForPassword    -> PasswordStep(hint = s.hint, onSubmit = viewModel::submitPassword)
                is AuthState.WaitingForRegistration -> RegistrationStep(onSubmit = viewModel::register)
                else -> {
                    CircularProgressIndicator(color = TgColors.Blue, strokeWidth = 2.5.dp)
                    Spacer(Modifier.height(12.dp))
                    Text("Connecting…", color = TgColors.TextHint, fontSize = 13.sp)
                }
            }

            error?.let {
                Spacer(Modifier.height(16.dp))
                Text(it, color = TgColors.Red, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun PhoneStep(onSubmit: (String) -> Unit) {
    var phone by remember { mutableStateOf("") }
    StepColumn {
        Text("Your Phone Number", color = TgColors.TextPrimary, fontWeight = FontWeight.Medium, fontSize = 16.sp)
        Spacer(Modifier.height(4.dp))
        Text("Enter your number in international format", color = TgColors.TextSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))
        TgTextField(
            value = phone,
            onValueChange = { phone = it },
            label = "Phone (+1...)",
            keyboardType = KeyboardType.Phone,
        )
        Spacer(Modifier.height(12.dp))
        TgButton("Next") { onSubmit(phone.trim()) }
    }
}

@Composable
private fun CodeStep(onSubmit: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    StepColumn {
        Text("Enter Code", color = TgColors.TextPrimary, fontWeight = FontWeight.Medium, fontSize = 16.sp)
        Spacer(Modifier.height(4.dp))
        Text("Telegram sent you a verification code", color = TgColors.TextSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))
        TgTextField(value = code, onValueChange = { code = it }, label = "Code", keyboardType = KeyboardType.Number)
        Spacer(Modifier.height(12.dp))
        TgButton("Verify") { onSubmit(code.trim()) }
    }
}

@Composable
private fun PasswordStep(hint: String?, onSubmit: (String) -> Unit) {
    var password by remember { mutableStateOf("") }
    StepColumn {
        Text("Two-Step Password", color = TgColors.TextPrimary, fontWeight = FontWeight.Medium, fontSize = 16.sp)
        Spacer(Modifier.height(4.dp))
        if (!hint.isNullOrBlank()) Text("Hint: $hint", color = TgColors.TextSecondary, fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))
        TgTextField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            keyboardType = KeyboardType.Password,
            isPassword = true,
        )
        Spacer(Modifier.height(12.dp))
        TgButton("Sign In") { onSubmit(password) }
    }
}

@Composable
private fun RegistrationStep(onSubmit: (String, String) -> Unit) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    StepColumn {
        Text("Create Account", color = TgColors.TextPrimary, fontWeight = FontWeight.Medium, fontSize = 16.sp)
        Spacer(Modifier.height(4.dp))
        Text("You don't have a Telegram account yet", color = TgColors.TextSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))
        TgTextField(value = firstName, onValueChange = { firstName = it }, label = "First Name")
        Spacer(Modifier.height(8.dp))
        TgTextField(value = lastName, onValueChange = { lastName = it }, label = "Last Name")
        Spacer(Modifier.height(12.dp))
        TgButton("Register", enabled = firstName.isNotBlank()) { onSubmit(firstName.trim(), lastName.trim()) }
    }
}

// ── Shared input components ───────────────────────────────────────────────────

@Composable
private fun TgTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TgColors.TextHint, fontSize = 14.sp) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TgColors.TextPrimary,
            unfocusedTextColor = TgColors.TextPrimary,
            focusedBorderColor = TgColors.Blue,
            unfocusedBorderColor = TgColors.TextHint,
            focusedContainerColor = TgColors.BgSecondary,
            unfocusedContainerColor = TgColors.BgSecondary,
            cursorColor = TgColors.Blue,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun TgButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = TgColors.Blue,
            disabledContainerColor = TgColors.BgTertiary,
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().height(48.dp),
    ) {
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

@Composable
private fun StepColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}
