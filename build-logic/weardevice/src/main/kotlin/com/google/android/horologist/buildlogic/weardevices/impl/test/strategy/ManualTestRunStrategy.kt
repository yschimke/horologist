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

package com.google.android.horologist.buildlogic.weardevices.impl.test.strategy

class ManualTestRunStrategy : TestRunStrategy() {
    override val sync: Boolean = false

    override val instrumentOptions: String =
        "-e listener com.google.android.horologist.benchmark.tools.RunWhileOnBatteryListener"

    override fun waitForResults() {
        // no need to wait because of listener
        Thread.sleep(10000)
    }
}
