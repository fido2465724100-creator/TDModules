package com.tdmodules.localization

import com.tdmodules.localization.engine.BuiltinTranslations
import com.tdmodules.localization.engine.LocalizationEngine
import com.tdmodules.localization.model.CommonKeys
import com.tdmodules.localization.model.LocaleRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LocalizationModuleTest {

    @Test
    fun testLocaleRegistryHasAll109Languages() {
        val locales = LocaleRegistry.getAllLocales()
        assertTrue(locales.size >= 109, "Registry must contain at least 109 languages, but was ${locales.size}")

        val uk = LocaleRegistry.getLocale("uk")
        assertNotNull(uk)
        assertEquals("Українська", uk.nativeName)
        assertEquals("Ukrainian", uk.englishName)

        val de = LocaleRegistry.getLocale("de")
        assertNotNull(de)
        assertEquals("Deutsch", de.nativeName)

        // Check RTL flags
        assertTrue(LocaleRegistry.isRtl("ar"))
        assertTrue(LocaleRegistry.isRtl("he"))
        assertTrue(!LocaleRegistry.isRtl("en"))
    }

    @Test
    fun testLocalizationEngineFallbackChain() {
        val engine = LocalizationEngine(defaultLang = "uk", fallbackLang = "en")
        engine.registerTranslations("en", BuiltinTranslations.EN)
        engine.registerTranslations("uk", BuiltinTranslations.UK)

        // 1. Direct translation
        assertEquals("Налаштування", engine.tr(CommonKeys.BTN_SETTINGS))
        assertEquals("Зберегти", engine.tr(CommonKeys.BTN_SAVE))

        // 2. Format interpolation
        assertEquals("Помилка: 404", engine.tr(CommonKeys.STATUS_ERROR, 404))

        // 3. Fallback to EN if key missing in UK
        val customMap = mapOf("custom_only_en" to "Hello World")
        engine.registerTranslations("en", customMap)
        assertEquals("Hello World", engine.tr("custom_only_en"))

        // 4. Fallback to raw key if missing in both
        assertEquals("non_existent_key", engine.tr("non_existent_key"))
    }

    @Test
    fun testLanguageSwitchingAndListeners() {
        val engine = LocalizationEngine(defaultLang = "en", fallbackLang = "en")
        engine.registerTranslations("en", BuiltinTranslations.EN)
        engine.registerTranslations("de", BuiltinTranslations.DE)

        assertEquals("Settings", engine.tr(CommonKeys.BTN_SETTINGS))

        var notifiedLang = ""
        engine.addOnLanguageChangeListener { newLang ->
            notifiedLang = newLang
        }

        engine.setLanguage("de")
        assertEquals("de", engine.getCurrentLanguage())
        assertEquals("de", notifiedLang)
        assertEquals("Einstellungen", engine.tr(CommonKeys.BTN_SETTINGS))
    }
}
