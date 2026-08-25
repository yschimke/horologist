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
import com.google.android.horologist.remotecompose.lottie.format.Animation
import com.google.android.horologist.remotecompose.lottie.format.layer.ShapeLayer
import com.google.android.horologist.remotecompose.lottie.format.mask.MaskMode
import com.google.android.horologist.remotecompose.lottie.renderer.buildRemotePathFromBezier
import com.google.android.horologist.remotecompose.lottie.renderer.properties.RemoteBezierValue
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SuppressLint("RestrictedApi")
@RunWith(AndroidJUnit4::class)
class MaskCompositingTest {

  private fun createRectRemoteBezier(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
  ): RemoteBezierValue {
    return RemoteBezierValue(
      closed = true,
      vertices =
        listOf(
          listOf(x.rf, y.rf),
          listOf((x + width).rf, y.rf),
          listOf((x + width).rf, (y + height).rf),
          listOf(x.rf, (y + height).rf),
        ),
      inTangents =
        listOf(
          listOf(0f.rf, 0f.rf),
          listOf(0f.rf, 0f.rf),
          listOf(0f.rf, 0f.rf),
          listOf(0f.rf, 0f.rf),
        ),
      outTangents =
        listOf(
          listOf(0f.rf, 0f.rf),
          listOf(0f.rf, 0f.rf),
          listOf(0f.rf, 0f.rf),
          listOf(0f.rf, 0f.rf),
        ),
    )
  }

  @Test
  fun buildRemotePathFromBezier_withMultipleSubpaths_combinesAllSubpaths() {
    val subpath1 = createRectRemoteBezier(0f, 0f, 50f, 50f)
    val subpath2 = createRectRemoteBezier(60f, 60f, 50f, 50f)

    val combinedPath = buildRemotePathFromBezier(listOf(subpath1, subpath2))
    assertThat(combinedPath).isNotNull()
  }

  @Test
  fun layerMasks_decodingMultipleAddAndSubtractModes() {
    val json =
      """
      {
        "v": "5.7.0",
        "fr": 30,
        "ip": 0,
        "op": 30,
        "w": 100,
        "h": 100,
        "layers": [
          {
            "ty": 4,
            "nm": "MultiMaskLayer",
            "ind": 1,
            "ip": 0,
            "op": 30,
            "ks": {
              "p": { "a": 0, "k": [0.0, 0.0, 0.0] },
              "a": { "a": 0, "k": [0.0, 0.0, 0.0] },
              "s": { "a": 0, "k": [100.0, 100.0, 100.0] },
              "r": { "a": 0, "k": 0.0 },
              "o": { "a": 0, "k": 100.0 }
            },
            "masksProperties": [
              {
                "nm": "AddMask1",
                "mode": "a",
                "inv": false,
                "pt": {
                  "a": 0,
                  "k": {
                    "c": true,
                    "v": [[0.0, 0.0], [40.0, 0.0], [40.0, 40.0], [0.0, 40.0]],
                    "i": [[0.0, 0.0], [0.0, 0.0], [0.0, 0.0], [0.0, 0.0]],
                    "o": [[0.0, 0.0], [0.0, 0.0], [0.0, 0.0], [0.0, 0.0]]
                  }
                },
                "o": { "a": 0, "k": 100.0 }
              },
              {
                "nm": "AddMask2",
                "mode": "a",
                "inv": false,
                "pt": {
                  "a": 0,
                  "k": {
                    "c": true,
                    "v": [[50.0, 50.0], [90.0, 50.0], [90.0, 90.0], [50.0, 90.0]],
                    "i": [[0.0, 0.0], [0.0, 0.0], [0.0, 0.0], [0.0, 0.0]],
                    "o": [[0.0, 0.0], [0.0, 0.0], [0.0, 0.0], [0.0, 0.0]]
                  }
                },
                "o": { "a": 0, "k": 100.0 }
              },
              {
                "nm": "SubtractMask",
                "mode": "s",
                "inv": false,
                "pt": {
                  "a": 0,
                  "k": {
                    "c": true,
                    "v": [[10.0, 10.0], [20.0, 10.0], [20.0, 20.0], [10.0, 20.0]],
                    "i": [[0.0, 0.0], [0.0, 0.0], [0.0, 0.0], [0.0, 0.0]],
                    "o": [[0.0, 0.0], [0.0, 0.0], [0.0, 0.0], [0.0, 0.0]]
                  }
                },
                "o": { "a": 0, "k": 100.0 }
              }
            ]
          }
        ]
      }
      """
        .trimIndent()

    val animation = Animation.decodeFromString(json)
    val layer = animation.layers[0] as ShapeLayer
    assertThat(layer.masksProperties).hasSize(3)
    assertThat(layer.masksProperties[0].mode).isEqualTo(MaskMode.Add)
    assertThat(layer.masksProperties[1].mode).isEqualTo(MaskMode.Add)
    assertThat(layer.masksProperties[2].mode).isEqualTo(MaskMode.Subtract)
  }
}
