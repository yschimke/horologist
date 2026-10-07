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
import org.junit.Test

/**
 * The property the whole module rests on: interpolating linearly between consecutive keyframes
 * gives the font's own outline at every axis value in between, for any text.
 */
class AxisKeyframesTest {

  @Test
  fun roundnessNeedsOnlyItsEndpoints() {
    assertThat(testFont.axisKeyframes("Hello", "ROND")).containsExactly(0f, 100f).inOrder()
  }

  @Test
  fun weightHasIntermediateMasters() {
    val keys = testFont.axisKeyframes("Hello", "wght")
    assertThat(keys.first()).isEqualTo(1f)
    assertThat(keys.last()).isEqualTo(1000f)
    assertThat(keys).contains(400f)
    assertThat(keys.size).isGreaterThan(2)
  }

  @Test
  fun rangeLimitsKeyframes() {
    assertThat(testFont.axisKeyframes("Hello", "ROND", 20f..60f))
      .containsExactly(20f, 60f)
      .inOrder()
    val keys = testFont.axisKeyframes("Hello", "wght", 300f..700f)
    assertThat(keys.first()).isEqualTo(300f)
    assertThat(keys.last()).isEqualTo(700f)
    assertThat(keys).contains(400f)
  }

  @Test
  fun keyframesShareOneCommandSequence() {
    for (axis in listOf("ROND", "wght")) {
      val sequences =
        testFont.axisKeyframes(TEXT, axis).map { v ->
          Recorder().also { testFont.layout(TEXT, mapOf(axis to v)).emit(it) }.commands
        }
      sequences.forEach { assertThat(it).isEqualTo(sequences.first()) }
    }
  }

  @Test
  fun tweensReproduceTheFont() {
    for ((axis, location) in
      listOf(
        "ROND" to mapOf("wght" to 400f),
        "ROND" to mapOf("wght" to 800f),
        "wght" to mapOf("ROND" to 0f),
        "wght" to mapOf("ROND" to 60f),
      )) {
      val keys = testFont.axisKeyframes(TEXT, axis)
      val frames = keys.map { v -> points(testFont.layout(TEXT, location + (axis to v))) }
      val axisInfo = testFont.axes.first { it.tag == axis }
      var worst = 0f
      for (step in 0..200) {
        val v = axisInfo.minValue + (axisInfo.maxValue - axisInfo.minValue) * step / 200f
        val segment = (0 until keys.size - 1).first { v <= keys[it + 1] }
        val t = (v - keys[segment]) / (keys[segment + 1] - keys[segment])
        val a = frames[segment]
        val b = frames[segment + 1]
        val truth = points(testFont.layout(TEXT, location + (axis to v)))
        for (i in truth.indices) {
          worst = maxOf(worst, abs(a[i] + (b[i] - a[i]) * t - truth[i]))
        }
      }
      // Font units: 0.05 is 1/40000 em, far below a pixel at any size.
      assertWithMessage("$axis with $location").that(worst).isLessThan(0.05f)
    }
  }

  @Test
  fun everyTestFontSharesOneCommandSequencePerAxis() {
    for (testFont in testFonts) {
      for (axis in testFont.axes) {
        val sequences =
          testFont.font.axisKeyframes(LATIN, axis).map { v ->
            Recorder().also { testFont.font.layout(LATIN, mapOf(axis to v)).emit(it) }.commands
          }
        sequences.forEach {
          assertWithMessage("$axis of $testFont").that(it).isEqualTo(sequences.first())
        }
      }
    }
  }

  /**
   * The tween reproduces the font on every tested axis of every test font, with the other axes at
   * their defaults and again at an off-default point, so the `avar` mappings and the regions that
   * span two axes are both exercised.
   */
  @Test
  fun tweensReproduceEveryTestFont() {
    for (testFont in testFonts) {
      val font = testFont.font
      val offDefault =
        font.axes.associate { it.tag to it.minValue + (it.maxValue - it.minValue) * 0.7f }
      for (axis in testFont.axes) {
        for (others in listOf(emptyMap(), offDefault - axis)) {
          val worst = worstTweenError(font, LATIN, axis, others, steps = 100)
          assertWithMessage("$axis of $testFont with $others").that(worst).isLessThan(0.05f)
        }
      }
    }
  }

  private fun worstTweenError(
    font: VariableFont,
    text: String,
    axis: String,
    location: Map<String, Float>,
    steps: Int,
  ): Float {
    val keys = font.axisKeyframes(text, axis)
    val frames = keys.map { v -> points(font.layout(text, location + (axis to v))) }
    val axisInfo = font.axes.first { it.tag == axis }
    var worst = 0f
    for (step in 0..steps) {
      val v = axisInfo.minValue + (axisInfo.maxValue - axisInfo.minValue) * step / steps
      val segment = (0 until keys.size - 1).first { v <= keys[it + 1] }
      val t = (v - keys[segment]) / (keys[segment + 1] - keys[segment])
      val a = frames[segment]
      val b = frames[segment + 1]
      val truth = points(font.layout(text, location + (axis to v)))
      for (i in truth.indices) {
        worst = maxOf(worst, abs(a[i] + (b[i] - a[i]) * t - truth[i]))
      }
    }
    return worst
  }

  private fun points(outline: TextOutline): FloatArray =
    outline.contours
      .flatMap { c -> (0 until c.size).flatMap { listOf(c.xs[it], c.ys[it]) } }
      .plus(outline.advance)
      .toFloatArray()

  private class Recorder : PathSink<Float> {
    val commands = mutableListOf<String>()

    override fun moveTo(x: Float, y: Float) {
      commands += "M"
    }

    override fun lineTo(x: Float, y: Float) {
      commands += "L"
    }

    override fun quadTo(x1: Float, y1: Float, x2: Float, y2: Float) {
      commands += "Q"
    }

    override fun close() {
      commands += "Z"
    }
  }

  private companion object {
    const val TEXT = "Hello, Wear OS 12:45! café"

    /** Within every test font's Basic Latin subset. */
    const val LATIN = "Hamburgefonstiv HOW 0123 &?"
  }
}
