package com.erdemtsynduev.awrtcandroid.utils

import com.erdemtsynduev.awrtcandroid.model.netevent.ConnectionId
import com.erdemtsynduev.awrtcandroid.model.netevent.NetEventDataType
import com.erdemtsynduev.awrtcandroid.model.netevent.NetEventType
import com.erdemtsynduev.awrtcandroid.model.netevent.NetworkEvent
import kotlin.experimental.and

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

    val dataString: String? = null
    val dataByteArray: ByteArray? = null
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
    return NetworkEvent(type, conId, dataString, dataByteArray)
}

/**
 * Конвертируем NetworkEvent в массив байт
 */
fun NetworkEvent.toByteArray(): ByteArray {
    val dataType: NetEventDataType
    var length = 4

    // Получение типа и длины
    if (this.dataString == null && this.dataByteArray == null) {
        dataType = NetEventDataType.NULL
    } else if (this.dataString != null) {
        dataType = NetEventDataType.UTF16_STRING
        val byteArray = this.dataString?.toByteArray(Charsets.UTF_16)
        byteArray?.let {
            length += 4 + it.size
        }
    } else if (this.dataByteArray != null) {
        dataType = NetEventDataType.BYTE_ARRAY
        val byteArray = this.dataByteArray
        byteArray?.let {
            length += 4 + it.size
        }
    } else {
        dataType = NetEventDataType.NULL
    }

    // Создание байтового массива
    val result = ByteArray(length)
    // Сохраняем данные по NetEventType и сохраняем в первый байт
    this.netEventType?.value?.let {
        result[0] = it
    }
    // Сохраняем данные по NetEventDataType и сохраняем во второй байт
    dataType.value.let {
        result[1] = it
    }

    this.connectionId.id.let {
        write2BytesToBuffer(result, 2, it)
    }

    // Определяем тип данных NetEventDataType
    if (dataType == NetEventDataType.BYTE_ARRAY) {
        this.dataByteArray?.size?.let {
            write4BytesToBuffer(result, 4, it)
        }
        // Получаем данные из массива элементов
        this.dataByteArray?.forEachIndexed { index, byte ->
            result[8 + index] = byte
        }
    } else if (dataType == NetEventDataType.UTF16_STRING) {
        this.dataString?.length?.let {
            write4BytesToBuffer(result, 4, it)
        }
        this.dataString?.toByteArray(Charsets.UTF_16)?.forEachIndexed { index, byte ->
            result[8 + index] = byte
        }
    }
    return result
}

private fun write2BytesToBuffer(buffer: ByteArray, offset: Int, value: Short) {
    buffer[offset + 0] = (value.toInt() and 0xff).toByte()
    buffer[offset + 1] = (value.toInt() and 0xff shl 8).toByte()
}

private fun write4BytesToBuffer(buffer: ByteArray, offset: Int, data: Int) {
    buffer[offset + 0] = (data shr 0).toByte()
    buffer[offset + 1] = (data shr 8).toByte()
    buffer[offset + 2] = (data shr 16).toByte()
    buffer[offset + 3] = (data shr 24).toByte()
}

private fun read4BytesFromBuffer(buffer: ByteArray, offset: Int): Int {
    return (buffer[offset + 3].toInt() shl 24) or
            (buffer[offset + 2].toInt() and 0xff shl 16) or
            (buffer[offset + 1].toInt() and 0xff shl 8) or
            (buffer[offset + 0].toInt() and 0xff)
}