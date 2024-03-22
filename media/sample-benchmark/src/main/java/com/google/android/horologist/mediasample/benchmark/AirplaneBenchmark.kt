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

@file:OptIn(ExperimentalMetricApi::class)

package com.google.android.horologist.mediasample.benchmark

import android.os.BatteryManager
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.Metric
import androidx.benchmark.macro.PowerMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.horologist.media.benchmark.Util.executeShellScript
import com.google.android.horologist.mediasample.benchmark.TestMedia.MediaSampleApp
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class AirplaneBenchmark {

    private lateinit var batteryManager: BatteryManager

    @get:Rule
    public val benchmarkRule: MacrobenchmarkRule = MacrobenchmarkRule()

    @Test
    public fun startup(): Unit = benchmarkRule.measureRepeated(
        packageName = MediaSampleApp.packageName,
        metrics = metrics(),
        compilationMode = CompilationMode.Partial(warmupIterations = 0),
        iterations = 1,
        setupBlock = {
            if (IncludePower) {
                device.executeShellScript("echo 1 > /d/google_charger/input_suspend")
            }

            device.executeShellScript("settings put global airplane_mode_on 1")
            device.executeShellScript("am broadcast -a android.intent.action.AIRPLANE_MODE")

            batteryManager = InstrumentationRegistry.getInstrumentation().context.getSystemService(
                BatteryManager::class.java)
        },
    ) {
        check(iteration != null) {
            "Iteration $iteration"
        }

        if (IncludePower) {
            check(!batteryManager.isCharging) {
                "Should not be charging"
            }


            println(device.executeShellScript("dumpsys battery"))
        }

        runBlocking {
            delay(25.seconds)

            repeat(25) {
                if (IncludePower) {
                    println(device.executeShellScript("dumpsys battery"))
                }
                delay(1.minutes)
            }

            delay(1.seconds)
        }

        if (IncludePower) {
            device.executeShellScript("echo 0 > /d/google_charger/input_suspend")
        }

        device.executeShellScript("settings put global airplane_mode_on 0")
        device.executeShellScript("am broadcast -a android.intent.action.AIRPLANE_MODE")
    }

    public open fun metrics(): List<Metric> = buildList {
//        add(FrameTimingMetric())
        if (IncludePower) {
            add(PowerMetric(type = PowerMetric.Type.Battery()))
        }
    }

    public companion object {
        public val IncludePower: Boolean = true
    }
}