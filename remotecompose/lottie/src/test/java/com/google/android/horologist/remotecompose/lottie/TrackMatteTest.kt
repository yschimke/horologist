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
import androidx.compose.ui.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.horologist.remotecompose.lottie.format.Animation
import com.google.android.horologist.remotecompose.lottie.format.asset.PrecompAsset
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.Rectangle
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.grouping.Group
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.Fill
import com.google.android.horologist.remotecompose.lottie.format.layer.MatteMode
import com.google.android.horologist.remotecompose.lottie.format.layer.ShapeLayer
import com.google.android.horologist.remotecompose.lottie.format.layer.SolidColorLayer
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticColorProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticPositionProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticScalarProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticVectorProperty
import com.google.android.horologist.remotecompose.lottie.renderer.layers.MatteContext
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SuppressLint("RestrictedApi")
@RunWith(AndroidJUnit4::class)
class TrackMatteTest {

  @Test
  fun hiddenShapeLayer_functionsAsMatteContextSource() {
    val matteShapeLayer =
      ShapeLayer(
        index = 1,
        name = "MatteSource",
        hidden = true,
        matteTarget = 1,
        shapes =
          listOf(
            Group(
              shapes =
                listOf(
                  Rectangle(
                    position = StaticPositionProperty(value = listOf(50f, 50f)),
                    size = StaticVectorProperty(value = listOf(40f, 40f)),
                    cornerRadius = StaticScalarProperty(value = 0f),
                  ),
                  Fill(
                    color = StaticColorProperty(value = Color.White.rc),
                    opacity = StaticScalarProperty(value = 100f),
                  ),
                )
            )
          ),
      )

    val targetLayer =
      ShapeLayer(
        index = 2,
        name = "TargetLayer",
        matteMode = MatteMode.Alpha,
        shapes =
          listOf(
            Group(
              shapes =
                listOf(
                  Rectangle(
                    position = StaticPositionProperty(value = listOf(50f, 50f)),
                    size = StaticVectorProperty(value = listOf(100f, 100f)),
                    cornerRadius = StaticScalarProperty(value = 0f),
                  ),
                  Fill(
                    color = StaticColorProperty(value = Color.Red.rc),
                    opacity = StaticScalarProperty(value = 100f),
                  ),
                )
            )
          ),
      )

    val matteContext = MatteContext(matteLayer = matteShapeLayer, matteTransforms = emptyList())
    assertThat(matteContext.matteLayer.hidden).isTrue()
    assertThat(matteContext.matteLayer).isInstanceOf(ShapeLayer::class.java)
    val shapes = (matteContext.matteLayer as ShapeLayer).shapes
    assertThat(shapes).isNotEmpty()
  }

  @Test
  fun hiddenSolidColorLayer_functionsAsMatteContextSource() {
    val matteSolidLayer =
      SolidColorLayer(
        index = 1,
        name = "SolidMatteSource",
        hidden = true,
        matteTarget = 1,
        solidColor = "#FFFFFF",
        solidWidth = 60f,
        solidHeight = 60f,
      )

    val matteContext = MatteContext(matteLayer = matteSolidLayer, matteTransforms = emptyList())
    assertThat(matteContext.matteLayer.hidden).isTrue()
    assertThat(matteContext.matteLayer).isInstanceOf(SolidColorLayer::class.java)
    val solid = matteContext.matteLayer as SolidColorLayer
    assertThat(solid.solidWidth).isEqualTo(60f)
    assertThat(solid.solidHeight).isEqualTo(60f)
  }

  @Test
  fun precompLayer_withHiddenMatteSource_deserializesAndResolvesMatteHierarchy() {
    val json =
      """
      {
        "v": "5.7.0",
        "fr": 30,
        "ip": 0,
        "op": 30,
        "w": 100,
        "h": 100,
        "assets": [
          {
            "id": "comp_0",
            "layers": [
              {
                "ty": 4,
                "nm": "HiddenMatteInsidePrecomp",
                "ind": 1,
                "hd": true,
                "ip": 0,
                "op": 30,
                "shapes": [
                  {
                    "ty": "gr",
                    "it": [
                      { "ty": "el", "p": { "a": 0, "k": [0, 0] }, "s": { "a": 0, "k": [30, 30] } },
                      { "ty": "fl", "c": { "a": 0, "k": [1, 1, 1, 1] }, "o": { "a": 0, "k": 100 } },
                      { "ty": "tr", "p": { "a": 0, "k": [0, 0] }, "a": { "a": 0, "k": [0, 0] }, "s": { "a": 0, "k": [100, 100] }, "r": { "a": 0, "k": 0 }, "o": { "a": 0, "k": 100 } }
                    ]
                  }
                ]
              },
              {
                "ty": 4,
                "nm": "TargetInsidePrecomp",
                "ind": 2,
                "tt": 1,
                "ip": 0,
                "op": 30,
                "shapes": [
                  {
                    "ty": "gr",
                    "it": [
                      { "ty": "rc", "p": { "a": 0, "k": [0, 0] }, "s": { "a": 0, "k": [80, 80] }, "r": { "a": 0, "k": 0 } },
                      { "ty": "fl", "c": { "a": 0, "k": [0, 1, 0, 1] }, "o": { "a": 0, "k": 100 } },
                      { "ty": "tr", "p": { "a": 0, "k": [0, 0] }, "a": { "a": 0, "k": [0, 0] }, "s": { "a": 0, "k": [100, 100] }, "r": { "a": 0, "k": 0 }, "o": { "a": 0, "k": 100 } }
                    ]
                  }
                ]
              }
            ]
          }
        ],
        "layers": [
          {
            "ty": 0,
            "nm": "PrecompLayer",
            "refId": "comp_0",
            "ip": 0,
            "op": 30,
            "w": 100,
            "h": 100
          }
        ]
      }
      """
        .trimIndent()

    val animation = Animation.decodeFromString(json)
    assertThat(animation.assets).hasSize(1)
    val precompAsset = animation.assets[0] as PrecompAsset
    assertThat(precompAsset.layers).hasSize(2)
    assertThat(precompAsset.layers[0].hidden).isTrue()
    assertThat(precompAsset.layers[1].matteMode).isEqualTo(MatteMode.Alpha)
  }
}
