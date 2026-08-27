# Implementation Plan: Lottie 1.0.1 Specification Parity Remediation & Hardening {#PL_LOTTIE_REMED}

> **Code:** PL_LOTTIE_REMED
> **Status:** in-progress
> **Created:** 2026-08-25
> **Updated:** 2026-08-25
>
> **Concept:** [Lottie Remediation Concept](lottie_remediation.concept.md) (`C_LOTTIE_REMED`)
> **Specification:** [Lottie Remediation Specification](lottie_remediation.sp.md) (`SP_LOTTIE_REMED`)
> **Source Audit:** [Lottie 1.0.1 Specification Audit & Visual Verification Analysis](lottie_audit_and_gap_analysis.md) (`DOC_LOTTIE_AUDIT_2026_V3`)
> **Depends on:** none
> **Used by:** `format`, `renderer`, `MediaLottieDiffScreenshotTest`, `LottieFeatureDiffScreenshotTest`
>
> Actionable, step-by-step implementation plan to resolve all critical rendering bugs, mathematical regressions, compositing defects, and visual diff discrepancies identified in `DOC_LOTTIE_AUDIT_2026_V3`.

---

## 1. Goal

Eliminate all visual rendering regressions and mathematical defects across `:remotecompose:lottie`:
1. **Critical Rendering & Data Loss Fixes (Phase 1):** Solid fill opacity compounding, vector glyph harvesting in `TextLayer`, EvenOdd fill rule canvas dispatch, sub-frame Look-Up Table easing lerping, and inverted alpha/non-adjacent track mattes.
2. **Core Algorithm, Math Safety & Modifier Compounding (Phase 2):** Dynamic primitive keyframe evaluation in `Rectangle`/`Ellipse`/`PolyStar`, symmetric scale-zero transform clamping, native `ImageLayer` bitmap source rect scaling, multi-mask `Add` mode union merging, canvas skew axis rotation ordering, and `Repeater` group compounding.
3. **Typography Parity & Verification Hardening (Phase 3):** Multiline text and stroke-over-fill ordering, stroke miter limit application to `RemotePaint`, gradient stop array bounds protection, precomposition canvas boundary clipping, and full Roborazzi golden baseline verification.

---

## 2. Commit & Task Isolation Rules (AGENTS.md Protocol)

- **TDD First:** Write unit test assertions or Roborazzi screenshot test assertions before implementing production code for each task.
- **Atomic Tasks:** Execute, verify, and commit each task independently before proceeding to the next.
- **Commit Scope Separation:** Isolate non-functional refactoring (Commit A) from functional changes (Commit B).
- **Verification Execution Order:**
  1. `./gradlew :remotecompose:lottie:ktfmtFormat`
  2. `./gradlew :remotecompose:lottie:metalavaGenerateSignatureDebug`
  3. `./gradlew :remotecompose:lottie:compileDebugKotlin`
  4. `./gradlew :remotecompose:lottie:assembleDebug`
  5. `./gradlew :remotecompose:lottie:testDebugUnitTest --no-build-cache`
  6. `./gradlew :remotecompose:lottie:verifyRoborazziDebug`
  7. `./gradlew :remotecompose:lottie:check`

---

## 3. Progress Overview

- [x] [Phase 1: Critical Rendering & Data Loss Fixes](#PL_LOTTIE_REMED_P1)
  - [x] [Task 1.1: Solid Fill Opacity Compounding in RemoteFill & Shape.kt](#PL_LOTTIE_REMED_T1_1)
  - [x] [Task 1.2: TextLayer Vector Glyph Harvesting in gatherShapes](#PL_LOTTIE_REMED_T1_2)
  - [x] [Task 1.3: EvenOdd Fill Rule Canvas Path Dispatch](#PL_LOTTIE_REMED_T1_3)
  - [x] [Task 1.4: Sub-Frame Look-Up Table Easing Linear Interpolation](#PL_LOTTIE_REMED_T1_4)
  - [x] [Task 1.5: Non-Adjacent Matte Routing & [BLOCKED] Inverted Alpha Clipping](#PL_LOTTIE_REMED_T1_5)
- [x] [Phase 2: Core Algorithm, Math Safety & Modifier Compounding](#PL_LOTTIE_REMED_P2)
  - [x] [Task 2.1: Animated Primitive Keyframe Evaluation in Rectangle, Ellipse, PolyStar](#PL_LOTTIE_REMED_T2_1)
  - [x] [Task 2.2: Scale-Zero Singularity Matrix Symmetry in Transform.kt](#PL_LOTTIE_REMED_T2_2)
  - [x] [Task 2.3: ImageLayer Native Bitmap Bounds Scaling](#PL_LOTTIE_REMED_T2_3)
  - [x] [Task 2.4: Multi-Mask Add Mode Composite Path Union](#PL_LOTTIE_REMED_T2_4)
  - [x] [Task 2.5: Canvas Skew Axis Rotation Ordering & Repeater Group Compounding](#PL_LOTTIE_REMED_T2_5)
- [x] [Phase 3: Extended Parity, Typography & Verification Hardening](#PL_LOTTIE_REMED_P3)
  - [x] [Task 3.1: Multiline Text, Line Height & Stroke-Over-Fill in TextLayer](#PL_LOTTIE_REMED_T3_1)
  - [x] [Task 3.2: Stroke Miter Limit & Gradient Stop Boundary Guards](#PL_LOTTIE_REMED_T3_2)
  - [x] [Task 3.3: Precomp Canvas Boundary Clipping & Roborazzi Baseline Verification](#PL_LOTTIE_REMED_T3_3)
- [ ] [Phase 4: Actionable Extended Modifiers, Arc Length Trim Path & Keyframe Parity](#PL_LOTTIE_REMED_P4)
  - [x] [Task 4.1: Dynamic Arc-Length Segmenting in Animated Trim Path](#PL_LOTTIE_REMED_T4_1)
  - [ ] [Task 4.2: Extended Graphic Modifiers Rendering (ZigZag, Pucker/Bloat, Twist, OffsetPath)](#PL_LOTTIE_REMED_T4_2)
  - [ ] [Task 4.3: Multi-Dimensional Easing Tangent Vector Support](#PL_LOTTIE_REMED_T4_3)
  - [ ] [Task 4.4: Hidden Layers as Track Matte Sources](#PL_LOTTIE_REMED_T4_4)
- [ ] [BLOCKED UPSTREAM] [Phase 5: Canvas-Level Difference Clipping & Subtractive Mask Parity](#PL_LOTTIE_REMED_P5)
  - [ ] [Task 5.1: Inverted Alpha & Inverted Luma Track Matte Canvas Clipping](#PL_LOTTIE_REMED_T5_1)
  - [ ] [Task 5.2: Subtractive & Difference Layer Masks Canvas Clipping](#PL_LOTTIE_REMED_T5_2)

---

## 4. Detailed Tasks & Phases

### Phase 1: Critical Rendering & Data Loss Fixes {#PL_LOTTIE_REMED_P1}

#### Task 1.1: Solid Fill Opacity Compounding in RemoteFill & Shape.kt {#PL_LOTTIE_REMED_T1_1}
- **Objective:** Evaluate `fill.opacity` (`o`) in `Shape.kt:fill()` and pass it to `RemoteFill`, multiplying $\alpha_{\text{final}} = \alpha_{\text{fillColor}} \times (\text{opacity} / 100) \times \alpha_{\text{inherited}}$.
- **Files to Modify:**
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/RemoteStyle.kt`
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/Shape.kt`
  - `remotecompose/lottie/src/test/java/com/google/android/horologist/remotecompose/lottie/renderer/StyleTest.kt`
- **Verify:** `SP_LOTTIE_REMED_03_01` — `./gradlew :remotecompose:lottie:testDebugUnitTest --no-build-cache`
- **Commit:** `fix(lottie): compound fill opacity into RemoteFill paint alpha`

#### Task 1.2: TextLayer Vector Glyph Harvesting in gatherShapes {#PL_LOTTIE_REMED_T1_2}
- **Objective:** Update `Shape.kt:gatherShapes` to accept `inheritedStyle: ShapeStyle?` so unstyled glyph geometries in `chars[].data.shapes` are preserved and styled by `TextLayer`.
- **Files to Modify:**
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/Shape.kt`
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/layers/TextLayer.kt`
  - `remotecompose/lottie/src/test/java/com/google/android/horologist/remotecompose/lottie/LottieFeatureDiffScreenshotTest.kt`
- **Verify:** `SP_LOTTIE_REMED_03_02` — `./gradlew :remotecompose:lottie:testDebugUnitTest --no-build-cache`
- **Commit:** `fix(lottie): preserve and style unstyled vector glyphs in TextLayer`

#### Task 1.3: EvenOdd Fill Rule Canvas Path Dispatch {#PL_LOTTIE_REMED_T1_3}
- **Objective:** Set `path.fillType = PathFillType.EvenOdd` in `RemoteShape.kt` (`RemoteLottiePath` and `RemoteCompiledPath`) when `fillRule == FillRule.EvenOdd`.
- **Files to Modify:**
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/RemoteShape.kt`
  - `remotecompose/lottie/src/test/java/com/google/android/horologist/remotecompose/lottie/LottieFeatureDiffScreenshotTest.kt`
- **Verify:** `SP_LOTTIE_REMED_03_03` — `./gradlew :remotecompose:lottie:testDebugUnitTest --no-build-cache`
- **Commit:** `fix(lottie): apply EvenOdd fill rule to canvas remote paths`

#### Task 1.4: Sub-Frame Look-Up Table Easing Linear Interpolation {#PL_LOTTIE_REMED_T1_4}
- **Objective:** Implement piecewise linear interpolation (`lerp`) between Look-Up Table indices in `Animation.kt:lookupValueInBezier` for fractional progress evaluations.
- **Files to Modify:**
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/Animation.kt`
  - `remotecompose/lottie/src/test/java/com/google/android/horologist/remotecompose/lottie/MediaLottieDiffScreenshotTest.kt`
- **Verify:** `SP_LOTTIE_REMED_03_04` — `./gradlew :remotecompose:lottie:testDebugUnitTest --no-build-cache`
- **Commit:** `fix(lottie): interpolate fractional look-up table frames in bezier easing`

#### Task 1.5: Non-Adjacent Matte Routing & [BLOCKED] Inverted Alpha Clipping {#PL_LOTTIE_REMED_T1_5}
- **Objective:** Resolve `matteParent` index lookup across non-adjacent layer indices and ensure hidden layers (`hd: true`) contribute as matte sources.
- **Upstream Blocker:** Canvas-level inverted alpha clipping (`tt: 2`, `ClipOp.Difference`) is **blocked** by the upstream `androidx.compose.remote` `RemoteCanvas.clipPath` bug. **Do not attempt workarounds or implementations of inverted alpha clipping until upstream is fixed.**
- **Files Modified:**
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/LottieAnimation.kt`
  - `remotecompose/lottie/src/test/java/com/google/android/horologist/remotecompose/lottie/LottieFeatureDiffScreenshotTest.kt`
- **Verify:** `SP_LOTTIE_REMED_03_06` — `./gradlew :remotecompose:lottie:testDebugUnitTest --no-build-cache`
- **Commit:** `fix(lottie): support non-adjacent matte routing`

---

### Phase 2: Core Algorithm, Math Safety & Modifier Compounding {#PL_LOTTIE_REMED_P2}

#### Task 2.1: Animated Primitive Keyframe Evaluation in Rectangle, Ellipse, PolyStar {#PL_LOTTIE_REMED_T2_1}
- **Objective:** Replace `.constantValueOrNull ?: 0f` unwrapping in `Rectangle.kt`, `Ellipse.kt`, and `PolyStar.kt` with dynamic keyframed Bézier property synthesis.
- **Files to Modify:**
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/shapes/Rectangle.kt`
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/shapes/Ellipse.kt`
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/shapes/PolyStar.kt`
  - `remotecompose/lottie/src/test/java/com/google/android/horologist/remotecompose/lottie/renderer/ShapeEvaluationTest.kt`
- **Verify:** `SP_LOTTIE_REMED_03_07` — `./gradlew :remotecompose:lottie:testDebugUnitTest --no-build-cache`
- **Commit:** `fix(lottie): evaluate animated keyframes on parametric shape primitives`

#### Task 2.2: Scale-Zero Singularity Matrix Symmetry in Transform.kt {#PL_LOTTIE_REMED_T2_2}
- **Objective:** Symmetrically clamp both forward scale ($s_{\text{clamped}} = \max(|s|, 10^{-4})$) and inverse scale in `Transform.kt` to prevent permanent matrix scaling down on sibling layers.
- **Files to Modify:**
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/Transform.kt`
  - `remotecompose/lottie/src/test/java/com/google/android/horologist/remotecompose/lottie/renderer/TransformTest.kt`
- **Verify:** `./gradlew :remotecompose:lottie:testDebugUnitTest --no-build-cache`
- **Commit:** `fix(lottie): balance forward and inverse scale-zero singularity clamping`

#### Task 2.3: ImageLayer Native Bitmap Bounds Scaling {#PL_LOTTIE_REMED_T2_3}
- **Objective:** Use `bitmap.width` and `bitmap.height` for `srcRight` and `srcBottom` in `ImageLayer.kt:drawScaledBitmap` to scale 1x1 and high-DPI bitmaps correctly.
- **Files to Modify:**
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/layers/ImageLayer.kt`
  - `remotecompose/lottie/src/test/java/com/google/android/horologist/remotecompose/lottie/LottieFeatureDiffScreenshotTest.kt`
- **Verify:** `SP_LOTTIE_REMED_03_08` — `./gradlew :remotecompose:lottie:testDebugUnitTest --no-build-cache`
- **Commit:** `fix(lottie): use native bitmap bounds in ImageLayer source rect scaling`

#### Task 2.4: Multi-Mask Add Mode Composite Path Union {#PL_LOTTIE_REMED_T2_4}
- **Objective:** Merge multiple `MaskMode.Add` masks into a single compound path using `Path.Op.UNION` in `Shape.kt:applyLayerMasks` before clipping canvas.
- **Files to Modify:**
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/Shape.kt`
  - `remotecompose/lottie/src/test/java/com/google/android/horologist/remotecompose/lottie/renderer/MaskTest.kt`
- **Verify:** `SP_LOTTIE_REMED_03_09` — `./gradlew :remotecompose:lottie:testDebugUnitTest --no-build-cache`
- **Commit:** `fix(lottie): combine multiple Add masks using boolean path union`

#### Task 2.5: Canvas Skew Axis Rotation Ordering & Repeater Group Compounding {#PL_LOTTIE_REMED_T2_5}
- **Objective:** Invert rotation call sequence in `renderer/Transform.kt` to match analytical `GeometryTransform.kt`; recursively transform child shapes in `Repeater.kt` for `RemoteGroup` nodes.
- **Files to Modify:**
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/Transform.kt`
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/shapes/Repeater.kt`
  - `remotecompose/lottie/src/test/java/com/google/android/horologist/remotecompose/lottie/renderer/RepeaterTest.kt`
- **Verify:** `SP_LOTTIE_REMED_03_10` — `./gradlew :remotecompose:lottie:testDebugUnitTest --no-build-cache`
- **Commit:** `fix(lottie): align canvas skew rotation order and compound Repeater groups`

---

### Phase 3: Extended Parity, Typography & Verification Hardening {#PL_LOTTIE_REMED_P3}

#### Task 3.1: Multiline Text, Line Height & Stroke-Over-Fill in TextLayer {#PL_LOTTIE_REMED_T3_1}
- **Objective:** Advance Y position by `lineHeight` on newline characters in `TextLayer.kt`, and reorder fill/stroke drawing according to `TextDocument.strokeOverFill`.
- **Files to Modify:**
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/layers/TextLayer.kt`
  - `remotecompose/lottie/src/test/java/com/google/android/horologist/remotecompose/lottie/renderer/TextLayerTest.kt`
- **Verify:** `./gradlew :remotecompose:lottie:testDebugUnitTest --no-build-cache`
- **Commit:** `feat(lottie): support multiline text layout and stroke-over-fill drawing order`

#### Task 3.2: Stroke Miter Limit & Gradient Stop Boundary Guards {#PL_LOTTIE_REMED_T3_2}
- **Objective:** Apply `strokeMiterLimit` in `RemoteStroke.getPaint()`, clamp color stop counts in `extractGradientColorsAndPositions`, and guard zero-duration keyframes in `Animation.kt`.
- **Files to Modify:**
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/RemoteStyle.kt`
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/Animation.kt`
  - `remotecompose/lottie/src/test/java/com/google/android/horologist/remotecompose/lottie/renderer/StyleTest.kt`
- **Verify:** `./gradlew :remotecompose:lottie:testDebugUnitTest --no-build-cache`
- **Commit:** `fix(lottie): apply paint miter limits and guard gradient stop boundaries`

#### Task 3.3: Precomp Canvas Boundary Clipping & Roborazzi Baseline Verification {#PL_LOTTIE_REMED_T3_3}
- **Objective:** Enforce `canvas.clipRect(0, 0, width, height)` in `PrecompLayer.kt`, and verify all unit and Roborazzi screenshot diff tests.
- **Files to Modify:**
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/layers/PrecompLayer.kt`
  - `remotecompose/lottie/src/test/java/com/google/android/horologist/remotecompose/lottie/LottieFeatureDiffScreenshotTest.kt`
- **Verify:** `./gradlew :remotecompose:lottie:check`
- **Commit:** `fix(lottie): clip precomposition canvas boundaries and verify test baselines`

---

### Phase 4: Actionable Extended Modifiers, Arc Length Trim Path & Keyframe Parity {#PL_LOTTIE_REMED_P4}

#### Task 4.1: Dynamic Arc-Length Segmenting in Animated Trim Path {#PL_LOTTIE_REMED_T4_1}
- **Objective:** Eliminate chord distortion when trimming animated curved paths (`trimPathAnimated_frame*`). Replace endpoint-only Cartesian lerping with continuous dynamic de Casteljau segment sampling evaluated at the exact frame trim offset.
- **Files to Modify:**
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/shapes/TrimPath.kt`
  - `remotecompose/lottie/src/test/java/com/google/android/horologist/remotecompose/lottie/LottieFeatureDiffScreenshotTest.kt`
- **Verify:** `./gradlew :remotecompose:lottie:testDebugUnitTest --no-build-cache`
- **Commit:** `fix(lottie): evaluate animated trim path curves via dynamic de Casteljau arc segmenting`

#### Task 4.2: Extended Graphic Modifiers Rendering (ZigZag, Pucker/Bloat, Twist, OffsetPath) {#PL_LOTTIE_REMED_T4_2}
- **Objective:** Implement geometry evaluators in `renderer/shapes/` for Lottie modifiers whose AST models are already parsed: ZigZag (`zz`), Pucker/Bloat (`pb`), Twist (`tw`), and OffsetPath (`op`).
- **Files to Modify:**
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/Shape.kt`
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/shapes/`
  - `remotecompose/lottie/src/test/java/com/google/android/horologist/remotecompose/lottie/renderer/ShapeEvaluationTest.kt`
- **Verify:** `./gradlew :remotecompose:lottie:testDebugUnitTest --no-build-cache`
- **Commit:** `feat(lottie): implement geometry modifiers for ZigZag, Pucker/Bloat, Twist, and OffsetPath`

#### Task 4.3: Multi-Dimensional Easing Tangent Vector Support {#PL_LOTTIE_REMED_T4_3}
- **Objective:** Support multi-dimensional keyframe easing tangents in `Scalar.kt` and `Vector.kt` when properties have split dimensions or multidimensional Bezier control points.
- **Files to Modify:**
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/properties/Scalar.kt`
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/properties/Vector.kt`
  - `remotecompose/lottie/src/test/java/com/google/android/horologist/remotecompose/lottie/KeyframeEasingTest.kt`
- **Verify:** `./gradlew :remotecompose:lottie:testDebugUnitTest --no-build-cache`
- **Commit:** `fix(lottie): parse multidimensional keyframe easing tangents`

#### Task 4.4: Hidden Layers as Track Matte Sources {#PL_LOTTIE_REMED_T4_4}
- **Objective:** Ensure layers with `hidden == true` (`hd: true`) contribute their geometry to track matte clipping without rendering themselves, fulfilling Lottie 1.0.1 specification Section 2.2.
- **Files to Modify:**
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/Shape.kt`
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/layers/Layer.kt`
  - `remotecompose/lottie/src/test/java/com/google/android/horologist/remotecompose/lottie/LottieFeatureDiffScreenshotTest.kt`
- **Verify:** `./gradlew :remotecompose:lottie:testDebugUnitTest --no-build-cache`
- **Commit:** `fix(lottie): allow hidden layers to act as track matte sources`

---

### Phase 5: [BLOCKED UPSTREAM] Canvas-Level Difference Clipping & Subtractive Mask Parity {#PL_LOTTIE_REMED_P5}

#### Task 5.1: Inverted Alpha & Inverted Luma Track Matte Canvas Clipping {#PL_LOTTIE_REMED_T5_1}
- **Status:** **[BLOCKED - DO NOT PROCEED]**
- **Blocker Description:** Blocked by upstream `androidx.compose.remote` bug where `RemoteCanvas.clipPath` ignores `ClipOp.Difference` and serializes all clips as `INTERSECT`.
- **Action:** Postpone implementation until `androidx.compose.remote` provides `addClipPath(int pathId, int regionOp)` support.

#### Task 5.2: Subtractive & Difference Layer Masks Canvas Clipping {#PL_LOTTIE_REMED_T5_2}
- **Status:** **[BLOCKED - DO NOT PROCEED]**
- **Blocker Description:** Blocked by upstream `androidx.compose.remote` bug where `RemoteCanvas.clipPath` ignores `ClipOp.Difference` for `MaskMode.Subtract` and `MaskMode.Difference`.
- **Action:** Postpone implementation until `androidx.compose.remote` provides `addClipPath(int pathId, int regionOp)` support.

---

## 5. Upstream Library Blockers (`androidx.compose.remote`)

### 5.1 `RemoteCanvas.clipPath` Ignores `clipOp` & Omits `regionOp` Wire Serialization

- **Upstream Modules Affected:** `androidx.compose.remote:remote-creation-compose`, `androidx.compose.remote:remote-creation-core`, `androidx.compose.remote:remote-core`
- **Issue Summary:** 
  1. `RemoteCanvas.clipPath(path: RemotePath, clipOp: ClipOp = ClipOp.Intersect)` accepts a `clipOp` parameter but ignores it, unconditionally invoking `document.addClipPath(pathId)`.
  2. `RemoteComposeWriter` and `RemoteComposeBuffer` lack an `addClipPath(int pathId, int regionOp)` overload.
  3. `ClipPath.apply(buffer, id)` writes only `id`, setting high bits to `0` instead of packing `regionOp` (which `ClipPath.read()` expects via `pack >> 24`).
  4. Both Compose and Android View players receive `regionOp = 0`, causing all `clipPath` calls to execute as `ClipOp.Intersect`.
- **Policy Directive:**
  - **DO NOT PROCEED** with implementing features or workarounds that depend on canvas-level `ClipOp.Difference` / `clipPath` until the upstream fix lands in `androidx.compose.remote`.
  - All dependent tests (e.g. `layerMaskSolidSubtract`, `trackMatteInvertedAlpha`) remain documented as blocked on upstream.
