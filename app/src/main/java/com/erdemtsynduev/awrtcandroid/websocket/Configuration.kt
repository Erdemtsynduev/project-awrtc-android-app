package com.erdemtsynduev.awrtcandroid.websocket

class Configuration {

    var heartbeat: Int = 30
    var locked = false

    fun heartbeat(value: Int): Boolean {
        return if (this.locked) {
            true
        } else {
            this.heartbeat = value
            false
        }
    }

    fun lock() {
        this.locked = true
    }
}