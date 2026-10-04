package com.example.network

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.net.URI
import java.util.concurrent.TimeUnit

sealed class ControllerSignalingState {
    object Disconnected : ControllerSignalingState()
    object ConfigurationRequired : ControllerSignalingState()
    data class Connecting(val attempt: Int, val url: String) : ControllerSignalingState()
    data class Connected(val url: String) : ControllerSignalingState()
    data class Reconnecting(val nextAttemptInSec: Int, val attemptCount: Int) : ControllerSignalingState()
    data class Error(val message: String, val isSecurityViolation: Boolean = false) : ControllerSignalingState()
}

class ControllerSignalingClient(
    private val controllerId: String,
    private val onMessageReceived: (ControllerMessage) -> Unit
) {
    private val tag = "ControllerSignaling"
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private val client: OkHttpClient = OkHttpClient.Builder()
        .pingInterval(15, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private var currentWebSocket: WebSocket? = null
    private var reconnectJob: Job? = null
    private var heartbeatJob: Job? = null
    private var targetServerUrl: String = ""
    private var reconnectAttempts = 0
    private var isExplicitlyDisconnected = false

    private val _state = MutableStateFlow<ControllerSignalingState>(ControllerSignalingState.Disconnected)
    val state: StateFlow<ControllerSignalingState> = _state.asStateFlow()

    /**
     * Strictly verifies that the signaling endpoint conforms to production security standards.
     * Blocks 127.0.0.1, localhost, 10.0.2.2, http://, and ws://.
     */
    fun validateEndpointUrl(url: String): Pair<Boolean, String> {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) {
            return Pair(false, "Signaling URL cannot be blank.")
        }

        return try {
            val uri = URI(trimmed)
            val scheme = uri.scheme?.lowercase() ?: ""
            val host = uri.host?.lowercase() ?: ""

            if (scheme != "wss") {
                return Pair(
                    false,
                    "Insecure protocol ($scheme://) rejected. Salim requires TLS-encrypted 'wss://' signaling."
                )
            }

            if (host == "127.0.0.1" || host == "localhost" || host == "10.0.2.2") {
                return Pair(
                    false,
                    "Loopback address '$host' rejected. Salim cannot connect to itself; Ubaid is a separate phone on a remote network. Use a valid public signaling domain."
                )
            }

            Pair(true, "Endpoint valid")
        } catch (e: Exception) {
            Pair(false, "Malformed endpoint URL: ${e.message}")
        }
    }

    fun connect(url: String) {
        val trimmed = url.trim()
        targetServerUrl = trimmed
        isExplicitlyDisconnected = false

        val validation = validateEndpointUrl(trimmed)
        if (!validation.first) {
            Log.e(tag, "Signaling validation error: ${validation.second}")
            _state.value = ControllerSignalingState.Error(validation.second, isSecurityViolation = true)
            return
        }

        reconnectJob?.cancel()
        disconnectInternal()

        reconnectAttempts++
        _state.value = ControllerSignalingState.Connecting(reconnectAttempts, trimmed)

        try {
            val request = Request.Builder()
                .url(trimmed)
                .addHeader("X-Salim-Controller-Id", controllerId)
                .addHeader("X-Salim-Role", "controller")
                .addHeader("User-Agent", "SalimRemoteController/1.0")
                .build()

            currentWebSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.i(tag, "WebSocket connection established to $trimmed")
                    reconnectAttempts = 0
                    _state.value = ControllerSignalingState.Connected(trimmed)
                    startHeartbeat()

                    // Send initial HELLO announcement
                    val hello = ControllerMessage(
                        messageType = SalimProtocolConstants.TYPE_HELLO,
                        payload = org.json.JSONObject().apply {
                            put("controllerId", controllerId)
                            put("role", "controller")
                            put("protocolVersion", SalimProtocolConstants.VERSION)
                        }
                    )
                    sendMessage(hello)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    val message = ControllerMessage.parse(text)
                    if (message != null) {
                        onMessageReceived(message)
                    } else {
                        Log.w(tag, "Received unparseable message: $text")
                    }
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    Log.i(tag, "WebSocket closing: $code / $reason")
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Log.i(tag, "WebSocket closed: $code / $reason")
                    handleConnectionLoss("WebSocket closed: $reason")
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.e(tag, "WebSocket failure: ${t.message}", t)
                    handleConnectionLoss(t.message ?: "Signaling link failed")
                }
            })
        } catch (e: Exception) {
            Log.e(tag, "Error creating WebSocket connection", e)
            handleConnectionLoss(e.message ?: "Initialization failed")
        }
    }

    private fun handleConnectionLoss(reason: String) {
        heartbeatJob?.cancel()
        currentWebSocket = null

        if (isExplicitlyDisconnected) {
            _state.value = ControllerSignalingState.Disconnected
            return
        }

        // Bounded exponential backoff: 1s, 2s, 4s, 8s, 16s, max 30s
        val delaySec = when (reconnectAttempts) {
            0, 1 -> 1
            2 -> 2
            3 -> 4
            4 -> 8
            5 -> 16
            else -> 30
        }

        _state.value = ControllerSignalingState.Reconnecting(delaySec, reconnectAttempts)

        reconnectJob = scope.launch {
            delay(delaySec * 1000L)
            if (!isExplicitlyDisconnected && isActive) {
                Log.i(tag, "Attempting reconnect #$reconnectAttempts to $targetServerUrl")
                connect(targetServerUrl)
            }
        }
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(15000L)
                val ping = ControllerMessage(
                    messageType = SalimProtocolConstants.TYPE_PING,
                    payload = org.json.JSONObject().apply {
                        put("controllerId", controllerId)
                    }
                )
                sendMessage(ping)
            }
        }
    }

    fun sendMessage(message: ControllerMessage): Boolean {
        val ws = currentWebSocket ?: return false
        return try {
            ws.send(message.toJson())
        } catch (e: Exception) {
            Log.e(tag, "Failed to send message", e)
            false
        }
    }

    fun disconnect() {
        isExplicitlyDisconnected = true
        reconnectJob?.cancel()
        heartbeatJob?.cancel()
        disconnectInternal()
        _state.value = ControllerSignalingState.Disconnected
    }

    private fun disconnectInternal() {
        try {
            currentWebSocket?.close(1000, "Normal Controller disconnect")
        } catch (e: Exception) {
            // Ignored
        } finally {
            currentWebSocket = null
        }
    }
}
