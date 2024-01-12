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

package com.google.android.horologist.buildlogic.weardevices.impl

import com.android.build.api.instrumentation.StaticTestData
import com.android.build.api.instrumentation.manageddevice.DeviceTestRunParameters
import com.android.build.api.instrumentation.manageddevice.DeviceTestRunTaskAction
import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.AndroidDebugBridgeClientFactory
import com.malinskiy.adam.request.Feature
import com.malinskiy.adam.request.misc.FetchHostFeaturesRequest
import com.malinskiy.adam.request.pkg.InstallRemotePackageRequest
import com.malinskiy.adam.request.pkg.StreamingPackageInstallRequest
import com.malinskiy.adam.request.shell.v2.ShellCommandRequest
import com.malinskiy.adam.request.sync.v2.PushFileRequest
import com.malinskiy.adam.request.testrunner.InstrumentOptions
import com.malinskiy.adam.request.testrunner.TestEvent
import com.malinskiy.adam.request.testrunner.TestRunnerRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

open class TestRunTaskAction : DeviceTestRunTaskAction<DeviceTestRunInput> {

    override fun runTests(params: DeviceTestRunParameters<DeviceTestRunInput>): Boolean {
        // TODO run tests

        println("TestRunTaskAction ${params}")

        val adb = AndroidDebugBridgeClientFactory()
            .build()

        val testData = params.testRunData.testData
        val serial = params.deviceInput.serial.get()

        val results = runBlocking(Dispatchers.Default) {
            installAndRunTests(adb, serial, testData).also {
                // TODO find out why needed
                println(coroutineContext.job.children.toList())
                coroutineContext.cancelChildren()
            }
        }

        return true
    }

    private suspend fun CoroutineScope.installAndRunTests(
        adb: AndroidDebugBridgeClient,
        serial: String?,
        testData: StaticTestData
    ): List<TestEvent> {

        val supportedFeatures = adb.execute(FetchHostFeaturesRequest(), serial = serial)

        println(supportedFeatures)

        install(testData, adb, serial, supportedFeatures)

        println("Executing")
        val channel = adb.execute(
            request = TestRunnerRequest(
                testPackage = testData.applicationId,
                runnerClass = testData.instrumentationRunner,
                instrumentOptions = InstrumentOptions(
                    pkg = listOf("com.google.android.horologist.networks")
                ),
                supportedFeatures = supportedFeatures,
                coroutineScope = this,
            ),
            serial = serial
        )
        println("Executed")

        return withTimeout(10.seconds) { channel.receive() }
    }

    private suspend fun install(
        testData: StaticTestData,
        adb: AndroidDebugBridgeClient,
        serial: String?,
        supportedFeatures: List<Feature>
    ) {
        coroutineScope {
            val copyAndInstall = false
            if (copyAndInstall) {
                println("Copying ${testData.testApk}")
                val remoteFile = "/data/local/tmp/${testData.testApk.name}"

                val resultCopy = adb.execute(
                    PushFileRequest(
                        local = testData.testApk,
                        remotePath = remoteFile,
                        supportedFeatures = supportedFeatures
                    ),
                    this,
                    serial = serial
                )
                val completion = resultCopy.consumeAsFlow().last()
                if (completion != 1.0) {
                    throw Exception("Incomplete APK copy")
                }

                println("Installing ${testData.testApk}")
                val result = adb.execute(
                    InstallRemotePackageRequest(
                        absoluteRemoteFilePath = remoteFile,
                        reinstall = false,
                    ),
                    serial = serial
                )
                if (result.exitCode != 0) {
                    throw Exception("Install failed (${result.exitCode}): ${result.output}")
                }

                adb.execute(ShellCommandRequest("rm $remoteFile"), serial = serial)
            } else {
                println("Streaming install")
                val success = adb.execute(
                    StreamingPackageInstallRequest(
                        pkg = testData.testApk,
                        supportedFeatures = supportedFeatures,
                        reinstall = false,
                        extraArgs = emptyList()
                    ),
                    serial = serial
                )
            }
        }
    }
}