# ============================================================================
# TDModules Hardened ProGuard / R8 Obfuscation & Security Rules
# ============================================================================

# 1. Общие настройки защиты
-repackageclasses 'com.tdmodules.internal'
-allowaccessmodification
-overloadaggressively
-flattenpackagehierarchy

# 2. Удаление логов и отладочных сообщений в релизной сборке
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}

# 3. Защита криптографических примитивов
-keepclassmembers class com.tdmodules.security.SessionCrypto {
    public <methods>;
}
-keep class com.tdmodules.security.SecureStringMask {
    public static java.lang.String demask(java.lang.String, byte);
    public static java.lang.String reveal(java.lang.String);
}
-keep class com.tdmodules.security.RateLimiter {
    public <methods>;
}

# 4. Сериализация моделей образовательного модуля (JSON / Ktor / Gson / Moshi)
-keepclassmembers class com.tdmodules.education.model.** {
    <fields>;
    public <methods>;
}
-keep class com.tdmodules.education.model.** { *; }

# 5. Нейросетевой пул и профилировщик
-keepclassmembers class com.tdmodules.ai.FreeModelPool {
    public <methods>;
}
-keepclassmembers class com.tdmodules.ai.PromptSanitizer {
    public <methods>;
}
-keepclassmembers class com.tdmodules.ai.DeviceProfiler {
    public <methods>;
}
-keep class com.tdmodules.ai.ModelRecommendation { *; }
-keep class com.tdmodules.ai.DeviceHardwareSpecs { *; }

# 6. Сетевой туннель (JSch / Coroutines)
-keep class com.jcraft.jsch.** { *; }
-dontwarn com.jcraft.jsch.**
-keepclassmembers class com.tdmodules.network.TunnelManager {
    public <methods>;
}
-keepclassmembers class com.tdmodules.network.NetworkUtils {
    public <methods>;
}
