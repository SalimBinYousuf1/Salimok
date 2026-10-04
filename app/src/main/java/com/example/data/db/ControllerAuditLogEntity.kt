package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "controller_audit_logs")
data class ControllerAuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String,
    val hostId: String = "",
    val details: String,
    val severity: String = "INFO" // INFO, WARN, ERROR, CRITICAL
)
