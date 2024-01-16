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

package com.google.android.horologist.buildlogic.weardevices.impl.test.strategy

import com.android.build.api.instrumentation.manageddevice.DeviceTestRunParameters
import com.android.build.gradle.internal.LoggerWrapper
import com.android.ddmlib.testrunner.IInstrumentationResultParser
import com.google.android.horologist.buildlogic.weardevices.impl.test.DeviceTestRunInput
import com.google.android.horologist.buildlogic.weardevices.impl.test.adb.AdbHolder
import com.malinskiy.adam.exception.RequestRejectedException
import com.malinskiy.adam.request.Feature
import com.malinskiy.adam.request.shell.v2.ShellCommandRequest
import com.malinskiy.adam.request.sync.v2.StatFileRequest
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

class ManualTestRunStrategy : AsyncTestRunStrategy() {

    suspend fun checkAndConfigure() {
    }

    suspend fun cleanupAndWaitForResults(
        adb: AdbHolder
    ) {
        println("Disconnect now")
        println("Waiting for disconnection")

        while (adb.isConnected()) {
            println(".")
            delay(1.seconds)
        }
        adb.close()

        println("Disconnected")

        println("Waiting for connection")

        while (true) {
            println(".")
            try {
                adb.connect()
                if (adb.isConnected()) {
                    break
                }
            } catch (e: RequestRejectedException) {

            }

            delay(1.seconds)
        }

        println("Connected")
    }

    override suspend fun launchTests(
        adb: AdbHolder,
        params: DeviceTestRunParameters<DeviceTestRunInput>,
        logger: LoggerWrapper,
        parser: IInstrumentationResultParser,
    ) {
        launchTestsAsync(
            adb = adb,
            testRunData = params.testRunData,
            logger = logger,
            parser = parser,
            instrumentOptions = "-e listener com.google.android.horologist.benchmark.tools.RunWhileOnBatteryListener",
            checkAndConfigure = { checkAndConfigure() },
            cleanupAndWaitForResults = { cleanupAndWaitForResults(it) }
        )
    }
}
