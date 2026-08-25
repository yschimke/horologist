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
import com.google.android.horologist.remotecompose.lottie.format.LottieDecoder
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.GraphicElement
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.Rectangle
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.MergeMode
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.MergePaths
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.Fill
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticColorProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticPositionProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticScalarProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticVectorProperty
import com.google.android.horologist.remotecompose.lottie.renderer.RemoteLottiePath
import com.google.android.horologist.remotecompose.lottie.renderer.RemoteShape
import com.google.android.horologist.remotecompose.lottie.renderer.gatherShapesForTest
import com.google.android.horologist.remotecompose.lottie.renderer.properties.RemoteBezierValue
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.evaluateMergePaths
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SuppressLint("RestrictedApi")
@RunWith(AndroidJUnit4::class)
class MergePathsTest {

  private val settings = LottieSettings(0f.rf, SlotMap(emptyMap()))

  private fun createRectPath(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
  ): RemoteLottiePath {
    val subpath =
      RemoteBezierValue(
        closed = true,
        vertices =
          listOf(
            listOf(left.rf, top.rf),
            listOf(right.rf, top.rf),
            listOf(right.rf, bottom.rf),
            listOf(left.rf, bottom.rf),
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
    return RemoteLottiePath(listOf(subpath))
  }

  @Test
  fun mergeMode_decodingJson() {
    val json =
      """
      {
        "v": "5.5.7",
        "fr": 60,
        "ip": 0,
        "op": 60,
        "w": 100,
        "h": 100,
        "layers": [
          {
            "ddd": 0,
            "ind": 1,
            "ty": 4,
            "nm": "Shape Layer",
            "sr": 1,
            "ks": {},
            "shapes": [
              {
                "ty": "mm",
                "nm": "Merge Paths 1",
                "mm": 3,
                "hd": false
              }
            ]
          }
        ]
      }
      """
        .trimIndent()

    val animation = LottieDecoder.decodeFromString(json)
    val layer =
      animation.layers[0]
        as com.google.android.horologist.remotecompose.lottie.format.layer.ShapeLayer
    val mergePaths = layer.shapes[0] as MergePaths
    assertThat(mergePaths.mode).isEqualTo(MergeMode.Subtract)
    assertThat(mergePaths.hidden).isFalse()
  }

  @Test
  fun evaluateMergePaths_modeMerge_combinesSubpaths() {
    val path1 = createRectPath(0f, 0f, 50f, 50f)
    val path2 = createRectPath(100f, 100f, 150f, 150f)

    val mergeModifier = MergePaths(mode = MergeMode.Merge)
    val result: List<RemoteShape> =
      evaluateMergePaths(listOf(path1, path2), mergeModifier, settings)

    assertThat(result).hasSize(1)
    val merged = result[0] as RemoteLottiePath
    assertThat(merged.path).hasSize(2)
  }

  @Test
  fun evaluateMergePaths_modeAdd_computesUnion() {
    // Two overlapping rectangles: [0, 0, 100, 100] and [50, 0, 150, 100]
    val path1 = createRectPath(0f, 0f, 100f, 100f)
    val path2 = createRectPath(50f, 0f, 150f, 100f)

    val mergeModifier = MergePaths(mode = MergeMode.Add)
    val result: List<RemoteShape> =
      evaluateMergePaths(listOf(path1, path2), mergeModifier, settings)

    assertThat(result).hasSize(1)
    val unionPath = result[0] as RemoteLottiePath
    assertThat(unionPath.path).isNotEmpty()

    // Verify bounds of union path vertices span x in [0, 150] and y in [0, 100]
    val allXs = unionPath.path.flatMap { it.vertices.map { v -> v[0].constantValueOrNull ?: 0f } }
    val allYs = unionPath.path.flatMap { it.vertices.map { v -> v[1].constantValueOrNull ?: 0f } }

    assertThat(allXs.minOrNull()).isWithin(0.1f).of(0f)
    assertThat(allXs.maxOrNull()).isWithin(0.1f).of(150f)
    assertThat(allYs.minOrNull()).isWithin(0.1f).of(0f)
    assertThat(allYs.maxOrNull()).isWithin(0.1f).of(100f)
  }

  @Test
  fun evaluateMergePaths_modeSubtract_computesDifference() {
    // Large rectangle [0, 0, 100, 100] minus inner cutout [25, 25, 75, 75]
    val outer = createRectPath(0f, 0f, 100f, 100f)
    val inner = createRectPath(25f, 25f, 75f, 75f)

    val mergeModifier = MergePaths(mode = MergeMode.Subtract)
    val result: List<RemoteShape> =
      evaluateMergePaths(listOf(outer, inner), mergeModifier, settings)

    assertThat(result).hasSize(1)
    val diffPath = result[0] as RemoteLottiePath
    // Difference should contain multiple contours (outer + cutout)
    assertThat(diffPath.path.size).isAtLeast(1)
  }

  @Test
  fun evaluateMergePaths_modeIntersect_computesIntersection() {
    // Rect 1: [0, 0, 100, 100], Rect 2: [50, 50, 150, 150] -> Intersection is [50, 50, 100, 100]
    val path1 = createRectPath(0f, 0f, 100f, 100f)
    val path2 = createRectPath(50f, 50f, 150f, 150f)

    val mergeModifier = MergePaths(mode = MergeMode.Intersect)
    val result: List<RemoteShape> =
      evaluateMergePaths(listOf(path1, path2), mergeModifier, settings)

    assertThat(result).hasSize(1)
    val intersectPath = result[0] as RemoteLottiePath
    val allXs =
      intersectPath.path.flatMap { it.vertices.map { v -> v[0].constantValueOrNull ?: 0f } }
    val allYs =
      intersectPath.path.flatMap { it.vertices.map { v -> v[1].constantValueOrNull ?: 0f } }

    assertThat(allXs.minOrNull()).isWithin(0.1f).of(50f)
    assertThat(allXs.maxOrNull()).isWithin(0.1f).of(100f)
    assertThat(allYs.minOrNull()).isWithin(0.1f).of(50f)
    assertThat(allYs.maxOrNull()).isWithin(0.1f).of(100f)
  }

  @Test
  fun evaluateMergePaths_modeExcludeIntersections_computesXor() {
    val path1 = createRectPath(0f, 0f, 100f, 100f)
    val path2 = createRectPath(50f, 0f, 150f, 100f)

    val mergeModifier = MergePaths(mode = MergeMode.ExcludeIntersections)
    val result: List<RemoteShape> =
      evaluateMergePaths(listOf(path1, path2), mergeModifier, settings)

    assertThat(result).hasSize(1)
    val xorPath = result[0] as RemoteLottiePath
    assertThat(xorPath.path).isNotEmpty()
  }

  @Test
  fun evaluateMergePaths_hiddenModifier_returnsUnmodifiedShapes() {
    val path1 = createRectPath(0f, 0f, 50f, 50f)
    val path2 = createRectPath(100f, 100f, 150f, 150f)

    val hiddenModifier = MergePaths(mode = MergeMode.Add, hidden = true)
    val result: List<RemoteShape> =
      evaluateMergePaths(listOf(path1, path2), hiddenModifier, settings)

    assertThat(result).hasSize(2)
  }

  @Test
  fun evaluateMergePaths_emptyShapes_returnsEmptyList() {
    val mergeModifier = MergePaths(mode = MergeMode.Add)
    val result: List<RemoteShape> = evaluateMergePaths(emptyList(), mergeModifier, settings)

    assertThat(result).isEmpty()
  }

  @Test
  fun gatherShapes_withMergePathsInPipeline_combinesGeometriesBeforeFill() {
    val rect1 =
      Rectangle(
        position = StaticPositionProperty(value = listOf(0f, 0f)),
        size = StaticVectorProperty(value = listOf(100f, 100f)),
        cornerRadius = StaticScalarProperty(value = 0f),
      )
    val rect2 =
      Rectangle(
        position = StaticPositionProperty(value = listOf(50f, 0f)),
        size = StaticVectorProperty(value = listOf(100f, 100f)),
        cornerRadius = StaticScalarProperty(value = 0f),
      )
    val mergePaths = MergePaths(mode = MergeMode.Add)
    val fill = Fill(color = StaticColorProperty(value = Color.Red.rc))

    val elements: List<GraphicElement> = listOf(rect1, rect2, mergePaths, fill)
    val styledGroups = gatherShapesForTest(elements, settings)

    assertThat(styledGroups).hasSize(1)
    assertThat(styledGroups[0].shapes).hasSize(1)
  }
}
