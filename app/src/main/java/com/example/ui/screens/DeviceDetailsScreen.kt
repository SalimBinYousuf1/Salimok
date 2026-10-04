package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.SalimViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DeviceDetailsScreen(
    hostId: String,
    viewModel: SalimViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val trustedHosts by viewModel.trustedHosts.collectAsState()
    val host = trustedHosts.find { it.hostId == hostId }

    val liveTelemetry by viewModel.liveTelemetry.collectAsState()
    val remoteCapabilities by viewModel.remoteCapabilities.collectAsState()
    val isConnected = viewModel.activeHost.collectAsState().value?.hostId == hostId

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF0F172A))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = host?.displayName ?: "Host Details",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = hostId,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }

        // Live vs Cached status badge
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (isConnected) Color(0xFFA7F3D0) else Color(0xFFE2E8F0),
                        RoundedCornerShape(12.dp)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isConnected) Color(0xFFF0FDF4) else Color.White
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isConnected) "Session Active (Live Real-Time Data)" else "Host Idle / Offline (Displaying Last Known Cached State)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isConnected) Color(0xFF065F46) else Color(0xFF64748B)
                    )
                    if (host != null) {
                        Text(
                            text = dateFormat.format(Date(host.lastSeen)),
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }

        // Power & Battery Telemetry
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Power & Battery Subsystem", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    Spacer(modifier = Modifier.height(10.dp))

                    val batteryPercent = if (isConnected && liveTelemetry != null) liveTelemetry!!.batteryPercent else host?.cachedBatteryPercent ?: -1
                    val isCharging = if (isConnected && liveTelemetry != null) liveTelemetry!!.isCharging else host?.cachedBatteryCharging ?: false
                    val temp = if (isConnected && liveTelemetry != null) "${liveTelemetry!!.batteryTempCelsius}°C" else "N/A"
                    val health = if (isConnected && liveTelemetry != null) liveTelemetry!!.batteryHealth else "Normal"

                    TelemetryRow("Charge Level", if (batteryPercent >= 0) "$batteryPercent%" else "Unknown")
                    TelemetryRow("Charging Status", if (isCharging) "Charging (AC / USB) ⚡" else "Discharging")
                    TelemetryRow("Battery Temperature", temp)
                    TelemetryRow("Battery Health", health)
                }
            }
        }

        // Network & Radio Subsystem
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Network & Transport Interface", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    Spacer(modifier = Modifier.height(10.dp))

                    val netType = if (isConnected && liveTelemetry != null) liveTelemetry!!.networkType else host?.cachedNetworkType ?: "Unknown"
                    val localIp = if (isConnected && liveTelemetry != null) liveTelemetry!!.localIpAddress else "N/A"

                    TelemetryRow("Bearer Type", netType)
                    TelemetryRow("Local Host IP", localIp)
                    TelemetryRow("Signaling URL", host?.signalingServerUrl ?: "wss://signaling.ubaid-host.net/v1")
                }
            }
        }

        // Display & OS
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Display & Hardware", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    Spacer(modifier = Modifier.height(10.dp))

                    val model = if (isConnected && liveTelemetry != null) liveTelemetry!!.deviceModel else host?.cachedDeviceModel ?: "Unknown"
                    val androidVer = if (isConnected && liveTelemetry != null) liveTelemetry!!.androidVersion else host?.cachedAndroidVersion ?: "Android 14"
                    val res = if (isConnected && liveTelemetry != null) liveTelemetry!!.displayResolution else host?.cachedResolution ?: "1080x2400"

                    TelemetryRow("Device Model", model)
                    TelemetryRow("Android Version", androidVer)
                    TelemetryRow("Display Resolution", res)
                }
            }
        }

        // Remote Capabilities Verification
        if (isConnected && remoteCapabilities != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Verified Ubaid Capabilities", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        Spacer(modifier = Modifier.height(10.dp))

                        val caps = remoteCapabilities!!
                        CapabilityStatusRow("Accessibility Remote Input", caps.hasAccessibility)
                        CapabilityStatusRow("MediaProjection Screen Capture", caps.hasMediaProjection)
                        CapabilityStatusRow("Background Battery Exemption", caps.isBatteryOptimizationsIgnored)
                        CapabilityStatusRow("Foreground Service Running", caps.isForegroundServiceRunning)
                        CapabilityStatusRow("Device Owner Provisioned", caps.isDeviceOwner)
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = Color(0xFF64748B))
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
    }
}

@Composable
private fun CapabilityStatusRow(name: String, enabled: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = name, fontSize = 13.sp, color = Color(0xFF334155))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (enabled) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (enabled) Color(0xFF10B981) else Color(0xFFF59E0B),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (enabled) "Available" else "Disabled on Host",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) Color(0xFF10B981) else Color(0xFFF59E0B)
            )
        }
    }
}
