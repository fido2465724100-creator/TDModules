# 🌐 07. Международная локализация и реестр языков (`core-localization`)

Модуль **`core-localization`** решает фундаментальную проблему разработки: устраняет разрозненные локальные костыли перевода и гарантирует мгновенную поддержку **109 мировых языков** в любом приложении экосистемы (`UniTest`, `Tdroid`, `TCM`, `SuperDriver`, `TDRobotics`).

---

## 🏗️ Ключевые возможности

1. **Реестр 109 языков (`LocaleRegistry`)**:
   - Полный перечень языков по стандарту ISO 639-1.
   - Самоназвания на родном языке (Endonyms) + международные английские названия.
   - Флаг письма справа налево (`isRtl: Boolean`) для арабского, иврита, урду, фарси, идиша и др.

2. **Отказоустойчивый движок (`LocalizationEngine`)**:
   - **On-Demand Lazy Loading**: в оперативной памяти смартфона держится только активный язык + аварийный fallback (EN), что экономит ОЗУ.
   - **Zero-Crash Fallback Chain**: `Target Language ➔ Fallback Language (EN) ➔ Key`. Интерфейс никогда не падает при отсутствии конкретного ключа.
   - **Универсальная интерполяция**: поддержка спецификаторов `%d`, `%s`, `{}` и `{0}`.
   - **Реактивные слушатели**: мгновенная перерисовка экранов при смене языка через `addOnLanguageChangeListener`.

3. **Встроенные базовые словари (`BuiltinTranslations`)**:
   - Стандартные системные действия (`ОК`, `Отмена`, `Сохранить`, `Назад`, `Удалить`, `Настройки`, `Поиск`, `Старт`, `Класс`, `ИИ`) уже встроены для ключевых языков (RU, EN, UK, DE) прямо в байткод модуля.

---

## 💻 Примеры использования

### 1. Базовый перевод строк
```kotlin
import com.tdmodules.localization.engine.LocalizationEngine
import com.tdmodules.localization.model.CommonKeys

val i18n = LocalizationEngine.default

// Получить перевод кнопки
val saveBtnText = i18n.tr(CommonKeys.BTN_SAVE) // "Save" или "Зберегти"

// Перевод с параметрами
val errorMsg = i18n.tr("status_error", 404) // "Ошибка: 404"
```

### 2. Смена языка и реактивность
```kotlin
i18n.addOnLanguageChangeListener { newLangCode ->
    println("Язык интерфейса изменен на: $newLangCode")
    refreshUI()
}

// Переключение на украинский язык
i18n.setLanguage("uk")
```

### 3. Подключение внешних JSON словарей (Android / Desktop)
```kotlin
i18n.dictionaryLoader = { langCode ->
    // Загрузка JSON файла из assets или файловой системы
    loadJsonDictionary(langCode)
}
```

---

## 📜 Регламент для разработчиков (CONTRIBUTING RULE)
**КАТЕГОРИЧЕСКИ ЗАПРЕЩЕНО** вставлять видимые пользователю строковые литералы на русском или английском языке напрямую в разметку вёрстки (`android:text="..."`, `android:hint="..."`) или в код Activity/Compose.
Любой видимый текст обязан определяться через строковый ключ `i18n.tr("key_name")`.
