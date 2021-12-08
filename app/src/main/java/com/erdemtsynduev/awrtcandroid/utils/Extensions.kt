package com.erdemtsynduev.awrtcandroid.utils

import com.erdemtsynduev.awrtcandroid.model.netevent.ConnectionId
import com.erdemtsynduev.awrtcandroid.model.netevent.NetEventDataType
import com.erdemtsynduev.awrtcandroid.model.netevent.NetEventType
import com.erdemtsynduev.awrtcandroid.model.netevent.NetEvent

/**
 * Конвертируем массив байтов в NetworkEvent
 */
fun ByteArray.toNetworkEvent(): NetEvent {
    // Получаем первый байт из массива байтов
    val byteType = this[0]
    // Конвертируем первый байт в тип NetEventType
    val type: NetEventType = NetEventType.from(byteType)
    // Получаем второй байт из массива байтов
    val byteDataType = this[1]
    // Конвертируем второй байт в тип NetEventDataType
    val dataType: NetEventDataType = NetEventDataType.from(byteDataType)

    // Получаем значение по id, считывая третий и четвертый байт
    val id: Short = ByteUtils.read2BytesFromBuffer(this, 2).toShort()

    var dataString: String? = null
    var dataByteArray: ByteArray? = null
    when (dataType) {
        NetEventDataType.BYTE_ARRAY -> {
            val newByteArray = this.drop(7)
            dataByteArray = newByteArray.toByteArray()
        }
        NetEventDataType.UTF16_STRING -> {
            val newByteArray = this.drop(7)
            dataString = String(newByteArray.toByteArray(), Charsets.UTF_16)
        }
        NetEventDataType.NULL -> {
            // Данных нет
        }
    }

    val conId = ConnectionId(id)
    return NetEvent(type, conId, dataString, dataByteArray)
}

/**
 * Конвертируем NetworkEvent в массив байт
 */
fun NetEvent.toByteArray(): ByteArray {
    val dataType: NetEventDataType
    var length = 4

    // Получение типа и длины
    if (this.dataString == null && this.dataByteArray == null) {
        dataType = NetEventDataType.NULL
    } else if (this.dataString != null) {
        dataType = NetEventDataType.UTF16_STRING
        val byteArray = this.dataString?.toByteArray(Charsets.UTF_16LE)
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
        ByteUtils.write2BytesToBuffer(result, 2, it)
    }

    // Определяем тип данных NetEventDataType
    if (dataType == NetEventDataType.BYTE_ARRAY) {
        this.dataByteArray?.size?.let {
            ByteUtils.write4BytesToBuffer(result, 4, it)
        }
        // Получаем данные из массива элементов
        this.dataByteArray?.forEachIndexed { index, byte ->
            result[8 + index] = byte
        }
    } else if (dataType == NetEventDataType.UTF16_STRING) {
        this.dataString?.length?.let {
            ByteUtils.write4BytesToBuffer(result, 4, it)
        }
        this.dataString?.toByteArray(Charsets.UTF_16LE)?.forEachIndexed { index, byte ->
            result[8 + index] = byte
        }
    }
    return result
}