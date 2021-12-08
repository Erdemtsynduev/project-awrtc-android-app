package com.erdemtsynduev.awrtcandroid.utils

import com.erdemtsynduev.awrtcandroid.model.netevent.ConnectionId
import com.erdemtsynduev.awrtcandroid.model.netevent.NetEventDataType
import com.erdemtsynduev.awrtcandroid.model.netevent.NetEventType
import com.erdemtsynduev.awrtcandroid.model.netevent.NetworkEvent

/**
 * Конвертируем массив байтов в NetworkEvent
 */
fun ByteArray.toNetworkEvent(): NetworkEvent {
    // Получаем первый байт из массива байтов
    val byteType = this[0]
    // Конвертируем первый байт в тип NetEventType
    val type: NetEventType = NetEventType.from(byteType)
    // Получаем второй байт из массива байтов
    val byteDataType = this[1]
    // Конвертируем второй байт в тип NetEventDataType
    val dataType: NetEventDataType = NetEventDataType.from(byteDataType)

    val id: Short = -1

    val data: Any? = null
    when (dataType) {
        NetEventDataType.BYTE_ARRAY -> {
            // TODO Получаем данные по байтовому массиву
        }
        NetEventDataType.UTF16_STRING -> {
            // TODO Получаем данные по байтовому массиву
        }
        NetEventDataType.NULL -> {
            //message has no data
        }
        else -> {
            // Error
        }
    }

    val conId = ConnectionId(id)
    return NetworkEvent(type, conId, data)
}

/**
 * Конвертируем NetworkEvent в массив байт
 */
fun NetworkEvent.toByteArray(): ByteArray {
    val dataType: NetEventDataType
    var length = 4

    // Получение типа и длины
    when (this.data) {
        null -> {
            dataType = NetEventDataType.NULL
        }
        is String -> {
            dataType = NetEventDataType.UTF16_STRING
            val stringData = this.getInfo()
            stringData?.let {
                length += it.length * 2 + 4
            }
        }
        else -> {
            dataType = NetEventDataType.BYTE_ARRAY
            val byteArray = this.getMessageData()
            byteArray?.let {
                length += 4 + it.size
            }
        }
    }

    // Создание байтового массива
    val result = ByteArray(length)
    // Сохраняем данные по NetEventType и сохраняем в первый байт
    this.getNetEventType()?.value?.let {
        result[0] = it
    }
    // Сохраняем данные по NetEventDataType и сохраняем во второй байт
    result[1] = dataType.value

    if (dataType == NetEventDataType.BYTE_ARRAY) {
        // TODO Получаем данные по байтовому массиву
    } else if (dataType == NetEventDataType.UTF16_STRING) {
        // TODO Получаем данные по байтовому массиву
    }

    return result
}