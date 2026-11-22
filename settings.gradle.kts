pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        google()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        google()
    }
}

rootProject.name = "TDModules"

include(":core-security")
project(":core-security").projectDir = file("modules/core-security")

include(":core-network")
project(":core-network").projectDir = file("modules/core-network")

include(":core-ai")
project(":core-ai").projectDir = file("modules/core-ai")

include(":core-education")
project(":core-education").projectDir = file("modules/core-education")

include(":core-tdscript")
project(":core-tdscript").projectDir = file("modules/core-tdscript")

include(":core-localization")
project(":core-localization").projectDir = file("modules/core-localization")


