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

@file:OptIn(ExperimentalMetricApi::class)

package com.google.android.horologist.media.benchmark

import android.Manifest
import android.os.BatteryManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.Metric
import androidx.benchmark.macro.PowerMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.media3.session.MediaBrowser
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.google.android.horologist.media.benchmark.MediaControllerHelper.startPlaying
import com.google.android.horologist.media.benchmark.MediaControllerHelper.stopPlaying
import com.google.android.horologist.media.benchmark.Util.executeShellScript
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@LargeTest
public abstract class BasePlaybackBenchmark {
//    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
//    @get:Rule
//    public val grantPermissionRule: GrantPermissionRule =
//        GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    private lateinit var batteryManager: BatteryManager

    @get:Rule
    public val benchmarkRule: MacrobenchmarkRule = MacrobenchmarkRule()

    public lateinit var mediaControllerFuture: ListenableFuture<MediaBrowser>

    public abstract val mediaApp: MediaApp

    @Test
    public fun startup(): Unit = benchmarkRule.measureRepeated(
        packageName = mediaApp.packageName,
        metrics = metrics(),
        compilationMode = CompilationMode.Partial(warmupIterations = 0),
        iterations = 1,
        setupBlock = {
//            InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(mediaApp.packageName, Manifest.permission.POST_NOTIFICATIONS)

            mediaControllerFuture = MediaControllerHelper.lookupController(
                mediaApp.playerComponentName,
            )

            // Wait for service
            mediaControllerFuture.get()

            if (IncludePower) {
                device.executeShellScript("echo 1 > /d/google_charger/input_suspend")
            }

            batteryManager = InstrumentationRegistry.getInstrumentation().context.getSystemService(BatteryManager::class.java)
        },
    ) {
        onStartup()

        check(iteration != null) {
            "Iteration $iteration"
        }

        val mediaController = mediaControllerFuture.get()

        if (IncludePower) {
            check(!batteryManager.isCharging) {
                "Should not be charging"
            }


            println(device.executeShellScript("dumpsys battery"))
        }

        runBlocking {
            delay(5.seconds)

            mediaController.startPlaying(mediaApp.testMedia)

            delay(20.seconds)

            checkPlayingState(mediaController)

            repeat(20) {
                if (IncludePower) {
                    println(device.executeShellScript("dumpsys battery"))
                }
                delay(30.seconds)
            }

            mediaController.stopPlaying()

            delay(1.seconds)
        }

        if (IncludePower) {
            device.executeShellScript("echo 0 > /d/google_charger/input_suspend")
        }
    }

    public open fun metrics(): List<Metric> = buildList {
        add(FrameTimingMetric())
        if (IncludePower) {
            add(PowerMetric(type = PowerMetric.Type.Battery()))
        }
    }

    public open fun MacrobenchmarkScope.onStartup() {
        startActivityAndWait()
    }

    public open suspend fun checkPlayingState(mediaController: MediaBrowser) {
        withContext(Dispatchers.Main) {
            if (!mediaController.isPlaying) {
                throw IllegalStateException("Not playing after 20 seconds")
            }
        }
    }

    public companion object {
        public val IncludePower: Boolean = true
    }
}
