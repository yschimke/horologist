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

import com.google.android.horologist.buildlogic.weardevices.AdbDisconnect
import com.google.android.horologist.buildlogic.weardevices.TestRunMode
import com.google.android.horologist.buildlogic.weardevices.WearDevice

plugins {
    id("com.android.application")
    kotlin("android")
    id("com.google.devtools.ksp")
    id("dagger.hilt.android.plugin")
    id("weardevices")
}

android {
    compileSdk = 34

    defaultConfig {
        applicationId = "com.google.android.horologist.ai.sample.prompt"
        // Min because of Tiles
        minSdk = 30
        targetSdk = 33

        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
        }
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

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
        animationsDisabled = true
        managedDevices {
            devices {
                register("emulatorSync", WearDevice::class.java) {
                    serial = "emulator-5554"
                    runMode = TestRunMode.NormalSync
                }
                register("emulatorAsync", WearDevice::class.java) {
                    serial = "emulator-5554"
                    runMode = TestRunMode.NormalAsync(adbDisconnect = AdbDisconnect.Simulate)
                }
                register("pixelWatch2Suspend", WearDevice::class.java) {
                    serial = "3B111JEAVL001J"
                    runMode = TestRunMode.InputSuspend
                }
                register("pixelWatch2Async", WearDevice::class.java) {
                    serial = "3B111JEAVL001J"
                    runMode = TestRunMode.NormalAsync(adbDisconnect = AdbDisconnect.UsbControl(port = 4, location = "1-2", uhubctl = File("/usr/local/google/home/yschimke/workspacesda/uhubctl/uhubctl")))
                }
                register("pixelWatch2Manual", WearDevice::class.java) {
                    serial = "3B111JEAVL001J"
                    runMode = TestRunMode.Manual
                }
            }
        }
    }

    namespace = "com.google.android.horologist.ai.sample.prompt"
}

dependencies {
    api(projects.annotations)

    implementation(projects.ai.sample.wearCore)

    implementation(projects.ai.ui)
    implementation(projects.ai.sample.core)
    implementation(projects.composables)
    implementation(projects.composeLayout)
    implementation(projects.composeMaterial)

    implementation(libs.dagger.hiltandroid)
    implementation(libs.androidx.wear.input)
    ksp(libs.dagger.hiltandroidcompiler)
    implementation(libs.hilt.navigationcompose)

    implementation(projects.datalayer.core)
    implementation(projects.datalayer.grpc)
    implementation(projects.datalayer.watch)
    implementation(libs.kotlinx.coroutines.playservices)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.complications.data)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.wear)
    implementation(libs.compose.foundation.foundation)
    implementation(libs.compose.material.iconscore)
    implementation(libs.compose.material.iconsext)
    implementation(libs.compose.ui.toolingpreview)
    implementation(libs.kotlin.stdlib)
    implementation(libs.wearcompose.material)
    implementation(libs.wearcompose.foundation)
    implementation(libs.wearcompose.navigation)
    implementation(libs.androidx.tracing.ktx)
    implementation(libs.androidx.tracing.perfetto)

    implementation(libs.mikepenz.markdown)

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

    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.androidx.test.ext.ktx)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.kotlinx.coroutines.guava)

    androidTestImplementation(projects.benchmarkTools)
}
