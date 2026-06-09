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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amn3zia.app.ui.theme.TgColors

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
                title = { Text("Privacy & Security", fontWeight = FontWeight.Medium, fontSize = 18.sp) },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {

            // ── 1. Ghost Mode ───────────────────────────────────────────────
            Section("Ghost Mode", Icons.Filled.VisibilityOff) {
                TgToggle("Ghost Mode (master)", state.ghostModeEnabled, viewModel::toggleGhostMode)
                TgToggle("Hide typing status", state.suppressTyping, viewModel::toggleSuppressTyping, state.ghostModeEnabled)
                TgToggle("Control online status", state.controlReadReceipts, viewModel::toggleReadReceipts, state.ghostModeEnabled)
                TgToggle("Delayed read marking", state.delayedReadMarking, viewModel::toggleDelayedReadMarking, state.ghostModeEnabled && state.controlReadReceipts)
                TgItem("Manual 'mark as read'", Icons.Filled.MarkChatRead, onClick = {})
                TgToggle("Anti-View (read without receipt)", state.antiViewEnabled, viewModel::toggleAntiView)
            }

            // ── 2. Anti-Tracking ────────────────────────────────────────────
            Section("Anti-Tracking", Icons.Filled.TrackChanges) {
                TgToggle("Disable link previews", state.disableLinkPreviews, enabled = false)
                TgToggle("Block external media auto-load", state.blockExternalMedia, enabled = false)
                TgInfo("Link previews and external media are always blocked — enforced at the protocol layer.")
                TgToggle("Silent Mode (no notifications sent)", state.silentModeEnabled, viewModel::toggleSilentMode)
            }

            // ── 3. Proxy Control ────────────────────────────────────────────
            Section("Proxy Control", Icons.Filled.VpnKey) {
                TgNavigate("Manage Proxies (SOCKS5 / MTProto)", Icons.Filled.VpnKey, onOpenProxy)
                TgToggle("Kill-switch (cut traffic if proxy down)", state.killSwitchEnabled, viewModel::toggleKillSwitch)
                TgInfo("Each account has its own isolated proxy configuration.")
            }

            // ── 4. Screen Protection ────────────────────────────────────────
            Section("Content Protection", Icons.Filled.ScreenLockLandscape) {
                TgToggle(
                    "Block screenshots & blur in recents",
                    state.screenshotsBlocked,
                    { activity?.let(viewModel::toggleScreenshotsBlocked) },
                )
                TgToggle("Blur recents switcher (independent)", state.blurInRecents, viewModel::toggleBlurInRecents)
                TgToggle("Blur media until tapped", state.mediaVisibilityBlur, viewModel::toggleMediaBlur)
            }

            // ── 5. Local Encryption ─────────────────────────────────────────
            Section("Local Encryption", Icons.Filled.Lock) {
                TgInfo("All databases, cache, and files are encrypted with per-account AES-256 keys stored in the Android Keystore hardware-backed enclave. No cloud sync of keys.")
            }

            // ── 6. App Lock ─────────────────────────────────────────────────
            Section("App Lock", Icons.Filled.PhonelinkLock) {
                TgNavigate(
                    if (state.appLockEnabled) "App Lock  ·  ${state.appLockType.uppercase()}" else "App Lock  ·  OFF",
                    Icons.Filled.PhonelinkLock,
                    onOpenAppLock,
                )
                if (state.appLockEnabled) {
                    TgInfo("Auto-lock timeout: ${state.autoLockTimeoutSec}s  ·  Type: ${state.appLockType}")
                }
            }

            // ── 7. Self-Destruct ────────────────────────────────────────────
            Section("Self-Destruct", Icons.Filled.DeleteForever) {
                TgInfo("Wipe all local data after ${state.selfDestructAttempts} failed PIN attempts.")
                TgSlider(
                    label = "Failed attempts before wipe: ${state.selfDestructAttempts}",
                    value = state.selfDestructAttempts.toFloat(),
                    range = 3f..20f,
                    onValueChange = { viewModel.setSelfDestructAttempts(it.toInt()) },
                )
            }

            // ── 8. Auto-Clean ───────────────────────────────────────────────
            Section("Auto-Clean", Icons.Filled.CleaningServices) {
                TgNavigate("Configure Auto-Clean Rules", Icons.Filled.CleaningServices, onOpenAutoClean)
                TgInfo("Auto-delete chats, messages, media, and cache by timer, on close, or on inactivity.")
            }

            // ── 9–11. Panic System ──────────────────────────────────────────
            Section("Panic System", Icons.Filled.Warning, accentColor = TgColors.Red) {
                TgInfo("Panic button instantly wipes all data: chats, messages, media, cache, database, encryption keys, and logs you out (without deleting the Telegram account). A confirmation code is required.")
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onOpenPanic,
                    colors = ButtonDefaults.buttonColors(containerColor = TgColors.PanicRed),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Icon(Icons.Filled.Warning, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("PANIC — Wipe Everything", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
            }

            // ── 12–13. Multi-Account ────────────────────────────────────────
            Section("Multi-Account", Icons.Filled.People) {
                TgInfo("Unlimited accounts with full isolation: separate databases, separate encryption keys, separate proxies.")
            }

            // ── 14–16. Stealth ──────────────────────────────────────────────
            Section("Stealth", Icons.Filled.Visibility) {
                TgNavigate("Hidden Chats (extra PIN protection)", Icons.Filled.Lock, onOpenHidden)
                TgNavigate("Fake UI (calculator disguise)", Icons.Filled.Calculate, onOpenFakeUi)
                TgInfo("Hidden chats don't appear in the main list. Fake UI shows a calculator on launch — enter the secret code to reveal AMN3ZIA.")
            }

            // ── 17. Privacy Dashboard ───────────────────────────────────────
            // (you're already on it)

            // ── 19. Send Delay ──────────────────────────────────────────────
            Section("Message Controls", Icons.Filled.Schedule) {
                TgSlider(
                    label = "Send delay: ${if (state.sendDelaySec == 0) "off" else "${state.sendDelaySec}s"}",
                    value = state.sendDelaySec.toFloat(),
                    range = 0f..30f,
                    onValueChange = { viewModel.setSendDelay(it.toInt()) },
                )
                TgInfo("Messages are queued and can be cancelled before they are sent.")
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Reusable components ───────────────────────────────────────────────────────

@Composable
private fun Section(
    title: String,
    icon: ImageVector,
    accentColor: Color = TgColors.Blue,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        // Section header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
            Text(title.uppercase(), color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
        }

        // Items card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 0.dp)
                .background(TgColors.BgSecondary),
            content = content,
        )

        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun TgToggle(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit = {},
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            color = if (enabled) TgColors.TextPrimary else TgColors.TextHint,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f).padding(end = 12.dp),
        )
        Switch(
            checked = checked,
            onCheckedChange = { onToggle() },
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedTrackColor = TgColors.Blue,
                checkedThumbColor = Color.White,
                uncheckedTrackColor = TgColors.BgTertiary,
            ),
        )
    }
    TgDivider()
}

@Composable
private fun TgNavigate(label: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, contentDescription = null, tint = TgColors.Blue, modifier = Modifier.size(20.dp))
            Text(label, color = TgColors.TextPrimary, fontSize = 15.sp)
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TgColors.TextHint, modifier = Modifier.size(18.dp))
    }
    TgDivider()
}

@Composable
private fun TgItem(label: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = TgColors.Blue, modifier = Modifier.size(20.dp))
        Text(label, color = TgColors.TextPrimary, fontSize = 15.sp)
    }
    TgDivider()
}

@Composable
private fun TgInfo(text: String) {
    Text(
        text,
        color = TgColors.TextHint,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun TgSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(label, color = TgColors.TextPrimary, fontSize = 14.sp)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = (range.endInclusive - range.start).toInt() - 1,
            colors = SliderDefaults.colors(activeTrackColor = TgColors.Blue, thumbColor = TgColors.Blue),
        )
    }
    TgDivider()
}

@Composable
private fun TgDivider() {
    HorizontalDivider(
        color = TgColors.Divider,
        thickness = 0.5.dp,
        modifier = Modifier.padding(start = 16.dp),
    )
}
