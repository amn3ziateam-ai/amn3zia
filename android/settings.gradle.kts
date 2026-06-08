pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io") // tdlib prebuilt aar (e.g. com.github.tdlib:td)
    }
}

rootProject.name = "AMN3ZIA"
include(":app")
