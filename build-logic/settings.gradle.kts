// Composite build hosting the convention plugins. Included by the root settings.gradle.kts via
// pluginManagement { includeBuild("build-logic") }, so every module can apply them by id.
rootProject.name = "build-logic"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    // Reuse the main build's catalog so plugin versions live in exactly one place.
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
