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
import com.android.ddmlib.testrunner.IInstrumentationResultParser
import com.google.android.horologist.buildlogic.weardevices.impl.test.adb.AdbHolder
import com.malinskiy.adam.request.Feature
import com.malinskiy.adam.request.shell.AsyncCompatShellCommandRequest
import com.malinskiy.adam.request.shell.v2.ShellCommandResultChunk
import com.malinskiy.adam.request.testrunner.InstrumentOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow

abstract class SyncTestRunStrategy: TestRunStrategy {

    internal suspend fun launchTestsSync(
        adb: AdbHolder,
        testRunData: TestRunData,
        parser: IInstrumentationResultParser
    ) {
        coroutineScope {
            adb.execute(
                SyncProtoTestRunnerRequest(
                    testPackage = testRunData.testData.applicationId,
                    runnerClass = testRunData.testData.instrumentationRunner,
                    instrumentOptions = InstrumentOptions(),
                    coroutineScope = this,
                    supportedFeatures = adb.supportedFeatures,
                    parser = parser
                ),
            ).consumeAsFlow().collect()
        }
    }

    class SyncProtoTestRunnerRequest(
        testPackage: String,
        instrumentOptions: InstrumentOptions,
        runnerClass: String,
        private val parser: IInstrumentationResultParser,
        supportedFeatures: List<Feature>,
        coroutineScope: CoroutineScope,
    ) : AsyncCompatShellCommandRequest<Unit>(
        cmd = "am instrument -w -r -m $instrumentOptions $testPackage/$runnerClass",
        supportedFeatures = supportedFeatures,
        coroutineScope = coroutineScope,
        socketIdleTimeout = Long.MAX_VALUE,
    ) {

        override suspend fun convertChunk(response: ShellCommandResultChunk): Unit? {
            return response.stdout?.let { bytes ->
                parser.addOutput(bytes, 0, bytes.size)
            }
        }

        override suspend fun close(channel: SendChannel<Unit>) {
            channel.send(Unit)
        }
    }
}