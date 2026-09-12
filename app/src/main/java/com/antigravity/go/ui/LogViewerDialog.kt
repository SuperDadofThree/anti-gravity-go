package com.antigravity.go.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.go.util.AppLogger

@Composable
fun LogViewerDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var logContent by remember { mutableStateOf(AppLogger.getLogs()) }
    var logCount by remember { mutableStateOf(AppLogger.getLogCount()) }
    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Application Logs",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "$logCount lines",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Monospace Log Console Area
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0D1117)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp, max = 360.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .verticalScroll(verticalScroll)
                            .horizontalScroll(horizontalScroll)
                    ) {
                        Text(
                            text = if (logContent.isBlank()) "No logs captured yet." else logContent,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = Color(0xFF7EE787)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Row 1: Copy and Share
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            AppLogger.copyLogsToClipboard(context)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("📋 Copy")
                    }
                    Button(
                        onClick = {
                            AppLogger.shareLogs(context)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("↗ Share")
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Action Row 2: System Logcat Dump & Clear
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val logcat = AppLogger.collectSystemLogcat(300)
                            AppLogger.shareLogs(context, extraHeader = logcat)
                        },
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Text("Dump Logcat")
                    }
                    OutlinedButton(
                        onClick = {
                            AppLogger.clearLogs(context)
                            logContent = AppLogger.getLogs()
                            logCount = AppLogger.getLogCount()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.weight(0.9f)
                    ) {
                        Text("Clear")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
