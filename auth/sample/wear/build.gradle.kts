import java.util.Properties

/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
    id("com.android.application")
    kotlin("android")
    id("com.google.devtools.ksp")
    id("dagger.hilt.android.plugin")
    kotlin("plugin.serialization")
}

val localProperties = Properties().apply {
    val localPropertiesFile = project.rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        load(localPropertiesFile.inputStream())
    }
}

android {
    compileSdk = 34

    defaultConfig {
        applicationId = "com.google.android.horologist.auth.sample"
        // Min because of Tiles
        minSdk = 26
        targetSdk = 34

        versionCode = 1
        versionName = "1.0"

        buildConfigField(
            "String",
            "GSI_CLIENT_ID",
            "\"" + localProperties["gsiclientid"] + "\"",
        )

        buildConfigField(
            "String",
            "OAUTH_DEVICE_GRANT_CLIENT_ID",
            "\"" + localProperties["OAUTH_DEVICE_GRANT_CLIENT_ID"] + "\"",
        )

        buildConfigField(
            "String",
            "OAUTH_DEVICE_GRANT_CLIENT_SECRET",
            "\"" + localProperties["OAUTH_DEVICE_GRANT_CLIENT_SECRET"] + "\"",
        )

        buildConfigField(
            "String",
            "OAUTH_PKCE_CLIENT_ID",
            "\"" + localProperties["OAUTH_PKCE_CLIENT_ID"] + "\"",
        )

        buildConfigField(
            "String",
            "OAUTH_PKCE_CLIENT_SECRET",
            "\"" + localProperties["OAUTH_PKCE_CLIENT_SECRET"] + "\"",
        )


        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        getByName("debug") {
            keyAlias = "androiddebugkey"
            keyPassword = "android"
            storeFile = file("../debug.keystore")
            storePassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )

            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    kotlinOptions {
        jvmTarget = "11"

        // Allow for widescale experimental APIs in Alpha libraries we build upon
        freeCompilerArgs = freeCompilerArgs +
            listOf(
                "-opt-in=com.google.android.horologist.annotations.ExperimentalHorologistApi",
            )
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }

    @Suppress("UnstableApiUsage")
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
        animationsDisabled = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1,INDEX.LIST,DEPENDENCIES}"
        }
    }

    lint {
        // https://buganizer.corp.google.com/issues/328279054
        disable.add("UnsafeOptInUsageError")
    }

    namespace = "com.google.android.horologist.auth.sample"
}

dependencies {
    api(projects.annotations)

    implementation(projects.auth.composables)
    implementation(projects.auth.data)
    implementation(projects.auth.dataWatchOauth)
    implementation(projects.auth.sample.shared)
    implementation(projects.auth.providerGoogle)
    implementation(projects.auth.ui)
    implementation(projects.composables)
    implementation(projects.composeLayout)
    implementation(projects.composeMaterial)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.complications.data)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.wear)
    implementation(libs.compose.foundation.foundation)
    implementation(libs.compose.material.iconscore)
    implementation(libs.compose.material.iconsext)
    implementation(libs.compose.ui.toolingpreview)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlin.stdlib)
    implementation(libs.wearcompose.material)
    implementation(libs.wearcompose.foundation)
    implementation(libs.wearcompose.navigation)

    implementation(libs.com.squareup.okhttp3.logging.interceptor)
    implementation(libs.com.squareup.okhttp3.okhttp)
    implementation(libs.kotlinx.coroutines.playservices)
    implementation(libs.moshi.kotlin)
    implementation(libs.playservices.auth)
    implementation(libs.playservices.wearable)
    implementation(libs.androidx.navigation.runtime)
    implementation(libs.google.api.client)
    implementation(libs.google.api.client.android)
    implementation(libs.google.api.services.drive)

    implementation(libs.kotlinx.serialization.core)
    implementation(libs.androidx.wear.remote.interactions)
    implementation(libs.androidx.wear.phone.interactions)

    implementation(libs.dagger.hiltandroid)
    ksp(libs.dagger.hiltandroidcompiler)
    implementation(libs.hilt.navigationcompose)

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.androidx.wear.tooling.preview)
    debugImplementation(projects.composeTools)
    releaseCompileOnly(projects.composeTools)

    testImplementation(libs.androidx.navigation.testing)
    testImplementation(libs.androidx.test.espressocore)
    testImplementation(libs.compose.ui.test)
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.robolectric)
}
