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
import com.amn3zia.app.ui.theme.AmnColors
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanicScreen(onBack: () -> Unit, onWipe: () -> Unit = {}) {
    val code = remember { String.format("%06d", Random.nextInt(0, 999999)) }
    var input by remember { mutableStateOf("") }

    Scaffold(
        containerColor = AmnColors.Background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AmnColors.Surface,
                    titleContentColor = AmnColors.Error,
                    navigationIconContentColor = AmnColors.Primary,
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = AmnColors.Primary)
                    }
                },
                title = { Text("Emergency Wipe", fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = AmnColors.Error) },
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
            Spacer(Modifier.height(36.dp))

            // Warning icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(AmnColors.Error.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Warning, null, tint = AmnColors.Error, modifier = Modifier.size(40.dp))
            }

            Spacer(Modifier.height(20.dp))
            Text("IRREVERSIBLE ACTION", color = AmnColors.Error, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(12.dp))

            Text(
                text = "This will permanently delete:\n• All messages & media\n• Encryption keys\n• App database & cache\n\nYou will be signed out.\nYour Telegram account will NOT be deleted.",
                color = AmnColors.TextSecondary,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(28.dp))

            // Confirmation code card
            Surface(
                color = AmnColors.Surface,
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 0.dp,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Type this code to confirm", color = AmnColors.TextSecondary, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(code, color = AmnColors.TextPrimary, fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = 8.sp)
                }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = input,
                onValueChange = { if (it.length <= 6) input = it },
                placeholder = { Text("Enter confirmation code", color = AmnColors.TextTertiary) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor      = AmnColors.TextPrimary,
                    unfocusedTextColor    = AmnColors.TextPrimary,
                    focusedBorderColor    = AmnColors.Error,
                    unfocusedBorderColor  = AmnColors.Border,
                    cursorColor           = AmnColors.Error,
                    focusedContainerColor = AmnColors.Surface,
                    unfocusedContainerColor = AmnColors.Surface,
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = { if (input == code) onWipe() },
                enabled = input == code,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AmnColors.Error,
                    disabledContainerColor = AmnColors.SurfaceAlt,
                    disabledContentColor = AmnColors.TextDisabled,
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                elevation = ButtonDefaults.buttonElevation(0.dp),
            ) {
                Icon(Icons.Filled.DeleteForever, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("WIPE ALL DATA", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onBack) {
                Text("Cancel", color = AmnColors.TextSecondary)
            }
        }
    }
}
