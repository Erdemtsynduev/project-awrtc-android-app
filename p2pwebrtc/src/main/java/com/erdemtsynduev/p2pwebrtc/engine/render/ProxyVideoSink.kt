package com.erdemtsynduev.p2pwebrtc.engine.render

import org.webrtc.Logging
import org.webrtc.VideoFrame
import org.webrtc.VideoSink

class ProxyVideoSink : VideoSink {

    private var target: VideoSink? = null

    @Synchronized
    override fun onFrame(frame: VideoFrame) {
        if (target == null) {
            Logging.d(TAG, "Dropping frame in proxy because target is null.")
            return
        }
        target?.onFrame(frame)
    }

    @Synchronized
    fun setTarget(target: VideoSink?) {
        this.target = target
    }

    companion object {
        private const val TAG = "ProxyVideoSink"
    }
}