package com.erdemtsynduev.socketmanager.websocket

import com.erdemtsynduev.socketmanager.model.netevent.ConnectionId

/**
 * Shared interface for WebRtcNetwork and UnityNetwork.
 *
 * Keep in mind that in the current version the network can only act as a server
 * (StartServer method) or as a client (via Connect method).
 */
interface IBasicNetwork : INetwork {

    /**
     * Starts a new server. After the server is started the Dequeue method will return a
     * ServerInitialized event with the address in the Info field.
     *
     * If the server fails to start it will return a ServerInitFailed event. If the
     * server is closed due to an error or the Shutdown method a ServerClosed event
     * will be triggered.
     */
    fun startServer(address: String?)

    fun stopServer()

    /**
     * Connects to a given address or roomname.
     *
     * This call will result in one of those 2 events in response:
     * NewConnection if the connection was established
     * ConnectionFailed if the connection failed.
     *
     * @param address A string that identifies the target.
     * Returns the Connection id the established connection will have (only supported by WebRtcNetwork).
     */
    fun connect(address: String?): ConnectionId
}