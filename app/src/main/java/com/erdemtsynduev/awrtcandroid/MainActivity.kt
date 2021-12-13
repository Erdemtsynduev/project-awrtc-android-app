package com.erdemtsynduev.awrtcandroid

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import com.erdemtsynduev.awrtcandroid.model.CandidateModel
import com.erdemtsynduev.awrtcandroid.model.OfferModel
import com.erdemtsynduev.rtcclient.AppSdpObserver
import com.erdemtsynduev.rtcclient.PeerConnectionObserver
import com.erdemtsynduev.rtcclient.RTCClient
import com.erdemtsynduev.socketmanager.SocketClientManager
import com.erdemtsynduev.socketmanager.model.netevent.ConnectionId
import com.erdemtsynduev.socketmanager.model.netevent.NetEvent
import com.erdemtsynduev.socketmanager.model.netevent.NetEventType
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.android.synthetic.main.activity_main.*
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.SessionDescription
import com.google.gson.JsonParser

import com.google.gson.JsonObject
import org.json.JSONObject
import java.util.HashMap

class MainActivity : AppCompatActivity() {

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 1
        private const val CAMERA_PERMISSION = Manifest.permission.CAMERA
    }

    private lateinit var rtcClient: RTCClient

    private val socketManager = SocketClientManager.instance("ws://192.168.1.191:12776")

    private var connectionId: ConnectionId? = null
    private var netEvent: NetEvent? = null

    private val sdpObserver = object : AppSdpObserver() {
        override fun onCreateSuccess(p0: SessionDescription?) {
            super.onCreateSuccess(p0)
            val mapData: MutableMap<String, String?> = HashMap<String, String?>()
            mapData["type"] = p0?.type?.canonicalForm()
            mapData["sdp"] = p0?.description
            val objectData = JSONObject(mapData as Map<*, *>?)
            val jsonString: String = objectData.toString()
            socketManager.sendData(
                id = connectionId,
                data = jsonString.toByteArray(Charsets.UTF_16LE),
                reliable = true
            )
        }
    }

    private val renderer = SyncedRenderer {
        socketManager.update()
        netEvent = socketManager.dequeue()
        checkNetEvent(netEvent)
        socketManager.flush()
    }

    private fun checkNetEvent(netEvent: NetEvent?) {
        if (netEvent != null) {
            Log.d("MainActivity", netEvent.toString())
            if (netEvent.netEventType == NetEventType.RELIABLE_MESSAGE_RECEIVED) {
                if (netEvent.connectionId == connectionId) {
                    netEvent.dataByteArray?.let {
                        val dataString = String(it, Charsets.UTF_16LE)
                        Log.d("MainActivity", dataString)
                        val dataInt = dataString.toIntOrNull()
                        if (dataInt != null && dataInt > 0) {
                            val newDataInt = dataInt - 1
                            socketManager.sendData(
                                id = connectionId,
                                data = newDataInt.toString().toByteArray(Charsets.UTF_16LE),
                                reliable = true
                            )
                        } else {
                            val jsonObject = JsonParser().parse(dataString) as JsonObject
                            val isHasType = jsonObject.has("type")
                            val gson = GsonBuilder().setPrettyPrinting().create()

                            if (isHasType) {
                                val typeString = jsonObject.get("type").asString
                                val sdpString = jsonObject.get("sdp").asString
                                if (typeString == SessionDescription.Type.OFFER.canonicalForm()) {
                                    val sessionDescription = SessionDescription(
                                        SessionDescription.Type.OFFER,
                                        sdpString
                                    )
                                    signallingClientListener.onOfferReceived(sessionDescription)
                                } else {
                                    val sessionDescription = SessionDescription(
                                        SessionDescription.Type.ANSWER,
                                        sdpString
                                    )
                                    signallingClientListener.onAnswerReceived(sessionDescription)
                                }
                            } else {
                                val data = gson.fromJson(dataString, CandidateModel::class.java)
                                val iceCandidate = data?.sdpMLineIndex?.let { it1 ->
                                    IceCandidate(
                                        data.sdpMid,
                                        it1, data.candidate
                                    )
                                }
                                iceCandidate?.let {
                                    signallingClientListener.onIceCandidateReceived(it)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        renderer.start()
        Handler().postDelayed({ test() }, 3000)
        checkCameraPermission()
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                CAMERA_PERMISSION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestCameraPermission()
        } else {
            onCameraPermissionGranted()
        }
    }

    private fun requestCameraPermission(dialogShown: Boolean = false) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                CAMERA_PERMISSION
            ) && !dialogShown
        ) {
            showPermissionRationaleDialog()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(CAMERA_PERMISSION),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun showPermissionRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle("Camera Permission Required")
            .setMessage("This app need the camera to function")
            .setPositiveButton("Grant") { dialog, _ ->
                dialog.dismiss()
                requestCameraPermission(true)
            }
            .setNegativeButton("Deny") { dialog, _ ->
                dialog.dismiss()
                onCameraPermissionDenied()
            }
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            onCameraPermissionGranted()
        } else {
            onCameraPermissionDenied()
        }
    }

    private fun onCameraPermissionDenied() {
        Toast.makeText(this, "Camera Permission Denied", Toast.LENGTH_LONG).show()
    }

    private fun onCameraPermissionGranted() {
        rtcClient = RTCClient(
            application,
            object : PeerConnectionObserver() {
                override fun onIceCandidate(p0: IceCandidate?) {
                    super.onIceCandidate(p0)
                    val data = CandidateModel(
                        sdpMLineIndex = p0?.sdpMLineIndex,
                        candidate = p0?.sdp,
                        sdpMid = p0?.sdpMid,
                    )
                    val gson = GsonBuilder().create()
                    val dataString = gson.toJson(data)
                    socketManager.sendData(
                        id = connectionId,
                        data = dataString.toByteArray(Charsets.UTF_16LE),
                        reliable = true
                    )
                    rtcClient.addIceCandidate(p0)
                }

                override fun onAddStream(p0: MediaStream?) {
                    super.onAddStream(p0)
                    p0?.videoTracks?.get(0)?.addSink(remote_view)
                }
            }
        )
        rtcClient.initSurfaceView(remote_view)
        call_button.setOnClickListener { rtcClient.call(sdpObserver) }
    }

    private val signallingClientListener = object : SignallingClientListener {
        override fun onConnectionEstablished() {
            call_button.isClickable = true
        }

        override fun onOfferReceived(description: SessionDescription) {
            rtcClient.onRemoteSessionReceived(description)
            rtcClient.answer(sdpObserver)
            remote_view_loading.isGone = true
        }

        override fun onAnswerReceived(description: SessionDescription) {
            rtcClient.onRemoteSessionReceived(description)
            remote_view_loading.isGone = true
        }

        override fun onIceCandidateReceived(iceCandidate: IceCandidate) {
            rtcClient.addIceCandidate(iceCandidate)
        }
    }

    fun test() {
        connectionId = socketManager.connect("QYNFQQD")
    }

    override fun onDestroy() {
        super.onDestroy()
        renderer.stop()
        socketManager.dispose()
    }
}
