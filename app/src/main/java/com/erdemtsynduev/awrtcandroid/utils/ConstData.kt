package com.erdemtsynduev.awrtcandroid.utils

object ConstData {

    /**
     * Версия протокола, реализованная здесь
     */
    var PROTOCOL_VERSION: Int = 2

    /**
     * Минимальная версия протокола, которая все еще поддерживается.
     * Серверы V 1 не распознают сердцебиение и версию
     * сообщения, но просто регистрирует неизвестное сообщение и
     * продолжайте нормально.
     */
    var PROTOCOL_VERSION_MIN: Int = 1
}