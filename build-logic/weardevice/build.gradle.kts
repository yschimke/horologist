/*
 * Copyright 2024 The Android Open Source Project
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

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java-gradle-plugin")
    `kotlin-dsl`
}

group = "com.google.android.horologist.buildlogic.weardevice"

// Configure the build-logic plugins to target JDK 17
// This matches the JDK used to build the project, and is not related to what is running on device.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }
}

gradlePlugin {
    plugins {
        register("weardevices") {
            id = "weardevices"
            implementationClass = "com.google.android.horologist.buildlogic.weardevices.WearDevicePlugin"
        }
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.android.tools.common)
    compileOnly(libs.kotlin.gradlePlugin)
    implementation("com.malinskiy.adam:adam:0.5.2")
    implementation("com.android.tools.build:builder-test-api:8.2.1")
    implementation("com.android.tools.ddms:ddmlib:31.2.1")
    implementation(libs.kotlinx.coroutines.core)
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}