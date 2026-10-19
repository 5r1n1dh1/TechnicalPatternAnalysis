import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.repository

plugins {
    id("org.jetbrains.kotlin.gradle.plugin.serialization") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    id("com.android.application") version "8.5.2" apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.27" apply false
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = "TechnicalPatternAnalysis"