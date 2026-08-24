# Implementation Plan: Gradient Property Specification Compliance & Modular Architecture {#PL_LOTTIE_GRADIENT}

> **Code:** PL_LOTTIE_GRADIENT
> **Status:** completed
> **Created:** 2026-08-22
> **Updated:** 2026-08-22
>
> **Concept:** [Format](format.concept.md), [Renderer](renderer.concept.md)
> **Specification:** [Format](format.sp.md), [Renderer](renderer.sp.md)
> **Specification Reference:** [Lottie 1.0.1 Gradient / Value Property](https://lottie.github.io/lottie-spec/1.0.1/single-page/#specs-values-gradient-colors)
> **Depends on:** none
> **Used by:** `renderer`, `format`
>
> Implementation plan for Lottie Gradient property compliance with the Lottie 1.0.1 specification and modular architecture: domain value modeling in `format/values/Gradient.kt` (`GradientValue` handling opaque $4 \times p$ and transparent $4 \times p + 2 \times q$ stops), AST property modeling in `format/properties/Gradient.kt`, custom deserializers supporting flexible gradient value formats (raw float arrays, nested `k`/`p` objects, slot IDs `sid`, hold keyframes `h`, easing tangents `i`/`o`), dynamic expression tree frame interpolation with hold keyframes and delayed starts in `renderer/properties/Gradient.kt`, updating consumers, and verification.

## Goal

Ensure compliance for Lottie Gradient properties according to the [Lottie 1.0.1 specification](https://lottie.github.io/lottie-spec/1.0.1/single-page/#specs-values-gradient-colors):
1. Align AST data models with the Lottie 1.0.1 specification under `com.google.android.horologist.remotecompose.lottie.format.values` and `com.google.android.horologist.remotecompose.lottie.format.properties`.
2. Extract domain shape payload `GradientValue` into `format/values/Gradient.kt`, supporting both opaque color stops ($4 \times p$) and trailing opacity stops ($2 \times q$).
3. Extract animatable property wrappers `BaseGradientProperty`, `StaticGradientProperty`, `AnimatedGradientProperty`, and `GradientPropertyKeyframe` into `format/properties/Gradient.kt`.
4. Store color components as normalized `Float`s in AST (`List<Float>`) and `RemoteFloat`s in renderer (`List<RemoteFloat>`) for seamless keyframe `lerp` interpolation, with lazy resolution to `androidx.compose.ui.graphics.Color`.
5. Support flexible deserialization (flat float arrays `[offset, r, g, b, ...]`, nested objects with `p` color count and `k` values, slot IDs `sid`, hold keyframes `h` as boolean or integer, and tangent easing representations).
6. Decouple renderer architecture into `com.google.android.horologist.remotecompose.lottie.renderer.properties.Gradient.kt` with dynamic `RemoteFloat` expression tree evaluations and `RemoteGradientValue`.
7. Implement keyframe interpolation and chaining engine for multi-stop gradient color and opacity values (cubic Bézier easing curves, linear interpolation `lerp`, delayed start, hold keyframes).
8. Update consumers and shared helpers across format and renderer packages.
9. Verify with comprehensive unit testing, format checks, and Roborazzi screenshot verification.

---

## Specification Alignment (Lottie 1.0.1)

| Lottie Spec Element | Spec Section | Format AST Representation | Renderer Representation |
|---|---|---|---|
| **Opaque Gradient Colors** | `#specs-values-gradient-colors` | `GradientValue(p, values)` ($4 \times p$ floats) | `RemoteGradientValue(p, values)` |
| **Transparent Gradient Colors** | `#specs-values-gradient-colors` | `GradientValue(p, values)` ($4p + 2q$ floats) | `RemoteGradientValue(p, values)` |
| **Gradient Property** | `#specs-properties-gradient-colors` | `BaseGradientProperty` (`StaticGradientProperty` / `AnimatedGradientProperty`) | `animateGradient(...) -> RemoteGradientValue` |
| **Gradient Keyframe** | `#specs-properties-gradient-keyframe` | `GradientPropertyKeyframe` (`t`, `s`, `i`, `o`, `h`) | `GradientAnimationSegment` + easing curve |
| **Keyframe Easing** | `#specs-properties-keyframe-easing` | `ScalarKeyframeEasing` (`x`, `y` each float or array) | `CubicBezierEasing` |
| **Slot ID** | `#specs-helpers-slottable-property` | `sid: String?` | Resolved via `LottieSettings.slotMap` |
| **Hold Keyframe** | `#specs-properties-base-keyframe` | `h: Boolean` (deserialized from `0`/`1`/`boolean`) | `selectIfLt` hold branch |

---

## Architectural Design & Data Models

### 1. Format Values Layer (`com.google.android.horologist.remotecompose.lottie.format.values.Gradient.kt`)

```kotlin
package com.google.android.horologist.remotecompose.lottie.format.values

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ColorStop(val offset: Float, val red: Float, val green: Float, val blue: Float)

@Serializable
internal data class OpacityStop(val offset: Float, val alpha: Float)

@Serializable
internal data class ResolvedColorStop(val offset: Float, val color: Color)

@Serializable(with = GradientValueSerializer::class)
internal data class GradientValue(
  @SerialName("p") val numberOfColors: Int = 0,
  @SerialName("k") val values: List<Float> = emptyList(),
) {
  val hasTransparency: Boolean
    get() = values.size > numberOfColors * 4

  val colorStops: List<ColorStop>
    get() {
      val stops = mutableListOf<ColorStop>()
      val colorCount = if (numberOfColors > 0) numberOfColors else values.size / 4
      for (i in 0 until colorCount) {
        val base = i * 4
        if (base + 3 < values.size) {
          stops.add(
            ColorStop(
              offset = values[base],
              red = normalizeColorComponent(values[base + 1]),
              green = normalizeColorComponent(values[base + 2]),
              blue = normalizeColorComponent(values[base + 3]),
            )
          )
        }
      }
      return stops
    }

  val opacityStops: List<OpacityStop>
    get() {
      val stops = mutableListOf<OpacityStop>()
      val colorCount = if (numberOfColors > 0) numberOfColors else values.size / 4
      val opacityOffset = colorCount * 4
      var i = opacityOffset
      while (i + 1 < values.size) {
        stops.add(
          OpacityStop(
            offset = values[i],
            alpha = normalizeColorComponent(values[i + 1]),
          )
        )
        i += 2
      }
      return stops
    }

  fun resolveStops(): List<ResolvedColorStop> {
    val cStops = colorStops
    val oStops = opacityStops
    if (cStops.isEmpty()) return emptyList()
    if (oStops.isEmpty()) {
      return cStops.map {
        ResolvedColorStop(
          offset = it.offset,
          color = Color(red = it.red, green = it.green, blue = it.blue, alpha = 1f),
        )
      }
    }

    val allOffsets = (cStops.map { it.offset } + oStops.map { it.offset }).distinct().sorted()
    return allOffsets.map { offset ->
      val (r, g, b) = interpolateRgbAt(cStops, offset)
      val alpha = interpolateAlphaAt(oStops, offset)
      ResolvedColorStop(offset = offset, color = Color(red = r, green = g, blue = b, alpha = alpha))
    }
  }

  private fun normalizeColorComponent(v: Float): Float =
    if (v > 1f) (v / 255f).coerceIn(0f, 1f) else v.coerceIn(0f, 1f)

  private fun interpolateRgbAt(stops: List<ColorStop>, offset: Float): Triple<Float, Float, Float> {
    if (stops.size == 1 || offset <= stops.first().offset) {
      val first = stops.first()
      return Triple(first.red, first.green, first.blue)
    }
    if (offset >= stops.last().offset) {
      val last = stops.last()
      return Triple(last.red, last.green, last.blue)
    }
    for (i in 0 until stops.size - 1) {
      val s1 = stops[i]
      val s2 = stops[i + 1]
      if (offset >= s1.offset && offset <= s2.offset) {
        val range = s2.offset - s1.offset
        val factor = if (range == 0f) 0f else (offset - s1.offset) / range
        val r = s1.red + (s2.red - s1.red) * factor
        val g = s1.green + (s2.green - s1.green) * factor
        val b = s1.blue + (s2.blue - s1.blue) * factor
        return Triple(r, g, b)
      }
    }
    val last = stops.last()
    return Triple(last.red, last.green, last.blue)
  }

  private fun interpolateAlphaAt(stops: List<OpacityStop>, offset: Float): Float {
    if (stops.isEmpty()) return 1f
    if (stops.size == 1 || offset <= stops.first().offset) return stops.first().alpha
    if (offset >= stops.last().offset) return stops.last().alpha
    for (i in 0 until stops.size - 1) {
      val s1 = stops[i]
      val s2 = stops[i + 1]
      if (offset >= s1.offset && offset <= s2.offset) {
        val range = s2.offset - s1.offset
        val factor = if (range == 0f) 0f else (offset - s1.offset) / range
        return s1.alpha + (s2.alpha - s1.alpha) * factor
      }
    }
    return stops.last().alpha
  }
}
```

### 2. Format Properties Layer (`com.google.android.horologist.remotecompose.lottie.format.properties.Gradient.kt`)

```kotlin
package com.google.android.horologist.remotecompose.lottie.format.properties

import com.google.android.horologist.remotecompose.lottie.format.values.GradientValue
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable(with = BaseGradientPropertySerializer::class)
internal sealed class BaseGradientProperty {
  abstract val animated: Boolean
  abstract val slotId: String?
}

@Serializable(with = StaticGradientPropertySerializer::class)
internal data class StaticGradientProperty(
  @SerialName("sid") override val slotId: String? = null,
  override val animated: Boolean = false,
  @SerialName("k") val value: GradientValue = GradientValue(),
) : BaseGradientProperty()

@Serializable(with = AnimatedGradientPropertySerializer::class)
internal data class AnimatedGradientProperty(
  @SerialName("sid") override val slotId: String? = null,
  @SerialName("p") val numberOfColors: Int? = null,
  @SerialName("a") val animatedInt: Int = 1,
  @SerialName("k") val keyframes: List<GradientPropertyKeyframe> = emptyList(),
) : BaseGradientProperty() {
  override val animated: Boolean
    get() = animatedInt == 1
}

@Serializable(with = GradientPropertyKeyframeSerializer::class)
internal data class GradientPropertyKeyframe(
  @SerialName("t") val frame: Float = 0f,
  @SerialName("h") val hold: Boolean = false,
  @SerialName("i") val inTangent: ScalarKeyframeEasing? = null,
  @SerialName("o") val outTangent: ScalarKeyframeEasing? = null,
  @SerialName("s") val value: List<GradientValue> = emptyList(),
)
```

### 3. Renderer Layer (`com.google.android.horologist.remotecompose.lottie.renderer.properties.Gradient.kt`)

```kotlin
package com.google.android.horologist.remotecompose.lottie.renderer.properties

import android.annotation.SuppressLint
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.lerp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.selectIfLt
import com.google.android.horologist.remotecompose.lottie.LottieSettings
import com.google.android.horologist.remotecompose.lottie.format.properties.AnimatedGradientProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.BaseGradientProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticGradientProperty
import com.google.android.horologist.remotecompose.lottie.format.values.GradientValue
import com.google.android.horologist.remotecompose.lottie.renderer.lookupValueInBezier
import com.google.android.horologist.remotecompose.lottie.renderer.scalarLinearEasingIn
import com.google.android.horologist.remotecompose.lottie.renderer.scalarLinearEasingOut

internal data class RemoteGradientValue(
  val numberOfColors: Int,
  val values: List<RemoteFloat>,
) {
  val hasTransparency: Boolean
    get() = values.size > numberOfColors * 4
}

internal data class GradientAnimationSegment(
  val startFrame: Float,
  val value: List<RemoteFloat>,
)

internal fun GradientValue.toRemote(): RemoteGradientValue {
  return RemoteGradientValue(
    numberOfColors = numberOfColors,
    values = values.map { it.rf },
  )
}

@SuppressLint("RestrictedApi")
internal fun animateGradient(
  gradient: BaseGradientProperty,
  animationSettings: LottieSettings,
): RemoteGradientValue {
  return when (gradient) {
    is StaticGradientProperty -> gradient.value.toRemote()
    is AnimatedGradientProperty -> {
      if (gradient.keyframes.isEmpty()) {
        return RemoteGradientValue(numberOfColors = gradient.numberOfColors ?: 0, values = emptyList())
      }
      if (gradient.keyframes.size == 1) {
        val single = gradient.keyframes[0].value.firstOrNull() ?: GradientValue(numberOfColors = gradient.numberOfColors ?: 0)
        return single.toRemote()
      }

      val firstKeyframe = gradient.keyframes[0]
      val firstVal = firstKeyframe.value.firstOrNull() ?: GradientValue(numberOfColors = gradient.numberOfColors ?: 0)
      val numberOfColors = gradient.numberOfColors ?: firstVal.numberOfColors

      val animationSegments = mutableListOf<GradientAnimationSegment>()
      if (firstKeyframe.frame != 0f) {
        animationSegments.add(
          GradientAnimationSegment(
            startFrame = 0f,
            value = firstVal.values.map { it.rf },
          )
        )
      }

      for (i in 0 until gradient.keyframes.size - 1) {
        val startKeyframe = gradient.keyframes[i]
        val endKeyframe = gradient.keyframes[i + 1]
        val duration = endKeyframe.frame - startKeyframe.frame
        val frameInAnimation = animationSettings.currentFrame - startKeyframe.frame

        val startGradient = startKeyframe.value.firstOrNull() ?: firstVal
        val endGradient = endKeyframe.value.firstOrNull() ?: startGradient

        val segmentValues =
          if (startKeyframe.hold) {
            startGradient.values.mapIndexed { index, startCoord ->
              val endCoord = endGradient.values.getOrElse(index) { startCoord }
              selectIfLt(frameInAnimation, duration.rf, startCoord.rf, endCoord.rf)
            }
          } else {
            val outTangent = startKeyframe.outTangent ?: scalarLinearEasingOut
            val inTangent = startKeyframe.inTangent ?: scalarLinearEasingIn

            val progress =
              lookupValueInBezier(
                outTangent.x,
                outTangent.y,
                inTangent.x,
                inTangent.y,
                duration,
                frameInAnimation,
              )

            startGradient.values.mapIndexed { index, startCoord ->
              val endCoord = endGradient.values.getOrElse(index) { startCoord }
              lerp(startCoord.rf, endCoord.rf, progress)
            }
          }

        animationSegments.add(GradientAnimationSegment(startKeyframe.frame, segmentValues))
      }

      val chainedValues = chainGradientAnimation(animationSegments, animationSettings.currentFrame)
      RemoteGradientValue(numberOfColors = numberOfColors, values = chainedValues)
    }
  }
}

@SuppressLint("RestrictedApi")
private fun chainGradientAnimation(
  segments: List<GradientAnimationSegment>,
  frame: RemoteFloat,
): List<RemoteFloat> {
  if (segments.size == 1) {
    return segments[0].value
  }

  val firstSegment = segments[0]
  val remainingChained = chainGradientAnimation(segments.subList(1, segments.size), frame)
  val nextStartFrame = segments[1].startFrame.rf

  return firstSegment.value.mapIndexed { index, coordVal ->
    val remainingVal = remainingChained.getOrElse(index) { coordVal }
    selectIfLt(frame, nextStartFrame, coordVal, remainingVal)
  }
}
```

---

## Phases

### Phase 1 — Test Harness & Assertions [DONE] {#PL_LOTTIE_GRADIENT_P1}
- Add unit tests in `LottieDecoderResilienceTest.kt` covering:
  - `GradientValue` static decoding from raw float arrays (opaque $4 \times p$ and transparent $4p + 2q$).
  - `GradientValue` static decoding from nested objects with `p` color count and `k` values `{"p": 2, "k": [...]}`.
  - `GradientValue.colorStops`, `opacityStops`, and `resolveStops()` correctness.
  - `StaticGradientProperty` decoding with slot IDs (`sid`).
  - `AnimatedGradientProperty` decoding with keyframes, hold keyframes (`h: 1`, `h: true`), delayed start, and slot IDs (`sid`).
  - Gradient keyframe easing tangents `i` and `o` with numbers and 1-element arrays.
- Add unit tests in `ParsingTest.kt` verifying gradient properties deserialization.
- Add unit tests in `AnimationTest.kt` for `animateGradient`:
  - Static gradient input (opaque & transparent).
  - Empty keyframes fallback.
  - Single keyframe static hold.
  - Multi-keyframe animation interpolation across frames.
  - Hold keyframe evaluation.
  - Delayed start keyframe evaluation.

### Phase 2 — AST Extraction to `format/values/Gradient.kt` & `format/properties/Gradient.kt` [DONE] {#PL_LOTTIE_GRADIENT_P2}
- Create `format/values/Gradient.kt` defining domain geometry model `GradientValue`, `ColorStop`, `OpacityStop`, `ResolvedColorStop`.
- Create `format/properties/Gradient.kt` defining `BaseGradientProperty`, `StaticGradientProperty`, `AnimatedGradientProperty`, and `GradientPropertyKeyframe`.
- Update any consuming elements (`GraphicElement.GradientFill`, `GraphicElement.GradientStroke`).

### Phase 3 — Deserialization Implementation [DONE] {#PL_LOTTIE_GRADIENT_P3}
- Implement `GradientValueSerializer` in `format/values/Gradient.kt` supporting flexible arrays and `{p, k}` objects.
- Implement `BaseGradientPropertySerializer`, `StaticGradientPropertySerializer`, `AnimatedGradientPropertySerializer`, `GradientPropertyKeyframeSerializer` in `format/properties/Gradient.kt`, and `GradientTypeSerializer` in `format/LottieDecoder.kt`.
- Clean up any legacy gradient serializer logic from `format/LottieDecoder.kt`.
- Ensure resilience against flexible Lottie JSON structures (flat arrays, nested objects, booleans/ints for hold flags).

### Phase 4 — Renderer Interpolation & Consumers [DONE] {#PL_LOTTIE_GRADIENT_P4}
- Implement `RemoteGradientValue`, `GradientAnimationSegment`, `animateGradient`, and `chainGradientAnimation` in `renderer/properties/Gradient.kt`.
- Wire consumers into `renderer/Shape.kt` (`gradientFill`, `gradientStroke`) and `renderer/RemoteStyle.kt` (`RemoteGradientFill`, `RemoteGradientStroke`).

### Phase 5 — Verification Suite & Full Check [DONE] {#PL_LOTTIE_GRADIENT_P5}
- Run mandatory verification commands in order:
  1. `./gradlew :remotecompose:lottie:ktfmtFormat`
  2. `./gradlew :remotecompose:lottie:metalavaGenerateSignatureDebug`
  3. `./gradlew :remotecompose:lottie:compileDebugKotlin`
  4. `./gradlew :remotecompose:lottie:assembleDebug`
  5. `./gradlew :remotecompose:lottie:testDebugUnitTest --no-build-cache`
  6. `./gradlew :remotecompose:lottie:check`
  7. `./gradlew :remotecompose:lottie:verifyRoborazziDebug`

---

## Alternatives Considered

### 1. Color Representation in AST and Renderer (`List<Float>` vs `IntArray` / `List<Int>` vs `List<Color>` / `List<RemoteColor>`)
- **Option A (Array of Integers e.g. `IntArray` / `List<Int>` packed ARGB or [0..255] ints)**:
  - *Pros:* Slightly more compact memory representation on JVM.
  - *Cons:* Discards floating-point precision of color offsets and component values; causes quantization errors/color banding when interpolated across keyframes; incompatible with dynamic Remote Compose `RemoteFloat` state graphs.
  - *Verdict:* Rejected.
- **Option B (Pre-converted `List<Color>` / `List<RemoteColor>`)**:
  - *Pros:* Direct access to `Color` / `RemoteColor` instances.
  - *Cons:* Prematurely decouples opacity stops from color stops (in Lottie spec, opacity stops have their own independent offsets that may not align with color stops); requires keyframe interpolation via `tween` which assumes identical stop topologies between keyframes; higher object allocation rate per keyframe.
  - *Verdict:* Rejected.
- **Option C (Flat `List<Float>` in AST & `List<RemoteFloat>` in Renderer with Lazy `Color` Resolution) [Selected]**:
  - *Pros:* 1:1 match with Lottie 1.0.1 specification format (`#specs-values-gradient-colors`); enables smooth, precise, independent component linear interpolation (`lerp`) across timeline keyframes regardless of opacity/color stop offset alignments; supports dynamic Remote Compose state graphs via `RemoteFloat`; provides structured accessors (`colorStops`, `opacityStops`, `resolveStops(): List<ResolvedColorStop>`) to yield `androidx.compose.ui.graphics.Color` for rendering and shader construction.
  - *Verdict:* Chosen.

### 2. `GradientValue` in `format/values/` vs Inlining in `format/properties/`
- **Option A (Inlining in `format/properties/Gradient.kt`)**: Fewer files, but mixes domain value representations with animatable property abstractions, breaking parity with `format/values/Bezier.kt`.
- **Option B (`GradientValue` in `format/values/Gradient.kt`) [Selected]**: Clean separation matching the `format/values/Bezier.kt` architecture, where domain values (`GradientValue`) encapsulate the raw color stops/count and can be tested/serialized independently of animatable properties (`BaseGradientProperty`).

### 3. Flat $4p + 2q$ Array Storage vs Pre-Split Color/Opacity Collections
- **Option A (Pre-splitting into `List<ColorStop>` and `List<OpacityStop>` during deserialization)**: Requires reconstructing the flat array during keyframe interpolation or evaluating complex multi-list lerps across timeline frames.
- **Option B (Flat `values: List<Float>` with lazy decomposition properties) [Selected]**: 1:1 match with Lottie spec keyframe arrays, high-performance parallel component lerp interpolation, and lazy decomposition via `colorStops`, `opacityStops`, and `resolveStops()`.
