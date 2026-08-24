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

import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.ui.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.horologist.remotecompose.lottie.format.properties.AnimatedVectorProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticVectorProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.VectorPropertyKeyframe
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animateVector
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AnimationTest {
  private val emptySlotMap = SlotMap.Empty

  @Test
  fun animateVectorWithStaticInput_returnsInput() {
    val staticVector =
      StaticVectorProperty(animated = false.rb, value = listOf(1f.rf, 2f.rf, 3f.rf))

    val result1 = animateVector(staticVector, LottieSettings(0.rf, emptySlotMap))
    assertThat(result1.map { it.constantValue })
      .isEqualTo(staticVector.value.map { it.constantValue })

    val result2 = animateVector(staticVector, LottieSettings(5.rf, emptySlotMap))
    assertThat(result2.map { it.constantValue })
      .isEqualTo(staticVector.value.map { it.constantValue })
  }

  @Test
  fun animateVectorWithEmptyKeyframes_returnsEmptyList() {
    val animatedVector = AnimatedVectorProperty(keyframes = emptyList())

    val result = animateVector(animatedVector, LottieSettings(0.rf, emptySlotMap))
    assertThat(result).isEmpty()
  }

  @Test
  fun animateVectorWithSingleKeyframe_returnsInput() {
    val animatedVector =
      AnimatedVectorProperty(
        animated = true.rb,
        keyframes =
          listOf(
            VectorPropertyKeyframe(
              frame = 0f.rf,
              value = listOf(1f.rf, 2f.rf, 3f.rf),
              hold = false.rb,
            )
          ),
      )

    val result1 = animateVector(animatedVector, LottieSettings(0.rf, emptySlotMap))
    assertThat(result1.map { it.constantValue })
      .isEqualTo(animatedVector.keyframes[0].value.map { it.constantValue })

    val result2 = animateVector(animatedVector, LottieSettings(5.rf, emptySlotMap))
    assertThat(result2.map { it.constantValue })
      .isEqualTo(animatedVector.keyframes[0].value.map { it.constantValue })
  }

  @Test
  fun animateVectorWithTwoKeyframes_returnsAnimatedValues() {
    val animatedVector =
      AnimatedVectorProperty(
        animated = true.rb,
        keyframes =
          listOf(
            VectorPropertyKeyframe(
              frame = 0f.rf,
              value = listOf(1f.rf, 2f.rf, 3f.rf),
              hold = false.rb,
            ),
            VectorPropertyKeyframe(
              frame = 10f.rf,
              value = listOf(4f.rf, 5f.rf, 6f.rf),
              hold = false.rb,
            ),
          ),
      )

    val firstFrameResult = animateVector(animatedVector, LottieSettings(0.rf, emptySlotMap))
    val middleFrameResult = animateVector(animatedVector, LottieSettings(5.rf, emptySlotMap))
    val lastFrameResult = animateVector(animatedVector, LottieSettings(10.rf, emptySlotMap))
    val afterAnimationResult = animateVector(animatedVector, LottieSettings(15.rf, emptySlotMap))

    assertThat(firstFrameResult.map { it.constantValue }.toFloatArray())
      .isEqualTo(animatedVector.keyframes[0].value.map { it.constantValue }.toFloatArray())
    assertThat(middleFrameResult.map { it.constantValue }.toFloatArray())
      .isEqualTo(floatArrayOf(2.5f, 3.5f, 4.5f))
    assertThat(lastFrameResult.map { it.constantValue }.toFloatArray())
      .isEqualTo(animatedVector.keyframes[1].value.map { it.constantValue }.toFloatArray())
    assertThat(afterAnimationResult.map { it.constantValue }.toFloatArray())
      .isEqualTo(animatedVector.keyframes[1].value.map { it.constantValue }.toFloatArray())
  }

  @Test
  fun animateVectorWithHoldKeyframe_holdsValue() {
    val animatedVector =
      AnimatedVectorProperty(
        keyframes =
          listOf(
            VectorPropertyKeyframe(frame = 0f, hold = true, value = listOf(10f, 20f)),
            VectorPropertyKeyframe(frame = 10f, value = listOf(30f, 40f)),
          )
      )

    val firstFrameResult = animateVector(animatedVector, LottieSettings(0.rf, emptySlotMap))
    val middleFrameResult = animateVector(animatedVector, LottieSettings(5.rf, emptySlotMap))
    val lastFrameResult = animateVector(animatedVector, LottieSettings(10.rf, emptySlotMap))
    val afterAnimationResult = animateVector(animatedVector, LottieSettings(15.rf, emptySlotMap))

    assertThat(firstFrameResult.map { it.constantValue }).isEqualTo(listOf(10f, 20f))
    assertThat(middleFrameResult.map { it.constantValue }).isEqualTo(listOf(10f, 20f))
    assertThat(lastFrameResult.map { it.constantValue }).isEqualTo(listOf(30f, 40f))
    assertThat(afterAnimationResult.map { it.constantValue }).isEqualTo(listOf(30f, 40f))
  }

  @Test
  fun animateVectorWithDelayedStart_holdsInitialValue() {
    val animatedVector =
      AnimatedVectorProperty(
        keyframes =
          listOf(
            VectorPropertyKeyframe(frame = 5f, value = floatArrayOf(10f, 20f)),
            VectorPropertyKeyframe(frame = 10f, value = floatArrayOf(30f, 40f)),
          )
      )

    val beforeStartResult = animateVector(animatedVector, LottieSettings(0.rf, emptySlotMap))
    assertThat(beforeStartResult.map { it.constantValue }.toFloatArray())
      .isEqualTo(floatArrayOf(10f, 20f))
  }

  @Test
  fun animateColorWithStaticInput_returnsInput() {
    val staticColor = StaticColorProperty(value = Color.Red.rc)

    val result1 = animateColor(staticColor, LottieSettings(0.rf, emptySlotMap))
    assertThat(result1.constantValueOrNull).isEqualTo(Color.Red)

    val result2 = animateColor(staticColor, LottieSettings(5.rf, emptySlotMap))
    assertThat(result2.constantValueOrNull).isEqualTo(Color.Red)
  }

  @Test
  fun animateColorWithSlot_returnsSlotColor() {
    val slotMap = SlotMap(mapOf("color_slot" to Color.Blue.rc))
    val staticColor = StaticColorProperty(slotId = "color_slot", value = Color.Red.rc)

    val result = animateColor(staticColor, LottieSettings(0.rf, slotMap))
    assertThat(result.constantValueOrNull).isEqualTo(Color.Blue)

    val animatedColor =
      AnimatedColorProperty(
        slotId = "color_slot",
        keyframes = listOf(ColorPropertyKeyframe(frame = 0f, value = Color.Red.rc)),
      )
    val animResult = animateColor(animatedColor, LottieSettings(0.rf, slotMap))
    assertThat(animResult.constantValueOrNull).isEqualTo(Color.Blue)
  }

  @Test
  fun animateColorWithEmptyKeyframes_returnsTransparent() {
    val animatedColor = AnimatedColorProperty(keyframes = emptyList())

    val result = animateColor(animatedColor, LottieSettings(0.rf, emptySlotMap))
    assertThat(result.constantValueOrNull).isEqualTo(Color.Transparent)
  }

  @Test
  fun animateColorWithSingleKeyframe_returnsInput() {
    val animatedColor =
      AnimatedColorProperty(
        keyframes = listOf(ColorPropertyKeyframe(frame = 0f, value = Color.Green.rc))
      )

    val result1 = animateColor(animatedColor, LottieSettings(0.rf, emptySlotMap))
    assertThat(result1.constantValueOrNull).isEqualTo(Color.Green)

    val result2 = animateColor(animatedColor, LottieSettings(5.rf, emptySlotMap))
    assertThat(result2.constantValueOrNull).isEqualTo(Color.Green)
  }

  @Test
  fun animateColorWithTwoKeyframes_returnsAnimatedValues() {
    val animatedColor =
      AnimatedColorProperty(
        keyframes =
          listOf(
            ColorPropertyKeyframe(frame = 0f, value = Color(1f, 0f, 0f, 1f).rc),
            ColorPropertyKeyframe(frame = 10f, value = Color(0f, 1f, 0f, 1f).rc),
          )
      )

    val firstFrameResult = animateColor(animatedColor, LottieSettings(0.rf, emptySlotMap))
    val lastFrameResult = animateColor(animatedColor, LottieSettings(10.rf, emptySlotMap))
    val afterAnimationResult = animateColor(animatedColor, LottieSettings(15.rf, emptySlotMap))

    assertThat(firstFrameResult.constantValueOrNull).isEqualTo(Color(1f, 0f, 0f, 1f))
    assertThat(lastFrameResult.constantValueOrNull).isEqualTo(Color(0f, 1f, 0f, 1f))
    assertThat(afterAnimationResult.constantValueOrNull).isEqualTo(Color(0f, 1f, 0f, 1f))
  }

  @Test
  fun animateColorWithHoldKeyframe_holdsValue() {
    val animatedColor =
      AnimatedColorProperty(
        keyframes =
          listOf(
            ColorPropertyKeyframe(frame = 0f, hold = true, value = Color.Red.rc),
            ColorPropertyKeyframe(frame = 10f, value = Color.Blue.rc),
          )
      )

    val firstFrameResult = animateColor(animatedColor, LottieSettings(0.rf, emptySlotMap))
    val middleFrameResult = animateColor(animatedColor, LottieSettings(5.rf, emptySlotMap))
    val lastFrameResult = animateColor(animatedColor, LottieSettings(10.rf, emptySlotMap))

    assertThat(firstFrameResult.constantValueOrNull).isEqualTo(Color.Red)
    assertThat(middleFrameResult.constantValueOrNull).isEqualTo(Color.Red)
    assertThat(lastFrameResult.constantValueOrNull).isEqualTo(Color.Blue)
  }

  @Test
  fun animateColorWithDelayedStart_holdsInitialValue() {
    val animatedColor =
      AnimatedColorProperty(
        keyframes =
          listOf(
            ColorPropertyKeyframe(frame = 5f, value = Color.Yellow.rc),
            ColorPropertyKeyframe(frame = 10f, value = Color.Cyan.rc),
          )
      )

    val beforeStartResult = animateColor(animatedColor, LottieSettings(0.rf, emptySlotMap))
    assertThat(beforeStartResult.constantValueOrNull).isEqualTo(Color.Yellow)
  }

  @Test
  fun animateBezierWithStaticInput_returnsInput() {
    val bezierValue =
      BezierValue(
        closed = true,
        inTangents = listOf(listOf(0f, 0f), listOf(1f, 1f)),
        outTangents = listOf(listOf(2f, 2f), listOf(3f, 3f)),
        vertices = listOf(listOf(10f, 10f), listOf(20f, 20f)),
      )
    val staticBezier = StaticBezierProperty(value = bezierValue)

    val result = animateBezier(staticBezier, LottieSettings(0.rf, emptySlotMap))
    assertThat(result).hasSize(1)
    assertThat(result[0].closed).isTrue()
    assertThat(result[0].vertices.map { point -> point.map { it.constantValueOrNull } })
      .isEqualTo(listOf(listOf(10f, 10f), listOf(20f, 20f)))
    assertThat(result[0].inTangents.map { point -> point.map { it.constantValueOrNull } })
      .isEqualTo(listOf(listOf(0f, 0f), listOf(1f, 1f)))
    assertThat(result[0].outTangents.map { point -> point.map { it.constantValueOrNull } })
      .isEqualTo(listOf(listOf(2f, 2f), listOf(3f, 3f)))
  }

  @Test
  fun animateBezierWithEmptyKeyframes_returnsEmptyList() {
    val animatedBezier = AnimatedBezierProperty(keyframes = emptyList())

    val result = animateBezier(animatedBezier, LottieSettings(0.rf, emptySlotMap))
    assertThat(result).isEmpty()
  }

  @Test
  fun animateBezierWithSingleKeyframe_returnsInput() {
    val bezierValue =
      BezierValue(
        closed = false,
        inTangents = listOf(listOf(0f, 0f)),
        outTangents = listOf(listOf(1f, 1f)),
        vertices = listOf(listOf(5f, 5f)),
      )
    val animatedBezier =
      AnimatedBezierProperty(
        keyframes = listOf(BezierPropertyKeyframe(frame = 0f, value = listOf(bezierValue)))
      )

    val result = animateBezier(animatedBezier, LottieSettings(0.rf, emptySlotMap))
    assertThat(result).hasSize(1)
    assertThat(result[0].closed).isFalse()
    assertThat(result[0].vertices.map { point -> point.map { it.constantValueOrNull } })
      .isEqualTo(listOf(listOf(5f, 5f)))
  }

  @Test
  fun animateBezierWithTwoKeyframes_returnsAnimatedValues() {
    val keyframe1 =
      BezierPropertyKeyframe(
        frame = 0f,
        value =
          listOf(
            BezierValue(
              closed = true,
              inTangents = listOf(listOf(0f, 0f), listOf(2f, 2f)),
              outTangents = listOf(listOf(1f, 1f), listOf(3f, 3f)),
              vertices = listOf(listOf(0f, 0f), listOf(10f, 10f)),
            )
          ),
      )
    val keyframe2 =
      BezierPropertyKeyframe(
        frame = 10f,
        value =
          listOf(
            BezierValue(
              closed = true,
              inTangents = listOf(listOf(10f, 10f), listOf(12f, 12f)),
              outTangents = listOf(listOf(11f, 11f), listOf(13f, 13f)),
              vertices = listOf(listOf(10f, 20f), listOf(30f, 40f)),
            )
          ),
      )
    val animatedBezier = AnimatedBezierProperty(keyframes = listOf(keyframe1, keyframe2))

    val firstFrameResult = animateBezier(animatedBezier, LottieSettings(0.rf, emptySlotMap))
    val middleFrameResult = animateBezier(animatedBezier, LottieSettings(5.rf, emptySlotMap))
    val lastFrameResult = animateBezier(animatedBezier, LottieSettings(10.rf, emptySlotMap))
    val afterAnimationResult = animateBezier(animatedBezier, LottieSettings(15.rf, emptySlotMap))

    assertThat(firstFrameResult[0].vertices.map { point -> point.map { it.constantValue } })
      .isEqualTo(listOf(listOf(0f, 0f), listOf(10f, 10f)))
    assertThat(middleFrameResult[0].vertices.map { point -> point.map { it.constantValue } })
      .isEqualTo(listOf(listOf(5f, 10f), listOf(20f, 25f)))
    assertThat(lastFrameResult[0].vertices.map { point -> point.map { it.constantValue } })
      .isEqualTo(listOf(listOf(10f, 20f), listOf(30f, 40f)))
    assertThat(afterAnimationResult[0].vertices.map { point -> point.map { it.constantValue } })
      .isEqualTo(listOf(listOf(10f, 20f), listOf(30f, 40f)))

    assertThat(middleFrameResult[0].inTangents.map { point -> point.map { it.constantValue } })
      .isEqualTo(listOf(listOf(5f, 5f), listOf(7f, 7f)))
    assertThat(middleFrameResult[0].outTangents.map { point -> point.map { it.constantValue } })
      .isEqualTo(listOf(listOf(6f, 6f), listOf(8f, 8f)))
  }

  @Test
  fun animateBezierWithHoldKeyframe_holdsValue() {
    val keyframe1 =
      BezierPropertyKeyframe(
        frame = 0f,
        hold = true,
        value = listOf(BezierValue(closed = false, vertices = listOf(listOf(0f, 0f)))),
      )
    val keyframe2 =
      BezierPropertyKeyframe(
        frame = 10f,
        value = listOf(BezierValue(closed = false, vertices = listOf(listOf(100f, 100f)))),
      )
    val animatedBezier = AnimatedBezierProperty(keyframes = listOf(keyframe1, keyframe2))

    val firstFrameResult = animateBezier(animatedBezier, LottieSettings(0.rf, emptySlotMap))
    val middleFrameResult = animateBezier(animatedBezier, LottieSettings(5.rf, emptySlotMap))
    val lastFrameResult = animateBezier(animatedBezier, LottieSettings(10.rf, emptySlotMap))
    val afterAnimationResult = animateBezier(animatedBezier, LottieSettings(15.rf, emptySlotMap))

    assertThat(firstFrameResult[0].vertices.map { point -> point.map { it.constantValue } })
      .isEqualTo(listOf(listOf(0f, 0f)))
    assertThat(middleFrameResult[0].vertices.map { point -> point.map { it.constantValue } })
      .isEqualTo(listOf(listOf(0f, 0f)))
    assertThat(lastFrameResult[0].vertices.map { point -> point.map { it.constantValue } })
      .isEqualTo(listOf(listOf(100f, 100f)))
    assertThat(afterAnimationResult[0].vertices.map { point -> point.map { it.constantValue } })
      .isEqualTo(listOf(listOf(100f, 100f)))
  }

  @Test
  fun animateBezierWithDelayedStart_holdsInitialValue() {
    val keyframe1 =
      BezierPropertyKeyframe(
        frame = 5f,
        value = listOf(BezierValue(closed = true, vertices = listOf(listOf(10f, 10f)))),
      )
    val keyframe2 =
      BezierPropertyKeyframe(
        frame = 10f,
        value = listOf(BezierValue(closed = true, vertices = listOf(listOf(20f, 20f)))),
      )
    val animatedBezier = AnimatedBezierProperty(keyframes = listOf(keyframe1, keyframe2))

    val beforeStartResult = animateBezier(animatedBezier, LottieSettings(0.rf, emptySlotMap))
    val startFrameResult = animateBezier(animatedBezier, LottieSettings(5.rf, emptySlotMap))
    val endFrameResult = animateBezier(animatedBezier, LottieSettings(10.rf, emptySlotMap))

    assertThat(beforeStartResult[0].vertices.map { point -> point.map { it.constantValue } })
      .isEqualTo(listOf(listOf(10f, 10f)))
    assertThat(startFrameResult[0].vertices.map { point -> point.map { it.constantValue } })
      .isEqualTo(listOf(listOf(10f, 10f)))
    assertThat(endFrameResult[0].vertices.map { point -> point.map { it.constantValue } })
      .isEqualTo(listOf(listOf(20f, 20f)))
  }

  @Test
  fun animateBezierWithMultipleSubpaths_interpolatesEachSubpath() {
    val keyframe1 =
      BezierPropertyKeyframe(
        frame = 0f,
        value =
          listOf(
            BezierValue(closed = true, vertices = listOf(listOf(0f, 0f))),
            BezierValue(closed = false, vertices = listOf(listOf(100f, 100f))),
          ),
      )
    val keyframe2 =
      BezierPropertyKeyframe(
        frame = 10f,
        value =
          listOf(
            BezierValue(closed = true, vertices = listOf(listOf(10f, 10f))),
            BezierValue(closed = false, vertices = listOf(listOf(200f, 200f))),
          ),
      )
    val animatedBezier = AnimatedBezierProperty(keyframes = listOf(keyframe1, keyframe2))

    val middleFrameResult = animateBezier(animatedBezier, LottieSettings(5.rf, emptySlotMap))

    assertThat(middleFrameResult).hasSize(2)
    assertThat(middleFrameResult[0].closed).isTrue()
    assertThat(middleFrameResult[0].vertices.map { point -> point.map { it.constantValue } })
      .isEqualTo(listOf(listOf(5f, 5f)))
    assertThat(middleFrameResult[1].closed).isFalse()
    assertThat(middleFrameResult[1].vertices.map { point -> point.map { it.constantValue } })
      .isEqualTo(listOf(listOf(150f, 150f)))
  }

  @Test
  fun animatePositionWithStaticInput_returnsInput() {
    val staticPosition = StaticPositionProperty(value = floatArrayOf(10f, 20f))

    val result1 = animatePosition(staticPosition, LottieSettings(0.rf, emptySlotMap))
    assertThat(result1.x.constantValue).isEqualTo(10f)
    assertThat(result1.y.constantValue).isEqualTo(20f)

    val result2 = animatePosition(staticPosition, LottieSettings(5.rf, emptySlotMap))
    assertThat(result2.x.constantValue).isEqualTo(10f)
    assertThat(result2.y.constantValue).isEqualTo(20f)
  }

  @Test
  fun animatePositionWithEmptyKeyframes_returnsZeroPoint() {
    val animatedPosition = AnimatedPositionProperty(keyframes = emptyList())

    val result = animatePosition(animatedPosition, LottieSettings(0.rf, emptySlotMap))
    assertThat(result.x.constantValue).isEqualTo(0f)
    assertThat(result.y.constantValue).isEqualTo(0f)
  }

  @Test
  fun animatePositionWithSingleKeyframe_returnsInput() {
    val animatedPosition =
      AnimatedPositionProperty(
        keyframes = listOf(VectorPropertyKeyframe(frame = 0f, value = listOf(10f, 20f)))
      )

    val result1 = animatePosition(animatedPosition, LottieSettings(0.rf, emptySlotMap))
    assertThat(result1.x.constantValue).isEqualTo(10f)
    assertThat(result1.y.constantValue).isEqualTo(20f)

    val result2 = animatePosition(animatedPosition, LottieSettings(5.rf, emptySlotMap))
    assertThat(result2.x.constantValue).isEqualTo(10f)
    assertThat(result2.y.constantValue).isEqualTo(20f)
  }

  @Test
  fun animatePositionWithTwoKeyframes_returnsAnimatedValues() {
    val animatedPosition =
      AnimatedPositionProperty(
        keyframes =
          listOf(
            VectorPropertyKeyframe(frame = 0f, value = listOf(10f, 20f)),
            VectorPropertyKeyframe(frame = 10f, value = listOf(30f, 40f)),
          )
      )

    val firstFrameResult = animatePosition(animatedPosition, LottieSettings(0.rf, emptySlotMap))
    val middleFrameResult = animatePosition(animatedPosition, LottieSettings(5.rf, emptySlotMap))
    val lastFrameResult = animatePosition(animatedPosition, LottieSettings(10.rf, emptySlotMap))
    val afterAnimationResult =
      animatePosition(animatedPosition, LottieSettings(15.rf, emptySlotMap))

    assertThat(firstFrameResult.x.constantValue).isEqualTo(10f)
    assertThat(firstFrameResult.y.constantValue).isEqualTo(20f)
    assertThat(middleFrameResult.x.constantValue).isEqualTo(20f)
    assertThat(middleFrameResult.y.constantValue).isEqualTo(30f)
    assertThat(lastFrameResult.x.constantValue).isEqualTo(30f)
    assertThat(lastFrameResult.y.constantValue).isEqualTo(40f)
    assertThat(afterAnimationResult.x.constantValue).isEqualTo(30f)
    assertThat(afterAnimationResult.y.constantValue).isEqualTo(40f)
  }

  @Test
  fun animatePositionWithDelayedStart_holdsInitialValue() {
    val animatedPosition =
      AnimatedPositionProperty(
        keyframes =
          listOf(
            VectorPropertyKeyframe(frame = 5f, value = listOf(10f, 20f)),
            VectorPropertyKeyframe(frame = 10f, value = listOf(30f, 40f)),
          )
      )

    val beforeStartResult = animatePosition(animatedPosition, LottieSettings(0.rf, emptySlotMap))
    assertThat(beforeStartResult.x.constantValue).isEqualTo(10f)
    assertThat(beforeStartResult.y.constantValue).isEqualTo(20f)
  }

  @Test
  fun animatePositionWithHoldKeyframe_holdsValue() {
    val animatedPosition =
      AnimatedPositionProperty(
        keyframes =
          listOf(
            PositionPropertyKeyframe(frame = 0f, hold = true, value = listOf(10f, 20f)),
            PositionPropertyKeyframe(frame = 10f, value = listOf(30f, 40f)),
          )
      )

    val firstFrameResult = animatePosition(animatedPosition, LottieSettings(0.rf, emptySlotMap))
    val middleFrameResult = animatePosition(animatedPosition, LottieSettings(5.rf, emptySlotMap))
    val lastFrameResult = animatePosition(animatedPosition, LottieSettings(10.rf, emptySlotMap))
    val afterAnimationResult =
      animatePosition(animatedPosition, LottieSettings(15.rf, emptySlotMap))

    assertThat(firstFrameResult.x.constantValue).isEqualTo(10f)
    assertThat(firstFrameResult.y.constantValue).isEqualTo(20f)
    assertThat(middleFrameResult.x.constantValue).isEqualTo(10f)
    assertThat(middleFrameResult.y.constantValue).isEqualTo(20f)
    assertThat(lastFrameResult.x.constantValue).isEqualTo(30f)
    assertThat(lastFrameResult.y.constantValue).isEqualTo(40f)
    assertThat(afterAnimationResult.x.constantValue).isEqualTo(30f)
    assertThat(afterAnimationResult.y.constantValue).isEqualTo(40f)
  }

  @Test
  fun animatePositionWithSplitPosition_evaluatesXYIndependently() {
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

    val frame0Result = animatePosition(splitPosition, LottieSettings(0.rf, emptySlotMap))
    val frame5Result = animatePosition(splitPosition, LottieSettings(5.rf, emptySlotMap))
    val frame10Result = animatePosition(splitPosition, LottieSettings(10.rf, emptySlotMap))
    val frame15Result = animatePosition(splitPosition, LottieSettings(15.rf, emptySlotMap))

    assertThat(frame0Result.x.constantValue).isEqualTo(10f)
    assertThat(frame0Result.y.constantValue).isEqualTo(50f)
    assertThat(frame5Result.x.constantValue).isEqualTo(20f)
    assertThat(frame5Result.y.constantValue).isEqualTo(50f)
    assertThat(frame10Result.x.constantValue).isEqualTo(30f)
    assertThat(frame10Result.y.constantValue).isEqualTo(50f)
    assertThat(frame15Result.x.constantValue).isEqualTo(30f)
    assertThat(frame15Result.y.constantValue).isEqualTo(50f)
  }

  @Test
  fun animateScalarWithStaticInput_returnsInput() {
    val staticScalar = StaticScalarProperty(value = 42f)

    val result1 = animateScalar(staticScalar, LottieSettings(0.rf, emptySlotMap))
    assertThat(result1.constantValueOrNull).isEqualTo(42f)

    val result2 = animateScalar(staticScalar, LottieSettings(5.rf, emptySlotMap))
    assertThat(result2.constantValueOrNull).isEqualTo(42f)
  }

  @Test
  fun animateScalarWithEmptyKeyframes_returnsZero() {
    val animatedScalar = AnimatedScalarProperty(keyframes = emptyList())

    val result = animateScalar(animatedScalar, LottieSettings(0.rf, emptySlotMap))
    assertThat(result.constantValueOrNull).isEqualTo(0f)
  }

  @Test
  fun animateScalarWithSingleKeyframe_returnsInput() {
    val animatedScalar =
      AnimatedScalarProperty(keyframes = listOf(ScalarPropertyKeyframe(frame = 0f, value = 42f)))

    val result1 = animateScalar(animatedScalar, LottieSettings(0.rf, emptySlotMap))
    assertThat(result1.constantValueOrNull).isEqualTo(42f)

    val result2 = animateScalar(animatedScalar, LottieSettings(5.rf, emptySlotMap))
    assertThat(result2.constantValueOrNull).isEqualTo(42f)
  }

  @Test
  fun animateScalarWithTwoKeyframes_returnsAnimatedValues() {
    val animatedScalar =
      AnimatedScalarProperty(
        keyframes =
          listOf(
            ScalarPropertyKeyframe(frame = 0f, value = 10f),
            ScalarPropertyKeyframe(frame = 10f, value = 30f),
          )
      )

    val firstFrameResult = animateScalar(animatedScalar, LottieSettings(0.rf, emptySlotMap))
    val middleFrameResult = animateScalar(animatedScalar, LottieSettings(5.rf, emptySlotMap))
    val lastFrameResult = animateScalar(animatedScalar, LottieSettings(10.rf, emptySlotMap))
    val afterAnimationResult = animateScalar(animatedScalar, LottieSettings(15.rf, emptySlotMap))

    assertThat(firstFrameResult.constantValueOrNull).isEqualTo(10f)
    assertThat(middleFrameResult.constantValueOrNull).isEqualTo(20f)
    assertThat(lastFrameResult.constantValueOrNull).isEqualTo(30f)
    assertThat(afterAnimationResult.constantValueOrNull).isEqualTo(30f)
  }

  @Test
  fun animateScalarWithDelayedStart_holdsInitialValue() {
    val animatedScalar =
      AnimatedScalarProperty(
        keyframes =
          listOf(
            ScalarPropertyKeyframe(frame = 5f, value = 10f),
            ScalarPropertyKeyframe(frame = 10f, value = 30f),
          )
      )

    val beforeStartResult = animateScalar(animatedScalar, LottieSettings(0.rf, emptySlotMap))
    assertThat(beforeStartResult.constantValueOrNull).isEqualTo(10f)
  }

  @Test
  fun animateScalarWithHoldKeyframe_holdsValue() {
    val animatedScalar =
      AnimatedScalarProperty(
        keyframes =
          listOf(
            ScalarPropertyKeyframe(frame = 0f, hold = true, value = 10f),
            ScalarPropertyKeyframe(frame = 10f, value = 30f),
          )
      )

    val firstFrameResult = animateScalar(animatedScalar, LottieSettings(0.rf, emptySlotMap))
    val middleFrameResult = animateScalar(animatedScalar, LottieSettings(5.rf, emptySlotMap))
    val lastFrameResult = animateScalar(animatedScalar, LottieSettings(10.rf, emptySlotMap))
    val afterAnimationResult = animateScalar(animatedScalar, LottieSettings(15.rf, emptySlotMap))

    assertThat(firstFrameResult.constantValueOrNull).isEqualTo(10f)
    assertThat(middleFrameResult.constantValueOrNull).isEqualTo(10f)
    assertThat(lastFrameResult.constantValueOrNull).isEqualTo(30f)
    assertThat(afterAnimationResult.constantValueOrNull).isEqualTo(30f)
  }

  @Test
  fun animateGradientWithStaticInput_returnsInput() {
    val staticGradient =
      StaticGradientProperty(
        value = GradientValue(numberOfColors = 2, values = listOf(0f, 1f, 0f, 0f, 1f, 0f, 1f, 0f))
      )

    val result1 = animateGradient(staticGradient, LottieSettings(0.rf, emptySlotMap))
    assertThat(result1.numberOfColors).isEqualTo(2)
    assertThat(result1.hasTransparency).isFalse()
    assertThat(result1.values.map { it.constantValueOrNull }).isEqualTo(staticGradient.value.values)

    val result2 = animateGradient(staticGradient, LottieSettings(5.rf, emptySlotMap))
    assertThat(result2.values.map { it.constantValueOrNull }).isEqualTo(staticGradient.value.values)
  }

  @Test
  fun animateGradientWithStaticTransparentInput_returnsInput() {
    val staticGradient =
      StaticGradientProperty(
        value =
          GradientValue(
            numberOfColors = 2,
            values = listOf(0f, 1f, 0f, 0f, 1f, 0f, 1f, 0f, 0f, 1f, 1f, 0.5f),
          )
      )

    val result = animateGradient(staticGradient, LottieSettings(0.rf, emptySlotMap))
    assertThat(result.numberOfColors).isEqualTo(2)
    assertThat(result.hasTransparency).isTrue()
    assertThat(result.values.map { it.constantValueOrNull }).isEqualTo(staticGradient.value.values)
  }

  @Test
  fun animateGradientWithEmptyKeyframes_returnsEmpty() {
    val animatedGradient = AnimatedGradientProperty(numberOfColors = 2, keyframes = emptyList())

    val result = animateGradient(animatedGradient, LottieSettings(0.rf, emptySlotMap))
    assertThat(result.numberOfColors).isEqualTo(2)
    assertThat(result.values).isEmpty()
  }

  @Test
  fun animateGradientWithSingleKeyframe_returnsInput() {
    val gradientValue =
      GradientValue(numberOfColors = 2, values = listOf(0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f))
    val animatedGradient =
      AnimatedGradientProperty(
        numberOfColors = 2,
        keyframes = listOf(GradientPropertyKeyframe(frame = 0f, value = listOf(gradientValue))),
      )

    val result1 = animateGradient(animatedGradient, LottieSettings(0.rf, emptySlotMap))
    assertThat(result1.numberOfColors).isEqualTo(2)
    assertThat(result1.values.map { it.constantValueOrNull }).isEqualTo(gradientValue.values)

    val result2 = animateGradient(animatedGradient, LottieSettings(5.rf, emptySlotMap))
    assertThat(result2.values.map { it.constantValueOrNull }).isEqualTo(gradientValue.values)
  }

  @Test
  fun animateGradientWithTwoKeyframes_returnsAnimatedValues() {
    val keyframe1 =
      GradientPropertyKeyframe(
        frame = 0f,
        value =
          listOf(GradientValue(numberOfColors = 2, values = listOf(0f, 1f, 0f, 0f, 1f, 0f, 1f, 0f))),
      )
    val keyframe2 =
      GradientPropertyKeyframe(
        frame = 10f,
        value =
          listOf(GradientValue(numberOfColors = 2, values = listOf(0f, 0f, 1f, 0f, 1f, 0f, 0f, 1f))),
      )
    val animatedGradient =
      AnimatedGradientProperty(numberOfColors = 2, keyframes = listOf(keyframe1, keyframe2))

    val firstFrameResult = animateGradient(animatedGradient, LottieSettings(0.rf, emptySlotMap))
    val middleFrameResult = animateGradient(animatedGradient, LottieSettings(5.rf, emptySlotMap))
    val lastFrameResult = animateGradient(animatedGradient, LottieSettings(10.rf, emptySlotMap))
    val afterAnimationResult =
      animateGradient(animatedGradient, LottieSettings(15.rf, emptySlotMap))

    assertThat(firstFrameResult.values.map { it.constantValueOrNull })
      .isEqualTo(listOf(0f, 1f, 0f, 0f, 1f, 0f, 1f, 0f))
    assertThat(middleFrameResult.values.map { it.constantValueOrNull })
      .isEqualTo(listOf(0f, 0.5f, 0.5f, 0f, 1f, 0f, 0.5f, 0.5f))
    assertThat(lastFrameResult.values.map { it.constantValueOrNull })
      .isEqualTo(listOf(0f, 0f, 1f, 0f, 1f, 0f, 0f, 1f))
    assertThat(afterAnimationResult.values.map { it.constantValueOrNull })
      .isEqualTo(listOf(0f, 0f, 1f, 0f, 1f, 0f, 0f, 1f))
  }

  @Test
  fun animateGradientWithHoldKeyframe_holdsValue() {
    val keyframe1 =
      GradientPropertyKeyframe(
        frame = 0f,
        hold = true,
        value =
          listOf(GradientValue(numberOfColors = 2, values = listOf(0f, 1f, 0f, 0f, 1f, 0f, 1f, 0f))),
      )
    val keyframe2 =
      GradientPropertyKeyframe(
        frame = 10f,
        value =
          listOf(GradientValue(numberOfColors = 2, values = listOf(0f, 0f, 0f, 1f, 1f, 1f, 1f, 1f))),
      )
    val animatedGradient =
      AnimatedGradientProperty(numberOfColors = 2, keyframes = listOf(keyframe1, keyframe2))

    val firstFrameResult = animateGradient(animatedGradient, LottieSettings(0.rf, emptySlotMap))
    val middleFrameResult = animateGradient(animatedGradient, LottieSettings(5.rf, emptySlotMap))
    val lastFrameResult = animateGradient(animatedGradient, LottieSettings(10.rf, emptySlotMap))
    val afterAnimationResult =
      animateGradient(animatedGradient, LottieSettings(15.rf, emptySlotMap))

    assertThat(firstFrameResult.values.map { it.constantValueOrNull })
      .isEqualTo(listOf(0f, 1f, 0f, 0f, 1f, 0f, 1f, 0f))
    assertThat(middleFrameResult.values.map { it.constantValueOrNull })
      .isEqualTo(listOf(0f, 1f, 0f, 0f, 1f, 0f, 1f, 0f))
    assertThat(lastFrameResult.values.map { it.constantValueOrNull })
      .isEqualTo(listOf(0f, 0f, 0f, 1f, 1f, 1f, 1f, 1f))
    assertThat(afterAnimationResult.values.map { it.constantValueOrNull })
      .isEqualTo(listOf(0f, 0f, 0f, 1f, 1f, 1f, 1f, 1f))
  }

  @Test
  fun animateGradientWithDelayedStart_holdsInitialValue() {
    val keyframe1 =
      GradientPropertyKeyframe(
        frame = 5f,
        value =
          listOf(GradientValue(numberOfColors = 2, values = listOf(0f, 1f, 0f, 0f, 1f, 0f, 1f, 0f))),
      )
    val keyframe2 =
      GradientPropertyKeyframe(
        frame = 10f,
        value =
          listOf(GradientValue(numberOfColors = 2, values = listOf(0f, 0f, 1f, 0f, 1f, 0f, 0f, 1f))),
      )
    val animatedGradient =
      AnimatedGradientProperty(numberOfColors = 2, keyframes = listOf(keyframe1, keyframe2))

    val beforeStartResult = animateGradient(animatedGradient, LottieSettings(0.rf, emptySlotMap))
    val startFrameResult = animateGradient(animatedGradient, LottieSettings(5.rf, emptySlotMap))
    val endFrameResult = animateGradient(animatedGradient, LottieSettings(10.rf, emptySlotMap))

    assertThat(beforeStartResult.values.map { it.constantValueOrNull })
      .isEqualTo(listOf(0f, 1f, 0f, 0f, 1f, 0f, 1f, 0f))
    assertThat(startFrameResult.values.map { it.constantValueOrNull })
      .isEqualTo(listOf(0f, 1f, 0f, 0f, 1f, 0f, 1f, 0f))
    assertThat(endFrameResult.values.map { it.constantValueOrNull })
      .isEqualTo(listOf(0f, 0f, 1f, 0f, 1f, 0f, 0f, 1f))
  }

  @Test
  fun parseHexColor_parsesSixDigitHexWithAndWithoutHash() {
    assertThat(parseHexColor("#ff0000")).isEqualTo(Color(0xFFFF0000))
    assertThat(parseHexColor("00ff00")).isEqualTo(Color(0xFF00FF00))
    assertThat(parseHexColor("#0000ff")).isEqualTo(Color(0xFF0000FF))
  }

  @Test
  fun parseHexColor_parsesEightDigitHexWithAndWithoutHash() {
    assertThat(parseHexColor("#80ff0000")).isEqualTo(Color(0x80FF0000))
    assertThat(parseHexColor("4000ff00")).isEqualTo(Color(0x4000FF00))
  }

  @Test
  fun parseHexColor_parsesThreeDigitHexWithAndWithoutHash() {
    assertThat(parseHexColor("#f00")).isEqualTo(Color(0xFFFF0000))
    assertThat(parseHexColor("0f0")).isEqualTo(Color(0xFF00FF00))
  }

  @Test
  fun parseHexColor_invalidColorReturnsTransparent() {
    assertThat(parseHexColor("")).isEqualTo(Color.Transparent)
    assertThat(parseHexColor("invalid_hex")).isEqualTo(Color.Transparent)
    assertThat(parseHexColor("#zzzzzz")).isEqualTo(Color.Transparent)
  }

  @Test
  fun buildAncestorTransforms_withLinearHierarchy_populatesOrderedTransforms() {
    val t1 = Transform(name = "t1")
    val t2 = Transform(name = "t2")
    val t3 = Transform(name = "t3")

    val root = NullLayer(index = 1, parent = null, transform = t1)
    val child = NullLayer(index = 2, parent = 1, transform = t2)
    val grandChild = NullLayer(index = 3, parent = 2, transform = t3)

    val transforms = buildAncestorTransforms(listOf(root, child, grandChild))

    assertThat(transforms[1]).isEmpty()
    assertThat(transforms[2]).isEqualTo(listOf(t1))
    assertThat(transforms[3]).isEqualTo(listOf(t1, t2))
  }

  @Test
  fun buildAncestorTransforms_withCycleInTree_haltsRecursionWithoutStackOverflow() {
    val t1 = Transform(name = "t1")
    val t2 = Transform(name = "t2")
    val t3 = Transform(name = "t3")

    // 1 (root) -> 2 (parent 1) -> 3 (parent 2) -> 1 (parent 3: cycle back to 1)
    val root = NullLayer(index = 1, parent = null, transform = t1)
    val child = NullLayer(index = 2, parent = 1, transform = t2)
    val grandChild = NullLayer(index = 3, parent = 2, transform = t3)
    val cycleChild = NullLayer(index = 1, parent = 3, transform = t1)

    val transforms = buildAncestorTransforms(listOf(root, child, grandChild, cycleChild))

    assertThat(transforms).isNotNull()
    assertThat(transforms[1]).isEmpty()
    assertThat(transforms[2]).isEqualTo(listOf(t1))
    assertThat(transforms[3]).isEqualTo(listOf(t1, t2))
  }

  @Test
  fun buildAncestorTransforms_withSelfReferencingParent_haltsRecursion() {
    val t1 = Transform(name = "t1")
    val root = NullLayer(index = 1, parent = null, transform = t1)
    val selfRefChild = NullLayer(index = 2, parent = 1, transform = t1)
    val selfChild = NullLayer(index = 2, parent = 2, transform = t1)

    val transforms = buildAncestorTransforms(listOf(root, selfRefChild, selfChild))
    assertThat(transforms).isNotNull()
    assertThat(transforms[1]).isEmpty()
    assertThat(transforms[2]).isEqualTo(listOf(t1))
  }

  @Test
  fun buildAncestorTransforms_withDisconnectedCycle_doesNotHangAndReturnsEmpty() {
    val t1 = Transform(name = "t1")
    val t2 = Transform(name = "t2")

    // Disconnected cycle: 1 -> 2 -> 1, none rooted at null
    val layer1 = NullLayer(index = 1, parent = 2, transform = t1)
    val layer2 = NullLayer(index = 2, parent = 1, transform = t2)

    val transforms = buildAncestorTransforms(listOf(layer1, layer2))
    assertThat(transforms).isEmpty()
  }

  @Test
  fun buildRemotePathFromBezier_withEmptyOrDegenerateInput_safelyConstructsPath() {
    val emptyPath = buildRemotePathFromBezier(emptyList())
    assertThat(emptyPath).isNotNull()

    val degenerateSubpath =
      RemoteBezierValue(
        closed = true,
        vertices = emptyList(),
        inTangents = emptyList(),
        outTangents = emptyList(),
      )
    val resultEmptyVertices = buildRemotePathFromBezier(listOf(degenerateSubpath))
    assertThat(resultEmptyVertices).isNotNull()

    val singleVertexSubpath =
      RemoteBezierValue(
        closed = false,
        vertices = listOf(listOf(10f.rf, 20f.rf)),
        inTangents = emptyList(),
        outTangents = emptyList(),
      )
    val resultSingleVertex = buildRemotePathFromBezier(listOf(singleVertexSubpath))
    assertThat(resultSingleVertex).isNotNull()

    val singleVertexClosedSubpath =
      RemoteBezierValue(
        closed = true,
        vertices = listOf(listOf(10f.rf, 20f.rf)),
        inTangents = emptyList(),
        outTangents = emptyList(),
      )
    val resultSingleVertexClosed = buildRemotePathFromBezier(listOf(singleVertexClosedSubpath))
    assertThat(resultSingleVertexClosed).isNotNull()
  }
}
