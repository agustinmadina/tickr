plugins {
    id("tickr.kmp.library")
}

kotlin {
    android {
        namespace = "dev.madina.tickr.feature.portfolio.di"
    }

    sourceSets {
        commonMain.dependencies {
            // The only module that sees all three siblings. This is where implementations are bound
            // to interfaces, and the reason no other module needs to.
            implementation(project(":features:feature-portfolio:domain"))
            implementation(project(":features:feature-portfolio:data"))
            implementation(project(":features:feature-portfolio:ui"))
            implementation(project(":core:core-common"))
            implementation(libs.koin.core)
        }
    }
}
