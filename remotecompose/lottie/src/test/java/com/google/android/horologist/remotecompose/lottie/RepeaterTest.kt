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
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rf
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.Rectangle
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.grouping.Transform
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.CompositeMode
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.Repeater
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.Fill
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticColorProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticPositionProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticScalarProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticVectorProperty
import com.google.android.horologist.remotecompose.lottie.renderer.RemoteLottiePath
import com.google.android.horologist.remotecompose.lottie.renderer.gatherShapesForTest
import com.google.android.horologist.remotecompose.lottie.renderer.properties.RemoteBezierValue
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.evaluateRepeater
import com.google.common.truth.Truth.assertThat
import org.junit.Test

@SuppressLint("RestrictedApi")
class RepeaterTest {

  private val settings = LottieSettings(0f.rf, SlotMap(emptyMap()))

  @Test
  fun repeaterGeometryDuplication_linearTranslation() {
    val subpath =
      RemoteBezierValue(
        closed = true,
        vertices = listOf(listOf(10f.rf, 10f.rf)),
        inTangents = listOf(listOf(0f.rf, 0f.rf)),
        outTangents = listOf(listOf(0f.rf, 0f.rf)),
      )
    val lottiePath = RemoteLottiePath(listOf(subpath))

    val repeater =
      Repeater(
        copies = StaticScalarProperty(value = 3f),
        offset = StaticScalarProperty(value = 0f),
        composite = CompositeMode.Above,
        transform =
          Transform(
            anchorPoint = StaticPositionProperty(value = listOf(0f, 0f)),
            positionTranslation = StaticPositionProperty(value = listOf(50f, 0f)),
            scale = StaticVectorProperty(value = listOf(100f, 100f)),
            rotation = StaticScalarProperty(value = 0f),
            opacity = StaticScalarProperty(value = 100f),
          ),
      )

    val results = evaluateRepeater(listOf(lottiePath), repeater, settings)
    assertThat(results).hasSize(3)

    // Copy 0: offset 0 -> position (10, 10)
    val path0 = results[0].shape as RemoteLottiePath
    assertThat(path0.path[0].vertices[0][0].constantValueOrNull).isWithin(0.01f).of(10f)
    assertThat(path0.path[0].vertices[0][1].constantValueOrNull).isWithin(0.01f).of(10f)

    // Copy 1: offset 1 -> position (60, 10)
    val path1 = results[1].shape as RemoteLottiePath
    assertThat(path1.path[0].vertices[0][0].constantValueOrNull).isWithin(0.01f).of(60f)
    assertThat(path1.path[0].vertices[0][1].constantValueOrNull).isWithin(0.01f).of(10f)

    // Copy 2: offset 2 -> position (110, 10)
    val path2 = results[2].shape as RemoteLottiePath
    assertThat(path2.path[0].vertices[0][0].constantValueOrNull).isWithin(0.01f).of(110f)
    assertThat(path2.path[0].vertices[0][1].constantValueOrNull).isWithin(0.01f).of(10f)
  }

  @Test
  fun repeaterGeometryDuplication_withOffset() {
    val subpath =
      RemoteBezierValue(
        closed = true,
        vertices = listOf(listOf(10f.rf, 10f.rf)),
        inTangents = listOf(listOf(0f.rf, 0f.rf)),
        outTangents = listOf(listOf(0f.rf, 0f.rf)),
      )
    val lottiePath = RemoteLottiePath(listOf(subpath))

    val repeater =
      Repeater(
        copies = StaticScalarProperty(value = 3f),
        offset = StaticScalarProperty(value = 1f),
        composite = CompositeMode.Above,
        transform =
          Transform(
            anchorPoint = StaticPositionProperty(value = listOf(0f, 0f)),
            positionTranslation = StaticPositionProperty(value = listOf(50f, 0f)),
            scale = StaticVectorProperty(value = listOf(100f, 100f)),
            rotation = StaticScalarProperty(value = 0f),
            opacity = StaticScalarProperty(value = 100f),
          ),
      )

    val results = evaluateRepeater(listOf(lottiePath), repeater, settings)
    assertThat(results).hasSize(3)

    // Copy 0 with offset 1 -> k = 1 -> position (60, 10)
    val path0 = results[0].shape as RemoteLottiePath
    assertThat(path0.path[0].vertices[0][0].constantValueOrNull).isWithin(0.01f).of(60f)

    // Copy 1 with offset 1 -> k = 2 -> position (110, 10)
    val path1 = results[1].shape as RemoteLottiePath
    assertThat(path1.path[0].vertices[0][0].constantValueOrNull).isWithin(0.01f).of(110f)

    // Copy 2 with offset 1 -> k = 3 -> position (160, 10)
    val path2 = results[2].shape as RemoteLottiePath
    assertThat(path2.path[0].vertices[0][0].constantValueOrNull).isWithin(0.01f).of(160f)
  }

  @Test
  fun repeaterGeometryDuplication_rotationAndScale() {
    val subpath =
      RemoteBezierValue(
        closed = true,
        vertices = listOf(listOf(100f.rf, 0f.rf)),
        inTangents = listOf(listOf(0f.rf, 0f.rf)),
        outTangents = listOf(listOf(0f.rf, 0f.rf)),
      )
    val lottiePath = RemoteLottiePath(listOf(subpath))

    val repeater =
      Repeater(
        copies = StaticScalarProperty(value = 4f),
        offset = StaticScalarProperty(value = 0f),
        composite = CompositeMode.Above,
        transform =
          Transform(
            anchorPoint = StaticPositionProperty(value = listOf(0f, 0f)),
            positionTranslation = StaticPositionProperty(value = listOf(0f, 0f)),
            scale = StaticVectorProperty(value = listOf(100f, 100f)),
            rotation = StaticScalarProperty(value = 90f),
            opacity = StaticScalarProperty(value = 100f),
          ),
      )

    val results = evaluateRepeater(listOf(lottiePath), repeater, settings)
    assertThat(results).hasSize(4)

    // Copy 0: rot 0 deg -> (100, 0)
    val path0 = results[0].shape as RemoteLottiePath
    assertThat(path0.path[0].vertices[0][0].constantValueOrNull).isWithin(0.01f).of(100f)
    assertThat(path0.path[0].vertices[0][1].constantValueOrNull).isWithin(0.01f).of(0f)

    // Copy 1: rot 90 deg -> (0, 100)
    val path1 = results[1].shape as RemoteLottiePath
    assertThat(path1.path[0].vertices[0][0].constantValueOrNull).isWithin(0.01f).of(0f)
    assertThat(path1.path[0].vertices[0][1].constantValueOrNull).isWithin(0.01f).of(100f)

    // Copy 2: rot 180 deg -> (-100, 0)
    val path2 = results[2].shape as RemoteLottiePath
    assertThat(path2.path[0].vertices[0][0].constantValueOrNull).isWithin(0.01f).of(-100f)
    assertThat(path2.path[0].vertices[0][1].constantValueOrNull).isWithin(0.01f).of(0f)

    // Copy 3: rot 270 deg -> (0, -100)
    val path3 = results[3].shape as RemoteLottiePath
    assertThat(path3.path[0].vertices[0][0].constantValueOrNull).isWithin(0.01f).of(0f)
    assertThat(path3.path[0].vertices[0][1].constantValueOrNull).isWithin(0.01f).of(-100f)
  }

  @Test
  fun repeaterCompositeMode_belowReversesOrder() {
    val subpath =
      RemoteBezierValue(
        closed = true,
        vertices = listOf(listOf(0f.rf, 0f.rf)),
        inTangents = listOf(listOf(0f.rf, 0f.rf)),
        outTangents = listOf(listOf(0f.rf, 0f.rf)),
      )
    val lottiePath = RemoteLottiePath(listOf(subpath))

    val repeater =
      Repeater(
        copies = StaticScalarProperty(value = 3f),
        offset = StaticScalarProperty(value = 0f),
        composite = CompositeMode.Below,
        transform =
          Transform(
            anchorPoint = StaticPositionProperty(value = listOf(0f, 0f)),
            positionTranslation = StaticPositionProperty(value = listOf(10f, 0f)),
            scale = StaticVectorProperty(value = listOf(100f, 100f)),
            rotation = StaticScalarProperty(value = 0f),
            opacity = StaticScalarProperty(value = 100f),
          ),
      )

    val results = evaluateRepeater(listOf(lottiePath), repeater, settings)
    assertThat(results).hasSize(3)

    // CompositeMode.Below renders index 2 first, then 1, then 0
    val path0 = results[0].shape as RemoteLottiePath
    assertThat(path0.path[0].vertices[0][0].constantValueOrNull).isWithin(0.01f).of(20f)

    val path1 = results[1].shape as RemoteLottiePath
    assertThat(path1.path[0].vertices[0][0].constantValueOrNull).isWithin(0.01f).of(10f)

    val path2 = results[2].shape as RemoteLottiePath
    assertThat(path2.path[0].vertices[0][0].constantValueOrNull).isWithin(0.01f).of(0f)
  }

  @Test
  fun repeaterOpacityInterpolation_startEndOpacity() {
    val subpath =
      RemoteBezierValue(
        closed = true,
        vertices = listOf(listOf(0f.rf, 0f.rf)),
        inTangents = listOf(listOf(0f.rf, 0f.rf)),
        outTangents = listOf(listOf(0f.rf, 0f.rf)),
      )
    val lottiePath = RemoteLottiePath(listOf(subpath))

    val repeater =
      Repeater(
        copies = StaticScalarProperty(value = 3f),
        offset = StaticScalarProperty(value = 0f),
        composite = CompositeMode.Above,
        transform =
          Transform(
            anchorPoint = StaticPositionProperty(value = listOf(0f, 0f)),
            positionTranslation = StaticPositionProperty(value = listOf(10f, 0f)),
            scale = StaticVectorProperty(value = listOf(100f, 100f)),
            rotation = StaticScalarProperty(value = 0f),
            startOpacity = StaticScalarProperty(value = 100f),
            endOpacity = StaticScalarProperty(value = 0f),
          ),
      )

    val results = evaluateRepeater(listOf(lottiePath), repeater, settings)
    assertThat(results).hasSize(3)

    assertThat(results[0].opacityMultiplier.constantValueOrNull).isWithin(0.01f).of(1f)
    assertThat(results[1].opacityMultiplier.constantValueOrNull).isWithin(0.01f).of(0.5f)
    assertThat(results[2].opacityMultiplier.constantValueOrNull).isWithin(0.01f).of(0f)
  }

  @Test
  fun repeaterInShapeLayer_gatherShapesIntegration() {
    val rect =
      Rectangle(
        position = StaticPositionProperty(value = listOf(0f, 0f)),
        size = StaticVectorProperty(value = listOf(20f, 20f)),
      )
    val repeater =
      Repeater(
        copies = StaticScalarProperty(value = 3f),
        offset = StaticScalarProperty(value = 0f),
        composite = CompositeMode.Above,
        transform =
          Transform(
            anchorPoint = StaticPositionProperty(value = listOf(0f, 0f)),
            positionTranslation = StaticPositionProperty(value = listOf(30f, 0f)),
            scale = StaticVectorProperty(value = listOf(100f, 100f)),
            rotation = StaticScalarProperty(value = 0f),
            opacity = StaticScalarProperty(value = 100f),
          ),
      )
    val fill = Fill(color = StaticColorProperty(value = androidx.compose.ui.graphics.Color.Red.rc))

    val styledGroups = gatherShapesForTest(listOf(rect, repeater, fill), settings)
    assertThat(styledGroups).isNotEmpty()
    val totalShapes = styledGroups.sumOf { it.shapes.size }
    assertThat(totalShapes).isEqualTo(3)
  }

  @Test
  fun repeaterInShapeLayer_withVaryingOpacity_drawOrder() {
    val rect =
      Rectangle(
        position = StaticPositionProperty(value = listOf(0f, 0f)),
        size = StaticVectorProperty(value = listOf(20f, 20f)),
      )
    val repeater =
      Repeater(
        copies = StaticScalarProperty(value = 3f),
        offset = StaticScalarProperty(value = 0f),
        composite = CompositeMode.Above,
        transform =
          Transform(
            anchorPoint = StaticPositionProperty(value = listOf(0f, 0f)),
            positionTranslation = StaticPositionProperty(value = listOf(30f, 0f)),
            scale = StaticVectorProperty(value = listOf(100f, 100f)),
            rotation = StaticScalarProperty(value = 0f),
            startOpacity = StaticScalarProperty(value = 100f),
            endOpacity = StaticScalarProperty(value = 20f),
          ),
      )
    val fill = Fill(color = StaticColorProperty(value = androidx.compose.ui.graphics.Color.Red.rc))

    val styledGroups = gatherShapesForTest(listOf(rect, repeater, fill), settings)
    // For varying opacity, 3 individual StyledShapes are created
    assertThat(styledGroups).hasSize(3)
    // Draw order must be copy 0, then copy 1, then copy 2
    val p0 = styledGroups[0].shapes[0] as RemoteLottiePath
    val p1 = styledGroups[1].shapes[0] as RemoteLottiePath
    val p2 = styledGroups[2].shapes[0] as RemoteLottiePath

    assertThat(p0.path[0].vertices[0][0].constantValueOrNull).isWithin(0.01f).of(10f)
    assertThat(p1.path[0].vertices[0][0].constantValueOrNull).isWithin(0.01f).of(40f)
    assertThat(p2.path[0].vertices[0][0].constantValueOrNull).isWithin(0.01f).of(70f)
  }
}
