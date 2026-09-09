import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    id("tickr.kmp.compose")
}

kotlin {
    android {
        namespace = "dev.madina.tickr.shared"
        androidResources {
            enable = true
        }
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
            implementation(project(":core:core-common"))
            implementation(project(":core:core-ui"))
            implementation(project(":features:feature-portfolio:ui"))
            // The aggregator is the one place allowed to see a feature's di module.
            implementation(project(":features:feature-portfolio:di"))
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.kotlinx.coroutinesCore)
            implementation(libs.kotlinx.collections.immutable)
        }

        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }
    }
}
