package com.erdemtsynduev.awrtcandroid.model.netevent

class NetworkEvent(
    var netEventType: NetEventType? = null,
    var connectionId: ConnectionId = ConnectionId(),
    var dataString: String? = null,
    var dataByteArray: ByteArray? = null
)