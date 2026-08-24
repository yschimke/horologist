# Implementation Plan: Track Matte Masking & Container Style Resolution {#PL_LOTTIE_MATTE}

> **Code:** PL_LOTTIE_MATTE
> **Status:** done
> **Created:** 2026-08-23
> **Updated:** 2026-08-23
>
> **Concept:** [Track Matte Concept](track_matte_and_styles.concept.md)
> **Specification:** [Track Matte Specification](track_matte_and_styles.sp.md)
> **Depends on:** `C_LOTTIE_MATTE`, `SP_LOTTIE_MATTE`
> **Used by:** `Shape.kt`, `ShapeLayer.kt`, `LottieAnimation.kt`, `MediaLottieDiffScreenshotTest.kt`
>
> Implementation plan for container-level shape style scope resolution across sibling groups and Alpha Track Matte layer pairing with canvas clipping in `:remotecompose:lottie`.

## Goal

Achieve exact visual parity against reference `lottie-android` for media vector animations (`volume_down`, `volume_up`, `unmute_to_mute`, `mute_to_unmute`):
1. **Container Style Resolution:** Accumulate sibling group geometries in container scope using `createStyledGroup` so outer `Fill`/`Stroke` styles correctly paint inner group paths under group transforms.
2. **Track Matte Layer Pairing & Canvas Clipping:** Suppress direct rendering of matte source layers (`td: 1`), apply canvas clipping (`RemoteCanvas.clipRect`/`clipPath`), and invert transforms via `inverseTransform()` to preserve root device clip without clearing canvas state.
3. **Dynamic Parametric Shapes & Opacity Propagation:** Convert `evaluateEllipse` to `RemoteLottiePath` for dynamic keyframed sizes and propagate layer/group opacity to `RemoteStyle.getPaint(inheritedOpacity)` while suppressing 0-width hairline strokes.
4. **Verification & Golden Baselines:** Verify with `./gradlew :remotecompose:lottie:check`, record Roborazzi baselines, and verify 100% pixel match.

---

## Tasks & Phases

### Task 1: Container & Sibling Group Style Scope Resolution {#PL_LOTTIE_MATTE_T1}

- **Status:** Done
- **Objective:** Enable outer/container styles (`Fill`, `Stroke`, `GradientFill`, `GradientStroke`) in `gatherShapes()` to bind to geometries inside preceding sibling child groups.
- **Files Modified:**
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/Shape.kt`
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/RemoteShape.kt`
- **Implementation Details:**
  - In `gatherShapes()`, use `createStyledGroup` to wrap sibling group geometries with container-level styles into `RemoteGroup` instances while preserving group transforms.
- **Verification:**
  - `./gradlew :remotecompose:lottie:compileDebugKotlin`
  - `./gradlew :remotecompose:lottie:testDebugUnitTest --no-build-cache`

---

### Task 2: Track Matte Layer Pairing & Canvas Geometry Clipping {#PL_LOTTIE_MATTE_T2}

- **Status:** Done
- **Objective:** Pair matte source layers (`td == 1`) with target layers (`tt == 1`), suppress standalone matte rendering, and apply canvas clipping in `ShapeLayer`.
- **Files Modified:**
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/LottieAnimation.kt`
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/layers/Layer.kt`
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/layers/ShapeLayer.kt`
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/Shape.kt`
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/Transform.kt`
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/shapes/Ellipse.kt`
- **Implementation Details:**
  - In `LottieAnimation.kt`, filter out layers with `matteTarget == 1` from direct composable rendering and pair with target layers.
  - In `Transform.kt`, implemented `inverseTransform()` to invert canvas matrix after matte clipping without popping the clip stack.
  - In `Ellipse.kt`, converted `evaluateEllipse()` to return `RemoteLottiePath` for dynamic keyframed size animation.
  - In `RemoteStyle.kt` and `RemoteShape.kt`, propagated layer/group opacity to paints and suppressed 0-width hairline strokes.
- **Verification:**
  - `./gradlew :remotecompose:lottie:compileDebugKotlin`
  - `./gradlew :remotecompose:lottie:testDebugUnitTest --no-build-cache`

---

### Task 3: Verification & Golden Roborazzi Baselines {#PL_LOTTIE_MATTE_T3}

- **Status:** Done
- **Objective:** Record new Roborazzi screenshot baselines for media animations (`volumeDown`, `volumeUp`, `unmuteToMute`, `muteToUnmute`) and run full module verification pipeline.
- **Verification Pipeline:**
  1. Record Baselines: `./gradlew :remotecompose:lottie:recordRoborazziDebug --no-build-cache` (Passed)
  2. Format Code: `./gradlew :remotecompose:lottie:ktfmtFormat` (Passed)
  3. Update Signatures: `./gradlew :remotecompose:lottie:metalavaGenerateSignatureDebug` (Passed)
  4. Compile Kotlin: `./gradlew :remotecompose:lottie:compileDebugKotlin` (Passed)
  5. Assemble Build: `./gradlew :remotecompose:lottie:assembleDebug` (Passed)
  6. Run Unit Tests: `./gradlew :remotecompose:lottie:testDebugUnitTest --no-build-cache` (Passed)
  7. Run All Checks: `./gradlew :remotecompose:lottie:check` (Passed)
  8. Verify Screenshots: `./gradlew :remotecompose:lottie:verifyRoborazziDebug --no-build-cache` (Passed)
