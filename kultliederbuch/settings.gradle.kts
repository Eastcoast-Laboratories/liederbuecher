pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        google()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

// Settings for kultliederbuch multiplatform project
rootProject.name = "kultliederbuch"
include(":app-android")
include(":shared")
