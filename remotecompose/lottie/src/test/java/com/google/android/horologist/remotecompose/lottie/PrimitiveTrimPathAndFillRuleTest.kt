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
import androidx.compose.ui.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.Ellipse
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.PolyStar
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.PolyStarType
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.Rectangle
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.TrimPath
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.Fill
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.FillRule
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.GradientFill
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.GradientType
import com.google.android.horologist.remotecompose.lottie.format.properties.AnimatedScalarProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.ScalarPropertyKeyframe
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticColorProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticGradientProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticPositionProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticScalarProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticVectorProperty
import com.google.android.horologist.remotecompose.lottie.format.values.GradientValue
import com.google.android.horologist.remotecompose.lottie.renderer.RemoteCompiledPath
import com.google.android.horologist.remotecompose.lottie.renderer.RemoteFill
import com.google.android.horologist.remotecompose.lottie.renderer.RemoteGradientFill
import com.google.android.horologist.remotecompose.lottie.renderer.RemoteLottiePath
import com.google.android.horologist.remotecompose.lottie.renderer.gatherShapesForTest
import com.google.android.horologist.remotecompose.lottie.renderer.properties.Point
import com.google.android.horologist.remotecompose.lottie.renderer.properties.RemoteGradientValue
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.evaluateEllipse
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.evaluatePolyStar
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.evaluateRectangle
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SuppressLint("RestrictedApi")
@RunWith(AndroidJUnit4::class)
class PrimitiveTrimPathAndFillRuleTest {

  private val settings = LottieSettings(currentFrame = 0f.rf, slotMap = SlotMap.Empty)

  @Test
  fun evaluateRectangle_withStaticTrimPath_trimsPathCorrectly() {
    val rect =
      Rectangle(
        position = StaticPositionProperty(value = listOf(0f, 0f)),
        size = StaticVectorProperty(value = listOf(100f, 100f)),
        cornerRadius = StaticScalarProperty(value = 0f),
      )
    val trimPath =
      TrimPath(
        start = StaticScalarProperty(value = 0f),
        end = StaticScalarProperty(value = 50f),
        offset = StaticScalarProperty(value = 0f),
      )

    val result = evaluateRectangle(rect, settings, trimPath)
    assertThat(result).isNotNull()
    assertThat(result!!.path).isNotEmpty()
    // Trimmed subpath should now be open
    val subpath = result.path.first()
    assertThat(subpath.closed).isFalse()
  }

  @Test
  fun evaluateEllipse_withStaticTrimPath_trimsPathCorrectly() {
    val ellipse =
      Ellipse(
        position = StaticPositionProperty(value = listOf(0f, 0f)),
        size = StaticVectorProperty(value = listOf(100f, 100f)),
      )
    val trimPath =
      TrimPath(
        start = StaticScalarProperty(value = 25f),
        end = StaticScalarProperty(value = 75f),
        offset = StaticScalarProperty(value = 0f),
      )

    val result = evaluateEllipse(ellipse, settings, trimPath)
    assertThat(result).isNotNull()
    assertThat(result!!.path).isNotEmpty()
    val subpath = result.path.first()
    assertThat(subpath.closed).isFalse()
  }

  @Test
  fun evaluatePolyStar_withStaticTrimPath_trimsPathCorrectly() {
    val star =
      PolyStar(
        starType = PolyStarType.Star,
        points = StaticScalarProperty(value = 5f),
        position = StaticPositionProperty(value = listOf(0f, 0f)),
        rotation = StaticScalarProperty(value = 0f),
        innerRadius = StaticScalarProperty(value = 20f),
        outerRadius = StaticScalarProperty(value = 50f),
        innerRoundedness = StaticScalarProperty(value = 0f),
        outerRoundedness = StaticScalarProperty(value = 0f),
      )
    val trimPath =
      TrimPath(
        start = StaticScalarProperty(value = 0f),
        end = StaticScalarProperty(value = 30f),
        offset = StaticScalarProperty(value = 0f),
      )

    val result = evaluatePolyStar(star, settings, trimPath)
    assertThat(result).isNotNull()
    assertThat(result!!.path).isNotEmpty()
    val subpath = result.path.first()
    assertThat(subpath.closed).isFalse()
  }

  @Test
  fun evaluateRectangle_withAnimatedTrimPath_evaluatesAcrossFrames() {
    val rect =
      Rectangle(
        position = StaticPositionProperty(value = listOf(0f, 0f)),
        size = StaticVectorProperty(value = listOf(100f, 100f)),
        cornerRadius = StaticScalarProperty(value = 0f),
      )
    val trimPath =
      TrimPath(
        start = StaticScalarProperty(value = 0f),
        end =
          AnimatedScalarProperty(
            keyframes =
              listOf(
                ScalarPropertyKeyframe(frame = 0f, value = 0f),
                ScalarPropertyKeyframe(frame = 30f, value = 100f),
              )
          ),
        offset = StaticScalarProperty(value = 0f),
      )

    val frame0Settings = LottieSettings(currentFrame = 0f.rf)
    val resultFrame0 = evaluateRectangle(rect, frame0Settings, trimPath)
    assertThat(resultFrame0).isNotNull()

    val frame15Settings = LottieSettings(currentFrame = 15f.rf)
    val resultFrame15 = evaluateRectangle(rect, frame15Settings, trimPath)
    assertThat(resultFrame15).isNotNull()
  }

  @Test
  fun fillRule_defaultsToNonZero() {
    val fill = Fill(color = StaticColorProperty(value = Color.Black.rc))
    assertThat(fill.fillRule).isEqualTo(FillRule.NonZero)

    val gradientFill =
      GradientFill(
        colors =
          StaticGradientProperty(
            value = GradientValue(numberOfColors = 1, values = listOf(0f, 1f, 0f, 0f))
          ),
        startPoint = StaticPositionProperty(value = listOf(0f, 0f)),
        endPoint = StaticPositionProperty(value = listOf(100f, 0f)),
      )
    assertThat(gradientFill.fillRule).isEqualTo(FillRule.NonZero)
  }

  @Test
  fun fillRule_supportsEvenOdd() {
    val fill = Fill(color = StaticColorProperty(value = Color.Red.rc), fillRule = FillRule.EvenOdd)
    assertThat(fill.fillRule).isEqualTo(FillRule.EvenOdd)

    val gradientFill =
      GradientFill(
        colors =
          StaticGradientProperty(
            value = GradientValue(numberOfColors = 1, values = listOf(0f, 1f, 0f, 0f))
          ),
        startPoint = StaticPositionProperty(value = listOf(0f, 0f)),
        endPoint = StaticPositionProperty(value = listOf(100f, 0f)),
        fillRule = FillRule.EvenOdd,
      )
    assertThat(gradientFill.fillRule).isEqualTo(FillRule.EvenOdd)
  }

  @Test
  fun remoteFill_and_remoteGradientFill_holdFillRule() {
    val remoteFill = RemoteFill(fillColor = Color.Red.rc, fillRule = FillRule.EvenOdd)
    assertThat(remoteFill.fillRule).isEqualTo(FillRule.EvenOdd)

    val gradientValue =
      RemoteGradientValue(
        numberOfColors = 2,
        values = listOf(0f.rf, 1f.rf, 0f.rf, 0f.rf, 1f.rf, 0f.rf, 0f.rf, 1f.rf),
      )
    val remoteGradientFill =
      RemoteGradientFill(
        gradient = gradientValue,
        startPoint = Point(0f.rf, 0f.rf),
        endPoint = Point(100f.rf, 0f.rf),
        gradientType = GradientType.Linear,
        opacity = 100f.rf,
        fillRule = FillRule.EvenOdd,
      )
    assertThat(remoteGradientFill.fillRule).isEqualTo(FillRule.EvenOdd)
  }

  @Test
  fun remoteLottiePath_and_remoteCompiledPath_holdFillRule() {
    val lottiePath = RemoteLottiePath(path = emptyList(), fillRule = FillRule.EvenOdd)
    assertThat(lottiePath.fillRule).isEqualTo(FillRule.EvenOdd)

    val compiledPath =
      RemoteCompiledPath(
        path = androidx.compose.remote.creation.RemotePath(),
        fillRule = FillRule.EvenOdd,
      )
    assertThat(compiledPath.fillRule).isEqualTo(FillRule.EvenOdd)
  }

  @Test
  fun remoteShape_withFillRule_updatesFillRuleCorrectly() {
    val lottiePath = RemoteLottiePath(path = emptyList(), fillRule = FillRule.NonZero)
    val updatedLottiePath = lottiePath.withFillRule(FillRule.EvenOdd)
    assertThat((updatedLottiePath as RemoteLottiePath).fillRule).isEqualTo(FillRule.EvenOdd)

    val compiledPath =
      RemoteCompiledPath(
        path = androidx.compose.remote.creation.RemotePath(),
        fillRule = FillRule.NonZero,
      )
    val updatedCompiledPath = compiledPath.withFillRule(FillRule.EvenOdd)
    assertThat((updatedCompiledPath as RemoteCompiledPath).fillRule).isEqualTo(FillRule.EvenOdd)

    val group =
      com.google.android.horologist.remotecompose.lottie.renderer.RemoteGroup(
        childShapes =
          listOf(
            com.google.android.horologist.remotecompose.lottie.renderer.StyledShapes(
              listOf(lottiePath),
              com.google.android.horologist.remotecompose.lottie.renderer.NoopStyle(),
            )
          ),
        animationSettings = settings,
        transform = null,
      )
    val updatedGroup = group.withFillRule(FillRule.EvenOdd)
    val updatedInnerShape = updatedGroup.childShapes.first().shapes.first()
    assertThat((updatedInnerShape as RemoteLottiePath).fillRule).isEqualTo(FillRule.EvenOdd)
  }

  @Test
  fun gatherShapes_withEvenOddFill_propagatesFillRuleToGeometries() {
    val rect =
      Rectangle(
        position = StaticPositionProperty(value = listOf(0f, 0f)),
        size = StaticVectorProperty(value = listOf(100f, 100f)),
      )
    val fill = Fill(color = StaticColorProperty(value = Color.Red.rc), fillRule = FillRule.EvenOdd)

    val styledShapes = gatherShapesForTest(listOf(rect, fill), settings)
    assertThat(styledShapes).isNotEmpty()

    val shapeGroup = styledShapes.first()
    assertThat(shapeGroup.shapes).isNotEmpty()
    val evaluatedShape = shapeGroup.shapes.first()
    assertThat((evaluatedShape as RemoteLottiePath).fillRule).isEqualTo(FillRule.EvenOdd)
  }

  @Test
  fun gatherShapes_withEvenOddGradientFill_propagatesFillRuleToGeometries() {
    val rect =
      Rectangle(
        position = StaticPositionProperty(value = listOf(0f, 0f)),
        size = StaticVectorProperty(value = listOf(100f, 100f)),
      )
    val gradientFill =
      GradientFill(
        colors =
          StaticGradientProperty(
            value = GradientValue(numberOfColors = 1, values = listOf(0f, 1f, 0f, 0f))
          ),
        startPoint = StaticPositionProperty(value = listOf(0f, 0f)),
        endPoint = StaticPositionProperty(value = listOf(100f, 0f)),
        fillRule = FillRule.EvenOdd,
      )

    val styledShapes = gatherShapesForTest(listOf(rect, gradientFill), settings)
    assertThat(styledShapes).isNotEmpty()

    val shapeGroup = styledShapes.first()
    assertThat(shapeGroup.shapes).isNotEmpty()
    val evaluatedShape = shapeGroup.shapes.first()
    assertThat((evaluatedShape as RemoteLottiePath).fillRule).isEqualTo(FillRule.EvenOdd)
  }

  @Test
  fun remoteShape_drawing_withEvenOdd_maintainsFillRuleConfiguration() {
    val evenOddLottiePath = RemoteLottiePath(path = emptyList(), fillRule = FillRule.EvenOdd)
    val nonZeroLottiePath = RemoteLottiePath(path = emptyList(), fillRule = FillRule.NonZero)

    assertThat(evenOddLottiePath.fillRule).isEqualTo(FillRule.EvenOdd)
    assertThat(nonZeroLottiePath.fillRule).isEqualTo(FillRule.NonZero)

    val evenOddCompiledPath =
      RemoteCompiledPath(
        path = androidx.compose.remote.creation.RemotePath(),
        fillRule = FillRule.EvenOdd,
      )
    val nonZeroCompiledPath =
      RemoteCompiledPath(
        path = androidx.compose.remote.creation.RemotePath(),
        fillRule = FillRule.NonZero,
      )

    assertThat(evenOddCompiledPath.fillRule).isEqualTo(FillRule.EvenOdd)
    assertThat(nonZeroCompiledPath.fillRule).isEqualTo(FillRule.NonZero)
  }
}
