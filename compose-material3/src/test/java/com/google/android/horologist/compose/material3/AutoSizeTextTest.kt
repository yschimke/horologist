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

package com.google.android.horologist.compose.material3

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.AutoSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.MaterialTheme
import com.google.android.horologist.screenshots.rng.WearDevice
import com.google.android.horologist.screenshots.rng.WearDeviceScreenshotTest
import org.junit.Test

class AutoSizeTextTest(device: WearDevice) : WearDeviceScreenshotTest(device) {

    @Test
    fun autoSized() {
        runTest {
            MaterialTheme {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//                    Text(
//                        modifier = Modifier.fillMaxWidth(),
//                        text = "Auto Size Title",
//                        autoSize = AutoSize.StepBased(),
//                        maxLines = 1,
//                    )

                    BasicText(
                        "Auto Size Title",
                        color = ColorProducer { Color.White },
                        autoSize = AutoSize.StepBased(
                            minFontSize = 10.sp, maxFontSize = 80.sp, stepSize = 10.sp
                        ),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
