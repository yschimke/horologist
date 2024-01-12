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

package com.google.android.horologist.buildlogic.weardevices.impl.adb

import com.android.build.api.instrumentation.manageddevice.TestRunData
import com.android.build.gradle.internal.LoggerWrapper
import com.android.build.gradle.internal.testing.CustomTestRunListener
import com.android.ddmlib.testrunner.RemoteAndroidTestRunner
import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.request.Feature
import com.malinskiy.adam.request.pkg.StreamingPackageInstallRequest
import com.malinskiy.adam.request.testrunner.InstrumentOptions
import com.malinskiy.adam.request.testrunner.TestAssumptionFailed
import com.malinskiy.adam.request.testrunner.TestFailed
import com.malinskiy.adam.request.testrunner.TestRunFailed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.subscribe
import java.io.File

internal suspend fun AndroidDebugBridgeClient.launchTests(
    supportedFeatures: List<Feature>,
    outputLogPath: String? = null,
    serial: String?,
    coroutineScope: CoroutineScope,
    testRunData: TestRunData,
    logger: LoggerWrapper
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
    val parser = mode.createInstrumentationResultParser(testRunData.testRunId, listOf(resultsListener))

    execute(
        request = ProtoTestRunnerRequest(
            testPackage = testRunData.testData.applicationId,
            runnerClass = testRunData.testData.instrumentationRunner,
            instrumentOptions = InstrumentOptions(),
            supportedFeatures = supportedFeatures,
            coroutineScope = coroutineScope,
            parser = parser
        ),
        serial = serial
    ).consumeAsFlow().collect()

    return !resultsListener.runResult.isRunFailure
}

internal suspend fun AndroidDebugBridgeClient.installApk(
    apk: File,
    serial: String?,
    supportedFeatures: List<Feature>
) {
    val success = execute(
        StreamingPackageInstallRequest(
            pkg = apk,
            supportedFeatures = supportedFeatures,
            reinstall = false,
            extraArgs = emptyList()
        ),
        serial = serial
    )
    if (!success) {
        throw Exception("APK ${apk} installation failed")
    }
}