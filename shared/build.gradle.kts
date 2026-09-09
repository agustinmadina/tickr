import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    android {
        namespace = "dev.madina.tickr.shared"
        minSdk = libs.versions.android.minSdk.get().toInt()
        compileSdk = libs.versions.android.compileSdk.get().toInt()

        androidResources {
            enable = true
        }

        @Suppress("UnstableApiUsage")
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }

        withHostTestBuilder { }
    }

    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
            // Without it the linker warns and falls back to the bundle name.
            binaryOption("bundleId", "dev.madina.tickr.shared")
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName.set("tickr")
        browser {
            commonWebpackConfig {
                outputFileName = "tickr.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            // The `compose.*` accessors are deprecated in favour of explicit coordinates, but each
            // Compose Multiplatform artifact is on its own version train (material3 ships
            // 1.12.0-alphaNN while runtime ships 1.12.0), so pinning them by hand means maintaining
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
            implementation(libs.kotlinx.coroutinesCore)
            implementation(libs.kotlinx.collections.immutable)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }
    }
}
