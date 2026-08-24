# Specification: Lottie Screenshot Diff Testing Framework {#SP_LOTTIE_DIFF}

> **Code:** SP_LOTTIE_DIFF
> **Status:** active
> **Created:** 2026-08-23
> **Updated:** 2026-08-23
>
> **Concept:** [Lottie Screenshot Diff Concept](screenshot_diff.concept.md)
> **Specification Reference:** [TESTING_DOC.md](../src/main/java/com/google/android/horologist/remotecompose/lottie/docs/TESTING_DOC.md)
> **Depends on:** `C_LOTTIE_DIFF`, `SP_RND`, `SP_LOTTIE_SHAPES`
> **Used by:** `PL_LOTTIE_DIFF`, `LottieDiffScreenshotTest`, `MediaLottieDiffScreenshotTest`, `LottieFeatureDiffScreenshotTest`
>
> Detailed technical specification for the screenshot diff test harness, UI layout structure, and test case keyframe capture definitions across `:remotecompose:lottie`.

## 01. Test Harness Architecture & State {#SP_LOTTIE_DIFF_01}

### 01.1 State Propagation Contracts {#SP_LOTTIE_DIFF_01_01}

In `LottieDiffScreenshotTest.kt`, the root Composable dynamically binds to `progressState: MutableState<Float>`.

```kotlin
val progressState = mutableStateOf(progress)
val clockNanoTimeState = mutableStateOf(0L)
val clock = SettableRemoteClock()
```

When `LottieDiffTestScope.captureProgress(progress)` or `LottieDiffTestScope.captureFrame(frame)` executes:
1. `progressState.value` updates with the normalized progress float $[0.0, 1.0]$.
2. `clockNanoTimeState.value` synchronizes the `SettableRemoteClock`.
3. `composeRule.waitForIdle()` flushes pending composition passes.
4. The Roborazzi capture runs against test node tag `"LottieDiff"`.

---

## 02. UI Layout & Styling Specification {#SP_LOTTIE_DIFF_02}

### 02.1 Root Preview Container & Comparison Row {#SP_LOTTIE_DIFF_02_01}
- **Component:** `Row`
- **Modifier:** `Modifier.background(Color(0xFF1E1E1E)).padding(8.dp).testTag("LottieDiff")`
- **Arrangement:** `horizontalArrangement = Arrangement.spacedBy(8.dp)`
- **Alignment:** `verticalAlignment = Alignment.CenterVertically`
- **Children:**
  - `LottieAndroidPreview`: Reference Airbnb `lottie-android` preview container with label "lottie-android".
  - `LottieRcPreview`: Target RemoteCompose `rc/lottie` preview container with label "rc/lottie".

---

## 03. Test Case Keyframe Matrix {#SP_LOTTIE_DIFF_03}

### 03.1 `MediaLottieDiffScreenshotTest` Suite {#SP_LOTTIE_DIFF_03_01}

| Test Method | Raw Resource | Animation Characteristic | Keyframe Milestones | Generated Screenshot Suffixes |
|---|---|---|---|---|
| `geometry` | `R.raw.geometry` | Bézier geometry path morphing | `0.0f`, `0.5f`, `1.0f` | `_geometry_progress0.png`, `_geometry_progress50.png`, `_geometry_progress100.png` |
| `playPause` | `R.raw.play_pause` | Pause bars to play triangle morph | `0.0f`, `0.5f`, `1.0f` | `_playPause_progress0.png`, `_playPause_progress50.png`, `_playPause_progress100.png` |
| `next` | `R.raw.next` | Skip next track shape transition | `0.0f`, `0.5f`, `1.0f` | `_next_progress0.png`, `_next_progress50.png`, `_next_progress100.png` |
| `m3PlayPause` | `R.raw.m3_play_pause` | Material 3 multi-path grouped morph | `0.0f`, `0.5f`, `1.0f` | `_m3PlayPause_progress0.png`, `_m3PlayPause_progress50.png`, `_m3PlayPause_progress100.png` |
| `m3Next` | `R.raw.m3_next` | Material 3 skip next track transition | `0.0f`, `0.5f`, `1.0f` | `_m3Next_progress0.png`, `_m3Next_progress50.png`, `_m3Next_progress100.png` |
| `volumeUp` | `R.raw.volume_up` | Sound wave expansion timeline | `0.0f`, `0.5f`, `1.0f` | `_volumeUp_progress0.png`, `_volumeUp_progress50.png`, `_volumeUp_progress100.png` |
| `volumeDown` | `R.raw.volume_down` | Sound wave collapse timeline | `0.0f`, `0.5f`, `1.0f` | `_volumeDown_progress0.png`, `_volumeDown_progress50.png`, `_volumeDown_progress100.png` |
| `muteToUnmute` | `R.raw.mute_to_unmute` | Animated slash stroke trimming | `0.0f`, `0.5f`, `1.0f` | `_muteToUnmute_progress0.png`, `_muteToUnmute_progress50.png`, `_muteToUnmute_progress100.png` |
| `unmuteToMute` | `R.raw.unmute_to_mute` | Animated slash stroke emergence | `0.0f`, `0.5f`, `1.0f` | `_unmuteToMute_progress0.png`, `_unmuteToMute_progress50.png`, `_unmuteToMute_progress100.png` |

### 03.2 `LottieFeatureDiffScreenshotTest` Suite {#SP_LOTTIE_DIFF_03_02}

| Test Method | Resource / Source | Animation Characteristic | Keyframe Milestones | Generated Screenshot Suffixes |
|---|---|---|---|---|
| `positionStatic` | `R.raw.position_static` | Static position vector | Default (`0.0f`) | `_positionStatic.png` |
| `positionAnimated` | `R.raw.position_animated` | 4-keyframe corner translation | Frames `0f`, `20f`, `40f`, `60f` | `_positionAnimated_frame0.png`, `_positionAnimated_frame20.png`, `_positionAnimated_frame40.png`, `_positionAnimated_frame60.png` |
| `rectEllipse` | `R.raw.rect_ellipse` | Static parametric shapes | Default (`0.0f`) | `_rectEllipse.png` |
| `polystar` | `R.raw.polystar` | Static parametric polystars | Default (`0.0f`) | `_polystar.png` |
| `parentChain` | `R.raw.parent_chain` | Deep ancestor parenting hierarchy | Default (`0.0f`) | `_parentChain.png` |
| `transformSkew` | Inline JSON | Static 2D skew matrix | Default (`0.0f`) | `_transformSkew.png` |

### 03.3 `LottieScalingDiffScreenshotTest` Suite {#SP_LOTTIE_DIFF_03_03}

Tests 16 combinations of Lottie dimensions ($64\times64$, $64\times96$, $128\times128$, $192\times128$) and Box sizes ($64\times64$, $64\times96$, $128\times128$, $192\times128$). Header displays explicit dimension mapping (`Lottie: WxHpx | Box: WxHdp`).

---

## 04. Baseline File Naming & Cleanup Rules {#SP_LOTTIE_DIFF_04}

1. **Suffix Mapping:**
   - Multi-progress captures use suffix `_progress${(progress * 100).toInt()}` (e.g., `_progress0`, `_progress50`, `_progress100`).
   - Frame captures use suffix `_frame${frame.toInt()}` (e.g., `_frame0`, `_frame20`).
2. **Obsolete File Removal:**
   - Legacy single-frame PNGs that are superseded by multi-progress snapshots (such as `MediaLottieDiffScreenshotTest_geometry.png` or `MediaLottieDiffScreenshotTest_playPause.png`) must be deleted from `remotecompose/lottie/src/test/screenshots/`.

---

## 05. Verification Criteria & Gate {#SP_LOTTIE_DIFF_05}

- **[SP_LOTTIE_DIFF_05_01]** Formatting passes `./gradlew :remotecompose:lottie:ktfmtFormat`.
- **[SP_LOTTIE_DIFF_05_02]** Signature files pass `./gradlew :remotecompose:lottie:metalavaGenerateSignatureDebug`.
- **[SP_LOTTIE_DIFF_05_03]** Kotlin compiles without errors `./gradlew :remotecompose:lottie:compileDebugKotlin`.
- **[SP_LOTTIE_DIFF_05_04]** Unit tests pass `./gradlew :remotecompose:lottie:testDebugUnitTest --no-build-cache`.
- **[SP_LOTTIE_DIFF_05_05]** Module check passes `./gradlew :remotecompose:lottie:check`.
- **[SP_LOTTIE_DIFF_05_06]** Roborazzi verification passes `./gradlew :remotecompose:lottie:verifyRoborazziDebug`.
