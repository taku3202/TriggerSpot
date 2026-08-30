package com.signics.triggerspot.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.signics.triggerspot.data.TriggerDevice

@Composable
fun DeviceSelectionDialog(
    pairedDevices: List<TriggerDevice>,
    onDeviceSelected: (TriggerDevice) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Bluetooth Device") },
        text = {
            if (pairedDevices.isEmpty()) {
                Text("No paired devices found.")
            } else {
                LazyColumn {
                    items(pairedDevices) { device ->
                        ListItem(
                            headlineContent = { Text(device.name) },
                            supportingContent = { Text(device.address) },
                            modifier = Modifier.clickable { onDeviceSelected(device) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
