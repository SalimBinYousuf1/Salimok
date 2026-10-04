package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.statemachine.ControllerConnectionState

@Composable
fun HostStatusBadge(
    state: ControllerConnectionState,
    modifier: Modifier = Modifier
) {
    val (dotColor, bgColor, borderColor) = when (state) {
        ControllerConnectionState.CONNECTED, ControllerConnectionState.ACTIVE -> Triple(
            Color(0xFF10B981), // Emerald
            Color(0xFFECFDF5),
            Color(0xFFA7F3D0)
        )
        ControllerConnectionState.CONNECTING, ControllerConnectionState.AUTHENTICATING, ControllerConnectionState.SESSION_STARTING -> Triple(
            Color(0xFF2563EB), // Blue
            Color(0xFFEFF6FF),
            Color(0xFFBFDBFE)
        )
        ControllerConnectionState.RECONNECTING, ControllerConnectionState.CAPABILITY_LIMITED -> Triple(
            Color(0xFFF59E0B), // Amber
            Color(0xFFFFFBEB),
            Color(0xFFFDE68A)
        )
        ControllerConnectionState.ERROR, ControllerConnectionState.REQUIRES_PAIRING -> Triple(
            Color(0xFFEF4444), // Red
            Color(0xFFFEF2F2),
            Color(0xFFFECACA)
        )
        ControllerConnectionState.DISCONNECTED, ControllerConnectionState.OFFLINE -> Triple(
            Color(0xFF64748B), // Slate
            Color(0xFFF8FAFC),
            Color(0xFFE2E8F0)
        )
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = state.displayName,
            color = Color(0xFF0F172A),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
