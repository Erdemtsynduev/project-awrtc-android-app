package com.erdemtsynduev.awrtcandroid.network

import com.erdemtsynduev.awrtcandroid.model.netevent.ConnectionId

/**
 * Общий интерфейс для WebRtcNetwork и UnityNetwork.
 *
 * Имейте в виду, что в текущей версии сеть может работать только как сервер (метод StartServer) или
 * как клиент (через метод Connect).
 */
interface WebRtcNetwork {

    /**
     * Запускает новый сервер.
     *
     * После запуска сервера метод Dequeue вернет
     * Событие ServerInitialized с адресом в поле Info.
     * Если сервер не запускается, он вернет событие ServerInitFailed.
     * Если сервер закрыт из-за ошибки или метода Shutdown, вызванного событием ServerClosed
     * будет запущен.
     */
    fun startServer(address: String?)

    /**
     * Остановка сервера.
     */
    fun stopServer()

    /**
     * Подключается к заданному адресу или имени комнаты.
     * Этот вызов приведет к одному из этих двух событий в ответ:
     * NEW_CONNECTION, если соединение было установлено
     * CONNECTION_FAILED, если соединение не удалось.
     *
     * @param address Строка, определяющая цель.
     * Возвращает идентификатор соединения, которое будет иметь
     * установленное соединение (поддерживается только WebRtcNetwork)
     */
    fun connect(address: String): ConnectionId;
}