/*
 * Copyright 2022 The Android Open Source Project
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

import com.google.android.horologist.buildlogic.weardevices.TestRunMode
import com.google.android.horologist.buildlogic.weardevices.WearDevice

plugins {
    id("com.android.test")
    kotlin("android")
    id("weardevices")
//    id("com.google.firebase.testlab")
}

android {
    namespace = "com.google.android.horologist.ai.sample.prompt.benchmark"
    compileSdk = 34

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    defaultConfig {
        minSdk = 30
        targetSdk = 33

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["androidx.benchmark.fullTracing.enable"] = "true"
    }

    buildTypes {
        // This benchmark buildType is used for benchmarking, and should function like your
        // release build (for example, with minification on). It's signed with a debug key
        // for easy local/CI testing.
        create("benchmark") {
            isDebuggable = true
            signingConfig = getByName("debug").signingConfig

            matchingFallbacks.add("release")
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
                    runMode = TestRunMode.NormalAsync
                }
                register("pixelWatch2Suspend", WearDevice::class.java) {
                    serial = "3B111JEAVL001J"
                    runMode = TestRunMode.InputSuspend
                }
                register("pixelWatch2Manual", WearDevice::class.java) {
                    serial = "3B111JEAVL001J"
                    runMode = TestRunMode.Manual
                }
            }
        }
//        firebaseTestLab {
//            managedDevices {
//                pixelWatch {
//                    device = "r11"
//                    apiLevel = 30
//                }
//            }
//        }
    }

    targetProjectPath = ":ai:sample:wear-prompt-app"
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

dependencies {
    api(projects.annotations)

    implementation(projects.benchmarkTools)

    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.benchmark.junit4)
    implementation(libs.androidx.test.ext.ktx)
    implementation(libs.espresso.core)
    implementation(libs.androidx.test.uiautomator)
    implementation(libs.kotlinx.coroutines.android)
}

//tasks.withType<Test>().configureEach {
//    println(name)
//    forkEvery = 0
//}

androidComponents {
    beforeVariants(selector().all()) {
        it.enable = it.buildType == "benchmark"
    }
}
