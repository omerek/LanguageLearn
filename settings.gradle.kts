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

// `-PcoreOnly` leaves out :app so the algorithms and content tests in :core
// run on machines without the Android SDK.
if (!coreOnly) {
    include(":app")
}
