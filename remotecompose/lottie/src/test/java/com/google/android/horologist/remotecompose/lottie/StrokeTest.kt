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

import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.GradientType
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.LineCap
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.LineJoin
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.StrokeDash
import com.google.android.horologist.remotecompose.lottie.format.properties.AnimatedScalarProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.ScalarPropertyKeyframe
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticScalarProperty
import com.google.android.horologist.remotecompose.lottie.renderer.RemoteGradientStroke
import com.google.android.horologist.remotecompose.lottie.renderer.RemoteStroke
import com.google.android.horologist.remotecompose.lottie.renderer.createDashPathEffect
import com.google.android.horologist.remotecompose.lottie.renderer.properties.Point
import com.google.android.horologist.remotecompose.lottie.renderer.properties.RemoteGradientValue
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StrokeTest {

  private val settings = LottieSettings(currentFrame = 0f.rf, slotMap = SlotMap.Empty)

  @Test
  fun remoteStroke_appliesLineCapAndJoinCorrectly() {
    val buttMiter =
      RemoteStroke(
        strokeColor = Color.Red.rc,
        strokeWidth = 5f.rf,
        opacity = 100f.rf,
        lineCap = LineCap.Butt,
        lineJoin = LineJoin.Miter,
        miterLimit = 4f.rf,
      )
    val paint1 = buttMiter.getPaint()
    assertThat(paint1.style).isEqualTo(PaintingStyle.Stroke)
    assertThat(paint1.strokeCap).isEqualTo(StrokeCap.Butt)
    assertThat(paint1.strokeJoin).isEqualTo(StrokeJoin.Miter)
    assertThat(buttMiter.miterLimit?.constantValueOrNull).isEqualTo(4f)

    val squareBevel =
      RemoteStroke(
        strokeColor = Color.Blue.rc,
        strokeWidth = 2f.rf,
        opacity = 80f.rf,
        lineCap = LineCap.Square,
        lineJoin = LineJoin.Bevel,
      )
    val paint2 = squareBevel.getPaint()
    assertThat(paint2.strokeCap).isEqualTo(StrokeCap.Square)
    assertThat(paint2.strokeJoin).isEqualTo(StrokeJoin.Bevel)

    val roundRound =
      RemoteStroke(
        strokeColor = Color.Green.rc,
        strokeWidth = 3f.rf,
        opacity = 100f.rf,
        lineCap = LineCap.Round,
        lineJoin = LineJoin.Round,
      )
    val paint3 = roundRound.getPaint()
    assertThat(paint3.strokeCap).isEqualTo(StrokeCap.Round)
    assertThat(paint3.strokeJoin).isEqualTo(StrokeJoin.Round)
  }

  @Test
  fun remoteStroke_withDashPattern_setsPathEffectOnPaint() {
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f), 2f)
    val stroke =
      RemoteStroke(
        strokeColor = Color.Black.rc,
        strokeWidth = 2f.rf,
        opacity = 100f.rf,
        dashPattern = dashEffect,
      )

    val paint = stroke.getPaint()
    assertThat(paint.pathEffect).isEqualTo(dashEffect)
  }

  @Test
  fun remoteStroke_withoutDashPattern_hasNullPathEffect() {
    val stroke =
      RemoteStroke(
        strokeColor = Color.Black.rc,
        strokeWidth = 2f.rf,
        opacity = 100f.rf,
        dashPattern = null,
      )

    val paint = stroke.getPaint()
    assertThat(paint.pathEffect).isNull()
  }

  @Test
  fun remoteGradientStroke_withDashPattern_setsPathEffectAndShader() {
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f), 0f)
    val gradientValue =
      RemoteGradientValue(
        numberOfColors = 2,
        values = listOf(0f.rf, 1f.rf, 0f.rf, 0f.rf, 1f.rf, 0f.rf, 0f.rf, 1f.rf),
      )
    val gradientStroke =
      RemoteGradientStroke(
        gradient = gradientValue,
        startPoint = Point(0f.rf, 0f.rf),
        endPoint = Point(100f.rf, 0f.rf),
        gradientType = GradientType.Linear,
        opacity = 100f.rf,
        strokeWidth = 4f.rf,
        lineCap = LineCap.Round,
        lineJoin = LineJoin.Round,
        miterLimit = 5f.rf,
        dashPattern = dashEffect,
      )

    val paint = gradientStroke.getPaint()
    assertThat(paint.style).isEqualTo(PaintingStyle.Stroke)
    assertThat(paint.pathEffect).isEqualTo(dashEffect)
    assertThat(paint.shader).isNotNull()
    assertThat(gradientStroke.miterLimit?.constantValueOrNull).isEqualTo(5f)
  }

  @Test
  fun createDashPathEffect_withDashesGapsAndOffset_returnsNonNullPathEffect() {
    val dashes =
      listOf(
        StrokeDash(dashType = "d", name = "dash", value = StaticScalarProperty(value = 12f)),
        StrokeDash(dashType = "g", name = "gap", value = StaticScalarProperty(value = 6f)),
        StrokeDash(dashType = "o", name = "offset", value = StaticScalarProperty(value = 3f)),
      )

    val effect = createDashPathEffect(dashes, settings)
    assertThat(effect).isNotNull()
  }

  @Test
  fun createDashPathEffect_withSingleDash_duplicatesIntervalsToEvenLength() {
    val dashes =
      listOf(StrokeDash(dashType = "d", name = "dash", value = StaticScalarProperty(value = 15f)))

    val effect = createDashPathEffect(dashes, settings)
    assertThat(effect).isNotNull()
  }

  @Test
  fun createDashPathEffect_withNullOrEmptyOrAllZeros_returnsNull() {
    assertThat(createDashPathEffect(null, settings)).isNull()
    assertThat(createDashPathEffect(emptyList(), settings)).isNull()

    val allZeros =
      listOf(
        StrokeDash(dashType = "d", value = StaticScalarProperty(value = 0f)),
        StrokeDash(dashType = "g", value = StaticScalarProperty(value = 0f)),
      )
    assertThat(createDashPathEffect(allZeros, settings)).isNull()
  }

  @Test
  fun createDashPathEffect_withAnimatedDash_evaluatesAtCurrentFrame() {
    val keyframes =
      listOf(
        ScalarPropertyKeyframe(frame = 0f, value = 10f),
        ScalarPropertyKeyframe(frame = 30f, value = 20f),
      )
    val animatedDash =
      listOf(
        StrokeDash(dashType = "d", value = AnimatedScalarProperty(keyframes = keyframes)),
        StrokeDash(dashType = "g", value = StaticScalarProperty(value = 5f)),
      )

    val frame0Settings = LottieSettings(currentFrame = 0f.rf)
    val effectFrame0 = createDashPathEffect(animatedDash, frame0Settings)
    assertThat(effectFrame0).isNotNull()

    val frame15Settings = LottieSettings(currentFrame = 15f.rf)
    val effectFrame15 = createDashPathEffect(animatedDash, frame15Settings)
    assertThat(effectFrame15).isNotNull()
  }
}
