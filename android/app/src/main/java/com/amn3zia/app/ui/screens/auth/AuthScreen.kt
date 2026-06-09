package com.amn3zia.app.ui.screens.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amn3zia.app.core.tdlib.AuthState
import com.amn3zia.app.ui.theme.AmnColors

@Composable
fun AuthScreen(onAuthenticated: () -> Unit) {
    val viewModel: AuthViewModel = viewModel()
    val state by viewModel.authState.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(state) {
        if (state is AuthState.Ready) onAuthenticated()
    }

    when (state) {
        is AuthState.Initializing -> SplashScreen()
        else -> LoginScreen(state = state, error = error, viewModel = viewModel)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SPLASH SCREEN
// Per TZ: this is the ONLY screen allowed to differ from Telegram visually.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SplashScreen() {
    // Animate the progress bar width
    val transition = rememberInfiniteTransition(label = "splash")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = 1800, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "progress",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AmnColors.Background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            // ── Logo ──────────────────────────────────────────────────────
            AmnLogoMark(size = 88)

            Spacer(Modifier.height(20.dp))

            // ── App name ──────────────────────────────────────────────────
            Text(
                text = "AMN3ZIA",
                color = AmnColors.TextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Private Messenger",
                color = AmnColors.TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.5.sp,
            )

            Spacer(Modifier.height(40.dp))

            // ── Progress bar ──────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(AmnColors.Border),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = progress.coerceIn(0.1f, 0.9f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(1.dp))
                        .background(AmnColors.Primary),
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = "Connecting securely...",
                color = AmnColors.TextTertiary,
                fontSize = 12.sp,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LOGIN FLOW  (mirrors Telegram's phone → code → password → ready flow exactly)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LoginScreen(
    state: AuthState,
    error: String?,
    viewModel: AuthViewModel,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AmnColors.Background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Logo (smaller on login screens)
            AmnLogoMark(size = 64)
            Spacer(Modifier.height(16.dp))

            Text(
                "AMN3ZIA",
                color = AmnColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                letterSpacing = 3.sp,
            )
            Spacer(Modifier.height(32.dp))

            when (val s = state) {
                is AuthState.WaitingForPhoneNumber ->
                    PhoneStep(onSubmit = viewModel::submitPhoneNumber)

                is AuthState.WaitingForCode ->
                    CodeStep(onSubmit = viewModel::submitCode)

                is AuthState.WaitingForPassword ->
                    PasswordStep(hint = s.hint, onSubmit = viewModel::submitPassword)

                is AuthState.WaitingForRegistration ->
                    RegistrationStep(onSubmit = viewModel::register)

                else -> {
                    CircularProgressIndicator(
                        color = AmnColors.Primary,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }

            error?.let {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = it,
                    color = AmnColors.Error,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Step composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PhoneStep(onSubmit: (String) -> Unit) {
    var phone by remember { mutableStateOf("") }
    StepColumn {
        Text("Your Phone", color = AmnColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
        Spacer(Modifier.height(6.dp))
        Text("Enter your number with country code", color = AmnColors.TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        AmnTextField(value = phone, onValueChange = { phone = it }, label = "+1 000 000 0000", keyboardType = KeyboardType.Phone)
        Spacer(Modifier.height(16.dp))
        AmnButton("Continue") { onSubmit(phone.trim()) }
    }
}

@Composable
private fun CodeStep(onSubmit: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    StepColumn {
        Text("Verification Code", color = AmnColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
        Spacer(Modifier.height(6.dp))
        Text("We sent a code to your Telegram app", color = AmnColors.TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        AmnTextField(value = code, onValueChange = { code = it }, label = "Code", keyboardType = KeyboardType.Number)
        Spacer(Modifier.height(16.dp))
        AmnButton("Verify") { onSubmit(code.trim()) }
    }
}

@Composable
private fun PasswordStep(hint: String?, onSubmit: (String) -> Unit) {
    var password by remember { mutableStateOf("") }
    StepColumn {
        Text("Two-Step Verification", color = AmnColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
        Spacer(Modifier.height(6.dp))
        if (!hint.isNullOrBlank()) {
            Text("Hint: $hint", color = AmnColors.TextTertiary, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
        }
        Text("Enter your cloud password", color = AmnColors.TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        AmnTextField(value = password, onValueChange = { password = it }, label = "Password",
            keyboardType = KeyboardType.Password, isPassword = true)
        Spacer(Modifier.height(16.dp))
        AmnButton("Sign In") { onSubmit(password) }
    }
}

@Composable
private fun RegistrationStep(onSubmit: (String, String) -> Unit) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    StepColumn {
        Text("Create Account", color = AmnColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
        Spacer(Modifier.height(6.dp))
        Text("You don't have a Telegram account yet.\nPlease create one.", color = AmnColors.TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        AmnTextField(value = firstName, onValueChange = { firstName = it }, label = "First Name")
        Spacer(Modifier.height(10.dp))
        AmnTextField(value = lastName, onValueChange = { lastName = it }, label = "Last Name (optional)")
        Spacer(Modifier.height(16.dp))
        AmnButton("Register", enabled = firstName.isNotBlank()) { onSubmit(firstName.trim(), lastName.trim()) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared sub-components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AmnLogoMark(size: Int) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size * 0.27f).dp))
            .background(AmnColors.Primary),
        contentAlignment = Alignment.Center,
    ) {
        // Stylised "A" monogram
        Text(
            text = "A",
            color = Color.White,
            fontSize = (size * 0.46f).sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun AmnTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = AmnColors.TextTertiary, fontSize = 14.sp) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor         = AmnColors.TextPrimary,
            unfocusedTextColor       = AmnColors.TextPrimary,
            focusedBorderColor       = AmnColors.Primary,
            unfocusedBorderColor     = AmnColors.Border,
            focusedContainerColor    = AmnColors.Surface,
            unfocusedContainerColor  = AmnColors.Surface,
            cursorColor              = AmnColors.Primary,
            focusedLabelColor        = AmnColors.Primary,
            unfocusedLabelColor      = AmnColors.TextTertiary,
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun AmnButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor         = AmnColors.Primary,
            disabledContainerColor = AmnColors.SurfaceAlt,
            contentColor           = Color.White,
            disabledContentColor   = AmnColors.TextDisabled,
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
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
