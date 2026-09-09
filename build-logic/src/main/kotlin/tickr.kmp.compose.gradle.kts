// For modules that render: core-ui, every feature's ui/, and the shared aggregator. Adds Compose
// on top of the base target set, with the same dependency list everywhere so a screen can move
// between modules without its imports breaking.

plugins {
    id("tickr.kmp.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    sourceSets.commonMain.dependencies {
        // The `compose.*` accessors are deprecated in favour of explicit coordinates, but each
        // Compose Multiplatform artifact is on its own version train (material3 ships
        // 1.12.0-alphaNN while runtime ships 1.12.0), so pinning them by hand would mean tracking
        // several release calendars. The plugin resolves each one correctly.
        @Suppress("DEPRECATION")
        api(compose.runtime)
        @Suppress("DEPRECATION")
        api(compose.foundation)
        @Suppress("DEPRECATION")
        api(compose.material3)
        @Suppress("DEPRECATION")
        api(compose.ui)
        @Suppress("DEPRECATION")
        api(compose.components.resources)
    }
}
