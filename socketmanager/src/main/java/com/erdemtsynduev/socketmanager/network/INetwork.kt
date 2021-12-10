package com.erdemtsynduev.socketmanager.network

import com.erdemtsynduev.socketmanager.model.netevent.ConnectionId
import com.erdemtsynduev.socketmanager.model.netevent.NetEvent

/**
 * Interface to a network that doesn't enforce storing any states.
 * Anything more is reusable between multiple different networks.
 */
interface INetwork {

     /**
     * This will return the incoming network events. Call this method and handle
     * the incommen events until it returns false.
     *
     * Returns true if the parameter evt contains a new event. False if there are no events to process left
     */
    fun dequeue(): NetEvent?

    fun peek(): NetEvent?

    /**
     * Sends buffered data.
     * Might also clear all unused events from the queue!
     */
    fun flush()

    /**
     * Sends the content if a byte array to the given connection.
     * @param id The id of the recipient
     * @param data Byte array containing the data to send
     * @param reliable True to send a reliable message(tcp style) and false to send unreliable (udp style)
     */
    fun sendData(id: ConnectionId?, data: ByteArray?, reliable: Boolean?): Boolean

    /**
     * Disconnects the given connection
     * @param id Id of the connection to disconnect.
     */
    fun disconnect(id: ConnectionId)

    /**
     * Disconnects all connection and shutsdown the server if started.
     * Dequeue will still return the confirmation messages such
     * as Disconnected event for each connection.
     */
    fun shutdown()

    /**
     * Call this every frame if you intend to read incoming messages using Dequeue. This will make
     * sure all data is read received by the network.
     */
    fun update()

    fun dispose()
}