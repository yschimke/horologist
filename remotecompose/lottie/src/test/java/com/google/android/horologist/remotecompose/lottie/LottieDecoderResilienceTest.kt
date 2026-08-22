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

import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.ui.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.horologist.remotecompose.lottie.format.AnimatedScalarProperty
import com.google.android.horologist.remotecompose.lottie.format.Animation
import com.google.android.horologist.remotecompose.lottie.format.LottieDecoder
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.grouping.Group
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.Fill
import com.google.android.horologist.remotecompose.lottie.format.layer.NullLayer
import com.google.android.horologist.remotecompose.lottie.format.layer.ShapeLayer
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticColorProperty
import com.google.android.horologist.remotecompose.lottie.format.values.GradientValueSerializer
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LottieDecoderResilienceTest {

  @Test
  fun unknownLayerType_deserializesAsUnknownLayerFallback() {
    val json =
      """
      {
        "v": "5.7.0",
        "fr": 60,
        "ip": 0,
        "op": 60,
        "w": 100,
        "h": 100,
        "layers": [
          { "ty": 999, "nm": "UnsupportedAudioLayer", "ind": 1, "ip": 0, "op": 60 },
          { "ty": 4, "nm": "ValidShapeLayer", "ind": 2, "ip": 0, "op": 60, "shapes": [] }
        ]
      }
      """
        .trimIndent()

    val animation = Animation.decodeFromString(json)

    assertThat(animation.layers).hasSize(2)
    assertThat(animation.layers[0]).isInstanceOf(NullLayer::class.java)
    assertThat(animation.layers[0].name).isEqualTo("UnsupportedAudioLayer")
    assertThat(animation.layers[1]).isInstanceOf(ShapeLayer::class.java)
  }

  @Test
  fun unknownShapeType_deserializesAsUnknownElementFallback() {
    val json =
      """
      {
        "v": "5.7.0",
        "fr": 30,
        "ip": 0,
        "op": 30,
        "w": 50,
        "h": 50,
        "layers": [
          {
            "ty": 4,
            "nm": "ShapeLayer",
            "ip": 0,
            "op": 30,
            "shapes": [
              { "ty": "unknown_shape_type", "nm": "CustomShape" },
              {
                "ty": "fl",
                "nm": "RedFill",
                "c": { "a": 0, "k": [1.0, 0.0, 0.0, 1.0] },
                "o": { "a": 0, "k": 100 }
              }
            ]
          }
        ]
      }
      """
        .trimIndent()

    val animation = Animation.decodeFromString(json)

    val shapeLayer = animation.layers[0] as ShapeLayer
    assertThat(shapeLayer.shapes).hasSize(2)
    assertThat(shapeLayer.shapes[0]).isInstanceOf(Group::class.java)
    assertThat(shapeLayer.shapes[1]).isInstanceOf(Fill::class.java)
  }

  @Test
  fun colorProperty_handlesRgbRgbaAndScaledIntegers() {
    val json =
      """
      {
        "v": "5.7.0",
        "fr": 30,
        "ip": 0,
        "op": 30,
        "w": 50,
        "h": 50,
        "layers": [
          {
            "ty": 4,
            "nm": "ShapeLayer",
            "ip": 0,
            "op": 30,
            "shapes": [
              {
                "ty": "fl",
                "nm": "RgbFill",
                "c": { "a": 0, "k": [1.0, 0.5, 0.0] },
                "o": { "a": 0, "k": 100 }
              },
              {
                "ty": "fl",
                "nm": "ScaledIntFill",
                "c": { "a": 0, "k": [255, 128, 0, 255] },
                "o": { "a": 0, "k": 100 }
              }
            ]
          }
        ]
      }
      """
        .trimIndent()

    val animation = Animation.decodeFromString(json)

    val shapeLayer = animation.layers[0] as ShapeLayer
    val fill1 = shapeLayer.shapes[0] as Fill
    val fill2 = shapeLayer.shapes[1] as Fill

    assertThat((fill1.color as StaticColorProperty).value).isNotNull()
    assertThat((fill2.color as StaticColorProperty).value).isNotNull()
  }

  @Test
  fun colorProperty_parsesSlotId() {
    val json =
      """
      {
        "v": "5.7.0",
        "fr": 30,
        "ip": 0,
        "op": 30,
        "w": 50,
        "h": 50,
        "layers": [
          {
            "ty": 4,
            "nm": "ShapeLayer",
            "ip": 0,
            "op": 30,
            "shapes": [
              {
                "ty": "fl",
                "nm": "SidFill",
                "c": { "a": 0, "sid": "color.primary", "k": [1.0, 0.0, 0.0, 1.0] },
                "o": { "a": 0, "k": 100 }
              },
              {
                "ty": "fl",
                "nm": "DefaultFill",
                "c": { "a": 0, "k": [0.0, 1.0, 0.0, 1.0] },
                "o": { "a": 0, "k": 100 }
              }
            ]
          }
        ]
      }
      """
        .trimIndent()

    val animation = Animation.decodeFromString(json)

    val shapeLayer = animation.layers[0] as ShapeLayer
    val fill1 = shapeLayer.shapes[0] as Fill
    val fill2 = shapeLayer.shapes[1] as Fill

    assertThat(fill1.color.slotId).isEqualTo("color.primary")
    assertThat(fill2.color.slotId).isNull()

    val slotMap = SlotMap(mapOf("color.primary" to Color(0xFF00FF00.toInt()).rc))
    assertThat(slotMap.colorSlots["color.primary"]).isNotNull()
    assertThat(slotMap.colorSlots["unknown"]).isNull()
  }

  @Test
  fun colorProperty_parsesHexColorStrings() {
    val json =
      """
      {
        "v": "5.7.0",
        "fr": 30,
        "ip": 0,
        "op": 30,
        "w": 50,
        "h": 50,
        "layers": [
          {
            "ty": 4,
            "nm": "ShapeLayer",
            "shapes": [
              {
                "ty": "fl",
                "nm": "Hex6Fill",
                "c": { "k": "#FF8000" }
              },
              {
                "ty": "fl",
                "nm": "Hex8Fill",
                "c": { "k": "#80FF8000" }
              },
              {
                "ty": "fl",
                "nm": "Hex3Fill",
                "c": { "k": "#F80" }
              },
              {
                "ty": "fl",
                "nm": "Hex4Fill",
                "c": { "k": "#8F80" }
              }
            ]
          }
        ]
      }
      """
        .trimIndent()

    val animation = Animation.decodeFromString(json)

    val shapeLayer = animation.layers[0] as ShapeLayer
    assertThat(shapeLayer.shapes).hasSize(4)
    val fill1 = shapeLayer.shapes[0] as Fill
    val fill2 = shapeLayer.shapes[1] as Fill
    val fill3 = shapeLayer.shapes[2] as Fill
    val fill4 = shapeLayer.shapes[3] as Fill

    assertThat(fill1.color.value).isNotNull()
    assertThat(fill2.color.value).isNotNull()
    assertThat(fill3.color.value).isNotNull()
    assertThat(fill4.color.value).isNotNull()
  }

  @Test
  fun colorProperty_4ComponentFloat_preservesAlpha() {
    val json =
      """
      {
        "v": "5.7.0",
        "fr": 30,
        "ip": 0,
        "op": 30,
        "w": 50,
        "h": 50,
        "layers": [
          {
            "ty": 4,
            "nm": "ShapeLayer",
            "shapes": [
              {
                "ty": "fl",
                "nm": "AlphaFill",
                "c": { "k": [1.0, 0.0, 0.0, 0.5] }
              }
            ]
          }
        ]
      }
      """
        .trimIndent()

    val animation = Animation.decodeFromString(json)

    val shapeLayer = animation.layers[0] as Layer.ShapeLayer
    val fill = shapeLayer.shapes[0] as GraphicElement.Fill
    assertThat(fill.color.value).isNotNull()
  }

  @Test
  fun colorProperty_animatedColor_parsesKeyframes() {
    val json =
      """
      {
        "v": "5.7.0",
        "fr": 30,
        "ip": 0,
        "op": 30,
        "w": 50,
        "h": 50,
        "layers": [
          {
            "ty": 4,
            "nm": "ShapeLayer",
            "shapes": [
              {
                "ty": "fl",
                "nm": "AnimatedColorFill",
                "c": {
                  "a": 1,
                  "k": [
                    {
                      "t": 0,
                      "s": [1.0, 0.0, 0.0, 1.0],
                      "i": { "x": 0.5, "y": 1.0 },
                      "o": { "x": 0.5, "y": 0.0 }
                    },
                    {
                      "t": 30,
                      "s": [0.0, 0.0, 1.0, 1.0]
                    }
                  ]
                }
              }
            ]
          }
        ]
      }
      """
        .trimIndent()

    val animation = Animation.decodeFromString(json)

    val shapeLayer = animation.layers[0] as ShapeLayer
    val fill = shapeLayer.shapes[0] as Fill
    assertThat(fill.color.animated).isTrue()
  }

  @Test
  fun bezierProperty_handlesBooleanAndIntClosedFlag() {
    val json =
      """
      {
        "v": "5.7.0",
        "fr": 30,
        "ip": 0,
        "op": 30,
        "w": 50,
        "h": 50,
        "layers": [
          {
            "ty": 4,
            "nm": "ShapeLayer",
            "shapes": [
              {
                "ty": "sh",
                "nm": "IntClosedPath",
                "ks": {
                  "a": 0,
                  "k": {
                    "c": 1,
                    "i": [[0.0, 0.0], [0.0, 0.0]],
                    "o": [[0.0, 0.0], [0.0, 0.0]],
                    "v": [[10.0, 10.0], [20.0, 20.0]]
                  }
                }
              },
              {
                "ty": "sh",
                "nm": "BoolClosedPath",
                "ks": {
                  "a": 0,
                  "k": {
                    "c": false,
                    "i": [[0.0, 0.0], [0.0, 0.0]],
                    "o": [[0.0, 0.0], [0.0, 0.0]],
                    "v": [[10.0, 10.0], [20.0, 20.0]]
                  }
                }
              }
            ]
          }
        ]
      }
      """
        .trimIndent()

    val animation = Animation.decodeFromString(json)

    val shapeLayer = animation.layers[0] as ShapeLayer
    val path1 = shapeLayer.shapes[0] as Path
    val path2 = shapeLayer.shapes[1] as Path

    assertThat(path1.shape).isNotNull()
    assertThat(path2.shape).isNotNull()
  }

  @Test
  fun bezierProperty_handlesSingleObjectAndArrayKeyframeValue() {
    val json =
      """
      {
        "v": "5.7.0",
        "fr": 30,
        "ip": 0,
        "op": 30,
        "w": 50,
        "h": 50,
        "layers": [
          {
            "ty": 4,
            "nm": "ShapeLayer",
            "shapes": [
              {
                "ty": "sh",
                "nm": "ArrayKeyframePath",
                "ks": {
                  "a": 1,
                  "k": [
                    {
                      "t": 0,
                      "s": [
                        {
                          "c": true,
                          "i": [[0.0, 0.0]],
                          "o": [[0.0, 0.0]],
                          "v": [[0.0, 0.0]]
                        }
                      ]
                    },
                    {
                      "t": 30,
                      "s": [
                        {
                          "c": true,
                          "i": [[0.0, 0.0]],
                          "o": [[0.0, 0.0]],
                          "v": [[10.0, 10.0]]
                        }
                      ]
                    }
                  ]
                }
              },
              {
                "ty": "sh",
                "nm": "SingleObjectKeyframePath",
                "ks": {
                  "a": 1,
                  "k": [
                    {
                      "t": 0,
                      "s": {
                        "c": 0,
                        "i": [[0.0, 0.0]],
                        "o": [[0.0, 0.0]],
                        "v": [[0.0, 0.0]]
                      }
                    },
                    {
                      "t": 30,
                      "s": {
                        "c": 0,
                        "i": [[0.0, 0.0]],
                        "o": [[0.0, 0.0]],
                        "v": [[10.0, 10.0]]
                      }
                    }
                  ]
                }
              }
            ]
          }
        ]
      }
      """
        .trimIndent()

    val animation = Animation.decodeFromString(json)

    val shapeLayer = animation.layers[0] as ShapeLayer
    val path1 = shapeLayer.shapes[0] as Path
    val path2 = shapeLayer.shapes[1] as Path

    assertThat(path1.shape.animated).isTrue()
    assertThat(path2.shape.animated).isTrue()
  }

  @Test
  fun bezierProperty_parsesSlotId() {
    val json =
      """
      {
        "v": "5.7.0",
        "fr": 30,
        "ip": 0,
        "op": 30,
        "w": 50,
        "h": 50,
        "layers": [
          {
            "ty": 4,
            "nm": "ShapeLayer",
            "shapes": [
              {
                "ty": "sh",
                "nm": "SlotPath",
                "ks": {
                  "sid": "path.custom_outline",
                  "a": 0,
                  "k": {
                    "c": true,
                    "i": [[0.0, 0.0]],
                    "o": [[0.0, 0.0]],
                    "v": [[0.0, 0.0]]
                  }
                }
              }
            ]
          }
        ]
      }
      """
        .trimIndent()

    val animation = Animation.decodeFromString(json)

    val shapeLayer = animation.layers[0] as ShapeLayer
    val path = shapeLayer.shapes[0] as Path
    assertThat(path.shape).isNotNull()
  }

  @Test
  fun vectorProperty_handlesFloatArraysAndSingleNumberFallback() {
    val json =
      """
      {
        "v": "5.7.0",
        "fr": 30,
        "ip": 0,
        "op": 30,
        "w": 50,
        "h": 50,
        "layers": [
          {
            "ty": 4,
            "nm": "ShapeLayer",
            "shapes": [
              {
                "ty": "rc",
                "nm": "StandardRect",
                "s": { "k": [100.0, 50.0] }
              },
              {
                "ty": "el",
                "nm": "3DVectorEllipse",
                "s": { "k": [40.0, 40.0, 0.0] }
              }
            ]
          }
        ]
      }
      """
        .trimIndent()

    val animation = Animation.decodeFromString(json)

    val shapeLayer = animation.layers[0] as ShapeLayer
    assertThat(shapeLayer.shapes).hasSize(2)
    val rect = shapeLayer.shapes[0] as Rectangle
    val ellipse = shapeLayer.shapes[1] as Ellipse

    assertThat(rect.size).isNotNull()
    assertThat(ellipse.size).isNotNull()
  }

  @Test
  fun vectorProperty_parsesSlotId() {
    val json =
      """
      {
        "v": "5.7.0",
        "fr": 30,
        "ip": 0,
        "op": 30,
        "w": 50,
        "h": 50,
        "layers": [
          {
            "ty": 4,
            "nm": "ShapeLayer",
            "shapes": [
              {
                "ty": "rc",
                "nm": "SlotRect",
                "s": {
                  "sid": "vector.rect_size",
                  "k": [120.0, 80.0]
                }
              },
              {
                "ty": "tr",
                "nm": "TransformGroup",
                "s": {
                  "sid": "vector.scale",
                  "a": 1,
                  "k": [
                    { "t": 0, "s": [100.0, 100.0] },
                    { "t": 30, "s": [200.0, 200.0] }
                  ]
                }
              }
            ]
          }
        ]
      }
      """
        .trimIndent()

    val animation = Animation.decodeFromString(json)

    val shapeLayer = animation.layers[0] as ShapeLayer
    val rect = shapeLayer.shapes[0] as Rectangle
    val transform = shapeLayer.shapes[1] as Transform

    assertThat(rect.size).isNotNull()
    assertThat(transform.scale).isNotNull()
  }

  @Test
  fun vectorProperty_animatedKeyframesWithSingleAndNestedValues() {
    val json =
      """
      {
        "v": "5.7.0",
        "fr": 30,
        "ip": 0,
        "op": 30,
        "w": 50,
        "h": 50,
        "layers": [
          {
            "ty": 4,
            "nm": "ShapeLayer",
            "shapes": [
              {
                "ty": "rc",
                "nm": "AnimatedRect",
                "s": {
                  "a": 1,
                  "k": [
                    {
                      "t": 0,
                      "s": [50.0, 50.0],
                      "i": { "x": 0.5, "y": 1.0 },
                      "o": { "x": 0.5, "y": 0.0 }
                    },
                    {
                      "t": 30,
                      "s": [100.0, 100.0]
                    }
                  ]
                }
              }
            ]
          }
        ]
      }
      """
        .trimIndent()

    val animation = Animation.decodeFromString(json)

    val shapeLayer = animation.layers[0] as ShapeLayer
    val rect = shapeLayer.shapes[0] as Rectangle
    assertThat(rect.size.animated).isTrue()
  }

  @Test
  fun positionProperty_handlesFloatArraysAndSingleNumberFallback() {
    val json =
      """
      {
        "v": "5.7.0",
        "fr": 30,
        "ip": 0,
        "op": 30,
        "w": 50,
        "h": 50,
        "layers": [
          {
            "ty": 4,
            "nm": "ShapeLayer",
            "shapes": [
              {
                "ty": "rc",
                "nm": "StandardRect",
                "p": { "k": [100.0, 50.0] }
              },
              {
                "ty": "el",
                "nm": "3DPositionEllipse",
                "p": { "k": [40.0, 40.0, 0.0] }
              }
            ]
          }
        ]
      }
      """
        .trimIndent()

    val animation = Animation.decodeFromString(json)

    val shapeLayer = animation.layers[0] as ShapeLayer
    assertThat(shapeLayer.shapes).hasSize(2)
    val rect = shapeLayer.shapes[0] as Rectangle
    val ellipse = shapeLayer.shapes[1] as Ellipse

    assertThat(rect.position).isNotNull()
    assertThat(ellipse.position).isNotNull()
  }

  @Test
  fun positionProperty_parsesSlotId() {
    val json =
      """
      {
        "v": "5.7.0",
        "fr": 30,
        "ip": 0,
        "op": 30,
        "w": 50,
        "h": 50,
        "layers": [
          {
            "ty": 4,
            "nm": "ShapeLayer",
            "shapes": [
              {
                "ty": "rc",
                "nm": "SlotRect",
                "p": {
                  "sid": "position.rect_pos",
                  "k": [120.0, 80.0]
                }
              },
              {
                "ty": "tr",
                "nm": "TransformGroup",
                "p": {
                  "sid": "position.translation",
                  "a": 1,
                  "k": [
                    { "t": 0, "s": [100.0, 100.0] },
                    { "t": 30, "s": [200.0, 200.0] }
                  ]
                }
              }
            ]
          }
        ]
      }
      """
        .trimIndent()

    val animation = Animation.decodeFromString(json)

    val shapeLayer = animation.layers[0] as ShapeLayer
    val rect = shapeLayer.shapes[0] as Rectangle
    val transform = shapeLayer.shapes[1] as Transform

    assertThat(rect.position).isNotNull()
    assertThat(transform.positionTranslation).isNotNull()
  }

  @Test
  fun positionProperty_animatedKeyframesWithSingleAndNestedValues() {
    val json =
      """
      {
        "v": "5.7.0",
        "fr": 30,
        "ip": 0,
        "op": 30,
        "w": 50,
        "h": 50,
        "layers": [
          {
            "ty": 4,
            "nm": "ShapeLayer",
            "shapes": [
              {
                "ty": "rc",
                "nm": "AnimatedRect",
                "p": {
                  "a": 1,
                  "k": [
                    {
                      "t": 0,
                      "s": [50.0, 50.0],
                      "i": { "x": 0.5, "y": 1.0 },
                      "o": { "x": 0.5, "y": 0.0 }
                    },
                    {
                      "t": 30,
                      "s": [100.0, 100.0]
                    }
                  ]
                }
              }
            ]
          }
        ]
      }
      """
        .trimIndent()

    val animation = Animation.decodeFromString(json)

    val shapeLayer = animation.layers[0] as ShapeLayer
    val rect = shapeLayer.shapes[0] as Rectangle
    assertThat(rect.position.animated).isTrue()
  }

  @Test
  fun extraPluginMetadata_ignoredCleanly() {
    val json =
      """
      {
        "v": "5.7.0",
        "fr": 30,
        "ip": 0,
        "op": 30,
        "w": 50,
        "h": 50,
        "meta": { "g": "LottieFiles 2.0" },
        "_ae_version": "17.5.0",
        "layers": []
      }
      """
        .trimIndent()

    val animation = Animation.decodeFromString(json)

    assertThat(animation.frameRate).isEqualTo(30)
    assertThat(animation.layers).isEmpty()
  }

  @Test
  fun gradientProperty_opaqueArray_samplesColorAtPositions() {
    val json = "[0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0]"
    val gradient =
      LottieDecoder.json.decodeFromString(GradientValueSerializer(colorStopCount = 2), json)

    val colorStart = gradient.getColorForPosition(0.0f).constantValueOrNull
    assertThat(colorStart?.red).isEqualTo(1f)
    assertThat(colorStart?.green).isEqualTo(0f)
    assertThat(colorStart?.blue).isEqualTo(0f)
    assertThat(colorStart?.alpha).isEqualTo(1f)

    val colorMid = gradient.getColorForPosition(0.5f).constantValueOrNull
    assertThat(colorMid?.red).isWithin(0.01f).of(0.5f)
    assertThat(colorMid?.green).isWithin(0.01f).of(0.5f)
    assertThat(colorMid?.alpha).isEqualTo(1f)

    val colorEnd = gradient.getColorForPosition(1.0f).constantValueOrNull
    assertThat(colorEnd?.green).isEqualTo(1f)
    assertThat(colorEnd?.alpha).isEqualTo(1f)
  }

  @Test
  fun gradientProperty_transparentObject_samplesColorWithAlpha() {
    val json = "[0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.5]"
    val gradient =
      LottieDecoder.json.decodeFromString(GradientValueSerializer(colorStopCount = 2), json)

    val colorStart = gradient.getColorForPosition(0.0f).constantValueOrNull
    assertThat(colorStart?.red).isEqualTo(1f)
    assertThat(colorStart?.alpha).isEqualTo(1f)

    val colorEnd = gradient.getColorForPosition(1.0f).constantValueOrNull
    assertThat(colorEnd?.blue).isEqualTo(1f)
    assertThat(colorEnd?.alpha).isWithin(0.01f).of(0.5f)
  }

  @Test
  fun gradientProperty_scaled255Integers_samplesNormalizedColor() {
    val json = "[0.0, 255.0, 0.0, 0.0, 1.0, 0.0, 255.0, 0.0, 0.0, 255.0, 1.0, 128.0]"
    val gradient =
      LottieDecoder.json.decodeFromString(GradientValueSerializer(colorStopCount = 2), json)

    val colorStart = gradient.getColorForPosition(0.0f).constantValueOrNull
    assertThat(colorStart?.red).isEqualTo(1f)
    assertThat(colorStart?.alpha).isEqualTo(1f)

    val colorEnd = gradient.getColorForPosition(1.0f).constantValueOrNull
    assertThat(colorEnd?.green).isEqualTo(1f)
    assertThat(colorEnd?.alpha).isWithin(0.01f).of(128f / 255f)
  }

  @Test
  fun gradientProperty_independentStops_interpolatesAlongTimeline() {
    val json =
      """
      [
        0.0, 1.0, 0.0, 0.0,
        0.5, 0.0, 1.0, 0.0,
        1.0, 0.0, 0.0, 1.0,
        0.25, 0.8,
        0.75, 0.4
      ]
      """
        .trimIndent()
    val gradient =
      LottieDecoder.json.decodeFromString(GradientValueSerializer(colorStopCount = 3), json)

    val c0 = gradient.getColorForPosition(0.0f).constantValueOrNull
    assertThat(c0?.red).isWithin(0.01f).of(1.0f)
    assertThat(c0?.green).isWithin(0.01f).of(0.0f)
    assertThat(c0?.alpha).isWithin(0.01f).of(0.8f)

    val c025 = gradient.getColorForPosition(0.25f).constantValueOrNull
    assertThat(c025?.red).isWithin(0.01f).of(0.5f)
    assertThat(c025?.green).isWithin(0.01f).of(0.5f)
    assertThat(c025?.alpha).isWithin(0.01f).of(0.8f)

    val c05 = gradient.getColorForPosition(0.5f).constantValueOrNull
    assertThat(c05?.green).isWithin(0.01f).of(1.0f)
    assertThat(c05?.alpha).isWithin(0.01f).of(0.6f)

    val c075 = gradient.getColorForPosition(0.75f).constantValueOrNull
    assertThat(c075?.green).isWithin(0.01f).of(0.5f)
    assertThat(c075?.blue).isWithin(0.01f).of(0.5f)
    assertThat(c075?.alpha).isWithin(0.01f).of(0.4f)

    val c1 = gradient.getColorForPosition(1.0f).constantValueOrNull
    assertThat(c1?.blue).isWithin(0.01f).of(1.0f)
    assertThat(c1?.alpha).isWithin(0.01f).of(0.4f)
  }

  @Test
  fun gradientFill_animatedAndSlotId_deserializes() {
    val json =
      """
      {
        "v": "5.7.0",
        "fr": 30,
        "ip": 0,
        "op": 30,
        "w": 50,
        "h": 50,
        "layers": [
          {
            "ty": 4,
            "nm": "ShapeLayer",
            "shapes": [
              {
                "ty": "gf",
                "nm": "DynamicGradFill",
                "t": 1,
                "s": { "k": [0.0, 0.0] },
                "e": { "k": [50.0, 50.0] },
                "g": {
                  "sid": "slot.grad",
                  "a": 1,
                  "p": 2,
                  "k": [
                    {
                      "t": 0,
                      "s": [0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0]
                    },
                    {
                      "t": 30,
                      "s": [0.0, 0.0, 0.0, 1.0, 1.0, 1.0, 1.0, 0.0]
                    }
                  ]
                }
              }
            ]
          }
        ]
      }
      """
        .trimIndent()

    val animation = Animation.decodeFromString(json)
    val shapeLayer = animation.layers[0] as ShapeLayer
    val gf = shapeLayer.shapes[0] as GradientFill
    assertThat(gf.colors.animated).isTrue()
    assertThat(gf.colors.slotId).isEqualTo("slot.grad")
    val animGradient = gf.colors as AnimatedGradientProperty
    assertThat(animGradient.keyframes).hasSize(2)
  }

  @Test
  fun gradientStroke_withHighlights_deserializes() {
    val json =
      """
      {
        "v": "5.7.0",
        "fr": 30,
        "ip": 0,
        "op": 30,
        "w": 50,
        "h": 50,
        "layers": [
          {
            "ty": 4,
            "nm": "ShapeLayer",
            "shapes": [
              {
                "ty": "gs",
                "nm": "RadialGradStroke",
                "t": 2,
                "s": { "k": [25.0, 25.0] },
                "e": { "k": [50.0, 50.0] },
                "r": { "k": 45.0 },
                "h": { "k": 90.0 },
                "w": { "k": 2.5 },
                "g": {
                  "p": 2,
                  "k": [0.0, 1.0, 1.0, 0.0, 1.0, 0.0, 1.0, 1.0]
                }
              }
            ]
          }
        ]
      }
      """
        .trimIndent()

    val animation = Animation.decodeFromString(json)
    val shapeLayer = animation.layers[0] as ShapeLayer
    val gs = shapeLayer.shapes[0] as GradientStroke
    assertThat((gs.highlightLength as StaticScalarProperty).value).isEqualTo(45.0f)
    assertThat((gs.highlightAngle as StaticScalarProperty).value).isEqualTo(90.0f)
    assertThat((gs.strokeWidth as StaticScalarProperty).value).isEqualTo(2.5f)
  }

  @Test
  fun shapeTypeEnum_deserializesFromStringOrDefaultsToUnknown() {
    assertThat(ShapeType.fromValueOrNull("sh")).isEqualTo(ShapeType.Path)
    assertThat(ShapeType.fromValueOrNull("rc")).isEqualTo(ShapeType.Rectangle)
    assertThat(ShapeType.fromValueOrNull("el")).isEqualTo(ShapeType.Ellipse)
    assertThat(ShapeType.fromValueOrNull("sr")).isEqualTo(ShapeType.PolyStar)
    assertThat(ShapeType.fromValueOrNull("gr")).isEqualTo(ShapeType.Group)
    assertThat(ShapeType.fromValueOrNull("tr")).isEqualTo(ShapeType.Transform)
    assertThat(ShapeType.fromValueOrNull("fl")).isEqualTo(ShapeType.Fill)
    assertThat(ShapeType.fromValueOrNull("st")).isEqualTo(ShapeType.Stroke)
    assertThat(ShapeType.fromValueOrNull("gf")).isEqualTo(ShapeType.GradientFill)
    assertThat(ShapeType.fromValueOrNull("gs")).isEqualTo(ShapeType.GradientStroke)
    assertThat(ShapeType.fromValueOrNull("no")).isEqualTo(ShapeType.NoStyle)
    assertThat(ShapeType.fromValueOrNull("tm")).isEqualTo(ShapeType.TrimPath)
    assertThat(ShapeType.fromValueOrNull("rp")).isEqualTo(ShapeType.Repeater)
    assertThat(ShapeType.fromValueOrNull("rd")).isEqualTo(ShapeType.RoundedCorners)
    assertThat(ShapeType.fromValueOrNull("mm")).isEqualTo(ShapeType.MergePaths)
    assertThat(ShapeType.fromValueOrNull("op")).isEqualTo(ShapeType.OffsetPath)
    assertThat(ShapeType.fromValueOrNull("pb")).isEqualTo(ShapeType.PuckerBloat)
    assertThat(ShapeType.fromValueOrNull("tw")).isEqualTo(ShapeType.Twist)
    assertThat(ShapeType.fromValueOrNull("zz")).isEqualTo(ShapeType.ZigZag)
    assertThat(ShapeType.fromValueOrNull("unsupported")).isNull()

    val decodedKnown = LottieDecoder.json.decodeFromString(ShapeType.serializer(), "\"st\"")
    assertThat(decodedKnown).isEqualTo(ShapeType.Stroke)

    assertThat(LottieDecoder.json.decodeFromString(ShapeType.serializer(), "\"op\""))
      .isEqualTo(ShapeType.OffsetPath)
    assertThat(LottieDecoder.json.decodeFromString(ShapeType.serializer(), "\"pb\""))
      .isEqualTo(ShapeType.PuckerBloat)
    assertThat(LottieDecoder.json.decodeFromString(ShapeType.serializer(), "\"tw\""))
      .isEqualTo(ShapeType.Twist)
    assertThat(LottieDecoder.json.decodeFromString(ShapeType.serializer(), "\"zz\""))
      .isEqualTo(ShapeType.ZigZag)

    val decodedUnknown =
      LottieDecoder.json.decodeFromString(ShapeType.serializer(), "\"invalid_type\"")
    assertThat(decodedUnknown).isEqualTo(ShapeType.Unknown)
  }

  @Test
  fun lineCapAndJoin_handlesIntegerAndFloatAndFallback() {
    assertThat(LottieDecoder.json.decodeFromString(LineCapSerializer, "1")).isEqualTo(LineCap.Butt)
    assertThat(LottieDecoder.json.decodeFromString(LineCapSerializer, "2")).isEqualTo(LineCap.Round)
    assertThat(LottieDecoder.json.decodeFromString(LineCapSerializer, "3"))
      .isEqualTo(LineCap.Square)
    assertThat(LottieDecoder.json.decodeFromString(LineCapSerializer, "2.0"))
      .isEqualTo(LineCap.Round)
    assertThat(LottieDecoder.json.decodeFromString(LineCapSerializer, "999"))
      .isEqualTo(LineCap.Round)

    assertThat(LottieDecoder.json.decodeFromString(LineJoinSerializer, "1"))
      .isEqualTo(LineJoin.Miter)
    assertThat(LottieDecoder.json.decodeFromString(LineJoinSerializer, "2"))
      .isEqualTo(LineJoin.Round)
    assertThat(LottieDecoder.json.decodeFromString(LineJoinSerializer, "3"))
      .isEqualTo(LineJoin.Bevel)
    assertThat(LottieDecoder.json.decodeFromString(LineJoinSerializer, "1.0"))
      .isEqualTo(LineJoin.Miter)
    assertThat(LottieDecoder.json.decodeFromString(LineJoinSerializer, "999"))
      .isEqualTo(LineJoin.Round)
  }

  @Test
  fun enumSerializers_trimCompositeMergePolyStarFillRuleZigZag_handlesIntegersFloatsAndFallbacks() {
    assertThat(LottieDecoder.json.decodeFromString(TrimModeSerializer, "1"))
      .isEqualTo(TrimMode.Simultaneously)
    assertThat(LottieDecoder.json.decodeFromString(TrimModeSerializer, "2.0"))
      .isEqualTo(TrimMode.Individually)
    assertThat(LottieDecoder.json.decodeFromString(TrimModeSerializer, "99"))
      .isEqualTo(TrimMode.Simultaneously)

    assertThat(LottieDecoder.json.decodeFromString(CompositeModeSerializer, "1"))
      .isEqualTo(CompositeMode.Above)
    assertThat(LottieDecoder.json.decodeFromString(CompositeModeSerializer, "2.0"))
      .isEqualTo(CompositeMode.Below)
    assertThat(LottieDecoder.json.decodeFromString(CompositeModeSerializer, "99"))
      .isEqualTo(CompositeMode.Above)

    assertThat(LottieDecoder.json.decodeFromString(MergeModeSerializer, "1"))
      .isEqualTo(MergeMode.Merge)
    assertThat(LottieDecoder.json.decodeFromString(MergeModeSerializer, "2"))
      .isEqualTo(MergeMode.Add)
    assertThat(LottieDecoder.json.decodeFromString(MergeModeSerializer, "3"))
      .isEqualTo(MergeMode.Subtract)
    assertThat(LottieDecoder.json.decodeFromString(MergeModeSerializer, "4.0"))
      .isEqualTo(MergeMode.Intersect)
    assertThat(LottieDecoder.json.decodeFromString(MergeModeSerializer, "5"))
      .isEqualTo(MergeMode.ExcludeIntersections)
    assertThat(LottieDecoder.json.decodeFromString(MergeModeSerializer, "99"))
      .isEqualTo(MergeMode.Merge)

    assertThat(LottieDecoder.json.decodeFromString(PolyStarTypeSerializer, "1"))
      .isEqualTo(PolyStarType.Star)
    assertThat(LottieDecoder.json.decodeFromString(PolyStarTypeSerializer, "2.0"))
      .isEqualTo(PolyStarType.Polygon)
    assertThat(LottieDecoder.json.decodeFromString(PolyStarTypeSerializer, "99"))
      .isEqualTo(PolyStarType.Star)

    assertThat(LottieDecoder.json.decodeFromString(FillRuleSerializer, "1"))
      .isEqualTo(FillRule.NonZero)
    assertThat(LottieDecoder.json.decodeFromString(FillRuleSerializer, "2.0"))
      .isEqualTo(FillRule.EvenOdd)
    assertThat(LottieDecoder.json.decodeFromString(FillRuleSerializer, "99"))
      .isEqualTo(FillRule.NonZero)

    assertThat(LottieDecoder.json.decodeFromString(ZigZagTypeSerializer, "1"))
      .isEqualTo(ZigZagType.Corner)
    assertThat(LottieDecoder.json.decodeFromString(ZigZagTypeSerializer, "2.0"))
      .isEqualTo(ZigZagType.Smooth)
    assertThat(LottieDecoder.json.decodeFromString(ZigZagTypeSerializer, "99"))
      .isEqualTo(ZigZagType.Corner)
  }
}
