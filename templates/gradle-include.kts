// ============================================================================
// Инструкция по подключению TDModules в Gradle-проекты
// (Tdroid, TDRobotics, TDnetScript, UniTest, SuperDriver)
// ============================================================================

// 💡 ВАЖНО: Подключайте ТОЛЬКО ТЕ модули, которые реально нужны вашему приложению!
// Не нужно подключать всё подряд.

// Вариант 1: Подключение через includeBuild в settings.gradle.kts (РЕКОМЕНДУЕМЫЙ)
// В settings.gradle.kts объявите только нужные substitution:
/*
includeBuild("../TDModules") {
    dependencySubstitution {
        // Подключайте ТОЛЬКО то, что используется:
        substitute(module("com.tdmodules:core-security")).using(project(":core-security"))
        substitute(module("com.tdmodules:core-network")).using(project(":core-network"))
        substitute(module("com.tdmodules:core-ai")).using(project(":core-ai"))
        substitute(module("com.tdmodules:core-education")).using(project(":core-education"))
        // substitute(module("com.tdmodules:core-tdscript")).using(project(":core-tdscript")) // если нужен скриптинг
    }
}
*/

// А в build.gradle.kts вашего app/модуля добавьте только нужные зависимости:
/*
dependencies {
    implementation("com.tdmodules:core-security") // если нужно шифрование
    // implementation("com.tdmodules:core-network") // если нужны туннели
    // implementation("com.tdmodules:core-ai")      // если нужен ИИ
    // implementation("com.tdmodules:core-education") // если нужны тесты/словари
    // implementation("com.tdmodules:core-tdscript")  // если нужен язык TDscript
}
*/

// Вариант 2: Прямое добавление только нужного исходного кода в sourceSets (build.gradle.kts):
/*
android {
    sourceSets {
        getByName("main").java.srcDirs(
            "../TDModules/modules/core-security/src/main/kotlin" // только нужные папки!
        )
    }
}
*/
