import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.repository

plugins {
    id("org.jetbrains.kotlin.gradle.plugin.serialization") version "1.9.22" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "1.9.22" apply false
    id("com.android.application") version "7.4.2" apply false
    id("com.google.devtools.ksp") version "1.9.22" apply false
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = "MyInvestinmentAssistant"