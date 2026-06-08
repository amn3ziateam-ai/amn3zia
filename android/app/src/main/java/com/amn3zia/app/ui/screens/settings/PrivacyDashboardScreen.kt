package com.amn3zia.app.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/** Privacy dashboard — every privacy control lives here, per the UX requirement. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyDashboardScreen(onBack: () -> Unit, onOpenPanic: () -> Unit) {
    val viewModel: SettingsViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val activity = context as? android.app.Activity

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Dashboard") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SectionTitle("Ghost Mode")
            SettingSwitch("Ghost Mode (master toggle)", state.ghostModeEnabled, viewModel::toggleGhostMode)
            SettingSwitch("Suppress typing indicator", state.suppressTyping, viewModel::toggleSuppressTyping, enabled = state.ghostModeEnabled)
            SettingSwitch("Control read receipts", state.controlReadReceipts, viewModel::toggleReadReceipts, enabled = state.ghostModeEnabled)
            SettingSwitch("Delayed read marking", state.delayedReadMarking, viewModel::toggleDelayedReadMarking, enabled = state.ghostModeEnabled && state.controlReadReceipts)

            SectionTitle("Anti-Tracking")
            SettingSwitch("Disable link previews", state.disableLinkPreviews, {}, enabled = false)
            SettingSwitch("Block external media auto-load", state.blockExternalMedia, {}, enabled = false)
            HelpText("Anti-tracking is enforced per-account at the protocol layer and always on by default in this build.")

            SectionTitle("Screen Protection")
            SettingSwitch(
                "Block screenshots & blur app switcher",
                state.screenshotsBlocked,
                { activity?.let(viewModel::toggleScreenshotsBlocked) },
            )

            SectionTitle("Encryption")
            HelpText("All local databases and files are encrypted at rest with per-account keys, wrapped by your device's hardware-backed Keystore.")

            SectionTitle("Danger zone")
            Button(
                onClick = onOpenPanic,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE6294B)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Warning, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Panic Button — wipe everything")
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontWeight = FontWeight.Bold, color = Color(0xFF7C5CFF), modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
}

@Composable
private fun HelpText(text: String) {
    Text(text, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onToggle: () -> Unit, enabled: Boolean = true) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = { onToggle() }, enabled = enabled)
    }
}
