package com.example.data.repository

import android.content.Context
import com.example.data.db.ControllerAuditLogEntity
import com.example.data.db.SalimDatabase
import com.example.data.db.TrustedHostEntity
import com.example.network.HostConnectionManager
import com.example.network.HostLiveTelemetry
import com.example.network.HostRemoteCapabilities
import com.example.security.ControllerSecurityManager
import com.example.statemachine.ControllerConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

class ControllerRepository(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val database = SalimDatabase.getDatabase(context)

    val securityManager = ControllerSecurityManager.getInstance()
    val connectionManager = HostConnectionManager(context, scope, securityManager, database)

    val trustedHosts: Flow<List<TrustedHostEntity>> = database.trustedHostDao().getAllHostsFlow()
    val primaryHost: Flow<TrustedHostEntity?> = database.trustedHostDao().getPrimaryHostFlow()
    val auditLogs: Flow<List<ControllerAuditLogEntity>> = database.auditLogDao().getRecentLogs()

    val connectionState: StateFlow<ControllerConnectionState> = connectionManager.connectionState
    val statusMessage: StateFlow<String> = connectionManager.statusMessage
    val activeHost: StateFlow<TrustedHostEntity?> = connectionManager.activeHost
    val activeSessionId: StateFlow<String?> = connectionManager.activeSessionId
    val liveTelemetry: StateFlow<HostLiveTelemetry?> = connectionManager.liveTelemetry
    val remoteCapabilities: StateFlow<HostRemoteCapabilities?> = connectionManager.remoteCapabilities
    val latestFrameBytes: StateFlow<ByteArray?> = connectionManager.latestFrameBytes
    val isScreenStreaming: StateFlow<Boolean> = connectionManager.isScreenStreaming
    val lastInputFeedback: StateFlow<String?> = connectionManager.lastInputFeedback

    fun connectToHost(host: TrustedHostEntity) {
        connectionManager.connectToHost(host)
    }

    fun disconnect() {
        connectionManager.disconnect()
    }

    fun startScreenStreaming() {
        connectionManager.startScreenStreaming()
    }

    fun stopScreenStreaming() {
        connectionManager.stopScreenStreaming()
    }

    fun sendTap(normX: Float, normY: Float) {
        connectionManager.sendTap(normX, normY)
    }

    fun sendSwipe(normX1: Float, normY1: Float, normX2: Float, normY2: Float, durationMs: Long = 300L) {
        connectionManager.sendSwipe(normX1, normY1, normX2, normY2, durationMs)
    }

    fun sendGlobalAction(action: String) {
        connectionManager.sendGlobalAction(action)
    }

    fun sendTextInput(text: String) {
        connectionManager.sendTextInput(text)
    }

    fun pairWithHost(hostId: String, code: String, signalingUrl: String, onComplete: (Boolean, String) -> Unit) {
        connectionManager.sendPairingRequest(hostId, code, signalingUrl, onComplete)
    }

    suspend fun deleteHost(hostId: String) = withContext(Dispatchers.IO) {
        if (connectionManager.activeHost.value?.hostId == hostId) {
            connectionManager.disconnect()
        }
        database.trustedHostDao().deleteHost(hostId)
    }

    suspend fun clearAuditLogs() = withContext(Dispatchers.IO) {
        database.auditLogDao().clearAll()
    }

    companion object {
        @Volatile
        private var INSTANCE: ControllerRepository? = null

        fun getInstance(context: Context): ControllerRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ControllerRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
