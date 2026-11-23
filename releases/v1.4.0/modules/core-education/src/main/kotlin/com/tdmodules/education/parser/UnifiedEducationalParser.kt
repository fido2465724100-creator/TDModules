package com.tdmodules.education.parser

import com.tdmodules.education.model.*
import java.util.UUID

/**
 * Универсальный образовательный парсер (Unified Educational Parser).
 * Объединяет все форматы из проектов UniTest, Tdroid, Drlmstudio:
 * 1. Двухколоночные словари и карточки (CSV, TSV, "Термин :: Определение", "Слово - Перевод", "Вопрос | Ответ").
 * 2. Опросники с маркерами правильных ответов (*, +, [x], (x), 1), A)).
 * 3. Карточки Anki (TSV с тегами и HTML-очисткой).
 * 4. Концепт-ориентированные мультиязычные словари (Concept-Pivot).
 * 5. Генерация дистракторов (неправильных вариантов) для создания тестов из словарей на лету.
 */
object UnifiedEducationalParser {

    private val TWO_COLUMN_DELIMITERS = listOf("\t", "::", " | ", ";", " - ")

    /**
     * Автоматическое определение разделителя двух колонок
     */
    fun detectDelimiter(lines: List<String>): String? {
        val sample = lines.take(25)
        for (delim in TWO_COLUMN_DELIMITERS) {
            val count = sample.count { it.contains(delim) }
            if (count >= sample.size * 0.55 && count >= 2) {
                return delim
            }
        }
        return null
    }

    /**
     * Парсинг сырого текста в полиморфный тест (PolymorphicQuizDeck)
     */
    fun parseToQuizDeck(
        rawText: String,
        defaultTitle: String = "Новый тест",
        defaultLang: String = "ru"
    ): PolymorphicQuizDeck {
        val nonBlankLines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (nonBlankLines.isEmpty()) {
            return PolymorphicQuizDeck(
                titleMap = mapOf(defaultLang to defaultTitle),
                questions = emptyList(),
                activeLanguage = defaultLang
            )
        }

        // 1. Проверяем блочный формат тестов с маркерами ответов
        val markerQuestions = parseMarkerBlocks(rawText, defaultLang)
        if (markerQuestions.isNotEmpty()) {
            return PolymorphicQuizDeck(
                titleMap = mapOf(defaultLang to defaultTitle, "en" to "New Quiz"),
                questions = markerQuestions,
                activeLanguage = defaultLang,
                quizType = if (markerQuestions.first().localizations[defaultLang]?.options?.size == 3)
                    EducationalQuizType.SCIENCE_3 else EducationalQuizType.STANDARD_4
            )
        }

        // 2. Проверяем двухколоночный формат (термин ↔ определение / карточки)
        val delimiter = detectDelimiter(nonBlankLines) ?: " - "
        val termPairs = parseTwoColumnPairs(nonBlankLines, delimiter)
        if (termPairs.size >= 2) {
            return convertPairsToQuizDeck(termPairs, defaultTitle, defaultLang)
        }

        // 3. Резервный возврат пустого или минимального теста
        return PolymorphicQuizDeck(
            titleMap = mapOf(defaultLang to defaultTitle),
            questions = emptyList(),
            activeLanguage = defaultLang
        )
    }

    /**
     * Парсинг сырого текста в Concept-Pivot словарь (TerminologyDeck)
     */
    fun parseToTerminologyDeck(
        rawText: String,
        title: String = "Словарь терминов",
        sourceLang: String = "ru",
        targetLang: String = "en"
    ): TerminologyDeck {
        val nonBlankLines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val delimiter = detectDelimiter(nonBlankLines) ?: " - "
        val pairs = parseTwoColumnPairs(nonBlankLines, delimiter)

        val terms = pairs.mapIndexed { index, (left, right) ->
            // left: source (например русский), right: target (английский или иностранный)
            val conceptId = "term_${index + 1}_${UUID.randomUUID().toString().take(8)}"
            val englishAnchor = if (targetLang.lowercase() == "en") right else left

            val term = ConceptTerm(
                conceptId = conceptId,
                englishAnchor = englishAnchor,
                category = "Общее",
                translations = mutableMapOf(
                    sourceLang to TermTranslation(term = left),
                    targetLang to TermTranslation(term = right)
                )
            )
            term
        }.toMutableList()

        return TerminologyDeck(
            title = title,
            defaultSourceLang = sourceLang,
            defaultTargetLang = targetLang,
            terms = terms
        )
    }

    /**
     * Извлечение пар [Ключ -> Значение]
     */
    fun parseTwoColumnPairs(lines: List<String>, delimiter: String): List<Pair<String, String>> {
        val pairs = mutableListOf<Pair<String, String>>()
        for (line in lines) {
            val parts = line.split(delimiter, limit = 2)
            if (parts.size == 2) {
                val q = parts[0].trim().removePrefix("\"").removeSuffix("\"").trim()
                val a = parts[1].trim().removePrefix("\"").removeSuffix("\"").trim()
                if (q.isNotEmpty() && a.isNotEmpty()) {
                    pairs.add(q to a)
                }
            }
        }
        return pairs
    }

    /**
     * Преобразование пар терминов в полноценный тест с генерацией дистракторов
     */
    private fun convertPairsToQuizDeck(
        pairs: List<Pair<String, String>>,
        title: String,
        lang: String
    ): PolymorphicQuizDeck {
        val allAnswers = pairs.map { it.second }.distinct()
        val questions = mutableListOf<PolymorphicQuizQuestion>()

        pairs.forEachIndexed { index, (prompt, correctAns) ->
            val distractors = allAnswers.filter { it != correctAns }.shuffled().take(3)
            val optionsMap = mutableMapOf<String, String>()

            // Инвариантный ключ правильного ответа
            val correctKey = "opt_correct"
            optionsMap[correctKey] = correctAns

            distractors.forEachIndexed { dIdx, distractor ->
                optionsMap["opt_distractor_${dIdx + 1}"] = distractor
            }

            val localization = QuestionLocalization(
                prompt = prompt,
                options = optionsMap
            )

            questions.add(
                PolymorphicQuizQuestion(
                    id = "q_${index + 1}",
                    index = index + 1,
                    correctOptionKey = correctKey,
                    localizations = mutableMapOf(lang to localization),
                    quizType = if (optionsMap.size == 3) EducationalQuizType.SCIENCE_3 else EducationalQuizType.STANDARD_4
                )
            )
        }

        return PolymorphicQuizDeck(
            titleMap = mapOf(lang to title),
            questions = questions,
            activeLanguage = lang
        )
    }

    /**
     * Парсинг блоков с вопросами и вариантами (*, +, [x], A), 1))
     */
    private fun parseMarkerBlocks(rawText: String, lang: String): List<PolymorphicQuizQuestion> {
        val questions = mutableListOf<PolymorphicQuizQuestion>()
        val rawBlocks = rawText.split(Regex("""\r?\n\s*\r?\n"""))
        var counter = 1

        for (block in rawBlocks) {
            val lines = block.lines().map { it.trim() }.filter { it.isNotEmpty() }
            if (lines.size < 2) continue

            val promptLines = mutableListOf<String>()
            val optionLines = mutableListOf<String>()
            var readingPrompt = true

            for (line in lines) {
                if (readingPrompt) {
                    if (isOptionLine(line) && promptLines.isNotEmpty()) {
                        readingPrompt = false
                        optionLines.add(line)
                    } else {
                        promptLines.add(line)
                    }
                } else {
                    optionLines.add(line)
                }
            }

            if (promptLines.isNotEmpty() && optionLines.size >= 2) {
                val promptText = cleanPromptPrefix(promptLines.joinToString(" "))
                val parsedOptions = optionLines.mapIndexed { optIdx, optLine ->
                    val (text, isCorrect) = extractOptionInfo(optLine)
                    Triple("opt_${optIdx + 1}", text, isCorrect)
                }

                // Находим правильный ключ (если не указан, берем первый)
                val correctKey = parsedOptions.find { it.third }?.first ?: parsedOptions.first().first
                val optionsMap = parsedOptions.associate { it.first to it.second }

                questions.add(
                    PolymorphicQuizQuestion(
                        id = "q_${counter}",
                        index = counter++,
                        correctOptionKey = correctKey,
                        localizations = mutableMapOf(
                            lang to QuestionLocalization(prompt = promptText, options = optionsMap)
                        ),
                        quizType = if (optionsMap.size == 3) EducationalQuizType.SCIENCE_3 else EducationalQuizType.STANDARD_4
                    )
                )
            }
        }

        return questions
    }

    private fun isOptionLine(line: String): Boolean {
        val trimmed = line.trimStart()
        if (trimmed.startsWith("*") || trimmed.startsWith("+") ||
            trimmed.startsWith("-") || trimmed.startsWith("•") ||
            trimmed.startsWith("[x]") || trimmed.startsWith("[ ]") ||
            trimmed.startsWith("(x)") || trimmed.startsWith("( )")) {
            return true
        }

        val regex = Regex("""^(\*|\+|\-|•)?\s*([a-zA-Zа-яА-Я0-9][\.\)]|\[[0-9a-zA-Z]\])\s+.*""")
        return regex.matches(trimmed)
    }

    private fun extractOptionInfo(line: String): Pair<String, Boolean> {
        var text = line.trim()
        var isCorrect = false

        var changed = true
        while (changed) {
            val prev = text
            if (text.startsWith("*") || text.startsWith("+") || text.startsWith("[x]") || text.startsWith("(x)")) {
                isCorrect = true
                text = text.removePrefix("*").removePrefix("+").removePrefix("[x]").removePrefix("(x)").trim()
            } else if (text.startsWith("-") || text.startsWith("•") || text.startsWith("[ ]") || text.startsWith("( )")) {
                text = text.removePrefix("-").removePrefix("•").removePrefix("[ ]").removePrefix("( )").trim()
            }

            val prefixRegex = Regex("""^([a-zA-Zа-яА-Я0-9][\.\)]|\[[0-9a-zA-Z]\])\s*""")
            val match = prefixRegex.find(text)
            if (match != null) {
                text = text.substring(match.range.last + 1).trim()
            }

            if (text.startsWith("*") || text.endsWith("*") || text.startsWith("+") || text.endsWith("+")) {
                isCorrect = true
                text = text.replace("*", "").replace("+", "").trim()
            }

            changed = (text != prev)
        }

        return text to isCorrect
    }

    private fun cleanPromptPrefix(prompt: String): String {
        return prompt.replace(Regex("""^\s*(\d+[\.\)]|Вопрос\s*\d+[\.:]?)\s*""", RegexOption.IGNORE_CASE), "").trim()
    }
}
