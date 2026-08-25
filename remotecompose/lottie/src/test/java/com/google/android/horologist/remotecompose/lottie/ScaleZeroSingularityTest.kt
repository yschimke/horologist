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
import com.google.android.horologist.remotecompose.lottie.renderer.clampScale
import com.google.android.horologist.remotecompose.lottie.renderer.computeInverseScale
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SuppressLint("RestrictedApi")
@RunWith(AndroidJUnit4::class)
class ScaleZeroSingularityTest {

  @Test
  fun clampScale_withPositiveNormalScale_preservesValue() {
    val scale = clampScale(2.5f.rf)
    assertThat(scale.constantValueOrNull).isWithin(0.0001f).of(2.5f)
  }

  @Test
  fun clampScale_withPositiveZeroScale_clampsToEpsilon() {
    val scale = clampScale(0f.rf)
    assertThat(scale.constantValueOrNull).isWithin(0.00001f).of(0.0001f)
  }

  @Test
  fun clampScale_withPositiveNearZeroScale_clampsToEpsilon() {
    val scale = clampScale(0.00001f.rf)
    assertThat(scale.constantValueOrNull).isWithin(0.00001f).of(0.0001f)
  }

  @Test
  fun clampScale_withNegativeNormalScale_preservesNegativeValue() {
    val scale = clampScale((-2.5f).rf)
    assertThat(scale.constantValueOrNull).isWithin(0.0001f).of(-2.5f)
  }

  @Test
  fun clampScale_withNegativeNearZeroScale_clampsToNegativeEpsilon() {
    val scale = clampScale((-0.00001f).rf)
    assertThat(scale.constantValueOrNull).isWithin(0.00001f).of(-0.0001f)
  }

  @Test
  fun forwardAndInverseScale_whenMultiplied_yieldsIdentityAcrossSingularities() {
    val testValues = listOf(2.0f, 0.5f, 0.0f, 0.00001f, -0.00001f, -0.5f, -2.0f)
    for (value in testValues) {
      val fwd = clampScale(value.rf)
      val inv = computeInverseScale(fwd)
      val product = (fwd * inv).constantValueOrNull
      assertThat(product).isNotNull()
      assertThat(product!!).isWithin(0.001f).of(1.0f)
    }
  }
}
