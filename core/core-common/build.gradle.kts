plugins {
    id("tickr.kmp.library")
}

kotlin {
    android {
        namespace = "dev.madina.tickr.core.common"
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutinesCore)
        }
    }
}
