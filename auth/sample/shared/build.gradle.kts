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

import com.google.protobuf.gradle.id

plugins {
    id("com.android.library")
    id("org.jetbrains.dokka")
    id("com.google.protobuf")
    id("dagger.hilt.android.plugin")
    id("com.google.devtools.ksp")
    kotlin("android")
}

android {
    compileSdk = 34

    defaultConfig {
        minSdk = 21

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        buildConfig = false
    }

    kotlinOptions {
        jvmTarget = "11"
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
    }

    lint {
        checkReleaseBuilds = false
        textReport = true
    }

    resourcePrefix = "horologist_"

    namespace = "com.google.android.horologist.auth.sample.shared"
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.27.1"
    }
    plugins {
        create("javalite") {
            artifact = "com.google.protobuf:protoc-gen-javalite:3.0.0"
        }
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.64.0"
        }
        create("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:1.3.0:jdk8@jar"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java") {
                    option("lite")
                }
                create("kotlin") {
                    option("lite")
                }
            }
            task.plugins {
                create("grpc") {
                    option("lite")
                }
                create("grpckt") {
                    option("lite")
                }
            }
        }
    }
}

dependencies {
    api(projects.annotations)
    api(projects.datalayer.grpc)

    implementation(libs.androidx.corektx)
    implementation(libs.androidx.datastore)
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.protobuf.kotlin.lite)
    api(libs.webauthn2.json.serialization)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.com.squareup.okhttp3.okhttp)
    implementation(libs.com.squareup.okhttp3.logging.interceptor)
    api(libs.androidx.credentials)
    api(libs.googleid)
    api(libs.moshi.kotlin)
    api(libs.androidx.credentials.play.services.auth)
    implementation(libs.playservices.auth)

    implementation(libs.dagger.hiltandroid)
    implementation(libs.androidx.lifecycle.process)
    ksp(libs.dagger.hiltandroidcompiler)
    implementation(libs.hilt.navigationcompose)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.androidx.test.ext.ktx)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
}

// tasks.maybeCreate("prepareKotlinIdeaImport")
//    .dependsOn("generateDebugProto")
