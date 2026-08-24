/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.horologist.remotecompose.lottie

import androidx.compose.remote.creation.compose.shaders.RemoteLinearShader
import androidx.compose.remote.creation.compose.shaders.RemoteRadialShader
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.GradientType
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.LineCap
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.LineJoin
import com.google.android.horologist.remotecompose.lottie.format.properties.AnimatedGradientProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.GradientPropertyKeyframe
import com.google.android.horologist.remotecompose.lottie.format.values.GradientValue
import com.google.android.horologist.remotecompose.lottie.renderer.RemoteGradientFill
import com.google.android.horologist.remotecompose.lottie.renderer.RemoteGradientStroke
import com.google.android.horologist.remotecompose.lottie.renderer.properties.Point
import com.google.android.horologist.remotecompose.lottie.renderer.properties.RemoteGradientValue
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animateGradient
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GradientTest {

  private val emptySlotMap = SlotMap.Empty

  @Test
  fun remoteGradientFill_linearGradient_createsLinearShaderWithColorsAndPositions() {
    // 2 color stops: 0% -> Red, 100% -> Blue
    val gradientValue =
      RemoteGradientValue(
        numberOfColors = 2,
        values =
          listOf(
            0.0f.rf,
            1.0f.rf,
            0.0f.rf,
            0.0f.rf, // stop 0: red
            1.0f.rf,
            0.0f.rf,
            0.0f.rf,
            1.0f.rf, // stop 1: blue
          ),
      )

    val gradientFill =
      RemoteGradientFill(
        gradient = gradientValue,
        startPoint = Point(0f.rf, 10f.rf),
        endPoint = Point(100f.rf, 110f.rf),
        gradientType = GradientType.Linear,
        opacity = 100f.rf,
      )

    val paint = gradientFill.getPaint(inheritedOpacity = 1f.rf)

    assertThat(paint.style).isEqualTo(PaintingStyle.Fill)
    assertThat(paint.shader).isInstanceOf(RemoteLinearShader::class.java)

    val shader = paint.shader as RemoteLinearShader
    assertThat(shader.x0.constantValue).isEqualTo(0f)
    assertThat(shader.y0.constantValue).isEqualTo(10f)
    assertThat(shader.x1.constantValue).isEqualTo(100f)
    assertThat(shader.y1.constantValue).isEqualTo(110f)

    assertThat(shader.colors).hasSize(2)
    assertThat(shader.colors[0].constantValueOrNull).isEqualTo(Color(1f, 0f, 0f, 1f))
    assertThat(shader.colors[1].constantValueOrNull).isEqualTo(Color(0f, 0f, 1f, 1f))

    assertThat(shader.positions).isNotNull()
    assertThat(shader.positions!![0].constantValue).isEqualTo(0f)
    assertThat(shader.positions!![1].constantValue).isEqualTo(1f)
  }

  @Test
  fun remoteGradientFill_radialGradient_calculatesEuclideanRadiusAndCenter() {
    // Center at (50, 50), Edge at (50, 80) -> Radius = 30
    val gradientValue =
      RemoteGradientValue(
        numberOfColors = 2,
        values =
          listOf(
            0.0f.rf,
            0.0f.rf,
            1.0f.rf,
            0.0f.rf, // stop 0: green
            1.0f.rf,
            1.0f.rf,
            1.0f.rf,
            0.0f.rf, // stop 1: yellow
          ),
      )

    val gradientFill =
      RemoteGradientFill(
        gradient = gradientValue,
        startPoint = Point(50f.rf, 50f.rf),
        endPoint = Point(50f.rf, 80f.rf),
        gradientType = GradientType.Radial,
        opacity = 100f.rf,
      )

    val paint = gradientFill.getPaint(inheritedOpacity = 1f.rf)

    assertThat(paint.style).isEqualTo(PaintingStyle.Fill)
    assertThat(paint.shader).isInstanceOf(RemoteRadialShader::class.java)

    val shader = paint.shader as RemoteRadialShader
    assertThat(shader.centerX.constantValue).isEqualTo(50f)
    assertThat(shader.centerY.constantValue).isEqualTo(50f)
    assertThat(shader.radius.constantValue).isEqualTo(30f)

    assertThat(shader.colors).hasSize(2)
    assertThat(shader.colors[0].constantValueOrNull).isEqualTo(Color(0f, 1f, 0f, 1f))
    assertThat(shader.colors[1].constantValueOrNull).isEqualTo(Color(1f, 1f, 0f, 1f))
  }

  @Test
  fun remoteGradientFill_withAlphaStopsAndInheritedOpacity_scalesColorAlpha() {
    // 2 color stops + 2 alpha stops (50% alpha and 100% alpha)
    // Layer opacity = 80%, Inherited opacity = 50% -> total scale = 0.4
    val gradientValue =
      RemoteGradientValue(
        numberOfColors = 2,
        values =
          listOf(
            0.0f.rf,
            1.0f.rf,
            0.0f.rf,
            0.0f.rf, // color stop 0: red
            1.0f.rf,
            0.0f.rf,
            1.0f.rf,
            0.0f.rf, // color stop 1: green
            0.0f.rf,
            0.5f.rf, // alpha stop 0: 50%
            1.0f.rf,
            1.0f.rf, // alpha stop 1: 100%
          ),
      )

    val gradientFill =
      RemoteGradientFill(
        gradient = gradientValue,
        startPoint = Point(0f.rf, 0f.rf),
        endPoint = Point(100f.rf, 0f.rf),
        gradientType = GradientType.Linear,
        opacity = 80f.rf,
      )

    val paint = gradientFill.getPaint(inheritedOpacity = 0.5f.rf)
    val shader = paint.shader as RemoteLinearShader

    assertThat(shader.colors).hasSize(2)
    // stop 0 alpha: 0.5 * 0.8 * 0.5 = 0.2
    assertThat(shader.colors[0].constantValueOrNull).isEqualTo(Color(1f, 0f, 0f, 0.2f))
    // stop 1 alpha: 1.0 * 0.8 * 0.5 = 0.4
    assertThat(shader.colors[1].constantValueOrNull).isEqualTo(Color(0f, 1f, 0f, 0.4f))
  }

  @Test
  fun remoteGradientFill_emptyValues_returnsFallbackTransparentColors() {
    val gradientValue = RemoteGradientValue(numberOfColors = 0, values = emptyList())

    val gradientFill =
      RemoteGradientFill(
        gradient = gradientValue,
        startPoint = Point(0f.rf, 0f.rf),
        endPoint = Point(10f.rf, 10f.rf),
        gradientType = GradientType.Linear,
        opacity = 100f.rf,
      )

    val paint = gradientFill.getPaint(inheritedOpacity = 1f.rf)
    val shader = paint.shader as RemoteLinearShader

    assertThat(shader.colors).hasSize(2)
    assertThat(shader.colors[0].constantValueOrNull).isEqualTo(Color.Transparent)
    assertThat(shader.colors[1].constantValueOrNull).isEqualTo(Color.Transparent)
  }

  @Test
  fun remoteGradientStroke_configuresStrokeStyleAndParameters() {
    val gradientValue =
      RemoteGradientValue(
        numberOfColors = 2,
        values = listOf(0.0f.rf, 0.0f.rf, 0.0f.rf, 0.0f.rf, 1.0f.rf, 1.0f.rf, 1.0f.rf, 1.0f.rf),
      )

    val gradientStroke =
      RemoteGradientStroke(
        gradient = gradientValue,
        startPoint = Point(0f.rf, 0f.rf),
        endPoint = Point(200f.rf, 0f.rf),
        gradientType = GradientType.Linear,
        opacity = 100f.rf,
        strokeWidth = 4f.rf,
        lineCap = LineCap.Square,
        lineJoin = LineJoin.Bevel,
      )

    val paint = gradientStroke.getPaint(inheritedOpacity = 1f.rf)

    assertThat(paint.style).isEqualTo(PaintingStyle.Stroke)
    assertThat(paint.strokeWidth.constantValue).isEqualTo(4f)
    assertThat(paint.strokeCap).isEqualTo(StrokeCap.Square)
    assertThat(paint.strokeJoin).isEqualTo(StrokeJoin.Bevel)
    assertThat(paint.shader).isInstanceOf(RemoteLinearShader::class.java)
  }

  @Test
  fun animateGradient_withKeyframes_interpolatesColorValues() {
    val animatedGradient =
      AnimatedGradientProperty(
        numberOfColors = 2,
        keyframes =
          listOf(
            GradientPropertyKeyframe(
              frame = 0f,
              value =
                listOf(
                  GradientValue(
                    numberOfColors = 2,
                    values =
                      listOf(
                        0.0f,
                        0.0f,
                        0.0f,
                        0.0f, // 0%: black
                        1.0f,
                        0.0f,
                        0.0f,
                        0.0f, // 100%: black
                      ),
                  )
                ),
            ),
            GradientPropertyKeyframe(
              frame = 10f,
              value =
                listOf(
                  GradientValue(
                    numberOfColors = 2,
                    values =
                      listOf(
                        0.0f,
                        1.0f,
                        1.0f,
                        1.0f, // 0%: white
                        1.0f,
                        1.0f,
                        1.0f,
                        1.0f, // 100%: white
                      ),
                  )
                ),
            ),
          ),
      )

    val frame0 = animateGradient(animatedGradient, LottieSettings(0.rf, emptySlotMap))
    val frame5 = animateGradient(animatedGradient, LottieSettings(5.rf, emptySlotMap))
    val frame10 = animateGradient(animatedGradient, LottieSettings(10.rf, emptySlotMap))

    assertThat(frame0.values[1].constantValue).isEqualTo(0f)
    assertThat(frame5.values[1].constantValue).isEqualTo(0.5f)
    assertThat(frame10.values[1].constantValue).isEqualTo(1f)
  }
}
