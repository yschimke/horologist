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

import com.android.build.api.instrumentation.manageddevice.TestRunData
import com.android.build.gradle.internal.LoggerWrapper
import com.android.ddmlib.testrunner.IInstrumentationResultParser
import com.google.android.horologist.buildlogic.weardevices.impl.test.adb.AdbHolder
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest
import com.malinskiy.adam.request.sync.v2.PullFileRequest
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import java.io.File

abstract class AsyncTestRunStrategy: TestRunStrategy {

    internal suspend fun launchTestsAsync(
        adb: AdbHolder,
        testRunData: TestRunData,
        logger: LoggerWrapper,
        parser: IInstrumentationResultParser,
        instrumentOptions: String? = null,
        checkAndConfigure: suspend (AdbHolder) -> Unit = {},
        cleanupAndWaitForResults: suspend (AdbHolder) -> Unit = {}
    ) {
        checkAndConfigure(adb)

        val outputLogPath: String = java.util.UUID.randomUUID().toString()

        val cmd =
            "am instrument -r -m -f $outputLogPath ${instrumentOptions.orEmpty()} ${testRunData.testData.applicationId}/${testRunData.testData.instrumentationRunner}"
        val withNohup = "nohup $cmd </dev/null >/dev/null 2>/dev/null &\n echo launched"
        adb.execute(ShellCommandRequest(withNohup))

        cleanupAndWaitForResults(adb)

        val tmpFile = File.createTempFile("protoTestOutput", "pb")

        try {
            coroutineScope {
                adb.execute(
                    PullFileRequest("/sdcard/$outputLogPath", tmpFile, adb.supportedFeatures), this
                ).collect()
            }

            val bytes = tmpFile.readBytes()
            parser.addOutput(bytes, 0, bytes.size)
        } finally {
            tmpFile.delete()
        }
    }
}