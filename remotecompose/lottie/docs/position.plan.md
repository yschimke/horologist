# Implementation Plan: Position Property Specification Compliance & Modular Architecture {#PL_LOTTIE_POSITION}

> **Code:** PL_LOTTIE_POSITION
> **Status:** completed
> **Created:** 2026-08-22
> **Updated:** 2026-08-22
>
> **Concept:** [Format](format.concept.md), [Renderer](renderer.concept.md)
> **Specification:** [Format](format.sp.md), [Renderer](renderer.sp.md)
> **Specification Reference:** [Lottie 1.0.1 Position Property](https://lottie.github.io/lottie-spec/1.0.1/single-page/#specs-properties-position-property)
> **Depends on:** none
> **Used by:** `renderer`, `format`
>
> Implementation plan for Lottie Position property compliance with the Lottie 1.0.1 specification and modular architecture: AST modeling in `format/properties/Position.kt`, custom deserializers supporting standard multi-dimensional positions, split positions (`s: true`), flexible array/primitive values, slot IDs, hold keyframes, frame interpolation and chaining in `renderer/properties/Position.kt`, updating consumers (`Transform`, `Rectangle`, `Ellipse`, `PolyStar`), and verification.

## Goal

Ensure compliance for Lottie Position properties according to the [Lottie 1.0.1 specification](https://lottie.github.io/lottie-spec/1.0.1/single-page/#specs-properties-position-property):
1. Align AST data models with the Lottie 1.0.1 specification under `com.google.android.horologist.remotecompose.lottie.format.properties`.
2. Extract animatable property wrappers `BasePositionProperty`, `StaticPositionProperty`, `AnimatedPositionProperty`, `SplitPositionProperty`, and `PositionPropertyKeyframe` into `format/properties/Position.kt`.
3. Support flexible deserialization (primitive floats, float arrays, nested float arrays, slot IDs `sid`, hold keyframes `h` as boolean or integer, split positions `s: true` with individual `x`/`y`/`z` scalar properties, and spatial tangents `ti`/`to`).
4. Decouple renderer architecture into `com.google.android.horologist.remotecompose.lottie.renderer.properties.Position.kt` with dynamic `RemoteFloat` / `Point` expression tree evaluations.
5. Implement keyframe interpolation and chaining engine for 2D position vectors (cubic Bézier easing curves, coordinate linear interpolation `lerp`, delayed start, hold keyframes, and split position resolution via `animateScalar`).
6. Update consumers in `format/Shapes.kt`, `renderer/Transform.kt`, and `renderer/Shape.kt` to use the modular position properties.
7. Clean up legacy position models, serializers, and animation functions from `format/Properties.kt`, `format/LottieDecoder.kt`, and `renderer/Animation.kt`.
8. Verify with comprehensive unit testing, format checks, and Roborazzi screenshot verification.

---

## Specification Alignment (Lottie 1.0.1)

| Lottie Spec Element | Spec Section | Format AST Representation | Renderer Representation |
|---|---|---|---|
| **Multi-dimensional Position** | `#specs-properties-position-property` | `StaticPositionProperty` / `AnimatedPositionProperty` | `animatePosition(...) -> Point` |
| **Position Keyframe** | `#specs-properties-position-keyframe` | `PositionPropertyKeyframe` (`t`, `s`, `i`, `o`, `h`, `ti`, `to`) | `PositionAnimationSegment` + easing curve |
| **Split Position** | `#specs-properties-split-position-keyframe` | `SplitPositionProperty` (`s`, `x`, `y`, `z`) | `Point(animateScalar(x), animateScalar(y))` |
| **Slot ID** | `#specs-helpers-slottable-property` | `sid: String?` | Resolved via `LottieSettings.slotMap` |
| **Hold Keyframe** | `#specs-properties-base-keyframe` | `h: Boolean` (deserialized from `0`/`1`/`boolean`) | `selectIfLt` hold branch |

---

## Architectural Design & Data Models

### 1. Format Layer (`com.google.android.horologist.remotecompose.lottie.format.properties.Position.kt`)

```kotlin
package com.google.android.horologist.remotecompose.lottie.format.properties

import com.google.android.horologist.remotecompose.lottie.format.BaseScalarProperty
import com.google.android.horologist.remotecompose.lottie.format.ScalarKeyframeEasing
import com.google.android.horologist.remotecompose.lottie.format.StaticScalarProperty
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable(with = BasePositionPropertySerializer::class)
internal sealed class BasePositionProperty {
  abstract val animated: Boolean
  abstract val slotId: String?
}

@Serializable(with = StaticPositionPropertySerializer::class)
internal data class StaticPositionProperty(
  @SerialName("sid") override val slotId: String? = null,
  override val animated: Boolean = false,
  @SerialName("k") val value: List<Float>,
) : BasePositionProperty()

@Serializable(with = AnimatedPositionPropertySerializer::class)
internal data class AnimatedPositionProperty(
  @SerialName("sid") override val slotId: String? = null,
  @SerialName("a") val animatedInt: Int = 1,
  @SerialName("k") val keyframes: List<PositionPropertyKeyframe>,
) : BasePositionProperty() {
  override val animated: Boolean
    get() = animatedInt == 1
}

@Serializable(with = SplitPositionPropertySerializer::class)
internal data class SplitPositionProperty(
  @SerialName("sid") override val slotId: String? = null,
  @SerialName("s") val split: Boolean = true,
  @SerialName("x") val x: BaseScalarProperty = StaticScalarProperty(value = 0f),
  @SerialName("y") val y: BaseScalarProperty = StaticScalarProperty(value = 0f),
  @SerialName("z") val z: BaseScalarProperty? = null,
) : BasePositionProperty() {
  override val animated: Boolean
    get() = x.animated || y.animated || (z?.animated ?: false)
}

@Serializable(with = PositionPropertyKeyframeSerializer::class)
internal data class PositionPropertyKeyframe(
  @SerialName("t") val frame: Float = 0f,
  @SerialName("h") val hold: Boolean = false,
  @SerialName("i") val inTangent: ScalarKeyframeEasing? = null,
  @SerialName("o") val outTangent: ScalarKeyframeEasing? = null,
  @SerialName("s") val value: List<Float> = emptyList(),
  @SerialName("ti") val spatialInTangent: List<Float>? = null,
  @SerialName("to") val spatialOutTangent: List<Float>? = null,
)
```

### 2. Deserializers (`format/properties/Position.kt`)

- **`BasePositionPropertySerializer`**: Polymorphic serializer checking:
  - If `s: true` (or boolean/integer 1) -> `SplitPositionPropertySerializer`
  - Else if `a == 1` -> `AnimatedPositionPropertySerializer`
  - Else -> `StaticPositionPropertySerializer`
- **`StaticPositionPropertySerializer`**: Handles:
  - Primitive numbers: `k: 42.0` -> `listOf(42.0f, 42.0f)` or `listOf(42.0f)`
  - Number arrays: `k: [100.0, 50.0]` -> `listOf(100.0f, 50.0f)`
  - Nested arrays: `k: [[100.0, 50.0]]` -> `listOf(100.0f, 50.0f)`
  - Slot ID extraction: `sid: "custom.position"`
- **`AnimatedPositionPropertySerializer`**: Deserializes keyframe list from `k` with slot ID support.
- **`SplitPositionPropertySerializer`**: Deserializes split `x`, `y`, `z` scalar properties with slot ID support.
- **`PositionPropertyKeyframeSerializer`**: Deserializes:
  - `t`: Float frame number.
  - `h`: Boolean or integer (0 or 1).
  - `i`, `o`: Easing handles using `ScalarKeyframeEasingSerializer`.
  - `s`: Array `[x, y]`, single number `x`, or nested array `[[x, y]]`.
  - `ti`, `to`: Spatial tangent float arrays.

### 3. Renderer Layer (`com.google.android.horologist.remotecompose.lottie.renderer.properties.Position.kt`)

```kotlin
package com.google.android.horologist.remotecompose.lottie.renderer.properties

import androidx.compose.remote.creation.compose.state.RemoteFloat
import com.google.android.horologist.remotecompose.lottie.LottieSettings
import com.google.android.horologist.remotecompose.lottie.format.properties.BasePositionProperty

internal data class Point(val x: RemoteFloat, val y: RemoteFloat)

internal data class PositionAnimationSegment(val startFrame: Float, val x: RemoteFloat, val y: RemoteFloat)

internal fun animatePosition(
  position: BasePositionProperty,
  animationSettings: LottieSettings,
): Point
```

- **Static Resolution**: Maps `value[0].rf` and `value[1].rf` to `Point`.
- **Split Resolution**: Evaluates `Point(x = animateScalar(position.x, animationSettings), y = animateScalar(position.y, animationSettings))`.
- **Animated Resolution**:
  - `keyframes.isEmpty()` -> `Point(0f.rf, 0f.rf)`
  - `keyframes.size == 1` -> `Point(keyframes[0].value.getOrElse(0) { 0f }.rf, keyframes[0].value.getOrElse(1) { 0f }.rf)`
  - `keyframes.size > 1`:
    - Delayed start: if `firstKeyframe.frame != 0f`, prepends segment at frame `0f` with `firstKeyframe.value`.
    - Easing progression: calculates progress `progress = lookupValueInBezier(...)`.
    - Hold keyframe: `selectIfLt(frameInAnimation, duration.rf, startVal.rf, endVal.rf)`.
    - Linear interpolation: `lerp(startVal.rf, endVal.rf, progress)`.
    - Chaining: `chainPositionAnimation(segments, currentFrame)` using `selectIfLt` between consecutive keyframe start frames for X and Y axes.

---

## Phases

### Phase 1 — Test Harness & Assertions {#PL_LOTTIE_POSITION_P1}
- **`LottieDecoderResilienceTest.kt`**:
  - `positionProperty_handlesFloatArraysAndSingleNumberFallback`: Test decoding 2D/3D float arrays, single float fallback, and nested array wrappers.
  - `positionProperty_parsesSlotId`: Test slot ID (`sid`) extraction on static and animated position properties.
  - `positionProperty_animatedKeyframesWithSingleAndNestedValues`: Test keyframe deserialization with easing handles, hold flags (`h: 1`), single numbers, arrays, and spatial tangents (`ti`, `to`).
  - `positionProperty_splitPosition_deserializesXYScalars`: Test split position property (`s: true`) with independent scalar properties for X and Y.
- **`ParsingTest.kt`**:
  - Update `rectEllipse_deserializes` to verify `Rectangle.position` and `Ellipse.position` using `List<Float>` in `StaticPositionProperty`.
- **`AnimationTest.kt`**:
  - `animatePositionWithStaticInput_returnsInput`: Static position evaluation.
  - `animatePositionWithSingleKeyframe_returnsInput`: Single keyframe evaluation.
  - `animatePositionWithTwoKeyframes_returnsAnimatedValues`: Keyframe interpolation across frames for X and Y coordinates.
  - `animatePositionWithHoldKeyframe_holdsValue`: Hold keyframe transitions.
  - `animatePositionWithDelayedStart_holdsInitialValue`: Delayed start animation before and at start frame.
  - `animatePositionWithEmptyKeyframes_returnsZeroPoint`: Empty keyframe edge case safety.
  - `animatePositionWithSplitPosition_evaluatesXYIndependently`: Split position with animated/static X and Y scalars.

### Phase 2 — AST Extraction to `format/properties/Position.kt` {#PL_LOTTIE_POSITION_P2}
- Create `format/properties/Position.kt` defining `BasePositionProperty`, `StaticPositionProperty`, `AnimatedPositionProperty`, `SplitPositionProperty`, and `PositionPropertyKeyframe`.
- Remove legacy position classes from `format/Properties.kt`.
- Update `GraphicElement.Rectangle`, `GraphicElement.Ellipse`, `GraphicElement.PolyStar`, and `GraphicElement.Transform` in `format/Shapes.kt` to use `com.google.android.horologist.remotecompose.lottie.format.properties.*`.

### Phase 3 — Deserialization Implementation {#PL_LOTTIE_POSITION_P3}
- Implement `BasePositionPropertySerializer`, `StaticPositionPropertySerializer`, `AnimatedPositionPropertySerializer`, `SplitPositionPropertySerializer`, and `PositionPropertyKeyframeSerializer` in `format/properties/Position.kt`.
- Remove legacy `BasePositionPropertySerializer` from `format/LottieDecoder.kt`.

### Phase 4 — Renderer Position Interpolation & Consumers {#PL_LOTTIE_POSITION_P4}
- Implement `animatePosition`, `Point`, `PositionAnimationSegment`, and `chainPositionAnimation` in `renderer/properties/Position.kt`.
- Update `renderer/Transform.kt` and `renderer/Shape.kt` to import `com.google.android.horologist.remotecompose.lottie.renderer.properties.animatePosition` and `Point`.
- Remove legacy `animatePosition` and `Point` from `renderer/Animation.kt`.

### Phase 5 — Verification Suite & Checks {#PL_LOTTIE_POSITION_P5}
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
- **Option B (`List<Float>`) [Selected]**: Clean Kotlin data classes with automatic value equality, idiomatic collections interoperability, direct compatibility with Kotlin serialization lists, and consistency with `Vector.kt`.

### 2. Dedicated `SplitPositionProperty` vs Generic Compound Property
- **Option A (Generic Compound Property)**: Treat split positions as arbitrary map of scalar properties. Harder to type strongly and doesn't match Lottie schema where `x`, `y`, `z` are explicit keys.
- **Option B (`SplitPositionProperty` with explicit `x`, `y`, `z`) [Selected]**: Strongly typed AST matching Lottie 1.0.1 specification (`s`, `x`, `y`, `z`), enabling type-safe deserialization and rendering.
