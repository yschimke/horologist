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

package com.google.android.horologist.buildlogic.weardevices.impl.test.strategy

import com.android.build.api.instrumentation.manageddevice.DeviceTestRunParameters
import com.android.build.gradle.internal.LoggerWrapper
import com.android.ddmlib.testrunner.IInstrumentationResultParser
import com.google.android.horologist.buildlogic.weardevices.AdbDisconnect
import com.google.android.horologist.buildlogic.weardevices.impl.test.DeviceTestRunInput
import com.google.android.horologist.buildlogic.weardevices.impl.test.adb.AdbHolder
import com.malinskiy.adam.request.shell.v2.ShellCommandRequest

class InputSuspendStrategy : SyncTestRunStrategy() {
    val adbDisconnect = AdbDisconnect.InputSuspend

    suspend fun checkAndConfigure(adb: AdbHolder) {
        adbDisconnect.disconnect(adb)
    }

    suspend fun cleanupAndWaitForResults(adb: AdbHolder) {
        adbDisconnect.reconnect(adb)
    }

    override suspend fun launchTests(
        adb: AdbHolder,
        params: DeviceTestRunParameters<DeviceTestRunInput>,
        logger: LoggerWrapper,
        parser: IInstrumentationResultParser,
    ) {
        checkAndConfigure(adb)

        launchTestsSync(
            adb = adb,
            testRunData = params.testRunData,
            parser = parser,
        )

        cleanupAndWaitForResults(adb)
    }
}
