pluginManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

include(":app")
enablePlugin("com.google.devtools.ksp") // For Kotlin Symbol Processing
enablePlugin("com.android.application")
enablePlugin("org.jetbrains.kotlin.plugin.compose")
enablePlugin("org.jetbrains.kotlin.plugin.serialization")

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}