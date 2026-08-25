# Specification: Comprehensive Lottie Visual Screenshot Diff Test Suite {#SP_LOTTIE_SCREENSHOT_TESTS}

> **Code:** SP_LOTTIE_SCREENSHOT_TESTS
> **Status:** active
> **Created:** 2026-08-25
> **Updated:** 2026-08-25
>
> **Depends on:** [`C_LOTTIE_SCREENSHOT_TESTS`](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/docs/screenshot_test_suite.concept.md)
> **Used by:** [`PL_LOTTIE_SCREENSHOT_TESTS`](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/docs/screenshot_test_suite.plan.md)

---

## 1. Contracts & Interfaces

### 1.1 Test Suite Organization
All screenshot test classes shall inherit from [`LottieDiffScreenshotTest`](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/test/java/com/google/android/horologist/remotecompose/lottie/LottieDiffScreenshotTest.kt) and utilize `runLottieDiffTest(json = ...)` or `runLottieDiffTest(resId = ...)` with Roborazzi frame capture blocks.

```kotlin
abstract class LottieDiffScreenshotTest {
  fun runLottieDiffTest(
    json: String,
    capture: (LottieCaptureScope.() -> Unit)? = null,
  )
}
```

### 1.2 Test Suites Breakdown
1. **`LottieStylingScreenshotTest`**:
   - `gradientLinearAndRadial`: Tests `RemoteLinearShader` and `RemoteRadialShader` rendering.
   - `strokeDashAndMiter`: Tests `RemoteStroke` with animated dash array `[10, 5, 2, 5]` and acute angle miter clipping.
   - `evenOddFillRule`: Tests `FillRule.EvenOdd` on concentric polygon holes.
   - `primitiveTrimPaths`: Tests `TrimPath` (`s`, `e`, `o`) applied across `Rectangle`, `Ellipse`, `PolyStar`, and `Path`.
2. **`LottieCompositingScreenshotTest`**:
   - `invertedAlphaTrackMatte`: Tests `tt: 2` inverted alpha track matte clipping against complex animated background shapes.
   - `nonAdjacentMatteParent`: Tests matte layer referencing a non-adjacent parent layer index (`tp`).
   - `layerMaskClipping`: Tests multi-mask clipping pipeline (`masksProperties`) with `Add`, `Subtract`, and `Intersect` modes.
3. **`LottieModifiersScreenshotTest`**:
   - `repeaterModifier`: Tests `Repeater` (`rp`) with 5 copies, cumulative transform progression, and start/end opacity decay.
   - `roundedCornersModifier`: Tests `RoundedCorners` (`rd`) with dynamic radius applied to sharp polygonal star paths.
   - `mergePathsOperations`: Tests `MergePaths` (`mm`) Boolean operations (`Union`, `Subtract`, `Intersect`, `Exclude`).
4. **`LottieAdvancedFeaturesScreenshotTest`**:
   - `nestedPrecompositions`: Tests 3-level deep sub-composition hierarchy with local layer timing scaling.
   - `timeRemapping`: Tests precomposition time remapping (`tm`) with reversed and non-linear playback curves.
   - `bitmapImageLayer`: Tests `ImageLayer` (`ty: 2`) embedded Base64 bitmap rendering with layer opacity and scaling.
   - `vectorTypographyTextLayer`: Tests `TextLayer` (`ty: 5`) vector glyph rendering with Left, Center, and Right justification and tracking.

---

## 2. Test Vectors & Data Specifications

### 2.1 Synthetic Test Vector Requirements
- All synthetic test vectors shall be valid Lottie 1.0.1 JSON strings conforming to the official schema.
- Standard composition dimensions: `100x100` or `200x200` at `30 fps`.
- Frame captures must target milestone timestamps: `frame = 0f`, `frame = 15f`, `frame = 30f` where dynamic keyframe interpolation occurs.

---

## 3. Verification Criteria & Traceability

- **`SP_LOTTIE_SCREENSHOT_TESTS_05_01` (Styling & Shaders):** Roborazzi screenshot diff test verifies linear/radial gradients, stroke dashes, miter limits, and EvenOdd fill rules with 0 visual regressions.
- **`SP_LOTTIE_SCREENSHOT_TESTS_05_02` (Compositing & Masks):** Roborazzi screenshot diff test verifies inverted track mattes, non-adjacent mattes, and layer masks.
- **`SP_LOTTIE_SCREENSHOT_TESTS_05_03` (Advanced Modifiers):** Roborazzi screenshot diff test verifies Repeaters, RoundedCorners, and MergePaths.
- **`SP_LOTTIE_SCREENSHOT_TESTS_05_04` (Compositions, Images & Typography):** Roborazzi screenshot diff test verifies nested precomps, time remapping, bitmap image layers, and vector text typography.

---

## 4. Rollback Strategy

Screenshot test additions do not alter production bytecode or runtime APIs. If any test case causes runner failures or flakiness on CI, individual test methods can be isolated or updated without affecting runtime rendering logic.
