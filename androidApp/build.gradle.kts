import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("tickr.ktlint")
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "dev.madina.tickr"
    compileSdk =
        libs.versions.android.compileSdk
            .get()
            .toInt()

    defaultConfig {
        applicationId = "dev.madina.tickr"
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.android.targetSdk
                .get()
                .toInt()
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

dependencies {
    implementation(project(":shared"))
    // Directly, not transitively: the Application hands Android's Context to the storage layer,
    // which is the one thing common code cannot do for itself.
    implementation(project(":core:core-storage"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
}
