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
import androidx.compose.ui.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.horologist.remotecompose.lottie.format.Animation
import com.google.android.horologist.remotecompose.lottie.format.layer.LayerType
import com.google.android.horologist.remotecompose.lottie.format.layer.TextDocument
import com.google.android.horologist.remotecompose.lottie.format.layer.TextDocumentKeyframe
import com.google.android.horologist.remotecompose.lottie.format.layer.TextDocumentProperty
import com.google.android.horologist.remotecompose.lottie.format.layer.TextJustify
import com.google.android.horologist.remotecompose.lottie.format.layer.TextLayer
import com.google.android.horologist.remotecompose.lottie.renderer.layers.evaluateTextDocument
import com.google.android.horologist.remotecompose.lottie.renderer.layers.parseColorFromList
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SuppressLint("RestrictedApi")
@RunWith(AndroidJUnit4::class)
class TextLayerTest {

  @Test
  fun decodeAnimation_withTextLayer_parsesTextDocumentAndProperties() {
    val json =
      """
      {
        "v": "5.9.6",
        "fr": 30.0,
        "ip": 0.0,
        "op": 60.0,
        "w": 400,
        "h": 400,
        "layers": [
          {
            "ty": 5,
            "nm": "TitleText",
            "ind": 1,
            "ip": 0.0,
            "op": 60.0,
            "t": {
              "d": {
                "k": [
                  {
                    "s": {
                      "t": "Hello Horologist",
                      "s": 32.0,
                      "f": "Roboto-Bold",
                      "j": 2,
                      "tr": 10.0,
                      "lh": 40.0,
                      "ls": 0.0,
                      "fc": [1.0, 0.5, 0.0, 1.0],
                      "sc": [0.0, 0.0, 0.0, 1.0],
                      "sw": 2.0,
                      "of": true
                    },
                    "t": 0.0
                  }
                ]
              },
              "m": {
                "g": 1
              }
            }
          }
        ]
      }
      """
        .trimIndent()

    val animation = Animation.decodeFromString(json)
    assertThat(animation.layers).hasSize(1)

    val layer = animation.layers[0]
    assertThat(layer).isInstanceOf(TextLayer::class.java)
    val textLayer = layer as TextLayer
    assertThat(textLayer.name).isEqualTo("TitleText")
    assertThat(textLayer.type).isEqualTo(LayerType.Text)

    val textData = textLayer.text
    assertThat(textData).isNotNull()
    val docProp = textData?.document
    assertThat(docProp).isNotNull()
    assertThat(docProp?.keyframes).hasSize(1)

    val kf = docProp?.keyframes?.get(0)
    assertThat(kf?.time).isEqualTo(0f)
    val doc = kf?.start
    assertThat(doc).isNotNull()
    assertThat(doc?.text).isEqualTo("Hello Horologist")
    assertThat(doc?.fontSize).isEqualTo(32f)
    assertThat(doc?.fontName).isEqualTo("Roboto-Bold")
    assertThat(doc?.justification).isEqualTo(TextJustify.Center)
    assertThat(doc?.tracking).isEqualTo(10f)
    assertThat(doc?.lineHeight).isEqualTo(40f)
    assertThat(doc?.fillColor).containsExactly(1.0f, 0.5f, 0.0f, 1.0f).inOrder()
    assertThat(doc?.strokeColor).containsExactly(0.0f, 0.0f, 0.0f, 1.0f).inOrder()
    assertThat(doc?.strokeWidth).isEqualTo(2f)
    assertThat(doc?.strokeOverFill).isTrue()
  }

  @Test
  fun decodeAnimation_withFontsAndChars_parsesGlyphs() {
    val json =
      """
      {
        "v": "5.9.6",
        "fr": 30.0,
        "ip": 0.0,
        "op": 60.0,
        "w": 300,
        "h": 300,
        "fonts": {
          "list": [
            {
              "fName": "Roboto-Regular",
              "fFamily": "Roboto",
              "fStyle": "Regular",
              "ascent": 75.0
            }
          ]
        },
        "chars": [
          {
            "ch": "A",
            "fFamily": "Roboto",
            "style": "Regular",
            "size": 100.0,
            "w": 65.0,
            "data": {
              "shapes": []
            }
          }
        ],
        "layers": []
      }
      """
        .trimIndent()

    val animation = Animation.decodeFromString(json)
    assertThat(animation.fonts).isNotNull()
    assertThat(animation.fonts?.list).hasSize(1)
    assertThat(animation.fonts?.list?.get(0)?.name).isEqualTo("Roboto-Regular")
    assertThat(animation.fonts?.list?.get(0)?.family).isEqualTo("Roboto")

    assertThat(animation.chars).hasSize(1)
    val charA = animation.chars[0]
    assertThat(charA.character).isEqualTo("A")
    assertThat(charA.family).isEqualTo("Roboto")
    assertThat(charA.width).isEqualTo(65f)
    assertThat(charA.size).isEqualTo(100f)
  }

  @Test
  fun evaluateTextDocument_singleKeyframe_returnsDocument() {
    val doc = TextDocument(text = "Static Text", fontSize = 24f)
    val prop =
      TextDocumentProperty(keyframes = listOf(TextDocumentKeyframe(start = doc, time = 0f)))

    val result = evaluateTextDocument(prop, 15f)
    assertThat(result).isEqualTo(doc)
  }

  @Test
  fun evaluateTextDocument_multiKeyframe_returnsActiveKeyframeByTime() {
    val doc1 = TextDocument(text = "First Text", fontSize = 20f)
    val doc2 = TextDocument(text = "Second Text", fontSize = 30f)
    val prop =
      TextDocumentProperty(
        keyframes =
          listOf(
            TextDocumentKeyframe(start = doc1, time = 0f),
            TextDocumentKeyframe(start = doc2, time = 30f),
          )
      )

    val resultEarly = evaluateTextDocument(prop, 10f)
    assertThat(resultEarly?.text).isEqualTo("First Text")
    assertThat(resultEarly?.fontSize).isEqualTo(20f)

    val resultLate = evaluateTextDocument(prop, 45f)
    assertThat(resultLate?.text).isEqualTo("Second Text")
    assertThat(resultLate?.fontSize).isEqualTo(30f)
  }

  @Test
  fun textJustify_fromValue_mapsAllEnumVariants() {
    assertThat(TextJustify.fromValueOrNull(0)).isEqualTo(TextJustify.Left)
    assertThat(TextJustify.fromValueOrNull(1)).isEqualTo(TextJustify.Right)
    assertThat(TextJustify.fromValueOrNull(2)).isEqualTo(TextJustify.Center)
    assertThat(TextJustify.fromValueOrNull(3)).isEqualTo(TextJustify.JustifyWithLastLineLeft)
    assertThat(TextJustify.fromValueOrNull(4)).isEqualTo(TextJustify.JustifyWithLastLineRight)
    assertThat(TextJustify.fromValueOrNull(5)).isEqualTo(TextJustify.JustifyWithLastLineCenter)
    assertThat(TextJustify.fromValueOrNull(6)).isEqualTo(TextJustify.JustifyWithLastLineFull)
    assertThat(TextJustify.fromValueOrNull(99)).isNull()
  }

  @Test
  fun parseColorFromList_handlesRgbAndRgba() {
    val rgb = parseColorFromList(listOf(1.0f, 0.0f, 0.0f))
    assertThat(rgb).isEqualTo(Color(1f, 0f, 0f, 1f))

    val rgba = parseColorFromList(listOf(0.0f, 1.0f, 0.0f, 0.5f))
    assertThat(rgba).isEqualTo(Color(0f, 1f, 0f, 0.5f))

    val empty = parseColorFromList(emptyList())
    assertThat(empty).isEqualTo(Color.Black)
  }
}
