import android.buildFeatures.compose
import android.buildFeatures.kapt
import android.buildFeatures.buildConfig
import android.extensions.configureAndroid
import android.extensions.configureAndroidExtensions
import android.extensions.configureAndroidLibrary
import android.extensions.configureKotlin
import android.extensions.configureKotlinAndroid
import android.extensions.configureKotlinSerialization
import android.extensions.configureKotlinTooling
import android.extensions.configureProguard
import android.extensions.configureTest
import android.extensions.configureTestAndroidJunit
import android.extensions.configureTestAndroidJunit5
import android.extensions.configureTestAndroidManifest
import android.extensions.configureTestKotlin
import android.extensions.configureTestKotlinJunit4
import com.google.devtools.ksp.kspOptions
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.repository

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

android {
    compileSdk = 35
    minSdk = 26

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    defaultConfig {
        applicationId = "com.tpa.app"
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "BASE_URL", "\"https://replace-me.vercel.app/\"")
    }

    signingConfig = signingConfigs.getByName("debug")

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.9.22"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

androidExtensions {
    isExperimental = true
}

ksp {
    kspOptions {
        option("ksp.stableApiMode", "true")
        option("ksp.disableErrorProneChecks", "true")
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.03.03"))
    implementation("androidx.activity:activity-compose:1.7.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.material:material3:1.2.1")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.9.0")
    implementation("com.squareup.retrofit2:converter-kotlinx-json:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0") {
        because("MPAndroidChart for candlestick chart")
    }
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-cbor:1.5.2")
    ksp("androidx.compose.runtime:compose-ksp:2024.03.03")
    ksp("androidx.compose.ui:ui-tooling-ksp:2024.03.03")
    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.test.ext:junit:1.1.5")
    testImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}

repository("https://jitpack.io") {
    mavenContent {
        includeGroup("com.github.PhilJay")
    }
}