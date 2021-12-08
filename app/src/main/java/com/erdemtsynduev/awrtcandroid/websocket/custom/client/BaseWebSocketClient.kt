package com.erdemtsynduev.awrtcandroid.websocket.custom.client

interface BaseWebSocketClient {

    /**
     * Отправить данные в веб сокет
     */
    fun sendByteArray(data: ByteArray?)

    /**
     * Изменить состояние соединения веб сокета
     */
    fun setConnectFlag(connectFlag: Boolean)

    /**
     * Выключить проверку ssl сертификата
     */
    fun enableUnsafeSslConnection(): Boolean
}