package com.amn3zia.app.ui.screens.panic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amn3zia.app.ui.theme.TgColors
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanicScreen(onBack: () -> Unit, onWipe: () -> Unit = {}) {
    val code = remember { String.format("%06d", Random.nextInt(0, 999999)) }
    var input by remember { mutableStateOf("") }
    var confirmed by remember { mutableStateOf(false) }

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
                title = { Text("Panic — Wipe Everything", fontWeight = FontWeight.Medium, fontSize = 18.sp, color = TgColors.Red) },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(40.dp))

            // Warning icon
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(TgColors.Red.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Warning, contentDescription = null, tint = TgColors.Red, modifier = Modifier.size(44.dp))
            }

            Spacer(Modifier.height(24.dp))
            Text("IRREVERSIBLE ACTION", color = TgColors.Red, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                "This will permanently delete:\n• All local chat messages\n• All media and files\n• Encryption keys\n• Database and cache\n\nYou will be logged out.\nYour Telegram account will NOT be deleted.",
                color = TgColors.TextSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(32.dp))

            // Confirmation code box
            Surface(
                color = TgColors.BgSecondary,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Type this code to confirm:", color = TgColors.TextSecondary, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(code, color = TgColors.TextPrimary, fontSize = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = 8.sp)
                }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = input,
                onValueChange = { if (it.length <= 6) input = it },
                placeholder = { Text("Enter code", color = TgColors.TextHint) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TgColors.TextPrimary,
                    unfocusedTextColor = TgColors.TextPrimary,
                    focusedBorderColor = TgColors.Red,
                    unfocusedBorderColor = TgColors.TextHint,
                    cursorColor = TgColors.Red,
                    focusedContainerColor = TgColors.BgSecondary,
                    unfocusedContainerColor = TgColors.BgSecondary,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    if (input == code) {
                        confirmed = true
                        onWipe()
                    }
                },
                enabled = input == code,
                colors = ButtonDefaults.buttonColors(
                    containerColor = TgColors.PanicRed,
                    disabledContainerColor = TgColors.BgSecondary,
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Icon(Icons.Filled.DeleteForever, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("WIPE ALL DATA", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(Modifier.height(16.dp))

            TextButton(onClick = onBack) {
                Text("Cancel", color = TgColors.TextSecondary, fontSize = 14.sp)
            }
        }
    }
}
