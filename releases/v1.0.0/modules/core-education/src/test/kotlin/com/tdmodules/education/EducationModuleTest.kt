package com.tdmodules.education

import com.tdmodules.education.engine.PositionalShuffle
import com.tdmodules.education.model.*
import com.tdmodules.education.parser.UnifiedEducationalParser
import org.junit.Assert.*
import org.junit.Test

class EducationModuleTest {

    @Test
    fun testConceptPivotAndSpacedRepetition() {
        val concept = ConceptTerm(
            conceptId = "med.cardio.infarction",
            englishAnchor = "myocardial infarction",
            category = "Кардиология",
            translations = mutableMapOf(
                "ru" to TermTranslation(term = "инфаркт миокарда", definition = "Некроз участка сердечной мышцы"),
                "de" to TermTranslation(term = "Myokardinfarkt", definition = "Absterben von Herzmuskelgewebe"),
                "es" to TermTranslation(term = "infarto de miocardio", definition = "Necrosis del musculo cardiaco")
            )
        )

        // Проверяем проекцию на разные языки
        assertEquals("инфаркт миокарда", concept.getTranslation("ru").term)
        assertEquals("Myokardinfarkt", concept.getTranslation("de").term)
        assertEquals("infarto de miocardio", concept.getTranslation("es").term)

        // Тестируем интервальное повторение (SM-2 SRS) для немецкого языка
        val srsDe = concept.getOrCreateSrs("de")
        assertEquals(1, srsDe.intervalDays)
        assertEquals(0, srsDe.masteryLevel)

        // Пользователь дал отличный ответ (quality = 5)
        srsDe.recordReview(5)
        assertEquals(1, srsDe.intervalDays)
        assertEquals(1, srsDe.repetitionCount)
        assertEquals(1, srsDe.masteryLevel)

        // Повторный отличный ответ через день
        srsDe.recordReview(5)
        assertEquals(6, srsDe.intervalDays)
        assertEquals(2, srsDe.repetitionCount)

        // Доктор решает переключиться на испанский — прогресс немецкого сохранился, а испанский готов!
        val srsEs = concept.getOrCreateSrs("es")
        assertEquals(1, srsEs.intervalDays)
        assertEquals(0, srsEs.masteryLevel)
        assertEquals(6, srsDe.intervalDays) // Немецкий не затерт!
    }

    @Test
    fun testPolymorphicQuizLanguageSwitching() {
        val question = PolymorphicQuizQuestion(
            id = "phys_01",
            correctOptionKey = "opt_correct",
            localizations = mutableMapOf(
                "de" to QuestionLocalization(
                    prompt = "Was besagt der zweite Hauptsatz der Thermodynamik?",
                    options = mapOf(
                        "opt_correct" to "Die Entropie eines isolierten Systems nimmt niemals ab.",
                        "opt_false_1" to "Energie kann weder erzeugt noch vernichtet werden."
                    )
                ),
                "en" to QuestionLocalization(
                    prompt = "What does the second law of thermodynamics state?",
                    options = mapOf(
                        "opt_correct" to "The entropy of an isolated system never decreases.",
                        "opt_false_1" to "Energy can neither be created nor destroyed."
                    )
                ),
                "ru" to QuestionLocalization(
                    prompt = "Что гласит второй закон термодинамики?",
                    options = mapOf(
                        "opt_correct" to "Энтропия изолированной системы никогда не убывает.",
                        "opt_false_1" to "Энергия не может быть создана или уничтожена."
                    )
                )
            ),
            quizType = EducationalQuizType.SCIENCE_3
        )

        // Отображение на немецком:
        val renderedDe = question.render(lang = "de", shuffleOptions = false)
        assertEquals("Was besagt der zweite Hauptsatz der Thermodynamik?", renderedDe.prompt)
        val correctDe = renderedDe.options.find { it.isCorrect }
        assertNotNull(correctDe)
        assertEquals("Die Entropie eines isolierten Systems nimmt niemals ab.", correctDe!!.text)

        // Переключение на русский посреди теста:
        val renderedRu = question.render(lang = "ru", shuffleOptions = false)
        assertEquals("Что гласит второй закон термодинамики?", renderedRu.prompt)
        val correctRu = renderedRu.options.find { it.isCorrect }
        assertNotNull(correctRu)
        assertEquals("Энтропия изолированной системы никогда не убывает.", correctRu!!.text)

        // Ключи правильного ответа идентичны!
        assertTrue(question.isCorrectAnswer("opt_correct"))
        assertFalse(question.isCorrectAnswer("opt_false_1"))
    }

    @Test
    fun testUnifiedEducationalParserTwoColumn() {
        val input = """
            Force :: Сила
            Velocity :: Скорость
            Acceleration :: Ускорение
            Mass :: Масса
        """.trimIndent()

        val deck = UnifiedEducationalParser.parseToQuizDeck(input, "Физика термины", "ru")
        assertEquals(4, deck.questions.size)

        // Каждый вопрос содержит правильный вариант и 3 дистрактора из остальных слов
        val firstQ = deck.questions.first()
        val loc = firstQ.localizations["ru"]!!
        assertEquals(4, loc.options.size)
        assertTrue(loc.options.containsKey(firstQ.correctOptionKey))
    }

    @Test
    fun testUnifiedEducationalParserMarkers() {
        val input = """
            Вопрос 1. Единица измерения силы в СИ?
            * Ньютон
            - Паскаль
            - Джоуль
            - Ватт
            
            Вопрос 2. Формула второго закона Ньютона?
            - E = mc2
            * F = ma
            - P = UI
        """.trimIndent()

        val deck = UnifiedEducationalParser.parseToQuizDeck(input, "Законы Ньютона", "ru")
        assertEquals(2, deck.questions.size)

        val q1 = deck.questions[0]
        val loc1 = q1.localizations["ru"]!!
        assertEquals("Единица измерения силы в СИ?", loc1.prompt)
        val correctText1 = loc1.options[q1.correctOptionKey]
        assertEquals("Ньютон", correctText1)

        val q2 = deck.questions[1]
        val loc2 = q2.localizations["ru"]!!
        val correctText2 = loc2.options[q2.correctOptionKey]
        assertEquals("F = ma", correctText2)
    }

    @Test
    fun testPositionalShuffleDetection() {
        // Тест, где автор всегда ставил верный ответ на 1-ю позицию
        val questions = (1..5).map { idx ->
            RenderedQuestion(
                id = "q$idx",
                index = idx,
                language = "ru",
                prompt = "Вопрос $idx",
                options = listOf(
                    RenderedOption("opt_1", "Верный вариант", isCorrect = true),
                    RenderedOption("opt_2", "Ложный вариант", isCorrect = false)
                ),
                explanation = ""
            )
        }

        assertTrue(PositionalShuffle.hasFirstPositionBias(questions))
        val normalized = PositionalShuffle.normalizeAndShuffle(questions)
        assertEquals(5, normalized.size)
        assertTrue(normalized.first().explanation.contains("случайным образом"))
    }
}
