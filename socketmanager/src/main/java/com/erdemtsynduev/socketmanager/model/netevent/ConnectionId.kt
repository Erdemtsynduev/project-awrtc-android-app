package com.erdemtsynduev.socketmanager.model.netevent

data class ConnectionId(var id: Short = INVALID) {
    companion object {
        const val INVALID: Short = -1
    }
}