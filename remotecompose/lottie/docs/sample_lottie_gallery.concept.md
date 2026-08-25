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

`LottieScreen` supports rich display of all animations:
1. **Scrollable Gallery View:** A `ScalingLazyColumn` displaying all 25+ animations as cards with live animated previews, title badges, and category headers.
2. **Interactive Detail View / Pager:** Swiping or tapping an animation allows full-screen playback with Play/Pause, Restart, and Prev/Next stepping.

### 3.2. Complete Animation Coverage  {#C_SAMPLE_LOTTIE_03_02}

The embedded catalog contains:
- **Media Icons:** `geometry`, `m3_play_pause`, `m3_next`, `play_pause`, `next`, `volume_up`, `volume_down`, `mute_to_unmute`, `unmute_to_mute`.
- **Parametric Shapes & Hierarchies:** `polystar`, `rect_ellipse`, `position_animated`, `position_static`, `parent_chain`, `grandparent`, `transform_skew`.
- **Gradients, Strokes & Fills:** `gradient_linear`, `gradient_radial`, `gradient_stroke`, `stroke_dash_miter`, `fill_rule_evenodd`.
- **Modifiers, Paths & Masks:** `trim_paths`, `layer_masks`, `repeater_modifiers`, `rounded_corners`, `merge_paths`.
- **Compositions & Advanced:** `nested_precompositions`, `time_remapping`, `image_layer_base64`, `text_layer_glyphs`.

## 4. Integration Points  {#C_SAMPLE_LOTTIE_04}

- **Entry Point:** Existing `LottieScreen(modifier: Modifier = Modifier)` in `com.google.android.horologist.lottie.LottieScreen`.
- **Renderer:** `com.google.android.horologist.remotecompose.lottie.LottieAnimatedPreview` and `LottiePreview`.

## 5. Design Decisions & Alternatives Considered  {#C_SAMPLE_LOTTIE_DEC}

- `C_SAMPLE_LOTTIE_DEC_01` (Single-File Architecture): Implement all showcase logic, nested composables, and catalog data directly inside `LottieScreen.kt`.
  - *Pros:* Zero impact on `SampleWearApp` navigation, zero changes to other files, high cohesion.
  - *Cons:* Larger `LottieScreen.kt` file.
  - *Resolution:* Adopted per user directive.
- `C_SAMPLE_LOTTIE_DEC_02` (Asset Placement): Copy raw JSON files to `sample/src/main/res/raw/` with programmatic JSON definitions embedded for feature-specific cases.
  - *Pros:* Standard Android resource resolution, offline capability, self-contained.
  - *Resolution:* Adopted.
- **Alternatives Considered:**
  - *Multi-Screen Navigation Hierarchy:* Creating separate `LottieMenuScreen` and `LottiePlayerScreen` registered in `Screen.kt` and `SampleWearApp.kt`. *Rejected:* Unnecessary routing complexity when `LottieScreen` can encapsulate the entire interactive experience cleanly.
  - *Automated Unit/Screenshot Tests:* Authoring Roborazzi tests for the sample app. *Rejected:* Unnecessary for internal demo screens per user directive.
