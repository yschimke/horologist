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
import com.malinskiy.adam.request.shell.v1.ShellCommandResult
import com.malinskiy.adam.request.shell.v1.SyncShellCommandRequest
import com.malinskiy.adam.request.testrunner.InstrumentOptions

class AsyncProtoTestRunnerRequest(
    private val testPackage: String,
    private val instrumentOptions: InstrumentOptions,
    private val runnerClass: String,
    private val userId: Int? = null,
    private val abi: String? = null,
    private val outputLogPath: String,
) : SyncShellCommandRequest<Unit>(
    cmd = StringBuilder().apply {
        // https://android.googlesource.com/platform/frameworks/base/+/master/cmds/am/src/com/android/commands/am/Instrument.java#226
        append("am instrument -m -r")

        if (userId != null) {
            append(" --user $userId")
        }

        if (abi != null) {
            append(" --abi $abi")
        }

//        if (protobuf) {
        append(" -m")

        append(" -f $outputLogPath")

        // https://stackoverflow.com/questions/33896315/how-to-retrieve-test-results-when-using-adb-shell-am-instrument
        append(" -e listener com.google.android.horologist.networks.ResultsWriter")

        append(instrumentOptions.toString())

        append(" $testPackage/$runnerClass")
    }.toString()
) {
    override fun convertResult(response: ShellCommandResult) {
        println(response.output)
        return Unit
    }
}