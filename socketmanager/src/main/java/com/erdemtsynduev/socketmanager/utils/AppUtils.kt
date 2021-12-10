package com.erdemtsynduev.socketmanager.utils

import com.erdemtsynduev.socketmanager.model.netevent.NetEventType
import kotlin.math.roundToInt

object AppUtils {


    /**
     * Получить рандомный ключ для подключения
     */
    fun getRandomKey(): String {
        var result = ""
        for (i in 0 until 7) {
            val data = 65 + (Math.random() * 25).roundToInt()
            Char(data).toString()
            result += Char(data).toString()
        }
        return result
    }

    fun getHeartbeat(): ByteArray {
        val byteArray = ByteArray(1)
        byteArray[0] = NetEventType.META_HEART_BEAT.value
        return byteArray
    }

    fun getVersion(protocolVersion: Int): ByteArray {
        val byteArray = ByteArray(2)
        byteArray[0] = NetEventType.META_VERSION.value
        byteArray[1] = protocolVersion.toByte()
        return byteArray
    }
}