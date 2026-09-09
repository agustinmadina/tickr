plugins {
    id("tickr.kmp.compose")
}

kotlin {
    android {
        namespace = "dev.madina.tickr.core.ui"
        androidResources {
            enable = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.androidx.lifecycle.viewmodelCompose)
            api(libs.androidx.lifecycle.runtimeCompose)
            api(libs.koin.compose)
            api(libs.koin.compose.viewmodel)
            api(libs.kotlinx.collections.immutable)
            implementation(libs.kotlinx.coroutinesCore)
        }
    }
}
