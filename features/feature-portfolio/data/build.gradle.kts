plugins {
    id("tickr.kmp.library")
}

kotlin {
    android {
        namespace = "dev.madina.tickr.feature.portfolio.data"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":features:feature-portfolio:domain"))
            implementation(project(":core:core-common"))
            implementation(libs.koin.core)
        }

        commonTest.dependencies {
            implementation(libs.kotlinx.coroutinesTest)
        }
    }
}
