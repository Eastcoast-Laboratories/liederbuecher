// Root Gradle build script for kultliederbuch
plugins {
    kotlin("multiplatform") version "1.9.23" apply false
    id("com.android.application") version "8.2.2" apply false
    id("com.android.library") version "8.2.2" apply false
    id("com.squareup.sqldelight") version "2.0.1" apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
