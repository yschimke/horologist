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

import android.annotation.SuppressLint
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rf
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.Path
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.OffsetPath
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.PuckerBloat
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.Twist
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.ZigZag
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.ZigZagType
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.Fill
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.LineJoin
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticBezierProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticColorProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticPositionProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticScalarProperty
import com.google.android.horologist.remotecompose.lottie.format.values.BezierValue
import com.google.android.horologist.remotecompose.lottie.renderer.RemoteLottiePath
import com.google.android.horologist.remotecompose.lottie.renderer.gatherShapesForTest
import com.google.android.horologist.remotecompose.lottie.renderer.properties.toRemote
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.evaluateOffsetPath
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.evaluatePuckerBloat
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.evaluateTwist
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.evaluateZigZag
import com.google.common.truth.Truth.assertThat
import org.junit.Test

@SuppressLint("RestrictedApi")
class ExtendedModifiersTest {

  private val settings = LottieSettings(0f.rf, SlotMap(emptyMap()))

  @Test
  fun evaluateZigZag_corner_createsRidgesAlongLine() {
    // Open horizontal line from (0, 0) to (100, 0)
    val line =
      BezierValue(
        closed = false,
        vertices = listOf(listOf(0f, 0f), listOf(100f, 0f)),
        inTangents = listOf(listOf(0f, 0f), listOf(0f, 0f)),
        outTangents = listOf(listOf(0f, 0f), listOf(0f, 0f)),
      )
    val shape = RemoteLottiePath(listOf(line.toRemote()))
    val zigZag =
      ZigZag(
        size = StaticScalarProperty(value = 10f),
        ridgesPerSegment = StaticScalarProperty(value = 2f),
        pointType = ZigZagType.Corner,
      )

    val results = evaluateZigZag(listOf(shape), zigZag, settings)
    assertThat(results).hasSize(1)
    val resultPath = results[0] as RemoteLottiePath
    val subpath = resultPath.path[0]

    // 2 ridges -> 4 intermediate points + 2 endpoints = 6 vertices
    assertThat(subpath.vertices).hasSize(6)
    assertThat(subpath.closed).isFalse()

    // Start point at (0, 0)
    assertThat(subpath.vertices[0][0].constantValueOrNull).isWithin(0.01f).of(0f)
    assertThat(subpath.vertices[0][1].constantValueOrNull).isWithin(0.01f).of(0f)

    // Intermediate points have alternating non-zero Y displacement (+- 5f)
    val y1 = subpath.vertices[1][1].constantValueOrNull ?: 0f
    val y2 = subpath.vertices[2][1].constantValueOrNull ?: 0f
    assertThat(kotlin.math.abs(y1)).isWithin(0.01f).of(5f)
    assertThat(kotlin.math.abs(y2)).isWithin(0.01f).of(5f)
    assertThat(y1 * y2).isLessThan(0f) // Opposite signs

    // End point at (100, 0)
    assertThat(subpath.vertices[5][0].constantValueOrNull).isWithin(0.01f).of(100f)
    assertThat(subpath.vertices[5][1].constantValueOrNull).isWithin(0.01f).of(0f)
  }

  @Test
  fun evaluateZigZag_smooth_createsCurvedWaveAlongLine() {
    val line =
      BezierValue(
        closed = false,
        vertices = listOf(listOf(0f, 0f), listOf(100f, 0f)),
        inTangents = listOf(listOf(0f, 0f), listOf(0f, 0f)),
        outTangents = listOf(listOf(0f, 0f), listOf(0f, 0f)),
      )
    val shape = RemoteLottiePath(listOf(line.toRemote()))
    val zigZag =
      ZigZag(
        size = StaticScalarProperty(value = 10f),
        ridgesPerSegment = StaticScalarProperty(value = 2f),
        pointType = ZigZagType.Smooth,
      )

    val results = evaluateZigZag(listOf(shape), zigZag, settings)
    val resultPath = results[0] as RemoteLottiePath
    val subpath = resultPath.path[0]

    // Intermediate vertices should have non-zero in/out tangents for smooth waves
    val outTan1 = subpath.outTangents[1]
    assertThat(outTan1[0].constantValueOrNull).isNotEqualTo(0f)
  }

  @Test
  fun evaluateZigZag_zeroSizeOrRidges_leavesPathUnchanged() {
    val line =
      BezierValue(
        closed = false,
        vertices = listOf(listOf(0f, 0f), listOf(100f, 0f)),
        inTangents = listOf(listOf(0f, 0f), listOf(0f, 0f)),
        outTangents = listOf(listOf(0f, 0f), listOf(0f, 0f)),
      )
    val shape = RemoteLottiePath(listOf(line.toRemote()))
    val zigZag =
      ZigZag(
        size = StaticScalarProperty(value = 0f),
        ridgesPerSegment = StaticScalarProperty(value = 2f),
      )

    val results = evaluateZigZag(listOf(shape), zigZag, settings)
    val resultPath = results[0] as RemoteLottiePath
    assertThat(resultPath.path[0].vertices).hasSize(2)
  }

  @Test
  fun evaluatePuckerBloat_positiveAmountBloat_expandsTangents() {
    // 100x100 square centered at (50, 50)
    val square =
      BezierValue(
        closed = true,
        vertices = listOf(listOf(0f, 0f), listOf(100f, 0f), listOf(100f, 100f), listOf(0f, 100f)),
        inTangents = listOf(listOf(0f, 0f), listOf(0f, 0f), listOf(0f, 0f), listOf(0f, 0f)),
        outTangents = listOf(listOf(0f, 0f), listOf(0f, 0f), listOf(0f, 0f), listOf(0f, 0f)),
      )
    val shape = RemoteLottiePath(listOf(square.toRemote()))
    val puckerBloat = PuckerBloat(amount = StaticScalarProperty(value = 50f))

    val results = evaluatePuckerBloat(listOf(shape), puckerBloat, settings)
    val resultPath = results[0] as RemoteLottiePath
    val subpath = resultPath.path[0]

    assertThat(subpath.vertices).hasSize(4)
    // Tangents at vertices should be non-zero (bowed out)
    assertThat(subpath.outTangents[0][0].constantValueOrNull).isNotEqualTo(0f)
    assertThat(subpath.inTangents[0][0].constantValueOrNull).isNotEqualTo(0f)
  }

  @Test
  fun evaluatePuckerBloat_negativeAmountPucker_pullsVerticesInward() {
    // 100x100 square centered at (50, 50)
    val square =
      BezierValue(
        closed = true,
        vertices = listOf(listOf(0f, 0f), listOf(100f, 0f), listOf(100f, 100f), listOf(0f, 100f)),
        inTangents = listOf(listOf(0f, 0f), listOf(0f, 0f), listOf(0f, 0f), listOf(0f, 0f)),
        outTangents = listOf(listOf(0f, 0f), listOf(0f, 0f), listOf(0f, 0f), listOf(0f, 0f)),
      )
    val shape = RemoteLottiePath(listOf(square.toRemote()))
    val puckerBloat = PuckerBloat(amount = StaticScalarProperty(value = -50f))

    val results = evaluatePuckerBloat(listOf(shape), puckerBloat, settings)
    val resultPath = results[0] as RemoteLottiePath
    val subpath = resultPath.path[0]

    // Vertices should be pulled towards center (50, 50)
    val v0x = subpath.vertices[0][0].constantValueOrNull ?: 0f
    val v0y = subpath.vertices[0][1].constantValueOrNull ?: 0f
    assertThat(v0x).isGreaterThan(0f)
    assertThat(v0y).isGreaterThan(0f)
  }

  @Test
  fun evaluateTwist_rotatesVerticesAndTangentsProportionalToDistance() {
    val line =
      BezierValue(
        closed = false,
        vertices = listOf(listOf(0f, 0f), listOf(100f, 0f)),
        inTangents = listOf(listOf(0f, 0f), listOf(0f, 0f)),
        outTangents = listOf(listOf(0f, 0f), listOf(0f, 0f)),
      )
    val shape = RemoteLottiePath(listOf(line.toRemote()))
    val twist =
      Twist(
        angle = StaticScalarProperty(value = 90f),
        center = StaticPositionProperty(value = listOf(0f, 0f)),
      )

    val results = evaluateTwist(listOf(shape), twist, settings)
    val resultPath = results[0] as RemoteLottiePath
    val subpath = resultPath.path[0]

    // Origin (0,0) distance is 0 -> unchanged
    assertThat(subpath.vertices[0][0].constantValueOrNull).isWithin(0.01f).of(0f)
    assertThat(subpath.vertices[0][1].constantValueOrNull).isWithin(0.01f).of(0f)

    // Point (100, 0) distance is 100 -> rotated 90 degrees CCW/CW -> (0, 100)
    val x1 = subpath.vertices[1][0].constantValueOrNull ?: 0f
    val y1 = subpath.vertices[1][1].constantValueOrNull ?: 0f
    assertThat(x1).isWithin(0.1f).of(0f)
    assertThat(y1).isWithin(0.1f).of(100f)
  }

  @Test
  fun evaluateOffsetPath_positiveAmount_expandsSquare() {
    // 100x100 square from (0,0) to (100,100)
    val square =
      BezierValue(
        closed = true,
        vertices = listOf(listOf(0f, 0f), listOf(100f, 0f), listOf(100f, 100f), listOf(0f, 100f)),
        inTangents = listOf(listOf(0f, 0f), listOf(0f, 0f), listOf(0f, 0f), listOf(0f, 0f)),
        outTangents = listOf(listOf(0f, 0f), listOf(0f, 0f), listOf(0f, 0f), listOf(0f, 0f)),
      )
    val shape = RemoteLottiePath(listOf(square.toRemote()))
    val offsetPath =
      OffsetPath(amount = StaticScalarProperty(value = 10f), lineJoin = LineJoin.Miter)

    val results = evaluateOffsetPath(listOf(shape), offsetPath, settings)
    val resultPath = results[0] as RemoteLottiePath
    val subpath = resultPath.path[0]

    assertThat(subpath.vertices).hasSize(4)
    // Vertex 0 at (0,0) should expand outward to (-10, -10)
    assertThat(subpath.vertices[0][0].constantValueOrNull).isWithin(0.1f).of(-10f)
    assertThat(subpath.vertices[0][1].constantValueOrNull).isWithin(0.1f).of(-10f)

    // Vertex 2 at (100,100) should expand outward to (110, 110)
    assertThat(subpath.vertices[2][0].constantValueOrNull).isWithin(0.1f).of(110f)
    assertThat(subpath.vertices[2][1].constantValueOrNull).isWithin(0.1f).of(110f)
  }

  @Test
  fun gatherShapes_withZigZagAndFill_emitsModifiedGeometries() {
    val line =
      BezierValue(
        closed = false,
        vertices = listOf(listOf(0f, 0f), listOf(100f, 0f)),
        inTangents = listOf(listOf(0f, 0f), listOf(0f, 0f)),
        outTangents = listOf(listOf(0f, 0f), listOf(0f, 0f)),
      )
    val elements =
      listOf(
        Path(shape = StaticBezierProperty(value = line)),
        ZigZag(
          size = StaticScalarProperty(value = 10f),
          ridgesPerSegment = StaticScalarProperty(value = 2f),
          pointType = ZigZagType.Corner,
        ),
        Fill(color = StaticColorProperty(value = androidx.compose.ui.graphics.Color.Blue.rc)),
      )

    val shapeGroups = gatherShapesForTest(elements, settings)
    assertThat(shapeGroups).hasSize(1)
    val group = shapeGroups[0]
    val lottiePath = group.shapes[0] as RemoteLottiePath
    assertThat(lottiePath.path[0].vertices).hasSize(6)
  }
}
