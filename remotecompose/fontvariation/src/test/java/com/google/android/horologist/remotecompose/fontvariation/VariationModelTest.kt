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

package com.google.android.horologist.remotecompose.fontvariation

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.math.abs
import kotlin.random.Random
import org.junit.Test

/**
 * The expression approach evaluates the font's own variation model on the player. Its outlines are
 * [LinearForm]s; evaluated anywhere in the design space they must equal [layout] there, which
 * [VariableFontTest] in turn checks against `fontTools`.
 */
class VariationModelTest {

  @Test
  fun linearFormsEqualTheFontEverywhere() {
    val random = Random(11)
    for (testFont in testFonts) {
      val font = testFont.font
      val varied = font.variedLayout(TEXT)
      val locations =
        listOf(emptyMap(), font.axes.associate { it.tag to it.minValue }) +
          font.axes.associate { it.tag to it.maxValue } +
          List(6) {
            font.axes.associate {
              it.tag to it.minValue + (it.maxValue - it.minValue) * random.nextFloat()
            }
          }
      for (location in locations) {
        val coords = font.normalize(location)
        val truth = font.layout(TEXT, location)
        assertWithMessage("advance of $testFont at $location")
          .that(varied.advance.evaluate(coords))
          .isWithin(TOLERANCE)
          .of(truth.advance)
        assertThat(varied.contours).hasSize(truth.contours.size)
        varied.contours.zip(truth.contours).forEach { (v, t) ->
          val e = v.evaluate(coords)
          for (i in 0 until t.size) {
            val off = maxOf(abs(e.xs[i] - t.xs[i]), abs(e.ys[i] - t.ys[i]))
            assertWithMessage("point of $testFont at $location").that(off).isLessThan(TOLERANCE)
          }
        }
      }
    }
  }

  /**
   * Holding some axes fixed folds their factors into the coefficients; what is left, evaluated on
   * the animated axes alone, must still be the font.
   */
  @Test
  fun specializedFormsEqualTheFont() {
    val random = Random(5)
    for (testFont in testFonts) {
      val font = testFont.font
      val varied = font.variedLayout(TEXT)
      val animated = testFont.axes.take(2).map { tag -> font.axes.indexOfFirst { it.tag == tag } }
      val fixed =
        font.axes
          .filterIndexed { i, _ -> i !in animated }
          .associate { it.tag to it.minValue + (it.maxValue - it.minValue) * random.nextFloat() }
      val specialization = AxisSpecialization(animated.toSet(), font.normalize(fixed))
      repeat(4) {
        val moving = animated.associate { i ->
          val axis = font.axes[i]
          axis.tag to axis.minValue + (axis.maxValue - axis.minValue) * random.nextFloat()
        }
        val coords = font.normalize(fixed + moving)
        val truth = font.layout(TEXT, fixed + moving)
        assertWithMessage("advance of $testFont at ${fixed + moving}")
          .that(specialization.specialize(varied.advance).evaluate(coords))
          .isWithin(TOLERANCE)
          .of(truth.advance)
        varied.contours.zip(truth.contours).forEach { (v, t) ->
          for (i in 0 until t.size) {
            val x = specialization.specialize(v.xs[i]).evaluate(coords)
            val y = specialization.specialize(v.ys[i]).evaluate(coords)
            val off = maxOf(abs(x - t.xs[i]), abs(y - t.ys[i]))
            assertWithMessage("point of $testFont at ${fixed + moving}")
              .that(off)
              .isLessThan(TOLERANCE)
          }
        }
      }
    }
  }

  @Test
  fun maxAdvanceIsTheWidestAnywhere() {
    val font = testFonts.first { it.name == "Roboto Flex" }.font
    val varied = font.variedLayout(TEXT)
    val animated = listOf("wght", "wdth").map { tag -> font.axes.indexOfFirst { it.tag == tag } }
    val widest = font.maxAdvance(TEXT, varied.advance, animated.toSet(), font.normalize(emptyMap()))
    val random = Random(3)
    var sampled = 0f
    repeat(500) {
      val location = animated.associate { i ->
        val axis = font.axes[i]
        axis.tag to axis.minValue + (axis.maxValue - axis.minValue) * random.nextFloat()
      }
      sampled = maxOf(sampled, font.layout(TEXT, location).advance)
    }
    assertThat(widest).isAtLeast(sampled - TOLERANCE)
    assertThat(widest)
      .isWithin(TOLERANCE)
      .of(font.layout(TEXT, mapOf("wght" to 1000f, "wdth" to 151f)).advance)
  }

  @Test
  fun variedAndStaticOutlinesEmitTheSameCommands() {
    for (testFont in testFonts) {
      val static = Commands<Float>().also { testFont.font.layout(TEXT, emptyMap()).emit(it) }
      val varied = Commands<LinearForm>().also { testFont.font.variedLayout(TEXT).emit(it) }
      assertWithMessage("$testFont").that(varied.commands).isEqualTo(static.commands)
    }
  }

  @Test
  fun constantRegionsCollapse() {
    // An axis that does not move contributes no terms: Noto Sans has only wght and wdth, and a
    // glyph's terms are only the regions that actually move it.
    val varied = testFonts.first { it.name == "Noto Sans" }.font.variedLayout("H")
    val regions = varied.contours.flatMap { c -> c.xs + c.ys }.flatMap { it.terms.keys }.toSet()
    assertThat(regions).isNotEmpty()
    assertThat(regions.size).isLessThan(50)
  }

  private class Commands<T> : PathSink<T> {
    val commands = mutableListOf<String>()

    override fun moveTo(x: T, y: T) {
      commands += "M"
    }

    override fun lineTo(x: T, y: T) {
      commands += "L"
    }

    override fun quadTo(x1: T, y1: T, x2: T, y2: T) {
      commands += "Q"
    }

    override fun close() {
      commands += "Z"
    }
  }

  private companion object {
    const val TEXT = "Hamburgefonstiv HOW 0123 &?"
    const val TOLERANCE = 0.01f
  }
}
