package com.erdemtsynduev.awrtcandroid.websocket

import android.util.Log
import com.erdemtsynduev.awrtcandroid.model.WebSocketConnectionStatus
import com.erdemtsynduev.awrtcandroid.model.WebSocketServerStatus
import com.erdemtsynduev.awrtcandroid.model.netevent.ConnectionId
import com.erdemtsynduev.awrtcandroid.model.netevent.NetEventType
import com.erdemtsynduev.awrtcandroid.model.netevent.NetEvent
import com.erdemtsynduev.awrtcandroid.utils.toByteArray
import com.erdemtsynduev.awrtcandroid.utils.toNetworkEvent
import com.erdemtsynduev.websocket.WebSocketClientCallback
import com.erdemtsynduev.websocket.client.CustomWebSocketClient
import java.lang.Exception
import java.net.URI
import java.nio.ByteBuffer
import java.util.*
import kotlin.collections.ArrayDeque
import kotlin.math.roundToInt

class SocketClientManager(
    var urlWebSocket: String? = null,
    var config: Configuration? = null
) : WebSocketClientCallback {

    // web socket client
    private var webSocketClient: CustomWebSocketClient? = null

    // Текущий статус подключения к веб сокету. Будет обновляться на основе вызова обновления
    private var connectionStatus = WebSocketConnectionStatus.UNINITIALIZED

    // Очередь для буферизованных исходящих сообщений
    private var outgoingQueue = ArrayDeque<NetEvent>()

    // Буфер для входящих сообщений
    private var incomingQueue = ArrayDeque<NetEvent>()

    // Статус сервера для входящих подключений
    private var serverStatus = WebSocketServerStatus.OFFLINE

    // Исходящие соединения (просто необходимо сохранить,
    // чтобы можно было отправить сообщение об ошибке
    // если все сигнальное соединение не работает
    private var connecting = ArrayDeque<Short>()
    private var connections = ArrayDeque<Short>()

    // идентификатор следующего id подключения
    private var nextOutgoingConnectionId = ConnectionId(1)

    /**
     * Версия протокола, реализованная здесь
     */
    private val PROTOCOL_VERSION: Int = 2

    /**
     * Минимальная версия протокола, которая все еще поддерживается.
     * Серверы V 1 не распознают сердцебиение и версию
     * сообщения, но просто регистрирует неизвестное сообщение и
     * продолжайте нормально.
     */
    private val PROTOCOL_VERSION_MIN: Int = 1

    // Предполагайте 1, пока сообщение не получено
    private var remoteProtocolVersion: Int = 1

    private var lastHeartbeat: Long? = null
    private var heartbeatReceived = true

    private var isDisposed = false

    init {
        connectionStatus = WebSocketConnectionStatus.NOT_CONNECTED

        if (config == null) {
            config = Configuration()
        }
        config?.lock()
    }

    private fun webSocketConnect() {
        connectionStatus = WebSocketConnectionStatus.CONNECTING
        urlWebSocket?.let {
            connectSocket(it)
        }
    }

    // Подключение к веб сокету
    private fun connectSocket(url: String) {
        if (webSocketClient == null || !webSocketClient?.isOpen!!) {
            val uri = URI(url)
            webSocketClient = CustomWebSocketClient.instance(
                serverUri = uri,
                webSocketClientCallback = this
            )

            if (url.startsWith("wss")) {
                webSocketClient?.enableUnsafeSslConnection()
            }
            webSocketClient?.connect()
        }
    }

    private fun webSocketCleanup() {
        unConnect()
    }

    // Отключение от веб сокета
    private fun unConnect() {
        if (webSocketClient != null) {
            webSocketClient?.setConnectFlag(false)
            webSocketClient?.close()
            webSocketClient = null
        }
    }

    // Проверить текущее соединение
    private fun ensureServerConnection() {
        if (connectionStatus == WebSocketConnectionStatus.NOT_CONNECTED) {
            webSocketConnect()
        }
    }

    private fun updateHeartbeat() {
        if (connectionStatus == WebSocketConnectionStatus.CONNECTED && config?.heartbeat!! > 0) {
            val diff = Date().time - lastHeartbeat!!
            if (diff > (config?.heartbeat!! * 1000)) {
                //We trigger heatbeat timeouts only for protocol V2
                //protocol 1 can receive the heatbeats but
                //won't send a reply
                //(still helpful to trigger TCP ACK timeout)
                if (remoteProtocolVersion > 1 && !heartbeatReceived) {
                    triggerHeartbeatTimeout()
                    return
                }
                lastHeartbeat = Date().time
                heartbeatReceived = false
                sendHeartbeat()
            }
        }
    }

    // Closing due to heartbeat timeout. Server didn't respond in time.
    private fun triggerHeartbeatTimeout() {
        cleanUp()
    }

    private fun checkSleep() {
        if (connectionStatus == WebSocketConnectionStatus.CONNECTED
            && serverStatus == WebSocketServerStatus.OFFLINE
            && connecting.size == 0
            && connections.size == 0
        ) {
            //no server
            //no connection about to be established
            //no current connections
            //-> disconnect the server connection
            cleanUp()
        }
    }

    // Событие о том, что веб сокет открыт
    override fun onOpen() {
        changeStateAndSendVersion()
    }

    override fun onReconnect() {
        changeStateAndSendVersion()
    }

    private fun changeStateAndSendVersion() {
        // Меняем статус соединения на CONNECTED
        connectionStatus = WebSocketConnectionStatus.CONNECTED
        // Сохраняем текущее время
        lastHeartbeat = Date().time
        // Отправляем запрос на веб сокет для получения версии сервера
        sendVersion()
    }

    override fun onClose() {
        // Игнорировать закрытое событие, если оно
        // было вызвано выключением (это означает, что мы уже очистили)
        if (connectionStatus == WebSocketConnectionStatus.DISCONNECTING
            || connectionStatus == WebSocketConnectionStatus.NOT_CONNECTED
        ) return
        // Очищаем данные подключения
        cleanUp()
        // Изменяем состояние подключения
        connectionStatus = WebSocketConnectionStatus.NOT_CONNECTED
    }

    override fun onMessage(bytes: ByteBuffer) {
        if (connectionStatus == WebSocketConnectionStatus.DISCONNECTING
            || connectionStatus == WebSocketConnectionStatus.NOT_CONNECTED
        ) return
        // Парсим ответ от веб сокета
        parseMessage(bytes.array())
    }

    override fun onMessage(message: String) {
        Log.i(TAG, "onMessage string")
    }

    override fun onError(exception: Exception) {
        exception.printStackTrace()
    }

    /// <summary>
    /// called during Disconnecting state either trough server connection failed or due to Shutdown
    ///
    /// Also used to switch to sleeping mode. In this case there connection isn't used as
    /// server and doesn't have any connections (established or connecting) thus
    /// only WebsocketCleanup is in effect.
    ///
    /// WebsocketNetwork has to be still usable after this call like a newly
    /// created connections (except with events in the message queue)
    /// </summary>
    private fun cleanUp() {
        //check if this was done already (or we are in the process of cleanup already)
        if (connectionStatus == WebSocketConnectionStatus.DISCONNECTING
            || connectionStatus == WebSocketConnectionStatus.NOT_CONNECTED
        ) return

        connectionStatus = WebSocketConnectionStatus.DISCONNECTING

        // throw connection failed events for each connection in mConnecting
        connecting.forEach { conId ->
            enqueueIncomingNetEventType(NetEventType.CONNECTION_FAILED, ConnectionId(conId))
        }
        connecting.clear()

        // all connection it tries to establish right now fail due to shutdown
        connections.forEach { conId ->
            enqueueIncomingNetEventType(NetEventType.DISCONNECTED, ConnectionId(conId))
        }
        connections.clear()

        when (serverStatus) {
            WebSocketServerStatus.STARTING -> {
                //if server was Starting -> throw failed event
                enqueueIncomingNetEventType(NetEventType.SERVER_CLOSED, ConnectionId())
            }
            WebSocketServerStatus.ONLINE -> {
                //if server was Online -> throw close event
                enqueueIncomingNetEventType(NetEventType.SERVER_CLOSED, ConnectionId())
            }
            WebSocketServerStatus.SHUTTING_DOWN -> {
                //if server was ShuttingDown -> throw close event (don't think this can happen)
                enqueueIncomingNetEventType(NetEventType.SERVER_CLOSED, ConnectionId())
            }
            else -> {
                enqueueIncomingNetEventType(NetEventType.SERVER_CLOSED, ConnectionId())
            }
        }
        // Переводим статус сервера в OFFLINE
        serverStatus = WebSocketServerStatus.OFFLINE
        // Очищаем данные исходящих сообщений
        outgoingQueue.clear()
        // Очищаем веб сокет соединение
        webSocketCleanup()
        // Переводим статус веб сокет соединения в NOT_CONNECTED
        connectionStatus = WebSocketConnectionStatus.NOT_CONNECTED
    }

    private fun enqueueIncomingNetEventType(
        event: NetEventType,
        connectionId: ConnectionId
    ) {
        enqueueIncoming(
            NetEvent(
                netEventType = event,
                connectionId = connectionId
            )
        )
    }

    private fun enqueueOutgoing(evt: NetEvent) {
        outgoingQueue.addLast(evt)
    }

    private fun enqueueIncoming(evt: NetEvent) {
        incomingQueue.addLast(evt)
    }

    private fun tryRemoveConnecting(id: ConnectionId) {
        val index = connecting.indexOf(id.id)
        if (index != -1) {
            connecting.removeAt(index)
        }
    }

    private fun tryRemoveConnection(id: ConnectionId) {
        val index = connections.indexOf(id.id)
        if (index != -1) {
            connections.removeAt(index)
        }
    }

    private fun parseMessage(byteArray: ByteArray?) {
        // Если байтовый массив пустой или null останавливаем парсинг
        if (byteArray == null || byteArray.isEmpty()) {
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
            this.heartbeatReceived = true
            Log.i(TAG, "heartBeatReceived = $heartbeatReceived")
        } else {
            val event = byteArray.toNetworkEvent()
            Log.i(TAG, "event = $event")
        }
    }

    private fun handleIncomingEvent(evt: NetEvent) {
        if (evt.netEventType == NetEventType.NEW_CONNECTION) {
            //removing connecting info
            tryRemoveConnecting(evt.connectionId)
            //add connection
            connections.addLast(evt.connectionId.id)
        } else if (evt.netEventType == NetEventType.CONNECTION_FAILED) {
            //remove connecting info
            tryRemoveConnecting(evt.connectionId)
        } else if (evt.netEventType == NetEventType.DISCONNECTED) {
            //remove from connections
            tryRemoveConnection(evt.connectionId)
        } else if (evt.netEventType == NetEventType.SERVER_INITIALIZED) {
            serverStatus = WebSocketServerStatus.ONLINE
        } else if (evt.netEventType == NetEventType.SERVER_INIT_FAILED) {
            serverStatus = WebSocketServerStatus.OFFLINE
        } else if (evt.netEventType == NetEventType.SERVER_CLOSED) {
            serverStatus = WebSocketServerStatus.OFFLINE
        }

        enqueueIncoming(evt)
    }

    private fun handleOutgoingEvents() {
        while (outgoingQueue.size > 0) {
            val evt = this.outgoingQueue.firstOrNull()
            evt?.let {
                sendNetworkEvent(it)
            }
        }
    }

    private fun sendHeartbeat() {
        if (webSocketClient != null) {
            val byteArray = ByteArray(1)
            byteArray[0] = NetEventType.META_HEART_BEAT.value
            webSocketClient?.sendByteArray(byteArray)
        }
    }

    private fun sendVersion() {
        if (webSocketClient != null) {
            val byteArray = ByteArray(2)
            byteArray[0] = NetEventType.META_VERSION.value
            byteArray[1] = PROTOCOL_VERSION.toByte()
            webSocketClient?.sendByteArray(byteArray)
        }
    }

    private fun sendNetworkEvent(evt: NetEvent) {
        sendByteArray(evt.toByteArray())
    }

    private fun sendByteArray(data: ByteArray?) {
        if (webSocketClient != null) {
            webSocketClient?.sendByteArray(data)
        }
    }

    private fun nextConnectionId(): ConnectionId {
        val result = nextOutgoingConnectionId
        nextOutgoingConnectionId = ConnectionId((nextOutgoingConnectionId.id + 1).toShort())
        return result
    }

    private fun getRandomKey(): String {
        var result = ""
        for (i in 0 until 7) {
            val data = 65 + (Math.random() * 25).roundToInt()
            Char(data).toString()
            result += Char(data).toString()
        }
        return result
    }

    // interface implementation

    fun dequeue(): NetEvent? {
        return if (incomingQueue.size > 0) {
            incomingQueue.removeFirstOrNull()
        } else {
            null
        }
    }

    fun peek(): NetEvent? {
        return if (incomingQueue.size > 0) {
            incomingQueue.firstOrNull()
        } else {
            null
        }
    }

    fun update() {
        updateHeartbeat();
        checkSleep()
    }

    fun flush() {
        //ideally we buffer everything and then flush when it is connected as
        //websockets aren't suppose to be used for realtime communication anyway
        if (connectionStatus == WebSocketConnectionStatus.CONNECTED) {
            handleOutgoingEvents()
        }
    }

    fun sendData(id: ConnectionId?, data: ByteArray?, reliable: Boolean): Boolean {
        if (id == null || id.id == ConnectionId.INVALID) {
            // Ignored message. Invalid connection id.
            return false
        }
        if (data == null || data.isEmpty()) {
            return false
        }

        val evt: NetEvent = if (reliable) {
            NetEvent(
                netEventType = NetEventType.RELIABLE_MESSAGE_RECEIVED,
                connectionId = id,
                dataByteArray = data
            );
        } else {
            NetEvent(
                netEventType = NetEventType.UNRELIABLE_MESSAGE_RECEIVED,
                connectionId = id,
                dataByteArray = data
            );
        }

        enqueueOutgoing(evt)
        return true
    }

    fun disconnect(id: ConnectionId) {
        val evt = NetEvent(NetEventType.DISCONNECTED, id)
        enqueueOutgoing(evt)
    }

    fun shutdown() {
        cleanUp()
        connectionStatus = WebSocketConnectionStatus.NOT_CONNECTED
    }

    fun dispose() {
        if (!isDisposed) {
            shutdown()
            isDisposed = true
        }
    }

    fun startServer(address: String?) {
        var addressData = address
        if (addressData == null) {
            addressData = "" + getRandomKey()
        }

        if (serverStatus == WebSocketServerStatus.OFFLINE) {
            ensureServerConnection()
            serverStatus = WebSocketServerStatus.STARTING
            enqueueOutgoing(
                NetEvent(
                    netEventType = NetEventType.SERVER_INITIALIZED,
                    connectionId = ConnectionId(),
                    dataString = addressData
                )
            )
        } else {
            enqueueIncoming(
                NetEvent(
                    netEventType = NetEventType.SERVER_INIT_FAILED,
                    connectionId = ConnectionId(),
                    dataString = addressData
                )
            )
        }
    }

    fun stopServer() {
        enqueueOutgoing(
            NetEvent(
                netEventType = NetEventType.SERVER_CLOSED,
                connectionId = ConnectionId()
            )
        )
    }

    fun connect(address: String?): ConnectionId {
        ensureServerConnection()
        val newConId = nextConnectionId()
        connecting.addLast(newConId.id)
        val evt = NetEvent(
            netEventType = NetEventType.NEW_CONNECTION,
            connectionId = newConId,
            dataString = address
        )
        enqueueOutgoing(evt)
        return newConId
    }

    companion object {
        private const val TAG = "dds_SocketManager"
    }
}