package com.erdemtsynduev.socketmanager.utils

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
}