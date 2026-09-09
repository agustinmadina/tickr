plugins {
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    androidLibrary {
        namespace = "com.example.app.feature.{{FEATURE_PACKAGE}}.di"
        minSdk = libs.versions.android.minSdk.get().toInt()
        compileSdk = libs.versions.android.compileSdk.get().toInt()

        @Suppress("UnstableApiUsage")
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    iosArm64()
    iosSimulatorArm64()

    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            implementation(project(":features:feature-{{FEATURE_NAME}}:domain"))
            implementation(project(":features:feature-{{FEATURE_NAME}}:data"))
            implementation(project(":features:feature-{{FEATURE_NAME}}:ui"))
            implementation(project(":core:core-data"))

            implementation(libs.koin.core)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.kotlinx.coroutinesCore)
        }
    }
}
