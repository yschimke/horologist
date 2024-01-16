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
import com.malinskiy.adam.request.shell.v2.ShellCommandRequest
import com.malinskiy.adam.request.sync.v2.StatFileRequest
import kotlinx.coroutines.delay
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class NormalAsyncStrategy : AsyncTestRunStrategy() {

    private val estimatedRunTime: Duration = 1.seconds
    val uuid = UUID.randomUUID().toString()

    val markerFile = "/sdcard/marker.file.$uuid"

    suspend fun checkAndConfigure(
        adb: AdbHolder
    ) {
        adb.execute(ShellCommandRequest("rm $markerFile"))
        adb.execute(StatFileRequest(markerFile, adb.supportedFeatures))
    }

    suspend fun cleanupAndWaitForResults(
        adb: AdbHolder
    ) {
        delay(estimatedRunTime)
        adb.connect()

        for (index in 0 until 10) {
            if (adb.execute(
                    request = StatFileRequest(
                        remotePath = markerFile, supportedFeatures = adb.supportedFeatures
                    )
                ).exists()) break
            delay(1000)
        }
    }

    override suspend fun launchTests(
        adb: AdbHolder,
        params: DeviceTestRunParameters<DeviceTestRunInput>,
        logger: LoggerWrapper,
        parser: IInstrumentationResultParser,
    ) {
        launchTestsAsync(adb = adb,
            testRunData = params.testRunData,
            logger = logger,
            parser = parser,
            instrumentOptions = "-e listener com.google.android.horologist.benchmark.tools.MarkCompletionListener -e marker $markerFile -e signal $uuid",
            checkAndConfigure = { checkAndConfigure(it) },
            cleanupAndWaitForResults = { cleanupAndWaitForResults(it) })
    }
}