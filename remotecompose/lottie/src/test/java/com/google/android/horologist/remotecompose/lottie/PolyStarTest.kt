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
import androidx.compose.remote.creation.compose.state.rf
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.PolyStar
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.PolyStarType
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.grouping.Transform
import com.google.android.horologist.remotecompose.lottie.format.properties.AnimatedScalarProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.ScalarPropertyKeyframe
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticPositionProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticScalarProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticVectorProperty
import com.google.android.horologist.remotecompose.lottie.renderer.RemoteLottiePath
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.evaluatePolyStar
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.transformRemoteShape
import com.google.common.truth.Truth.assertThat
import kotlin.math.cos
import kotlin.math.sin
import org.junit.Test

@SuppressLint("RestrictedApi")
class PolyStarTest {

  private val settings = LottieSettings(0f.rf, SlotMap(emptyMap()))

  @Test
  fun evaluateStaticStarWithZeroRoundedness() {
    val star =
      PolyStar(
        starType = PolyStarType.Star,
        points = StaticScalarProperty(value = 5f),
        position = StaticPositionProperty(value = listOf(100f, 100f)),
        rotation = StaticScalarProperty(value = 0f),
        innerRadius = StaticScalarProperty(value = 40f),
        outerRadius = StaticScalarProperty(value = 100f),
        innerRoundedness = StaticScalarProperty(value = 0f),
        outerRoundedness = StaticScalarProperty(value = 0f),
      )

    val result = evaluatePolyStar(star, settings)
    assertThat(result).isNotNull()
    assertThat(result).isInstanceOf(RemoteLottiePath::class.java)

    val subpath = result!!.path.first()
    assertThat(subpath.closed).isTrue()
    assertThat(subpath.vertices).hasSize(10)
    assertThat(subpath.inTangents).hasSize(10)
    assertThat(subpath.outTangents).hasSize(10)

    // First vertex (outer point at angle = rotation - 90 = -90 deg): (0, -100) + pos (100, 100) =
    // (100, 0)
    assertThat(subpath.vertices[0][0].constantValueOrNull).isWithin(0.01f).of(100f)
    assertThat(subpath.vertices[0][1].constantValueOrNull).isWithin(0.01f).of(0f)

    // Second vertex (inner point at angle = -90 + 36 = -54 deg): (40 * cos(-54°), 40 * sin(-54°)) +
    // (100, 100)
    val rad54 = Math.toRadians(-54.0)
    val expectedX1 = (40.0 * cos(rad54)).toFloat() + 100f
    val expectedY1 = (40.0 * sin(rad54)).toFloat() + 100f
    assertThat(subpath.vertices[1][0].constantValueOrNull).isWithin(0.01f).of(expectedX1)
    assertThat(subpath.vertices[1][1].constantValueOrNull).isWithin(0.01f).of(expectedY1)

    // Zero roundedness means zero tangents
    for (i in 0 until 10) {
      assertThat(subpath.inTangents[i][0].constantValueOrNull).isWithin(0.001f).of(0f)
      assertThat(subpath.inTangents[i][1].constantValueOrNull).isWithin(0.001f).of(0f)
      assertThat(subpath.outTangents[i][0].constantValueOrNull).isWithin(0.001f).of(0f)
      assertThat(subpath.outTangents[i][1].constantValueOrNull).isWithin(0.001f).of(0f)
    }
  }

  @Test
  fun evaluateStarWithRoundedness() {
    val star =
      PolyStar(
        starType = PolyStarType.Star,
        points = StaticScalarProperty(value = 5f),
        position = StaticPositionProperty(value = listOf(0f, 0f)),
        rotation = StaticScalarProperty(value = 0f),
        innerRadius = StaticScalarProperty(value = 50f),
        outerRadius = StaticScalarProperty(value = 100f),
        innerRoundedness = StaticScalarProperty(value = 20f),
        outerRoundedness = StaticScalarProperty(value = 30f),
      )

    val result = evaluatePolyStar(star, settings)
    assertThat(result).isNotNull()
    val subpath = result!!.path.first()

    // With non-zero roundedness, control point tangents are generated
    var hasNonZeroTangents = false
    for (i in 0 until 10) {
      val inX = subpath.inTangents[i][0].constantValueOrNull ?: 0f
      val inY = subpath.inTangents[i][1].constantValueOrNull ?: 0f
      val outX = subpath.outTangents[i][0].constantValueOrNull ?: 0f
      val outY = subpath.outTangents[i][1].constantValueOrNull ?: 0f
      if (inX != 0f || inY != 0f || outX != 0f || outY != 0f) {
        hasNonZeroTangents = true
      }
    }
    assertThat(hasNonZeroTangents).isTrue()
  }

  @Test
  fun evaluateStaticPolygonWithZeroRoundedness() {
    val polygon =
      PolyStar(
        starType = PolyStarType.Polygon,
        points = StaticScalarProperty(value = 6f),
        position = StaticPositionProperty(value = listOf(50f, 50f)),
        rotation = StaticScalarProperty(value = 0f),
        outerRadius = StaticScalarProperty(value = 60f),
        outerRoundedness = StaticScalarProperty(value = 0f),
      )

    val result = evaluatePolyStar(polygon, settings)
    assertThat(result).isNotNull()
    assertThat(result).isInstanceOf(RemoteLottiePath::class.java)

    val subpath = result!!.path.first()
    assertThat(subpath.closed).isTrue()
    assertThat(subpath.vertices).hasSize(6)
    assertThat(subpath.inTangents).hasSize(6)
    assertThat(subpath.outTangents).hasSize(6)

    // First vertex (at angle = -90 deg): (0, -60) + pos (50, 50) = (50, -10)
    assertThat(subpath.vertices[0][0].constantValueOrNull).isWithin(0.01f).of(50f)
    assertThat(subpath.vertices[0][1].constantValueOrNull).isWithin(0.01f).of(-10f)

    // All tangents should be zero for zero roundedness
    for (i in 0 until 6) {
      assertThat(subpath.inTangents[i][0].constantValueOrNull).isWithin(0.001f).of(0f)
      assertThat(subpath.inTangents[i][1].constantValueOrNull).isWithin(0.001f).of(0f)
      assertThat(subpath.outTangents[i][0].constantValueOrNull).isWithin(0.001f).of(0f)
      assertThat(subpath.outTangents[i][1].constantValueOrNull).isWithin(0.001f).of(0f)
    }
  }

  @Test
  fun evaluatePolygonWithRoundedness() {
    val polygon =
      PolyStar(
        starType = PolyStarType.Polygon,
        points = StaticScalarProperty(value = 4f),
        position = StaticPositionProperty(value = listOf(0f, 0f)),
        rotation = StaticScalarProperty(value = 45f),
        outerRadius = StaticScalarProperty(value = 80f),
        outerRoundedness = StaticScalarProperty(value = 15f),
      )

    val result = evaluatePolyStar(polygon, settings)
    assertThat(result).isNotNull()
    val subpath = result!!.path.first()
    assertThat(subpath.vertices).hasSize(4)

    var hasNonZeroTangents = false
    for (i in 0 until 4) {
      val inX = subpath.inTangents[i][0].constantValueOrNull ?: 0f
      val inY = subpath.inTangents[i][1].constantValueOrNull ?: 0f
      if (inX != 0f || inY != 0f) {
        hasNonZeroTangents = true
      }
    }
    assertThat(hasNonZeroTangents).isTrue()
  }

  @Test
  fun evaluateAnimatedPolyStar() {
    val star =
      PolyStar(
        starType = PolyStarType.Star,
        points = StaticScalarProperty(value = 5f),
        position = StaticPositionProperty(value = listOf(0f, 0f)),
        rotation =
          AnimatedScalarProperty(
            keyframes =
              listOf(
                ScalarPropertyKeyframe(frame = 0f, value = 0f),
                ScalarPropertyKeyframe(frame = 30f, value = 180f),
              )
          ),
        innerRadius = StaticScalarProperty(value = 30f),
        outerRadius = StaticScalarProperty(value = 70f),
        innerRoundedness = StaticScalarProperty(value = 0f),
        outerRoundedness = StaticScalarProperty(value = 0f),
      )

    val result = evaluatePolyStar(star, settings)
    assertThat(result).isNotNull()
    val subpath = result!!.path.first()
    assertThat(subpath.vertices).hasSize(10)
  }

  @Test
  fun evaluateHiddenPolyStarReturnsNull() {
    val star =
      PolyStar(
        hidden = true,
        starType = PolyStarType.Star,
        points = StaticScalarProperty(value = 5f),
        position = StaticPositionProperty(value = listOf(0f, 0f)),
        innerRadius = StaticScalarProperty(value = 20f),
        outerRadius = StaticScalarProperty(value = 50f),
      )

    val result = evaluatePolyStar(star, settings)
    assertThat(result).isNull()
  }

  @Test
  fun polyStarTransformsWithGroupGeometryTransform() {
    val star =
      PolyStar(
        starType = PolyStarType.Star,
        points = StaticScalarProperty(value = 5f),
        position = StaticPositionProperty(value = listOf(0f, 0f)),
        rotation = StaticScalarProperty(value = 0f),
        innerRadius = StaticScalarProperty(value = 50f),
        outerRadius = StaticScalarProperty(value = 100f),
        innerRoundedness = StaticScalarProperty(value = 0f),
        outerRoundedness = StaticScalarProperty(value = 0f),
      )

    val shape = evaluatePolyStar(star, settings)
    assertThat(shape).isNotNull()

    val transform =
      Transform(
        anchorPoint = StaticPositionProperty(value = listOf(0f, 0f)),
        positionTranslation = StaticPositionProperty(value = listOf(50f, 50f)),
        scale = StaticVectorProperty(value = listOf(200f, 200f)),
        rotation = StaticScalarProperty(value = 0f),
        opacity = StaticScalarProperty(value = 100f),
      )

    val transformedShape = transformRemoteShape(shape!!, transform, settings)
    assertThat(transformedShape).isInstanceOf(RemoteLottiePath::class.java)

    val transformedLottiePath = transformedShape as RemoteLottiePath
    val subpath = transformedLottiePath.path.first()

    // Original vertex 0 was (0, -100).
    // Scaled by 2.0 -> (0, -200) + translated by (50, 50) -> (50, -150)
    assertThat(subpath.vertices[0][0].constantValueOrNull).isWithin(0.01f).of(50f)
    assertThat(subpath.vertices[0][1].constantValueOrNull).isWithin(0.01f).of(-150f)
  }
}
