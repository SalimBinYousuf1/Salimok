package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trusted_hosts")
data class TrustedHostEntity(
    @PrimaryKey val hostId: String,
    val displayName: String,
    val publicKey: String,
    val signalingServerUrl: String = "wss://signaling.ubaid-host.net/v1",
    val pairedAt: Long = System.currentTimeMillis(),
    val lastSeen: Long = System.currentTimeMillis(),
    val isPrimary: Boolean = true,
    val cachedBatteryPercent: Int = -1,
    val cachedBatteryCharging: Boolean = false,
    val cachedNetworkType: String = "Unknown",
    val cachedDeviceModel: String = "Unknown Android Host",
    val cachedAndroidVersion: String = "",
    val cachedResolution: String = "1080x2400",
    val hasScreenCapability: Boolean = true,
    val hasInputCapability: Boolean = true
)
