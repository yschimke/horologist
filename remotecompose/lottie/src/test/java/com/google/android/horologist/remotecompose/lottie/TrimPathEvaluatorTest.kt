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

    // Zero length: start 0.0, end 0.0 with keepStructureIfDegenerate = false -> empty
    val zeroTrim =
      trimBezierValue(line, startFraction = 0f, endFraction = 0f, keepStructureIfDegenerate = false)
    assertThat(zeroTrim).isEmpty()

    // Zero length with keepStructureIfDegenerate = true -> degenerate collapsed vertices
    val degenTrim =
      trimBezierValue(line, startFraction = 0f, endFraction = 0f, keepStructureIfDegenerate = true)
    assertThat(degenTrim).hasSize(1)
    assertThat(degenTrim[0].vertices[0][0]).isWithin(0.01f).of(0f)
    assertThat(degenTrim[0].vertices[1][0]).isWithin(0.01f).of(0f)
  }

  @Test
  fun trimBezierValue_multiSegment_preservesTopologyAcrossDifferentTrimRanges() {
    // 4-segment circle (radius 50 centered at 50, 50)
    val cp = 50f * 0.55228f
    val circle =
      BezierValue(
        closed = true,
        vertices = listOf(listOf(50f, 0f), listOf(100f, 50f), listOf(50f, 100f), listOf(0f, 50f)),
        inTangents = listOf(listOf(-cp, 0f), listOf(0f, -cp), listOf(cp, 0f), listOf(0f, cp)),
        outTangents = listOf(listOf(cp, 0f), listOf(0f, cp), listOf(-cp, 0f), listOf(0f, -cp)),
      )

    // Test different trim ranges with keepStructureIfDegenerate = true
    val trim1 =
      trimBezierValue(
        circle,
        startFraction = 0.25f,
        endFraction = 0.75f,
        keepStructureIfDegenerate = true,
      )
    assertThat(trim1).hasSize(1)
    assertThat(trim1[0].vertices).hasSize(5)

    val trim2 =
      trimBezierValue(
        circle,
        startFraction = 0.1f,
        endFraction = 0.2f,
        keepStructureIfDegenerate = true,
      )
    assertThat(trim2).hasSize(1)
    assertThat(trim2[0].vertices).hasSize(5)

    val trim3 =
      trimBezierValue(
        circle,
        startFraction = 0f,
        endFraction = 0f,
        keepStructureIfDegenerate = true,
      )
    assertThat(trim3).hasSize(1)
    assertThat(trim3[0].vertices).hasSize(5)

    val trimFull =
      trimBezierValue(
        circle,
        startFraction = 0f,
        endFraction = 1f,
        keepStructureIfDegenerate = true,
      )
    assertThat(trimFull).hasSize(1)
    assertThat(trimFull[0].vertices).hasSize(5)
  }

  @Test
  fun trimBezierValue_multiSegment_evaluatedPointsLieOnArc() {
    val cp = 50f * 0.55228f
    val circle =
      BezierValue(
        closed = true,
        vertices = listOf(listOf(50f, 0f), listOf(100f, 50f), listOf(50f, 100f), listOf(0f, 50f)),
        inTangents = listOf(listOf(-cp, 0f), listOf(0f, -cp), listOf(cp, 0f), listOf(0f, cp)),
        outTangents = listOf(listOf(cp, 0f), listOf(0f, cp), listOf(-cp, 0f), listOf(0f, -cp)),
      )

    // Trim first quadrant: 0.0 to 0.25
    val trimQuad =
      trimBezierValue(
        circle,
        startFraction = 0f,
        endFraction = 0.25f,
        keepStructureIfDegenerate = false,
      )
    assertThat(trimQuad).hasSize(1)
    val segs = trimQuad[0].toCubicSegments()
    assertThat(segs).hasSize(1)

    // Midpoint of the trimmed segment at t=0.5 (i.e. angle ~ 45 deg)
    val midPt = segs[0].pointAt(0.5f)
    val distFromCenter = Point(50f, 50f).distanceTo(midPt)
    // Distance from circle center (50, 50) must be ~ 50.0 (true arc), not ~ 35.3 (flat chord)
    assertThat(distFromCenter).isWithin(0.5f).of(50f)
  }
}
