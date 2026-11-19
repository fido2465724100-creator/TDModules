// ============================================================================
// Инструкция по подключению TDModules в Gradle-проекты
// (Tdroid, TDRobotics, TDnetScript, UniTest, SuperDriver)
// ============================================================================

// Вариант 1: Подключение через includeBuild в settings.gradle.kts (РЕКОМЕНДУЕМЫЙ)
// В файле settings.gradle.kts вашего проекта добавьте:
/*
includeBuild("../TDModules") {
    dependencySubstitution {
        substitute(module("com.tdmodules:core-security")).using(project(":core-security"))
        substitute(module("com.tdmodules:core-network")).using(project(":core-network"))
        substitute(module("com.tdmodules:core-ai")).using(project(":core-ai"))
        substitute(module("com.tdmodules:core-education")).using(project(":core-education"))
    }
}
*/

// Вариант 2: Прямое добавление исходного кода в sourceSets (build.gradle.kts приложения)
/*
android {
    sourceSets {
        getByName("main").java.srcDirs(
            "../TDModules/modules/core-security/src/main/kotlin",
            "../TDModules/modules/core-network/src/main/kotlin",
            "../TDModules/modules/core-ai/src/main/kotlin",
            "../TDModules/modules/core-education/src/main/kotlin"
        )
    }
}
*/

// Необходимые внешние зависимости в build.gradle.kts вашего проекта:
/*
dependencies {
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")

    // SSH Reverse Tunnels (для core-network)
    implementation("com.github.mwiede:jsch:0.2.16") // либо com.jcraft:jsch:0.1.55
}
*/
