plugins {
    id("tickr.kmp.library")
}

kotlin {
    android {
        namespace = "dev.madina.tickr.core.storage"
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.multiplatform.settings)
        }

        commonTest.dependencies {
            implementation(libs.multiplatform.settings.test)
        }
    }
}
