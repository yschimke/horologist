/*
 * Copyright 2026 The Android Open Source Project
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

@file:OptIn(ExperimentalTextApi::class)

package com.google.android.horologist.remotecompose.fontvariation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.takahirom.roborazzi.captureRoboImage
import com.google.android.horologist.screenshots.rng.WearScreenshotTest
import org.junit.Test

/**
 * Each row: the Remote Compose path tween on the left, the platform's own text in the same font
 * instanced at the same axis value on the right. One document is captured per axis and only its
 * named float changes between rows.
 */
class VariableFontTextScreenshotTest : WearScreenshotTest() {

  @Test
  fun roundness() {
    compare("ROND", listOf(0f, 50f, 100f))
  }

  @Test
  fun weight() {
    compare("wght", listOf(100f, 400f, 700f, 1000f))
  }

  private fun compare(axis: String, values: List<Float>) {
    composeRule.setContent {
      Column(Modifier.background(Color.Black).testTag("Box")) {
        for (v in values) {
          Row {
            VariableFontTextPreview(
              "Hello",
              axis,
              v,
              Modifier.size(100.dp, 40.dp),
              fontSize = 28.dp,
              documentWidth = 100,
              documentHeight = 40,
            )
            BasicText(
              "Hello",
              Modifier.size(100.dp, 40.dp),
              style =
                TextStyle(
                  color = Color.White,
                  fontSize = 28.sp,
                  fontFamily =
                    FontFamily(
                      Font(
                        R.raw.google_sans_flex_wght_rond,
                        weight = FontWeight.Normal,
                        variationSettings = FontVariation.Settings(FontVariation.Setting(axis, v)),
                      )
                    ),
                ),
            )
          }
        }
      }
    }
    composeRule.onNodeWithTag("Box").captureRoboImage(testName("_$axis"))
  }
}
