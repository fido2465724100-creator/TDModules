package com.tdmodules.ai

import com.tdmodules.security.SecureStringMask

enum class NodeHealthStatus {
    HEALTHY,
    DEGRADED,
    COOLDOWN
}

/**
 * Описание бесплатной ноды/сервера для ротации
 */
data class FreeEndpointNode(
    val id: String,
    val name: String,
    val baseUrl: String,
    val model: String,
    val apiKey: String = "",
    val cooldownMs: Long = 30_000L,
    var status: NodeHealthStatus = NodeHealthStatus.HEALTHY,
    var failureCount: Int = 0
)

/**
 * Пул бесплатных серверов и моделей с автоматической ротацией (Chain of Free LLMs)
 * и динамическим мониторингом доступности (Health Check & Degraded State).
 * Обеспечивает работу нейросетевых функций во всех проектах экосистемы с нулевыми затратами на серверы.
 */
object FreeModelPool {

    // Маскированные URL базовых сервисов для защиты от реверс-инжиниринга
    private val OVH_KEPLER_URL = SecureStringMask.demask(SecureStringMask.mask("https://oai.endpoints.kepler.ai.cloud.ovh.net/v1"))
    private val POLLINATIONS_URL = SecureStringMask.demask(SecureStringMask.mask("https://text.pollinations.ai/openai"))

    val NODES = listOf(
        FreeEndpointNode(
            id = "ovh_gpt120b",
            name = "TD AI Core (Alpha 120B)",
            baseUrl = OVH_KEPLER_URL,
            model = "gpt-oss-120b",
            cooldownMs = 30_000L
        ),
        FreeEndpointNode(
            id = "pollinations_fast",
            name = "TD Fast Reasoning (Beta Fast)",
            baseUrl = POLLINATIONS_URL,
            model = "openai-fast",
            cooldownMs = 45_000L
        ),
        FreeEndpointNode(
            id = "ovh_gpt20b",
            name = "TD Balanced (Gamma 20B)",
            baseUrl = OVH_KEPLER_URL,
            model = "gpt-oss-20b",
            cooldownMs = 30_000L
        ),
        FreeEndpointNode(
            id = "ovh_llama70b",
            name = "TD Academic Core (Delta 70B)",
            baseUrl = OVH_KEPLER_URL,
            model = "Meta-Llama-3_3-70B-Instruct",
            cooldownMs = 30_000L
        )
    )

    private val lastUsedMap = mutableMapOf<String, Long>()
    private val cooldownUntilMap = mutableMapOf<String, Long>()
    private var roundRobinIndex = 0

    @Synchronized
    fun getAvailableNode(): FreeEndpointNode? {
        val now = System.currentTimeMillis()
        val total = NODES.size
        for (i in 0 until total) {
            val candidate = NODES[(roundRobinIndex + i) % total]
            val cooldownUntil = cooldownUntilMap[candidate.id] ?: 0L
            val lastUsed = lastUsedMap[candidate.id] ?: 0L

            if (now >= cooldownUntil && (now - lastUsed >= candidate.cooldownMs)) {
                roundRobinIndex = (roundRobinIndex + i + 1) % total
                lastUsedMap[candidate.id] = now
                candidate.status = NodeHealthStatus.HEALTHY
                return candidate
            }
        }
        return null // Все серверы на кулдауне
    }

    /**
     * Возвращает список всех нод в порядке приоритета
     */
    @Synchronized
    fun getAllNodesInOrder(): List<FreeEndpointNode> {
        val total = NODES.size
        return (0 until total).map { NODES[(roundRobinIndex + it) % total] }
    }

    @Synchronized
    fun getEarliestWaitSeconds(): Int {
        val now = System.currentTimeMillis()
        var minWait = Long.MAX_VALUE
        for (node in NODES) {
            val cooldownUntil = cooldownUntilMap[node.id] ?: 0L
            val lastUsed = lastUsedMap[node.id] ?: 0L
            val readyAt = maxOf(cooldownUntil, lastUsed + node.cooldownMs)
            if (readyAt <= now) return 0
            val waitMs = readyAt - now
            if (waitMs < minWait) minWait = waitMs
        }
        return if (minWait == Long.MAX_VALUE) 0 else ((minWait + 999) / 1000).toInt()
    }

    @Synchronized
    fun reportError(nodeId: String, errorCode: Int, retryAfterSeconds: Int? = null) {
        val node = NODES.find { it.id == nodeId } ?: return
        node.failureCount++

        val now = System.currentTimeMillis()
        val cooldownDuration = if (retryAfterSeconds != null && retryAfterSeconds > 0) {
            retryAfterSeconds * 1000L
        } else {
            when (errorCode) {
                429 -> 60_000L // Rate limit: 60 сек
                503, 502 -> 45_000L // Сервис недоступен: 45 сек
                else -> 20_000L
            }
        }

        cooldownUntilMap[nodeId] = now + cooldownDuration
        node.status = if (node.failureCount >= 3) NodeHealthStatus.DEGRADED else NodeHealthStatus.COOLDOWN
    }

    /**
     * Псевдоним для совместимости со старыми вызовами reportNodeThrottled
     */
    @Synchronized
    fun reportNodeThrottled(nodeId: String, retryAfterSeconds: Int = 60) {
        reportError(nodeId, 429, retryAfterSeconds)
    }

    @Synchronized
    fun reportSuccess(nodeId: String) {
        val node = NODES.find { it.id == nodeId } ?: return
        node.failureCount = 0
        node.status = NodeHealthStatus.HEALTHY
        cooldownUntilMap.remove(nodeId)
    }

    /**
     * Псевдоним для совместимости со старыми вызовами reportNodeSuccess
     */
    @Synchronized
    fun reportNodeSuccess(nodeId: String) {
        reportSuccess(nodeId)
    }

    @Synchronized
    fun resetAll() {
        lastUsedMap.clear()
        cooldownUntilMap.clear()
        NODES.forEach {
            it.status = NodeHealthStatus.HEALTHY
            it.failureCount = 0
        }
    }

    // Совместимость со старыми свойствами
    val ENDPOINTS = listOf(NODES.first().baseUrl)
    val MODELS = NODES.map { it.model }
}
