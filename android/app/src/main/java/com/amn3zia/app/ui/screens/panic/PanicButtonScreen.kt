package com.amn3zia.app.ui.screens.panic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amn3zia.app.core.privacy.PanicState

private val PanicRed = Color(0xFFE6294B)

/**
 * Big red panic button — placed in Settings (and optionally pinned to a
 * bottom-left FAB on the chat list, see ChatListScreen). Two-step confirmation
 * with a randomly generated code is mandatory before any deletion happens.
 */
@Composable
fun PanicButtonScreen(onWipeComplete: () -> Unit) {
    val viewModel: PanicViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    val input by viewModel.input.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0F)), contentAlignment = Alignment.Center) {
        when (val s = state) {
            is PanicState.Idle -> IdleContent(onTap = viewModel::onPanicButtonTapped)
            is PanicState.AwaitingConfirmation -> ConfirmationContent(
                expectedCode = s.expectedCode,
                input = input,
                onInputChanged = viewModel::onInputChanged,
                onConfirm = viewModel::onConfirm,
                onCancel = viewModel::cancel,
            )
            is PanicState.Wiping -> WipingContent()
            is PanicState.Done -> {
                DoneContent()
                LaunchedEffect(Unit) { onWipeComplete() }
            }
            is PanicState.Failed -> FailedContent(reason = s.reason, onRetry = viewModel::acknowledgeFailureAndRetry)
        }
    }
}

@Composable
private fun IdleContent(onTap: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("PANIC BUTTON", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(
            "Instantly and irreversibly erase all local chats, media, cache and\nlog out of this app — without deleting your Telegram account.",
            color = Color.Gray, fontSize = 13.sp,
        )
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(PanicRed)
                .clickable(onClick = onTap),
            contentAlignment = Alignment.Center,
        ) {
            Text("WIPE\nNOW", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
private fun ConfirmationContent(
    expectedCode: String,
    input: String,
    onInputChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("⚠ THIS CANNOT BE UNDONE", color = PanicRed, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text("Type the code exactly to confirm permanent deletion:", color = Color.White, fontSize = 14.sp)
        Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF1A1A22)) {
            Text(
                expectedCode, color = PanicRed, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp,
                letterSpacing = 8.sp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            )
        }
        OutlinedTextField(
            value = input,
            onValueChange = onInputChanged,
            label = { Text("Confirmation code") },
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
            ),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onCancel) { Text("Cancel") }
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = PanicRed),
                enabled = input.isNotBlank(),
            ) { Text("ERASE EVERYTHING") }
        }
    }
}

@Composable
private fun WipingContent() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CircularProgressIndicator(color = PanicRed)
        Text("Erasing local data…", color = Color.White)
    }
}

@Composable
private fun DoneContent() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("✓ Done", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text("All local data was erased and you were logged out of this app.\nYour Telegram account itself was not touched.", color = Color.Gray, fontSize = 13.sp)
    }
}

@Composable
private fun FailedContent(reason: String, onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("✕ $reason", color = Color.White)
        Button(onClick = onRetry) { Text("OK") }
    }
}
