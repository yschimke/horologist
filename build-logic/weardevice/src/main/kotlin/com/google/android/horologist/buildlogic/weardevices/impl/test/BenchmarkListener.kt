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

package com.google.android.horologist.buildlogic.weardevices.impl.test

import com.android.ddmlib.testrunner.ITestRunListener
import com.android.ddmlib.testrunner.TestIdentifier
import com.google.android.horologist.buildlogic.weardevices.impl.test.adb.AdbHolder
import com.malinskiy.adam.request.sync.ListFilesRequest
import com.malinskiy.adam.request.sync.v2.PullFileRequest
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import java.io.File

class BenchmarkListener(
    val additionalTestOutput: File,
    val adb: AdbHolder
) : ITestRunListener {
    override fun testRunStarted(runName: String?, testCount: Int) {

    }

    override fun testStarted(test: TestIdentifier?) {
    }

    override fun testFailed(test: TestIdentifier?, trace: String?) {
    }

    override fun testAssumptionFailure(test: TestIdentifier?, trace: String?) {
    }

    override fun testIgnored(test: TestIdentifier?) {
    }

    override fun testEnded(test: TestIdentifier, testMetrics: MutableMap<String, String>) {
//        testMetrics.forEach { k, v ->
//            if (!(k.endsWith("logcat") || k.endsWith(".benchmark"))) {
//                println("$k: $v")
//            }
//        }

        // https://cs.android.com/android-studio/platform/tools/base/+/mirror-goog-studio-main:utp/android-test-plugin-host-additional-test-output/src/main/java/com/android/tools/utp/plugins/host/additionaltestoutput/AndroidAdditionalTestOutputPlugin.kt?q=android.studio.display.benchmark

        val benchmark = testMetrics["android.studio.display.benchmark"]
        val benchmarkv2 = testMetrics["android.studio.v2display.benchmark"]
        val outputDirPath = testMetrics["android.studio.v2display.benchmark.outputDirPath"]!!

        println(benchmark)

        runBlocking {
            copyAdditionalOutput(outputDirPath, additionalTestOutput)
        }

        val logcat = testMetrics["com.android.ddmlib.testrunner.logcat"]

        if (logcat != null) {
//            println(logcat)
        }
    }

    private suspend fun copyAdditionalOutput(outputDirPath: String, additionalTestOutput: File) {
        val files = adb.execute(ListFilesRequest(outputDirPath))
        coroutineScope {
            files.forEach {
                val output = additionalTestOutput.resolve(it.name)
                println("Fetching: " + it.name + " to " + output)
                val progress = adb.execute(
                    PullFileRequest(
                        it.directory + "/" + it.name,
                        output,
                        adb.supportedFeatures
                    ),
                    this
                )
                progress.collect()
            }
        }
    }

    override fun testRunFailed(errorMessage: String?) {
    }

    override fun testRunStopped(elapsedTime: Long) {
    }

    override fun testRunEnded(elapsedTime: Long, runMetrics: MutableMap<String, String>?) {
    }
}