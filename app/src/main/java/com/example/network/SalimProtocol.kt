package com.example.network

import org.json.JSONObject

object SalimProtocolConstants {
    const val VERSION = 1

    // Protocol Message Types (Strict 1:1 match with Ubaid Host)
    const val TYPE_HELLO = "HELLO"
    const val TYPE_AUTH = "AUTH"
    const val TYPE_AUTH_RESULT = "AUTH_RESULT"
    const val TYPE_CAPABILITIES = "CAPABILITIES"
    const val TYPE_PAIR_REQUEST = "PAIR_REQUEST"
    const val TYPE_PAIR_RESULT = "PAIR_RESULT"
    const val TYPE_SESSION_START = "SESSION_START"
    const val TYPE_SESSION_READY = "SESSION_READY"
    const val TYPE_SCREEN_START = "SCREEN_START"
    const val TYPE_SCREEN_STOP = "SCREEN_STOP"
    const val TYPE_SCREEN_FRAME = "SCREEN_FRAME"
    const val TYPE_INPUT_EVENT = "INPUT_EVENT"
    const val TYPE_TEXT_INPUT = "TEXT_INPUT"
    const val TYPE_DEVICE_STATUS = "DEVICE_STATUS"
    const val TYPE_COMMAND_REQUEST = "COMMAND_REQUEST"
    const val TYPE_COMMAND_RESULT = "COMMAND_RESULT"
    const val TYPE_ICE_CANDIDATE = "ICE_CANDIDATE"
    const val TYPE_SDP_OFFER = "SDP_OFFER"
    const val TYPE_SDP_ANSWER = "SDP_ANSWER"
    const val TYPE_PING = "PING"
    const val TYPE_PONG = "PONG"
    const val TYPE_ERROR = "ERROR"
    const val TYPE_SESSION_END = "SESSION_END"
}

data class ControllerMessage(
    val protocolVersion: Int = SalimProtocolConstants.VERSION,
    val messageType: String,
    val requestId: String = java.util.UUID.randomUUID().toString(),
    val sessionId: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val payload: JSONObject = JSONObject()
) {
    fun toJson(): String {
        val root = JSONObject()
        root.put("protocolVersion", protocolVersion)
        root.put("messageType", messageType)
        root.put("requestId", requestId)
        root.put("sessionId", sessionId)
        root.put("timestamp", timestamp)
        root.put("payload", payload)
        return root.toString()
    }

    companion object {
        fun parse(jsonString: String): ControllerMessage? {
            return try {
                val root = JSONObject(jsonString)
                val version = root.optInt("protocolVersion", 1)
                val type = root.getString("messageType")
                val reqId = root.optString("requestId", "")
                val sessId = root.optString("sessionId", "")
                val time = root.optLong("timestamp", System.currentTimeMillis())
                val payload = root.optJSONObject("payload") ?: JSONObject()
                ControllerMessage(version, type, reqId, sessId, time, payload)
            } catch (e: Exception) {
                null
            }
        }
    }
}
