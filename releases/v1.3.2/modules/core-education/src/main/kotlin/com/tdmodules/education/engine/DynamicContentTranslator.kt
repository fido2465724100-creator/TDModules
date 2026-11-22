package com.tdmodules.education.engine

import com.tdmodules.education.model.ConceptTerm
import com.tdmodules.education.model.PolymorphicQuizQuestion
import com.tdmodules.education.model.QuestionLocalization
import com.tdmodules.education.model.TermTranslation
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

/**
 * Канонический сервис динамического перевода учебного контента (вопросов, вариантов и терминов)
 * на выбранный язык пользователя (Dynamic Content Translation & Projection).
 *
 * Архитектура:
 * 1. Чистый Kotlin JVM без внешних зависимостей (HttpURLConnection).
 * 2. Двухуровневый кэш (в памяти + в самих моделях ConceptTerm/PolymorphicQuizQuestion).
 * 3. Отказоустойчивость: при сетевом сбое возвращает исходный текст.
 */
object DynamicContentTranslator {

    private val cache = ConcurrentHashMap<String, String>()

    /**
     * Быстрый онлайн-перевод текстовой строки на целевой язык.
     */
    fun translateText(text: String, targetLang: String, sourceLang: String = "auto"): String {
        if (text.isBlank()) return text
        val tgt = targetLang.lowercase().trim()
        val cacheKey = "$sourceLang->$tgt:$text"
        cache[cacheKey]?.let { return it }

        if (tgt == "ru" && text.any { it in '\u0400'..'\u04FF' }) return text

        return try {
            val encoded = URLEncoder.encode(text, "UTF-8")
            val urlStr = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=$sourceLang&tl=$tgt&dt=t&q=$encoded"
            val conn = URL(urlStr).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "Mozilla/5.0")
            conn.connectTimeout = 4000
            conn.readTimeout = 8000

            if (conn.responseCode == 200) {
                val response = conn.inputStream.use { stream ->
                    BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).readText()
                }
                val translated = parseGoogleTranslateJson(response)
                if (translated.isNotBlank()) {
                    cache[cacheKey] = translated
                    translated
                } else {
                    text
                }
            } else {
                text
            }
        } catch (_: Exception) {
            text
        }
    }

    /**
     * Парсер ответа Google Translate JSON без внешних библиотек.
     */
    internal fun parseGoogleTranslateJson(json: String): String {
        val trimmed = json.trim()
        if (!trimmed.startsWith("[[[") && !trimmed.startsWith("[[\"")) return ""
        val sb = StringBuilder()
        var i = 0
        while (i < trimmed.length) {
            if (trimmed.startsWith("[\"", i)) {
                val startQuote = i + 2
                var endQuote = startQuote
                var escaped = false
                while (endQuote < trimmed.length) {
                    val ch = trimmed[endQuote]
                    if (ch == '\\' && !escaped) {
                        escaped = true
                    } else if (ch == '"' && !escaped) {
                        break
                    } else {
                        escaped = false
                    }
                    endQuote++
                }
                if (endQuote < trimmed.length) {
                    val piece = trimmed.substring(startQuote, endQuote)
                        .replace("\\\"", "\"")
                        .replace("\\n", "\n")
                        .replace("\\r", "\r")
                        .replace("\\\\", "\\")
                    sb.append(piece)
                    i = endQuote + 1
                    continue
                }
            }
            i++
        }
        return sb.toString().trim()
    }

    /**
     * Гарантирует наличие перевода для понятия ConceptTerm на целевой язык.
     */
    fun ensureConceptTranslated(concept: ConceptTerm, targetLang: String): ConceptTerm {
        val tgt = targetLang.lowercase().trim()
        if (concept.translations.containsKey(tgt)) return concept

        val sourceText = concept.translations["en"]?.term?.ifBlank { null }
            ?: concept.englishAnchor.ifBlank { null }
            ?: concept.translations.values.firstOrNull()?.term
            ?: ""

        if (sourceText.isNotBlank()) {
            val translated = translateText(sourceText, tgt)
            concept.translations[tgt] = TermTranslation(term = translated)
        }
        return concept
    }

    /**
     * Гарантирует наличие перевода для полиморфного вопроса теста на целевой язык.
     */
    fun ensureQuestionTranslated(question: PolymorphicQuizQuestion, targetLang: String): PolymorphicQuizQuestion {
        val tgt = targetLang.lowercase().trim()
        if (question.localizations.containsKey(tgt)) return question

        val baseLoc = question.localizations["en"]
            ?: question.localizations["ru"]
            ?: question.localizations.values.firstOrNull()
            ?: return question

        val translatedPrompt = translateText(baseLoc.prompt, tgt)
        val translatedOptions = mutableMapOf<String, String>()
        baseLoc.options.forEach { (key, optText) ->
            translatedOptions[key] = translateText(optText, tgt)
        }
        val translatedExplanation = if (baseLoc.explanation.isNotBlank()) {
            translateText(baseLoc.explanation, tgt)
        } else ""

        question.localizations[tgt] = QuestionLocalization(
            prompt = translatedPrompt,
            options = translatedOptions,
            explanation = translatedExplanation
        )
        return question
    }
}
