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
import com.google.android.horologist.remotecompose.lottie.format.properties.AnimatedScalarProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.PositionPropertyKeyframe
import com.google.android.horologist.remotecompose.lottie.format.properties.ScalarPropertyKeyframe
import com.google.android.horologist.remotecompose.lottie.format.properties.SplitPositionProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticPositionProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticScalarProperty
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animatePosition
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PositionTest {
  private val emptySlotMap = SlotMap.Empty

  @Test
  fun animatePosition_withStaticInput_returnsExactCoordinates() {
    val staticPosition = StaticPositionProperty(value = listOf(10f, 20f))

    val result1 = animatePosition(staticPosition, LottieSettings(0.rf, emptySlotMap))
    assertThat(result1.x.constantValue).isEqualTo(10f)
    assertThat(result1.y.constantValue).isEqualTo(20f)

    val result2 = animatePosition(staticPosition, LottieSettings(5.rf, emptySlotMap))
    assertThat(result2.x.constantValue).isEqualTo(10f)
    assertThat(result2.y.constantValue).isEqualTo(20f)
  }

  @Test
  fun animatePosition_withSplitPosition_evaluatesXYIndependently() {
    val splitPosition =
      SplitPositionProperty(
        x =
          AnimatedScalarProperty(
            keyframes =
              listOf(
                ScalarPropertyKeyframe(frame = 0f, value = 10f),
                ScalarPropertyKeyframe(frame = 10f, value = 30f),
              )
          ),
        y = StaticScalarProperty(value = 50f),
      )

    val frame0 = animatePosition(splitPosition, LottieSettings(0.rf, emptySlotMap))
    val frame5 = animatePosition(splitPosition, LottieSettings(5.rf, emptySlotMap))
    val frame10 = animatePosition(splitPosition, LottieSettings(10.rf, emptySlotMap))

    assertThat(frame0.x.constantValue).isEqualTo(10f)
    assertThat(frame0.y.constantValue).isEqualTo(50f)
    assertThat(frame5.x.constantValue).isEqualTo(20f)
    assertThat(frame5.y.constantValue).isEqualTo(50f)
    assertThat(frame10.x.constantValue).isEqualTo(30f)
    assertThat(frame10.y.constantValue).isEqualTo(50f)
  }

  @Test
  fun animatePosition_withLinearKeyframes_interpolatesLinearly() {
    val animatedPosition =
      AnimatedPositionProperty(
        keyframes =
          listOf(
            PositionPropertyKeyframe(frame = 0f, value = listOf(0f, 0f)),
            PositionPropertyKeyframe(frame = 10f, value = listOf(100f, 200f)),
          )
      )

    val frame0 = animatePosition(animatedPosition, LottieSettings(0.rf, emptySlotMap))
    val frame5 = animatePosition(animatedPosition, LottieSettings(5.rf, emptySlotMap))
    val frame10 = animatePosition(animatedPosition, LottieSettings(10.rf, emptySlotMap))

    assertThat(frame0.x.constantValue).isEqualTo(0f)
    assertThat(frame0.y.constantValue).isEqualTo(0f)
    assertThat(frame5.x.constantValue).isEqualTo(50f)
    assertThat(frame5.y.constantValue).isEqualTo(100f)
    assertThat(frame10.x.constantValue).isEqualTo(100f)
    assertThat(frame10.y.constantValue).isEqualTo(200f)
  }

  @Test
  fun animatePosition_withSpatialBezierTangents_evaluatesCurvedTrajectory() {
    // A straight line between (0, 0) and (100, 0) with spatial tangents to=(0, 50), ti=(0, 50)
    // At s = 0.5:
    // X = 0.5 * 0 + 0.5 * 100 + 0.375 * (0 + 0) = 50f
    // Y = 0.5 * 0 + 0.5 * 0 + 0.375 * (50 + 50) = 37.5f
    val curvedPosition =
      AnimatedPositionProperty(
        keyframes =
          listOf(
            PositionPropertyKeyframe(
              frame = 0f,
              value = listOf(0f, 0f),
              spatialOutTangent = listOf(0f, 50f),
              spatialInTangent = listOf(0f, 50f),
            ),
            PositionPropertyKeyframe(frame = 10f, value = listOf(100f, 0f)),
          )
      )

    val frame0 = animatePosition(curvedPosition, LottieSettings(0.rf, emptySlotMap))
    val frame5 = animatePosition(curvedPosition, LottieSettings(5.rf, emptySlotMap))
    val frame10 = animatePosition(curvedPosition, LottieSettings(10.rf, emptySlotMap))

    assertThat(frame0.x.constantValue).isEqualTo(0f)
    assertThat(frame0.y.constantValue).isEqualTo(0f)
    assertThat(frame5.x.constantValue).isEqualTo(50f)
    assertThat(frame5.y.constantValue).isEqualTo(37.5f)
    assertThat(frame10.x.constantValue).isEqualTo(100f)
    assertThat(frame10.y.constantValue).isEqualTo(0f)
  }

  @Test
  fun animatePosition_withSpatialTangentsAndHold_holdsInitialValue() {
    val holdCurved =
      AnimatedPositionProperty(
        keyframes =
          listOf(
            PositionPropertyKeyframe(
              frame = 0f,
              hold = true,
              value = listOf(10f, 20f),
              spatialOutTangent = listOf(0f, 50f),
            ),
            PositionPropertyKeyframe(frame = 10f, value = listOf(100f, 200f)),
          )
      )

    val frame0 = animatePosition(holdCurved, LottieSettings(0.rf, emptySlotMap))
    val frame5 = animatePosition(holdCurved, LottieSettings(5.rf, emptySlotMap))
    val frame10 = animatePosition(holdCurved, LottieSettings(10.rf, emptySlotMap))

    assertThat(frame0.x.constantValue).isEqualTo(10f)
    assertThat(frame0.y.constantValue).isEqualTo(20f)
    assertThat(frame5.x.constantValue).isEqualTo(10f)
    assertThat(frame5.y.constantValue).isEqualTo(20f)
    assertThat(frame10.x.constantValue).isEqualTo(100f)
    assertThat(frame10.y.constantValue).isEqualTo(200f)
  }
}
