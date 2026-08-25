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
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.GradientType
import com.google.android.horologist.remotecompose.lottie.format.properties.AnimatedScalarProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.ScalarPropertyKeyframe
import com.google.android.horologist.remotecompose.lottie.renderer.RemoteGradientFill
import com.google.android.horologist.remotecompose.lottie.renderer.properties.Point
import com.google.android.horologist.remotecompose.lottie.renderer.properties.RemoteGradientValue
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animateScalar
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SuppressLint("RestrictedApi")
@RunWith(AndroidJUnit4::class)
class StyleAndGradientSafetyTest {

  @Test
  fun remoteGradientFill_withMalformedShortValues_doesNotThrow() {
    val gradient = RemoteGradientValue(numberOfColors = 4, values = listOf(0f.rf, 1f.rf))
    val fill =
      RemoteGradientFill(
        gradient = gradient,
        startPoint = Point(0f.rf, 0f.rf),
        endPoint = Point(100f.rf, 100f.rf),
        gradientType = GradientType.Linear,
        opacity = 100f.rf,
      )

    val paint = fill.getPaint()
    assertThat(paint.shader).isNotNull()
  }

  @Test
  fun remoteGradientFill_withExcessiveNumberOfColors_clampsToAvailableFloats() {
    val gradient =
      RemoteGradientValue(
        numberOfColors = 10,
        values = listOf(0f.rf, 1f.rf, 0f.rf, 0f.rf, 1f.rf, 0f.rf, 1f.rf, 0f.rf),
      )
    val fill =
      RemoteGradientFill(
        gradient = gradient,
        startPoint = Point(0f.rf, 0f.rf),
        endPoint = Point(100f.rf, 100f.rf),
        gradientType = GradientType.Linear,
        opacity = 100f.rf,
      )

    val paint = fill.getPaint()
    assertThat(paint.shader).isNotNull()
  }

  @Test
  fun animateScalar_withZeroDurationKeyframes_doesNotCrash() {
    val kf1 = ScalarPropertyKeyframe(value = 10f, frame = 0f)
    val kf2 = ScalarPropertyKeyframe(value = 20f, frame = 0f)
    val property = AnimatedScalarProperty(keyframes = listOf(kf1, kf2))
    val settings = LottieSettings(currentFrame = 0f.rf)

    val result = animateScalar(property, settings)
    assertThat(result).isNotNull()
  }
}
