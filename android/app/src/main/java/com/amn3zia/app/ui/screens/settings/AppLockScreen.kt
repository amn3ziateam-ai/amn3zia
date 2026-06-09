package com.amn3zia.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amn3zia.app.ui.theme.AmnColors

private val PAD = listOf("1","2","3","4","5","6","7","8","9","","0","⌫")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLockScreen(onBack: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var isConfirming by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    Scaffold(
        containerColor = AmnColors.Background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AmnColors.Surface,
                    titleContentColor = AmnColors.TextPrimary,
                    navigationIconContentColor = AmnColors.Primary,
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = AmnColors.Primary)
                    }
                },
                title = { Text("App Lock", fontWeight = FontWeight.SemiBold, fontSize = 17.sp) },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(36.dp))

            // Lock icon
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(AmnColors.PrimaryLight),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (isConfirming) Icons.Filled.LockOpen else Icons.Filled.Lock,
                    null,
                    tint = AmnColors.Primary,
                    modifier = Modifier.size(34.dp),
                )
            }

            Spacer(Modifier.height(20.dp))
            Text(
                if (!isConfirming) "Set PIN Code" else "Confirm PIN Code",
                color = AmnColors.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (errorMsg.isNotEmpty()) errorMsg else "Enter 4–6 digits",
                color = if (errorMsg.isNotEmpty()) AmnColors.Error else AmnColors.TextTertiary,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(24.dp))

            // PIN dots
            val current = if (isConfirming) confirmPin else pin
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                repeat(6) { i ->
                    val filled = i < current.length
                    Box(
                        modifier = Modifier
                            .size(13.dp)
                            .clip(CircleShape)
                            .background(if (filled) AmnColors.Primary else Color.Transparent)
                            .border(1.5.dp, if (filled) AmnColors.Primary else AmnColors.Border, CircleShape),
                    )
                }
            }

            Spacer(Modifier.height(36.dp))

            // Numpad
            Column(
                modifier = Modifier.padding(horizontal = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PAD.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        row.forEach { key ->
                            PinKey(key) {
                                errorMsg = ""
                                when (key) {
                                    "⌫" -> {
                                        if (isConfirming) { if (confirmPin.isNotEmpty()) confirmPin = confirmPin.dropLast(1) }
                                        else { if (pin.isNotEmpty()) pin = pin.dropLast(1) }
                                    }
                                    "" -> {}
                                    else -> {
                                        if (!isConfirming && pin.length < 6) pin += key
                                        else if (isConfirming && confirmPin.length < 6) confirmPin += key

                                        val newVal = if (isConfirming) confirmPin else pin
                                        if (newVal.length == 6) autoSubmit(
                                            pin = pin, confirmPin = confirmPin, isConfirming = isConfirming,
                                            onAdvance = { isConfirming = true },
                                            onSuccess = { onBack() },
                                            onMismatch = { errorMsg = "PINs don't match. Try again."; pin = ""; confirmPin = ""; isConfirming = false },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Manual confirm button for 4–5 digit PINs
            val curLen = (if (isConfirming) confirmPin else pin).length
            if (curLen in 4..5) {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        autoSubmit(
                            pin = pin, confirmPin = confirmPin, isConfirming = isConfirming,
                            onAdvance = { isConfirming = true },
                            onSuccess = { onBack() },
                            onMismatch = { errorMsg = "PINs don't match. Try again."; pin = ""; confirmPin = ""; isConfirming = false },
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmnColors.Primary),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(0.dp),
                    modifier = Modifier.padding(horizontal = 40.dp).fillMaxWidth(),
                ) {
                    Text(if (isConfirming) "Confirm" else "Next", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private fun autoSubmit(
    pin: String,
    confirmPin: String,
    isConfirming: Boolean,
    onAdvance: () -> Unit,
    onSuccess: () -> Unit,
    onMismatch: () -> Unit,
) {
    if (!isConfirming) {
        onAdvance()
    } else {
        if (confirmPin == pin) onSuccess() else onMismatch()
    }
}

@Composable
private fun PinKey(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(74.dp)
            .clip(CircleShape)
            .background(if (label.isNotEmpty()) AmnColors.Surface else Color.Transparent)
            .then(if (label.isNotEmpty()) Modifier.border(1.dp, AmnColors.Border, CircleShape) else Modifier)
            .clickable(enabled = label.isNotEmpty(), onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        when {
            label == "⌫" -> Icon(Icons.Filled.Backspace, null, tint = AmnColors.TextSecondary, modifier = Modifier.size(20.dp))
            label.isNotEmpty() -> Text(label, color = AmnColors.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Light, textAlign = TextAlign.Center)
        }
    }
}
