package com.erdemtsynduev.p2pwebrtc.engine

import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

interface EngineCallback {

    /**
     * Successfully joined the room
     */
    fun joinRoomSucc()

    /**
     * Exit the room successfully
     */
    fun exitRoom()

    fun onSendIceCandidate(userId: String?, candidate: IceCandidate?)

    fun onSendOffer(userId: String?, description: SessionDescription?)

    fun onSendAnswer(userId: String?, description: SessionDescription?)

    fun onRemoteStream(userId: String?)
}