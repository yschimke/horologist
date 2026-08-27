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

import androidx.compose.remote.creation.compose.state.rf
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.horologist.remotecompose.lottie.format.properties.AnimatedPositionProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.AnimatedVectorProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.BaseVectorProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.KeyframeEasing
import com.google.android.horologist.remotecompose.lottie.format.properties.PositionPropertyKeyframe
import com.google.android.horologist.remotecompose.lottie.format.properties.ScalarKeyframeEasing
import com.google.android.horologist.remotecompose.lottie.format.properties.VectorPropertyKeyframe
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animatePosition
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animateVector
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KeyframeEasingTest {
  private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
  }
  private val emptySlotMap = SlotMap.Empty

  @Test
  fun keyframeEasing_parsesScalarFloatTangents() {
    val jsonStr =
      """
      {
        "a": 1,
        "k": [
          {
            "t": 0,
            "s": [0, 0],
            "i": { "x": 0.4, "y": 0.6 },
            "o": { "x": 0.2, "y": 0.8 }
          },
          {
            "t": 10,
            "s": [100, 100]
          }
        ]
      }
      """
        .trimIndent()

    val property = json.decodeFromString<BaseVectorProperty>(jsonStr)
    assertThat(property).isInstanceOf(AnimatedVectorProperty::class.java)
    val animated = property as AnimatedVectorProperty
    assertThat(animated.keyframes).hasSize(2)

    val kf0 = animated.keyframes[0]
    assertThat(kf0.inTangent).isNotNull()
    assertThat(kf0.inTangent?.x).isEqualTo(listOf(0.4f))
    assertThat(kf0.inTangent?.y).isEqualTo(listOf(0.6f))
    assertThat(kf0.inTangent?.getTangent(0)).isEqualTo(ScalarKeyframeEasing(0.4f, 0.6f))
    assertThat(kf0.inTangent?.getTangent(1)).isEqualTo(ScalarKeyframeEasing(0.4f, 0.6f))

    assertThat(kf0.outTangent).isNotNull()
    assertThat(kf0.outTangent?.x).isEqualTo(listOf(0.2f))
    assertThat(kf0.outTangent?.y).isEqualTo(listOf(0.8f))
    assertThat(kf0.outTangent?.getTangent(0)).isEqualTo(ScalarKeyframeEasing(0.2f, 0.8f))
    assertThat(kf0.outTangent?.getTangent(1)).isEqualTo(ScalarKeyframeEasing(0.2f, 0.8f))
  }

  @Test
  fun keyframeEasing_parsesMultiDimensionalArrayTangents() {
    val jsonStr =
      """
      {
        "a": 1,
        "k": [
          {
            "t": 0,
            "s": [0, 0, 0],
            "i": { "x": [0.1, 0.2, 0.3], "y": [0.4, 0.5, 0.6] },
            "o": { "x": [0.7, 0.8, 0.9], "y": [0.15, 0.25, 0.35] }
          },
          {
            "t": 20,
            "s": [100, 200, 300]
          }
        ]
      }
      """
        .trimIndent()

    val property = json.decodeFromString<BaseVectorProperty>(jsonStr) as AnimatedVectorProperty
    val kf0 = property.keyframes[0]

    assertThat(kf0.inTangent?.x).isEqualTo(listOf(0.1f, 0.2f, 0.3f))
    assertThat(kf0.inTangent?.y).isEqualTo(listOf(0.4f, 0.5f, 0.6f))
    assertThat(kf0.inTangent?.getTangent(0)).isEqualTo(ScalarKeyframeEasing(0.1f, 0.4f))
    assertThat(kf0.inTangent?.getTangent(1)).isEqualTo(ScalarKeyframeEasing(0.2f, 0.5f))
    assertThat(kf0.inTangent?.getTangent(2)).isEqualTo(ScalarKeyframeEasing(0.3f, 0.6f))
    // Out-of-bounds dimension fallback to first element:
    assertThat(kf0.inTangent?.getTangent(3)).isEqualTo(ScalarKeyframeEasing(0.1f, 0.4f))

    assertThat(kf0.outTangent?.x).isEqualTo(listOf(0.7f, 0.8f, 0.9f))
    assertThat(kf0.outTangent?.y).isEqualTo(listOf(0.15f, 0.25f, 0.35f))
    assertThat(kf0.outTangent?.getTangent(0)).isEqualTo(ScalarKeyframeEasing(0.7f, 0.15f))
    assertThat(kf0.outTangent?.getTangent(1)).isEqualTo(ScalarKeyframeEasing(0.8f, 0.25f))
    assertThat(kf0.outTangent?.getTangent(2)).isEqualTo(ScalarKeyframeEasing(0.9f, 0.35f))
  }

  @Test
  fun animateVector_withMultiDimensionalEasing_evaluatesEachDimensionIndependently() {
    // Dimension 0: linear easing (out = (0, 0), in = (1, 1)) -> at t=5/10, value is 50f
    // Dimension 1: symmetric S-curve easing
    val animatedVector =
      AnimatedVectorProperty(
        keyframes =
          listOf(
            VectorPropertyKeyframe(
              frame = 0f,
              value = listOf(0f, 0f),
              outTangent = KeyframeEasing(x = listOf(0f, 0.5f), y = listOf(0f, 0f)),
              inTangent = KeyframeEasing(x = listOf(1f, 0.5f), y = listOf(1f, 1f)),
            ),
            VectorPropertyKeyframe(frame = 10f, value = listOf(100f, 100f)),
          )
      )

    val frame0 = animateVector(animatedVector, LottieSettings(0.rf, emptySlotMap))
    val frame5 = animateVector(animatedVector, LottieSettings(5.rf, emptySlotMap))
    val frame10 = animateVector(animatedVector, LottieSettings(10.rf, emptySlotMap))

    assertThat(frame0.map { it.constantValue }).isEqualTo(listOf(0f, 0f))
    assertThat(frame10.map { it.constantValue }).isEqualTo(listOf(100f, 100f))

    // For dimension 0: linear easing -> 50f at mid frame
    val midValues = frame5.map { it.constantValue }
    assertThat(midValues[0]).isEqualTo(50f)
    // For dimension 1: easing (0.5, 0) to (0.5, 1) -> symmetric S-curve, mid frame is 50f
    assertThat(midValues[1]).isWithin(1f).of(50f)
  }

  @Test
  fun animatePosition_withMultiDimensionalEasing_interpolatesXYWithSeparateCurves() {
    // Linear X, ease-in Y
    val animatedPosition =
      AnimatedPositionProperty(
        keyframes =
          listOf(
            PositionPropertyKeyframe(
              frame = 0f,
              value = listOf(0f, 0f),
              outTangent = KeyframeEasing(x = listOf(0f, 1f), y = listOf(0f, 0f)),
              inTangent = KeyframeEasing(x = listOf(1f, 1f), y = listOf(1f, 0f)),
            ),
            PositionPropertyKeyframe(frame = 10f, value = listOf(100f, 100f)),
          )
      )

    val frame5 = animatePosition(animatedPosition, LottieSettings(5.rf, emptySlotMap))
    assertThat(frame5.x.constantValue).isEqualTo(50f)
    // Ease-in with tangent (1, 0) stays flatter at t=0.5:
    assertThat(frame5.y.constantValue).isLessThan(50f)
  }

  @Test
  fun keyframeEasing_serializationRoundtrip() {
    val original = KeyframeEasing(x = listOf(0.1f, 0.2f, 0.3f), y = listOf(0.4f, 0.5f, 0.6f))
    val encoded = json.encodeToString(KeyframeEasing.serializer(), original)
    val decoded = json.decodeFromString(KeyframeEasing.serializer(), encoded)
    assertThat(decoded).isEqualTo(original)
  }
}
