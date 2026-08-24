# Implementation Plan: Group Geometry Transformations & Container Style Parity {#PL_LOTTIE_GROUP_TRANSFORM}

> **Code:** PL_LOTTIE_GROUP_TRANSFORM
> **Status:** in-progress
> **Created:** 2026-08-23
> **Updated:** 2026-08-23
>
> **Concept:** [Track Matte & Styles Concept](track_matte_and_styles.concept.md), [Shapes Concept](shapes.concept.md)
> **Specification:** [Track Matte & Styles Spec](track_matte_and_styles.sp.md), [Shapes Spec](shapes.sp.md)
> **Depends on:** `SP_LOTTIE_MATTE`, `SP_LOTTIE_SHAPES`
> **Used by:** `renderer/shapes`, `renderer/Shape.kt`, `MediaLottieDiffScreenshotTest`
>
> Implementation plan for resolving the sound wave circle sizing and stroke scaling discrepancy in `mute_to_unmute` and related animations by transforming shape geometry into container space for sibling styles.

## Problem Analysis

In Lottie animations (such as `mute_to_unmute.json`, `unmute_to_mute.json`, `volume_up.json`, `volume_down.json`), shape groups often contain geometry elements (e.g. `Ellipse Path`) paired with a local `Transform` (scaling $58\% \rightarrow 100\%$ or $0\% \rightarrow 58\%$), while the `Stroke` or `Fill` style is declared as an external sibling at the layer/container level.

- **Reference `lottie-android` behavior:** `ContentGroup.getPath()` bakes the group transform directly into the `Path` geometry, and sibling styles draw the transformed path in the layer's coordinate system. Stroke width remains constant in layer space (e.g. $28\text{px}$).
- **`rc/lottie` previous behavior:** `gatherShapes` wrapped unstyled groups and sibling styles in a `RemoteGroup`, applying the group transform to the `RemoteCanvas` at draw time. Canvas scaling scaled down `paint.strokeWidth` (e.g. $28 \times 0.58 = 16.24\text{px}$), causing inner sound wave rings to appear thinner and smaller in bounding radius.

## Tasks & Phases

### Task 1: Non-Functional Refactoring: Geometry Transformation Utilities {#PL_LOTTIE_GROUP_TRANSFORM_T1}
- **Objective:** Create `renderer/shapes/GeometryTransform.kt` containing pure mathematical functions to transform `RemoteBezierValue`, `RemoteLottiePath`, and `RemoteShape` instances by a Lottie `Transform` (anchor point, scale, skew, rotation, translation).
- **Files to Create:**
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/shapes/GeometryTransform.kt`
- **Commit:** `Extract shape and bezier geometry transformation engine` (Commit A - Non-functional)

### Task 2: Functional Feature: Transform Sibling Group Geometries in Layer Space {#PL_LOTTIE_GROUP_TRANSFORM_T2}
- **Objective:** Update `renderer/Shape.kt` (`gatherShapes` / `evaluateGroupGeometries`) to transform unstyled group geometries using `GeometryTransform` and apply sibling styles at container level without canvas-level scaling.
- **Files to Modify:**
  - `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/Shape.kt`
- **Commit:** `Apply group transformations to path geometry for container-level styles` (Commit B - Functional)

### Task 3: Verification & Baseline Update {#PL_LOTTIE_GROUP_TRANSFORM_T3}
- **Objective:** Run the verification pipeline and record updated Roborazzi screenshot baselines achieving 100% pixel parity against `lottie-android`.
- **Verification Commands:**
  1. `./gradlew :remotecompose:lottie:recordRoborazziDebug --no-build-cache`
  2. `./gradlew :remotecompose:lottie:verifyRoborazziDebug --no-build-cache`
  3. `./gradlew :remotecompose:lottie:ktfmtFormat`
  4. `./gradlew :remotecompose:lottie:compileDebugKotlin`
  5. `./gradlew :remotecompose:lottie:testDebugUnitTest --no-build-cache`
  6. `./gradlew :remotecompose:lottie:check`
- **Commit:** `Update Roborazzi screenshot baselines for sound wave geometry parity` (Commit C - Verification)
