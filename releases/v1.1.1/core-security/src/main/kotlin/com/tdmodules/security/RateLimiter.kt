package com.tdmodules.security

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Потокобезопасный ограничитель частоты запросов (Rate Limiter).
 * Защищает локальные серверы (Ktor / NanoHTTPD / Sockets) от DoS-атак,
 * подбора ответов и перегрузки мобильного устройства.
 *
 * Поддерживает:
 * - Окно ограничения в секундах
 * - Настраиваемый порог количества запросов
 * - Автоматическую очистку устаревших IP-адресов/клиентов (защита от утечки памяти OOM)
 */
class RateLimiter(
    val maxRequestsPerSecond: Int = 10,
    private val maxTrackedEntries: Int = 2000
) {

    // IP/ClientId -> Pair(timestampSec, requestCount)
    private val records = ConcurrentHashMap<String, Pair<Long, AtomicInteger>>()

    /**
     * Проверяет, разрешен ли запрос от данного идентификатора (IP или ClientToken).
     * @return true, если лимит не превышен; false, если превышен (HTTP 429 Too Many Requests).
     */
    fun isAllowed(clientId: String): Boolean {
        val currentSec = System.currentTimeMillis() / 1000

        // Периодическая самоочистка от старых записей при превышении лимита хранилища
        if (records.size > maxTrackedEntries) {
            evictStaleEntries(currentSec)
        }

        val record = records.compute(clientId) { _, existing ->
            if (existing == null || existing.first != currentSec) {
                Pair(currentSec, AtomicInteger(1))
            } else {
                existing.second.incrementAndGet()
                existing
            }
        }

        return (record?.second?.get() ?: 0) <= maxRequestsPerSecond
    }

    /**
     * Возвращает текущее количество запросов за текущую секунду
     */
    fun getCurrentCount(clientId: String): Int {
        val currentSec = System.currentTimeMillis() / 1000
        val record = records[clientId] ?: return 0
        return if (record.first == currentSec) record.second.get() else 0
    }

    /**
     * Сброс истории для клиента (например, после успешной капчи или разблокировки)
     */
    fun reset(clientId: String) {
        records.remove(clientId)
    }

    /**
     * Полная очистка
     */
    fun clear() {
        records.clear()
    }

    private fun evictStaleEntries(currentSec: Long) {
        records.entries.removeIf { it.value.first < currentSec - 5 }
    }
}
