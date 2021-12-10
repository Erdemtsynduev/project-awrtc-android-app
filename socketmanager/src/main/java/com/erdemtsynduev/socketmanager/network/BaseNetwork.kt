package com.erdemtsynduev.socketmanager.network

import com.erdemtsynduev.socketmanager.model.netevent.ConnectionId
import com.erdemtsynduev.socketmanager.model.netevent.NetEvent

/**
 * Интерфейс к сети, которая не требует сохранения каких-либо состояний.
 *
 * Все, что угодно, можно многократно использовать в нескольких разных сетях.
 */
interface BaseNetwork {

    /**
     * Это вернет входящие сетевые события. Вызовите этот метод и обрабатывайте
     * входящие в него события, пока он не вернет false.
     * Возвращает true, если параметр evt содержит новое событие.
     * false, если не осталось событий для обработки.
     */
    fun dequeue(): NetEvent

    fun peek(): NetEvent

    /**
     * Отправляет буферизованные данные.
     * Может также очистить все неиспользуемые события из очереди!
     */
    fun flush(): NetEvent

    /**
     * Отправляет содержимое байтового массива в указанное соединение.
     * @param id Идентификатор получателя
     * @param data байтовый массив, содержащий данные для отправки
     * @param reliable true для отправки надежного сообщения (стиль tcp)
     * и false для отправки ненадежного (стиль udp)
     */
    fun sendData(id: ConnectionId, data: ByteArray, reliable: Boolean): Boolean

    /**
     * Отключает данное соединение
     * @param id Идентификатор соединения, которое требуется отключить.
     */
    fun disconnect(id: ConnectionId)

    /**
     * Отключает все соединения и выключает сервер, если он запущен.
     * Dequeue по-прежнему будет возвращать подтверждающие сообщения,
     * такие как Disconnected event, для каждого соединения.
     */
    fun shutdown()

    /**
     * Вызывайте это каждый кадр, если вы собираетесь
     * читать входящие сообщения с помощью Dequeue. Это сделает
     * убедитесь, что все данные прочитаны и получены сетью.
     */
    fun update()

    fun dispose()
}