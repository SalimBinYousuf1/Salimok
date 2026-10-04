package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.ControllerAuditLogEntity
import com.example.data.db.TrustedHostEntity
import com.example.data.repository.ControllerRepository
import com.example.network.HostLiveTelemetry
import com.example.network.HostRemoteCapabilities
import com.example.statemachine.ControllerConnectionState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SalimViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ControllerRepository.getInstance(application)

    val controllerId: String = repository.securityManager.getControllerId()
    val controllerFingerprint: String = repository.securityManager.getPublicKeyFingerprint()

    val trustedHosts: StateFlow<List<TrustedHostEntity>> =
        repository.trustedHosts.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val connectionState: StateFlow<ControllerConnectionState> = repository.connectionState
    val statusMessage: StateFlow<String> = repository.statusMessage
    val activeHost: StateFlow<TrustedHostEntity?> = repository.activeHost
    val activeSessionId: StateFlow<String?> = repository.activeSessionId
    val liveTelemetry: StateFlow<HostLiveTelemetry?> = repository.liveTelemetry
    val remoteCapabilities: StateFlow<HostRemoteCapabilities?> = repository.remoteCapabilities
    val latestFrameBytes: StateFlow<ByteArray?> = repository.latestFrameBytes
    val isScreenStreaming: StateFlow<Boolean> = repository.isScreenStreaming
    val lastInputFeedback: StateFlow<String?> = repository.lastInputFeedback

    val auditLogs: StateFlow<List<ControllerAuditLogEntity>> =
        repository.auditLogs.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    fun connectToHost(host: TrustedHostEntity) {
        repository.connectToHost(host)
    }

    fun disconnect() {
        repository.disconnect()
    }

    fun startScreenStreaming() {
        repository.startScreenStreaming()
    }

    fun stopScreenStreaming() {
        repository.stopScreenStreaming()
    }

    fun sendTap(normX: Float, normY: Float) {
        repository.sendTap(normX, normY)
    }

    fun sendLongPress(normX: Float, normY: Float) {
        repository.connectionManager.sendLongPress(normX, normY)
    }

    fun sendSwipe(normX1: Float, normY1: Float, normX2: Float, normY2: Float, durationMs: Long = 300L) {
        repository.sendSwipe(normX1, normY1, normX2, normY2, durationMs)
    }

    fun sendGlobalAction(action: String) {
        repository.sendGlobalAction(action)
    }

    fun sendTextInput(text: String) {
        repository.sendTextInput(text)
    }

    fun pairWithHost(hostId: String, code: String, signalingUrl: String, onComplete: (Boolean, String) -> Unit) {
        repository.pairWithHost(hostId, code, signalingUrl, onComplete)
    }

    fun deleteHost(hostId: String) {
        viewModelScope.launch {
            repository.deleteHost(hostId)
        }
    }

    fun clearAuditLogs() {
        viewModelScope.launch {
            repository.clearAuditLogs()
        }
    }
}
