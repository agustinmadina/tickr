import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Every KMP module in this build applies this instead of repeating the target list. The three
// targets are the point of the project, so they are declared in one place: a module that silently
// drops wasmJs would break the web demo without failing anything locally.
//
// The module still declares its own `namespace`, which is the one value that cannot be shared.

plugins {
    id("tickr.ktlint")
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.kotlin.multiplatform")
}

private val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

private fun version(alias: String): Int = libs.findVersion(alias).get().requiredVersion.toInt()

kotlin {
    // Provisioned rather than inherited. Only jvmTarget was set, so the build compiled against
    // whatever JDK happened to be running it and a contributor on anything below 21 got a compile
    // error instead of a download.
    jvmToolchain(21)

    android {
        minSdk = version("android-minSdk")
        compileSdk = version("android-compileSdk")

        @Suppress("UnstableApiUsage")
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }

        // Enables the androidHostTest source set so tests run on the JVM for the Android target.
        withHostTestBuilder { }
    }

    iosArm64()
    iosSimulatorArm64()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets.commonTest.dependencies {
        implementation(libs.findLibrary("kotlin-test").get())
        implementation(libs.findLibrary("kotest-framework-engine").get())
        implementation(libs.findLibrary("kotest-assertions-core").get())
    }

    // Specs live in commonTest and execute on the Android host JVM, which is what CI runs. Kotest
    // needs a JUnit 5 runner to be discovered there; the native and wasm test targets compile the
    // same specs but do not execute them, which would require the Kotest KSP plugin to generate
    // entry points.
    // Resolved by name: `withHostTestBuilder` creates this source set, so there is no generated
    // accessor for it at the time this convention plugin is compiled.
    sourceSets.named("androidHostTest") {
        dependencies {
            implementation(libs.findLibrary("kotest-runner-junit5").get())
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
