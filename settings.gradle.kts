rootProject.name = "tickr"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    // Convention plugins (tickr.kmp.library, tickr.kmp.compose) live in this composite build.
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
}

include(":androidApp")
include(":shared")

// Core: domain-agnostic infrastructure. Nothing here may name a business concept.
include(":core:core-common")
include(":core:core-domain")
include(":core:core-network")
include(":core:core-ui")

// Features: one user-facing slice each, split so the build enforces the dependency direction.
include(":features:feature-portfolio:domain")
include(":features:feature-portfolio:data")
include(":features:feature-portfolio:ui")
include(":features:feature-portfolio:di")
