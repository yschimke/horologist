# Implementation Plan: Vector Property Specification Compliance & Modular Architecture {#PL_LOTTIE_VECTOR}

> **Code:** PL_LOTTIE_VECTOR
> **Status:** in-progress
> **Created:** 2026-08-22
> **Updated:** 2026-08-22
>
> **Concept:** [Format](format.concept.md), [Renderer](renderer.concept.md)
> **Specification:** [Format](format.sp.md), [Renderer](renderer.sp.md)
> **Specification Reference:** [Lottie 1.0.1 Vector Property](https://lottie.github.io/lottie-spec/1.0.1/single-page/#specs-properties-vector-property)
> **Depends on:** none
> **Used by:** `renderer`, `format`
>
> Implementation plan for Lottie Vector property compliance with the Lottie 1.0.1 specification and modular architecture: AST modeling in `format/properties/Vector.kt`, custom deserializers supporting flexible values (primitive numbers, arrays, nested arrays, slot IDs), dynamic expression tree frame interpolation with hold keyframes and delayed starts in `renderer/properties/Vector.kt`, updating consumers (`Transform`, `Rectangle`, `Ellipse`), and verification.

## Goal

Ensure compliance for Lottie Vector properties according to the [Lottie 1.0.1 specification](https://lottie.github.io/lottie-spec/1.0.1/single-page/#specs-properties-vector-property):
1. Align AST data models with the Lottie 1.0.1 specification under `com.google.android.horologist.remotecompose.lottie.format.properties`.
2. Extract animatable property wrappers `BaseVectorProperty`, `StaticVectorProperty`, `AnimatedVectorProperty`, and `VectorPropertyKeyframe` into `format/properties/Vector.kt`.
3. Support flexible deserialization (primitive floats, float arrays, nested float arrays, slot IDs `sid`, and hold keyframes `h` as boolean or integer).
4. Decouple renderer architecture into `com.google.android.horologist.remotecompose.lottie.renderer.properties.Vector.kt` with dynamic `RemoteFloat` expression tree evaluations.
5. Implement keyframe interpolation and chaining engine (cubic Bézier easing curves, multi-component vector linear interpolation `lerp`, delayed start, hold keyframes).
6. Update consumers in `format/Shapes.kt`, `renderer/Transform.kt`, and `renderer/Shape.kt` to use the modular vector properties.
7. Clean up legacy vector models, serializers, and animation functions from `format/Properties.kt`, `format/LottieDecoder.kt`, and `renderer/Animation.kt`.
8. Verify with comprehensive unit testing, format checks, and Roborazzi screenshot verification.

---

## Specification Alignment (Lottie 1.0.1)

| Lottie Spec Element | Spec Section | Format AST Representation | Renderer Representation |
|---|---|---|---|
| **Vector Value** | `#specs-values-vector` | `List<Float>` (e.g. `[x, y]` or `[x, y, z]`) | `List<RemoteFloat>` |
| **Vector Property** | `#specs-properties-vector-property` | `BaseVectorProperty` (`StaticVectorProperty` / `AnimatedVectorProperty`) | `animateVector(...)` |
| **Vector Keyframe** | `#specs-properties-vector-keyframe` | `VectorPropertyKeyframe` (`t`, `s`, `i`, `o`, `h`) | `VectorAnimationSegment` + easing curve |
| **Slot ID** | `#specs-helpers-slottable-property` | `sid: String?` | Resolved via `LottieSettings.slotMap` |
| **Hold Keyframe** | `#specs-properties-base-keyframe` | `h: Boolean` (deserialized from `0`/`1`/`boolean`) | `selectIfLt` hold branch |

---

## Architectural Design & Data Models

### 1. Format Layer (`com.google.android.horologist.remotecompose.lottie.format.properties.Vector.kt`)

```kotlin
package com.google.android.horologist.remotecompose.lottie.format.properties

import com.google.android.horologist.remotecompose.lottie.format.ScalarKeyframeEasing
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable(with = BaseVectorPropertySerializer::class)
internal sealed class BaseVectorProperty {
  abstract val animated: Boolean
  abstract val slotId: String?
}

@Serializable(with = StaticVectorPropertySerializer::class)
internal data class StaticVectorProperty(
  @SerialName("sid") override val slotId: String? = null,
  override val animated: Boolean = false,
  @SerialName("k") val value: List<Float>,
) : BaseVectorProperty()

@Serializable(with = AnimatedVectorPropertySerializer::class)
internal data class AnimatedVectorProperty(
  @SerialName("sid") override val slotId: String? = null,
  @SerialName("a") val animatedInt: Int = 1,
  @SerialName("k") val keyframes: List<VectorPropertyKeyframe>,
) : BaseVectorProperty() {
  override val animated: Boolean
    get() = animatedInt == 1
}

@Serializable(with = VectorPropertyKeyframeSerializer::class)
internal data class VectorPropertyKeyframe(
  @SerialName("t") val frame: Float = 0f,
  @SerialName("h") val hold: Boolean = false,
  @SerialName("i") val inTangent: ScalarKeyframeEasing? = null,
  @SerialName("o") val outTangent: ScalarKeyframeEasing? = null,
  @SerialName("s") val value: List<Float> = emptyList(),
)
```

### 2. Deserializers (`format/properties/Vector.kt`)

- **`BaseVectorPropertySerializer`**: Polymorphic serializer selecting `AnimatedVectorPropertySerializer` when `a == 1` and `StaticVectorPropertySerializer` otherwise.
- **`StaticVectorPropertySerializer`**: Handles:
  - Primitive numbers: `k: 42.0` -> `listOf(42.0f)`
  - Number arrays: `k: [100.0, 50.0]` -> `listOf(100.0f, 50.0f)`
  - Nested arrays: `k: [[100.0, 50.0]]` -> `listOf(100.0f, 50.0f)`
  - Slot ID extraction: `sid: "custom.scale"`
- **`AnimatedVectorPropertySerializer`**: Deserializes keyframe list from `k` with slot ID support.
- **`VectorPropertyKeyframeSerializer`**: Deserializes:
  - `t`: Float frame number.
  - `h`: Boolean or integer (0 or 1).
  - `i`, `o`: Easing handles using `ScalarKeyframeEasingSerializer`.
  - `s`: Array `[x, y]`, single number `x`, or nested array `[[x, y]]`.

### 3. Renderer Layer (`com.google.android.horologist.remotecompose.lottie.renderer.properties.Vector.kt`)

```kotlin
package com.google.android.horologist.remotecompose.lottie.renderer.properties

import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.lerp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.selectIfLt
import com.google.android.horologist.remotecompose.lottie.LottieSettings
import com.google.android.horologist.remotecompose.lottie.format.properties.AnimatedVectorProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.BaseVectorProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticVectorProperty
import com.google.android.horologist.remotecompose.lottie.renderer.lookupValueInBezier
import com.google.android.horologist.remotecompose.lottie.renderer.scalarLinearEasingIn
import com.google.android.horologist.remotecompose.lottie.renderer.scalarLinearEasingOut

internal data class VectorAnimationSegment(val startFrame: Float, val value: List<RemoteFloat>)

internal fun animateVector(
  vector: BaseVectorProperty,
  animationSettings: LottieSettings,
): List<RemoteFloat>
```

- **Static Resolution**: Maps each element of `property.value` to `RemoteFloat` (`it.rf`).
- **Animated Resolution**:
  - `keyframes.isEmpty()` -> `emptyList()`
  - `keyframes.size == 1` -> `keyframes[0].value.map { it.rf }`
  - `keyframes.size > 1`:
    - Delayed start: if `firstKeyframe.frame != 0f`, prepends segment at frame `0f` with `firstKeyframe.value`.
    - Easing progression: calculates progress `progress = lookupValueInBezier(...)`.
    - Hold keyframe: `selectIfLt(frameInAnimation, duration.rf, startValue.rf, endValue.rf)`.
    - Linear interpolation: `lerp(startValue.rf, endValue.rf, progress)`.
    - Chaining: `chainVectorAnimation(segments, currentFrame)` using `selectIfLt` between consecutive keyframe start frames.

---

## Phases

### Phase 1 — Test Harness & Assertions {#PL_LOTTIE_VECTOR_P1}
- **`LottieDecoderResilienceTest.kt`**:
  - `vectorProperty_handlesFloatArraysAndSingleNumberFallback`: Test decoding 2D/3D float arrays, single float fallback, and nested array wrappers.
  - `vectorProperty_parsesSlotId`: Test slot ID (`sid`) extraction on static and animated vector properties.
  - `vectorProperty_animatedKeyframesWithSingleAndNestedValues`: Test keyframe deserialization with easing handles, hold flags (`h: 1`), single numbers, and arrays.
- **`ParsingTest.kt`**:
  - Update `animatedVectorProperty_deserializes` and `rectEllipse_deserializes` to verify `Rectangle.size`, `Ellipse.size`, and `Transform.scale` with the new models.
- **`AnimationTest.kt`**:
  - `animateVectorWithStaticInput_returnsInput`: Static vector evaluation.
  - `animateVectorWithSingleKeyframe_returnsInput`: Single keyframe evaluation.
  - `animateVectorWithTwoKeyframes_returnsAnimatedValues`: Keyframe interpolation across frames.
  - `animateVectorWithHoldKeyframe_holdsValue`: Hold keyframe transitions.
  - `animateVectorWithDelayedStart_holdsInitialValue`: Delayed start animation before and at start frame.
  - `animateVectorWithEmptyKeyframes_returnsEmptyList`: Empty keyframe edge case safety.

### Phase 2 — AST Extraction to `format/properties/Vector.kt` {#PL_LOTTIE_VECTOR_P2}
- Create `format/properties/Vector.kt` defining `BaseVectorProperty`, `StaticVectorProperty`, `AnimatedVectorProperty`, and `VectorPropertyKeyframe`.
- Remove legacy vector classes from `format/Properties.kt`.
- Update `GraphicElement.Rectangle`, `GraphicElement.Ellipse`, and `GraphicElement.Transform` in `format/Shapes.kt` to use `com.google.android.horologist.remotecompose.lottie.format.properties.*`.

### Phase 3 — Deserialization Implementation {#PL_LOTTIE_VECTOR_P3}
- Implement `BaseVectorPropertySerializer`, `StaticVectorPropertySerializer`, `AnimatedVectorPropertySerializer`, and `VectorPropertyKeyframeSerializer` in `format/properties/Vector.kt`.
- Remove legacy `BaseVectorPropertySerializer` from `format/LottieDecoder.kt`.

### Phase 4 — Renderer Vector Interpolation & Consumers {#PL_LOTTIE_VECTOR_P4}
- Implement `animateVector`, `VectorAnimationSegment`, and `chainVectorAnimation` in `renderer/properties/Vector.kt`.
- Update `renderer/Transform.kt` and `renderer/Shape.kt` to import `com.google.android.horologist.remotecompose.lottie.renderer.properties.animateVector`.
- Remove legacy `animateVector` from `renderer/Animation.kt`.

### Phase 5 — Verification Suite & Checks {#PL_LOTTIE_VECTOR_P5}
- Run `./gradlew :remotecompose:lottie:ktfmtFormat`
- Run `./gradlew :remotecompose:lottie:metalavaGenerateSignatureDebug`
- Run `./gradlew :remotecompose:lottie:compileDebugKotlin`
- Run `./gradlew :remotecompose:lottie:assembleDebug`
- Run `./gradlew :remotecompose:lottie:testDebugUnitTest --no-build-cache`
- Run `./gradlew :remotecompose:lottie:check`
- Run `./gradlew :remotecompose:lottie:verifyRoborazziDebug`

---

## Alternatives Considered

### 1. `List<Float>` vs `FloatArray` in AST
- **Option A (`FloatArray`)**: Lower primitive allocation overhead on JVM, but requires manual `equals()` / `hashCode()` overrides in Kotlin data classes, lacks built-in immutable list semantics, and complicates kotlinx.serialization polymorphic serializers.
- **Option B (`List<Float>`) [Selected]**: Clean Kotlin data classes with automatic value equality, idiomatic collections interoperability, direct compatibility with Kotlin serialization lists, and straightforward mapping to `RemoteFloat` / `RemoteFloatArray`.

### 2. Monolithic vs Modular Serializer Placement
- **Option A (Keep in `format/LottieDecoder.kt`)**: Centralized decoder file, but creates large monolithic file and couples all format deserializers together.
- **Option B (Place in `format/properties/Vector.kt`) [Selected]**: Keeps AST and its serializers colocated in property domain file matching `Color.kt` and `Bezier.kt` pattern.
