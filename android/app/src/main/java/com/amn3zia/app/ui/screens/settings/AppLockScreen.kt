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
import com.amn3zia.app.ui.theme.TgColors

private val PAD = listOf("1","2","3","4","5","6","7","8","9","","0","⌫")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLockScreen(onBack: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var isConfirming by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }
    var lockEnabled by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = TgColors.Bg,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TgColors.BgSecondary,
                    titleContentColor = TgColors.TextPrimary,
                    navigationIconContentColor = TgColors.Blue,
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TgColors.Blue)
                    }
                },
                title = { Text("App Lock", fontWeight = FontWeight.Medium, fontSize = 18.sp) },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(32.dp))

            // Icon
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(TgColors.BgSecondary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (lockEnabled) Icons.Filled.LockOpen else Icons.Filled.Lock,
                    contentDescription = null,
                    tint = TgColors.Blue,
                    modifier = Modifier.size(36.dp),
                )
            }

            Spacer(Modifier.height(20.dp))
            Text(
                if (!isConfirming) "Enter a PIN" else "Confirm PIN",
                color = TgColors.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (errorMsg.isNotEmpty()) errorMsg else "4–6 digits",
                color = if (errorMsg.isNotEmpty()) TgColors.Red else TgColors.TextHint,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(24.dp))

            // PIN dots
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                val current = if (isConfirming) confirmPin else pin
                repeat(6) { i ->
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(if (i < current.length) TgColors.Blue else Color.Transparent)
                            .border(1.5.dp, if (i < current.length) TgColors.Blue else TgColors.TextHint, CircleShape),
                    )
                }
            }

            Spacer(Modifier.height(36.dp))

            // Numpad
            val current = if (isConfirming) confirmPin else pin
            Column(
                modifier = Modifier.padding(horizontal = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PAD.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        row.forEach { key ->
                            PadKey(key) {
                                errorMsg = ""
                                when (key) {
                                    "⌫" -> {
                                        if (isConfirming) { if (confirmPin.isNotEmpty()) confirmPin = confirmPin.dropLast(1) }
                                        else { if (pin.isNotEmpty()) pin = pin.dropLast(1) }
                                    }
                                    "" -> {}
                                    else -> {
                                        if (!isConfirming && pin.length < 6) {
                                            pin += key
                                            if (pin.length >= 4) {
                                                // ready — wait for next press or user can tap confirm
                                            }
                                        } else if (isConfirming && confirmPin.length < 6) {
                                            confirmPin += key
                                        }
                                        // Auto-submit at 6 digits
                                        val newVal = if (isConfirming) confirmPin else pin
                                        if (newVal.length == 6) {
                                            if (!isConfirming) {
                                                isConfirming = true
                                            } else {
                                                if (confirmPin == pin) {
                                                    lockEnabled = true
                                                    onBack()
                                                } else {
                                                    errorMsg = "PINs don't match — try again"
                                                    pin = ""; confirmPin = ""; isConfirming = false
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Confirm button (for 4–5 digit PINs)
            val currentLen = (if (isConfirming) confirmPin else pin).length
            if (currentLen in 4..5) {
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = {
                        if (!isConfirming) {
                            isConfirming = true
                        } else {
                            if (confirmPin == pin) {
                                lockEnabled = true
                                onBack()
                            } else {
                                errorMsg = "PINs don't match — try again"
                                pin = ""; confirmPin = ""; isConfirming = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TgColors.Blue),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.padding(horizontal = 48.dp).fillMaxWidth(),
                ) {
                    Text(if (isConfirming) "Confirm" else "Continue", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun PadKey(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(if (label.isNotEmpty()) TgColors.BgSecondary else Color.Transparent)
            .clickable(enabled = label.isNotEmpty(), onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (label == "⌫") {
            Icon(Icons.Filled.Backspace, contentDescription = "Delete", tint = TgColors.TextSecondary, modifier = Modifier.size(22.dp))
        } else if (label.isNotEmpty()) {
            Text(label, color = TgColors.TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Light, textAlign = TextAlign.Center)
        }
    }
}
