package com.erdemtsynduev.socketmanager.model.netevent

enum class NetEventDataType(val value: Byte) {
    NULL(0),

    /**
     * Начальная длина 32 бита + байтовый массив
     */
    BYTE_ARRAY(1),

    /**
     * Начальная длина 32 бита (в блоках utf16) + UTF 16
     */
    UTF16_STRING(2);

    companion object {
        fun from(type: Byte?): NetEventDataType = values().find { it.value == type } ?: NULL
    }
}