package com.tdmodules.ai

import java.io.File

data class ModelRecommendation(
    val name: String,
    val sizeLabel: String,
    val minRamGb: Double,
    val recommendedRamGb: Double,
    val description: String,
    val format: String,
    val downloadUrl: String,
    val isRecommendedForDevice: Boolean = false
)

data class DeviceHardwareSpecs(
    val totalRamGb: Double,
    val availRamGb: Double,
    val cpuCores: Int,
    val architecture: String,
    val isReadyForLocalLlm: Boolean,
    val recommendedModelTier: String,
    val recommendations: List<ModelRecommendation>
)

/**
 * Профилировщик аппаратных ресурсов устройства (RAM, CPU Cores, Архитектура).
 * Универсален: работает как на Android (ARM64/x86), так и на Desktop Linux/Windows/Mac.
 * Формирует автоматические рекомендации по локальному запуску GGUF моделей (1.5B, 3B, 7B/8B).
 */
object DeviceProfiler {

    val CATALOG_MODELS = listOf(
        ModelRecommendation(
            name = "Qwen 2.5 1.5B Instruct (Q4_K_M)",
            sizeLabel = "1.5 млрд параметров (~1.1 ГБ)",
            minRamGb = 4.0,
            recommendedRamGb = 6.0,
            description = "Сверхбыстрая и лёгкая модель. Идеальна для базовых тестов, школьных предметов, языковых карточек и сопоставлений слов. Минимальный расход батареи.",
            format = "GGUF / MediaPipe",
            downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF"
        ),
        ModelRecommendation(
            name = "Llama 3.2 3B Instruct (Q4_K_M)",
            sizeLabel = "3.2 млрд параметров (~2.0 ГБ)",
            minRamGb = 8.0,
            recommendedRamGb = 10.0,
            description = "Оптимальный баланс для устройств среднего и высокого класса. Превосходно справляется с тестами по естественным наукам, истории, литературе.",
            format = "GGUF",
            downloadUrl = "https://huggingface.co/bartowski/Llama-3.2-3B-Instruct-GGUF"
        ),
        ModelRecommendation(
            name = "Qwen 2.5 3B Instruct (Q4_K_M)",
            sizeLabel = "3.1 млрд параметров (~2.1 ГБ)",
            minRamGb = 8.0,
            recommendedRamGb = 10.0,
            description = "Высочайшая академическая точность в классе 3B. Отлично решает логические и математические тесты университетского уровня.",
            format = "GGUF",
            downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-3B-Instruct-GGUF"
        ),
        ModelRecommendation(
            name = "Gemma 2 2B Instruct (Q4_K_M)",
            sizeLabel = "2.6 млрд параметров (~1.7 ГБ)",
            minRamGb = 6.0,
            recommendedRamGb = 8.0,
            description = "Разработка Google DeepMind. Высокая культура русского и английского языка, строгий академический тон объяснений.",
            format = "GGUF / MediaPipe",
            downloadUrl = "https://huggingface.co/google/gemma-2-2b-it-GGUF"
        ),
        ModelRecommendation(
            name = "Qwen 2.5 7B Instruct (Q4_K_M)",
            sizeLabel = "7.6 млрд параметров (~4.5 ГБ)",
            minRamGb = 12.0,
            recommendedRamGb = 14.0,
            description = "Профессиональный уровень. Университетские дисциплины, квантовая физика, высшая математика. Требует мощный процессор и от 12-16 ГБ ОЗУ.",
            format = "GGUF",
            downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-7B-Instruct-GGUF"
        )
    )

    /**
     * Определение характеристик системы в кроссплатформенном режиме
     */
    fun getSpecs(customTotalRamGb: Double? = null, customAvailRamGb: Double? = null): DeviceHardwareSpecs {
        val cores = Runtime.getRuntime().availableProcessors()
        val arch = System.getProperty("os.arch") ?: "unknown"

        val (totalGb, availGb) = if (customTotalRamGb != null && customAvailRamGb != null) {
            customTotalRamGb to customAvailRamGb
        } else {
            readSystemRam()
        }

        val (ready, tier) = when {
            totalGb >= 11.5 -> true to "Флагман / Workstation (до 7B-8B Q4 без ограничений)"
            totalGb >= 7.5 -> true to "Оптимальный (до 3B-4B Q4: Qwen 2.5 3B / Llama 3.2 3B)"
            totalGb >= 5.0 -> true to "Базовый (модели 1.5B-2B: Qwen 1.5B / Gemma 2B)"
            totalGb >= 3.5 -> true to "Минимальный (модели 1B-1.5B в режиме жесткой экономии памяти)"
            else -> false to "Не рекомендуется для локальных LLM (менее 3.5 ГБ ОЗУ, используйте облачный FreeModelPool)"
        }

        val recommendations = CATALOG_MODELS.map { model ->
            val isFit = totalGb >= model.minRamGb
            val isPerfect = totalGb >= model.recommendedRamGb
            model.copy(isRecommendedForDevice = isFit && (isPerfect || totalGb < 8.0))
        }

        return DeviceHardwareSpecs(
            totalRamGb = Math.round(totalGb * 10.0) / 10.0,
            availRamGb = Math.round(availGb * 10.0) / 10.0,
            cpuCores = cores,
            architecture = arch,
            isReadyForLocalLlm = ready,
            recommendedModelTier = tier,
            recommendations = recommendations
        )
    }

    private fun readSystemRam(): Pair<Double, Double> {
        // Попытка прочесть /proc/meminfo на Linux/Android
        try {
            val memInfoFile = File("/proc/meminfo")
            if (memInfoFile.exists()) {
                var totalKb = 0L
                var availKb = 0L
                memInfoFile.forEachLine { line ->
                    if (line.startsWith("MemTotal:")) {
                        totalKb = line.filter { it.isDigit() }.toLongOrNull() ?: 0L
                    } else if (line.startsWith("MemAvailable:")) {
                        availKb = line.filter { it.isDigit() }.toLongOrNull() ?: 0L
                    }
                }
                if (totalKb > 0) {
                    val totalGb = totalKb.toDouble() / (1024 * 1024)
                    val availGb = (if (availKb > 0) availKb else totalKb / 2).toDouble() / (1024 * 1024)
                    return totalGb to availGb
                }
            }
        } catch (_: Exception) {}

        // Резерв: Runtime JVM heap
        val maxMem = Runtime.getRuntime().maxMemory().toDouble() / (1024 * 1024 * 1024)
        return (maxMem * 2.0) to maxMem
    }
}
