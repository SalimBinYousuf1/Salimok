package com.example.network

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.data.db.ControllerAuditLogEntity
import com.example.data.db.SalimDatabase
import com.example.data.db.TrustedHostEntity
import com.example.security.ControllerSecurityManager
import com.example.statemachine.ConnectionStateMachine
import com.example.statemachine.ControllerConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

data class HostLiveTelemetry(
    val batteryPercent: Int,
    val isCharging: Boolean,
    val batteryHealth: String,
    val batteryTempCelsius: Float,
    val networkType: String,
    val localIpAddress: String,
    val isScreenOn: Boolean,
    val isKeyguardLocked: Boolean,
    val displayResolution: String,
    val deviceModel: String,
    val androidVersion: String,
    val timestamp: Long
)

data class HostRemoteCapabilities(
    val hasInternet: Boolean = false,
    val hasNotifications: Boolean = false,
    val hasAccessibility: Boolean = false,
    val hasMediaProjection: Boolean = false,
    val isBatteryOptimizationsIgnored: Boolean = false,
    val isForegroundServiceRunning: Boolean = false,
    val isDeviceOwner: Boolean = false
)

class HostConnectionManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val securityManager: ControllerSecurityManager,
    private val database: SalimDatabase
) {
    private val tag = "HostConnectionManager"

    val stateMachine = ConnectionStateMachine()
    val connectionState: StateFlow<ControllerConnectionState> = stateMachine.state
    val statusMessage: StateFlow<String> = stateMachine.statusMessage

    private val _activeHost = MutableStateFlow<TrustedHostEntity?>(null)
    val activeHost: StateFlow<TrustedHostEntity?> = _activeHost.asStateFlow()

    private val _activeSessionId = MutableStateFlow<String?>(null)
    val activeSessionId: StateFlow<String?> = _activeSessionId.asStateFlow()

    private val _liveTelemetry = MutableStateFlow<HostLiveTelemetry?>(null)
    val liveTelemetry: StateFlow<HostLiveTelemetry?> = _liveTelemetry.asStateFlow()

    private val _remoteCapabilities = MutableStateFlow<HostRemoteCapabilities?>(null)
    val remoteCapabilities: StateFlow<HostRemoteCapabilities?> = _remoteCapabilities.asStateFlow()

    private val _latestFrameBytes = MutableStateFlow<ByteArray?>(null)
    val latestFrameBytes: StateFlow<ByteArray?> = _latestFrameBytes.asStateFlow()

    private val _isScreenStreaming = MutableStateFlow(false)
    val isScreenStreaming: StateFlow<Boolean> = _isScreenStreaming.asStateFlow()

    private val _lastInputFeedback = MutableStateFlow<String?>(null)
    val lastInputFeedback: StateFlow<String?> = _lastInputFeedback.asStateFlow()

    private var statusPollJob: Job? = null

    val signalingClient = ControllerSignalingClient(
        controllerId = securityManager.getControllerId(),
        onMessageReceived = { message -> handleIncomingMessage(message) }
    )

    init {
        // Observe signaling connection changes
        scope.launch {
            signalingClient.state.collect { sState ->
                when (sState) {
                    is ControllerSignalingState.Connected -> {
                        val host = _activeHost.value
                        if (host != null && _activeSessionId.value == null) {
                            stateMachine.transitionTo(
                                ControllerConnectionState.AUTHENTICATING,
                                "Signaling active; authenticating with ${host.displayName}"
                            )
                            sendAuthenticationRequest(host)
                        }
                    }
                    is ControllerSignalingState.Reconnecting -> {
                        stateMachine.transitionTo(
                            ControllerConnectionState.RECONNECTING,
                            "Signaling dropped; retrying in ${sState.nextAttemptInSec}s"
                        )
                    }
                    is ControllerSignalingState.Error -> {
                        stateMachine.transitionTo(
                            ControllerConnectionState.ERROR,
                            sState.message
                        )
                    }
                    is ControllerSignalingState.Disconnected -> {
                        if (stateMachine.state.value != ControllerConnectionState.OFFLINE &&
                            stateMachine.state.value != ControllerConnectionState.DISCONNECTED
                        ) {
                            stateMachine.transitionTo(
                                ControllerConnectionState.DISCONNECTED,
                                "Disconnected from Host"
                            )
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    fun connectToHost(host: TrustedHostEntity) {
        _activeHost.value = host
        _activeSessionId.value = null
        _latestFrameBytes.value = null
        _isScreenStreaming.value = false

        stateMachine.transitionTo(
            ControllerConnectionState.CONNECTING,
            "Connecting to signaling endpoint: ${host.signalingServerUrl}"
        )

        signalingClient.connect(host.signalingServerUrl)
    }

    fun disconnect() {
        statusPollJob?.cancel()
        val host = _activeHost.value
        val sessId = _activeSessionId.value

        if (sessId != null) {
            val endMsg = ControllerMessage(
                messageType = SalimProtocolConstants.TYPE_SESSION_END,
                sessionId = sessId
            )
            signalingClient.sendMessage(endMsg)
        }

        signalingClient.disconnect()
        _activeSessionId.value = null
        _isScreenStreaming.value = false
        stateMachine.transitionTo(ControllerConnectionState.DISCONNECTED, "Disconnected by user")
    }

    private fun sendAuthenticationRequest(host: TrustedHostEntity) {
        val nonce = UUID.randomUUID().toString()
        val rawChallenge = "${host.hostId}:${securityManager.getControllerId()}:$nonce".toByteArray(Charsets.UTF_8)
        val sig = securityManager.signDataToBase64(rawChallenge)

        val authMsg = ControllerMessage(
            messageType = SalimProtocolConstants.TYPE_AUTH,
            payload = JSONObject().apply {
                put("controllerId", securityManager.getControllerId())
                put("signature", sig)
                put("nonce", nonce)
            }
        )
        signalingClient.sendMessage(authMsg)
    }

    fun sendPairingRequest(
        hostId: String,
        pairingCode: String,
        signalingUrl: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        val nonce = UUID.randomUUID().toString()
        val rawData = "$hostId:$pairingCode:${securityManager.getControllerId()}:$nonce".toByteArray(Charsets.UTF_8)
        val sig = securityManager.signDataToBase64(rawData)

        // Ensure signaling client connects to target endpoint first
        scope.launch {
            stateMachine.transitionTo(ControllerConnectionState.CONNECTING, "Contacting Ubaid Host via signaling...")
            signalingClient.connect(signalingUrl)

            // Wait up to 5s for open connection
            var waited = 0
            while (signalingClient.state.value !is ControllerSignalingState.Connected && waited < 25) {
                delay(200L)
                waited++
            }

            val pairMsg = ControllerMessage(
                messageType = SalimProtocolConstants.TYPE_PAIR_REQUEST,
                payload = JSONObject().apply {
                    put("pairingCode", pairingCode)
                    put("controllerId", securityManager.getControllerId())
                    put("controllerPublicKey", securityManager.getPublicKeyBase64())
                    put("displayName", "Salim Controller (${android.os.Build.MODEL})")
                    put("signature", sig)
                    put("nonce", nonce)
                }
            )

            signalingClient.sendMessage(pairMsg)
        }
    }

    private fun handleIncomingMessage(message: ControllerMessage) {
        scope.launch(Dispatchers.IO) {
            when (message.messageType) {
                SalimProtocolConstants.TYPE_AUTH_RESULT -> handleAuthResult(message)
                SalimProtocolConstants.TYPE_PAIR_RESULT -> handlePairResult(message)
                SalimProtocolConstants.TYPE_CAPABILITIES -> handleCapabilities(message)
                SalimProtocolConstants.TYPE_DEVICE_STATUS -> handleDeviceStatus(message)
                SalimProtocolConstants.TYPE_COMMAND_RESULT -> handleCommandResult(message)
                SalimProtocolConstants.TYPE_SCREEN_FRAME -> handleScreenFrame(message)
                SalimProtocolConstants.TYPE_ERROR -> handleError(message)
                SalimProtocolConstants.TYPE_SESSION_END -> {
                    stateMachine.transitionTo(ControllerConnectionState.DISCONNECTED, "Host terminated session")
                    disconnect()
                }
            }
        }
    }

    private suspend fun handleAuthResult(message: ControllerMessage) {
        val payload = message.payload
        val authenticated = payload.optBoolean("authenticated", false)

        if (authenticated) {
            val sessionId = payload.optString("sessionId")
            _activeSessionId.value = sessionId

            val host = _activeHost.value
            if (host != null) {
                database.trustedHostDao().updateLastSeen(host.hostId, System.currentTimeMillis())
                logAudit("AUTH", host.hostId, "Authenticated secure session $sessionId with Ubaid", "INFO")
            }

            stateMachine.transitionTo(
                ControllerConnectionState.CONNECTED,
                "Authenticated with Ubaid Host ($sessionId)"
            )

            // Request capabilities & status immediately
            requestCapabilities()
            requestDeviceStatus()
            startStatusPolling()
        } else {
            val reason = payload.optString("reason", "Authentication failed")
            stateMachine.transitionTo(ControllerConnectionState.REQUIRES_PAIRING, reason)
            logAudit("AUTH_ERROR", _activeHost.value?.hostId ?: "", reason, "ERROR")
        }
    }

    private suspend fun handlePairResult(message: ControllerMessage) {
        val payload = message.payload
        val success = payload.optBoolean("success", false)

        if (success) {
            val hostId = payload.optString("hostId")
            val hostPublicKey = payload.optString("hostPublicKey")
            val host = TrustedHostEntity(
                hostId = hostId,
                displayName = "Ubaid ($hostId)",
                publicKey = hostPublicKey,
                pairedAt = System.currentTimeMillis(),
                lastSeen = System.currentTimeMillis(),
                isPrimary = true
            )
            database.trustedHostDao().insertHost(host)
            _activeHost.value = host
            logAudit("PAIR", hostId, "Successfully paired with Ubaid Host", "INFO")

            stateMachine.transitionTo(ControllerConnectionState.CONNECTED, "Paired successfully. Ready to connect.")
        } else {
            val reason = payload.optString("reason", "Pairing rejected by Host")
            stateMachine.transitionTo(ControllerConnectionState.ERROR, reason)
            logAudit("PAIR_ERROR", "", reason, "WARN")
        }
    }

    private fun handleCapabilities(message: ControllerMessage) {
        val p = message.payload
        val caps = HostRemoteCapabilities(
            hasInternet = p.optBoolean("hasInternet", false),
            hasNotifications = p.optBoolean("hasNotifications", false),
            hasAccessibility = p.optBoolean("hasAccessibility", false),
            hasMediaProjection = p.optBoolean("hasMediaProjection", false),
            isBatteryOptimizationsIgnored = p.optBoolean("isBatteryOptimizationsIgnored", false),
            isForegroundServiceRunning = p.optBoolean("isForegroundServiceRunning", false),
            isDeviceOwner = p.optBoolean("isDeviceOwner", false)
        )
        _remoteCapabilities.value = caps
    }

    private suspend fun handleDeviceStatus(message: ControllerMessage) {
        val p = message.payload
        val telemetry = HostLiveTelemetry(
            batteryPercent = p.optInt("batteryPercent", -1),
            isCharging = p.optBoolean("isCharging", false),
            batteryHealth = p.optString("batteryHealth", "Good"),
            batteryTempCelsius = p.optDouble("batteryTempCelsius", 25.0).toFloat(),
            networkType = p.optString("networkType", "Wi-Fi"),
            localIpAddress = p.optString("localIpAddress", "Unknown"),
            isScreenOn = p.optBoolean("isScreenOn", true),
            isKeyguardLocked = p.optBoolean("isKeyguardLocked", false),
            displayResolution = p.optString("displayResolution", "1080x2400"),
            deviceModel = p.optString("deviceModel", "Ubaid Host"),
            androidVersion = p.optString("androidVersion", "Android 14"),
            timestamp = p.optLong("timestamp", System.currentTimeMillis())
        )
        _liveTelemetry.value = telemetry

        // Update cached values in Room database
        _activeHost.value?.let { host ->
            database.trustedHostDao().updateCachedTelemetry(
                hostId = host.hostId,
                battery = telemetry.batteryPercent,
                isCharging = telemetry.isCharging,
                networkType = telemetry.networkType,
                model = telemetry.deviceModel,
                androidVer = telemetry.androidVersion,
                resolution = telemetry.displayResolution,
                timestamp = telemetry.timestamp
            )
        }
    }

    private fun handleCommandResult(message: ControllerMessage) {
        val p = message.payload
        val command = p.optString("command")
        val success = p.optBoolean("success", false)
        val msg = p.optString("message", if (success) "Executed" else "Failed")

        _lastInputFeedback.value = "[$command]: $msg"
    }

    private fun handleScreenFrame(message: ControllerMessage) {
        val base64Frame = message.payload.optString("data")
        if (base64Frame.isNotEmpty()) {
            try {
                val bytes = Base64.decode(base64Frame, Base64.NO_WRAP)
                _latestFrameBytes.value = bytes
            } catch (e: Exception) {
                Log.e(tag, "Failed to decode screen frame", e)
            }
        }
    }

    private fun handleError(message: ControllerMessage) {
        val errCode = message.payload.optString("errorCode")
        val errMsg = message.payload.optString("errorMessage")

        if (errCode == "CONTROLLER_REVOKED") {
            stateMachine.transitionTo(ControllerConnectionState.REQUIRES_PAIRING, "Host revoked access for this controller.")
            disconnect()
        } else {
            stateMachine.transitionTo(ControllerConnectionState.ERROR, "Host error: $errCode - $errMsg")
        }
    }

    fun requestCapabilities() {
        val sessId = _activeSessionId.value ?: return
        val msg = ControllerMessage(
            messageType = SalimProtocolConstants.TYPE_CAPABILITIES,
            sessionId = sessId
        )
        signalingClient.sendMessage(msg)
    }

    fun requestDeviceStatus() {
        val sessId = _activeSessionId.value ?: return
        val msg = ControllerMessage(
            messageType = SalimProtocolConstants.TYPE_DEVICE_STATUS,
            sessionId = sessId
        )
        signalingClient.sendMessage(msg)
    }

    fun startScreenStreaming() {
        val sessId = _activeSessionId.value ?: return
        stateMachine.transitionTo(ControllerConnectionState.ACTIVE, "Screen streaming active")
        _isScreenStreaming.value = true

        val msg = ControllerMessage(
            messageType = SalimProtocolConstants.TYPE_SCREEN_START,
            sessionId = sessId
        )
        signalingClient.sendMessage(msg)
    }

    fun stopScreenStreaming() {
        val sessId = _activeSessionId.value ?: return
        _isScreenStreaming.value = false
        stateMachine.transitionTo(ControllerConnectionState.CONNECTED, "Screen streaming paused")

        val msg = ControllerMessage(
            messageType = SalimProtocolConstants.TYPE_SCREEN_STOP,
            sessionId = sessId
        )
        signalingClient.sendMessage(msg)
    }

    fun sendTap(normX: Float, normY: Float) {
        val sessId = _activeSessionId.value ?: return
        val msg = ControllerMessage(
            messageType = SalimProtocolConstants.TYPE_INPUT_EVENT,
            sessionId = sessId,
            payload = JSONObject().apply {
                put("action", "TAP")
                put("x", normX)
                put("y", normY)
            }
        )
        signalingClient.sendMessage(msg)
    }

    fun sendLongPress(normX: Float, normY: Float) {
        val sessId = _activeSessionId.value ?: return
        val msg = ControllerMessage(
            messageType = SalimProtocolConstants.TYPE_INPUT_EVENT,
            sessionId = sessId,
            payload = JSONObject().apply {
                put("action", "LONG_PRESS")
                put("x", normX)
                put("y", normY)
            }
        )
        signalingClient.sendMessage(msg)
    }

    fun sendSwipe(normX1: Float, normY1: Float, normX2: Float, normY2: Float, durationMs: Long = 300L) {
        val sessId = _activeSessionId.value ?: return
        val msg = ControllerMessage(
            messageType = SalimProtocolConstants.TYPE_INPUT_EVENT,
            sessionId = sessId,
            payload = JSONObject().apply {
                put("action", "SWIPE")
                put("startX", normX1)
                put("startY", normY1)
                put("endX", normX2)
                put("endY", normY2)
                put("durationMs", durationMs)
            }
        )
        signalingClient.sendMessage(msg)
    }

    fun sendGlobalAction(action: String) {
        val sessId = _activeSessionId.value ?: return
        val msg = ControllerMessage(
            messageType = SalimProtocolConstants.TYPE_COMMAND_REQUEST,
            sessionId = sessId,
            payload = JSONObject().apply {
                put("command", action)
            }
        )
        signalingClient.sendMessage(msg)
    }

    fun sendTextInput(text: String) {
        val sessId = _activeSessionId.value ?: return
        val msg = ControllerMessage(
            messageType = SalimProtocolConstants.TYPE_TEXT_INPUT,
            sessionId = sessId,
            payload = JSONObject().apply {
                put("text", text)
            }
        )
        signalingClient.sendMessage(msg)
    }

    private fun startStatusPolling() {
        statusPollJob?.cancel()
        statusPollJob = scope.launch {
            while (isActive && _activeSessionId.value != null) {
                delay(10000L)
                requestDeviceStatus()
            }
        }
    }

    private suspend fun logAudit(eventType: String, hostId: String, details: String, severity: String) {
        try {
            database.auditLogDao().insertLog(
                ControllerAuditLogEntity(
                    eventType = eventType,
                    hostId = hostId,
                    details = details,
                    severity = severity
                )
            )
        } catch (e: Exception) {
            Log.e(tag, "Audit log write failure", e)
        }
    }
}
