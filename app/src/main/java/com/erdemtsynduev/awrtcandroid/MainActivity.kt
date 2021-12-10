package com.erdemtsynduev.awrtcandroid

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.erdemtsynduev.socketmanager.SocketClientManager

class MainActivity : AppCompatActivity() {

    private val socketManager = SocketClientManager.instance("ws://10.0.2.2:12776")

    val renderer = SyncedRenderer { _: Long ->
        socketManager.update()
        socketManager.flush()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        renderer.start()
        test()
    }

    fun test() {
        socketManager.connect("QYNFQQD")
    }

    override fun onDestroy() {
        super.onDestroy()
        renderer.stop()
        socketManager.dispose()
    }
}
