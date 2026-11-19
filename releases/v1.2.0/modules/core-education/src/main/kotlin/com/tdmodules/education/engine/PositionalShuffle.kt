package com.tdmodules.education.engine

import com.tdmodules.education.model.RenderedOption
import com.tdmodules.education.model.RenderedQuestion

/**
 * Модуль обнаружения позиционного смещения правильных ответов (Anti-Positional Bias).
 * Если в исходном тесте правильный ответ систематически находится на первой позиции
 * (например, автор теста всегда ставил правильный ответ под буквой 'A' или номером 1),
 * модуль детектирует смещение и перемешивает варианты, предотвращая угадывание.
 */
object PositionalShuffle {

    /**
     * Проверяет список вопросов на наличие позиционного смещения
     * (например, правильный ответ на индексе 0 встречается чаще 65% случаев).
     */
    fun hasFirstPositionBias(questions: List<RenderedQuestion>): Boolean {
        if (questions.size < 3) return false

        var firstPosCorrectCount = 0
        var totalWithCorrect = 0

        for (q in questions) {
            val correctIdx = q.options.indexOfFirst { it.isCorrect }
            if (correctIdx != -1) {
                totalWithCorrect++
                if (correctIdx == 0) firstPosCorrectCount++
            }
        }

        if (totalWithCorrect == 0) return false
        val ratio = firstPosCorrectCount.toDouble() / totalWithCorrect
        return ratio >= 0.65
    }

    /**
     * Нормализует список вопросов:
     * 1. Гарантирует наличие хотя бы одного правильного ответа.
     * 2. Перемешивает варианты ответов.
     * 3. Добавляет уведомление об объективном перемешивании при обнаружении предвзятости.
     */
    fun normalizeAndShuffle(questions: List<RenderedQuestion>): List<RenderedQuestion> {
        if (questions.isEmpty()) return questions

        val hasBias = hasFirstPositionBias(questions)

        return questions.map { q ->
            var opts = q.options

            // Если правильный ответ не был указан, помечаем первый по умолчанию
            val hasCorrect = opts.any { it.isCorrect }
            if (!hasCorrect && opts.isNotEmpty()) {
                opts = opts.mapIndexed { idx, opt ->
                    if (idx == 0) opt.copy(isCorrect = true) else opt
                }
            }

            // Перемешиваем варианты
            val shuffled = opts.shuffled()

            val explanation = when {
                q.explanation.isNotEmpty() -> q.explanation
                hasBias -> "ℹ️ Варианты перемешаны случайным образом для защиты от позиционного угадывания."
                else -> ""
            }

            q.copy(options = shuffled, explanation = explanation)
        }
    }
}
