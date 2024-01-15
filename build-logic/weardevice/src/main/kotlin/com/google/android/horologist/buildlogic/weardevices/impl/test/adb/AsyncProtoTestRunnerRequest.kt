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
import com.google.android.horologist.buildlogic.weardevices.impl.test.strategy.TestRunStrategy
import com.malinskiy.adam.request.Feature
import com.malinskiy.adam.request.shell.AsyncCompatShellCommandRequest
import com.malinskiy.adam.request.shell.v1.ShellCommandResult
import com.malinskiy.adam.request.shell.v1.SyncShellCommandRequest
import com.malinskiy.adam.request.shell.v2.ShellCommandResultChunk
import com.malinskiy.adam.request.testrunner.InstrumentOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.SendChannel

class AsyncProtoTestRunnerRequest(
    private val testPackage: String,
    private val instrumentOptions: InstrumentOptions,
    private val runnerClass: String,
    private val userId: Int? = null,
    private val abi: String? = null,
    private val outputLogPath: String,
    private val strategy: TestRunStrategy,
    supportedFeatures: List<Feature>,
    coroutineScope: CoroutineScope,
    socketIdleTimeout: Long? = Long.MAX_VALUE,
) : AsyncCompatShellCommandRequest<Unit>(
    cmd = StringBuilder().apply {
        // am instrument -r -m -f TestRunTaskAction -e listener com.google.android.horologist.benchmark.tools.RunWhileOnBatteryListener com.google.android.horologist.network.awareness.test/androidx.test.runner.AndroidJUnitRunner

        // Inspired by
        // https://android.googlesource.com/platform/frameworks/base/+/master/cmds/am/src/com/android/commands/am/Instrument.java#226

        // raw mode
        append("am instrument -r")

        if (userId != null) {
            append(" --user $userId")
        }

        if (abi != null) {
            append(" --abi $abi")
        }

        // use protobuf
        append(" -m")

        // Write to a log to read from later
        append(" -f $outputLogPath")

        // https://stackoverflow.com/questions/33896315/how-to-retrieve-test-results-when-using-adb-shell-am-instrument
        strategy.instrumentOptions?.let {
            append(" ")
            append(it)
        }

        append(instrumentOptions.toString())

        append(" $testPackage/$runnerClass")
    }.toString(),
    supportedFeatures = supportedFeatures,
    coroutineScope = coroutineScope,
    socketIdleTimeout = socketIdleTimeout,
) {
    override suspend fun convertChunk(response: ShellCommandResultChunk): Unit? {
        return Unit
    }

    override suspend fun close(channel: SendChannel<Unit>) {
        channel.send(Unit)
    }
}