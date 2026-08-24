# Implementation Plan: Scalar Property Specification Compliance & Modular Architecture {#PL_LOTTIE_SCALAR}

> **Code:** PL_LOTTIE_SCALAR
> **Status:** completed
> **Created:** 2026-08-22
> **Updated:** 2026-08-22
>
> **Concept:** [Format](format.concept.md), [Renderer](renderer.concept.md)
> **Specification:** [Format](format.sp.md), [Renderer](renderer.sp.md)
> **Specification Reference:** [Lottie 1.0.1 Scalar / Value Property](https://lottie.github.io/lottie-spec/1.0.1/single-page/#specs-properties-value)
> **Depends on:** none
> **Used by:** `renderer`, `format`
>
> Implementation plan for Lottie Scalar property compliance with the Lottie 1.0.1 specification and modular architecture: AST modeling in `format/properties/Scalar.kt`, custom deserializers supporting primitive floats, 1-element arrays, nested objects, slot IDs (`sid`), hold keyframes (`h`), keyframe easing (`i`/`o`), dynamic expression tree frame interpolation with hold keyframes and delayed starts in `renderer/properties/Scalar.kt`, updating consuming elements (`Transform`, `Rectangle`, `PolyStar`, `Fill`, `Position`), and verification.

## Goal

Ensure compliance for Lottie Scalar properties according to the [Lottie 1.0.1 specification](https://lottie.github.io/lottie-spec/1.0.1/single-page/#specs-properties-value):
1. Align AST data models with the Lottie 1.0.1 specification under `com.google.android.horologist.remotecompose.lottie.format.properties`.
2. Extract animatable property wrappers `BaseScalarProperty`, `StaticScalarProperty`, `AnimatedScalarProperty`, `ScalarPropertyKeyframe`, and `ScalarKeyframeEasing` into `format/properties/Scalar.kt`.
3. Support flexible deserialization (primitive numbers, 1-element arrays `[Float]`, nested objects `{"k": ...}`, slot IDs `sid`, hold keyframes `h` as boolean or integer, and tangent easing representations).
4. Decouple renderer architecture into `com.google.android.horologist.remotecompose.lottie.renderer.properties.Scalar.kt` with dynamic `RemoteFloat` expression tree evaluations.
5. Implement keyframe interpolation and chaining engine for scalar values (cubic Bézier easing curves, linear interpolation `lerp`, delayed start, hold keyframes).
6. Update consumers in `format/Shapes.kt`, `format/properties/Position.kt`, `renderer/Transform.kt`, and `renderer/Shape.kt` to use the modular scalar properties.
7. Clean up legacy scalar models, serializers, and animation functions from `format/Properties.kt`, `format/LottieDecoder.kt`, and `renderer/Animation.kt`.
8. Verify with comprehensive unit testing, format checks, and Roborazzi screenshot verification.

---

## Specification Alignment (Lottie 1.0.1)

| Lottie Spec Element | Spec Section | Format AST Representation | Renderer Representation |
|---|---|---|---|
| **Scalar Value** | `#specs-values-float` | `Float` (e.g. `10.0f` or `[10.0f]`) | `RemoteFloat` |
| **Scalar Property** | `#specs-properties-value` | `BaseScalarProperty` (`StaticScalarProperty` / `AnimatedScalarProperty`) | `animateScalar(...) -> RemoteFloat` |
| **Scalar Keyframe** | `#specs-properties-keyframe` | `ScalarPropertyKeyframe` (`t`, `s`, `i`, `o`, `h`) | `ScalarAnimationSegment` + easing curve |
| **Keyframe Easing** | `#specs-properties-keyframe-easing` | `ScalarKeyframeEasing` (`x`, `y` each float or array) | `CubicBezierEasing` |
| **Slot ID** | `#specs-helpers-slottable-property` | `sid: String?` | Resolved via `LottieSettings.slotMap` |
| **Hold Keyframe** | `#specs-properties-base-keyframe` | `h: Boolean` (deserialized from `0`/`1`/`boolean`) | `selectIfLt` hold branch |

---

## Architectural Design & Data Models

### 1. Format Layer (`com.google.android.horologist.remotecompose.lottie.format.properties.Scalar.kt`)

```kotlin
package com.google.android.horologist.remotecompose.lottie.format.properties

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable(with = BaseScalarPropertySerializer::class)
internal sealed class BaseScalarProperty {
  abstract val animated: Boolean
  abstract val slotId: String?
}

@Serializable(with = StaticScalarPropertySerializer::class)
internal data class StaticScalarProperty(
  @SerialName("sid") override val slotId: String? = null,
  override val animated: Boolean = false,
  @SerialName("k") val value: Float = 0f,
) : BaseScalarProperty()

@Serializable(with = AnimatedScalarPropertySerializer::class)
internal data class AnimatedScalarProperty(
  @SerialName("sid") override val slotId: String? = null,
  @SerialName("a") val animatedInt: Int = 1,
  @SerialName("k") val keyframes: List<ScalarPropertyKeyframe>,
) : BaseScalarProperty() {
  override val animated: Boolean
    get() = animatedInt == 1
}

@Serializable(with = ScalarPropertyKeyframeSerializer::class)
internal data class ScalarPropertyKeyframe(
  @SerialName("t") val frame: Float = 0f,
  @SerialName("h") val hold: Boolean = false,
  @SerialName("i") val inTangent: ScalarKeyframeEasing? = null,
  @SerialName("o") val outTangent: ScalarKeyframeEasing? = null,
  @SerialName("s") val value: Float = 0f,
)

@Serializable(with = ScalarKeyframeEasingSerializer::class)
internal data class ScalarKeyframeEasing(val x: Float, val y: Float)
```

### 2. Renderer Layer (`com.google.android.horologist.remotecompose.lottie.renderer.properties.Scalar.kt`)

```kotlin
package com.google.android.horologist.remotecompose.lottie.renderer.properties

import android.annotation.SuppressLint
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.lerp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.selectIfLt
import com.google.android.horologist.remotecompose.lottie.LottieSettings
import com.google.android.horologist.remotecompose.lottie.format.properties.AnimatedScalarProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.BaseScalarProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticScalarProperty
import com.google.android.horologist.remotecompose.lottie.renderer.lookupValueInBezier
import com.google.android.horologist.remotecompose.lottie.renderer.scalarLinearEasingIn
import com.google.android.horologist.remotecompose.lottie.renderer.scalarLinearEasingOut

internal data class ScalarAnimationSegment(val startFrame: Float, val value: RemoteFloat)

@SuppressLint("RestrictedApi")
internal fun animateScalar(
  scalar: BaseScalarProperty,
  animationSettings: LottieSettings,
): RemoteFloat {
  return when (scalar) {
    is StaticScalarProperty -> scalar.value.rf
    is AnimatedScalarProperty -> {
      if (scalar.keyframes.isEmpty()) {
        return 0f.rf
      }
      if (scalar.keyframes.size == 1) {
        return scalar.keyframes[0].value.rf
      }

      val animationSegments = mutableListOf<ScalarAnimationSegment>()

      val firstKeyframe = scalar.keyframes[0]
      if (firstKeyframe.frame != 0f) {
        animationSegments.add(ScalarAnimationSegment(startFrame = 0f, value = firstKeyframe.value.rf))
      }

      for (i in 0 until scalar.keyframes.size - 1) {
        val startKeyframe = scalar.keyframes[i]
        val endKeyframe = scalar.keyframes[i + 1]
        val duration = endKeyframe.frame - startKeyframe.frame
        val frameInAnimation = animationSettings.currentFrame - startKeyframe.frame

        val segmentValue =
          if (startKeyframe.hold) {
            selectIfLt(frameInAnimation, duration.rf, startKeyframe.value.rf, endKeyframe.value.rf)
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

            lerp(startKeyframe.value.rf, endKeyframe.value.rf, progress)
          }

        animationSegments.add(ScalarAnimationSegment(startKeyframe.frame, segmentValue))
      }

      chainScalarAnimation(animationSegments, animationSettings.currentFrame)
    }
  }
}

@SuppressLint("RestrictedApi")
private fun chainScalarAnimation(
  segments: List<ScalarAnimationSegment>,
  frame: RemoteFloat,
): RemoteFloat {
  if (segments.size == 1) {
    return segments[0].value
  }

  return selectIfLt(
    frame,
    segments[1].startFrame.rf,
    segments[0].value,
    chainScalarAnimation(segments.subList(1, segments.size), frame),
  )
}
```

---

## Phases

### Phase 1 — Test Harness & Assertions {#PL_LOTTIE_SCALAR_P1}
- Add unit tests in `LottieDecoderResilienceTest.kt` covering:
  - Static scalar properties from primitive numbers, 1-element arrays, and nested objects.
  - Animated scalar properties with single/multiple keyframes, hold keyframes (`h: 1`, `h: true`), delayed start, and slot IDs (`sid`).
  - Keyframe easing tangents `i` and `o` with numbers and 1-element arrays.
- Add unit tests in `ParsingTest.kt` verifying scalar properties in shapes and transforms (`Rectangle.cornerRadius`, `PolyStar.points`, `PolyStar.outerRadius`, `PolyStar.innerRadius`, `Transform.rotation`, `Transform.opacity`, `Fill.opacity`).
- Add unit tests in `AnimationTest.kt` for `animateScalar`:
  - Static scalar input.
  - Empty keyframes fallback (0f).
  - Single keyframe static hold.
  - Multi-keyframe animation interpolation across frames.
  - Hold keyframe evaluation.
  - Delayed start keyframe evaluation.

### Phase 2 — AST Extraction to `format/properties/Scalar.kt` {#PL_LOTTIE_SCALAR_P2}
- Create `format/properties/Scalar.kt` defining `BaseScalarProperty`, `StaticScalarProperty`, `AnimatedScalarProperty`, `ScalarPropertyKeyframe`, and `ScalarKeyframeEasing`.
- Update `GraphicElement` in `format/Shapes.kt` and `SplitPositionProperty` in `format/properties/Position.kt` to use the modular `BaseScalarProperty` and `StaticScalarProperty`.
- Remove legacy classes from `format/Properties.kt` (and remove `Properties.kt`).
- Update imports across `format/properties/Vector.kt`, `format/properties/Position.kt`, `format/properties/Bezier.kt`, `format/properties/Color.kt`, and `renderer/Animation.kt`.

### Phase 3 — Deserialization Implementation {#PL_LOTTIE_SCALAR_P3}
- Implement `BaseScalarPropertySerializer`, `StaticScalarPropertySerializer`, `AnimatedScalarPropertySerializer`, `ScalarPropertyKeyframeSerializer`, and `ScalarKeyframeEasingSerializer` in `format/properties/Scalar.kt`.
- Clean up legacy scalar serializers and `ScalarKeyframeEasingSerializer` from `format/LottieDecoder.kt`.
- Ensure full resilience against flexible Lottie JSON structures (primitives, arrays, objects, booleans/ints).

### Phase 4 — Renderer Interpolation & Consumer Wiring {#PL_LOTTIE_SCALAR_P4}
- Implement `animateScalar(BaseScalarProperty, LottieSettings): RemoteFloat` and `chainScalarAnimation` in `renderer/properties/Scalar.kt`.
- Wire `renderer/Transform.kt`, `renderer/Shape.kt`, and `renderer/properties/Position.kt` to use `renderer.properties.animateScalar`.
- Clean up `animateScalar` and `chainAnimation` from `renderer/Animation.kt`.

### Phase 5 — Verification Suite & Full Check {#PL_LOTTIE_SCALAR_P5}
- Run mandatory verification commands in order:
  1. `./gradlew :remotecompose:lottie:ktfmtFormat`
  2. `./gradlew :remotecompose:lottie:metalavaGenerateSignatureDebug`
  3. `./gradlew :remotecompose:lottie:compileDebugKotlin`
  4. `./gradlew :remotecompose:lottie:assembleDebug`
  5. `./gradlew :remotecompose:lottie:testDebugUnitTest --no-build-cache`
  6. `./gradlew :remotecompose:lottie:check`
  7. `./gradlew :remotecompose:lottie:verifyRoborazziDebug`
