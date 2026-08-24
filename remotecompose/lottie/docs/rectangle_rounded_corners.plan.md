# Implementation Plan: Lottie Rectangle Rounded Corners Parity {#PL_LOTTIE_RECTANGLE_ROUNDED_CORNERS}

> **Code:** PL_LOTTIE_RECTANGLE_ROUNDED_CORNERS
> **Status:** in-progress
> **Created:** 2026-08-23
> **Updated:** 2026-08-23
>
> **Concept:** [Shapes Concept](shapes.concept.md)
> **Specification:** [Shapes Specification](shapes.sp.md)
> **Specification Reference:** [Lottie 1.0.1 Shapes](https://lottie.github.io/lottie-spec/1.0.1/single-page/#specs-shapes)
> **Depends on:** `PL_LOTTIE_SHAPES`, `PL_LOTTIE_SHAPES_FIXES`
> **Used by:** `renderer/shapes`, `MediaLottieDiffScreenshotTest`
>
> Implementation plan for fixing parametric rectangle rounded corner rendering discrepancies across static and animated properties in `:remotecompose:lottie` to achieve 100% pixel parity against reference `lottie-android`.

## Goal

Resolve the sharp/undersized rounded corner rendering bug in Lottie parametric rectangles (`Rectangle Path 1`, `ty: "rc"`) by:
1. **Dynamic Expression Evaluation:** Eliminating static `.constantValueOrNull` resolution in `evaluateRectangle` which collapsed animated corner radius and animated rectangle size to `0f`.
2. **Dynamic Bézier Geometry Construction:** Using RemoteCompose `RemoteFloat` state operators (`min`, `clamp`) to dynamically compute `clampedR` and Bézier tangent coefficient $k = 0.55228475 \times \text{clampedR}$ for all 8 path vertices and tangents.
3. **Clip Path Compatibility:** Ensuring `clipShapes` in `Shape.kt` delegates rectangle clipping directly to `evaluateRectangle`.
4. **100% Screenshot Parity:** Verifying `MediaLottieDiffScreenshotTest.m3Next` (and all other media screenshot diff tests) against reference `lottie-android` outputs at 0%, 50%, and 100% progress.

---

## Technology Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Geometry Representation | 8-vertex Cubic Bézier Loop | Standard Bézier circular-arc approximation ($k = 0.55228475 \times r$) supporting smooth transitions from $r = 0$ to $r = \min(w/2, h/2)$ |
| Dynamic State Operations | `androidx.compose.remote.creation.compose.state.{min, clamp}` | Evaluates dynamic expressions on `RemoteFloat` for animated properties without requiring constant value resolution at composition time |
| Scope & Visibility | `internal` | Maintains internal visibility of renderer helpers without altering public API signatures |

---

## Required Knowledge

| Kind | Ref | Applies to | Note |
|---|---|---|---|
| Rule | `AGENTS.md` Spec-Driven Protocol | All Tasks | Atomic tasks, format verification, commit isolation |
| Rule | `AGENTS.md` Testing Policy | Task 2 | Execute in strict order: format, compile, unit test, roborazzi |

---

## Progress

- [x] [Task 1: Dynamic Bézier Rectangle Geometry Evaluation in `Rectangle.kt` & `Shape.kt`](#PL_LOTTIE_RECT_CORNER_T1)
- [ ] [Task 2: End-to-End Test Suite & Roborazzi Screenshot Verification](#PL_LOTTIE_RECT_CORNER_T2)

---

## Tasks & Phases

### Task 1: Dynamic Bézier Rectangle Geometry Evaluation in `Rectangle.kt` & `Shape.kt` {#PL_LOTTIE_RECT_CORNER_T1}

- **Objective:** Refactor `evaluateRectangle` in `Rectangle.kt` to use dynamic `RemoteFloat` expressions for corner radius and bounding dimensions, and update `clipShapes` in `Shape.kt`.
- **Implementation Details:**
  1. In `Rectangle.kt`:
     - Import `androidx.compose.remote.creation.compose.state.clamp` and `androidx.compose.remote.creation.compose.state.min`.
     - Calculate `maxRadius = min(halfWidth, halfHeight)`.
     - Calculate `clampedR = clamp(cornerRadius, 0f.rf, maxRadius)`.
     - Compute `kr = clampedR * 0.55228475f` and `rr = clampedR`.
     - Construct 8 vertices and tangent pairs (`inTangents`, `outTangents`) using `pos`, `halfWidth`, `halfHeight`, `rr`, and `kr`.
     - Return `RemoteLottiePath(listOf(remoteBezier))`.
  2. In `Shape.kt`:
     - Update `clipShapes` for `Rectangle` to consistently use `evaluateRectangle` and `buildRemotePathFromBezier`.
- **Files to Modify:**
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/shapes/Rectangle.kt`
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/Shape.kt`
- **Verify:**
  - `./gradlew :remotecompose:lottie:ktfmtFormat`
  - `./gradlew :remotecompose:lottie:compileDebugKotlin`
  - `./gradlew :remotecompose:lottie:testDebugUnitTest --no-build-cache`

---

### Task 2: End-to-End Test Suite & Roborazzi Screenshot Verification {#PL_LOTTIE_RECT_CORNER_T2}

- **Objective:** Verify that `MediaLottieDiffScreenshotTest` (including `m3Next` at 0%, 50%, 100%) and all unit tests achieve 100% pixel parity.
- **Verification Commands:**
  1. `./gradlew :remotecompose:lottie:ktfmtFormat`
  2. `./gradlew :remotecompose:lottie:metalavaGenerateSignatureDebug`
  3. `./gradlew :remotecompose:lottie:compileDebugKotlin`
  4. `./gradlew :remotecompose:lottie:assembleDebug`
  5. `./gradlew :remotecompose:lottie:testDebugUnitTest --no-build-cache`
  6. `./gradlew :remotecompose:lottie:recordRoborazziDebug --no-build-cache`
  7. `./gradlew :remotecompose:lottie:verifyRoborazziDebug --no-build-cache`
  8. `./gradlew :remotecompose:lottie:check`
