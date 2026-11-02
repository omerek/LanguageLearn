pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

val coreOnly = providers.gradleProperty("coreOnly").isPresent

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        if (coreOnly) {
            // The plugin portal mirrors Maven Central; it helps on machines that
            // are rate-limited by Central or cannot reach Google's Maven.
            gradlePluginPortal()
        } else {
            google()
        }
        mavenCentral()
    }
}

rootProject.name = "LanguageLearn"

include(":core")

// `-PcoreOnly` builds only the pure-Kotlin module. Useful on machines
// without the Android SDK (the algorithms and content tests live in :core).
if (!coreOnly) {
    include(":app")
}
