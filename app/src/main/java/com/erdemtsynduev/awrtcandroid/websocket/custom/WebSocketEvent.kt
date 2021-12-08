package com.erdemtsynduev.awrtcandroid.websocket.custom

import java.lang.Exception
import java.nio.ByteBuffer

interface WebSocketEvent {
    fun onOpen()
    fun onClose()
    fun onReconnect()
    fun onMessage(bytes: ByteBuffer)
    fun onMessage(message: String)
    fun onError(exception: Exception)
}