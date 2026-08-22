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

package com.google.android.horologist.remotecompose.lottie

import com.google.android.horologist.remotecompose.lottie.format.values.BezierValue
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.CubicSegment
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.Point
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.toBezierValue
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.toCubicSegments
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.trimBezierValue
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TrimPathEvaluatorTest {

  @Test
  fun pointOperations() {
    val p1 = Point(10f, 20f)
    val p2 = Point(4f, 8f)

    val sum = p1 + p2
    assertThat(sum.x).isEqualTo(14f)
    assertThat(sum.y).isEqualTo(28f)

    val diff = p1 - p2
    assertThat(diff.x).isEqualTo(6f)
    assertThat(diff.y).isEqualTo(12f)

    val scaled = p1 * 2f
    assertThat(scaled.x).isEqualTo(20f)
    assertThat(scaled.y).isEqualTo(40f)

    val dist = Point(0f, 0f).distanceTo(Point(3f, 4f))
    assertThat(dist).isEqualTo(5f)
  }

  @Test
  fun linearSegmentLengthAndEvaluation() {
    val seg =
      CubicSegment(
        p0 = Point(0f, 0f),
        p1 = Point(0f, 0f),
        p2 = Point(100f, 0f),
        p3 = Point(100f, 0f),
      )

    assertThat(seg.approximateLength()).isWithin(0.01f).of(100f)

    val mid = seg.pointAt(0.5f)
    assertThat(mid.x).isWithin(0.01f).of(50f)
    assertThat(mid.y).isWithin(0.01f).of(0f)

    val lengthTable = seg.computeLengthTable(16)
    assertThat(seg.tAtDistance(50f, lengthTable)).isWithin(0.01f).of(0.5f)
    assertThat(seg.tAtDistance(seg.pointAt(0.25f).x, lengthTable)).isWithin(0.01f).of(0.25f)
  }

  @Test
  fun deCasteljauSubdivision() {
    val seg =
      CubicSegment(
        p0 = Point(0f, 0f),
        p1 = Point(0f, 0f),
        p2 = Point(100f, 0f),
        p3 = Point(100f, 0f),
      )

    val (left, right) = seg.split(0.5f)
    assertThat(left.p0.x).isEqualTo(0f)
    assertThat(left.p3.x).isWithin(0.01f).of(50f)
    assertThat(right.p0.x).isWithin(0.01f).of(50f)
    assertThat(right.p3.x).isEqualTo(100f)

    val sub = seg.subsegment(0.25f, 0.75f)
    assertThat(sub.p0.x).isWithin(0.01f).of(seg.pointAt(0.25f).x)
    assertThat(sub.p3.x).isWithin(0.01f).of(seg.pointAt(0.75f).x)
  }

  @Test
  fun bezierValueConversionRoundtrip() {
    val original =
      BezierValue(
        closed = false,
        vertices = listOf(listOf(0f, 0f), listOf(100f, 100f)),
        inTangents = listOf(listOf(0f, 0f), listOf(0f, 0f)),
        outTangents = listOf(listOf(0f, 0f), listOf(0f, 0f)),
      )

    val segments = original.toCubicSegments()
    assertThat(segments).hasSize(1)

    val converted = segments.toBezierValue()
    assertThat(converted.vertices).hasSize(2)
    assertThat(converted.vertices[0][0]).isEqualTo(0f)
    assertThat(converted.vertices[1][0]).isEqualTo(100f)
  }

  @Test
  fun trimBezierValueLinearLine() {
    val line =
      BezierValue(
        closed = false,
        vertices = listOf(listOf(0f, 0f), listOf(100f, 0f)),
        inTangents = listOf(listOf(0f, 0f), listOf(0f, 0f)),
        outTangents = listOf(listOf(0f, 0f), listOf(0f, 0f)),
      )

    // Full trim: 0.0 to 1.0
    val fullTrim = trimBezierValue(line, startFraction = 0f, endFraction = 1f)
    assertThat(fullTrim).hasSize(1)
    assertThat(fullTrim[0].vertices[0][0]).isWithin(0.01f).of(0f)
    assertThat(fullTrim[0].vertices[1][0]).isWithin(0.01f).of(100f)

    // Inverted full trim: 1.0 to 0.0 -> full line
    val invFullTrim = trimBezierValue(line, startFraction = 1f, endFraction = 0f)
    assertThat(invFullTrim).hasSize(1)
    assertThat(invFullTrim[0].vertices[0][0]).isWithin(0.01f).of(0f)
    assertThat(invFullTrim[0].vertices[1][0]).isWithin(0.01f).of(100f)

    // Partial trim: start 0.5, end 0.0 -> 0% to 50%
    val halfTrim = trimBezierValue(line, startFraction = 0.5f, endFraction = 0f)
    assertThat(halfTrim).hasSize(1)
    assertThat(halfTrim[0].vertices[0][0]).isWithin(0.01f).of(0f)
    assertThat(halfTrim[0].vertices[1][0]).isWithin(0.01f).of(50f)

    // Zero length: start 0.0, end 0.0 -> empty
    val zeroTrim = trimBezierValue(line, startFraction = 0f, endFraction = 0f)
    assertThat(zeroTrim).isEmpty()
  }
}
