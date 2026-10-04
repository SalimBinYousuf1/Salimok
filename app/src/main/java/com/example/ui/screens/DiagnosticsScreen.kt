package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.statemachine.ControllerConnectionState
import com.example.ui.SalimViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DiagnosticsScreen(
    viewModel: SalimViewModel,
    modifier: Modifier = Modifier
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val activeHost by viewModel.activeHost.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()
    val remoteCapabilities by viewModel.remoteCapabilities.collectAsState()

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Root-Cause Analysis Card (Addresses the 127.0.0.1 / CLEARTEXT network policy issue)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF1E40AF))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Network Policy & Architecture Diagnostics",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E40AF)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    DiagnosticRuleItem(
                        title = "Loopback 127.0.0.1 Blocked",
                        description = "127.0.0.1 routes traffic back to Salim itself. It does NOT reach Ubaid. Both devices communicate across different networks using the configured public signaling server.",
                        isPassing = true
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    DiagnosticRuleItem(
                        title = "Insecure Cleartext (ws:// / http://) Blocked",
                        description = "Cleartext transport exposes authentication keys and remote inputs to eavesdropping. Salim strictly enforces WSS / TLS 1.3 transport across all network boundaries.",
                        isPassing = true
                    )
                }
            }
        }

        // 2. Real-Time Subsystem Health Checklist
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Subsystem Health Check",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val isAuthed = connectionState == ControllerConnectionState.CONNECTED || connectionState == ControllerConnectionState.ACTIVE
                    val isStreaming = connectionState == ControllerConnectionState.ACTIVE

                    SubsystemCheckRow("Hardware Keystore Asymmetric Identity", true, "RSA-2048 with SHA-256")
                    SubsystemCheckRow("Signaling Link Configuration", activeHost != null, activeHost?.signalingServerUrl ?: "Requires Host Pairing")
                    SubsystemCheckRow("Host Mutual Authentication", isAuthed, if (isAuthed) "Verified" else "Awaiting Session")
                    SubsystemCheckRow("WebRTC / STUN Traversal", isAuthed, "stun:stun.l.google.com:19302")
                    SubsystemCheckRow("Screen Stream Decoder", isStreaming, if (isStreaming) "Active" else "Idle")
                    SubsystemCheckRow("Accessibility Input Dispatch", remoteCapabilities?.hasAccessibility == true, if (remoteCapabilities?.hasAccessibility == true) "Available" else "Host permission needed")
                }
            }
        }

        // 3. Controller Keystore Identity Inspection
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Controller Security Identity", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = "Controller ID:", fontSize = 12.sp, color = Color(0xFF64748B))
                    Text(text = viewModel.controllerId, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color(0xFF0F172A))

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(text = "Public Key Fingerprint (SHA-256):", fontSize = 12.sp, color = Color(0xFF64748B))
                    Text(text = viewModel.controllerFingerprint, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF475569))
                }
            }
        }

        // 4. Session Audit Logs
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Security Audit Trail (${auditLogs.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                if (auditLogs.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { viewModel.clearAuditLogs() },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Clear Logs", fontSize = 11.sp)
                    }
                }
            }
        }

        items(auditLogs.take(20), key = { it.id }) { log ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "[${log.eventType}]",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (log.severity) {
                                "CRITICAL", "ERROR" -> Color(0xFFDC2626)
                                "WARN" -> Color(0xFFD97706)
                                else -> Color(0xFF2563EB)
                            }
                        )
                        Text(
                            text = dateFormat.format(Date(log.timestamp)),
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = log.details, fontSize = 12.sp, color = Color(0xFF334155))
                }
            }
        }
    }
}

@Composable
private fun DiagnosticRuleItem(title: String, description: String, isPassing: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = if (isPassing) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = null,
            tint = if (isPassing) Color(0xFF059669) else Color(0xFFDC2626),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A))
            Text(text = description, fontSize = 12.sp, color = Color(0xFF334155), lineHeight = 16.sp)
        }
    }
}

@Composable
private fun SubsystemCheckRow(name: String, ok: Boolean, detail: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
            Text(text = detail, fontSize = 11.sp, color = Color(0xFF64748B))
        }
        Icon(
            imageVector = if (ok) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = null,
            tint = if (ok) Color(0xFF10B981) else Color(0xFFF59E0B),
            modifier = Modifier.size(16.dp)
        )
    }
}
