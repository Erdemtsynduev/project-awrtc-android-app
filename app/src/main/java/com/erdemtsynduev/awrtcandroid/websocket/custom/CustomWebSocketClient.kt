package com.erdemtsynduev.awrtcandroid.websocket.custom

import com.erdemtsynduev.awrtcandroid.websocket.ssl.CustomX509TrustManager
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.lang.Exception
import java.net.URI
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager

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
            } catch (exception: InterruptedException) {
                exception.printStackTrace()
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
        webSocketClientEvent.onMessage(bytes)
    }

    override fun onMessage(message: String) {
        webSocketClientEvent.onMessage(message)
    }

    override fun setConnectFlag(connectFlag: Boolean) {
        this.connectFlag = connectFlag
    }

    override fun sendByteArray(data: ByteArray?) {
        data?.let {
            internalSend(data)
        }
    }

    private fun internalSend(data: ByteArray?) {
        if (connectFlag) {
            send(data)
        }
    }

    override fun enableUnsafeSslConnection(): Boolean {
        return try {
            val sslContext: SSLContext = SSLContext.getInstance("TLS")
            sslContext.init(null,
                arrayOf<TrustManager>(CustomX509TrustManager()),
                SecureRandom()
            )
            setSocketFactory(sslContext.socketFactory)
            true
        } catch (exception: Exception) {
            exception.printStackTrace()
            false
        }
    }
}