package com.tdmodules.localization.model

/**
 * Описание поддерживаемого языка в реестре локалей.
 *
 * @param code Двухбуквенный ISO 639-1 код языка (напр. "ru", "en", "uk", "de", "es", "zh")
 * @param nativeName Самоназвание языка на родном языке (Endonym)
 * @param englishName Английское международное название
 * @param isRtl Является ли язык письмом справа налево (Right-To-Left: AR, HE, FA, UR, YI, UG, SD, PS)
 */
data class SupportedLocale(
    val code: String,
    val nativeName: String,
    val englishName: String,
    val isRtl: Boolean = false
) {
    val displayName: String
        get() = "$nativeName ($englishName)"
}

/**
 * Стандартные общие ключи интерфейса (Common UI Keys),
 * присутствующие во всех приложениях экосистемы (UniTest, Tdroid, SuperDriver, TCM).
 */
object CommonKeys {
    const val APP_TITLE = "app_title"
    const val BTN_OK = "btn_ok"
    const val BTN_CANCEL = "btn_cancel"
    const val BTN_SAVE = "btn_save"
    const val BTN_BACK = "btn_back"
    const val BTN_CLOSE = "btn_close"
    const val BTN_DELETE = "btn_delete"
    const val BTN_EDIT = "btn_edit"
    const val BTN_SETTINGS = "btn_settings"
    const val BTN_START = "btn_start"
    const val BTN_STOP = "btn_stop"
    const val BTN_PAUSE = "btn_pause"
    const val BTN_RESUME = "btn_resume"
    const val BTN_RETRY = "btn_retry"
    const val BTN_FINISH = "btn_finish"
    const val BTN_NEXT = "btn_next"
    const val BTN_SEARCH = "btn_search"
    const val BTN_CLEAR = "btn_clear"
    const val BTN_SHARE = "btn_share"
    const val BTN_EXPORT = "btn_export"
    const val BTN_IMPORT = "btn_import"
    const val BTN_REFRESH = "btn_refresh"

    const val TAB_CATALOG = "tab_catalog"
    const val TAB_SETTINGS = "tab_settings"
    const val TAB_IMPORT = "tab_import"
    const val TAB_TEACHER = "tab_teacher"
    const val TAB_AI = "tab_ai"

    const val STATUS_LOADING = "status_loading"
    const val STATUS_READY = "status_ready"
    const val STATUS_ERROR = "status_error"
    const val STATUS_SUCCESS = "status_success"
    const val STATUS_CONNECTED = "status_connected"
    const val STATUS_DISCONNECTED = "status_disconnected"

    const val MSG_CONFIRM_DELETE = "msg_confirm_delete"
    const val LABEL_LANGUAGE = "label_language"
}
