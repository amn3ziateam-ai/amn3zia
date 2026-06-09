package com.amn3zia.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amn3zia.app.ui.theme.AmnColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyDashboardScreen(
    onBack: () -> Unit,
    onOpenPanic: () -> Unit,
    onOpenAppLock: () -> Unit = {},
    onOpenProxy: () -> Unit = {},
    onOpenAutoClean: () -> Unit = {},
    onOpenHidden: () -> Unit = {},
    onOpenFakeUi: () -> Unit = {},
) {
    val viewModel: SettingsViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val activity = context as? android.app.Activity

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
                title = { Text("Privacy & Security", fontWeight = FontWeight.SemiBold, fontSize = 17.sp) },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(8.dp))

            // 1. Ghost Mode
            Section("Ghost Mode") {
                PToggle("Ghost Mode (global)", state.ghostModeEnabled, viewModel::toggleGhostMode)
                PToggle("Hide typing indicator", state.suppressTyping, viewModel::toggleSuppressTyping, state.ghostModeEnabled)
                PToggle("Hide online status", state.controlReadReceipts, viewModel::toggleReadReceipts, state.ghostModeEnabled)
                PToggle("Delayed read marking", state.delayedReadMarking, viewModel::toggleDelayedReadMarking, state.ghostModeEnabled)
                PToggle("Anti-View (read without receipt)", state.antiViewEnabled, viewModel::toggleAntiView)
                PInfo("When Ghost Mode is on, no read receipts or typing indicators are sent to anyone.")
            }

            // 2. Anti-Tracking
            Section("Anti-Tracking") {
                PToggle("Disable link previews", state.disableLinkPreviews, enabled = false)
                PToggle("Block external media auto-load", state.blockExternalMedia, enabled = false)
                PToggle("Silent Mode (suppress notifications)", state.silentModeEnabled, viewModel::toggleSilentMode)
                PInfo("Link previews and external media loading are permanently disabled.")
            }

            // 3. Proxy
            Section("Proxy") {
                PNavigate("Manage Proxies (SOCKS5 / MTProto)", Icons.Filled.VpnKey, onOpenProxy)
                PToggle("Kill-switch (drop traffic if proxy disconnects)", state.killSwitchEnabled, viewModel::toggleKillSwitch)
            }

            // 4. Screen Protection
            Section("Screen Protection") {
                PToggle(
                    label = "Block screenshots & screen recording",
                    checked = state.screenshotsBlocked,
                    onToggle = { activity?.let(viewModel::toggleScreenshotsBlocked) },
                )
                PToggle("Blur app in recent apps switcher", state.blurInRecents, viewModel::toggleBlurInRecents)
                PToggle("Blur media until tapped", state.mediaVisibilityBlur, viewModel::toggleMediaBlur)
            }

            // 5. Encryption
            Section("Local Encryption") {
                PInfo("All chats, cache, and files are encrypted with per-account AES-256 keys backed by Android Keystore hardware. Keys never leave the device.")
            }

            // 6. App Lock
            Section("App Lock") {
                PNavigate(
                    label = if (state.appLockEnabled) "App Lock  ·  ${state.appLockType.uppercase()} enabled" else "App Lock  ·  Disabled",
                    icon  = Icons.Filled.PhonelinkLock,
                    onClick = onOpenAppLock,
                )
            }

            // 7. Self-Destruct
            Section("Self-Destruct") {
                PSlider(
                    label = "Wipe after ${state.selfDestructAttempts} failed PIN attempts",
                    value = state.selfDestructAttempts.toFloat(),
                    range = 3f..20f,
                    onValueChange = { viewModel.setSelfDestructAttempts(it.toInt()) },
                )
                PInfo("After the limit is reached, all local data is permanently deleted.")
            }

            // 8. Auto-Clean
            Section("Auto-Clean") {
                PNavigate("Configure Auto-Clean Rules", Icons.Filled.CleaningServices, onOpenAutoClean)
                PInfo("Automatically delete messages, media, and cache on schedule or on app close.")
            }

            // 9–11. Panic System
            Section("Panic System", accentColor = AmnColors.Error) {
                PInfo("Instant one-tap wipe: deletes all local data and logs out without removing the Telegram account. Requires confirmation code.")
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onOpenPanic,
                    colors = ButtonDefaults.buttonColors(containerColor = AmnColors.Error),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    elevation = ButtonDefaults.buttonElevation(0.dp),
                ) {
                    Icon(Icons.Filled.Warning, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("PANIC — Wipe Everything", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
            }

            // 12–13. Multi-Account
            Section("Multi-Account") {
                PInfo("Unlimited accounts. Each account has fully isolated data, encryption keys, and proxy settings.")
            }

            // 14–16. Stealth
            Section("Stealth") {
                PNavigate("Hidden Chats  (extra PIN protection)", Icons.Filled.Lock, onOpenHidden)
                PNavigate("Fake UI  (calculator disguise)", Icons.Filled.Calculate, onOpenFakeUi)
                PInfo("Hidden chats are invisible in the main list. Fake UI disguises the app as a calculator — enter the secret code to unlock.")
            }

            // 19. Message Controls
            Section("Message Controls") {
                PSlider(
                    label = "Send delay: ${if (state.sendDelaySec == 0) "off" else "${state.sendDelaySec}s"}",
                    value = state.sendDelaySec.toFloat(),
                    range = 0f..30f,
                    onValueChange = { viewModel.setSendDelay(it.toInt()) },
                )
                PInfo("Messages are held for the delay period and can be cancelled before sending.")
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Section ───────────────────────────────────────────────────────────────────

@Composable
private fun Section(
    title: String,
    accentColor: Color = AmnColors.Primary,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text  = title.uppercase(),
            color = accentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AmnColors.Surface),
            content = content,
        )
        HorizontalDivider(color = AmnColors.Border, thickness = 0.5.dp)
        Spacer(Modifier.height(8.dp))
    }
}

// ── Row types ─────────────────────────────────────────────────────────────────

@Composable
private fun PToggle(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit = {},
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            color = if (enabled) AmnColors.TextPrimary else AmnColors.TextDisabled,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f).padding(end = 12.dp),
        )
        Switch(
            checked = checked,
            onCheckedChange = { onToggle() },
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedTrackColor = AmnColors.Primary,
                checkedThumbColor = Color.White,
                uncheckedTrackColor = AmnColors.Border,
                uncheckedThumbColor = Color.White,
            ),
        )
    }
    HorizontalDivider(color = AmnColors.BorderLight, thickness = 0.5.dp, modifier = Modifier.padding(start = 16.dp))
}

@Composable
private fun PNavigate(label: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, tint = AmnColors.Primary, modifier = Modifier.size(20.dp))
            Text(label, color = AmnColors.TextPrimary, fontSize = 15.sp)
        }
        Icon(Icons.Filled.ChevronRight, null, tint = AmnColors.TextTertiary, modifier = Modifier.size(18.dp))
    }
    HorizontalDivider(color = AmnColors.BorderLight, thickness = 0.5.dp, modifier = Modifier.padding(start = 16.dp))
}

@Composable
private fun PInfo(text: String) {
    Text(
        text,
        color = AmnColors.TextTertiary,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun PSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(label, color = AmnColors.TextPrimary, fontSize = 14.sp)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = (range.endInclusive - range.start).toInt() - 1,
            colors = SliderDefaults.colors(activeTrackColor = AmnColors.Primary, thumbColor = AmnColors.Primary),
        )
    }
    HorizontalDivider(color = AmnColors.BorderLight, thickness = 0.5.dp, modifier = Modifier.padding(start = 16.dp))
}
