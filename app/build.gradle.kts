plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "dev.oxide.overlay"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.oxide.overlay"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1-reference"
    }
}

android.buildTypes.getByName("release").isMinifyEnabled = false
android.compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
kotlin { jvmToolchain(17) }
