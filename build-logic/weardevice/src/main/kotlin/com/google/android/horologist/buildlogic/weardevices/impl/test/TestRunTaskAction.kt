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

@file:Suppress("UnstableApiUsage")

package com.google.android.horologist.buildlogic.weardevices.impl.test

import com.android.build.api.instrumentation.manageddevice.DeviceTestRunParameters
import com.android.build.api.instrumentation.manageddevice.DeviceTestRunTaskAction
import com.android.build.gradle.internal.LoggerWrapper
import com.android.build.gradle.internal.testing.CustomTestRunListener
import com.android.ddmlib.testrunner.RemoteAndroidTestRunner
import com.google.android.horologist.buildlogic.weardevices.TestRunMode
import com.google.android.horologist.buildlogic.weardevices.impl.test.adb.AdbHolder
import com.google.android.horologist.buildlogic.weardevices.impl.test.adb.installApk
import com.google.android.horologist.buildlogic.weardevices.impl.test.strategy.InputSuspendStrategy
import com.google.android.horologist.buildlogic.weardevices.impl.test.strategy.ManualTestRunStrategy
import com.google.android.horologist.buildlogic.weardevices.impl.test.strategy.NormalAsyncStrategy
import com.google.android.horologist.buildlogic.weardevices.impl.test.strategy.NormalSyncStrategy
import com.google.android.horologist.buildlogic.weardevices.impl.test.strategy.TestRunStrategy
import com.google.android.horologist.buildlogic.weardevices.impl.util.DeviceConfigProvider
import com.malinskiy.adam.request.prop.GetPropRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.gradle.work.DisableCachingByDefault


@DisableCachingByDefault
open class TestRunTaskAction : DeviceTestRunTaskAction<DeviceTestRunInput> {
    override fun runTests(params: DeviceTestRunParameters<DeviceTestRunInput>): Boolean {
        // TODO output dir
        // https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:benchmark/gradle-plugin/src/main/kotlin/androidx/benchmark/gradle/BenchmarkReportTask.kt;l=113

        val testRunData = params.testRunData
        val testData = testRunData.testData
        val serial = params.deviceInput.serial.get()

        val strategy = findStrategy(params.deviceInput.runMode.get())

        val adb = AdbHolder(serial)

        return runBlocking(Dispatchers.Default) {
            adb.connect()

            val props = adb.execute(
                request = GetPropRequest()
            )

            val apks = testData.testedApkFinder.invoke(DeviceConfigProvider(props))

            apks.forEach {
                adb.installApk(
                    apk = it
                )
            }
            adb.installApk(
                apk = testData.testApk
            )

            val logger = LoggerWrapper.getLogger(TestRunTaskAction::class.java)

            val mode = RemoteAndroidTestRunner.StatusReporterMode.PROTO_STD
            val resultsListener = CustomTestRunListener(
                testRunData.deviceName, testRunData.projectPath, testRunData.variantName, logger
            )
            resultsListener.setReportDir(testRunData.outputDirectory.asFile)
            resultsListener.setHostName(adb.adb.host.hostName)
            val parser =
                mode.createInstrumentationResultParser(testRunData.testRunId, listOf(resultsListener))

            strategy.launchTests(adb, params, logger, parser)

            check(resultsListener.runResult.isRunComplete) { "Run not complete" }
            check(resultsListener.runResult.numCompleteTests > 0) { "No tests" }

            resultsListener.runResult.hasFailedTests().not()
        }
    }

    private fun findStrategy(runMode: TestRunMode): TestRunStrategy = when (runMode) {
        TestRunMode.Manual -> ManualTestRunStrategy()
        TestRunMode.InputSuspend -> InputSuspendStrategy()
        TestRunMode.NormalSync -> NormalSyncStrategy()
        is TestRunMode.NormalAsync -> NormalAsyncStrategy(runMode.adbDisconnect)
    }
}
