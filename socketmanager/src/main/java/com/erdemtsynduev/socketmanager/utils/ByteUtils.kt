package com.erdemtsynduev.socketmanager.utils

object ByteUtils {

    /**
     * Записать 2 байтов (short значение) в байтовый массив с offset
     * @param buffer байтовый массив для записи данных
     * @param offset параметр с каким отступом записывать значения
     * @param data значение типа Short для записи
     */
    fun write2BytesToBuffer(buffer: ByteArray, offset: Int, data: Short) {
        buffer[offset + 0] = (data.toInt() and 0xff).toByte()
        buffer[offset + 1] = (data.toInt() and 0xff shl 8).toByte()
    }

    /**
     * Чтение 2 байтов из байтового массива и перевод в Int
     * @param buffer байтовый массив для чтения данных
     * @param offset параметр с каким отступом читать данные
     */
    fun read2BytesFromBuffer(buffer: ByteArray, offset: Int): Int {
        return (buffer[offset + 1].toInt() and 0xff shl 8) or
                (buffer[offset + 0].toInt() and 0xff)
    }

    /**
     * Записать 4 байтов (Int значение) в байтовый массив с offset
     * @param buffer байтовый массив для записи данных
     * @param offset параметр с каким отступом записывать значения
     * @param data значение типа Int для записи
     */
    fun write4BytesToBuffer(buffer: ByteArray, offset: Int, data: Int) {
        buffer[offset + 0] = (data shr 0).toByte()
        buffer[offset + 1] = (data shr 8).toByte()
        buffer[offset + 2] = (data shr 16).toByte()
        buffer[offset + 3] = (data shr 24).toByte()
    }

    /**
     * Чтение 4 байтов из байтового массива и перевод в Int
     * @param buffer байтовый массив для чтения данных
     * @param offset параметр с каким отступом читать данные
     */
    fun read4BytesFromBuffer(buffer: ByteArray, offset: Int): Int {
        return (buffer[offset + 3].toInt() shl 24) or
                (buffer[offset + 2].toInt() and 0xff shl 16) or
                (buffer[offset + 1].toInt() and 0xff shl 8) or
                (buffer[offset + 0].toInt() and 0xff)
    }
}