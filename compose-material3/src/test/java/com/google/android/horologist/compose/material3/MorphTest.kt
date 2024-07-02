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

@file:OptIn(ExperimentalRoborazziApi::class)

package com.google.android.horologist.compose.material3

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.onRoot
import androidx.wear.compose.material3.ButtonDefaults
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.ThresholdValidator
import com.github.takahirom.roborazzi.captureRoboImage
import com.google.android.horologist.compose.material3.RoundButtonDefaults.circleSquareMorph
import com.google.android.horologist.compose.material3.RoundButtonDefaults.toShape
import com.google.android.horologist.screenshots.rng.WearScreenshotTest
import org.junit.Test

class MorphTest : WearScreenshotTest() {

    override fun testName(suffix: String): String =
        "src/test/snapshots/images/" +
            "${this.javaClass.`package`?.name}_${this.javaClass.simpleName}_" +
            "${testInfo.methodName}$suffix.png"

    private fun captureComponentImage(suffix: String = "") {
        composeRule.onRoot().captureRoboImage(
            filePath = testName(suffix),
            roborazziOptions = RoborazziOptions(
                recordOptions = RoborazziOptions.RecordOptions(
                    applyDeviceCrop = false,
                ),
                compareOptions = RoborazziOptions.CompareOptions(
                    resultValidator = ThresholdValidator(tolerance),
                ),
            ),
        )
    }

    @Test
    fun transition() {
        val progress = mutableFloatStateOf(0f)

        composeRule.setContent {
            Box(
                modifier = Modifier
                    .size(ButtonDefaults.Height)
                    .clip(circleSquareMorph.toShape { progress.floatValue })
                    .background(Color.Blue)
            )
        }

        captureComponentImage("_0")

        progress.floatValue = 0.5f

        captureComponentImage("_50")

        progress.floatValue = 1.0f

        captureComponentImage("_100")
    }
}
