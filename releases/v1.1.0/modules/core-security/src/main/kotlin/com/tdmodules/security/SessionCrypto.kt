package com.tdmodules.security

import java.nio.charset.StandardCharsets
import java.security.*
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Криптографический модуль сессии повышенной безопасности:
 * 1. Генерация временной пары RSA (2048 bit) для шифрования ответов клиентами (Web Crypto API / Native).
 * 2. Симметричное шифрование трафика AES-GCM-256 с уникальным одноразовым 12-байтным IV/Nonce для каждого пакета.
 * 3. Расшифровка входящих ответов клиентов через RSA-OAEP (SHA-256 + MGF1).
 * 4. Проверка и генерация HMAC-SHA256 подписей с защитой от тайминг-атак (MessageDigest.isEqual).
 */
class SessionCrypto {

    // 256-битный ключ сессии
    val sessionSecret: ByteArray = ByteArray(32).apply {
        SecureRandom().nextBytes(this)
    }

    private val keyPair: KeyPair

    init {
        val keyGen = KeyPairGenerator.getInstance("RSA")
        keyGen.initialize(2048, SecureRandom())
        keyPair = keyGen.generateKeyPair()
    }

    /**
     * Возвращает Base64-строку публичного ключа (SubjectPublicKeyInfo / SPKI),
     * передаваемую в URL hash клиента (#key=...) для Web Crypto API.
     */
    fun getPublicKeyBase64(): String {
        return Base64.getEncoder().encodeToString(keyPair.public.encoded)
    }

    /**
     * Расшифровка текста ответа, зашифрованного браузером/клиентом через RSA-OAEP (SHA-256).
     */
    fun decryptOaep(base64Ciphertext: String): String {
        return try {
            val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
            cipher.init(Cipher.DECRYPT_MODE, keyPair.private)
            val decodedBytes = Base64.getDecoder().decode(base64Ciphertext.trim())
            val decryptedBytes = cipher.doFinal(decodedBytes)
            String(decryptedBytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Шифрование данных с использованием AES-GCM-256 с генерацией уникального 12-байтного IV.
     * Формат результата: Base64(12-byte IV + Ciphertext with GCM auth tag).
     */
    fun encryptAesGcm(plainText: String): String {
        val iv = ByteArray(12).apply { SecureRandom().nextBytes(this) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(sessionSecret, "AES")
        val gcmSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)

        val cipherBytes = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
        val combined = ByteArray(iv.size + cipherBytes.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherBytes, 0, combined, iv.size, cipherBytes.size)

        return Base64.getEncoder().encodeToString(combined)
    }

    /**
     * Расшифровка пакета AES-GCM-256 с валидацией целостности и GCM-аутентификации.
     */
    fun decryptAesGcm(base64Payload: String): String {
        return try {
            val combined = Base64.getDecoder().decode(base64Payload.trim())
            if (combined.size < 12 + 16) return "" // IV (12) + GCM Tag (16)

            val iv = ByteArray(12)
            System.arraycopy(combined, 0, iv, 0, 12)
            val cipherBytes = ByteArray(combined.size - 12)
            System.arraycopy(combined, 12, cipherBytes, 0, cipherBytes.size)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val keySpec = SecretKeySpec(sessionSecret, "AES")
            val gcmSpec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)

            val decrypted = cipher.doFinal(cipherBytes)
            String(decrypted, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Генерация подписи HMAC-SHA256
     */
    fun signHmacSha256(data: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        val keySpec = SecretKeySpec(sessionSecret, "HmacSHA256")
        mac.init(keySpec)
        val hash = mac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
        return Base64.getEncoder().encodeToString(hash)
    }

    /**
     * Безопасная проверка подписи HMAC-SHA256 с защитой от атак по времени (Timing Attacks)
     */
    fun verifyHmacSha256(data: String, expectedSignatureBase64: String): Boolean {
        return try {
            val calculatedSignature = signHmacSha256(data)
            val a = Base64.getDecoder().decode(calculatedSignature)
            val b = Base64.getDecoder().decode(expectedSignatureBase64.trim())
            MessageDigest.isEqual(a, b)
        } catch (e: Exception) {
            false
        }
    }
}
