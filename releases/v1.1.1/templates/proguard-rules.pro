# ============================================================================
# TDModules — Унифицированные правила R8 / ProGuard
# ============================================================================

# Сохранение публичных API TDModules
-keep class com.tdmodules.** { *; }
-keep interface com.tdmodules.** { *; }

# Сохранение TDscript Core API для динамической рефлексии и хост-рантаймов
-keep class com.tdscript.core.** { *; }
-keep interface com.tdscript.core.** { *; }

# Сохранение моделей данных Concept-Pivot и тестов от переименования полей при JSON десериализации
-keepclassmembers class com.tdmodules.education.model.** {
    <fields>;
    <init>(...);
}

# Обфускация и сохранение стойкости SessionCrypto & SecureStringMask
-keepclassmembers class com.tdmodules.security.SessionCrypto {
    public static ** encryptAESGCM(...);
    public static ** decryptAESGCM(...);
}
-keepclassmembers class com.tdmodules.security.SecureStringMask {
    public static ** mask(...);
    public static ** unmask(...);
}

# Защита от декомпиляции: удаление отладочных атрибутов и номеров строк для закрытых сборок
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable,*Annotation*
