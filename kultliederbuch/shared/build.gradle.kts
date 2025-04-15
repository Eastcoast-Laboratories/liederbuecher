plugins {
    kotlin("multiplatform")
    id("com.squareup.sqldelight")
}

kotlin {
    android()
    ios()
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("com.squareup.sqldelight:runtime:2.0.1")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("com.squareup.sqldelight:android-driver:2.0.1")
            }
        }
        val iosMain by getting {
            dependencies {
                implementation("com.squareup.sqldelight:native-driver:2.0.1")
            }
        }
    }
}

sqldelight {
    databases {
        create("KultliederbuchDatabase") {
            packageName.set("de.kultliederbuch.shared.db")
        }
    }
}
