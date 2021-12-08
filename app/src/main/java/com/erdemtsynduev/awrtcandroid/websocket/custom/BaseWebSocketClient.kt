package com.erdemtsynduev.awrtcandroid.websocket.custom

interface BaseWebSocketClient {

    fun sendByteArray(data: ByteArray?)

    fun setConnectFlag(connectFlag: Boolean)
}