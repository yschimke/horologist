# Implementation Plan: Bezier Property Specification Compliance & Modular Architecture {#PL_LOTTIE_BEZIER}

> **Code:** PL_LOTTIE_BEZIER
> **Status:** completed
> **Created:** 2026-08-21
> **Updated:** 2026-08-22
>
> **Concept:** [Format](format.concept.md), [Renderer](renderer.concept.md)
> **Specification:** [Format](format.sp.md), [Renderer](renderer.sp.md)
> **Depends on:** none
> **Used by:** `renderer`, `format`
>
> Implementation plan for Lottie Bezier (Shape) property compliance with Lottie 1.0.1 specification and modular architecture: AST modeling in `format/properties/Bezier.kt`, custom serializer supporting flexible values and flags, dynamic expression tree frame interpolation with hold keyframes and delayed starts in `renderer/properties/Bezier.kt`, single-shape contour preservation, and atomic commit decomposition.

## Goal

Ensure compliance for Lottie Bezier / Shape properties:
1. Align AST data models with the Lottie 1.0.1 specification under `com.google.android.horologist.remotecompose.lottie.format.values` and `com.google.android.horologist.remotecompose.lottie.format.properties`.
2. Extract domain shape payload `BezierValue` into `format/values/Bezier.kt`.
3. Extract animatable property wrappers `BaseBezierProperty`, `StaticBezierProperty`, `AnimatedBezierProperty`, and `BezierPropertyKeyframe` into `format/properties/Bezier.kt`.
4. Support flexible deserialization (object or list for keyframe `s` value, boolean or integer 0/1 for `c` closed flag, slot IDs).
5. Decouple renderer architecture into `com.google.android.horologist.remotecompose.lottie.renderer.properties.Bezier.kt` with dynamic `RemoteFloat` expression tree evaluations and boundary safety.
6. Implement keyframe interpolation and chaining engine (cubic Bézier easing progress, multi-segment chaining, delayed start, hold keyframes).
7. Preserve stroke cap/join integrity and fill winding rules by encapsulating subpaths in a single `RemoteLottiePath`.
8. Verify with comprehensive unit testing and Roborazzi screenshot verification.

## Phases

### Phase 1 — Test Harness & Assertions [DONE] {#PL_LOTTIE_BEZIER_P1}
- Add unit tests in `LottieDecoderResilienceTest.kt` for flexible `c` flags (`1`/`0`/boolean), `s` keyframe values (single object vs array), and slot IDs.
- Add unit tests in `AnimationTest.kt` for static Bézier, single/multi keyframes, hold keyframes, delayed start, and open vs closed path index boundary safety.
- Add/update screenshot verification test covering Bézier path rendering.

### Phase 2 — AST Extraction to `format/values/Bezier.kt` & `format/properties/Bezier.kt` [DONE] {#PL_LOTTIE_BEZIER_P2}
- Create `format/values/Bezier.kt` defining domain geometry model `BezierValue`.
- Create `format/properties/Bezier.kt` defining `BaseBezierProperty`, `StaticBezierProperty`, `AnimatedBezierProperty`, and `BezierPropertyKeyframe`.
- Remove legacy Bezier classes from `format/Properties.kt` and `format/Values.kt`.
- Update `GraphicElement.Path` in `format/Shapes.kt` and renderer references to import from `format.values` and `format.properties`.

### Phase 3 — Deserialization Implementation [DONE] {#PL_LOTTIE_BEZIER_P3}
- Implement `BezierValueSerializer` in `format/values/Bezier.kt` supporting flexible `c` (int/bool).
- Implement `BaseBezierPropertySerializer`, `StaticBezierPropertySerializer`, `AnimatedBezierPropertySerializer`, and `BezierPropertyKeyframeSerializer` in `format/properties/Bezier.kt` supporting flexible `s` (object/array) and `sid`.
- Remove legacy `BaseBezierPropertySerializer` from `format/LottieDecoder.kt`.

### Phase 4 — Renderer Core & Dynamic Path Architecture [DONE] {#PL_LOTTIE_BEZIER_P4}
- Define `RemoteBezierValue` (with `RemoteFloat` matrices) and `animateBezier(BaseBezierProperty, LottieSettings): List<RemoteBezierValue>` for static and single-keyframe evaluation in `renderer/properties/Bezier.kt`.
- Update `RemoteLottiePath` in `renderer/RemoteShape.kt` to render subpaths in `drawScope.remotePath { ... }` with open path boundary fix (`maxIndex = if (closed) vertices.size else vertices.size - 1`).
- Wire `renderer/Shape.kt` to use `renderer.properties.animateBezier` and `RemoteLottiePath`.
- Clean up legacy `animateBezier` from `renderer/Animation.kt`.

### Phase 5 — Keyframe Interpolation & Chaining Engine [DONE] {#PL_LOTTIE_BEZIER_P5}
- Implement `evaluateKeyframeProgress` using cubic Bézier easing curves.
- Implement segment interpolation (`lerp`) and `chainBezierAnimation` across timeline thresholds.
- Add delayed start (`firstKeyframe.frame != 0f`) and hold keyframe (`hold == true`) handling in `renderer/properties/Bezier.kt`.

### Phase 6 — Verification Suite & Checks [DONE] {#PL_LOTTIE_BEZIER_P6}
- Run `./gradlew :remotecompose:lottie:ktfmtFormat`
- Run `./gradlew :remotecompose:lottie:metalavaGenerateSignatureDebug`
- Run `./gradlew :remotecompose:lottie:compileDebugKotlin`
- Run `./gradlew :remotecompose:lottie:assembleDebug`
- Run `./gradlew :remotecompose:lottie:testDebugUnitTest --no-build-cache`
- Run `./gradlew :remotecompose:lottie:check`
- Run `./gradlew :remotecompose:lottie:verifyRoborazziDebug`
