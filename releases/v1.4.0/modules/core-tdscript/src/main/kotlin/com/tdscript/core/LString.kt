package com.tdscript.core

/**
 * Локализованная строка LString с поддержкой 109 языков.
 * Конкатенация запрещена архитектурно во избежание разрушения грамматического строя языка.
 */
data class LString(
    val key: String,
    val defaultText: String,
    val params: Map<String, Any?> = emptyMap()
) {
    fun format(lang: String = "en", translationsMap: Map<String, Map<String, String>> = emptyMap()): String {
        val langDict = translationsMap[lang.lowercase()]
        var template = langDict?.get(key) ?: defaultText
        params.forEach { (paramKey, paramVal) ->
            template = template.replace("{$paramKey}", paramVal?.toString() ?: "null")
        }
        return template
    }
}
