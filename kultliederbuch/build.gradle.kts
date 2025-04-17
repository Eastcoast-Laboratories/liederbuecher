// Root Gradle build script for kultliederbuch
plugins {
    kotlin("multiplatform") version "1.9.23" apply false
    id("com.android.application") version "8.9.1" apply false
    id("com.android.library") version "8.9.1" apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
