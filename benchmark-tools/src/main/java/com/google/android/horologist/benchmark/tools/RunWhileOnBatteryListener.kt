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

package com.google.android.horologist.benchmark.tools

import android.annotation.SuppressLint
import android.os.BatteryManager
import androidx.test.internal.runner.listener.InstrumentationRunListener
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.runner.Description
import org.junit.runner.Result

@SuppressLint("RestrictedApi")
class RunWhileOnBatteryListener: InstrumentationRunListener() {
    val batteryManager = InstrumentationRegistry.getInstrumentation().context.getSystemService(BatteryManager::class.java)

    override fun testRunStarted(description: Description?) {
        println("RunWhileOnBatteryListener.testRunStarted")

        println("Waiting to stop charging")
        while (batteryManager.isCharging) {
            println(".")
            Thread.sleep(1000)
        }
        println("Not charging")
    }

    override fun testRunFinished(result: Result) {
        println("RunWhileOnBatteryListener.testRunFinished")

        println("Waiting to start charging")
        while (!batteryManager.isCharging) {
            println(".")
            Thread.sleep(1000)
        }
        println("Charging")
    }
}