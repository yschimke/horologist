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

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertWithMessage
import kotlin.math.abs
import kotlin.math.roundToInt
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders each test font through both Remote Compose approaches and through the platform's own
 * text, and compares the pixels.
 * - The path tween and the expression path draw the same line and must agree to within
 *   antialiasing: both are the font's own outlines.
 * - The expression path and the platform must agree on a single glyph. A whole line drifts apart,
 *   because the platform kerns and rounds each advance to the pixel grid while the paths use the
 *   font's nominal advances; one glyph isolates the outline itself.
 */
@Config(sdk = [35], qualifiers = "w600dp-h400dp-xhdpi")
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RenderFidelityTest {
  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun everyTestedAxis() {
    val cases = testFonts.flatMap { testFont ->
      testFont.axes.flatMap { axis ->
        val info = testFont.font.axes.first { it.tag == axis }
        listOf(info.minValue, (info.minValue + info.maxValue) / 2, info.maxValue).map {
          Case(testFont, mapOf(axis to it))
        }
      }
    }
    check(cases)
  }

  @Test
  fun severalAxesAtOnce() {
    check(
      listOf(
        Case(googleSansFlex, mapOf("wght" to 800f, "ROND" to 100f)),
        Case(testFonts[1], mapOf("wght" to 750f, "wdth" to 120f, "slnt" to -10f)),
        Case(testFonts[2], mapOf("wght" to 900f, "slnt" to -15f, "CASL" to 1f, "MONO" to 1f)),
        Case(testFonts[3], mapOf("wght" to 300f, "opsz" to 144f, "SOFT" to 100f)),
      )
    )
  }

  private fun check(cases: List<Case>) {
    val report = StringBuilder()
    val failures = mutableListOf<String>()
    for (case in cases) {
      show(case)
      val line = difference(capture("tween"), capture("expression"))
      val glyph = difference(capture("expressionGlyph"), capture("platformGlyph"))
      val result = "$case: tween/expression=$line expression/platform=$glyph"
      report.appendLine(result)
      if (line > LINE_TOLERANCE || glyph > GLYPH_TOLERANCE) failures += result
    }
    println(report)
    assertWithMessage(report.toString()).that(failures).isEmpty()
  }

  private class Case(val testFont: TestFont, val values: Map<String, Float>) {
    val resId: Int = R.raw::class.java.getField(testFont.resource).getInt(null)

    override fun toString(): String = "$testFont $values"
  }

  private var current by mutableStateOf<Case?>(null)

  private fun show(case: Case) {
    if (current == null) composeRule.setContent { current?.let { key(it) { Content(it) } } }
    current = case
    composeRule.waitForIdle()
  }

  @Composable
  private fun Content(case: Case) {
    val (axis, value) = case.values.entries.first()
    Column(Modifier.background(Color.Black)) {
      Box(Modifier.testTag("tween")) {
        VariableFontTextPreview(
          TEXT,
          axis,
          value,
          Modifier.size(WIDTH.dp, HEIGHT.dp),
          fontResId = case.resId,
          fontSize = SIZE.dp,
          location = case.values - axis,
          documentWidth = WIDTH,
          documentHeight = HEIGHT,
        )
      }
      Box(Modifier.testTag("expression")) { Expression(case, TEXT, WIDTH) }
      Box(Modifier.testTag("expressionGlyph")) { Expression(case, GLYPH, HEIGHT) }
      BasicText(
        GLYPH,
        Modifier.size(HEIGHT.dp, HEIGHT.dp).testTag("platformGlyph"),
        style =
          TextStyle(
            color = Color.White,
            fontSize = SIZE.sp,
            fontFamily =
              FontFamily(
                Font(
                  case.resId,
                  variationSettings =
                    FontVariation.Settings(
                      *case.values.map { (tag, v) -> FontVariation.Setting(tag, v) }.toTypedArray()
                    ),
                )
              ),
          ),
      )
    }
  }

  @Composable
  private fun Expression(case: Case, text: String, width: Int) {
    VariableFontExpressionTextPreview(
      text,
      case.values,
      Modifier.size(width.dp, HEIGHT.dp),
      fontResId = case.resId,
      fontSize = SIZE.dp,
      documentWidth = width,
      documentHeight = HEIGHT,
    )
  }

  private fun capture(tag: String): Bitmap {
    val root = composeRule.activity.window.decorView
    val whole = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
    composeRule.runOnUiThread { root.draw(Canvas(whole)) }
    val bounds = composeRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInWindow
    return Bitmap.createBitmap(
      whole,
      bounds.left.roundToInt(),
      bounds.top.roundToInt(),
      bounds.width.roundToInt(),
      bounds.height.roundToInt(),
    )
  }

  /**
   * The summed absolute difference in luminance over the summed ink of the two: 0 when identical, 1
   * when they share no ink. Normalizing by ink rather than area keeps a thin glyph in a large box
   * from looking better than it is.
   */
  private fun difference(a: Bitmap, b: Bitmap): Float {
    var diff = 0.0
    var ink = 0.0
    for (y in 0 until minOf(a.height, b.height)) {
      for (x in 0 until minOf(a.width, b.width)) {
        val la = (a.getPixel(x, y) and 0xff) / 255.0
        val lb = (b.getPixel(x, y) and 0xff) / 255.0
        diff += abs(la - lb)
        ink += maxOf(la, lb)
      }
    }
    return if (ink == 0.0) 0f else (diff / ink).toFloat()
  }

  private companion object {
    const val TEXT = "Hamburgefonstiv"
    const val GLYPH = "g"
    const val SIZE = 28
    const val WIDTH = 280
    const val HEIGHT = 40
    const val LINE_TOLERANCE = 0.001f
    const val GLYPH_TOLERANCE = 0.25f
  }
}
