plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":core-security"))
    implementation("com.github.mwiede:jsch:0.2.16")
}
