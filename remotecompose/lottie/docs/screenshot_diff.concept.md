# Concept: Lottie Screenshot Diff Testing Framework {#C_LOTTIE_DIFF}

> **Code:** C_LOTTIE_DIFF
> **Status:** active
> **Created:** 2026-08-23
> **Updated:** 2026-08-23
>
> **Depends on:** `C_RND`, `C_LOTTIE_SHAPES`, `C_LOTTIE_LAYERS`
> **Used by:** `SP_LOTTIE_DIFF`, `PL_LOTTIE_DIFF`
>
> Architectural concept for the RemoteCompose Lottie screenshot diff testing framework and multi-frame animation milestone verification against reference lottie-android outputs.

## 01. Context & Motivation {#C_LOTTIE_DIFF_01}

The `remotecompose/lottie` library provides a high-performance RemoteCompose-based renderer for Lottie 1.0.1 vector animations. To prevent regressions and ensure visual fidelity across vector geometry, styling, transforms, and layer hierarchies, screenshot diff tests compare RemoteCompose Lottie rendering side-by-side with the reference `lottie-android` (Airbnb) engine.

Previously, several animated media test cases in `MediaLottieDiffScreenshotTest` captured only single-frame baselines (0% progress), leaving mid-point morphs, track skips, sound wave transitions, and path trimming unverified throughout the animation lifecycle.

---

## 02. Architecture & Design Principles {#C_LOTTIE_DIFF_02}

### 02.1 Multi-Progress Animation Verification {#C_LOTTIE_DIFF_02_01}
Animated Lottie compositions undergo multi-frame milestone validation. For continuous animations (e.g., play/pause morphs, track skip transitions, volume sound wave expansions, audio mute slash trims, geometry interpolations), milestones are captured at start (0%), mid-point (50%), and completion (100%). For frame-specific step sequences (e.g., `position_animated`), milestones are captured at explicit keyframe intervals (frames 0, 20, 40, 60).

### 02.2 Static vs. Animated Distinction {#C_LOTTIE_DIFF_02_02}
Compositions containing only static properties (e.g., `position_static`, `rect_ellipse`, `polystar`, `parent_chain`, `transformSkew`) execute a single baseline capture at progress 0%. Compositions with dynamic time-varying properties execute multi-progress captures across their animation timeline.

---

## 03. Target Test Suites & Scope {#C_LOTTIE_DIFF_03}

1. **`LottieDiffScreenshotTest.kt` (Base Harness):** Provides unified `runLottieDiffTest` composables with side-by-side previews and `LottieDiffTestScope` multi-frame capture triggers.
2. **`MediaLottieDiffScreenshotTest.kt`:** Covers media, audio, and morphing vector icons (`playPause`, `m3PlayPause`, `next`, `m3Next`, `volumeUp`, `volumeDown`, `muteToUnmute`, `unmuteToMute`, `geometry`).
3. **`LottieFeatureDiffScreenshotTest.kt`:** Covers Lottie 1.0.1 feature specifications (`positionStatic`, `positionAnimated`, `rectEllipse`, `polystar`, `parentChain`, `transformSkew`).
4. **`LottieScalingDiffScreenshotTest.kt`:** Covers aspect ratio and container box scaling across 16 dimension combinations.

---

## 04. Alternatives Considered {#C_LOTTIE_DIFF_04}

### Progress Percentage Header in Previews
- *Option:* Render an explicit progress percentage header (e.g. `Progress: 50%`) above side-by-side comparison previews.
- *Trade-off:* Adds unnecessary visual chrome to preview containers, changes root layout height, and adds visual overhead without providing additional value over descriptive file suffixes.
- *Decision:* Rely on filename suffixes (`_progress0`, `_progress50`, `_progress100`, `_frame20`) and keep the diff preview container minimal.

### Global Video/GIF Diff Recording
- *Option:* Record animated GIF or video files for each test case.
- *Trade-off:* Large artifact size, non-deterministic video encoding diffs, and slow CI execution.
- *Decision:* Capture deterministic multi-frame PNG milestones (0%, 50%, 100%) via Roborazzi.
