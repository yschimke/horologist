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
    id("com.android.library")
    id("org.jetbrains.dokka")

    id("com.google.devtools.ksp")
    id("me.tylerbwong.gradle.metalava")
    id("weardevices")
    kotlin("android")
}

android {
    compileSdk = 34

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs = freeCompilerArgs + "-opt-in=kotlin.RequiresOptIn"
        freeCompilerArgs = freeCompilerArgs + "-opt-in=com.google.android.horologist.annotations.ExperimentalHorologistApi"
    }

    packaging {
        resources {
            excludes +=
                listOf(
                    "/META-INF/AL2.0",
                    "/META-INF/LGPL2.1",
                )
        }
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

    lint {
        checkReleaseBuilds = false
        textReport = true
        // https://buganizer.corp.google.com/issues/328279054
        disable.add("UnsafeOptInUsageError")
    }

    namespace = "com.google.android.horologist.network.awareness"
}

project.tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    // Workaround for https://youtrack.jetbrains.com/issue/KT-37652
    if (!this.name.endsWith("TestKotlin") && !this.name.startsWith("compileDebug")) {
        this.kotlinOptions {
            freeCompilerArgs = freeCompilerArgs + "-Xexplicit-api=strict"
        }
    }
}

metalava {
    sourcePaths.setFrom("src/main")
    filename.set("api/current.api")
    reportLintsAsErrors.set(true)
}

dependencies {
    api(projects.annotations)

    api(libs.kotlin.stdlib)
    api(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.tracing.ktx)
    implementation(libs.androidx.core)
    implementation(libs.kotlinx.coroutines.guava)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.androidx.test.ext.ktx)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.test.espressocore)
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.androidx.test.ext.ktx)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.kotlinx.coroutines.guava)

    androidTestImplementation(projects.benchmarkTools)
}

tasks.withType<org.jetbrains.dokka.gradle.DokkaTaskPartial>().configureEach {
    dokkaSourceSets {
        configureEach {
            moduleName.set("network-awareness-core")
        }
    }
}

apply(plugin = "com.vanniktech.maven.publish")
