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
import com.google.android.horologist.remotecompose.lottie.renderer.computeInverseScale
import com.google.common.truth.Truth.assertThat
import org.junit.Test

@SuppressLint("RestrictedApi")
class TransformTest {

  @Test
  fun computeInverseScale_standardValues() {
    val scale200 = 2f.rf
    val inv200 = computeInverseScale(scale200)
    assertThat(inv200.constantValueOrNull).isWithin(0.001f).of(0.5f)

    val scale50 = 0.5f.rf
    val inv50 = computeInverseScale(scale50)
    assertThat(inv50.constantValueOrNull).isWithin(0.001f).of(2.0f)

    val scaleNeg = -2f.rf
    val invNeg = computeInverseScale(scaleNeg)
    assertThat(invNeg.constantValueOrNull).isWithin(0.001f).of(-0.5f)
  }

  @Test
  fun computeInverseScale_zeroAndNearZeroGuarded() {
    // When scale is exactly 0, division by zero should be avoided and fallback to 1f
    val scaleZero = 0f.rf
    val invZero = computeInverseScale(scaleZero)
    assertThat(invZero.constantValueOrNull).isWithin(0.001f).of(1f)

    // When scale is near zero (< 0.0001f), guard should also prevent singularity
    val scaleNearZero = 0.00001f.rf
    val invNearZero = computeInverseScale(scaleNearZero)
    assertThat(invNearZero.constantValueOrNull).isWithin(0.001f).of(1f)

    val scaleNegativeNearZero = -0.00005f.rf
    val invNegNearZero = computeInverseScale(scaleNegativeNearZero)
    assertThat(invNegNearZero.constantValueOrNull).isWithin(0.001f).of(1f)
  }
}
