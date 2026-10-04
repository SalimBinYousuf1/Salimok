package com.example.statemachine

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ControllerConnectionState(val displayName: String, val description: String) {
    DISCONNECTED("Disconnected", "Idle; not connected to Ubaid Host"),
    CONNECTING("Connecting", "Establishing secure WebSocket signaling link"),
    AUTHENTICATING("Authenticating", "Exchanging cryptographic challenge with Ubaid"),
    CONNECTED("Connected", "Authenticated; Host verified and idle"),
    SESSION_STARTING("Starting Session", "Negotiating media streams and remote capabilities"),
    ACTIVE("Active Session", "Live remote control session in progress"),
    RECONNECTING("Reconnecting", "Connection interrupted; retrying with exponential backoff"),
    OFFLINE("Host Offline", "Ubaid Host is currently unreachable or powered off"),
    REQUIRES_PAIRING("Pairing Required", "Host rejected credentials; re-pairing needed"),
    CAPABILITY_LIMITED("Capabilities Limited", "Host connected but certain permissions are disabled"),
    ERROR("Connection Error", "Network or cryptographic validation failed")
}

class ConnectionStateMachine {
    private val tag = "ConnectionStateMachine"

    private val _state = MutableStateFlow(ControllerConnectionState.DISCONNECTED)
    val state: StateFlow<ControllerConnectionState> = _state.asStateFlow()

    private val _statusMessage = MutableStateFlow("Ready to connect")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    fun transitionTo(newState: ControllerConnectionState, message: String): Boolean {
        val oldState = _state.value
        if (oldState == newState) {
            _statusMessage.value = message
            return true
        }

        val isAllowed = when (oldState) {
            ControllerConnectionState.DISCONNECTED -> newState in listOf(
                ControllerConnectionState.CONNECTING,
                ControllerConnectionState.OFFLINE,
                ControllerConnectionState.REQUIRES_PAIRING
            )
            ControllerConnectionState.CONNECTING -> newState in listOf(
                ControllerConnectionState.AUTHENTICATING,
                ControllerConnectionState.RECONNECTING,
                ControllerConnectionState.OFFLINE,
                ControllerConnectionState.ERROR,
                ControllerConnectionState.DISCONNECTED
            )
            ControllerConnectionState.AUTHENTICATING -> newState in listOf(
                ControllerConnectionState.CONNECTED,
                ControllerConnectionState.REQUIRES_PAIRING,
                ControllerConnectionState.ERROR,
                ControllerConnectionState.DISCONNECTED
            )
            ControllerConnectionState.CONNECTED -> newState in listOf(
                ControllerConnectionState.SESSION_STARTING,
                ControllerConnectionState.ACTIVE,
                ControllerConnectionState.CAPABILITY_LIMITED,
                ControllerConnectionState.RECONNECTING,
                ControllerConnectionState.OFFLINE,
                ControllerConnectionState.DISCONNECTED
            )
            ControllerConnectionState.SESSION_STARTING -> newState in listOf(
                ControllerConnectionState.ACTIVE,
                ControllerConnectionState.CONNECTED,
                ControllerConnectionState.ERROR,
                ControllerConnectionState.DISCONNECTED
            )
            ControllerConnectionState.ACTIVE -> newState in listOf(
                ControllerConnectionState.CONNECTED,
                ControllerConnectionState.RECONNECTING,
                ControllerConnectionState.OFFLINE,
                ControllerConnectionState.ERROR,
                ControllerConnectionState.DISCONNECTED
            )
            ControllerConnectionState.RECONNECTING -> newState in listOf(
                ControllerConnectionState.CONNECTING,
                ControllerConnectionState.OFFLINE,
                ControllerConnectionState.ERROR,
                ControllerConnectionState.DISCONNECTED
            )
            ControllerConnectionState.OFFLINE,
            ControllerConnectionState.REQUIRES_PAIRING,
            ControllerConnectionState.CAPABILITY_LIMITED,
            ControllerConnectionState.ERROR -> true
        }

        if (isAllowed) {
            Log.i(tag, "Transition: $oldState -> $newState ($message)")
            _state.value = newState
            _statusMessage.value = message
            return true
        } else {
            Log.w(tag, "Rejected illegal transition: $oldState -> $newState ($message)")
            return false
        }
    }
}
