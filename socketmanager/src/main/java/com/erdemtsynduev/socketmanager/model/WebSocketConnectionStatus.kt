package com.erdemtsynduev.socketmanager.model

enum class WebSocketConnectionStatus {
    UNINITIALIZED,
    NOT_CONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING
}