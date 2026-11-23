package com.tdmodules.education.model

import java.util.UUID

/**
 * Тип тестирования и формат распределения вариантов
 */
enum class EducationalQuizType {
    MATCHING_PAIRS, // Сопоставление терминов и определений (карточки / пары)
    SCIENCE_3,      // Естественные науки / медицина / инженерия (1 верный + 2 дистрактора)
    STANDARD_4,     // Классический тест (1 верный + 3 дистрактора)
    TRUE_FALSE      // Верно / Неверно (2 варианта)
}

/**
 * Локализованный контент вопроса для конкретного языка (RU, EN, DE, FR и др.)
 */
data class QuestionLocalization(
    val prompt: String,
    val options: Map<String, String>, // optionKey -> Текст варианта на данном языке
    val explanation: String = ""
)

/**
 * Отрендеренный вариант ответа для отображения в интерфейсе
 */
data class RenderedOption(
    val key: String,
    val text: String,
    val isCorrect: Boolean
)

/**
 * Вопрос, подготовленный для отображения на активном языке
 */
data class RenderedQuestion(
    val id: String,
    val index: Int,
    val language: String,
    val prompt: String,
    val options: List<RenderedOption>,
    val explanation: String
)

/**
 * Полиморфный мультиязычный вопрос теста.
 * Ключевая особенность: инвариантность ключей вариантов (Key-Invariant Architecture).
 *
 * Пример:
 * - correctOptionKey = "opt_a"
 * - В немецкой локализации: prompt = "Was ist Energie?", opt_a = "Fahigkeit, Arbeit zu verrichten"
 * - В английской локализации: prompt = "What is energy?", opt_a = "Ability to do work"
 * - В русской локализации: prompt = "Что такое энергия?", opt_a = "Способность совершать работу"
 *
 * Пользователь может переключить язык прямо в середине теста:
 * логика проверки, статистика правильных ответов и позиция в тесте НЕ теряются!
 */
data class PolymorphicQuizQuestion(
    val id: String = UUID.randomUUID().toString(),
    val index: Int = 1,
    val subject: String = "Общее",
    val topic: String = "",
    val correctOptionKey: String,
    val localizations: MutableMap<String, QuestionLocalization> = mutableMapOf(),
    val quizType: EducationalQuizType = EducationalQuizType.STANDARD_4
) {
    /**
     * Проверка ответа по инвариантному ключу
     */
    fun isCorrectAnswer(selectedKey: String): Boolean {
        return correctOptionKey.trim() == selectedKey.trim()
    }

    /**
     * Получить отрендеренный вопрос для указанного языка с автоматическим fallback
     * и возможностью мгновенного доперевода на лету (autoTranslate).
     */
    fun render(
        lang: String, 
        fallbackLang: String = "en", 
        shuffleOptions: Boolean = true,
        autoTranslate: Boolean = false
    ): RenderedQuestion {
        val targetLang = lang.lowercase().trim()
        if (autoTranslate && !localizations.containsKey(targetLang)) {
            com.tdmodules.education.engine.DynamicContentTranslator.ensureQuestionTranslated(this, targetLang)
        }

        val loc = localizations[targetLang]
            ?: localizations[fallbackLang]
            ?: localizations.values.firstOrNull()
            ?: QuestionLocalization(prompt = "Missing translation", options = emptyMap())

        var optionList = loc.options.map { (key, text) ->
            RenderedOption(
                key = key,
                text = text,
                isCorrect = (key == correctOptionKey)
            )
        }

        if (shuffleOptions) {
            optionList = optionList.shuffled()
        }

        return RenderedQuestion(
            id = id,
            index = index,
            language = if (localizations.containsKey(targetLang)) targetLang else fallbackLang,
            prompt = loc.prompt,
            options = optionList,
            explanation = loc.explanation
        )
    }

    /**
     * Поддерживается ли данный язык в вопросе
     */
    fun hasLanguage(lang: String): Boolean = localizations.containsKey(lang.lowercase().trim())
}

/**
 * Полиморфный мультиязычный тест (набор вопросов)
 */
data class PolymorphicQuizDeck(
    val id: String = UUID.randomUUID().toString(),
    val titleMap: Map<String, String>, // Язык -> Название теста ("ru" -> "Физика: Термодинамика", "de" -> "Physik: Thermodynamik", "en" -> "Physics: Thermodynamics")
    val descriptionMap: Map<String, String> = emptyMap(),
    val questions: List<PolymorphicQuizQuestion> = emptyList(),
    var activeLanguage: String = "ru",
    val quizType: EducationalQuizType = EducationalQuizType.STANDARD_4,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getTitle(lang: String = activeLanguage, fallback: String = "en"): String {
        return titleMap[lang] ?: titleMap[fallback] ?: titleMap.values.firstOrNull() ?: "Тест"
    }

    fun getDescription(lang: String = activeLanguage, fallback: String = "en"): String {
        return descriptionMap[lang] ?: descriptionMap[fallback] ?: ""
    }

    /**
     * Мгновенное переключение языка тестирования без потери структуры
     */
    fun switchLanguage(newLang: String): PolymorphicQuizDeck {
        activeLanguage = newLang.lowercase().trim()
        return this
    }

    /**
     * Получить список всех доступных языков в данном тесте
     */
    fun getAvailableLanguages(): Set<String> {
        val set = mutableSetOf<String>()
        set.addAll(titleMap.keys)
        questions.forEach { q ->
            set.addAll(q.localizations.keys)
        }
        return set
    }
}
