plugins {
    id("com.android.application")
}

android {
    namespace = "com.chainloom.game"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.chainloom.game"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
