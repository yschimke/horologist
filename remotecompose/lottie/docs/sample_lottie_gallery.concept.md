# Wear OS Demo Sample App Lottie Showcase & Device Testing Gallery  {#C_SAMPLE_LOTTIE}

> **Code:** C_SAMPLE_LOTTIE
> **Status:** active
> **Created:** 2026-08-25
> **Updated:** 2026-08-25
> **Author:** dev-flow-orchestrator
> **Owner:** horologist-sample
> **Complexity:** low
> **Criticality:** supporting
>
> **Depends on:** [C_FMT](./format.concept.md), [C_RND](./renderer.concept.md), [C_LOTTIE_SHAPES](./shapes.concept.md), [C_LOTTIE_LAYERS](./layers.concept.md), [C_LOTTIE_MATTE](./track_matte_and_styles.concept.md)
> **Used by:** —
> **Spike:** —
> **Specification:** [SP_SAMPLE_LOTTIE](./sample_lottie_gallery.sp.md)
> **Plan:** [sample_lottie_gallery.plan.md](./sample_lottie_gallery.plan.md)
>
> Defines the self-contained Lottie animation showcase and real-device testing experience implemented directly inside `LottieScreen.kt` in the `:sample` application.

## Contents

- [1. Philosophy](#C_SAMPLE_LOTTIE_01) — Purpose of real-device validation, direct screen encapsulation, and showcase scope.
- [2. Domain Model](#C_SAMPLE_LOTTIE_02) — In-screen animation entries, category grouping, and pager/scroll view models.
- [3. Mechanisms](#C_SAMPLE_LOTTIE_03) — Multi-animation presentation, live preview rendering, interactive controls, and nested composables.
- [4. Integration Points](#C_SAMPLE_LOTTIE_04) — Existing `LottieScreen` entry point and Remote Compose player.
- [5. Design Decisions](#C_SAMPLE_LOTTIE_DEC) — Architectural choices and alternatives considered.

## 1. Philosophy  {#C_SAMPLE_LOTTIE_01}

### 1.1. Core Principle  {#C_SAMPLE_LOTTIE_01_01}

The `:sample` app's `LottieScreen` serves as an on-device visual test bench and capability showcase for Remote Compose Lottie rendering on physical Wear OS watches. To make validation fast, intuitive, and frictionless on real hardware, all animation showcase capabilities are contained directly within `LottieScreen.kt` without requiring external test suites or multi-screen routing changes.

### 1.2. Design Constraints  {#C_SAMPLE_LOTTIE_01_02}

- **Self-Contained Encapsulation:** All UI changes, nested composables, state management, and animation catalog definitions are self-contained inside `LottieScreen.kt`.
- **Maximum Animation Coverage:** Expose as many test and feature animations as possible (25+ animations covering Media controls, Parametric shapes, Gradients, Dashes, Fill rules, Trim paths, Inverted track mattes, Layer masks, Repeaters, Rounded corners, Merge paths, Precomps, Time remapping, Image layers, and Typography).
- **No Additional Test Overhead:** No unit or screenshot test suites are required for this demo feature; verification is conducted directly on build and device execution.
- **Wear OS Native Navigation:** Support smooth rotary scrolling or paging with clear title banners, animation progress, and play/pause controls.

## 2. Domain Model  {#C_SAMPLE_LOTTIE_02}

### 2.1. Key Entities  {#C_SAMPLE_LOTTIE_02_01}

- `LottieDemoItem`: Lightweight in-screen model holding an animation title, category, and source (raw resource ID or JSON string).
- `LottieScreenState`: UI state managing the currently selected/active animation index, playback state (playing vs paused), and display mode (Scrollable List vs Fullscreen Pager).

### 2.2. Data Flows  {#C_SAMPLE_LOTTIE_02_02}

```
SampleWearApp
    │
    ▼ (Screen.Lottie.route)
LottieScreen (Self-Contained)
    ├── Multi-Animation Showcase (ScalingLazyColumn / Pager)
    ├── Live RemoteCompose Previews (LottieAnimatedPreview / LottiePreview)
    └── Quick Playback & Navigation Controls (Next, Prev, Play/Pause, Info)
```

## 3. Mechanisms  {#C_SAMPLE_LOTTIE_03}

### 3.1. Showcase Presentation Modes  {#C_SAMPLE_LOTTIE_03_01}

`LottieScreen` supports rich display modes:
1. **Scrollable Gallery View:** A `ScalingLazyColumn` displaying all 26 animations grouped by category with live animated thumbnail previews, title badges, and a prominent top-level **Demo Mode** trigger button.
2. **Interactive Detail Player:** Full-screen animation playback with instant Next/Previous navigation, Play/Pause control, and dual playback regimes.
3. **Auto-Cycling Demo Mode (Kiosk Mode):** Automated full-screen playback that advances through all animations every couple of seconds for hands-free device inspection.

### 3.2. Playback Regimes (Time vs Crown Scrubbing)  {#C_SAMPLE_LOTTIE_03_02}

The detail player provides two distinct animation driving regimes:
1. **Time-Based Mode (⏱ Time):** Continuous looping playback driven by Compose animation transitions / system clock. Includes play/pause toggle.
2. **Crown-Based Mode (👑 Crown Scrubbing):** Physical rotary crown rotation intercepts scroll events to manually drive the normalized progress `0.0f..1.0f`, rendering frame-by-frame on `LottiePreview(progress = progress)`.

### 3.3. Next / Previous Recomposition & RemoteDocument Keying  {#C_SAMPLE_LOTTIE_03_03}

Because `rememberRemoteDocument` compiles and caches the binary Remote Compose document across recompositions, switching the active animation item must be explicitly keyed (`key(item.rawRes)`) to trigger immediate document recompilation and reset playback state upon pressing Next or Previous.

### 3.4. Complete Animation Coverage  {#C_SAMPLE_LOTTIE_03_04}

The embedded catalog contains 26 animations across 4 categories:
- **Media Icons (9):** `geometry`, `m3_play_pause`, `m3_next`, `play_pause`, `next`, `volume_up`, `volume_down`, `mute_to_unmute`, `unmute_to_mute`.
- **Parametric Shapes & Hierarchies (7):** `polystar`, `rect_ellipse`, `position_animated`, `position_static`, `parent_chain`, `grandparent`, `transform_skew`.
- **Gradients, Strokes & Fills (5):** `gradient_linear_fill`, `gradient_radial_fill`, `gradient_stroke`, `stroke_dash_pattern`, `fill_rule_even_odd`.
- **Modifiers & Paths (5):** `trim_path_primitives`, `repeater_linear_copies`, `repeater_radial_distribution`, `rounded_corners_star`, `merge_paths_overlapping_circles`.

## 4. Integration Points  {#C_SAMPLE_LOTTIE_04}

- **Entry Point:** Existing `LottieScreen(modifier: Modifier = Modifier)` in `com.google.android.horologist.lottie.LottieScreen`.
- **Renderer:** `com.google.android.horologist.remotecompose.lottie.LottieAnimatedPreview` and `LottiePreview`.
- **Rotary Input:** `Modifier.onRotaryScrollEvent` with `FocusRequester` for crown-driven progress scrubbing.

## 5. Design Decisions & Alternatives Considered  {#C_SAMPLE_LOTTIE_DEC}

- `C_SAMPLE_LOTTIE_DEC_01` (Single-File Architecture): Implement all showcase logic, nested composables, and catalog data directly inside `LottieScreen.kt`.
  - *Pros:* Zero impact on `SampleWearApp` navigation, zero changes to other files, high cohesion.
  - *Resolution:* Adopted per user directive.
- `C_SAMPLE_LOTTIE_DEC_02` (Dual Playback Regimes): Support both Time-based continuous play and Rotary Crown manual progress scrubbing.
  - *Pros:* Allows inspecting complex multi-layer animations frame-by-frame on physical hardware.
  - *Resolution:* Adopted.
- `C_SAMPLE_LOTTIE_DEC_03` (Kiosk Demo Mode): Auto-cycling showcase cycling every 3 seconds.
  - *Pros:* Perfect for hands-free demonstrations and rapid visual validation.
  - *Resolution:* Adopted.
- `C_SAMPLE_LOTTIE_DEC_04` (Explicit Compose Keying): Key the animation viewport with `key(item.rawRes)`.
  - *Pros:* Fixes RemoteDocument caching stale document across Next/Prev navigation.
  - *Resolution:* Adopted.

