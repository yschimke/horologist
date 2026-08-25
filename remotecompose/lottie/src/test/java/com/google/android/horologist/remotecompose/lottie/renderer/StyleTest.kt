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

package com.google.android.horologist.remotecompose.lottie.renderer

import android.annotation.SuppressLint
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.ui.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.horologist.remotecompose.lottie.LottieSettings
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.Rectangle
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.Fill
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.FillRule
import com.google.android.horologist.remotecompose.lottie.format.properties.AnimatedScalarProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.ScalarPropertyKeyframe
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticColorProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticPositionProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticScalarProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticVectorProperty
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SuppressLint("RestrictedApi")
@RunWith(AndroidJUnit4::class)
class StyleTest {

  @Test
  fun remoteFill_withDefaultOpacity_multipliesByInheritedOpacity() {
    val fill = RemoteFill(fillColor = Color.Red.rc)
    val paint = fill.getPaint(inheritedOpacity = 0.5f.rf)
    val alpha = paint.color.alpha.constantValueOrNull
    assertThat(alpha).isNotNull()
    assertThat(alpha!!).isWithin(0.01f).of(0.5f)
  }

  @Test
  fun remoteFill_withCustomOpacity_compoundsAlphaWithInheritedOpacity() {
    val fill = RemoteFill(fillColor = Color.Blue.rc, opacity = 50f.rf)
    val paint = fill.getPaint(inheritedOpacity = 0.8f.rf)
    val alpha = paint.color.alpha.constantValueOrNull
    assertThat(alpha).isNotNull()
    assertThat(alpha!!).isWithin(0.01f).of(1f * 0.5f * 0.8f) // 0.40f
  }

  @Test
  fun remoteFill_withAlphaColorAndCustomOpacity_compoundsAllAlphas() {
    val semiTransparentColor = Color(1f, 0f, 0f, 0.6f).rc
    val fill = RemoteFill(fillColor = semiTransparentColor, opacity = 50f.rf)
    val paint = fill.getPaint(inheritedOpacity = 0.5f.rf)
    val alpha = paint.color.alpha.constantValueOrNull
    assertThat(alpha).isNotNull()
    assertThat(alpha!!).isWithin(0.01f).of(0.6f * 0.5f * 0.5f) // 0.15f
  }

  @Test
  fun gatherShapes_withStaticFillOpacity_createsRemoteFillWithEvaluatedOpacity() {
    val rect =
      Rectangle(
        position = StaticPositionProperty(value = listOf(0f, 0f)),
        size = StaticVectorProperty(value = listOf(100f, 100f)),
      )
    val fill =
      Fill(
        color = StaticColorProperty(value = Color.Green.rc),
        opacity = StaticScalarProperty(value = 60f),
        fillRule = FillRule.NonZero,
      )

    val settings = LottieSettings(currentFrame = 0f.rf)
    val styledShapes = gatherShapesForTest(listOf(rect, fill), settings)

    assertThat(styledShapes).hasSize(1)
    val remoteFill = styledShapes[0].style as? RemoteFill
    assertThat(remoteFill).isNotNull()
    assertThat(remoteFill!!.opacity.constantValueOrNull).isEqualTo(60f)

    val paint = remoteFill.getPaint(inheritedOpacity = 1f.rf)
    assertThat(paint.color.alpha.constantValueOrNull).isWithin(1e-4f).of(0.6f)
  }

  @Test
  fun gatherShapes_withAnimatedFillOpacity_evaluatesOpacityAtCurrentFrame() {
    val rect =
      Rectangle(
        position = StaticPositionProperty(value = listOf(0f, 0f)),
        size = StaticVectorProperty(value = listOf(100f, 100f)),
      )
    val keyframes =
      listOf(
        ScalarPropertyKeyframe(frame = 0f, value = 0f),
        ScalarPropertyKeyframe(frame = 30f, value = 100f),
      )
    val fill =
      Fill(
        color = StaticColorProperty(value = Color.Yellow.rc),
        opacity = AnimatedScalarProperty(keyframes = keyframes),
      )

    val frame0Settings = LottieSettings(currentFrame = 0f.rf)
    val styledShapes0 = gatherShapesForTest(listOf(rect, fill), frame0Settings)
    val remoteFill0 = styledShapes0[0].style as RemoteFill
    assertThat(remoteFill0.opacity.constantValueOrNull).isEqualTo(0f)

    val frame30Settings = LottieSettings(currentFrame = 30f.rf)
    val styledShapes30 = gatherShapesForTest(listOf(rect, fill), frame30Settings)
    val remoteFill30 = styledShapes30[0].style as RemoteFill
    assertThat(remoteFill30.opacity.constantValueOrNull).isEqualTo(100f)
  }
}
