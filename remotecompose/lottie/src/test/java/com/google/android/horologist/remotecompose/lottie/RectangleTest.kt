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
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.Rectangle
import com.google.android.horologist.remotecompose.lottie.format.properties.AnimatedScalarProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.ScalarPropertyKeyframe
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticPositionProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticScalarProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticVectorProperty
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.evaluateRectangle
import com.google.common.truth.Truth.assertThat
import org.junit.Test

@SuppressLint("RestrictedApi")
class RectangleTest {

  private val settings = LottieSettings(0f.rf, SlotMap(emptyMap()))

  @Test
  fun evaluateStaticRectangleWithZeroRadius() {
    val rect =
      Rectangle(
        position = StaticPositionProperty(value = listOf(0f, 0f)),
        size = StaticVectorProperty(value = listOf(100f, 200f)),
        cornerRadius = StaticScalarProperty(value = 0f),
      )

    val result = evaluateRectangle(rect, settings)
    assertThat(result).isNotNull()
    val subpath = result!!.path.first()
    assertThat(subpath.closed).isTrue()
    assertThat(subpath.vertices).hasSize(8)

    // With corner radius 0, vertices degenerate to rectangular corners
    assertThat(subpath.vertices[0][0].constantValueOrNull).isWithin(0.01f).of(50f)
    assertThat(subpath.vertices[0][1].constantValueOrNull).isWithin(0.01f).of(-100f)

    assertThat(subpath.vertices[1][0].constantValueOrNull).isWithin(0.01f).of(50f)
    assertThat(subpath.vertices[1][1].constantValueOrNull).isWithin(0.01f).of(100f)

    assertThat(subpath.inTangents[0][0].constantValueOrNull).isWithin(0.01f).of(0f)
    assertThat(subpath.inTangents[0][1].constantValueOrNull).isWithin(0.01f).of(0f)
  }

  @Test
  fun evaluateStaticRectangleWithRadius() {
    val rect =
      Rectangle(
        position = StaticPositionProperty(value = listOf(75f, 0f)),
        size = StaticVectorProperty(value = listOf(20f, 111f)),
        cornerRadius = StaticScalarProperty(value = 10f),
      )

    val result = evaluateRectangle(rect, settings)
    assertThat(result).isNotNull()
    val subpath = result!!.path.first()
    assertThat(subpath.closed).isTrue()
    assertThat(subpath.vertices).hasSize(8)

    // HalfWidth = 10, HalfHeight = 55.5, rr = 10, kr = 5.5228475
    // pos.x = 75, pos.y = 0
    // vertex 0: (75 + 10, 0 - 55.5 + 10) = (85, -45.5)
    assertThat(subpath.vertices[0][0].constantValueOrNull).isWithin(0.01f).of(85f)
    assertThat(subpath.vertices[0][1].constantValueOrNull).isWithin(0.01f).of(-45.5f)

    // vertex 1: (75 + 10, 0 + 55.5 - 10) = (85, 45.5)
    assertThat(subpath.vertices[1][0].constantValueOrNull).isWithin(0.01f).of(85f)
    assertThat(subpath.vertices[1][1].constantValueOrNull).isWithin(0.01f).of(45.5f)

    // vertex 2: (75 + 10 - 10, 0 + 55.5) = (75, 55.5)
    assertThat(subpath.vertices[2][0].constantValueOrNull).isWithin(0.01f).of(75f)
    assertThat(subpath.vertices[2][1].constantValueOrNull).isWithin(0.01f).of(55.5f)

    // Tangents for corner arcs: kr ≈ 5.5228f
    assertThat(subpath.outTangents[1][1].constantValueOrNull).isWithin(0.01f).of(5.5228f)
    assertThat(subpath.inTangents[2][0].constantValueOrNull).isWithin(0.01f).of(5.5228f)
  }

  @Test
  fun evaluateRectangleWithClampedRadius() {
    val rect =
      Rectangle(
        position = StaticPositionProperty(value = listOf(0f, 0f)),
        size = StaticVectorProperty(value = listOf(20f, 100f)),
        cornerRadius = StaticScalarProperty(value = 50f), // greater than halfWidth (10)
      )

    val result = evaluateRectangle(rect, settings)
    assertThat(result).isNotNull()
    val subpath = result!!.path.first()

    // Max radius clamped to min(halfWidth=10, halfHeight=50) = 10
    // vertex 2: pos.x + halfWidth - clampedR = 0 + 10 - 10 = 0
    assertThat(subpath.vertices[2][0].constantValueOrNull).isWithin(0.01f).of(0f)
    assertThat(subpath.vertices[2][1].constantValueOrNull).isWithin(0.01f).of(50f)
  }

  @Test
  fun evaluateAnimatedRectangleRadius() {
    val rect =
      Rectangle(
        position = StaticPositionProperty(value = listOf(75f, 0f)),
        size = StaticVectorProperty(value = listOf(20f, 111f)),
        cornerRadius =
          AnimatedScalarProperty(
            keyframes =
              listOf(
                ScalarPropertyKeyframe(frame = 0f, value = 8f),
                ScalarPropertyKeyframe(frame = 14f, value = 10f),
              )
          ),
      )

    val result = evaluateRectangle(rect, settings)
    assertThat(result).isNotNull()
    val subpath = result!!.path.first()
    assertThat(subpath.vertices).hasSize(8)
    assertThat(subpath.inTangents).hasSize(8)
    assertThat(subpath.outTangents).hasSize(8)
  }

  @Test
  fun evaluateHiddenRectangleReturnsNull() {
    val rect =
      Rectangle(
        hidden = true,
        position = StaticPositionProperty(value = listOf(0f, 0f)),
        size = StaticVectorProperty(value = listOf(100f, 100f)),
        cornerRadius = StaticScalarProperty(value = 10f),
      )

    val result = evaluateRectangle(rect, settings)
    assertThat(result).isNull()
  }
}
