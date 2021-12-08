package com.erdemtsynduev.awrtcandroid.websocket.custom

import com.erdemtsynduev.awrtcandroid.model.netevent.NetEventType
import com.erdemtsynduev.awrtcandroid.utils.ConstData
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.lang.Exception
import java.net.URI
import java.nio.ByteBuffer

class CustomWebSocket(
    serverUri: URI?,
    private val webSocketEvent: WebSocketEvent
) : WebSocketClient(serverUri) {

    private var connectFlag = false

    override fun onClose(code: Int, reason: String, remote: Boolean) {
        if (connectFlag) {
            try {
                Thread.sleep(3000)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            webSocketEvent.onReconnect()
        } else {
            webSocketEvent.onClose()
        }
    }

    override fun onError(exception: Exception) {
        connectFlag = false
        webSocketEvent.onError(exception)
    }

    override fun onOpen(serverHandshake: ServerHandshake) {
        connectFlag = true
        webSocketEvent.onOpen()
    }

    override fun onMessage(bytes: ByteBuffer) {
        super.onMessage(bytes)
        webSocketEvent.onMessage(bytes)
    }

    override fun onMessage(message: String) {
        webSocketEvent.onMessage(message)
    }

    fun setConnectFlag(flag: Boolean) {
        connectFlag = flag
    }

    fun sendVersion() {
        val byteArray = ByteArray(2)
        byteArray[0] = NetEventType.META_VERSION.value
        byteArray[1] = ConstData.PROTOCOL_VERSION.toByte()
        this.internalSend(byteArray)
    }

    fun sendHeartbeat() {
        val byteArray = ByteArray(1)
        byteArray[0] = NetEventType.META_HEART_BEAT.value
        this.internalSend(byteArray)
    }

    fun sendByteArray(data: ByteArray?) {
        internalSend(data)
    }

    private fun internalSend(data: ByteArray?) {
        if (connectFlag) {
            send(data)
        }
    }
}