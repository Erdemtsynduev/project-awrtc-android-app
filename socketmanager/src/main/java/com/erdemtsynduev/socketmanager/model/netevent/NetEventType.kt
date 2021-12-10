package com.erdemtsynduev.socketmanager.model.netevent

enum class NetEventType(val value: Byte) {
    INVALID(0),
    UNRELIABLE_MESSAGE_RECEIVED(1),
    RELIABLE_MESSAGE_RECEIVED(2),

    /**
     * Подтверждение того, что сервер был запущен. другие люди смогут подключиться
     */
    SERVER_INITIALIZED(3),

    /**
     * Сервер не может быть запущен
     */
    SERVER_INIT_FAILED(4),

    /**
     * Сервер был закрыт. нет новых входящих подключений
     */
    SERVER_CLOSED(5),

    /**
     * Установлено новое входящее или исходящее соединение
     */
    NEW_CONNECTION(6),

    /**
     * Исходящее соединение не удалось
     */
    CONNECTION_FAILED(7),

    /**
     * Соединение было прервано
     */
    DISCONNECTED(8),

    /**
     * Еще не используется
     */
    FATAL_ERROR(100),

    /**
     * Еще не используется
     */
    WARNING(101),

    /**
     * Еще не используется
     */
    LOG(102),

    /**
     * Это значение и выше зарезервировано для других целей.
     * Никогда не должен попадать к пользователю и должен быть отфильтрован.
     */
    RESERVED_START(200.toByte()),

    /**
     * Зарезервированный
     * Используется протоколами, которые пересылают NetworkEvents
     */
    META_VERSION(201.toByte()),

    /**
     * Зарезервированный.
     * Используется протоколами, пересылающими NetworkEvents.
     */
    META_HEART_BEAT(202.toByte());

    companion object {
        fun from(type: Byte?): NetEventType = values().find { it.value == type } ?: INVALID
    }
}