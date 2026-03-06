import java.net.URI

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    @Suppress("UnstableApiUsage")
    repositories {
        google()
        mavenCentral()
        maven {
            url = URI("https://jitpack.io")
        }
    }
}

rootProject.name = "MedTimer"
include(":app")
include(":core:designsystem")
include(":core:ui")
include(":core:testing")
include(":core:database")
include(":core:domain")
include(":core:preferences")
include(":feature:statistics")
