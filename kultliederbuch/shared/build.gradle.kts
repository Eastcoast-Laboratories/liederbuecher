plugins {
    kotlin("multiplatform")
    id("com.squareup.sqldelight") version "1.5.5"
    id("com.android.library")
}

kotlin {
    android()
    ios()
    jvmToolchain(21)
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("com.squareup.sqldelight:runtime:1.5.5")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("com.squareup.sqldelight:android-driver:1.5.5")
            }
        }
        val iosMain by getting {
            dependencies {
                implementation("com.squareup.sqldelight:native-driver:1.5.5")
            }
        }
    }
}

sqldelight {
    database("KultliederbuchDatabase") {
        packageName = "de.kultliederbuch.shared.db"
    }
}

android {
    namespace = "de.kultliederbuch.shared"
    compileSdk = 34
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdk = 24
        targetSdk = 34
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
