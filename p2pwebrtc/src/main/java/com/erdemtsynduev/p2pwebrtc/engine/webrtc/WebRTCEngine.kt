package com.erdemtsynduev.p2pwebrtc.engine.webrtc

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.projection.MediaProjection
import android.text.TextUtils
import android.view.View
import com.erdemtsynduev.p2pwebrtc.engine.EngineCallback
import com.erdemtsynduev.p2pwebrtc.engine.IEngine
import com.erdemtsynduev.p2pwebrtc.engine.peer.IPeerEvent
import com.erdemtsynduev.p2pwebrtc.engine.peer.Peer
import com.erdemtsynduev.p2pwebrtc.engine.render.ProxyVideoSink
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera1Enumerator
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraEnumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RendererCommon
import org.webrtc.ScreenCapturerAndroid
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoCapturer
import org.webrtc.VideoDecoderFactory
import org.webrtc.VideoEncoderFactory
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import org.webrtc.audio.AudioDeviceModule
import org.webrtc.audio.JavaAudioDeviceModule
import java.lang.Exception
import java.util.ArrayList
import java.util.concurrent.ConcurrentHashMap

class WebRTCEngine(
    var isAudioOnly: Boolean,
    private val context: Context
) : IEngine, IPeerEvent {

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var rootEglBase: EglBase? = null
    private var localMediaStream: MediaStream? = null
    private var videoSource: VideoSource? = null
    private var audioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null
    private var videoCaptureAndroid: VideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var localProxyVideoSink: ProxyVideoSink? = null
    private var localSurfaceViewRenderer: SurfaceViewRenderer? = null
    private val peerMap: ConcurrentHashMap<String, Peer> = ConcurrentHashMap<String, Peer>()
    private val iceServerList: MutableList<PeerConnection.IceServer> =
        ArrayList<PeerConnection.IceServer>()
    private var engineCallback: EngineCallback? = null
    private val audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    override fun init(engineCallback: EngineCallback?) {
        this.engineCallback = engineCallback
        if (rootEglBase == null) {
            rootEglBase = EglBase.create()
        }
        if (peerConnectionFactory == null) {
            peerConnectionFactory = createConnectionFactory()
        }
        if (localMediaStream == null) {
            createLocalStream()
        }
    }

    override fun joinRoom(userIds: List<String>) {
        for (id in userIds) {
            // create Peer
            val peer = Peer(peerConnectionFactory, iceServerList, id, this)
            peer.setOffer(false)
            // add localStream
            peer.addLocalStream(localMediaStream)
            // add list peer
            peerMap[id] = peer
        }
        if (engineCallback != null) {
            engineCallback?.joinRoomSucc()
        }
        audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
    }

    override fun userIn(userId: String) {
        // create Peer
        val peer = Peer(peerConnectionFactory, iceServerList, userId, this)
        peer.setOffer(true)
        // add localStream
        peer.addLocalStream(localMediaStream)
        // add peer list
        peerMap[userId] = peer
        // createOffer
        peer.createOffer()
    }

    override fun userReject(userId: String?) {}

    override fun receiveOffer(userId: String, description: String?) {
        peerMap[userId]?.let { peer ->
            val sdp = SessionDescription(SessionDescription.Type.OFFER, description)
            peer.setOffer(false)
            peer.setRemoteDescription(sdp)
            peer.createAnswer()
        }
    }

    override fun receiveAnswer(userId: String, sdp: String?) {
        peerMap[userId]?.let { peer ->
            val sessionDescription = SessionDescription(SessionDescription.Type.ANSWER, sdp)
            peer.setRemoteDescription(sessionDescription)
        }
    }

    override fun receiveIceCandidate(userId: String?, id: String?, label: Int, candidate: String?) {
        peerMap[userId]?.let { peer ->
            val iceCandidate = IceCandidate(id, label, candidate)
            peer.addRemoteIceCandidate(iceCandidate)
        }
    }

    override fun leaveRoom(userId: String?) {
        peerMap[userId]?.let { peer ->
            peer.close()
            peerMap.remove(userId)
        }
        if (peerMap.size == 0) {
            engineCallback?.let {
                it.exitRoom()
            }
        }
    }

    override fun startPreview(isOverlay: Boolean): View? {
        localSurfaceViewRenderer = SurfaceViewRenderer(context)
        localSurfaceViewRenderer?.init(rootEglBase?.eglBaseContext, null)
        localSurfaceViewRenderer?.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_BALANCED)
        localSurfaceViewRenderer?.setMirror(false)
        localSurfaceViewRenderer?.setZOrderMediaOverlay(isOverlay)
        localSurfaceViewRenderer?.setEnableHardwareScaler(true)
        localProxyVideoSink = ProxyVideoSink()
        localProxyVideoSink?.setTarget(localSurfaceViewRenderer)
        localMediaStream?.videoTracks?.let {
            if (it.size > 0) {
                localMediaStream?.videoTracks?.get(0)?.addSink(localProxyVideoSink)
            }
        }

        return localSurfaceViewRenderer
    }

    override fun stopPreview() {
        if (localProxyVideoSink != null) {
            localProxyVideoSink?.setTarget(null)
            localProxyVideoSink = null
        }
        if (audioSource != null) {
            audioSource?.dispose()
            audioSource = null
        }
        // Release the camera
        if (videoCaptureAndroid != null) {
            try {
                videoCaptureAndroid?.stopCapture()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            videoCaptureAndroid?.dispose()
            videoCaptureAndroid = null
        }
        // Release the canvas
        if (surfaceTextureHelper != null) {
            surfaceTextureHelper?.dispose()
            surfaceTextureHelper = null
        }
        if (videoSource != null) {
            videoSource?.dispose()
            videoSource = null
        }
        if (localMediaStream != null) {
            localMediaStream = null
        }
        if (localSurfaceViewRenderer != null) {
            localSurfaceViewRenderer?.release()
        }
    }

    override fun startStream() {}
    override fun stopStream() {}
    override fun setupRemoteVideo(userId: String, isO: Boolean): View? {
        if (TextUtils.isEmpty(userId)) {
            return null
        }
        val peer: Peer = peerMap[userId] ?: return null
        if (peer.renderer == null) {
            peer.createRender(rootEglBase, context, isO)
        }
        return peer.renderer
    }

    override fun stopRemoteVideo() {}
    private var isSwitch = false // Are you switching cameras
    override fun switchCamera() {
        if (isSwitch) return
        isSwitch = true
        if (videoCaptureAndroid == null) return
        if (videoCaptureAndroid is CameraVideoCapturer) {
            val cameraVideoCapturer: CameraVideoCapturer =
                videoCaptureAndroid as CameraVideoCapturer
            try {
                cameraVideoCapturer.switchCamera(object : CameraVideoCapturer.CameraSwitchHandler {
                    override fun onCameraSwitchDone(isFrontCamera: Boolean) {
                        isSwitch = false
                    }

                    override fun onCameraSwitchError(errorDescription: String?) {
                        isSwitch = false
                    }
                })
            } catch (e: Exception) {
                isSwitch = false
            }
        } else {
            // "Will not switch camera, video caputurer is not a camera"
        }
    }

    override fun muteAudio(enable: Boolean): Boolean {
        if (localAudioTrack != null) {
            localAudioTrack?.setEnabled(false)
            return true
        }
        return false
    }

    override fun toggleSpeaker(enable: Boolean): Boolean {
        if (enable) {
            audioManager.isSpeakerphoneOn = true
            audioManager.setStreamVolume(
                AudioManager.STREAM_VOICE_CALL,
                audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL),
                AudioManager.STREAM_VOICE_CALL
            )
        } else {
            audioManager.isSpeakerphoneOn = false
            audioManager.setStreamVolume(
                AudioManager.STREAM_VOICE_CALL,
                audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL),
                AudioManager.STREAM_VOICE_CALL
            )
        }
        return true
    }

    override fun release() {
        audioManager.mode = AudioManager.MODE_NORMAL
        // Clear peer
        for (peer in peerMap.values) {
            peer.close()
        }
        peerMap.clear()


        // Stop preview
        stopPreview()
        if (peerConnectionFactory != null) {
            peerConnectionFactory?.dispose()
            peerConnectionFactory = null
        }
        if (rootEglBase != null) {
            rootEglBase?.release()
            rootEglBase = null
        }
    }

    // Other methods
    private fun initIceServer() {
        // Initialize some stun and turn addresses
        val var1: PeerConnection.IceServer =
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        iceServerList.add(var1)
    }

    /**
     * Construct PeerConnectionFactory
     *
     * @return PeerConnectionFactory
     */
    private fun createConnectionFactory(): PeerConnectionFactory {

        // 1. The initialization method must be called before starting
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions
                .builder(context)
                .createInitializationOptions()
        )

        // 2. Set codec method: default method
        val encoderFactory: VideoEncoderFactory
        val decoderFactory: VideoDecoderFactory
        encoderFactory = DefaultVideoEncoderFactory(
            rootEglBase?.eglBaseContext,
            true,
            true
        )
        decoderFactory = DefaultVideoDecoderFactory(rootEglBase?.eglBaseContext)

        // Construct Factory
        val audioDeviceModule: AudioDeviceModule =
            JavaAudioDeviceModule.builder(context).createAudioDeviceModule()
        val options: PeerConnectionFactory.Options = PeerConnectionFactory.Options()
        return PeerConnectionFactory.builder()
            .setOptions(options)
            .setAudioDeviceModule(audioDeviceModule)
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()
    }

    /**
     * Create a local stream
     */
    private fun createLocalStream() {
        localMediaStream = peerConnectionFactory?.createLocalMediaStream("ARDAMS")
        // Audio
        audioSource = peerConnectionFactory?.createAudioSource(createAudioConstraints())
        localAudioTrack = peerConnectionFactory?.createAudioTrack(AUDIO_TRACK_ID, audioSource)
        localMediaStream?.addTrack(localAudioTrack)

        // video
        if (!isAudioOnly) {
            videoCaptureAndroid = createVideoCapture()
            surfaceTextureHelper =
                SurfaceTextureHelper.create("CaptureThread", rootEglBase?.eglBaseContext)
            videoSource =
                videoCaptureAndroid?.isScreencast?.let { peerConnectionFactory?.createVideoSource(it) }
            videoCaptureAndroid?.initialize(
                surfaceTextureHelper,
                context,
                videoSource?.capturerObserver
            )
            videoCaptureAndroid?.startCapture(VIDEO_RESOLUTION_WIDTH, VIDEO_RESOLUTION_HEIGHT, FPS)
            val localVideoTrack: VideoTrack? =
                peerConnectionFactory?.createVideoTrack(VIDEO_TRACK_ID, videoSource)
            localMediaStream?.addTrack(localVideoTrack)
        }
    }

    // Whether to use screen recording
    private val screenCaptureEnabled = false

    /**
     * Create media method
     *
     * @return VideoCapturer
     */
    private fun createVideoCapture(): VideoCapturer? {
        if (screenCaptureEnabled) {
            return createScreenCapture()
        }
        val videoCapturer = if (Camera2Enumerator.isSupported(context)) {
            createCameraCapture(Camera2Enumerator(context))
        } else {
            createCameraCapture(Camera1Enumerator(true))
        }
        return videoCapturer
    }

    /**
     * Create camera media stream
     *
     * @param enumerator
     * @return VideoCapturer
     */
    private fun createCameraCapture(enumerator: CameraEnumerator): VideoCapturer? {
        val deviceNames: Array<String> = enumerator.deviceNames

        // First, try to find front facing camera
        for (deviceName in deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                return enumerator.createCapturer(deviceName, null)
            }
        }

        // Front facing camera not found, try something else
        for (deviceName in deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                return enumerator.createCapturer(deviceName, null)
            }
        }
        return null
    }

    @TargetApi(21)
    private fun createScreenCapture(): VideoCapturer? {
        return if (mediaProjectionPermissionResultCode != Activity.RESULT_OK) {
            null
        } else ScreenCapturerAndroid(
            mediaProjectionPermissionResultData, object : MediaProjection.Callback() {
                override fun onStop() {
                    // "User revoked permission to capture the screen."
                }
            })
    }

    // Configure audio parameters
    private fun createAudioConstraints(): MediaConstraints {
        val audioConstraints = MediaConstraints()
        audioConstraints.mandatory.add(
            MediaConstraints.KeyValuePair(AUDIO_ECHO_CANCELLATION_CONSTRAINT, "true")
        )
        audioConstraints.mandatory.add(
            MediaConstraints.KeyValuePair(AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT, "true")
        )
        audioConstraints.mandatory.add(
            MediaConstraints.KeyValuePair(AUDIO_HIGH_PASS_FILTER_CONSTRAINT, "true")
        )
        audioConstraints.mandatory.add(
            MediaConstraints.KeyValuePair(AUDIO_NOISE_SUPPRESSION_CONSTRAINT, "true")
        )
        return audioConstraints
    }

    // Callback
    override fun onSendIceCandidate(userId: String?, candidate: IceCandidate?) {
        if (engineCallback != null) {
            engineCallback?.onSendIceCandidate(userId, candidate)
        }
    }

    override fun onSendOffer(userId: String?, description: SessionDescription?) {
        if (engineCallback != null) {
            engineCallback?.onSendOffer(userId, description)
        }
    }

    override fun onSendAnswer(userId: String?, description: SessionDescription?) {
        if (engineCallback != null) {
            engineCallback?.onSendAnswer(userId, description)
        }
    }

    override fun onRemoteStream(userId: String?, stream: MediaStream?) {
        if (engineCallback != null) {
            engineCallback?.onRemoteStream(userId)
        }
    }

    override fun onRemoveStream(userId: String?, stream: MediaStream?) {
        leaveRoom(userId)
    }

    companion object {
        private const val VIDEO_TRACK_ID = "ARDAMSv0"
        private const val AUDIO_TRACK_ID = "ARDAMSa0"
        private const val VIDEO_RESOLUTION_WIDTH = 1280
        private const val VIDEO_RESOLUTION_HEIGHT = 720
        private const val FPS = 30
        private val mediaProjectionPermissionResultData: Intent? = null
        private const val mediaProjectionPermissionResultCode = 0

        // Various restrictions
        private const val AUDIO_ECHO_CANCELLATION_CONSTRAINT = "googEchoCancellation"
        private const val AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT = "googAutoGainControl"
        private const val AUDIO_HIGH_PASS_FILTER_CONSTRAINT = "googHighpassFilter"
        private const val AUDIO_NOISE_SUPPRESSION_CONSTRAINT = "googNoiseSuppression"
    }

    init {
        // Initialize icy server
        initIceServer()
    }
}