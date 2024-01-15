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
import com.android.ddmlib.testrunner.IInstrumentationResultParser
import com.malinskiy.adam.request.Feature
import com.malinskiy.adam.request.shell.AsyncCompatShellCommandRequest
import com.malinskiy.adam.request.shell.v2.ShellCommandResultChunk
import com.malinskiy.adam.request.testrunner.InstrumentOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.SendChannel

class SyncProtoTestRunnerRequest(
    private val testPackage: String,
    private val instrumentOptions: InstrumentOptions,
    private val runnerClass: String,
    private val userId: Int? = null,
    private val abi: String? = null,
    private val parser: IInstrumentationResultParser,
    supportedFeatures: List<Feature>,
    coroutineScope: CoroutineScope,
    socketIdleTimeout: Long? = Long.MAX_VALUE,
) : AsyncCompatShellCommandRequest<Unit>(
    cmd = StringBuilder().apply {
        // https://android.googlesource.com/platform/frameworks/base/+/master/cmds/am/src/com/android/commands/am/Instrument.java#226
        append("am instrument -w -r")

        if (userId != null) {
            append(" --user $userId")
        }

        if (abi != null) {
            append(" --abi $abi")
        }

        // Use protobuf
        append(" -m")

        // https://stackoverflow.com/questions/33896315/how-to-retrieve-test-results-when-using-adb-shell-am-instrument
//        append(" -e listener com.google.android.horologist.networks.ResultsWriter")

        append(instrumentOptions.toString())

        append(" $testPackage/$runnerClass")
    }.toString(),
    supportedFeatures = supportedFeatures,
    coroutineScope = coroutineScope,
    socketIdleTimeout = socketIdleTimeout,
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
