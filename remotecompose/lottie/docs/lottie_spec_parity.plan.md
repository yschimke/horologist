# Implementation Plan: Lottie 1.0.1 Specification Parity & Hardening {#PL_LOTTIE_SPEC_PARITY}

> **Code:** PL_LOTTIE_SPEC_PARITY
> **Status:** in-progress
> **Created:** 2026-08-24
> **Updated:** 2026-08-24
>
> **Concept:** [Format](format.concept.md), [Renderer](renderer.concept.md), [Shapes](shapes.concept.md), [Layers](layers.concept.md), [Track Matte & Styles](track_matte_and_styles.concept.md)
> **Specification:** [Format](format.sp.md), [Renderer](renderer.sp.md), [Shapes](shapes.sp.md), [Layers](layers.sp.md), [Track Matte & Styles](track_matte_and_styles.sp.md)
> **Specification Reference:** [Lottie 1.0.1 Specification](https://lottie.github.io/lottie-spec/1.0.1/single-page/)
> **Source Audit:** [Lottie 1.0.1 Specification Audit & Regression Analysis](lottie_audit_and_gap_analysis.md) (`DOC_LOTTIE_AUDIT_2026`)
> **Depends on:** none
> **Used by:** `format`, `renderer`, `MediaLottieDiffScreenshotTest`, `LottieFeatureDiffScreenshotTest`
>
> Comprehensive implementation plan to resolve regressions and bugs identified in the 35-commit audit, achieve 100% specification compliance with Lottie 1.0.1, and establish an automated, modular pipeline for vector rendering in `:remotecompose:lottie`.

---

## 1. Goal

Bring `:remotecompose:lottie` to full specification compliance with [Lottie 1.0.1](https://lottie.github.io/lottie-spec/1.0.1/single-page/) and resolve all critical regressions identified in [`DOC_LOTTIE_AUDIT_2026`](lottie_audit_and_gap_analysis.md):
1. **Critical Bug Fixes & Serialization Hardening:** Eliminate all `MissingFieldException` risks, fix `@SerialName` mismatches, resolve scale-zero division singularities, fix keyframe hold parsing, compound ancestor layer opacities, and guard against circular parent hierarchies.
2. **Core Rendering & Mathematical Parity:** Implement spatial Bézier path curve interpolation (`to`, `ti`), compile linear/radial gradient shaders in `RemotePaint`, implement stroke dash patterns (`d`) and miter limits (`ml`), pass `FillRule` to path renderers, and scale local layer timelines ($t_{\text{local}} = (t - st) / sr$).
3. **Composition, Precomps & Layer Mask Pipeline:** Introduce the root `assets[]` registry, build the recursive `PrecompLayer` sub-composition rendering engine, and implement layer-level Bézier masking (`masksProperties`).
4. **Advanced Modifiers, Typography & Bitmap Assets:** Implement geometry engines for `Repeater`, `RoundedCorners`, and `MergePaths`, decode bitmap image assets, and model vector typography.

---

## 2. Commit & Task Isolation Rules (AGENTS.md Protocol)

Following the project's Spec-Driven Execution Protocol:
- **TDD First:** Write unit tests or Roborazzi screenshot test assertions before implementing production code for each task.
- **Atomic Tasks:** Execute, verify, and commit each task independently before proceeding to the next.
- **Commit Scope Separation:** Isolate non-functional refactoring (Commit A) from functional changes (Commit B).
- **Commit Titles:** Describe the *effect* of the change (e.g. "Prevent division by zero during scale zero transitions"), not the implementation details.
- **Verification Order:**
  1. `./gradlew :remotecompose:lottie:ktfmtFormat`
  2. `./gradlew :remotecompose:lottie:metalavaGenerateSignatureDebug`
  3. `./gradlew :remotecompose:lottie:compileDebugKotlin`
  4. `./gradlew :remotecompose:lottie:assembleDebug`
  5. `./gradlew :remotecompose:lottie:testDebugUnitTest --no-build-cache`
  6. `./gradlew :remotecompose:lottie:recordRoborazziDebug --no-build-cache` / `verifyRoborazziDebug`
  7. `./gradlew :remotecompose:lottie:check`

---

## 3. Progress Overview

- [ ] [Phase 1: Critical Bug Fixes & Serialization Hardening](#PL_LOTTIE_SPEC_PARITY_P1)
  - [x] [Task 1.1: AST Model Defaults & Fractional Framerate](#PL_LOTTIE_SPEC_PARITY_T1_1)
  - [x] [Task 1.2: GradientStroke Annotations & Keyframe Hold Flag Parsing](#PL_LOTTIE_SPEC_PARITY_T1_2)
  - [x] [Task 1.3: Transform Inversion Singularities (Scale = 0 Guard)](#PL_LOTTIE_SPEC_PARITY_T1_3)
  - [ ] [Task 1.4: PolyStar Dynamic RemoteLottiePath Refactoring](#PL_LOTTIE_SPEC_PARITY_T1_4)
  - [ ] [Task 1.5: Hierarchy Cycle Guard & Dynamic Track Matte Path Builder](#PL_LOTTIE_SPEC_PARITY_T1_5)
- [ ] [Phase 2: Core Rendering & Mathematical Parity](#PL_LOTTIE_SPEC_PARITY_P2)
  - [ ] [Task 2.1: Spatial Bézier Tangents (to, ti) in Position Keyframes](#PL_LOTTIE_SPEC_PARITY_T2_1)
  - [ ] [Task 2.2: Gradient Shaders for GradientFill and GradientStroke](#PL_LOTTIE_SPEC_PARITY_T2_2)
  - [ ] [Task 2.3: Stroke Dash Pattern (d) and Miter Limit (ml)](#PL_LOTTIE_SPEC_PARITY_T2_3)
  - [ ] [Task 2.4: Path FillRule (EvenOdd) & Primitive TrimPath Dispatch](#PL_LOTTIE_SPEC_PARITY_T2_4)
  - [ ] [Task 2.5: Local Layer Timing Scaling & Inverted Alpha Track Mattes](#PL_LOTTIE_SPEC_PARITY_T2_5)
- [ ] [Phase 3: Composition, Precomps & Layer Mask Pipeline](#PL_LOTTIE_SPEC_PARITY_P3)
  - [ ] [Task 3.1: Root assets[] Asset Registry Model](#PL_LOTTIE_SPEC_PARITY_T3_1)
  - [ ] [Task 3.2: PrecompLayer Recursive Sub-Composition Rendering Engine](#PL_LOTTIE_SPEC_PARITY_T3_2)
  - [ ] [Task 3.3: Layer Masks Pipeline (masksProperties)](#PL_LOTTIE_SPEC_PARITY_T3_3)
  - [ ] [Task 3.4: Precomposition Time Remapping (tm) & Timeline Markers](#PL_LOTTIE_SPEC_PARITY_T3_4)
- [ ] [Phase 4: Advanced Modifiers, Typography & Bitmap Assets](#PL_LOTTIE_SPEC_PARITY_P4)
  - [ ] [Task 4.1: Repeater Modifier Geometry Engine](#PL_LOTTIE_SPEC_PARITY_T4_1)
  - [ ] [Task 4.2: RoundedCorners Modifier on RemoteBezierValue](#PL_LOTTIE_SPEC_PARITY_T4_2)
  - [ ] [Task 4.3: MergePaths Boolean Operations Engine](#PL_LOTTIE_SPEC_PARITY_T4_3)
  - [ ] [Task 4.4: Bitmap Asset Loading & ImageLayer Rendering](#PL_LOTTIE_SPEC_PARITY_T4_4)
  - [ ] [Task 4.5: Vector Typography (TextLayer) Data Structures](#PL_LOTTIE_SPEC_PARITY_T4_5)

---

## 4. Detailed Tasks & Phases

### Phase 1: Critical Bug Fixes & Serialization Hardening {#PL_LOTTIE_SPEC_PARITY_P1}

#### Task 1.1: AST Model Defaults & Fractional Framerate {#PL_LOTTIE_SPEC_PARITY_T1_1}
- **Objective:** Add sensible defaults across all AST models in `format/` to prevent `MissingFieldException` runtime crashes, and change `Animation.frameRate` from `Int` to `Float = 30f` to support non-integer framerates (e.g. `29.97`, `23.976`, `59.94`).
- **Files to Modify:**
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/Animation.kt`
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/layer/SolidColorLayer.kt`
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/styles/Fill.kt`
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/styles/Stroke.kt`
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/geometry/Path.kt`
  - `remotecompose/lottie/src/test/java/com/google/android/horologist/remotecompose/lottie/format/LottieDecoderResilienceTest.kt`
- **Verify:** `./gradlew :remotecompose:lottie:testDebugUnitTest --no-build-cache`
- **Commit:** `feat(lottie): provide AST default values and support fractional frame rates`

#### Task 1.2: GradientStroke Annotations & Keyframe Hold Flag Parsing {#PL_LOTTIE_SPEC_PARITY_T1_2}
- **Objective:** Fix serial name annotations in `GradientStroke.kt` (`highlightLength` $\to$ `"h"`, `highlightAngle` $\to$ `"a"`) and update `ColorPropertyKeyframeSerializer` to parse both integer (`intOrNull == 1`) and boolean (`booleanOrNull == true`) hold keyframe flags.
- **Files to Modify:**
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/styles/GradientStroke.kt`
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/properties/Color.kt`
  - `remotecompose/lottie/src/test/java/com/google/android/horologist/remotecompose/lottie/format/ParsingTest.kt`
- **Verify:** `./gradlew :remotecompose:lottie:testDebugUnitTest --no-build-cache`
- **Commit:** `fix(lottie): align GradientStroke serial names and support boolean hold keyframes`

#### Task 1.3: Transform Inversion Singularities (Scale = 0 Guard) {#PL_LOTTIE_SPEC_PARITY_T1_3}
- **Objective:** Guard against division by zero in `Transform.inverseTransform` when `scaleX` or `scaleY` evaluates to zero or near-zero using `computeInverseScale`. (Note: Per After Effects and Lottie specification, layer parenting links affine spatial transforms but not layer opacity `ks.o`, which remains local to the layer; shape groups compound opacity hierarchically inside `RemoteGroup`).
- **Files to Modify:**
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/Transform.kt`
  - `remotecompose/lottie/src/test/java/com/google/android/horologist/remotecompose/lottie/TransformTest.kt`
- **Verify:** `./gradlew :remotecompose:lottie:testDebugUnitTest --no-build-cache`
- **Commit:** `fix(lottie): guard scale zero singularities in transform inversion`

#### Task 1.4: PolyStar Dynamic RemoteLottiePath Refactoring {#PL_LOTTIE_SPEC_PARITY_T1_4}
- **Objective:** Refactor `evaluatePolyStar` in `PolyStar.kt` to construct and return a `RemoteLottiePath` containing `RemoteBezierValue` vertices and tangents, enabling full affine transform baking in `GeometryTransform.kt` and animated property support.
- **Files to Modify:**
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/shapes/PolyStar.kt`
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/shapes/GeometryTransform.kt`
  - `remotecompose/lottie/src/test/java/com/google/android/horologist/remotecompose/lottie/PolyStarTest.kt`
- **Verify:** `./gradlew :remotecompose:lottie:testDebugUnitTest --no-build-cache`
- **Commit:** `refactor(lottie): convert PolyStar evaluator to dynamic RemoteLottiePath`

#### Task 1.5: Hierarchy Cycle Guard & Dynamic Track Matte Path Safety {#PL_LOTTIE_SPEC_PARITY_T1_5}
- **Objective:** Add recursion cycle detection (`visited: Set<Int>`) in `buildAncestorTransforms` inside `LottieAnimation.kt` to prevent stack overflows on corrupted assets, and safeguard dynamic path evaluation during matte clipping in `Shape.kt`.
- **Files to Modify:**
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/LottieAnimation.kt`
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/Shape.kt`
  - `remotecompose/lottie/src/test/java/com/google/android/horologist/remotecompose/lottie/AnimationTest.kt`
- **Verify:** `./gradlew :remotecompose:lottie:check`
- **Commit:** `fix(lottie): guard against circular transform hierarchies and protect matte paths`

---

### Phase 2: Core Rendering & Mathematical Parity {#PL_LOTTIE_SPEC_PARITY_P2}

#### Task 2.1: Spatial Bézier Tangents (to, ti) in Position Keyframes {#PL_LOTTIE_SPEC_PARITY_T2_1}
- **Objective:** In `renderer/properties/Position.kt`, evaluate 2D cubic Bézier spatial path curves $\mathbf{P}(s) = (1-s)^3 P_0 + 3(1-s)^2 s (P_0 + \mathbf{to}) + 3(1-s) s^2 (P_1 + \mathbf{ti}) + s^3 P_1$ when keyframes specify spatial outgoing (`to`) and incoming (`ti`) tangents.
- **Files to Modify:**
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/properties/Position.kt`
  - `remotecompose/lottie/src/test/java/com/google/android/horologist/remotecompose/lottie/PositionTest.kt`
- **Verify:** `./gradlew :remotecompose:lottie:testDebugUnitTest --no-build-cache`
- **Commit:** `feat(lottie): evaluate spatial bezier path tangents for curved position keyframes`

#### Task 2.2: Gradient Shaders for GradientFill and GradientStroke {#PL_LOTTIE_SPEC_PARITY_T2_2}
- **Objective:** Implement linear and radial gradient shader construction in `RemotePaint` for `RemoteGradientFill` and `RemoteGradientStroke` using interpolated `RemoteGradientValue` color and opacity stops.
- **Files to Modify:**
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/RemoteStyle.kt`
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/shapes/GradientFill.kt`
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/shapes/GradientStroke.kt`
- **Verify:** `./gradlew :remotecompose:lottie:recordRoborazziDebug --no-build-cache`
- **Commit:** `feat(lottie): render linear and radial gradient shaders for fills and strokes`

#### Task 2.3: Stroke Dash Pattern (d) and Miter Limit (ml) {#PL_LOTTIE_SPEC_PARITY_T2_3}
- **Objective:** Configure `RemotePaint.pathEffect` with animated dash patterns from `Stroke.dashPattern` and apply `Stroke.miterLimit` to `RemoteStroke.getPaint()`.
- **Files to Modify:**
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/RemoteStyle.kt`
  - `remotecompose/lottie/src/test/java/com/google/android/horologist/remotecompose/lottie/StrokeTest.kt`
- **Verify:** `./gradlew :remotecompose:lottie:testDebugUnitTest --no-build-cache`
- **Commit:** `feat(lottie): apply stroke dash patterns and miter limits to paint`

#### Task 2.4: Path FillRule (EvenOdd) & Primitive TrimPath Dispatch {#PL_LOTTIE_SPEC_PARITY_T2_4}
- **Objective:** Pass `FillRule` (`EvenOdd` vs `NonZero`) to `RemotePath` and wire `activeTrimPath` to `Rectangle`, `Ellipse`, and `PolyStar` evaluators in `Shape.kt`.
- **Files to Modify:**
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/Shape.kt`
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/RemoteShape.kt`
- **Verify:** `./gradlew :remotecompose:lottie:testDebugUnitTest --no-build-cache`
- **Commit:** `feat(lottie): support EvenOdd fill rule and apply trim paths to primitive shapes`

#### Task 2.5: Local Layer Timing Scaling & Inverted Alpha Track Mattes {#PL_LOTTIE_SPEC_PARITY_T2_5}
- **Objective:** Calculate local layer time $t_{\text{local}} = (t - st) / sr$ in `Layer.kt`, support inverted alpha track mattes (`tt: 2`), and resolve non-adjacent matte parent references (`tp`).
- **Files to Modify:**
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/LottieAnimation.kt`
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/layers/Layer.kt`
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/Shape.kt`
- **Verify:** `./gradlew :remotecompose:lottie:check`
- **Commit:** `feat(lottie): implement local layer time scaling and inverted alpha track mattes`

---

### Phase 3: Composition, Precomps & Layer Mask Pipeline {#PL_LOTTIE_SPEC_PARITY_P3}

#### Task 3.1: Root assets[] Asset Registry Model {#PL_LOTTIE_SPEC_PARITY_T3_1}
- **Objective:** Add polymorphic `assets: List<Asset>` to `format/Animation.kt` supporting `PrecompAsset`, `ImageAsset`, and `AudioAsset`.
- **Files to Create / Modify:**
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/asset/Asset.kt`
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/Animation.kt`
- **Verify:** `./gradlew :remotecompose:lottie:testDebugUnitTest --no-build-cache`
- **Commit:** `feat(lottie): define asset repository models for precompositions and images`

#### Task 3.2: PrecompLayer Recursive Sub-Composition Rendering Engine {#PL_LOTTIE_SPEC_PARITY_T3_2}
- **Objective:** Implement recursive sub-composition layer dispatch in `renderer/layers/PrecompLayer.kt` resolving child layers from `Animation.assets` using `refId`.
- **Files to Modify:**
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/layers/PrecompLayer.kt`
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/LottieAnimation.kt`
- **Verify:** `./gradlew :remotecompose:lottie:recordRoborazziDebug --no-build-cache`
- **Commit:** `feat(lottie): implement precomposition layer recursive rendering engine`

#### Task 3.3: Layer Masks Pipeline (masksProperties) {#PL_LOTTIE_SPEC_PARITY_T3_3}
- **Objective:** Add `masksProperties: List<Mask>` to `format/layer/Layer.kt` and implement canvas clipping in `ShapeLayer.kt` for `Add`, `Subtract`, `Intersect`, and `Inverted` masks.
- **Files to Create / Modify:**
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/mask/Mask.kt`
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/layer/Layer.kt`
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/layers/ShapeLayer.kt`
- **Verify:** `./gradlew :remotecompose:lottie:testDebugUnitTest --no-build-cache`
- **Commit:** `feat(lottie): implement layer masks AST and canvas clipping pipeline`

#### Task 3.4: Precomposition Time Remapping (tm) & Timeline Markers {#PL_LOTTIE_SPEC_PARITY_T3_4}
- **Objective:** Evaluate `PrecompLayer.timeRemap` property to override nested composition progress, and parse named `markers: List<Marker>` in `Animation.kt`.
- **Files to Modify:**
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/layers/PrecompLayer.kt`
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/Animation.kt`
- **Verify:** `./gradlew :remotecompose:lottie:check`
- **Commit:** `feat(lottie): support precomposition time remapping and timeline markers`

---

### Phase 4: Advanced Modifiers, Typography & Bitmap Assets {#PL_LOTTIE_SPEC_PARITY_P4}

#### Task 4.1: Repeater Modifier Geometry Engine {#PL_LOTTIE_SPEC_PARITY_T4_1}
- **Objective:** Implement multi-instance geometry duplication in `renderer/shapes/Repeater.kt` with incremental affine transform propagation and start/end opacity compounding.
- **Files to Modify:**
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/Shape.kt`
- **Verify:** `./gradlew :remotecompose:lottie:testDebugUnitTest --no-build-cache`
- **Commit:** `feat(lottie): implement repeater modifier geometry duplication engine`

#### Task 4.2: RoundedCorners Modifier on RemoteBezierValue {#PL_LOTTIE_SPEC_PARITY_T4_2}
- **Objective:** Evaluate `RoundedCorners` modifier radius on `RemoteBezierValue` vertices to dynamically round sharp path corners.
- **Files to Modify:**
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/shapes/Path.kt`
- **Verify:** `./gradlew :remotecompose:lottie:testDebugUnitTest --no-build-cache`
- **Commit:** `feat(lottie): evaluate rounded corners modifier on bezier paths`

#### Task 4.3: MergePaths Boolean Operations Engine {#PL_LOTTIE_SPEC_PARITY_T4_3}
- **Objective:** Support boolean path operations (Union, Subtract, Intersect, Exclude) on `RemotePath` for `MergePaths` modifiers.
- **Files to Modify:**
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/Shape.kt`
- **Verify:** `./gradlew :remotecompose:lottie:testDebugUnitTest --no-build-cache`
- **Commit:** `feat(lottie): support boolean merge path operations`

#### Task 4.4: Bitmap Asset Loading & ImageLayer Rendering {#PL_LOTTIE_SPEC_PARITY_T4_4}
- **Objective:** Decode embedded Base64 data-URL and resource images and render `ImageLayer` via `RemoteCanvas.drawImage`.
- **Files to Modify:**
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/layers/ImageLayer.kt`
- **Verify:** `./gradlew :remotecompose:lottie:testDebugUnitTest --no-build-cache`
- **Commit:** `feat(lottie): render bitmap image assets on ImageLayer`

#### Task 4.5: Vector Typography (TextLayer) Data Structures {#PL_LOTTIE_SPEC_PARITY_T4_5}
- **Objective:** Model text document data structures in `format/layer/TextLayer.kt` and render vector text glyphs.
- **Files to Modify:**
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/layer/TextLayer.kt`
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/layers/TextLayer.kt`
- **Verify:** `./gradlew :remotecompose:lottie:check`
- **Commit:** `feat(lottie): model typography text document structures`
