package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.SalimViewModel
import com.example.ui.components.RemoteTouchSurface

@Composable
fun RemoteScreenViewerScreen(
    viewModel: SalimViewModel,
    onCloseViewer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeHost by viewModel.activeHost.collectAsState()
    val isStreaming by viewModel.isScreenStreaming.collectAsState()
    val latestFrameBytes by viewModel.latestFrameBytes.collectAsState()
    val lastInputFeedback by viewModel.lastInputFeedback.collectAsState()
    val liveTelemetry by viewModel.liveTelemetry.collectAsState()

    var showTextInputDialog by remember { mutableStateOf(false) }
    var textInputContent by remember { mutableStateOf("") }

    // Start streaming when entering, stop when leaving
    DisposableEffect(Unit) {
        viewModel.startScreenStreaming()
        onDispose {
            viewModel.stopScreenStreaming()
        }
    }

    val frameBitmap = remember(latestFrameBytes) {
        latestFrameBytes?.let { bytes ->
            try {
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. Live Remote Screen Canvas with Touch Event Surface
        RemoteTouchSurface(
            onTap = { normX, normY ->
                viewModel.sendTap(normX, normY)
            },
            onLongPress = { normX, normY ->
                viewModel.sendLongPress(normX, normY)
            },
            onSwipe = { normX1, normY1, normX2, normY2, durationMs ->
                viewModel.sendSwipe(normX1, normY1, normX2, normY2, durationMs)
            },
            remoteResolutionWidth = 1080f,
            remoteResolutionHeight = 2400f,
            modifier = Modifier.fillMaxSize()
        ) {
            if (frameBitmap != null) {
                Image(
                    bitmap = frameBitmap,
                    contentDescription = "Live Ubaid Host Screen",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                // Connecting / Waiting state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (isStreaming) {
                            CircularProgressIndicator(color = Color(0xFF60A5FA), modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Awaiting live video frames from Ubaid...",
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        } else {
                            Icon(
                                Icons.Default.VideocamOff,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Screen streaming is paused by Host",
                                color = Color(0xFF94A3B8),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // 2. Unobtrusive Top Status Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x99000000))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (frameBitmap != null) Color(0xFF10B981) else Color(0xFFF59E0B))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${activeHost?.displayName ?: "Ubaid"} (${liveTelemetry?.batteryPercent ?: "?"}%)",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            IconButton(
                onClick = onCloseViewer,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0x99000000))
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close Viewer", tint = Color.White)
            }
        }

        // Feedback toast near top
        if (lastInputFeedback != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xCC000000))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = lastInputFeedback!!,
                    color = Color(0xFFA7F3D0),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // 3. Unobtrusive Floating Bottom Navigation Bar (Minimalist Apple-inspired Pill)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xCC1E293B))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(28.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ViewerActionButton(label = "Back", icon = Icons.Default.ArrowBack) {
                    viewModel.sendGlobalAction("BACK")
                }
                ViewerActionButton(label = "Home", icon = Icons.Default.Home) {
                    viewModel.sendGlobalAction("HOME")
                }
                ViewerActionButton(label = "Recents", icon = Icons.Default.ViewCarousel) {
                    viewModel.sendGlobalAction("RECENTS")
                }
                ViewerActionButton(label = "Type", icon = Icons.Default.Keyboard) {
                    showTextInputDialog = true
                }
                ViewerActionButton(label = "Lock", icon = Icons.Default.Lock) {
                    viewModel.sendGlobalAction("LOCK_SCREEN")
                }
            }
        }

        // 4. Remote Text Input Dialog
        if (showTextInputDialog) {
            AlertDialog(
                onDismissRequest = { showTextInputDialog = false },
                title = { Text("Send Text to Ubaid") },
                text = {
                    Column {
                        Text(
                            text = "Inject text directly into the active focused input field on Ubaid.",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = textInputContent,
                            onValueChange = { textInputContent = it },
                            label = { Text("Input text") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (textInputContent.isNotEmpty()) {
                                viewModel.sendTextInput(textInputContent)
                                textInputContent = ""
                            }
                            showTextInputDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E40AF))
                    ) {
                        Text("Send Text")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTextInputDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun ViewerActionButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = label, color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}
