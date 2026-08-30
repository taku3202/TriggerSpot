package com.signics.triggerspot.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.signics.triggerspot.data.TriggerDevice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: TriggerSpotViewModel,
    onOpenSettings: () -> Unit
) {
    val triggerDevices by viewModel.triggerDevices.collectAsState()
    val isServiceRunning by viewModel.isServiceRunning.collectAsState()
    val hasWritePermission by viewModel.hasWriteSettingsPermission
    val isAccessibilityEnabled by viewModel.isAccessibilityEnabled
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("TriggerSpot") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Device")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Service Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isServiceRunning) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (isServiceRunning) "Monitoring Active" else "Monitoring Stopped",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (isServiceRunning) "Ready to detect connections" else "Service is not running",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = isServiceRunning,
                        onCheckedChange = { viewModel.toggleService(it) }
                    )
                }
            }

            if (!hasWritePermission) {
                PermissionWarningCard(
                    text = "Grant System Settings permission to enable automatic tethering.",
                    onClick = {
                        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                            data = Uri.parse("package:${viewModel.getApplication<android.app.Application>().packageName}")
                        }
                        viewModel.getApplication<android.app.Application>().startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                )
            }

            if (!isAccessibilityEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                PermissionWarningCard(
                    text = "Enable Accessibility Service to automatically toggle the hotspot switch.",
                    onClick = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        viewModel.getApplication<android.app.Application>().startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Trigger Devices", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))

            if (triggerDevices.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No devices registered. Tap + to add.")
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(triggerDevices) { device ->
                        TriggerDeviceItem(
                            device = device,
                            onDelete = { viewModel.removeDevice(device.address) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("Open Hotspot Settings")
            }
        }
    }

    if (showDialog) {
        DeviceSelectionDialog(
            pairedDevices = viewModel.getPairedDevices(),
            onDeviceSelected = {
                viewModel.addDevice(it)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
fun PermissionWarningCard(
    text: String,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "FIX",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun TriggerDeviceItem(
    device: TriggerDevice,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        ListItem(
            headlineContent = { Text(device.name) },
            supportingContent = { Text(device.address) },
            trailingContent = {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        )
    }
}
