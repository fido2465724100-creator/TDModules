plugins {
    kotlin("jvm") version "1.9.24" apply false
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    dependencies {
        val implementation by configurations
        val testImplementation by configurations

        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
        testImplementation(kotlin("test"))
        testImplementation("junit:junit:4.13.2")
    }

    tasks.withType<Test> {
        useJUnit()
    }
}
