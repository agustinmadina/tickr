plugins {
    id("tickr.kmp.library")
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    android {
        namespace = "dev.madina.tickr.feature.portfolio.data"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":features:feature-portfolio:domain"))
            implementation(project(":core:core-common"))
            implementation(project(":core:core-network"))
            implementation(project(":core:core-storage"))
            implementation(libs.koin.core)
            implementation(libs.kermit)
            implementation(libs.kotlinx.serialization.json)
        }

        commonTest.dependencies {
            implementation(libs.kotlinx.coroutinesTest)
            implementation(libs.ktor.client.mock)
            implementation(libs.multiplatform.settings.test)
        }
    }
}
