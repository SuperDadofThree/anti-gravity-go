package com.antigravity.go.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.antigravity.go.util.AppLogger
import com.antigravity.go.web.WebContainerState

@Composable
fun SettingsDialog(
    currentUrl: String,
    selectedAccount: String?,
    onDismiss: () -> Unit,
    onSaveUrl: (String) -> Unit,
    onClearCache: () -> Unit,
    onPromptAccountPicker: () -> Unit
) {
    var urlInput by remember { mutableStateOf(currentUrl) }
    var showLogViewer by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Antigravity Go Settings",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Target Web Endpoint",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                // Quick presets
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = urlInput == WebContainerState.DEFAULT_URL,
                        onClick = { urlInput = WebContainerState.DEFAULT_URL },
                        label = { Text("Default") }
                    )
                    FilterChip(
                        selected = urlInput.contains("10.0.2.2"),
                        onClick = { urlInput = "http://10.0.2.2:8080" },
                        label = { Text("Emulator") }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Google Account",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                if (!selectedAccount.isNullOrBlank()) {
                    Text(
                        text = "Current: $selectedAccount",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onPromptAccountPicker()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (selectedAccount.isNullOrBlank()) "Sign In with Device Account" else "Switch Device Account")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Storage & Session",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedButton(
                    onClick = {
                        onClearCache()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clear Web Cache & Reload")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Diagnostics & Logs",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedButton(
                    onClick = { showLogViewer = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Collect & View Logs (${AppLogger.getLogCount()} lines)")
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Antigravity Go Beta v0.0.4",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (urlInput.isNotBlank()) {
                        onSaveUrl(urlInput.trim())
                    }
                    onDismiss()
                }
            ) {
                Text("Save & Connect")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showLogViewer) {
        LogViewerDialog(
            onDismiss = { showLogViewer = false }
        )
    }
}
