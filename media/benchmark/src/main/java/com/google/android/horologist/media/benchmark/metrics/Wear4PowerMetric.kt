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

@file:OptIn(ExperimentalPerfettoTraceProcessorApi::class, ExperimentalMetricApi::class)

package com.google.android.horologist.media.benchmark.metrics

import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.TraceMetric
import androidx.benchmark.perfetto.ExperimentalPerfettoTraceProcessorApi
import androidx.benchmark.perfetto.PerfettoTraceProcessor

public class Wear4PowerMetric : TraceMetric() {
    override fun getMeasurements(
        captureInfo: CaptureInfo,
        traceSession: PerfettoTraceProcessor.Session
    ): List<Measurement> {
        val currentUa = traceSession.query(
            """
                  SELECT ts, value AS current_ua
                  FROM counter c
                  JOIN counter_track t ON c.track_id = t.id
                  WHERE name = 'batt.current_ua'
            """.trimIndent()
        ).map { it.double("current_ua") }

        return listOf(
            Measurement(
                "BatteryCurrentUaTotal",
                currentUa.sumOf { it }
            ),
            Measurement(
                "BatteryCurrentUaAvg",
                currentUa.average()
            ),
            Measurement(
                "BatteryCurrentUaMin",
                currentUa.max()
            ),
        )
    }
}
