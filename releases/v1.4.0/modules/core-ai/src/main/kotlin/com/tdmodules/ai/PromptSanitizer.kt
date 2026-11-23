package com.tdmodules.ai

import java.util.regex.Pattern

/**
 * Модуль защиты от Prompt Injection и санитизации пользовательского ввода.
 * Нейтрализует попытки заставить модель раскрыть системные инструкции, пароли,
 * обойти белый лейбл (White Label) или выполнить несанкционированные действия.
 */
object PromptSanitizer {

    private val INJECTION_PATTERNS = listOf(
        Pattern.compile("(?i)ignore\\s+(all\\s+)?(previous|prior|above)\\s+(instructions|directives|rules)"),
        Pattern.compile("(?i)disregard\\s+(all\\s+)?(previous|prior|above)\\s+(instructions|directives|rules)"),
        Pattern.compile("(?i)(reveal|show|print|display|tell\\s+me)\\s+(your|the)?\\s*(system\\s+prompt|instructions|initial\\s+prompt)"),
        Pattern.compile("(?i)(who|what)\\s+(created|trained|made|developed)\\s+you"),
        Pattern.compile("(?i)(are\\s+you|acting\\s+as)\\s+(chatgpt|openai|llama|qwen|claude|gemini)"),
        Pattern.compile("(?i)output\\s+the\\s+system\\s+directive"),
        Pattern.compile("(?i)jailbreak|dan\\s+mode|developer\\s+mode")
    )

    private const val REPLACEMENT_NOTICE = "[Запрос очищен системой защиты TDModules]"

    /**
     * Очищает пользовательский ввод от опасных инструкций внедрения
     */
    fun sanitize(input: String): String {
        if (input.isBlank()) return input

        var sanitized = input
        for (pattern in INJECTION_PATTERNS) {
            val matcher = pattern.matcher(sanitized)
            if (matcher.find()) {
                sanitized = matcher.replaceAll(REPLACEMENT_NOTICE)
            }
        }

        // Удаление потенциально опасных разделителей разметки промпта и чат-тегов
        sanitized = sanitized
            .replace("<|im_start|>", "")
            .replace("<|im_end|>", "")
            .replace("<|system|>", "")
            .replace("<|user|>", "")
            .replace("<|assistant|>", "")
            .replace("```system", "```")

        return sanitized.trim()
    }

    /**
     * Проверяет, содержит ли входной текст попытку инъекции
     */
    fun containsInjectionAttempt(input: String): Boolean {
        return INJECTION_PATTERNS.any { it.matcher(input).find() }
    }
}
