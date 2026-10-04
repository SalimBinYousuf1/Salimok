package com.example.ui.screens

import android.net.Uri
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.SalimViewModel

@Composable
fun PairingWizardScreen(
    viewModel: SalimViewModel,
    onPairingSuccess: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var hostIdInput by remember { mutableStateOf("") }
    var pairingCodeInput by remember { mutableStateOf("") }
    var signalingUrlInput by remember { mutableStateOf("wss://signaling.ubaid-host.net/v1") }
    var qrPasteInput by remember { mutableStateOf("") }

    var isPairingInProgress by remember { mutableStateOf(false) }
    var pairingFeedback by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    fun parseQrString(qrString: String) {
        try {
            val uri = Uri.parse(qrString.trim())
            val host = uri.getQueryParameter("hostId")
            val code = uri.getQueryParameter("code")
            val sigUrl = uri.getQueryParameter("url")

            if (!host.isNullOrEmpty()) hostIdInput = host
            if (!code.isNullOrEmpty()) pairingCodeInput = code
            if (!sigUrl.isNullOrEmpty()) signalingUrlInput = sigUrl
            pairingFeedback = "Parsed QR payload successfully"
            isError = false
        } catch (e: Exception) {
            pairingFeedback = "Invalid QR string format"
            isError = true
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF0F172A))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Pair New Ubaid Host",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            }
        }

        // Instructions Card
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
                        Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF1E40AF))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Mutual Cryptographic Pairing",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E40AF)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "1. Open Ubaid on Phone A.\n2. Tap 'Pairing' and generate a 6-digit code or QR code.\n3. Enter the Host ID and code below or paste the pairing URI to complete the digital signature handshake.",
                        fontSize = 13.sp,
                        color = Color(0xFF1E3A8A),
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Quick QR Paste / Scanner input
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
                        text = "Quick Paste Ubaid QR Payload",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = qrPasteInput,
                        onValueChange = {
                            qrPasteInput = it
                            if (it.startsWith("ubaid://")) {
                                parseQrString(it)
                            }
                        },
                        label = { Text("Paste ubaid:// URI or QR content") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        }

        // Manual Entry Form
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Host Details & Pairing Code",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = hostIdInput,
                        onValueChange = { hostIdInput = it.trim() },
                        label = { Text("Ubaid Host ID (e.g. UBAID-A1B2-C3D4)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = pairingCodeInput,
                        onValueChange = { pairingCodeInput = it.filter { char -> char.isDigit() }.take(6) },
                        label = { Text("6-Digit Pairing Code") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = signalingUrlInput,
                        onValueChange = { signalingUrlInput = it.trim() },
                        label = { Text("Secure Signaling Server URL (WSS)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Error warning on 127.0.0.1 or cleartext
                    if (signalingUrlInput.contains("127.0.0.1") || signalingUrlInput.contains("localhost")) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Loopback 127.0.0.1 rejected. Salim and Ubaid are separate devices on distinct networks. Use a routable public domain.",
                            color = Color(0xFFDC2626),
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (pairingFeedback != null) {
                        Text(
                            text = pairingFeedback!!,
                            color = if (isError) Color(0xFFDC2626) else Color(0xFF059669),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Button(
                        onClick = {
                            val hostId = hostIdInput.trim()
                            val code = pairingCodeInput.trim()
                            val url = signalingUrlInput.trim()

                            if (hostId.isEmpty() || code.length < 6) {
                                pairingFeedback = "Enter a valid Host ID and 6-digit code"
                                isError = true
                                return@Button
                            }

                            if (url.startsWith("http://") || url.startsWith("ws://") || url.contains("127.0.0.1")) {
                                pairingFeedback = "Insecure loopback/cleartext rejected. Must use wss://"
                                isError = true
                                return@Button
                            }

                            isPairingInProgress = true
                            pairingFeedback = "Contacting Ubaid Host via secure signaling..."
                            isError = false

                            viewModel.pairWithHost(hostId, code, url) { success, msg ->
                                isPairingInProgress = false
                                if (success) {
                                    pairingFeedback = "Successfully paired with Ubaid!"
                                    isError = false
                                    onPairingSuccess()
                                } else {
                                    pairingFeedback = "Pairing failed: $msg"
                                    isError = true
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E40AF)),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isPairingInProgress
                    ) {
                        if (isPairingInProgress) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pairing...")
                        } else {
                            Icon(Icons.Default.Key, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Authenticate & Pair With Host", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
