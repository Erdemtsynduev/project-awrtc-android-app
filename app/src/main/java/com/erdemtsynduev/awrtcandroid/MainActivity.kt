package com.erdemtsynduev.awrtcandroid

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.erdemtsynduev.socketmanager.websocket.SocketClientManager

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        test()
    }

    fun test() {
        val socketManager = SocketClientManager()
        socketManager.connect("ws://10.0.2.2:12776")
    }
}
