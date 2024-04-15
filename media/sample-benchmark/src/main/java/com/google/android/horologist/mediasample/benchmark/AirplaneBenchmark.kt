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

@file:OptIn(ExperimentalMetricApi::class, ExperimentalPerfettoCaptureApi::class)

package com.google.android.horologist.mediasample.benchmark

import android.os.BatteryManager
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.Metric
import androidx.benchmark.macro.PowerMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.benchmark.perfetto.ExperimentalPerfettoCaptureApi
import androidx.benchmark.perfetto.PerfettoConfig
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.horologist.media.benchmark.Util.executeShellScript
import com.google.android.horologist.mediasample.benchmark.TestMedia.MediaSampleApp
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okio.ByteString.Companion.decodeHex
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
                BatteryManager::class.java
            )
        },
        perfettoConfig = PerfettoConfig.Binary(
            "0a060880800220010a050880202001121b0a190a15616e64726f69642e7061636b616765735f6c697374100112210a1f0a136c696e75782e70726f636573735f73746174731001ba0605100120904e121e0a1c0a0d616e64726f69642e706f776572d2060a0864100210011003180112d3030ad0030a0c6c696e75782e6674726163651000a206bc030a117461736b2f7461736b5f6e65777461736b0a107461736b2f7461736b5f72656e616d650a1873636865642f73636865645f70726f636573735f657869740a1873636865642f73636865645f70726f636573735f667265650a186d6d5f6576656e742f6d6d5f6576656e745f7265636f72640a0d6b6d656d2f7273735f737461740a146b6d656d2f696f6e5f686561705f736872696e6b0a126b6d656d2f696f6e5f686561705f67726f770a0c696f6e2f696f6e5f737461740a186f6f6d2f6f6f6d5f73636f72655f61646a5f7570646174650a046469736b0a157566732f7566736863645f636c6b5f676174696e670a1e6c6f776d656d6f72796b696c6c65722f6c6f776d656d6f72795f6b696c6c1202616d120664616c76696b1204667265711203676678120368616c120469646c651205696e7075741205706f77657212057363686564120473796e631204766965771202776d1a29636f6d2e676f6f676c652e616e64726f69642e686f726f6c6f676973742e6d6564696173616d706c651a33636f6d2e676f6f676c652e616e64726f69642e686f726f6c6f676973742e6d6564696173616d706c652e62656e63686d61726b62020801400148882768904eb801c413".decodeHex()
                .toByteArray()
        ),
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