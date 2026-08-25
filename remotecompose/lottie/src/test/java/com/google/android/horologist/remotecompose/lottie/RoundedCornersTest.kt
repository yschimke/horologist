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
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.RoundedCorners
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.TrimPath
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.Fill
import com.google.android.horologist.remotecompose.lottie.format.properties.AnimatedScalarProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.ScalarPropertyKeyframe
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticBezierProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticColorProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticScalarProperty
import com.google.android.horologist.remotecompose.lottie.format.values.BezierValue
import com.google.android.horologist.remotecompose.lottie.renderer.RemoteLottiePath
import com.google.android.horologist.remotecompose.lottie.renderer.gatherShapesForTest
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.evaluatePath
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.roundBezierValue
import com.google.common.truth.Truth.assertThat
import org.junit.Test

@SuppressLint("RestrictedApi")
class RoundedCornersTest {

  private val settings = LottieSettings(0f.rf, SlotMap(emptyMap()))

  @Test
  fun roundBezierValue_sharpClosedTriangle_roundsAllThreeCorners() {
    // Closed right triangle: (0,0) -> (100,0) -> (0,100) -> closed
    val triangle =
      BezierValue(
        closed = true,
        vertices = listOf(listOf(0f, 0f), listOf(100f, 0f), listOf(0f, 100f)),
        inTangents = listOf(listOf(0f, 0f), listOf(0f, 0f), listOf(0f, 0f)),
        outTangents = listOf(listOf(0f, 0f), listOf(0f, 0f), listOf(0f, 0f)),
      )

    val rounded = roundBezierValue(triangle, radius = 10f)

    // Each of the 3 sharp corners is split into 2 vertices (pStart, pEnd) -> 6 vertices total
    assertThat(rounded.closed).isTrue()
    assertThat(rounded.vertices).hasSize(6)
    assertThat(rounded.inTangents).hasSize(6)
    assertThat(rounded.outTangents).hasSize(6)

    // Corner 0 at (0,0):
    // vPrev is (0,100), edge length 100 -> pStart at (0, 10)
    // vNext is (100,0), edge length 100 -> pEnd at (10, 0)
    val pStart0 = rounded.vertices[0]
    val pEnd0 = rounded.vertices[1]
    assertThat(pStart0[0]).isWithin(0.01f).of(0f)
    assertThat(pStart0[1]).isWithin(0.01f).of(10f)
    assertThat(pEnd0[0]).isWithin(0.01f).of(10f)
    assertThat(pEnd0[1]).isWithin(0.01f).of(0f)

    // Out-tangent at pStart0 points towards (0,0): (0, -10 * 0.5519)
    assertThat(rounded.outTangents[0][0]).isWithin(0.01f).of(0f)
    assertThat(rounded.outTangents[0][1]).isLessThan(0f)

    // In-tangent at pEnd0 points from (0,0): (-10 * 0.5519, 0)
    assertThat(rounded.inTangents[1][0]).isLessThan(0f)
    assertThat(rounded.inTangents[1][1]).isWithin(0.01f).of(0f)
  }

  @Test
  fun roundBezierValue_zeroRadius_preservesSharpPolygonGeometry() {
    val triangle =
      BezierValue(
        closed = true,
        vertices = listOf(listOf(0f, 0f), listOf(100f, 0f), listOf(0f, 100f)),
        inTangents = listOf(listOf(0f, 0f), listOf(0f, 0f), listOf(0f, 0f)),
        outTangents = listOf(listOf(0f, 0f), listOf(0f, 0f), listOf(0f, 0f)),
      )

    val rounded = roundBezierValue(triangle, radius = 0f)
    assertThat(rounded.vertices).hasSize(6)
    assertThat(rounded.vertices[0]).isEqualTo(listOf(0f, 0f))
    assertThat(rounded.vertices[1]).isEqualTo(listOf(0f, 0f))
    assertThat(rounded.vertices[2]).isEqualTo(listOf(100f, 0f))
    assertThat(rounded.vertices[3]).isEqualTo(listOf(100f, 0f))
    assertThat(rounded.vertices[4]).isEqualTo(listOf(0f, 100f))
    assertThat(rounded.vertices[5]).isEqualTo(listOf(0f, 100f))
    for (i in 0 until 6) {
      assertThat(rounded.inTangents[i]).isEqualTo(listOf(0f, 0f))
      assertThat(rounded.outTangents[i]).isEqualTo(listOf(0f, 0f))
    }
  }

  @Test
  fun roundBezierValue_clampsRadiusToHalfSegmentLength() {
    // Short edge of length 20: (0,0) to (20,0), then to (20,100) and (0,100)
    val rect =
      BezierValue(
        closed = true,
        vertices = listOf(listOf(0f, 0f), listOf(20f, 0f), listOf(20f, 100f), listOf(0f, 100f)),
        inTangents = listOf(listOf(0f, 0f), listOf(0f, 0f), listOf(0f, 0f), listOf(0f, 0f)),
        outTangents = listOf(listOf(0f, 0f), listOf(0f, 0f), listOf(0f, 0f), listOf(0f, 0f)),
      )

    // Requested radius 50f is larger than half of length 20 (which is 10f)
    val rounded = roundBezierValue(rect, radius = 50f)
    assertThat(rounded.vertices).hasSize(8)

    // Corner 1 at (20,0): edge from (0,0) has length 20 -> max radius clamped to 10f
    // pStart at (10, 0), pEnd at (20, 10)
    val pStart1 = rounded.vertices[2]
    val pEnd1 = rounded.vertices[3]
    assertThat(pStart1[0]).isWithin(0.01f).of(10f)
    assertThat(pStart1[1]).isWithin(0.01f).of(0f)
    assertThat(pEnd1[0]).isWithin(0.01f).of(20f)
    assertThat(pEnd1[1]).isWithin(0.01f).of(10f)
  }

  @Test
  fun roundBezierValue_openPath_leavesEndpointsUnrounded() {
    // Open path: (0,0) -> (50,50) -> (100,0)
    val openPath =
      BezierValue(
        closed = false,
        vertices = listOf(listOf(0f, 0f), listOf(50f, 50f), listOf(100f, 0f)),
        inTangents = listOf(listOf(0f, 0f), listOf(0f, 0f), listOf(0f, 0f)),
        outTangents = listOf(listOf(0f, 0f), listOf(0f, 0f), listOf(0f, 0f)),
      )

    val rounded = roundBezierValue(openPath, radius = 10f)
    // 3 vertices -> vertex 0 unchanged (1), vertex 1 rounded (2), vertex 2 unchanged (1) = 4 total
    assertThat(rounded.closed).isFalse()
    assertThat(rounded.vertices).hasSize(4)

    // First vertex is unrounded (0,0)
    assertThat(rounded.vertices[0][0]).isWithin(0.01f).of(0f)
    assertThat(rounded.vertices[0][1]).isWithin(0.01f).of(0f)

    // Last vertex is unrounded (100,0)
    assertThat(rounded.vertices[3][0]).isWithin(0.01f).of(100f)
    assertThat(rounded.vertices[3][1]).isWithin(0.01f).of(0f)
  }

  @Test
  fun roundBezierValue_smoothCurvesPreserved() {
    // Vertex with existing non-zero in/out tangents (smooth curve)
    val curved =
      BezierValue(
        closed = true,
        vertices = listOf(listOf(0f, 0f), listOf(100f, 0f)),
        inTangents = listOf(listOf(10f, 10f), listOf(-10f, -10f)),
        outTangents = listOf(listOf(-10f, -10f), listOf(10f, 10f)),
      )

    val rounded = roundBezierValue(curved, radius = 10f)
    // Both vertices already curved, preserved without splitting
    assertThat(rounded.vertices).hasSize(2)
    assertThat(rounded.inTangents[0]).isEqualTo(listOf(10f, 10f))
    assertThat(rounded.outTangents[0]).isEqualTo(listOf(-10f, -10f))
  }

  @Test
  fun evaluatePath_withRoundedCornersModifier() {
    val triangle =
      BezierValue(
        closed = true,
        vertices = listOf(listOf(0f, 0f), listOf(100f, 0f), listOf(0f, 100f)),
        inTangents = listOf(listOf(0f, 0f), listOf(0f, 0f), listOf(0f, 0f)),
        outTangents = listOf(listOf(0f, 0f), listOf(0f, 0f), listOf(0f, 0f)),
      )
    val pathGeometry = Path(shape = StaticBezierProperty(value = triangle))
    val roundedCorners = RoundedCorners(radius = StaticScalarProperty(value = 15f))

    val result = evaluatePath(pathGeometry, settings, roundedCorners = roundedCorners)
    assertThat(result).isNotNull()
    assertThat(result!!.path).hasSize(1)
    val bezier = result.path[0]
    assertThat(bezier.vertices).hasSize(6)
  }

  @Test
  fun evaluatePath_withAnimatedRoundedCorners() {
    val triangle =
      BezierValue(
        closed = true,
        vertices = listOf(listOf(0f, 0f), listOf(100f, 0f), listOf(0f, 100f)),
        inTangents = listOf(listOf(0f, 0f), listOf(0f, 0f), listOf(0f, 0f)),
        outTangents = listOf(listOf(0f, 0f), listOf(0f, 0f), listOf(0f, 0f)),
      )
    val pathGeometry = Path(shape = StaticBezierProperty(value = triangle))
    val animatedRadius =
      AnimatedScalarProperty(
        keyframes =
          listOf(
            ScalarPropertyKeyframe(frame = 0f, value = 0f),
            ScalarPropertyKeyframe(frame = 20f, value = 20f),
          )
      )
    val roundedCorners = RoundedCorners(radius = animatedRadius)

    // At frame 10 (halfway), radius is 10f
    val midSettings = LottieSettings(10f.rf, SlotMap(emptyMap()))
    val result = evaluatePath(pathGeometry, midSettings, roundedCorners = roundedCorners)
    assertThat(result).isNotNull()
    assertThat(result!!.path).hasSize(1)
    val bezier = result.path[0]
    assertThat(bezier.vertices).hasSize(6)
    // Corner 0 start point at (0, 10)
    assertThat(bezier.vertices[0][0].constantValueOrNull).isWithin(0.01f).of(0f)
    assertThat(bezier.vertices[0][1].constantValueOrNull).isWithin(0.01f).of(10f)
  }

  @Test
  fun gatherShapes_withRoundedCornersAndFill() {
    val triangle =
      BezierValue(
        closed = true,
        vertices = listOf(listOf(0f, 0f), listOf(100f, 0f), listOf(0f, 100f)),
        inTangents = listOf(listOf(0f, 0f), listOf(0f, 0f), listOf(0f, 0f)),
        outTangents = listOf(listOf(0f, 0f), listOf(0f, 0f), listOf(0f, 0f)),
      )
    val elements =
      listOf(
        Path(shape = StaticBezierProperty(value = triangle)),
        RoundedCorners(radius = StaticScalarProperty(value = 12f)),
        Fill(color = StaticColorProperty(value = androidx.compose.ui.graphics.Color.Red.rc)),
      )

    val shapeGroups = gatherShapesForTest(elements, settings)
    assertThat(shapeGroups).hasSize(1)
    val group = shapeGroups[0]
    assertThat(group.shapes).hasSize(1)
    val lottiePath = group.shapes[0] as RemoteLottiePath
    assertThat(lottiePath.path[0].vertices).hasSize(6)
  }

  @Test
  fun gatherShapes_withRoundedCornersAndTrimPath() {
    val triangle =
      BezierValue(
        closed = true,
        vertices = listOf(listOf(0f, 0f), listOf(100f, 0f), listOf(0f, 100f)),
        inTangents = listOf(listOf(0f, 0f), listOf(0f, 0f), listOf(0f, 0f)),
        outTangents = listOf(listOf(0f, 0f), listOf(0f, 0f), listOf(0f, 0f)),
      )
    val elements =
      listOf(
        Path(shape = StaticBezierProperty(value = triangle)),
        RoundedCorners(radius = StaticScalarProperty(value = 10f)),
        TrimPath(
          start = StaticScalarProperty(value = 0f),
          end = StaticScalarProperty(value = 50f),
          offset = StaticScalarProperty(value = 0f),
        ),
        Fill(color = StaticColorProperty(value = androidx.compose.ui.graphics.Color.Red.rc)),
      )

    val shapeGroups = gatherShapesForTest(elements, settings)
    assertThat(shapeGroups).hasSize(1)
    val group = shapeGroups[0]
    assertThat(group.shapes).hasSize(1)
    val lottiePath = group.shapes[0] as RemoteLottiePath
    assertThat(lottiePath.path).isNotEmpty()
  }
}
