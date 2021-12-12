package com.erdemtsynduev.p2pwebrtc.engine

import android.view.View

/**
 * rtc base class
 */
interface IEngine {

    /**
     * initialization
     */
    fun init(engineCallback: EngineCallback?)

    /**
     * Join room
     */
    fun joinRoom(userIds: List<String>)

    /**
     * Someone enters the room
     */
    fun userIn(userId: String)
    fun userReject(userId: String?)

    /**
     * receive Offer
     */
    fun receiveOffer(userId: String, description: String?)

    /**
     * receive Answer
     */
    fun receiveAnswer(userId: String, sdp: String?)

    /**
     * receive IceCandidate
     */
    fun receiveIceCandidate(userId: String?, id: String?, label: Int, candidate: String?)

    /**
     * Leave the room
     *
     * @param userId
     */
    fun leaveRoom(userId: String?)

    /**
     * Open local preview
     */
    fun startPreview(isOverlay: Boolean): View?

    /**
     * Close local preview
     */
    fun stopPreview()

    /**
     * Start remote streaming
     */
    fun startStream()

    /**
     * Stop remote streaming
     */
    fun stopStream()

    /**
     * Start remote preview
     */
    fun setupRemoteVideo(userId: String, isO: Boolean): View?

    /**
     * Turn off remote preview
     */
    fun stopRemoteVideo()

    /**
     * Switch camera
     */
    fun switchCamera()

    /**
     * Set mute
     */
    fun muteAudio(enable: Boolean): Boolean

    /**
     * Turn on the speakers
     */
    fun toggleSpeaker(enable: Boolean): Boolean

    /**
     * Release everything
     */
    fun release()
}