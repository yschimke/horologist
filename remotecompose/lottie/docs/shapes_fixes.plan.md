# Implementation Plan: Lottie Graphic Elements & Modifiers Parity {#PL_LOTTIE_SHAPES_FIXES}

> **Code:** PL_LOTTIE_SHAPES_FIXES
> **Status:** completed
> **Created:** 2026-08-22
> **Updated:** 2026-08-22
>
> **Concept:** [Shapes Concept](shapes.concept.md)
> **Specification:** [Shapes Specification](shapes.sp.md)
> **Specification Reference:** [Lottie 1.0.1 Shapes](https://lottie.github.io/lottie-spec/1.0.1/single-page/#specs-shapes)
> **Depends on:** `PL_LOTTIE_SHAPES`
> **Used by:** `format/graphicelement`, `renderer`, `MediaLottieDiffScreenshotTest`
>
> Implementation plan for completing remaining Lottie Graphic Elements gaps: implementing dynamic `TrimPath` evaluator for vector stroke animations (`mute_to_unmute`), evaluating `skew` and `skewAxis` in `renderer/Transform.kt`, adding AST models for remaining Lottie 1.0.1 modifiers (`OffsetPath`, `PuckerBloat`, `Twist`, `ZigZag`), and verifying screenshot parity.

## Goal

Resolve the remaining architectural and rendering gaps identified in the shapes audit:
1. **Dynamic Path Trimming (`TrimPath`):** Enable path trimming on `RemoteLottiePath` to render animated stroke transitions (such as the animated mute slash line in `mute_to_unmute`).
2. **Matrix Skew Support (`Transform`):** Evaluate `skew` (`sk`) and `skewAxis` (`sa`) properties in `renderer/Transform.kt`.
3. **Lottie 1.0.1 Modifier AST Completeness:** Add data models and serializers for `OffsetPath` (`op`), `PuckerBloat` (`pb`), `Twist` (`tw`), and `ZigZag` (`zz`).
4. **Verification & Screenshot Parity:** Ensure all unit tests, Metalava signature checks, and Roborazzi screenshot tests pass cleanly.

---

## Commit & Task Isolation Rules (AGENTS.md Protocol)

Following the project's Spec-Driven Execution Protocol:
- **Atomic Commits:** Each task must be executed, verified, and committed independently before proceeding to the next.
- **Commit Scope Separation:** Non-functional refactoring (Commit A) must be committed separately from functional feature additions (Commit B).
- **Commit Titles:** Must describe the *effect* of the change (e.g. "Animate path trimming for stroked vector lines"), not implementation details.

---

## Tasks & Phases

### Task 1: Complete Lottie 1.0.1 AST Modifiers {#PL_LOTTIE_SHAPES_FIXES_T1}

- **Objective:** Add data models, enums, and serializers for the remaining four Lottie 1.0.1 shape modifiers.
- **Files to Create / Modify:**
  - Create `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/modifiers/OffsetPath.kt` (`"op"`: `amount`, `lineJoin`, `miterLimit`).
  - Create `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/modifiers/PuckerBloat.kt` (`"pb"`: `amount`).
  - Create `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/modifiers/Twist.kt` (`"tw"`: `angle`, `center`).
  - Create `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/modifiers/ZigZag.kt` (`"zz"`: `size`, `ridgesPerSegment`, `pointType`).
  - Update `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/GraphicElement.kt` (`ShapeType` enum and `GraphicElementSerializer` mappings).
  - Add deserialization unit tests in `ParsingTest.kt` and resilience tests in `LottieDecoderResilienceTest.kt`.
- **Commit Scope:**
  - **Commit A (Functional):** `Add AST models and serializers for OffsetPath, PuckerBloat, Twist, and ZigZag modifiers`

---

### Task 2: Implement 2D Skew Transformation in Renderer {#PL_LOTTIE_SHAPES_FIXES_T2}

- **Objective:** Apply `transform.skew` (`sk`) and `transform.skewAxis` (`sa`) to `RemoteCanvas` during transform execution.
- **Mathematical Specification:**
  - When `skew` is non-zero:
    1. Rotate canvas by $-\text{skewAxis}$ (aligning skew axis with horizontal axis).
    2. Apply horizontal skew matrix / tilt with angle $\text{skew}$.
    3. Rotate canvas back by $+\text{skewAxis}$.
- **Files to Modify:**
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/Transform.kt`
  - Add unit / diff tests verifying transform skew rendering.
- **Commit Scope:**
  - **Commit B (Functional):** `Apply skew and skew axis transformations to rendered shape hierarchies`

---

### Task 3: Implement Dynamic Bézier Path Trimming (`TrimPath`) {#PL_LOTTIE_SHAPES_FIXES_T3}

- **Objective:** Evaluate `TrimPath` (`s`, `e`, `o`, `m`) to dynamically trim Bézier curves on `RemoteLottiePath` in `gatherShapes`.
- **Implementation Strategy:**
  - **Refactor (Non-functional):** Extract path curve sampling and Bézier length measurement utilities into `renderer/shapes/TrimPathEvaluator.kt`.
  - **Feature (Functional):**
    - Compute animated `start`, `end`, and `offset` values mapped to normalized path interval $[0, 1]$.
    - Implement de Casteljau cubic subdivision to trim cubic Bézier segments to exact fractional lengths.
    - Wire `TrimPath` handling into `renderer/Shape.kt` (`gatherShapes`) for sibling geometry shapes.
- **Files to Create / Modify:**
  - Create `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/shapes/TrimPathEvaluator.kt`
  - Modify `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/Shape.kt`
  - Modify `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/RemoteShape.kt`
- **Commit Scope:**
  - **Commit A (Non-Functional Refactoring):** `Extract Bézier curve segment sampling utilities for path trimming`
  - **Commit B (Functional):** `Animate path trimming for stroked vector lines and mute transitions`

---

### Task 4: End-to-End Test Suite Verification & Screenshot Diff Audit {#PL_LOTTIE_SHAPES_FIXES_T4}

- **Objective:** Verify that all screenshot diff tests (`m3_next`, `volume_up`, `volume_down`, `mute_to_unmute`, `unmute_to_mute`) pass Roborazzi verification against reference output.
- **Verification Commands:**
  1. `./gradlew :remotecompose:lottie:ktfmtFormat`
  2. `./gradlew :remotecompose:lottie:metalavaGenerateSignatureDebug`
  3. `./gradlew :remotecompose:lottie:compileDebugKotlin`
  4. `./gradlew :remotecompose:lottie:assembleDebug`
  5. `./gradlew :remotecompose:lottie:testDebugUnitTest --no-build-cache`
  6. `./gradlew :remotecompose:lottie:check`
  7. `./gradlew :remotecompose:lottie:verifyRoborazziDebug`
- **Commit Scope:**
  - **Commit (Verification):** `Update screenshot baselines and enable full graphic element test coverage`

---

## Alternatives Considered

| Approach | Pros | Cons | Verdict |
|---|---|---|---|
| **Trim at draw-time on `RemoteCanvas` via `PathMeasure`** | Relies on platform `PathMeasure` | `RemoteCanvas` operates on deferred commands and does not expose synchronous Android `PathMeasure` during recording | Rejected: Bézier segment subdivision must occur during format evaluation into `RemoteBezierValue` |
| **Bézier segment subdivision in `RemoteLottiePath` evaluation [Selected]** | Fully compatible with `RemoteCompose` serialization and animation interpolation | Requires mathematical cubic curve splitting logic | Chosen: Direct evaluation preserves remote-rendering capability |
