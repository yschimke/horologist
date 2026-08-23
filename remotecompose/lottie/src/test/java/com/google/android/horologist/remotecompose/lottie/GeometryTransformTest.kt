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
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.grouping.Transform
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticPositionProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticScalarProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticVectorProperty
import com.google.android.horologist.remotecompose.lottie.renderer.RemoteLottiePath
import com.google.android.horologist.remotecompose.lottie.renderer.properties.RemoteBezierValue
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.transformBezierValue
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.transformLottiePath
import com.google.common.truth.Truth.assertThat
import org.junit.Test

@SuppressLint("RestrictedApi")
class GeometryTransformTest {

  private val settings = LottieSettings(0f.rf, SlotMap(emptyMap()))

  @Test
  fun transformScale() {
    val subpath =
      RemoteBezierValue(
        closed = true,
        vertices = listOf(listOf(100f.rf, 200f.rf)),
        inTangents = listOf(listOf(10f.rf, 20f.rf)),
        outTangents = listOf(listOf(-10f.rf, -20f.rf)),
      )

    val transform =
      Transform(
        anchorPoint = StaticPositionProperty(value = listOf(0f, 0f)),
        positionTranslation = StaticPositionProperty(value = listOf(0f, 0f)),
        scale = StaticVectorProperty(value = listOf(50f, 50f)),
        rotation = StaticScalarProperty(value = 0f),
        opacity = StaticScalarProperty(value = 100f),
      )

    val result = transformBezierValue(subpath, transform, settings)
    assertThat(result.vertices[0][0].constantValueOrNull).isWithin(0.01f).of(50f)
    assertThat(result.vertices[0][1].constantValueOrNull).isWithin(0.01f).of(100f)

    assertThat(result.inTangents[0][0].constantValueOrNull).isWithin(0.01f).of(5f)
    assertThat(result.inTangents[0][1].constantValueOrNull).isWithin(0.01f).of(10f)
    assertThat(result.outTangents[0][0].constantValueOrNull).isWithin(0.01f).of(-5f)
    assertThat(result.outTangents[0][1].constantValueOrNull).isWithin(0.01f).of(-10f)
  }

  @Test
  fun transformTranslationAndAnchorPoint() {
    val subpath =
      RemoteBezierValue(
        closed = false,
        vertices = listOf(listOf(10f.rf, 20f.rf)),
        inTangents = listOf(listOf(5f.rf, 5f.rf)),
        outTangents = listOf(listOf(5f.rf, 5f.rf)),
      )

    val transform =
      Transform(
        anchorPoint = StaticPositionProperty(value = listOf(10f, 20f)),
        positionTranslation = StaticPositionProperty(value = listOf(100f, 200f)),
        scale = StaticVectorProperty(value = listOf(100f, 100f)),
        rotation = StaticScalarProperty(value = 0f),
        opacity = StaticScalarProperty(value = 100f),
      )

    val result = transformBezierValue(subpath, transform, settings)
    // Point (10, 20) shifted by anchor (10, 20) -> (0, 0) + translation (100, 200) -> (100, 200)
    assertThat(result.vertices[0][0].constantValueOrNull).isWithin(0.01f).of(100f)
    assertThat(result.vertices[0][1].constantValueOrNull).isWithin(0.01f).of(200f)

    // Tangents are unchanged by anchor point and translation
    assertThat(result.inTangents[0][0].constantValueOrNull).isWithin(0.01f).of(5f)
    assertThat(result.inTangents[0][1].constantValueOrNull).isWithin(0.01f).of(5f)
  }

  @Test
  fun transformRotation() {
    val subpath =
      RemoteBezierValue(
        closed = true,
        vertices = listOf(listOf(100f.rf, 0f.rf)),
        inTangents = listOf(listOf(10f.rf, 0f.rf)),
        outTangents = listOf(listOf(0f.rf, 10f.rf)),
      )

    val transform =
      Transform(
        anchorPoint = StaticPositionProperty(value = listOf(0f, 0f)),
        positionTranslation = StaticPositionProperty(value = listOf(0f, 0f)),
        scale = StaticVectorProperty(value = listOf(100f, 100f)),
        rotation = StaticScalarProperty(value = 90f),
        opacity = StaticScalarProperty(value = 100f),
      )

    val result = transformBezierValue(subpath, transform, settings)
    // Rotating (100, 0) by 90 degrees clockwise -> (0, 100)
    assertThat(result.vertices[0][0].constantValueOrNull).isWithin(0.01f).of(0f)
    assertThat(result.vertices[0][1].constantValueOrNull).isWithin(0.01f).of(100f)

    // InTangent (10, 0) -> (0, 10)
    assertThat(result.inTangents[0][0].constantValueOrNull).isWithin(0.01f).of(0f)
    assertThat(result.inTangents[0][1].constantValueOrNull).isWithin(0.01f).of(10f)

    // OutTangent (0, 10) -> (-10, 0)
    assertThat(result.outTangents[0][0].constantValueOrNull).isWithin(0.01f).of(-10f)
    assertThat(result.outTangents[0][1].constantValueOrNull).isWithin(0.01f).of(0f)
  }

  @Test
  fun transformSkew() {
    val subpath =
      RemoteBezierValue(
        closed = true,
        vertices = listOf(listOf(0f.rf, 100f.rf)),
        inTangents = listOf(listOf(0f.rf, 10f.rf)),
        outTangents = listOf(listOf(0f.rf, -10f.rf)),
      )

    val transform =
      Transform(
        anchorPoint = StaticPositionProperty(value = listOf(0f, 0f)),
        positionTranslation = StaticPositionProperty(value = listOf(0f, 0f)),
        scale = StaticVectorProperty(value = listOf(100f, 100f)),
        rotation = StaticScalarProperty(value = 0f),
        opacity = StaticScalarProperty(value = 100f),
        skew = StaticScalarProperty(value = 45f),
      )

    val result = transformBezierValue(subpath, transform, settings)
    // Point (0, 100) skewed horizontally by 45 deg -> (-100, 100)
    assertThat(result.vertices[0][0].constantValueOrNull).isWithin(0.01f).of(-100f)
    assertThat(result.vertices[0][1].constantValueOrNull).isWithin(0.01f).of(100f)

    // InTangent (0, 10) skewed -> (-10, 10)
    assertThat(result.inTangents[0][0].constantValueOrNull).isWithin(0.01f).of(-10f)
    assertThat(result.inTangents[0][1].constantValueOrNull).isWithin(0.01f).of(10f)
  }

  @Test
  fun transformLottiePathWrapper() {
    val subpath =
      RemoteBezierValue(
        closed = true,
        vertices = listOf(listOf(10f.rf, 10f.rf)),
        inTangents = listOf(listOf(0f.rf, 0f.rf)),
        outTangents = listOf(listOf(0f.rf, 0f.rf)),
      )
    val lottiePath = RemoteLottiePath(listOf(subpath))

    val transform =
      Transform(
        anchorPoint = StaticPositionProperty(value = listOf(0f, 0f)),
        positionTranslation = StaticPositionProperty(value = listOf(50f, 50f)),
        scale = StaticVectorProperty(value = listOf(200f, 200f)),
        rotation = StaticScalarProperty(value = 0f),
        opacity = StaticScalarProperty(value = 100f),
      )

    val result = transformLottiePath(lottiePath, transform, settings)
    assertThat(result.path).hasSize(1)
    assertThat(result.path[0].vertices[0][0].constantValueOrNull).isWithin(0.01f).of(70f)
    assertThat(result.path[0].vertices[0][1].constantValueOrNull).isWithin(0.01f).of(70f)
  }
}
