package com.tdmodules.security

import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * Модуль защиты строк и секретных конфигураций от статического анализа и декомпиляторов (jadx, ghidra, apktool).
 * Применяет многослойное XOR-маскирование с ротационным ключом и Base64-упаковку.
 * Препятствует поиску эндпоинтов, API-ключей и внутренних сигнатур через утилиту `strings`.
 */
object SecureStringMask {

    // Псевдослучайная маска для обфускации
    private const val DEFAULT_XOR_KEY: Byte = 0x5A.toByte()

    /**
     * Маскирует исходную строку (используется для генерации защищенных констант)
     */
    fun mask(plainText: String, key: Byte = DEFAULT_XOR_KEY): String {
        val bytes = plainText.toByteArray(StandardCharsets.UTF_8)
        val masked = ByteArray(bytes.size)
        for (i in bytes.indices) {
            val dynamicKey = (key.toInt() xor (i % 37)).toByte()
            masked[i] = (bytes[i].toInt() xor dynamicKey.toInt()).toByte()
        }
        return Base64.getEncoder().encodeToString(masked)
    }

    /**
     * Снимает маску в оперативной памяти во время выполнения (Just-in-Time Demasking)
     */
    fun demask(maskedBase64: String, key: Byte = DEFAULT_XOR_KEY): String {
        return try {
            val decoded = Base64.getDecoder().decode(maskedBase64.trim())
            val original = ByteArray(decoded.size)
            for (i in decoded.indices) {
                val dynamicKey = (key.toInt() xor (i % 37)).toByte()
                original[i] = (decoded[i].toInt() xor dynamicKey.toInt()).toByte()
            }
            String(original, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Одноразовый маскинг для временных секретов
     */
    fun obscure(value: String): String = mask(value)

    /**
     * Восстановление скрытой строки
     */
    fun reveal(obscuredValue: String): String = demask(obscuredValue)
}
