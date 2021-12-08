package com.erdemtsynduev.awrtcandroid.websocket

import android.util.Log
import com.erdemtsynduev.awrtcandroid.model.netevent.ConnectionId
import com.erdemtsynduev.awrtcandroid.model.netevent.NetEventType
import com.erdemtsynduev.awrtcandroid.model.netevent.NetEvent
import com.erdemtsynduev.awrtcandroid.utils.ConstData
import com.erdemtsynduev.awrtcandroid.utils.toByteArray
import com.erdemtsynduev.awrtcandroid.utils.toNetworkEvent
import com.erdemtsynduev.awrtcandroid.websocket.custom.CustomWebSocketClient
import com.erdemtsynduev.awrtcandroid.websocket.custom.WebSocketClientEvent
import com.erdemtsynduev.awrtcandroid.websocket.ssl.CustomX509TrustManager
import java.lang.Exception
import java.net.URI
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager

class SocketClientManager() : WebSocketClientEvent {

    private var customWebSocketClient: CustomWebSocketClient? = null

    private var remoteProtocolVersion = 1
    private var heartBeatReceived = true

    fun connect(url: String) {
        if (customWebSocketClient == null || !customWebSocketClient?.isOpen!!) {
            val uri = URI(url)
            customWebSocketClient = CustomWebSocketClient(uri, this)

            if (url.startsWith("wss")) {
                customWebSocketClient?.enableUnsafeSslConnection()
            }
            customWebSocketClient?.connect()
        }
    }

    fun unConnect() {
        if (customWebSocketClient != null) {
            customWebSocketClient?.setConnectFlag(false)
            customWebSocketClient?.close()
            customWebSocketClient = null
        }
    }

    override fun onOpen() {
        Log.i(TAG, "socket is open!")
        sendVersion()
        sendHeartbeat()
        startServer("")
    }

    override fun onClose() {
        Log.i(TAG, "onClose")
    }

    override fun onReconnect() {
        Log.i(TAG, "onReconnect")
    }

    override fun onMessage(bytes: ByteBuffer) {
        Log.i(TAG, "onMessage bytes")
        parseMessage(bytes.array())
    }

    override fun onMessage(message: String) {
        Log.i(TAG, "onMessage string")
    }

    override fun onError(exception: Exception) {
        Log.i(TAG, "onError")
    }

    private fun parseMessage(byteArray: ByteArray) {
        if (byteArray.isEmpty()) {
            Log.i(TAG, "byteArray empty")
            return
        } else if (byteArray[0] == NetEventType.META_VERSION.value) {
            Log.i(TAG, "byteArray NetEventType.META_VERSION")
            if (byteArray.size > 1) {
                this.remoteProtocolVersion = byteArray[1].toInt()
                Log.i(TAG, "remoteProtocolVersion = $remoteProtocolVersion")
            } else {
                Log.i(TAG, "Received an invalid MetaVersion header without content.")
            }
        } else if (byteArray[0] == NetEventType.META_HEART_BEAT.value) {
            this.heartBeatReceived = true
            Log.i(TAG, "heartBeatReceived = $heartBeatReceived")
        } else {
            val event = byteArray.toNetworkEvent()
            Log.i(TAG, "event = $event")
        }
    }

    fun startServer(address: String) {
        sendNetworkEvent(
            NetEvent(
                netEventType = NetEventType.SERVER_INITIALIZED,
                connectionId = ConnectionId(),
                dataString = "OYTIGZF"
            )
        )
    }

    fun stopServer() {
        sendNetworkEvent(
            NetEvent(
                netEventType = NetEventType.SERVER_CLOSED,
                connectionId = ConnectionId()
            )
        )
    }

    fun sendNetworkEvent(evt: NetEvent) {
        sendByteArray(evt.toByteArray())
    }

    private fun sendByteArray(data: ByteArray?) {
        if (customWebSocketClient != null) {
            customWebSocketClient?.sendByteArray(data)
        }
    }

    private fun sendVersion() {
        if (customWebSocketClient != null) {
            val byteArray = ByteArray(2)
            byteArray[0] = NetEventType.META_VERSION.value
            byteArray[1] = ConstData.PROTOCOL_VERSION.toByte()
            customWebSocketClient?.sendByteArray(byteArray)
        }
    }

    fun sendHeartbeat() {
        if (customWebSocketClient != null) {
            val byteArray = ByteArray(1)
            byteArray[0] = NetEventType.META_HEART_BEAT.value
            customWebSocketClient?.sendByteArray(byteArray)
        }
    }

    companion object {
        private const val TAG = "dds_SocketManager"
    }
}