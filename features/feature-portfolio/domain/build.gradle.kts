plugins {
    id("tickr.kmp.library")
}

kotlin {
    android {
        namespace = "dev.madina.tickr.feature.portfolio.domain"
    }

    sourceSets {
        commonMain.dependencies {
            // core-domain only. No Koin, no Ktor, no Compose: this module must stay framework-free,
            // and the build graph is what guarantees it.
            api(project(":core:core-domain"))
        }

        commonTest.dependencies {
            implementation(libs.kotlinx.coroutinesTest)
        }
    }
}
