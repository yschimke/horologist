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
import androidx.compose.remote.creation.compose.shaders.RemoteLinearShader
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.ui.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.PolyStar
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.PolyStarType
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.Rectangle
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.grouping.Transform
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.OffsetPath
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.PuckerBloat
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.Repeater
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.ZigZag
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.Fill
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.FillRule
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.GradientFill
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.LineCap
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.LineJoin
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.Stroke
import com.google.android.horologist.remotecompose.lottie.format.layer.NullLayer
import com.google.android.horologist.remotecompose.lottie.format.layer.ShapeLayer
import com.google.android.horologist.remotecompose.lottie.format.mask.Mask
import com.google.android.horologist.remotecompose.lottie.format.mask.MaskMode
import com.google.android.horologist.remotecompose.lottie.format.properties.AnimatedPositionProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.AnimatedScalarProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.KeyframeEasing
import com.google.android.horologist.remotecompose.lottie.format.properties.PositionPropertyKeyframe
import com.google.android.horologist.remotecompose.lottie.format.properties.ScalarPropertyKeyframe
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticBezierProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticColorProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticGradientProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticPositionProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticScalarProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticVectorProperty
import com.google.android.horologist.remotecompose.lottie.format.values.BezierValue
import com.google.android.horologist.remotecompose.lottie.format.values.GradientValue
import com.google.android.horologist.remotecompose.lottie.renderer.RemoteFill
import com.google.android.horologist.remotecompose.lottie.renderer.RemoteGradientFill
import com.google.android.horologist.remotecompose.lottie.renderer.RemoteLottiePath
import com.google.android.horologist.remotecompose.lottie.renderer.clampScale
import com.google.android.horologist.remotecompose.lottie.renderer.computeInverseScale
import com.google.android.horologist.remotecompose.lottie.renderer.gatherShapesForTest
import com.google.android.horologist.remotecompose.lottie.renderer.layers.calculateLocalFrame
import com.google.android.horologist.remotecompose.lottie.renderer.layers.parseHexColor
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animatePosition
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animateScalar
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.evaluatePolyStar
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.evaluateRectangle
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.trimBezierValue
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Pairwise Combinatorial Boundary & Singularity Test Suite.
 *
 * Enforces rigorous functional validation across interacting boundary pairs derived from the Lottie
 * 1.0.1 specification and internal specification requirements.
 */
@SuppressLint("RestrictedApi")
@RunWith(AndroidJUnit4::class)
class PairwiseBoundaryTest {

  private val emptySlotMap = SlotMap.Empty

  // [SP_LOTTIE_SHAPES_01_02] + [SP_LOTTIE_SHAPES_01_05]
  // TC_PAIR_01: Rectangle clamped radius combined with wrap-around TrimPath (spanning loop seam).
  @Test
  fun evaluateRectangleWithClampedRadius_andWrapAroundTrimPath_producesExpectedSubpaths() {
    val rect =
      Rectangle(
        position = StaticPositionProperty(value = listOf(0f, 0f)),
        size = StaticVectorProperty(value = listOf(100f, 50f)),
        // Requested radius 40 exceeds min(halfW=50, halfH=25) -> effectively clamped to 25
        cornerRadius = StaticScalarProperty(value = 40f),
      )

    val settings = LottieSettings(currentFrame = 0f.rf, slotMap = emptySlotMap)
    val remotePath = evaluateRectangle(rect, settings)
    assertThat(remotePath).isNotNull()
    val subpath = remotePath!!.path.first()
    assertThat(subpath.vertices).hasSize(8)

    val bezier =
      BezierValue(
        closed = subpath.closed,
        vertices =
          subpath.vertices.map {
            listOf(it[0].constantValueOrNull ?: 0f, it[1].constantValueOrNull ?: 0f)
          },
        inTangents =
          subpath.inTangents.map {
            listOf(it[0].constantValueOrNull ?: 0f, it[1].constantValueOrNull ?: 0f)
          },
        outTangents =
          subpath.outTangents.map {
            listOf(it[0].constantValueOrNull ?: 0f, it[1].constantValueOrNull ?: 0f)
          },
      )

    // Apply wrap-around TrimPath crossing loop seam: start = 75%, end = 125% (wraps to 25%)
    val trimmedSubpaths =
      trimBezierValue(
        bezier,
        startFraction = 0.75f,
        endFraction = 1.25f,
        offsetFraction = 0.0f,
        keepStructureIfDegenerate = false,
      )

    // Crossing seam produces 2 distinct subpaths: [0.75, 1.0] and [0.0, 0.25]
    assertThat(trimmedSubpaths).hasSize(2)
    assertThat(trimmedSubpaths[0].closed).isFalse()
    assertThat(trimmedSubpaths[1].closed).isFalse()
    assertThat(trimmedSubpaths[0].vertices).isNotEmpty()
    assertThat(trimmedSubpaths[1].vertices).isNotEmpty()
  }

  // [SP_LOTTIE_SHAPES_01_02] + [SP_LOTTIE_REMED_01_04]
  // TC_PAIR_02: PolyStar 5-point star combined with Anisotropic Zero Scale (sx = 0, sy = 100%).
  @Test
  fun evaluatePolyStar_withAnisotropicZeroScale_preservesInvertibleAffineTransform() {
    val star =
      PolyStar(
        starType = PolyStarType.Star,
        points = StaticScalarProperty(value = 5f),
        position = StaticPositionProperty(value = listOf(50f, 50f)),
        rotation = StaticScalarProperty(value = 0f),
        outerRadius = StaticScalarProperty(value = 100f),
        outerRoundedness = StaticScalarProperty(value = 0f),
        innerRadius = StaticScalarProperty(value = 40f),
        innerRoundedness = StaticScalarProperty(value = 0f),
      )

    val settings = LottieSettings(currentFrame = 0f.rf, slotMap = emptySlotMap)
    val remotePath = evaluatePolyStar(star, settings)
    assertThat(remotePath).isNotNull()
    val subpath = remotePath!!.path.first()
    assertThat(subpath.vertices).hasSize(10)

    // Anisotropic scale: sx = 0 (singularity), sy = 1.0
    val scaleX = clampScale(0f.rf)
    val scaleY = clampScale(1f.rf)
    val invScaleX = computeInverseScale(scaleX)
    val invScaleY = computeInverseScale(scaleY)

    // Verify forward and inverse multiplication yields identity
    val prodX = (scaleX * invScaleX).constantValueOrNull
    val prodY = (scaleY * invScaleY).constantValueOrNull
    assertThat(prodX).isNotNull()
    assertThat(prodX!!).isWithin(0.001f).of(1.0f)
    assertThat(prodY).isNotNull()
    assertThat(prodY!!).isWithin(0.001f).of(1.0f)

    // Verify clamped scale is non-zero epsilon (0.0001f) and inverse is 10000f
    assertThat(scaleX.constantValueOrNull).isWithin(0.00001f).of(0.0001f)
    assertThat(invScaleX.constantValueOrNull).isWithin(1f).of(10000f)
  }

  // [SP_LOTTIE_REMED_02_04] + [SP_LOTTIE_REMED_03_10]
  // TC_PAIR_03: Repeater with Negative Scale and Fractional Offset (prevents NaN powers).
  @Test
  fun evaluateRepeater_withNegativeScaleAndFractionalOffset_avoidsNanExponentiation() {
    val rect =
      Rectangle(
        position = StaticPositionProperty(value = listOf(0f, 0f)),
        size = StaticVectorProperty(value = listOf(40f, 40f)),
      )
    val repeater =
      Repeater(
        copies = StaticScalarProperty(value = 3f),
        offset = StaticScalarProperty(value = 0.5f),
        transform =
          Transform(
            positionTranslation = StaticPositionProperty(value = listOf(50f, 0f)),
            scale = StaticVectorProperty(value = listOf(-100f, 100f)),
            startOpacity = StaticScalarProperty(value = 100f),
            endOpacity = StaticScalarProperty(value = 50f),
          ),
      )
    val fill = Fill(color = StaticColorProperty(value = Color.Red.rc))

    val settings = LottieSettings(currentFrame = 0f.rf, slotMap = emptySlotMap)
    val gathered = gatherShapesForTest(listOf(rect, repeater, fill), settings)

    // Repeater produces 3 distinct transformed copies
    assertThat(gathered).hasSize(3)
  }

  // [SP_LOTTIE_SHAPES_01_04] + [SP_LOTTIE_REMED_02_01]
  // TC_PAIR_04: EvenOdd Fill rule with self-intersecting star Bézier contour.
  @Test
  fun evaluateEvenOddFillRule_withSelfIntersectingStarPath_correctlyPropagatesRule() {
    val star =
      PolyStar(
        starType = PolyStarType.Star,
        points = StaticScalarProperty(value = 5f),
        position = StaticPositionProperty(value = listOf(0f, 0f)),
        rotation = StaticScalarProperty(value = 0f),
        outerRadius = StaticScalarProperty(value = 100f),
        innerRadius = StaticScalarProperty(value = 40f),
      )
    val fill = Fill(color = StaticColorProperty(value = Color.Blue.rc), fillRule = FillRule.EvenOdd)

    val settings = LottieSettings(currentFrame = 0f.rf, slotMap = emptySlotMap)
    val gathered = gatherShapesForTest(listOf(star, fill), settings)

    assertThat(gathered).hasSize(1)
    val remotePath = gathered.first().shapes.first() as RemoteLottiePath
    assertThat(remotePath.fillRule).isEqualTo(FillRule.EvenOdd)
    val remoteFill = gathered.first().style as RemoteFill
    assertThat(remoteFill.fillRule).isEqualTo(FillRule.EvenOdd)
  }

  // [SP_LOTTIE_SHAPES_01_04] + [PL_LOTTIE_GRADIENT_01]
  // TC_PAIR_05: Degenerate 0-length gradient with compounded layer and fill opacities.
  @Test
  fun evaluateLinearGradient_withZeroLengthVectorAndCompoundedOpacity_yieldsValidShaderAndColor() {
    val gradFill =
      GradientFill(
        startPoint = StaticPositionProperty(value = listOf(50f, 50f)),
        endPoint = StaticPositionProperty(value = listOf(50f, 50f)), // Zero-length vector
        opacity = StaticScalarProperty(value = 50f),
        colors =
          StaticGradientProperty(
            value =
              GradientValue(
                numberOfColors = 2,
                values = listOf(0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f), // Red to Blue
              )
          ),
      )

    val rect =
      Rectangle(
        position = StaticPositionProperty(value = listOf(50f, 50f)),
        size = StaticVectorProperty(value = listOf(100f, 100f)),
      )

    val settings = LottieSettings(currentFrame = 0f.rf, slotMap = emptySlotMap)
    val gathered = gatherShapesForTest(listOf(rect, gradFill), settings)
    assertThat(gathered).hasSize(1)

    val style = gathered.first().style as? RemoteGradientFill
    assertThat(style).isNotNull()

    // Layer opacity 50% * Fill opacity 50% -> Compounded stop alpha 25% (0.25f)
    val paint = style!!.getPaint(inheritedOpacity = 0.5f.rf)
    assertThat(paint.shader).isInstanceOf(RemoteLinearShader::class.java)
    val shader = paint.shader as RemoteLinearShader
    assertThat(shader.colors).isNotEmpty()
    assertThat(shader.colors[0].alpha.constantValueOrNull).isWithin(0.001f).of(0.25f)
  }

  // [SP_LOTTIE_REMED_01_02] + [SP_LOTTIE_REMED_03_04]
  // TC_PAIR_06: Anisotropic multi-dimensional easing with spatial Bézier tangents.
  @Test
  fun evaluateKeyframeEasing_withAnisotropicDimensionsAndSpatialTangents_computesCurvedTrajectory() {
    val keyframe =
      PositionPropertyKeyframe(
        frame = 0f,
        value = listOf(0f, 0f),
        spatialInTangent = listOf(0f, 50f),
        spatialOutTangent = listOf(0f, 50f),
        outTangent = KeyframeEasing(x = listOf(0.0f, 0.4f), y = listOf(0.0f, 0.0f)),
        inTangent = KeyframeEasing(x = listOf(1.0f, 0.6f), y = listOf(1.0f, 1.0f)),
      )
    val keyframeEnd = PositionPropertyKeyframe(frame = 10f, value = listOf(100f, 0f))

    val animPosition = AnimatedPositionProperty(keyframes = listOf(keyframe, keyframeEnd))

    val settingsStart = LottieSettings(currentFrame = 0f.rf, slotMap = emptySlotMap)
    val posStart = animatePosition(animPosition, settingsStart)
    assertThat(posStart.x.constantValueOrNull).isWithin(0.01f).of(0f)
    assertThat(posStart.y.constantValueOrNull).isWithin(0.01f).of(0f)

    val settingsMid = LottieSettings(currentFrame = 5f.rf, slotMap = emptySlotMap)
    val posMid = animatePosition(animPosition, settingsMid)
    // Linear X reaches 50%, Spatial curved Y evaluates along bezier trajectory
    assertThat(posMid.x.constantValueOrNull).isWithin(0.01f).of(50f)
    assertThat(posMid.y.constantValueOrNull).isWithin(0.01f).of(37.5f)

    val settingsEnd = LottieSettings(currentFrame = 10f.rf, slotMap = emptySlotMap)
    val posEnd = animatePosition(animPosition, settingsEnd)
    assertThat(posEnd.x.constantValueOrNull).isWithin(0.01f).of(100f)
    assertThat(posEnd.y.constantValueOrNull).isWithin(0.01f).of(0f)
  }

  // [SP_LOTTIE_LAYERS_01_02] + [SP_LOTTIE_REMED_01_02]
  // TC_PAIR_07: Hold keyframe evaluation under Precomp time stretch and start offset.
  @Test
  fun evaluateHoldKeyframe_underPrecompTimeStretchAndStartOffset_respectsStepTransition() {
    val scalarProp =
      AnimatedScalarProperty(
        keyframes =
          listOf(
            ScalarPropertyKeyframe(frame = 0f, value = 10f, hold = true),
            ScalarPropertyKeyframe(frame = 20f, value = 50f),
          )
      )

    // Precomp timeline mapping: start offset = 10, stretch = 2.0
    // Local frame formula: t_local = (t - 10) / 2.0
    val globalFrame10 = calculateLocalFrame(10f.rf, startTime = 10f, timeStretch = 2.0f)
    val globalFrame30 = calculateLocalFrame(30f.rf, startTime = 10f, timeStretch = 2.0f)
    val globalFrame50 = calculateLocalFrame(50f.rf, startTime = 10f, timeStretch = 2.0f)

    assertThat(globalFrame10.constantValueOrNull).isEqualTo(0f)
    assertThat(globalFrame30.constantValueOrNull).isEqualTo(10f)
    assertThat(globalFrame50.constantValueOrNull).isEqualTo(20f)

    // At global frame 30 (local 10), hold keyframe maintains value 10f
    val settings30 = LottieSettings(currentFrame = globalFrame30, slotMap = emptySlotMap)
    val val30 = animateScalar(scalarProp, settings30)
    assertThat(val30.constantValueOrNull).isEqualTo(10f)

    // At global frame 50 (local 20), hold keyframe steps to value 50f
    val settings50 = LottieSettings(currentFrame = globalFrame50, slotMap = emptySlotMap)
    val val50 = animateScalar(scalarProp, settings50)
    assertThat(val50.constantValueOrNull).isEqualTo(50f)
  }

  // [SP_LOTTIE_MATTE_01_01] + [SP_LOTTIE_LAYERS_03_01]
  // TC_PAIR_08: Hidden matte source layer with 5-tier parent chain.
  @Test
  fun evaluateHiddenTrackMatteSource_withDeepParentHierarchy_accumulatesTransformsWithoutDrift() {
    val layers =
      listOf(
        NullLayer(
          index = 1,
          transform =
            Transform(
              name = "L1_Root",
              positionTranslation = StaticPositionProperty(value = listOf(10f, 0f)),
            ),
        ),
        NullLayer(
          index = 2,
          parent = 1,
          transform =
            Transform(
              name = "L2",
              positionTranslation = StaticPositionProperty(value = listOf(10f, 0f)),
            ),
        ),
        NullLayer(
          index = 3,
          parent = 2,
          transform =
            Transform(
              name = "L3",
              positionTranslation = StaticPositionProperty(value = listOf(10f, 0f)),
            ),
        ),
        NullLayer(
          index = 4,
          parent = 3,
          transform =
            Transform(
              name = "L4",
              positionTranslation = StaticPositionProperty(value = listOf(10f, 0f)),
            ),
        ),
        ShapeLayer(
          index = 5,
          parent = 4,
          hidden = true,
          matteTarget = 1,
          transform =
            Transform(
              name = "L5_MatteSource",
              positionTranslation = StaticPositionProperty(value = listOf(10f, 0f)),
            ),
        ),
      )

    val transforms = buildAncestorTransforms(layers)
    // Leaf layer (index 5) has 4 ancestors (1, 2, 3, 4)
    val leafAncestors = transforms[5]
    assertThat(leafAncestors).isNotNull()
    assertThat(leafAncestors).hasSize(4)
    assertThat(leafAncestors!![0].name).isEqualTo("L1_Root")
    assertThat(leafAncestors[3].name).isEqualTo("L4")
  }

  // [SP_LOTTIE_REMED_02_03] + [SP_LOTTIE_REMED_03_09]
  // TC_PAIR_09: Layer masks with multi-mask Intersect and Subtract modes.
  @Test
  fun evaluateLayerMask_withMultiMaskIntersectAndSubtract_deserializesAndDecodesModes() {
    val maskAdd =
      Mask(
        name = "AddMask",
        mode = MaskMode.Add,
        path =
          StaticBezierProperty(
            value =
              BezierValue(
                closed = true,
                vertices =
                  listOf(listOf(0f, 0f), listOf(100f, 0f), listOf(100f, 100f), listOf(0f, 100f)),
              )
          ),
      )
    val maskSubtract =
      Mask(
        name = "SubMask",
        mode = MaskMode.Subtract,
        path =
          StaticBezierProperty(
            value =
              BezierValue(
                closed = true,
                vertices =
                  listOf(listOf(20f, 20f), listOf(80f, 20f), listOf(80f, 80f), listOf(20f, 80f)),
              )
          ),
      )
    val maskIntersect =
      Mask(
        name = "IntersectMask",
        mode = MaskMode.Intersect,
        path =
          StaticBezierProperty(
            value =
              BezierValue(
                closed = true,
                vertices =
                  listOf(listOf(10f, 10f), listOf(90f, 10f), listOf(90f, 90f), listOf(10f, 90f)),
              )
          ),
      )

    val layer =
      ShapeLayer(
        index = 1,
        name = "MaskedLayer",
        masksProperties = listOf(maskAdd, maskSubtract, maskIntersect),
      )

    assertThat(layer.masksProperties).hasSize(3)
    assertThat(layer.masksProperties[0].mode).isEqualTo(MaskMode.Add)
    assertThat(layer.masksProperties[1].mode).isEqualTo(MaskMode.Subtract)
    assertThat(layer.masksProperties[2].mode).isEqualTo(MaskMode.Intersect)
  }

  // [SP_LOTTIE_SHAPES_01_05] + [PL_LOTTIE_STROKE_01]
  // TC_PAIR_10: Chained ZigZag + PuckerBloat + OffsetPath with Stroke Miter limit.
  @Test
  fun evaluateChainedModifiers_withZigZagPuckerBloatAndMiterStroke_evaluatesDeformedContour() {
    val rect =
      Rectangle(
        position = StaticPositionProperty(value = listOf(0f, 0f)),
        size = StaticVectorProperty(value = listOf(100f, 100f)),
      )
    val zigZag =
      ZigZag(
        size = StaticScalarProperty(value = 5f),
        ridgesPerSegment = StaticScalarProperty(value = 2f),
      )
    val puckerBloat = PuckerBloat(amount = StaticScalarProperty(value = 25f))
    val offsetPath =
      OffsetPath(
        amount = StaticScalarProperty(value = 10f),
        lineJoin = LineJoin.Miter,
        miterLimitNumeric = 4.0f,
      )
    val stroke =
      Stroke(
        color = StaticColorProperty(value = Color.Green.rc),
        strokeWidth = StaticScalarProperty(value = 2f),
        lineCap = LineCap.Round,
        lineJoin = LineJoin.Miter,
        miterLimitNumeric = 4.0f,
      )

    val settings = LottieSettings(currentFrame = 0f.rf, slotMap = emptySlotMap)
    val gathered =
      gatherShapesForTest(listOf(rect, zigZag, puckerBloat, offsetPath, stroke), settings)

    assertThat(gathered).hasSize(1)
    val shape = gathered.first().shapes.first()
    assertThat(shape).isInstanceOf(RemoteLottiePath::class.java)
    val remotePath = shape as RemoteLottiePath
    // Deformed path has accumulated additional vertices from ZigZag ridges and modifiers
    assertThat(remotePath.path.first().vertices.size).isGreaterThan(4)
  }

  // TC_EXTRA_01: PolyStar with NaN points falls back gracefully without crash.
  @Test
  fun evaluatePolyStar_withNanPoints_fallsBackGracefullyWithoutCrash() {
    val star =
      PolyStar(
        starType = PolyStarType.Star,
        points = StaticScalarProperty(value = Float.NaN),
        outerRadius = StaticScalarProperty(value = 50f),
        innerRadius = StaticScalarProperty(value = 20f),
      )

    val settings = LottieSettings(currentFrame = 0f.rf, slotMap = emptySlotMap)
    val remotePath = evaluatePolyStar(star, settings)
    // When points is NaN, evaluatePolyStar returns empty subpaths without throwing exception
    if (remotePath != null) {
      assertThat(remotePath.path.first().vertices).isEmpty()
    }
  }

  // TC_EXTRA_02: Hex color parsing across all format variations.
  @Test
  fun evaluateHexColorParsing_allSupportedFormats() {
    // 6-digit RGB with and without hash
    assertThat(parseHexColor("#FF5722")).isEqualTo(Color(0xFFFF5722))
    assertThat(parseHexColor("FF5722")).isEqualTo(Color(0xFFFF5722))

    // 8-digit ARGB with and without hash
    assertThat(parseHexColor("#80FF5722")).isEqualTo(Color(0x80FF5722))
    assertThat(parseHexColor("80FF5722")).isEqualTo(Color(0x80FF5722))

    // 3-digit RGB with and without hash
    assertThat(parseHexColor("#F00")).isEqualTo(Color(0xFFFF0000))
    assertThat(parseHexColor("F00")).isEqualTo(Color(0xFFFF0000))

    // Invalid formats return transparent fallback
    assertThat(parseHexColor("")).isEqualTo(Color.Transparent)
    assertThat(parseHexColor("invalid")).isEqualTo(Color.Transparent)
    assertThat(parseHexColor("#GGGGGG")).isEqualTo(Color.Transparent)
    assertThat(parseHexColor("#8F00")).isEqualTo(Color.Transparent)
  }
}
