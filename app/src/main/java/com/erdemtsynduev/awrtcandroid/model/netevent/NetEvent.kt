package com.erdemtsynduev.awrtcandroid.model.netevent

class NetEvent(
    var netEventType: NetEventType? = null,
    var connectionId: ConnectionId = ConnectionId(),
    var dataString: String? = null,
    var dataByteArray: ByteArray? = null
) {

    override fun toString(): String {
        return "NetworkEvent(netEventType=$netEventType, connectionId=$connectionId, dataString=$dataString, dataByteArray=${dataByteArray?.contentToString()})"
    }
}