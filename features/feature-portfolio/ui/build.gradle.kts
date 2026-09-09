plugins {
    id("tickr.kmp.compose")
}

kotlin {
    android {
        namespace = "dev.madina.tickr.feature.portfolio.ui"
        androidResources {
            enable = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            // domain and core-ui only. There is deliberately no dependency on :data, so a screen
            // cannot reach an implementation even by accident: it would not compile.
            implementation(project(":features:feature-portfolio:domain"))
            implementation(project(":core:core-ui"))
            implementation(libs.kotlinx.coroutinesCore)
        }

        commonTest.dependencies {
            implementation(libs.kotlinx.coroutinesTest)
        }
    }
}
