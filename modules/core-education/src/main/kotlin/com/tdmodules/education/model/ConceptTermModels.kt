package com.tdmodules.education.model

import java.util.UUID

/**
 * Данные интервального повторения (Spaced Repetition System — алгоритм SuperMemo SM-2).
 * Позволяет врачам, инженерам и студентам сохранять многолетний прогресс запоминания.
 */
data class SpacedRepetitionData(
    val targetLang: String,
    var intervalDays: Int = 1,
    var repetitionCount: Int = 0,
    var easeFactor: Double = 2.5,
    var nextReviewTimestamp: Long = System.currentTimeMillis(),
    var masteryLevel: Int = 0, // 0 (новый) .. 5 (полностью усвоено)
    var correctCount: Int = 0,
    var wrongCount: Int = 0
) {
    /**
     * Зафиксировать правильный или ошибочный ответ (совместимо с UI счетчиками зеленый/красный)
     */
    fun recordResult(isCorrect: Boolean, quality: Int = if (isCorrect) 5 else 1, currentTime: Long = System.currentTimeMillis()) {
        if (isCorrect) {
            correctCount++
        } else {
            wrongCount++
        }
        recordReview(quality, currentTime)
    }

    /**
     * Обновление интервала по классической формуле SM-2:
     * @param quality Оценка ответа: 0 (полный провал) .. 5 (мгновенный идеальный ответ).
     */
    fun recordReview(quality: Int, currentTime: Long = System.currentTimeMillis()) {
        val q = quality.coerceIn(0, 5)

        // Обновление фактора легкости EF' = EF + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))
        easeFactor = (easeFactor + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))).coerceAtLeast(1.3)

        if (q < 3) {
            // Ошибка: сброс цикла
            repetitionCount = 0
            intervalDays = 1
            masteryLevel = (masteryLevel - 1).coerceAtLeast(0)
        } else {
            // Успешный ответ
            when (repetitionCount) {
                0 -> intervalDays = 1
                1 -> intervalDays = 6
                else -> intervalDays = Math.round(intervalDays * easeFactor).toInt()
            }
            repetitionCount++
            masteryLevel = (masteryLevel + 1).coerceAtMost(5)
        }

        nextReviewTimestamp = currentTime + (intervalDays * 24L * 60L * 60L * 1000L)
    }
}

/**
 * Локализованное представление термина для конкретного языка (RU, DE, ES, FR, ZH и т.д.)
 */
data class TermTranslation(
    val term: String,
    val definition: String = "",
    val phonetic: String = "",
    val exampleSentence: String = "",
    val audioUrl: String = "",
    val synonyms: List<String> = emptyList()
)

/**
 * Единый семантический концепт термина (Concept-Pivot Model).
 * Вместо жестких изолированных пар слов (EN-RU), база привязывается к уникальному Concept ID
 * и международному английскому термину-якорю (English Anchor).
 *
 * Пример:
 * conceptId = "med.cardio.infarction"
 * englishAnchor = "myocardial infarction"
 * translations:
 *   "ru" -> "инфаркт миокарда"
 *   "de" -> "Myokardinfarkt"
 *   "es" -> "infarto de miocardio"
 *   "fr" -> "infarctus du myocarde"
 *
 * При смене языка изучения (например, с немецкого на испанский) прогресс и связи сохраняются!
 */
data class ConceptTerm(
    val conceptId: String = UUID.randomUUID().toString(),
    val englishAnchor: String,
    val category: String = "Общее",
    val partOfSpeech: String = "noun",
    val difficulty: Int = 1, // 1..5
    val translations: MutableMap<String, TermTranslation> = mutableMapOf(),
    val srsMap: MutableMap<String, SpacedRepetitionData> = mutableMapOf(),
    val tags: List<String> = emptyList()
) {
    /**
     * Получить перевод для языка. Если точного языка нет, возвращает fallbackLang (по умолчанию "en").
     */
    fun getTranslation(lang: String, fallbackLang: String = "en"): TermTranslation {
        val normalized = lang.lowercase().trim()
        translations[normalized]?.let { return it }

        // Fallback на английский якорь
        if (translations.containsKey(fallbackLang)) {
            return translations[fallbackLang]!!
        }

        return TermTranslation(term = englishAnchor)
    }

    /**
     * Получить или создать статистику SRS для конкретного целевого языка
     */
    fun getOrCreateSrs(targetLang: String): SpacedRepetitionData {
        val code = targetLang.lowercase().trim()
        return srsMap.getOrPut(code) { SpacedRepetitionData(targetLang = code) }
    }
}

/**
 * Плоская карточка для отображения на экране изучения
 */
data class FlashCardItem(
    val conceptId: String,
    val frontText: String,
    val backText: String,
    val hint: String = "",
    val example: String = "",
    val masteryLevel: Int = 0
)

/**
 * Коллекция понятий и терминов (Словарь / Набор карточек)
 */
data class TerminologyDeck(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val defaultSourceLang: String = "ru",
    val defaultTargetLang: String = "en",
    val terms: MutableList<ConceptTerm> = mutableListOf(),
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Проекция базы на выбранную языковую пару (например, Изучаю: DE, Интерфейс: RU)
     */
    fun toFlashCards(sourceLang: String = defaultSourceLang, targetLang: String = defaultTargetLang): List<FlashCardItem> {
        return terms.map { concept ->
            val source = concept.getTranslation(sourceLang)
            val target = concept.getTranslation(targetLang)
            val srs = concept.getOrCreateSrs(targetLang)

            FlashCardItem(
                conceptId = concept.conceptId,
                frontText = if (source.term.isNotEmpty()) source.term else concept.englishAnchor,
                backText = target.term,
                hint = target.phonetic.ifEmpty { target.definition },
                example = target.exampleSentence,
                masteryLevel = srs.masteryLevel
            )
        }
    }

    /**
     * Получить карточки, готовые к повторению по расписанию SM-2
     */
    fun getDueTerms(targetLang: String = defaultTargetLang, currentTime: Long = System.currentTimeMillis()): List<ConceptTerm> {
        return terms.filter { concept ->
            val srs = concept.getOrCreateSrs(targetLang)
            srs.nextReviewTimestamp <= currentTime
        }
    }

    /**
     * Учесть результат ответа по термину
     */
    fun recordAnswer(conceptId: String, targetLang: String, qualityScore: Int) {
        val term = terms.find { it.conceptId == conceptId } ?: return
        val srs = term.getOrCreateSrs(targetLang)
        srs.recordReview(qualityScore)
    }

    /**
     * Зафиксировать результат сопоставления пары (верно / неверно) для целевого языка
     */
    fun recordMatchResult(conceptId: String, targetLang: String, isCorrect: Boolean) {
        val term = terms.find { it.conceptId == conceptId } ?: return
        val srs = term.getOrCreateSrs(targetLang)
        srs.recordResult(isCorrect)
    }
}
