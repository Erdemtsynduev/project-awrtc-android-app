package com.erdemtsynduev.awrtcandroid.websocket

import android.util.Log
import com.erdemtsynduev.awrtcandroid.model.netevent.NetEventType
import com.erdemtsynduev.awrtcandroid.utils.toNetworkEvent
import com.erdemtsynduev.awrtcandroid.websocket.custom.CustomWebSocket
import com.erdemtsynduev.awrtcandroid.websocket.custom.WebSocketEvent
import com.erdemtsynduev.awrtcandroid.websocket.ssl.CustomTrustManager
import java.lang.Exception
import java.net.URI
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager

class SocketManager() : WebSocketEvent {

    private var webSocket: CustomWebSocket? = null

    private var remoteProtocolVersion = 1
    private var heartBeatReceived = true

    fun connect(url: String) {
        if (webSocket == null || !webSocket?.isOpen!!) {
            val uri = URI(url)
            webSocket = CustomWebSocket(uri, this)

            if (url.startsWith("wss")) {
                try {
                    val sslContext = SSLContext.getInstance("TLS")
                    sslContext?.init(
                        null,
                        arrayOf<TrustManager>(CustomTrustManager()),
                        SecureRandom()
                    )
                    var factory: SSLSocketFactory? = null
                    if (sslContext != null) {
                        factory = sslContext.socketFactory
                    }
                    if (factory != null) {
                        webSocket?.setSocketFactory(factory)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            webSocket?.connect()
        }
    }

    fun unConnect() {
        if (webSocket != null) {
            webSocket?.setConnectFlag(false)
            webSocket?.close()
            webSocket = null
        }
    }

    private fun sendVersion() {
        if (webSocket != null) {
            webSocket?.sendVersion()
        }
    }

    private fun sendHeartbeat() {
        if (webSocket != null) {
            webSocket?.sendHeartbeat()
        }
    }

    override fun onOpen() {
        Log.i(TAG, "socket is open!")
        sendVersion()
        sendHeartbeat()
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
            //TODO ADD callback
        }
    }

    companion object {
        private const val TAG = "dds_SocketManager"
    }
}