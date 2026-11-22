package com.tdmodules.localization.engine

import com.tdmodules.localization.model.LocaleRegistry
import com.tdmodules.localization.model.SupportedLocale
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Универсальный движок локализации для всех проектов экосистемы (TDModules).
 *
 * Особенности:
 * 1. On-Demand Lazy Loading: в памяти держится только активный язык + fallback-словарь (EN).
 * 2. Fallback Chain: Target Lang -> Fallback Lang (EN/RU) -> Key. Никаких крашей при отсутствии перевода!
 * 3. Поддержка параметров интерполяции: %d, %s, {0}, {1}, {}.
 * 4. Реактивные слушатели изменения языка: UI мгновенно перерисовывается без перезапуска активности.
 * 5. Встроенный реестр 109 мировых языков с Endonym и RTL флагами.
 */
class LocalizationEngine(
    private var defaultLang: String = "en",
    private var fallbackLang: String = "en"
) {
    private var currentLang: String = defaultLang.lowercase()
    private val activeDictionary = mutableMapOf<String, String>()
    private val fallbackDictionary = mutableMapOf<String, String>()
    private val listeners = CopyOnWriteArrayList<(String) -> Unit>()
    private val inMemoryStores = mutableMapOf<String, MutableMap<String, String>>()

    // Источник загрузки внешних словарей (абстракция от Android Assets / Desktop Files)
    var dictionaryLoader: ((langCode: String) -> Map<String, String>)? = null

    init {
        loadLanguage(currentLang)
    }

    /**
     * Переключить текущий язык интерфейса
     */
    fun setLanguage(langCode: String) {
        val normalized = langCode.trim().lowercase()
        if (currentLang == normalized && activeDictionary.isNotEmpty()) return

        currentLang = normalized
        loadLanguage(normalized)
        notifyListeners(normalized)
    }

    fun getCurrentLanguage(): String = currentLang

    fun getCurrentLocale(): SupportedLocale? = LocaleRegistry.getLocale(currentLang)

    fun isCurrentRtl(): Boolean = LocaleRegistry.isRtl(currentLang)

    /**
     * Основной метод получения перевода по ключу.
     * Автоматически разрешает цепочку: Current Lang -> Fallback Lang -> raw key.
     */
    fun tr(key: String, vararg args: Any): String {
        val raw = activeDictionary[key]
            ?: fallbackDictionary[key]
            ?: key

        if (args.isEmpty()) return raw

        return try {
            // Поддержка фигурных скобок {} из Tdroid и стандартного %s / %d
            if (raw.contains("{}")) {
                var formatted = raw
                for (arg in args) {
                    formatted = formatted.replaceFirst("{}", arg.toString())
                }
                formatted
            } else {
                String.format(raw, *args)
            }
        } catch (_: Exception) {
            raw
        }
    }

    /**
     * Регистрация локальных переводов напрямую в память (например, для тестов или динамических модулей)
     */
    fun registerTranslations(langCode: String, dict: Map<String, String>) {
        val code = langCode.trim().lowercase()
        inMemoryStores.getOrPut(code) { mutableMapOf() }.putAll(dict)

        if (code == currentLang) {
            activeDictionary.putAll(dict)
        }
        if (code == fallbackLang) {
            fallbackDictionary.putAll(dict)
        }
    }

    fun addOnLanguageChangeListener(listener: (String) -> Unit) {
        listeners.add(listener)
    }

    fun removeOnLanguageChangeListener(listener: (String) -> Unit) {
        listeners.remove(listener)
    }

    private fun notifyListeners(newLang: String) {
        for (l in listeners) {
            try {
                l.invoke(newLang)
            } catch (_: Exception) {}
        }
    }

    private fun loadLanguage(langCode: String) {
        activeDictionary.clear()
        
        // 1. Встроенные словари (если есть)
        BuiltinTranslations.getBuiltin(langCode)?.let {
            activeDictionary.putAll(it)
        }

        // 2. Внешний загрузчик
        val loaded = dictionaryLoader?.invoke(langCode)
        if (loaded != null) {
            activeDictionary.putAll(loaded)
        }

        // 3. Зарегистрированные in-memory словари
        inMemoryStores[langCode]?.let {
            activeDictionary.putAll(it)
        }

        // Загружаем резервный английский словарь, если он еще не в памяти
        if (fallbackDictionary.isEmpty() && langCode != fallbackLang) {
            BuiltinTranslations.getBuiltin(fallbackLang)?.let {
                fallbackDictionary.putAll(it)
            }
            val fbLoaded = dictionaryLoader?.invoke(fallbackLang)
            if (fbLoaded != null) {
                fallbackDictionary.putAll(fbLoaded)
            }
            inMemoryStores[fallbackLang]?.let {
                fallbackDictionary.putAll(it)
            }
        }
    }

    companion object {
        /**
         * Глобальный синглтон для удобного использования в любых частях приложения
         */
        val default: LocalizationEngine by lazy { LocalizationEngine() }
    }
}

