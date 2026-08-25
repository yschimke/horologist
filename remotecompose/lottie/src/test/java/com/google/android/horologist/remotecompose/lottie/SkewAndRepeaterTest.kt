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
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.grouping.Transform
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.Repeater
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticPositionProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticScalarProperty
import com.google.android.horologist.remotecompose.lottie.renderer.NoopStyle
import com.google.android.horologist.remotecompose.lottie.renderer.RemoteGroup
import com.google.android.horologist.remotecompose.lottie.renderer.RemoteLottiePath
import com.google.android.horologist.remotecompose.lottie.renderer.StyledShapes
import com.google.android.horologist.remotecompose.lottie.renderer.properties.RemoteBezierValue
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.evaluateRepeater
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SuppressLint("RestrictedApi")
@RunWith(AndroidJUnit4::class)
class SkewAndRepeaterTest {

  private fun createPointBezier(x: Float, y: Float): RemoteBezierValue {
    return RemoteBezierValue(
      closed = true,
      vertices = listOf(listOf(x.rf, y.rf)),
      inTangents = listOf(listOf(0f.rf, 0f.rf)),
      outTangents = listOf(listOf(0f.rf, 0f.rf)),
    )
  }

  @Test
  fun evaluateRepeater_withNestedRemoteGroup_recursivelyTransformsChildShapes() {
    val childPath = RemoteLottiePath(listOf(createPointBezier(0f, 0f)))
    val group =
      RemoteGroup(
        childShapes = listOf(StyledShapes(shapes = listOf(childPath), style = NoopStyle())),
        animationSettings = LottieSettings(currentFrame = 0f.rf),
        transform = null,
      )

    val repeaterTransform =
      Transform(
        positionTranslation = StaticPositionProperty(value = listOf(20f, 0f)),
        anchorPoint = StaticPositionProperty(value = listOf(0f, 0f)),
        startOpacity = StaticScalarProperty(value = 100f),
        endOpacity = StaticScalarProperty(value = 50f),
      )

    val repeater =
      Repeater(
        copies = StaticScalarProperty(value = 3f),
        offset = StaticScalarProperty(value = 0f),
        transform = repeaterTransform,
      )

    val settings = LottieSettings(currentFrame = 0f.rf)
    val instances = evaluateRepeater(listOf(group), repeater, settings)

    assertThat(instances).hasSize(3)

    // Instance 0 (k = 0): X offset = 0, opacity = 1.0
    val group0 = instances[0].shape as RemoteGroup
    val path0 = group0.childShapes[0].shapes[0] as RemoteLottiePath
    assertThat(path0.path[0].vertices[0][0].constantValueOrNull).isWithin(0.01f).of(0f)
    assertThat(instances[0].opacityMultiplier.constantValueOrNull).isWithin(0.01f).of(1.0f)

    // Instance 1 (k = 1): X offset = 20, opacity = 0.75
    val group1 = instances[1].shape as RemoteGroup
    val path1 = group1.childShapes[0].shapes[0] as RemoteLottiePath
    assertThat(path1.path[0].vertices[0][0].constantValueOrNull).isWithin(0.01f).of(20f)
    assertThat(instances[1].opacityMultiplier.constantValueOrNull).isWithin(0.01f).of(0.75f)

    // Instance 2 (k = 2): X offset = 40, opacity = 0.5
    val group2 = instances[2].shape as RemoteGroup
    val path2 = group2.childShapes[0].shapes[0] as RemoteLottiePath
    assertThat(path2.path[0].vertices[0][0].constantValueOrNull).isWithin(0.01f).of(40f)
    assertThat(instances[2].opacityMultiplier.constantValueOrNull).isWithin(0.01f).of(0.5f)
  }
}
