package com.erdemtsynduev.awrtcandroid.websocket.custom

import java.lang.Exception
import java.nio.ByteBuffer

/**
 * Общий интерфейс для WebSocketClientCustom
 */
interface WebSocketClientCallback {

    /**
     * Событие - Веб сокет открыт
     */
    fun onOpen()

    /**
     * Событие - Веб сокет закрыт
     */
    fun onClose()

    /**
     * Событие - Веб сокет переподключение
     */
    fun onReconnect()

    /**
     * Событие - Пришли данные из веб сокета в виде байтового массива
     */
    fun onMessage(bytes: ByteBuffer)

    /**
     * Событие - Пришли данные из веб сокета в виде строки
     */
    fun onMessage(message: String)

    /**
     * Событие - Произошла ошибка из веб сокета
     */
    fun onError(exception: Exception)
}