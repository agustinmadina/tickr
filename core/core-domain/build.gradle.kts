plugins {
    id("tickr.kmp.library")
}

kotlin {
    android {
        namespace = "dev.madina.tickr.core.domain"
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutinesCore)
        }

        commonTest.dependencies {
            implementation(libs.kotlinx.coroutinesTest)
        }
    }
}
