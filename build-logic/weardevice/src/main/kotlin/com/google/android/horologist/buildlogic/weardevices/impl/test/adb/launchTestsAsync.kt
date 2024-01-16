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

package com.google.android.horologist.buildlogic.weardevices.impl.test.adb

import com.android.build.api.instrumentation.manageddevice.TestRunData
import com.android.build.gradle.internal.LoggerWrapper
import com.android.build.gradle.internal.testing.CustomTestRunListener
import com.android.ddmlib.testrunner.RemoteAndroidTestRunner
import com.google.android.horologist.buildlogic.weardevices.impl.test.strategy.TestRunStrategy
import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.AndroidDebugBridgeClientFactory
import com.malinskiy.adam.request.Feature
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest
import com.malinskiy.adam.request.sync.v2.PullFileRequest
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import java.io.File

internal suspend fun launchTestsAsync(
    adb: AndroidDebugBridgeClient,
    supportedFeatures: List<Feature>,
    serial: String?,
    testRunData: TestRunData,
    logger: LoggerWrapper,
    strategy: TestRunStrategy,
): Boolean {
    var activeAdb = adb

    strategy.checkAndConfigure(activeAdb)

    val outputLogPath: String = java.util.UUID.randomUUID().toString()

    val mode = RemoteAndroidTestRunner.StatusReporterMode.PROTO_STD
    val resultsListener = CustomTestRunListener(
        testRunData.deviceName, testRunData.projectPath, testRunData.variantName, logger
    )
    resultsListener.setReportDir(testRunData.outputDirectory.asFile)
    resultsListener.setHostName(activeAdb.host.hostName)
    val parser =
        mode.createInstrumentationResultParser(testRunData.testRunId, listOf(resultsListener))

    val cmd =
        "am instrument -r -m -f $outputLogPath ${strategy.instrumentOptions.orEmpty()} ${testRunData.testData.applicationId}/${testRunData.testData.instrumentationRunner}"
    val withNohup = "nohup $cmd </dev/null >/dev/null 2>/dev/null &\n echo launched"
    activeAdb.execute(ShellCommandRequest(withNohup), serial = serial)

    activeAdb.close()

    strategy.waitForResults(activeAdb)

    activeAdb = AndroidDebugBridgeClientFactory().build()

    val tmpFile = File.createTempFile("protoTestOutput", "pb")

    try {
        coroutineScope {
            activeAdb.execute(
                PullFileRequest("/sdcard/$outputLogPath", tmpFile, supportedFeatures), this, serial
            ).consumeAsFlow().collect()
        }

        val bytes = tmpFile.readBytes()
        parser.addOutput(bytes, 0, bytes.size)
    } finally {
        tmpFile.delete()
    }

    resultsListener.runResult.testResults.forEach { t, u ->
        println(t)
        println(u.status)
    }

    return !resultsListener.runResult.hasFailedTests()
}

