/*
 * Copyright 2022 The Android Open Source Project
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

package com.google.android.horologist.mediasample.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test

@LargeTest
class StartupBenchmark {

    @get:Rule
    public val benchmarkRule: MacrobenchmarkRule = MacrobenchmarkRule()

    @Test
    public fun startup(): Unit = benchmarkRule.measureRepeated(
        packageName = "com.google.android.horologist.ai.sample.prompt",
        metrics = listOf(StartupTimingMetric(), Wear4PowerMetric()),
        compilationMode = CompilationMode.None(),
        iterations = 1,
        startupMode = StartupMode.WARM,
    ) {
        startActivityAndWait()
        // sleep to allow time for report fully drawn
//        Thread.sleep(5000)
    }
}
