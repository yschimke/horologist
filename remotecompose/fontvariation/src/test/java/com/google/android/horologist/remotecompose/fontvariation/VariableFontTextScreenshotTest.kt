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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.takahirom.roborazzi.captureRoboImage
import com.google.android.horologist.screenshots.rng.WearScreenshotTest
import org.junit.Test
import org.robolectric.annotation.Config

/**
 * One image per test font. Each row is one tested axis at its minimum or maximum, drawn three ways:
 * the Remote Compose path tween, the Remote Compose expression path, and the platform's own text in
 * the same font instanced at the same value. Each Remote Compose document is captured once and only
 * its named floats change.
 */
@Config(qualifiers = "w420dp-h900dp-xhdpi")
class VariableFontTextScreenshotTest : WearScreenshotTest() {

  @Test fun googleSansFlex() = compare(testFonts[0])

  @Test fun robotoFlex() = compare(testFonts[1])

  @Test fun recursive() = compare(testFonts[2])

  @Test fun fraunces() = compare(testFonts[3])

  @Test fun notoSans() = compare(testFonts[4])

  @Test fun inter() = compare(testFonts[5])

  private fun compare(testFont: TestFont) {
    val resId = R.raw::class.java.getField(testFont.resource).getInt(null)
    val rows =
      testFont.axes.flatMap { axis ->
        val info = testFont.font.axes.first { it.tag == axis }
        listOf(axis to info.minValue, axis to info.maxValue)
      }
    composeRule.setContent {
      Column(Modifier.background(Color.Black).testTag("Box")) {
        for ((axis, v) in rows) {
          Row {
            val cell = Modifier.size(CELL_WIDTH.dp, CELL_HEIGHT.dp)
            VariableFontTextPreview(
              TEXT,
              axis,
              v,
              cell,
              fontResId = resId,
              fontSize = SIZE.dp,
              documentWidth = CELL_WIDTH,
              documentHeight = CELL_HEIGHT,
            )
            VariableFontExpressionTextPreview(
              TEXT,
              mapOf(axis to v),
              cell,
              fontResId = resId,
              fontSize = SIZE.dp,
              documentWidth = CELL_WIDTH,
              documentHeight = CELL_HEIGHT,
            )
            BasicText(
              TEXT,
              cell,
              style =
                TextStyle(
                  color = Color.White,
                  fontSize = SIZE.sp,
                  fontFamily =
                    FontFamily(
                      Font(
                        resId,
                        variationSettings = FontVariation.Settings(FontVariation.Setting(axis, v)),
                      )
                    ),
                ),
            )
          }
        }
      }
    }
    composeRule.onNodeWithTag("Box").captureRoboImage(testName("_${testFont.resource}"))
  }

  private companion object {
    const val TEXT = "Hamburg"
    const val SIZE = 22
    const val CELL_WIDTH = 140
    const val CELL_HEIGHT = 32
  }
}
