package com.tdmodules.security

import org.junit.Assert.*
import org.junit.Test

class SecurityModuleTest {

    @Test
    fun testAesGcmEncryptionDecryption() {
        val crypto = SessionCrypto()
        val originalText = "TopSecretMedicalTerm:MyocardialInfarction#2026"

        val encrypted = crypto.encryptAesGcm(originalText)
        assertNotNull(encrypted)
        assertNotEquals(originalText, encrypted)

        val decrypted = crypto.decryptAesGcm(encrypted)
        assertEquals(originalText, decrypted)
    }

    @Test
    fun testHmacVerification() {
        val crypto = SessionCrypto()
        val payload = "student_id=42&action=submit_quiz&score=100"

        val signature = crypto.signHmacSha256(payload)
        assertTrue(crypto.verifyHmacSha256(payload, signature))

        val tamperedPayload = "student_id=42&action=submit_quiz&score=50"
        assertFalse(crypto.verifyHmacSha256(tamperedPayload, signature))
    }

    @Test
    fun testSecureStringMask() {
        val secretUrl = "https://internal.kepler.ai.cloud.ovh.net/v1"
        val masked = SecureStringMask.mask(secretUrl)

        assertFalse(masked.contains("kepler"))
        val revealed = SecureStringMask.demask(masked)
        assertEquals(secretUrl, revealed)
    }

    @Test
    fun testRateLimiterThreshold() {
        val limiter = RateLimiter(maxRequestsPerSecond = 5)
        val ip = "192.168.1.100"

        // Первые 5 запросов должны пройти
        for (i in 1..5) {
            assertTrue("Request $i should be allowed", limiter.isAllowed(ip))
        }

        // 6-й запрос должен быть отклонен
        assertFalse("Request 6 should be rejected", limiter.isAllowed(ip))

        // Другой IP должен спокойно проходить
        val otherIp = "192.168.1.101"
        assertTrue(limiter.isAllowed(otherIp))
    }
}
