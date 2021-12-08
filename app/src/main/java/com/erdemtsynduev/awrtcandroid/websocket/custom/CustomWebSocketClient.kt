package com.erdemtsynduev.awrtcandroid.websocket.custom

import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.lang.Exception
import java.net.URI
import java.nio.ByteBuffer

/**
 * Кастомный веб сокет с возможностью реконнекта
 * @param serverUri адрес для подключения к сокету
 * @param webSocketClientEvent ивенты событий вебсокета
 */
class CustomWebSocketClient(
    serverUri: URI?,
    private val webSocketClientEvent: WebSocketClientEvent
) : WebSocketClient(serverUri), BaseWebSocketClient {

    private var connectFlag = false

    override fun onClose(code: Int, reason: String, remote: Boolean) {
        if (connectFlag) {
            try {
                Thread.sleep(3000)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            webSocketClientEvent.onReconnect()
        } else {
            webSocketClientEvent.onClose()
        }
    }

    override fun onError(exception: Exception) {
        connectFlag = false
        webSocketClientEvent.onError(exception)
    }

    override fun onOpen(serverHandshake: ServerHandshake) {
        connectFlag = true
        webSocketClientEvent.onOpen()
    }

    override fun onMessage(bytes: ByteBuffer) {
        super.onMessage(bytes)
        webSocketClientEvent.onMessage(bytes)
    }

    override fun onMessage(message: String) {
        webSocketClientEvent.onMessage(message)
    }

    override fun setConnectFlag(connectFlag: Boolean) {
        this.connectFlag = connectFlag
    }

    override fun sendByteArray(data: ByteArray?) {
        if (data != null) {
            internalSend(data)
        }
    }

    private fun internalSend(data: ByteArray?) {
        if (connectFlag) {
            send(data)
        }
    }
}