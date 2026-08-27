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
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.Ellipse
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.PolyStar
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.PolyStarType
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.Rectangle
import com.google.android.horologist.remotecompose.lottie.format.properties.AnimatedPositionProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.AnimatedScalarProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.AnimatedVectorProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.KeyframeEasing
import com.google.android.horologist.remotecompose.lottie.format.properties.PositionPropertyKeyframe
import com.google.android.horologist.remotecompose.lottie.format.properties.ScalarKeyframeEasing
import com.google.android.horologist.remotecompose.lottie.format.properties.ScalarPropertyKeyframe
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticScalarProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.VectorPropertyKeyframe
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.evaluateEllipse
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.evaluatePolyStar
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.evaluateRectangle
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SuppressLint("RestrictedApi")
@RunWith(AndroidJUnit4::class)
class PrimitiveKeyframeEvaluationTest {

  @Test
  fun evaluateRectangle_withAnimatedSizeAndPosition_interpolatesAtCurrentFrame() {
    val linearPosEasing = KeyframeEasing(0f, 0f)

    val sizeProp =
      AnimatedVectorProperty(
        keyframes =
          listOf(
            VectorPropertyKeyframe(
              frame = 0f,
              value = listOf(100f, 100f),
              outTangent = linearPosEasing,
            ),
            VectorPropertyKeyframe(
              frame = 20f,
              value = listOf(200f, 300f),
              inTangent = linearPosEasing,
            ),
          )
      )

    val posProp =
      AnimatedPositionProperty(
        keyframes =
          listOf(
            PositionPropertyKeyframe(
              frame = 0f,
              value = listOf(0f, 0f),
              outTangent = linearPosEasing,
            ),
            PositionPropertyKeyframe(
              frame = 20f,
              value = listOf(50f, 50f),
              inTangent = linearPosEasing,
            ),
          )
      )

    val rect =
      Rectangle(
        position = posProp,
        size = sizeProp,
        cornerRadius = StaticScalarProperty(value = 0f),
      )

    // At frame 0: width = 100, height = 100, pos = (0, 0)
    val settings0 = LottieSettings(currentFrame = 0f.rf)
    val path0 = evaluateRectangle(rect, settings0)
    assertThat(path0).isNotNull()
    val subpath0 = path0!!.path[0]
    // Top-right vertex at frame 0: posX + halfW = 0 + 50 = 50, posY - halfH = 0 - 50 = -50
    assertThat(subpath0.vertices[0][0].constantValueOrNull).isWithin(0.01f).of(50f)
    assertThat(subpath0.vertices[0][1].constantValueOrNull).isWithin(0.01f).of(-50f)

    // At frame 10 (halfway): width = 150, height = 200, pos = (25, 25)
    val settings10 = LottieSettings(currentFrame = 10f.rf)
    val path10 = evaluateRectangle(rect, settings10)
    assertThat(path10).isNotNull()
    val subpath10 = path10!!.path[0]
    // Top-right vertex at frame 10: posX + halfW = 25 + 75 = 100, posY - halfH = 25 - 100 = -75
    assertThat(subpath10.vertices[0][0].constantValueOrNull).isWithin(0.01f).of(100f)
    assertThat(subpath10.vertices[0][1].constantValueOrNull).isWithin(0.01f).of(-75f)

    // At frame 20: width = 200, height = 300, pos = (50, 50)
    val settings20 = LottieSettings(currentFrame = 20f.rf)
    val path20 = evaluateRectangle(rect, settings20)
    assertThat(path20).isNotNull()
    val subpath20 = path20!!.path[0]
    // Top-right vertex at frame 20: posX + halfW = 50 + 100 = 150, posY - halfH = 50 - 150 = -100
    assertThat(subpath20.vertices[0][0].constantValueOrNull).isWithin(0.01f).of(150f)
    assertThat(subpath20.vertices[0][1].constantValueOrNull).isWithin(0.01f).of(-100f)
  }

  @Test
  fun evaluateEllipse_withAnimatedSize_interpolatesAtCurrentFrame() {
    val linearPosEasing = KeyframeEasing(0f, 0f)

    val sizeProp =
      AnimatedVectorProperty(
        keyframes =
          listOf(
            VectorPropertyKeyframe(
              frame = 0f,
              value = listOf(40f, 40f),
              outTangent = linearPosEasing,
            ),
            VectorPropertyKeyframe(
              frame = 20f,
              value = listOf(80f, 100f),
              inTangent = linearPosEasing,
            ),
          )
      )

    val ellipse = Ellipse(size = sizeProp)

    // At frame 0: size = 40x40 -> top vertex (posX, posY - halfH) = (0, -20)
    val settings0 = LottieSettings(currentFrame = 0f.rf)
    val path0 = evaluateEllipse(ellipse, settings0)
    assertThat(path0).isNotNull()
    val subpath0 = path0!!.path[0]
    assertThat(subpath0.vertices[0][1].constantValueOrNull).isWithin(0.01f).of(-20f)

    // At frame 10 (halfway): size = 60x70 -> top vertex (0, -35)
    val settings10 = LottieSettings(currentFrame = 10f.rf)
    val path10 = evaluateEllipse(ellipse, settings10)
    assertThat(path10).isNotNull()
    val subpath10 = path10!!.path[0]
    assertThat(subpath10.vertices[0][1].constantValueOrNull).isWithin(0.01f).of(-35f)

    // At frame 20: size = 80x100 -> top vertex (0, -50)
    val settings20 = LottieSettings(currentFrame = 20f.rf)
    val path20 = evaluateEllipse(ellipse, settings20)
    assertThat(path20).isNotNull()
    val subpath20 = path20!!.path[0]
    assertThat(subpath20.vertices[0][1].constantValueOrNull).isWithin(0.01f).of(-50f)
  }

  @Test
  fun evaluatePolyStar_withAnimatedRadius_interpolatesAtCurrentFrame() {
    val linearScalarEasing = ScalarKeyframeEasing(x = 0f, y = 0f)

    val radiusProp =
      AnimatedScalarProperty(
        keyframes =
          listOf(
            ScalarPropertyKeyframe(frame = 0f, value = 20f, outTangent = linearScalarEasing),
            ScalarPropertyKeyframe(frame = 20f, value = 60f, inTangent = linearScalarEasing),
          )
      )

    val star =
      PolyStar(
        starType = PolyStarType.Polygon,
        points = StaticScalarProperty(value = 4f),
        outerRadius = radiusProp,
      )

    // At frame 0: outerRadius = 20
    val settings0 = LottieSettings(currentFrame = 0f.rf)
    val path0 = evaluatePolyStar(star, settings0)
    assertThat(path0).isNotNull()

    // At frame 10 (halfway): outerRadius = 40
    val settings10 = LottieSettings(currentFrame = 10f.rf)
    val path10 = evaluatePolyStar(star, settings10)
    assertThat(path10).isNotNull()

    // At frame 20: outerRadius = 60
    val settings20 = LottieSettings(currentFrame = 20f.rf)
    val path20 = evaluatePolyStar(star, settings20)
    assertThat(path20).isNotNull()
  }
}
