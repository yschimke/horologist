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

import org.junit.Test

/**
 * Screenshot tests comparing RemoteCompose Lottie rendering with `lottie-android` reference output.
 *
 * Test cases for parametric shapes (`rect_ellipse`, `polystar`) are sourced from the
 * [Lottie Format Feature Support & Sample Test Suite](https://docs.google.com/document/d/1jXj3kbXL57kxjRc0soUqst2poa2-Lrc2qZAIzEmbB8w/edit).
 */
class LottieFeatureDiffScreenshotTest : LottieDiffScreenshotTest() {

  @Test
  fun positionStatic() {
    runLottieDiffTest(R.raw.position_static)
  }

  @Test
  fun positionAnimated() {
    runLottieDiffTest(R.raw.position_animated) {
      captureFrame(frame = 0f)
      captureFrame(frame = 20f)
      captureFrame(frame = 40f)
      captureFrame(frame = 60f)
    }
  }

  /** Tests parametric rectangle, rounded rectangle, ellipse, and circle shapes. */
  @Test
  fun rectEllipse() {
    runLottieDiffTest(R.raw.rect_ellipse)
  }

  /** Tests parametric star, rounded star, polygon, and rounded polygon shapes. */
  @Test
  fun polystar() {
    runLottieDiffTest(R.raw.polystar)
  }

  /**
   * Tests layer parenting more than one level deep.
   *
   * `parent_chain` is 20 dot layers chained child -> parent -> grandparent -> ..., each applying
   * the same relative delta (translate 30px, rotate 25 degrees, scale 93%). Accumulating those
   * deltas down the chain draws a shrinking, fading spiral.
   *
   * Only the layer's immediate parent transform is applied, so every layer from the third down
   * loses its ancestors' transforms and collapses onto a single point.
   *
   * See https://github.com/google/horologist/issues/2795.
   */
  @Test
  fun parentChain() {
    runLottieDiffTest(R.raw.parent_chain)
  }

  /** Tests 2D skew and skew axis transformation matrix rendering. */
  @Test
  fun transformSkew() {
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
            "nm": "SkewedLayer",
            "ind": 1,
            "ip": 0,
            "op": 30,
            "ks": {
              "p": { "a": 0, "k": [50.0, 50.0, 0.0] },
              "a": { "a": 0, "k": [0.0, 0.0, 0.0] },
              "s": { "a": 0, "k": [100.0, 100.0, 100.0] },
              "r": { "a": 0, "k": 0.0 },
              "o": { "a": 0, "k": 100.0 },
              "sk": { "a": 0, "k": 30.0 },
              "sa": { "a": 0, "k": 45.0 }
            },
            "shapes": [
              {
                "ty": "gr",
                "nm": "RectGroup",
                "it": [
                  {
                    "ty": "rc",
                    "p": { "a": 0, "k": [0.0, 0.0] },
                    "s": { "a": 0, "k": [40.0, 40.0] },
                    "r": { "a": 0, "k": 0.0 }
                  },
                  {
                    "ty": "fl",
                    "c": { "a": 0, "k": [0.2, 0.6, 1.0, 1.0] },
                    "o": { "a": 0, "k": 100.0 }
                  },
                  {
                    "ty": "tr",
                    "p": { "a": 0, "k": [0.0, 0.0] },
                    "a": { "a": 0, "k": [0.0, 0.0] },
                    "s": { "a": 0, "k": [100.0, 100.0] },
                    "r": { "a": 0, "k": 0.0 },
                    "o": { "a": 0, "k": 100.0 }
                  }
                ]
              }
            ]
          }
        ]
      }
      """
        .trimIndent()

    runLottieDiffTest(json = json)
  }

  /** Tests precomposition recursive sub-composition rendering engine. */
  @Test
  fun precompSubcompositionRendering() {
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
            "id": "comp_circle_group",
            "nm": "CircleSubcomp",
            "layers": [
              {
                "ty": 4,
                "nm": "SubcompShape",
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
                "shapes": [
                  {
                    "ty": "gr",
                    "nm": "ShapeGroup",
                    "it": [
                      {
                        "ty": "el",
                        "p": { "a": 0, "k": [0.0, 0.0] },
                        "s": { "a": 0, "k": [36.0, 36.0] }
                      },
                      {
                        "ty": "fl",
                        "c": { "a": 0, "k": [0.9, 0.2, 0.3, 1.0] },
                        "o": { "a": 0, "k": 100.0 }
                      },
                      {
                        "ty": "tr",
                        "p": { "a": 0, "k": [0.0, 0.0] },
                        "a": { "a": 0, "k": [0.0, 0.0] },
                        "s": { "a": 0, "k": [100.0, 100.0] },
                        "r": { "a": 0, "k": 0.0 },
                        "o": { "a": 0, "k": 100.0 }
                      }
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
            "nm": "PrecompInstance",
            "refId": "comp_circle_group",
            "ind": 10,
            "ip": 0,
            "op": 30,
            "w": 100,
            "h": 100,
            "ks": {
              "p": { "a": 0, "k": [50.0, 50.0, 0.0] },
              "a": { "a": 0, "k": [0.0, 0.0, 0.0] },
              "s": { "a": 0, "k": [100.0, 100.0, 100.0] },
              "r": { "a": 0, "k": 0.0 },
              "o": { "a": 0, "k": 100.0 }
            }
          }
        ]
      }
      """
        .trimIndent()

    runLottieDiffTest(json = json)
  }

  /** Tests linear gradient fill with multi-color stops on parametric shapes. */
  @Test
  fun gradientLinearFill() {
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
            "nm": "LinearGradientLayer",
            "ind": 1,
            "ip": 0,
            "op": 30,
            "ks": {
              "p": { "a": 0, "k": [50.0, 50.0, 0.0] },
              "a": { "a": 0, "k": [0.0, 0.0, 0.0] },
              "s": { "a": 0, "k": [100.0, 100.0, 100.0] },
              "r": { "a": 0, "k": 0.0 },
              "o": { "a": 0, "k": 100.0 }
            },
            "shapes": [
              {
                "ty": "gr",
                "nm": "RectWithLinearGrad",
                "it": [
                  {
                    "ty": "rc",
                    "p": { "a": 0, "k": [0.0, 0.0] },
                    "s": { "a": 0, "k": [70.0, 70.0] },
                    "r": { "a": 0, "k": 10.0 }
                  },
                  {
                    "ty": "gf",
                    "nm": "LinearGradFill",
                    "t": 1,
                    "o": { "a": 0, "k": 100.0 },
                    "r": 1,
                    "s": { "a": 0, "k": [-35.0, -35.0] },
                    "e": { "a": 0, "k": [35.0, 35.0] },
                    "g": {
                      "p": 3,
                      "k": [
                        0.0, 1.0, 0.2, 0.2,
                        0.5, 1.0, 0.8, 0.0,
                        1.0, 0.2, 0.4, 1.0
                      ]
                    }
                  },
                  {
                    "ty": "tr",
                    "p": { "a": 0, "k": [0.0, 0.0] },
                    "a": { "a": 0, "k": [0.0, 0.0] },
                    "s": { "a": 0, "k": [100.0, 100.0] },
                    "r": { "a": 0, "k": 0.0 },
                    "o": { "a": 0, "k": 100.0 }
                  }
                ]
              }
            ]
          }
        ]
      }
      """
        .trimIndent()

    runLottieDiffTest(json = json)
  }

  /** Tests radial gradient fill with color and opacity/alpha stops. */
  @Test
  fun gradientRadialFill() {
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
            "nm": "RadialGradientLayer",
            "ind": 1,
            "ip": 0,
            "op": 30,
            "ks": {
              "p": { "a": 0, "k": [50.0, 50.0, 0.0] },
              "a": { "a": 0, "k": [0.0, 0.0, 0.0] },
              "s": { "a": 0, "k": [100.0, 100.0, 100.0] },
              "r": { "a": 0, "k": 0.0 },
              "o": { "a": 0, "k": 100.0 }
            },
            "shapes": [
              {
                "ty": "gr",
                "nm": "CircleWithRadialGrad",
                "it": [
                  {
                    "ty": "el",
                    "p": { "a": 0, "k": [0.0, 0.0] },
                    "s": { "a": 0, "k": [70.0, 70.0] }
                  },
                  {
                    "ty": "gf",
                    "nm": "RadialGradFill",
                    "t": 2,
                    "o": { "a": 0, "k": 100.0 },
                    "r": 1,
                    "s": { "a": 0, "k": [0.0, 0.0] },
                    "e": { "a": 0, "k": [35.0, 0.0] },
                    "g": {
                      "p": 2,
                      "k": [
                        0.0, 0.0, 1.0, 0.8,
                        1.0, 0.8, 0.0, 1.0,
                        0.0, 1.0,
                        1.0, 0.3
                      ]
                    }
                  },
                  {
                    "ty": "tr",
                    "p": { "a": 0, "k": [0.0, 0.0] },
                    "a": { "a": 0, "k": [0.0, 0.0] },
                    "s": { "a": 0, "k": [100.0, 100.0] },
                    "r": { "a": 0, "k": 0.0 },
                    "o": { "a": 0, "k": 100.0 }
                  }
                ]
              }
            ]
          }
        ]
      }
      """
        .trimIndent()

    runLottieDiffTest(json = json)
  }

  /** Tests gradient stroke with multi-color stops, stroke width, and rounded caps/joins. */
  @Test
  fun gradientStroke() {
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
            "nm": "GradientStrokeLayer",
            "ind": 1,
            "ip": 0,
            "op": 30,
            "ks": {
              "p": { "a": 0, "k": [50.0, 50.0, 0.0] },
              "a": { "a": 0, "k": [0.0, 0.0, 0.0] },
              "s": { "a": 0, "k": [100.0, 100.0, 100.0] },
              "r": { "a": 0, "k": 0.0 },
              "o": { "a": 0, "k": 100.0 }
            },
            "shapes": [
              {
                "ty": "gr",
                "nm": "StrokeGradGroup",
                "it": [
                  {
                    "ty": "rc",
                    "p": { "a": 0, "k": [0.0, 0.0] },
                    "s": { "a": 0, "k": [64.0, 64.0] },
                    "r": { "a": 0, "k": 12.0 }
                  },
                  {
                    "ty": "gs",
                    "nm": "LinearGradStroke",
                    "t": 1,
                    "o": { "a": 0, "k": 100.0 },
                    "w": { "a": 0, "k": 8.0 },
                    "s": { "a": 0, "k": [-32.0, 0.0] },
                    "e": { "a": 0, "k": [32.0, 0.0] },
                    "g": {
                      "p": 2,
                      "k": [
                        0.0, 1.0, 0.6, 0.0,
                        1.0, 0.0, 0.8, 1.0
                      ]
                    },
                    "lc": 2,
                    "lj": 2
                  },
                  {
                    "ty": "tr",
                    "p": { "a": 0, "k": [0.0, 0.0] },
                    "a": { "a": 0, "k": [0.0, 0.0] },
                    "s": { "a": 0, "k": [100.0, 100.0] },
                    "r": { "a": 0, "k": 0.0 },
                    "o": { "a": 0, "k": 100.0 }
                  }
                ]
              }
            ]
          }
        ]
      }
      """
        .trimIndent()

    runLottieDiffTest(json = json)
  }

  /** Tests stroke dash patterns and dash offsets. */
  @Test
  fun strokeDashPattern() {
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
            "nm": "DashLayer",
            "ind": 1,
            "ip": 0,
            "op": 30,
            "ks": {
              "p": { "a": 0, "k": [50.0, 50.0, 0.0] },
              "a": { "a": 0, "k": [0.0, 0.0, 0.0] },
              "s": { "a": 0, "k": [100.0, 100.0, 100.0] },
              "r": { "a": 0, "k": 0.0 },
              "o": { "a": 0, "k": 100.0 }
            },
            "shapes": [
              {
                "ty": "gr",
                "nm": "DashedRectGroup",
                "it": [
                  {
                    "ty": "rc",
                    "p": { "a": 0, "k": [0.0, 0.0] },
                    "s": { "a": 0, "k": [64.0, 64.0] },
                    "r": { "a": 0, "k": 12.0 }
                  },
                  {
                    "ty": "st",
                    "nm": "DashStroke",
                    "c": { "a": 0, "k": [0.2, 0.7, 1.0, 1.0] },
                    "o": { "a": 0, "k": 100.0 },
                    "w": { "a": 0, "k": 6.0 },
                    "lc": 2,
                    "lj": 2,
                    "d": [
                      { "n": "d", "nm": "dash", "v": { "a": 0, "k": 10.0 } },
                      { "n": "g", "nm": "gap", "v": { "a": 0, "k": 5.0 } },
                      { "n": "d", "nm": "dash2", "v": { "a": 0, "k": 2.0 } },
                      { "n": "g", "nm": "gap2", "v": { "a": 0, "k": 5.0 } },
                      { "n": "o", "nm": "offset", "v": { "a": 0, "k": 0.0 } }
                    ]
                  },
                  {
                    "ty": "tr",
                    "p": { "a": 0, "k": [0.0, 0.0] },
                    "a": { "a": 0, "k": [0.0, 0.0] },
                    "s": { "a": 0, "k": [100.0, 100.0] },
                    "r": { "a": 0, "k": 0.0 },
                    "o": { "a": 0, "k": 100.0 }
                  }
                ]
              }
            ]
          }
        ]
      }
      """
        .trimIndent()

    runLottieDiffTest(json = json)
  }

  /** Tests acute miter limit clipping on sharp polystar stroke corners. */
  @Test
  fun strokeMiterLimit() {
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
            "nm": "MiterLayer",
            "ind": 1,
            "ip": 0,
            "op": 30,
            "ks": {
              "p": { "a": 0, "k": [50.0, 50.0, 0.0] },
              "a": { "a": 0, "k": [0.0, 0.0, 0.0] },
              "s": { "a": 0, "k": [100.0, 100.0, 100.0] },
              "r": { "a": 0, "k": 0.0 },
              "o": { "a": 0, "k": 100.0 }
            },
            "shapes": [
              {
                "ty": "gr",
                "nm": "MiterStarGroup",
                "it": [
                  {
                    "ty": "sr",
                    "sy": 1,
                    "pt": { "a": 0, "k": 5.0 },
                    "p": { "a": 0, "k": [0.0, 0.0] },
                    "r": { "a": 0, "k": 0.0 },
                    "or": { "a": 0, "k": 38.0 },
                    "os": { "a": 0, "k": 0.0 },
                    "ir": { "a": 0, "k": 16.0 },
                    "is": { "a": 0, "k": 0.0 }
                  },
                  {
                    "ty": "st",
                    "nm": "MiterStroke",
                    "c": { "a": 0, "k": [1.0, 0.5, 0.0, 1.0] },
                    "o": { "a": 0, "k": 100.0 },
                    "w": { "a": 0, "k": 6.0 },
                    "lc": 1,
                    "lj": 1,
                    "ml": { "a": 0, "k": 4.0 }
                  },
                  {
                    "ty": "tr",
                    "p": { "a": 0, "k": [0.0, 0.0] },
                    "a": { "a": 0, "k": [0.0, 0.0] },
                    "s": { "a": 0, "k": [100.0, 100.0] },
                    "r": { "a": 0, "k": 0.0 },
                    "o": { "a": 0, "k": 100.0 }
                  }
                ]
              }
            ]
          }
        ]
      }
      """
        .trimIndent()

    runLottieDiffTest(json = json)
  }

  /** Tests EvenOdd fill rule on self-intersecting star polygon contours. */
  @Test
  fun fillRuleEvenOdd() {
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
            "nm": "EvenOddLayer",
            "ind": 1,
            "ip": 0,
            "op": 30,
            "ks": {
              "p": { "a": 0, "k": [50.0, 50.0, 0.0] },
              "a": { "a": 0, "k": [0.0, 0.0, 0.0] },
              "s": { "a": 0, "k": [100.0, 100.0, 100.0] },
              "r": { "a": 0, "k": 0.0 },
              "o": { "a": 0, "k": 100.0 }
            },
            "shapes": [
              {
                "ty": "gr",
                "nm": "EvenOddGroup",
                "it": [
                  {
                    "ty": "sh",
                    "nm": "StarPath",
                    "ks": {
                      "a": 0,
                      "k": {
                        "c": true,
                        "v": [
                          [0.0, -40.0],
                          [23.51, 32.36],
                          [-38.04, -12.36],
                          [38.04, -12.36],
                          [-23.51, 32.36]
                        ],
                        "i": [[0.0, 0.0], [0.0, 0.0], [0.0, 0.0], [0.0, 0.0], [0.0, 0.0]],
                        "o": [[0.0, 0.0], [0.0, 0.0], [0.0, 0.0], [0.0, 0.0], [0.0, 0.0]]
                      }
                    }
                  },
                  {
                    "ty": "fl",
                    "nm": "EvenOddFill",
                    "c": { "a": 0, "k": [0.2, 0.8, 0.5, 1.0] },
                    "o": { "a": 0, "k": 100.0 },
                    "r": 2
                  },
                  {
                    "ty": "tr",
                    "p": { "a": 0, "k": [0.0, 0.0] },
                    "a": { "a": 0, "k": [0.0, 0.0] },
                    "s": { "a": 0, "k": [100.0, 100.0] },
                    "r": { "a": 0, "k": 0.0 },
                    "o": { "a": 0, "k": 100.0 }
                  }
                ]
              }
            ]
          }
        ]
      }
      """
        .trimIndent()

    runLottieDiffTest(json = json)
  }
}
