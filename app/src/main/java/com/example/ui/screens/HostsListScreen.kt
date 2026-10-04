package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.TrustedHostEntity
import com.example.statemachine.ControllerConnectionState
import com.example.ui.SalimViewModel
import com.example.ui.components.HostStatusBadge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HostsListScreen(
    viewModel: SalimViewModel,
    onNavigateToViewer: () -> Unit,
    onNavigateToPairing: () -> Unit,
    onNavigateToDetails: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val trustedHosts by viewModel.trustedHosts.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val activeHost by viewModel.activeHost.collectAsState()
    val liveTelemetry by viewModel.liveTelemetry.collectAsState()
    val lastInputFeedback by viewModel.lastInputFeedback.collectAsState()

    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. App Header & Controller Identity
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Salim Controller",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "ID: ${viewModel.controllerId}",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF64748B)
                            )
                        }
                        HostStatusBadge(state = connectionState)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = statusMessage,
                        fontSize = 13.sp,
                        color = Color(0xFF475569)
                    )

                    if (lastInputFeedback != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Last Ack: $lastInputFeedback",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF059669)
                        )
                    }
                }
            }
        }

        // 2. Active Connected Host Banner (if Connected)
        if (activeHost != null && (connectionState == ControllerConnectionState.CONNECTED || connectionState == ControllerConnectionState.ACTIVE)) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFA7F3D0), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = activeHost!!.displayName,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF065F46)
                                )
                            }

                            OutlinedButton(
                                onClick = { viewModel.disconnect() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Disconnect", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Live Host Telemetry Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val battery = liveTelemetry?.batteryPercent ?: activeHost!!.cachedBatteryPercent
                            val isCharging = liveTelemetry?.isCharging ?: activeHost!!.cachedBatteryCharging
                            val net = liveTelemetry?.networkType ?: activeHost!!.cachedNetworkType
                            val model = liveTelemetry?.deviceModel ?: activeHost!!.cachedDeviceModel

                            Text(
                                text = "Battery: ${if (battery >= 0) "$battery% ${if (isCharging) "⚡" else ""}" else "N/A"}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Network: $net",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0F172A)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Large Primary Action: View Screen
                        Button(
                            onClick = onNavigateToViewer,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E40AF)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.ScreenShare, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("View & Control Ubaid Screen", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Quick Navigation Command Bar
                        Text(
                            text = "Quick Navigation Actions",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF065F46)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            QuickCommandButton("Back", Icons.Default.ArrowBack) {
                                viewModel.sendGlobalAction("BACK")
                            }
                            QuickCommandButton("Home", Icons.Default.Home) {
                                viewModel.sendGlobalAction("HOME")
                            }
                            QuickCommandButton("Recents", Icons.Default.ViewCarousel) {
                                viewModel.sendGlobalAction("RECENTS")
                            }
                            QuickCommandButton("Notify", Icons.Default.Notifications) {
                                viewModel.sendGlobalAction("NOTIFICATIONS")
                            }
                            QuickCommandButton("Lock", Icons.Default.Lock) {
                                viewModel.sendGlobalAction("LOCK_SCREEN")
                            }
                        }
                    }
                }
            }
        }

        // 3. Trusted Hosts Section Header & Add Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Trusted Hosts (${trustedHosts.size})",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                Button(
                    onClick = onNavigateToPairing,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E40AF)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pair New Host", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 4. Hosts List
        if (trustedHosts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No Ubaid Hosts Paired Yet",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Pair with your Ubaid phone using the 6-digit code or QR code to begin remote control.",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B),
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onNavigateToPairing,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E40AF))
                        ) {
                            Text("Pair With Ubaid Now")
                        }
                    }
                }
            }
        } else {
            items(trustedHosts, key = { it.hostId }) { host ->
                val isCurrentActive = activeHost?.hostId == host.hostId &&
                        (connectionState == ControllerConnectionState.CONNECTED || connectionState == ControllerConnectionState.ACTIVE)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (isCurrentActive) Color(0xFFBFDBFE) else Color(0xFFE2E8F0),
                            RoundedCornerShape(12.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrentActive) Color(0xFFEFF6FF) else Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = host.displayName,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = host.hostId,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF64748B)
                                )
                            }

                            if (isCurrentActive) {
                                Text(
                                    text = "● LIVE",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF059669)
                                )
                            } else {
                                Text(
                                    text = "Last seen: ${dateFormat.format(Date(host.lastSeen))}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Cached vs Live State Rule Implementation
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val batteryStr = if (isCurrentActive && liveTelemetry != null) {
                                "${liveTelemetry!!.batteryPercent}% ${if (liveTelemetry!!.isCharging) "⚡" else ""}"
                            } else if (host.cachedBatteryPercent >= 0) {
                                "${host.cachedBatteryPercent}% (cached)"
                            } else {
                                "N/A"
                            }

                            val netStr = if (isCurrentActive && liveTelemetry != null) {
                                liveTelemetry!!.networkType
                            } else {
                                host.cachedNetworkType
                            }

                            Text(
                                text = "Battery: $batteryStr",
                                fontSize = 12.sp,
                                color = Color(0xFF475569)
                            )
                            Text(
                                text = "Network: $netStr",
                                fontSize = 12.sp,
                                color = Color(0xFF475569)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { onNavigateToDetails(host.hostId) },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Host Details", fontSize = 12.sp)
                            }

                            Row {
                                if (!isCurrentActive) {
                                    Button(
                                        onClick = { viewModel.connectToHost(host) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E40AF))
                                    ) {
                                        Text("Connect", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                }

                                IconButton(onClick = { viewModel.deleteHost(host.hostId) }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Unpair Host",
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickCommandButton(label: String, icon: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFD1D5DB), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color(0xFF1E40AF), modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E40AF))
        }
    }
}
