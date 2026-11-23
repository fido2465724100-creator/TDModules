// ============================================================================
// Справка по подключению TDModules в Gradle-проекты
// ============================================================================

// TDModules — это модульный репозиторий. Выбирайте любые модули, 
// которые требуются вашему проекту:
// - core-security   (шифрование AES-GCM-256, RSA-OAEP, маскирование строк, RateLimiter)
// - core-network    (SSH-туннели, сокетный Heartbeat, Direct LAN)
// - core-ai         (ротация бесплатных LLM, PromptSanitizer, DeviceProfiler)
// - core-education  (Concept-Pivot словари, полиморфные тесты, SM-2 SRS)
// - core-tdscript   (язык TDscript Core 1.0.1, Host SPI, Capabilities)

// Вариант подключения через includeBuild в settings.gradle.kts:
/*
includeBuild("../TDModules") {
    dependencySubstitution {
        substitute(module("com.tdmodules:core-security")).using(project(":core-security"))
        substitute(module("com.tdmodules:core-network")).using(project(":core-network"))
        substitute(module("com.tdmodules:core-ai")).using(project(":core-ai"))
        substitute(module("com.tdmodules:core-education")).using(project(":core-education"))
        substitute(module("com.tdmodules:core-tdscript")).using(project(":core-tdscript"))
    }
}
*/

// В build.gradle.kts приложения подключайте нужные вам зависимости:
/*
dependencies {
    // Берите любые нужные модули:
    implementation("com.tdmodules:core-security")
    implementation("com.tdmodules:core-network")
    implementation("com.tdmodules:core-ai")
    implementation("com.tdmodules:core-education")
    implementation("com.tdmodules:core-tdscript")
}
*/
