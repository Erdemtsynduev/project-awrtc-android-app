package com.erdemtsynduev.p2pwebrtc.engine.peer

import android.content.Context
import android.util.Log
import com.erdemtsynduev.p2pwebrtc.engine.render.ProxyVideoSink
import org.webrtc.*
import org.webrtc.PeerConnection.*
import org.webrtc.RendererCommon.RendererEvents
import java.util.ArrayList

class Peer(
    private val peerConnectionFactory: PeerConnectionFactory?,
    private val iceServerList: List<IceServer>,
    private val userId: String?,
    private val iPeerEvent: IPeerEvent?
) :
    SdpObserver, Observer {
    private val pc: PeerConnection?
    private var queuedRemoteCandidates: MutableList<IceCandidate>?
    private var localSdp: SessionDescription? = null
    private var isOffer = false
    private var _remoteStream: MediaStream? = null
    var renderer: SurfaceViewRenderer? = null
    var sink: ProxyVideoSink? = null

    fun createPeerConnection(): PeerConnection? {
        val rtcConfig = RTCConfiguration(iceServerList)
        return peerConnectionFactory?.createPeerConnection(rtcConfig, this)
    }

    fun setOffer(isOffer: Boolean) {
        this.isOffer = isOffer
    }

    // Create offer
    fun createOffer() {
        if (pc == null) return
        Log.d("dds_test", "createOffer")
        pc.createOffer(this, offerOrAnswerConstraint())
    }

    // Create answer
    fun createAnswer() {
        if (pc == null) return
        Log.d("dds_test", "createAnswer")
        pc.createAnswer(this, offerOrAnswerConstraint())
    }

    // Set LocalDescription
    fun setLocalDescription(sdp: SessionDescription?) {
        Log.d("dds_test", "setLocalDescription")
        if (pc == null) return
        pc.setLocalDescription(this, sdp)
    }

    // Set RemoteDescription
    fun setRemoteDescription(sdp: SessionDescription?) {
        if (pc == null) return
        Log.d("dds_test", "setRemoteDescription")
        pc.setRemoteDescription(this, sdp)
    }

    // Add local stream
    fun addLocalStream(stream: MediaStream?) {
        if (pc == null) return
        Log.d("dds_test", "addLocalStream$userId")
        pc.addStream(stream)
    }

    // Add RemoteIceCandidate
    fun addRemoteIceCandidate(candidate: IceCandidate) {
        if (pc != null) {
            if (queuedRemoteCandidates != null) {
                queuedRemoteCandidates!!.add(candidate)
            } else {
                pc.addIceCandidate(candidate)
            }
        }
    }

    // Remove RemoteIceCandidates
    fun removeRemoteIceCandidates(candidates: Array<IceCandidate?>?) {
        if (pc == null) {
            return
        }
        drainCandidates()
        pc.removeIceCandidates(candidates)
    }

    fun createRender(mRootEglBase: EglBase?, context: Context?, isOverlay: Boolean) {
        renderer = SurfaceViewRenderer(context)
        renderer?.init(mRootEglBase?.eglBaseContext, object : RendererEvents {
            override fun onFirstFrameRendered() {
                // "createRender onFirstFrameRendered"
            }

            override fun onFrameResolutionChanged(
                videoWidth: Int,
                videoHeight: Int,
                rotation: Int
            ) {
                // "createRender onFrameResolutionChanged"
            }
        })
        renderer?.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
        renderer?.setMirror(true)
        renderer?.setZOrderMediaOverlay(isOverlay)
        sink = ProxyVideoSink()
        sink?.setTarget(renderer)
        if (_remoteStream != null && _remoteStream!!.videoTracks.size > 0) {
            _remoteStream!!.videoTracks[0].addSink(sink)
        }
    }

    // Close Peer
    fun close() {
        if (renderer != null) {
            renderer!!.release()
            renderer = null
        }
        if (sink != null) {
            sink?.setTarget(null)
        }
        if (pc != null) {
            pc.close()
            pc.dispose()
        }
    }

    //------------------------------Observer-------------------------------------
    override fun onSignalingChange(signalingState: SignalingState) {
    }

    override fun onIceConnectionChange(newState: IceConnectionState) {}
    override fun onIceConnectionReceivingChange(receiving: Boolean) {
    }

    override fun onIceGatheringChange(newState: IceGatheringState) {
    }

    override fun onIceCandidate(candidate: IceCandidate) {
        iPeerEvent?.onSendIceCandidate(userId, candidate)
    }

    override fun onIceCandidatesRemoved(candidates: Array<IceCandidate>) {
    }

    override fun onAddStream(stream: MediaStream) {
        stream.audioTracks[0].setEnabled(true)
        _remoteStream = stream
        iPeerEvent?.onRemoteStream(userId, stream)
    }

    override fun onRemoveStream(stream: MediaStream) {
        iPeerEvent?.onRemoveStream(userId, stream)
    }

    override fun onDataChannel(dataChannel: DataChannel) {
    }

    override fun onRenegotiationNeeded() {
    }

    override fun onAddTrack(receiver: RtpReceiver, mediaStreams: Array<MediaStream>) {
    }

    // SdpObserver
    override fun onCreateSuccess(origSdp: SessionDescription) {
        val sdpString = origSdp.description
        val sdp = SessionDescription(origSdp.type, sdpString)
        localSdp = sdp
        setLocalDescription(sdp)
    }

    override fun onSetSuccess() {
        // sender
        if (isOffer) {
            if (pc?.remoteDescription == null) {
                if (!isOffer) {
                    //Recipient, send Answer
                    iPeerEvent?.onSendAnswer(userId, localSdp)
                } else {
                    //Sender, send your own offer
                    iPeerEvent?.onSendOffer(userId, localSdp)
                }
            } else {
                // "Remote SDP set succesfully"
                drainCandidates()
            }
        } else {
            if (pc?.localDescription != null) {
                if (!isOffer) {
                    //Recipient, send Answer
                    iPeerEvent?.onSendAnswer(userId, localSdp)
                } else {
                    //Sender, send your own offer
                    iPeerEvent?.onSendOffer(userId, localSdp)
                }
                drainCandidates()
            } else {
                // "Remote SDP set succesfully"
            }
        }
    }

    override fun onCreateFailure(error: String) {
    }

    override fun onSetFailure(error: String) {
    }

    private fun drainCandidates() {
        if (queuedRemoteCandidates != null) {
            for (candidate in queuedRemoteCandidates!!) {
                pc?.addIceCandidate(candidate)
            }
            queuedRemoteCandidates = null
        }
    }

    private fun offerOrAnswerConstraint(): MediaConstraints {
        val mediaConstraints = MediaConstraints()
        val keyValuePairs = ArrayList<MediaConstraints.KeyValuePair>()
        keyValuePairs.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        keyValuePairs.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        mediaConstraints.mandatory.addAll(keyValuePairs)
        return mediaConstraints
    }

    init {
        queuedRemoteCandidates = ArrayList()
        pc = createPeerConnection()
    }
}