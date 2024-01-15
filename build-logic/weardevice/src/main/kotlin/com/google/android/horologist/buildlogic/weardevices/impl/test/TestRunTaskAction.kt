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
import com.google.android.horologist.buildlogic.weardevices.TestRunMode
import com.google.android.horologist.buildlogic.weardevices.impl.test.adb.installApk
import com.google.android.horologist.buildlogic.weardevices.impl.test.adb.launchTestsAsync
import com.google.android.horologist.buildlogic.weardevices.impl.test.adb.launchTestsSync
import com.google.android.horologist.buildlogic.weardevices.impl.test.strategy.DryRunStrategy
import com.google.android.horologist.buildlogic.weardevices.impl.test.strategy.InputSuspendStrategy
import com.google.android.horologist.buildlogic.weardevices.impl.test.strategy.ManualTestRunStrategy
import com.google.android.horologist.buildlogic.weardevices.impl.test.strategy.TestRunStrategy
import com.google.android.horologist.buildlogic.weardevices.impl.util.DeviceConfigProvider
import com.malinskiy.adam.AndroidDebugBridgeClientFactory
import com.malinskiy.adam.request.misc.FetchHostFeaturesRequest
import com.malinskiy.adam.request.prop.GetPropRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.gradle.work.DisableCachingByDefault


@DisableCachingByDefault
open class TestRunTaskAction : DeviceTestRunTaskAction<DeviceTestRunInput> {
    override fun runTests(params: DeviceTestRunParameters<DeviceTestRunInput>): Boolean {
        val adb = AndroidDebugBridgeClientFactory().build()

        val strategy = findStrategy(params.deviceInput.runMode.get())

        val testData = params.testRunData.testData
        val serial = params.deviceInput.serial.get()

        return runBlocking(Dispatchers.Default) {
            val supportedFeatures = adb.execute(
                request = FetchHostFeaturesRequest(), serial = serial
            )

            val props = adb.execute(
                request = GetPropRequest(), serial = serial
            )

            val apks = testData.testedApkFinder.invoke(DeviceConfigProvider(props))

            apks.forEach {
                adb.installApk(
                    apk = it, serial = serial, supportedFeatures = supportedFeatures
                )
            }
            adb.installApk(
                apk = testData.testApk, serial = serial, supportedFeatures = supportedFeatures
            )

            if (strategy.sync) {
                launchTestsSync(
                    adb = adb,
                    testRunData = params.testRunData,
                    strategy = strategy,
                    supportedFeatures = supportedFeatures,
                    serial = serial,
                    coroutineScope = this,
                    logger = LoggerWrapper.getLogger(TestRunTaskAction::class.java)
                )
            } else {
                launchTestsAsync(
                    adb = adb,
                    testRunData = params.testRunData,
                    strategy = strategy,
                    supportedFeatures = supportedFeatures,
                    serial = serial,
                    logger = LoggerWrapper.getLogger(TestRunTaskAction::class.java),
                )
            }
        }
    }

    private fun findStrategy(runMode: TestRunMode): TestRunStrategy = when (runMode) {
        TestRunMode.Manual -> ManualTestRunStrategy()
        TestRunMode.InputSuspend -> InputSuspendStrategy()
        TestRunMode.SyncDryRun -> DryRunStrategy(sync = true)
        TestRunMode.AsyncDryRun -> DryRunStrategy(sync = false)
    }
}
