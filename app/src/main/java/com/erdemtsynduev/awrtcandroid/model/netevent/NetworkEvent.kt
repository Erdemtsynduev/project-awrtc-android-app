package com.erdemtsynduev.awrtcandroid.model.netevent

class NetworkEvent(
    private var netEventType: NetEventType? = null,
    private var connectionId: ConnectionId = ConnectionId(),
    var data: Any? = null
) {

    fun getRawData(): Any? {
        return data
    }

    fun getMessageData(): ByteArray? {
        return data as? ByteArray
    }

    fun getInfo(): String? {
        return if (data is String?) {
            data as? String
        } else {
            null
        }
    }

    fun getNetEventType(): NetEventType? {
        return netEventType
    }

    fun getConnectionId(): ConnectionId {
        return connectionId
    }

    override fun toString(): String {
        return "NetworkEvent(netEventType=$netEventType, connectionId=$connectionId, data=$data)"
    }
}