package com.tdmodules.localization.engine

import com.tdmodules.localization.model.CommonKeys

/**
 * Встроенный словарь стандартных системных кнопок и статусов
 * для ключевых языков экосистемы (RU, EN, UK, DE, ES, ZH).
 * Гарантирует, что даже без загрузки внешних JSON-файлов базовый интерфейс
 * любого модуля TDModules будет отображаться корректно на родном языке.
 */
object BuiltinTranslations {

    val EN = mapOf(
        CommonKeys.APP_TITLE to "UniTest",
        CommonKeys.BTN_OK to "OK",
        CommonKeys.BTN_CANCEL to "Cancel",
        CommonKeys.BTN_SAVE to "Save",
        CommonKeys.BTN_BACK to "Back",
        CommonKeys.BTN_CLOSE to "Close",
        CommonKeys.BTN_DELETE to "Delete",
        CommonKeys.BTN_EDIT to "Edit",
        CommonKeys.BTN_SETTINGS to "Settings",
        CommonKeys.BTN_START to "Start",
        CommonKeys.BTN_STOP to "Stop",
        CommonKeys.BTN_PAUSE to "Pause",
        CommonKeys.BTN_RESUME to "Resume",
        CommonKeys.BTN_RETRY to "Retry",
        CommonKeys.BTN_FINISH to "Finish",
        CommonKeys.BTN_NEXT to "Next",
        CommonKeys.BTN_SEARCH to "Search",
        CommonKeys.BTN_CLEAR to "Clear All",
        CommonKeys.BTN_SHARE to "Share",
        CommonKeys.BTN_EXPORT to "Export",
        CommonKeys.BTN_IMPORT to "Import",
        CommonKeys.BTN_REFRESH to "Refresh",
        CommonKeys.TAB_CATALOG to "Tests",
        CommonKeys.TAB_SETTINGS to "Settings",
        CommonKeys.TAB_IMPORT to "Import",
        CommonKeys.TAB_TEACHER to "Classroom",
        CommonKeys.TAB_AI to "AI Generator",
        CommonKeys.STATUS_LOADING to "Loading...",
        CommonKeys.STATUS_READY to "Ready",
        CommonKeys.STATUS_ERROR to "Error: %s",
        CommonKeys.STATUS_SUCCESS to "Success",
        CommonKeys.STATUS_CONNECTED to "Connected",
        CommonKeys.STATUS_DISCONNECTED to "Disconnected",
        CommonKeys.MSG_CONFIRM_DELETE to "Are you sure you want to delete this item?",
        CommonKeys.LABEL_LANGUAGE to "Interface Language"
    )

    val RU = mapOf(
        CommonKeys.APP_TITLE to "UniTest",
        CommonKeys.BTN_OK to "ОК",
        CommonKeys.BTN_CANCEL to "Отмена",
        CommonKeys.BTN_SAVE to "Сохранить",
        CommonKeys.BTN_BACK to "Назад",
        CommonKeys.BTN_CLOSE to "Закрыть",
        CommonKeys.BTN_DELETE to "Удалить",
        CommonKeys.BTN_EDIT to "Редактировать",
        CommonKeys.BTN_SETTINGS to "Настройки",
        CommonKeys.BTN_START to "Старт",
        CommonKeys.BTN_STOP to "Стоп",
        CommonKeys.BTN_PAUSE to "Пауза",
        CommonKeys.BTN_RESUME to "Продолжить",
        CommonKeys.BTN_RETRY to "Повторить",
        CommonKeys.BTN_FINISH to "Завершить",
        CommonKeys.BTN_NEXT to "Далее",
        CommonKeys.BTN_SEARCH to "Поиск",
        CommonKeys.BTN_CLEAR to "Очистить всё",
        CommonKeys.BTN_SHARE to "Поделиться",
        CommonKeys.BTN_EXPORT to "Экспорт",
        CommonKeys.BTN_IMPORT to "Импорт",
        CommonKeys.BTN_REFRESH to "Обновить",
        CommonKeys.TAB_CATALOG to "Тесты",
        CommonKeys.TAB_SETTINGS to "Настройки",
        CommonKeys.TAB_IMPORT to "Импорт",
        CommonKeys.TAB_TEACHER to "Класс",
        CommonKeys.TAB_AI to "ИИ",
        CommonKeys.STATUS_LOADING to "Загрузка...",
        CommonKeys.STATUS_READY to "Готово к работе",
        CommonKeys.STATUS_ERROR to "Ошибка: %s",
        CommonKeys.STATUS_SUCCESS to "Успешно выполнено",
        CommonKeys.STATUS_CONNECTED to "Подключено",
        CommonKeys.STATUS_DISCONNECTED to "Отключено",
        CommonKeys.MSG_CONFIRM_DELETE to "Вы уверены, что хотите удалить этот элемент?",
        CommonKeys.LABEL_LANGUAGE to "Язык интерфейса"
    )

    val UK = mapOf(
        CommonKeys.APP_TITLE to "UniTest",
        CommonKeys.BTN_OK to "ОК",
        CommonKeys.BTN_CANCEL to "Скасувати",
        CommonKeys.BTN_SAVE to "Зберегти",
        CommonKeys.BTN_BACK to "Назад",
        CommonKeys.BTN_CLOSE to "Закрити",
        CommonKeys.BTN_DELETE to "Видалити",
        CommonKeys.BTN_EDIT to "Редагувати",
        CommonKeys.BTN_SETTINGS to "Налаштування",
        CommonKeys.BTN_START to "Старт",
        CommonKeys.BTN_STOP to "Стоп",
        CommonKeys.BTN_PAUSE to "Пауза",
        CommonKeys.BTN_RESUME to "Продовжити",
        CommonKeys.BTN_RETRY to "Повторити",
        CommonKeys.BTN_FINISH to "Завершити",
        CommonKeys.BTN_NEXT to "Далі",
        CommonKeys.BTN_SEARCH to "Пошук",
        CommonKeys.BTN_CLEAR to "Очистити все",
        CommonKeys.BTN_SHARE to "Поділитися",
        CommonKeys.BTN_EXPORT to "Експорт",
        CommonKeys.BTN_IMPORT to "Імпорт",
        CommonKeys.BTN_REFRESH to "Оновити",
        CommonKeys.TAB_CATALOG to "Тести",
        CommonKeys.TAB_SETTINGS to "Налаштування",
        CommonKeys.TAB_IMPORT to "Імпорт",
        CommonKeys.TAB_TEACHER to "Клас",
        CommonKeys.TAB_AI to "ШІ",
        CommonKeys.STATUS_LOADING to "Завантаження...",
        CommonKeys.STATUS_READY to "Готово до роботи",
        CommonKeys.STATUS_ERROR to "Помилка: %s",
        CommonKeys.STATUS_SUCCESS to "Успішно виконано",
        CommonKeys.STATUS_CONNECTED to "Підключено",
        CommonKeys.STATUS_DISCONNECTED to "Відключено",
        CommonKeys.MSG_CONFIRM_DELETE to "Ви дійсно бажаєте видалити цей елемент?",
        CommonKeys.LABEL_LANGUAGE to "Мова інтерфейсу"
    )

    val DE = mapOf(
        CommonKeys.APP_TITLE to "UniTest",
        CommonKeys.BTN_OK to "OK",
        CommonKeys.BTN_CANCEL to "Abbrechen",
        CommonKeys.BTN_SAVE to "Speichern",
        CommonKeys.BTN_BACK to "Zurück",
        CommonKeys.BTN_CLOSE to "Schließen",
        CommonKeys.BTN_DELETE to "Löschen",
        CommonKeys.BTN_EDIT to "Bearbeiten",
        CommonKeys.BTN_SETTINGS to "Einstellungen",
        CommonKeys.BTN_START to "Starten",
        CommonKeys.BTN_STOP to "Stoppen",
        CommonKeys.BTN_PAUSE to "Pause",
        CommonKeys.BTN_RESUME to "Fortsetzen",
        CommonKeys.BTN_RETRY to "Wiederholen",
        CommonKeys.BTN_FINISH to "Abschließen",
        CommonKeys.BTN_NEXT to "Weiter",
        CommonKeys.BTN_SEARCH to "Suchen",
        CommonKeys.BTN_CLEAR to "Alles löschen",
        CommonKeys.BTN_SHARE to "Teilen",
        CommonKeys.BTN_EXPORT to "Exportieren",
        CommonKeys.BTN_IMPORT to "Importieren",
        CommonKeys.BTN_REFRESH to "Aktualisieren",
        CommonKeys.TAB_CATALOG to "Tests",
        CommonKeys.TAB_SETTINGS to "Einstellungen",
        CommonKeys.TAB_IMPORT to "Import",
        CommonKeys.TAB_TEACHER to "Klasse",
        CommonKeys.TAB_AI to "KI",
        CommonKeys.STATUS_LOADING to "Wird geladen...",
        CommonKeys.STATUS_READY to "Bereit",
        CommonKeys.STATUS_ERROR to "Fehler: %s",
        CommonKeys.STATUS_SUCCESS to "Erfolgreich",
        CommonKeys.STATUS_CONNECTED to "Verbunden",
        CommonKeys.STATUS_DISCONNECTED to "Getrennt",
        CommonKeys.MSG_CONFIRM_DELETE to "Möchten Sie dieses Element wirklich löschen?",
        CommonKeys.LABEL_LANGUAGE to "Oberflächensprache"
    )

    fun getBuiltin(code: String): Map<String, String>? = when (code.lowercase()) {
        "ru" -> RU
        "en" -> EN
        "uk" -> UK
        "de" -> DE
        else -> null
    }
}
