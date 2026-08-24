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
import com.google.android.horologist.remotecompose.lottie.format.layer.MatteMode
import com.google.android.horologist.remotecompose.lottie.format.layer.ShapeLayer
import com.google.android.horologist.remotecompose.lottie.renderer.layers.MatteContext
import com.google.android.horologist.remotecompose.lottie.renderer.layers.calculateLocalFrame
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SuppressLint("RestrictedApi")
@RunWith(AndroidJUnit4::class)
class LayerTimingAndTrackMatteTest {

  @Test
  fun calculateLocalFrame_defaultTiming_returnsSameFrame() {
    val currentFrame = 25f.rf
    val localFrame = calculateLocalFrame(currentFrame, startTime = null, timeStretch = null)
    assertThat(localFrame.constantValueOrNull).isWithin(0.001f).of(25f)
  }

  @Test
  fun calculateLocalFrame_withStartTimeOffset_shiftsFrame() {
    val currentFrame = 30f.rf
    val localFrame = calculateLocalFrame(currentFrame, startTime = 10f, timeStretch = null)
    // t_local = 30 - 10 = 20
    assertThat(localFrame.constantValueOrNull).isWithin(0.001f).of(20f)
  }

  @Test
  fun calculateLocalFrame_withTimeStretch_scalesFrameRate() {
    val currentFrame = 40f.rf
    val localFrame = calculateLocalFrame(currentFrame, startTime = 0f, timeStretch = 2f)
    // t_local = (40 - 0) / 2 = 20
    assertThat(localFrame.constantValueOrNull).isWithin(0.001f).of(20f)
  }

  @Test
  fun calculateLocalFrame_withStartTimeAndTimeStretch_calculatesCorrectLocalTime() {
    val currentFrame = 50f.rf
    val localFrame = calculateLocalFrame(currentFrame, startTime = 10f, timeStretch = 2f)
    // t_local = (50 - 10) / 2 = 20
    assertThat(localFrame.constantValueOrNull).isWithin(0.001f).of(20f)
  }

  @Test
  fun matteContext_holdsMatteMode() {
    val matteLayer = ShapeLayer(index = 1, shapes = emptyList())
    val alphaContext = MatteContext(matteLayer, emptyList(), MatteMode.Alpha)
    assertThat(alphaContext.matteMode).isEqualTo(MatteMode.Alpha)

    val invertedContext = MatteContext(matteLayer, emptyList(), MatteMode.InvertedAlpha)
    assertThat(invertedContext.matteMode).isEqualTo(MatteMode.InvertedAlpha)
  }

  @Test
  fun layer_withNegativeTimeStretch_supportsReversedPlayback() {
    val currentFrame = 10f.rf
    val localFrame = calculateLocalFrame(currentFrame, startTime = 50f, timeStretch = -1f)
    // t_local = (10 - 50) / -1 = 40
    assertThat(localFrame.constantValueOrNull).isWithin(0.001f).of(40f)
  }

  @Test
  fun layer_withZeroTimeStretch_guardsAgainstDivisionByZero() {
    val currentFrame = 25f.rf
    val localFrame = calculateLocalFrame(currentFrame, startTime = 0f, timeStretch = 0f)
    // Falls back to safeSr = 1f
    assertThat(localFrame.constantValueOrNull).isWithin(0.001f).of(25f)
  }
}
