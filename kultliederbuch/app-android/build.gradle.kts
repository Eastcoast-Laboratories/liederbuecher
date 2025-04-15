import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget

plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "de.kultliederbuch.android"
    compileSdk = 34
    defaultConfig {
        applicationId = "de.kultliederbuch.android"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation(project(":shared"))
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.compose.ui:ui:1.6.1")
    implementation("androidx.compose.material3:material3:1.2.0")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
}
