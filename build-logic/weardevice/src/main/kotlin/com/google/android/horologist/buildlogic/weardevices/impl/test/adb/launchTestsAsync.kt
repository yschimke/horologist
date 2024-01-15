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
import com.malinskiy.adam.request.Feature
import com.malinskiy.adam.request.sync.v2.PullFileRequest
import com.malinskiy.adam.request.testrunner.InstrumentOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import java.io.File
import java.time.LocalTime

internal suspend fun AndroidDebugBridgeClient.launchTestsAsync(
    outputLogPath: String,
    supportedFeatures: List<Feature>,
    serial: String?,
    coroutineScope: CoroutineScope,
    testRunData: TestRunData,
    logger: LoggerWrapper,
    strategy: TestRunStrategy
): Boolean {
    val mode = RemoteAndroidTestRunner.StatusReporterMode.PROTO_STD
    val resultsListener = CustomTestRunListener(
        testRunData.deviceName,
        testRunData.projectPath,
        testRunData.variantName,
        logger
    )
    resultsListener.setReportDir(testRunData.outputDirectory.asFile)
    resultsListener.setHostName(host.hostName)
    val parser =
        mode.createInstrumentationResultParser(testRunData.testRunId, listOf(resultsListener))

    println("" + LocalTime.now() + "Before")
    val flow = execute(
        request = AsyncProtoTestRunnerRequest(
            testPackage = testRunData.testData.applicationId,
            runnerClass = testRunData.testData.instrumentationRunner,
            instrumentOptions = InstrumentOptions(),
            outputLogPath = outputLogPath,
            strategy = strategy,
            supportedFeatures = supportedFeatures,
            coroutineScope = coroutineScope,
        ),
        serial = serial
    ).consumeAsFlow()
    println("" + LocalTime.now() + "After flow")

    flow.first()

    println("" + LocalTime.now() + "After first")

    strategy.waitForResults()

    println("" + LocalTime.now() + "After results")

    val tmpFile = File.createTempFile("sdfdfssdf", "sdffsdd")

    execute(
        PullFileRequest("/sdcard/$outputLogPath", tmpFile, supportedFeatures),
        coroutineScope,
        serial
    ).consumeAsFlow().collect {
        println(it)
    }

    println(tmpFile.length())

    val bytes = tmpFile.readBytes()
    parser.addOutput(bytes, 0, bytes.size)

    return !resultsListener.runResult.isRunFailure
}