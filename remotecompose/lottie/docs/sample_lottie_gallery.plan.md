# Implementation Plan: Self-Contained LottieScreen Animation Showcase & Real-Device Testing Gallery  {#PL_SAMPLE_LOTTIE}

> **Code:** PL_SAMPLE_LOTTIE
> **Status:** in-progress
> **Created:** 2026-08-25
> **Updated:** 2026-08-25
>
> **Concept:** [C_SAMPLE_LOTTIE](./sample_lottie_gallery.concept.md)
> **Specification:** [SP_SAMPLE_LOTTIE](./sample_lottie_gallery.sp.md)
> **Depends on:** [PL_LOTTIE_SPEC_PARITY](./lottie_spec_parity.plan.md), [PL_LOTTIE_SCREENSHOT_TESTS](./screenshot_test_suite.plan.md)
> **Used by:** —
>
> Implementation roadmap to package all 25+ test animations directly inside `LottieScreen.kt` in `:sample` for physical testing and inspection on real Wear OS devices.

## Goal

Expand `LottieScreen.kt` in `:sample` into a self-contained, interactive multi-animation showcase displaying 25+ animations across all supported Lottie capabilities, complete with gallery list and detail player modes.

## Technology Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Scope / Architecture | Single-File in `LottieScreen.kt` | Encapsulates all showcase UI, nested composables, and catalog definitions in one place without touching sample app routing |
| Layout | `ScalingLazyColumn` + `Card` / `Chip` | Follows Horologist Wear Compose best practices with smooth rotary scrolling and circular padding |
| Viewports | `LottieAnimatedPreview` / `LottiePreview` | Leverages Remote Compose player with automatic looping and frame bindings |
| Asset Packaging | `sample/src/main/res/raw/` + Programmatic JSON strings | Standard Android raw resource resolution with inline feature definitions |

## Required Knowledge

| Kind | Ref | Applies to | Note |
|---|---|---|---|
| rule | `style.md` | All Phases | Kotlin coding and formatting standards |

## Progress

- [x] [Phase 1 — Raw Animation Asset Consolidation](#PL_SAMPLE_LOTTIE_P1)
- [x] [Phase 2 — Self-Contained LottieScreen Showcase Implementation](#PL_SAMPLE_LOTTIE_P2)
- [x] [Phase 3 — Code Formatting & Build Verification](#PL_SAMPLE_LOTTIE_P3)

## Phases

### Phase 1 — Raw Animation Asset Consolidation (`sample/src/main/res/raw/`) [DONE]  {#PL_SAMPLE_LOTTIE_P1}

**Depends on:** none
**Implements:** [SP_SAMPLE_LOTTIE_01](./sample_lottie_gallery.sp.md#SP_SAMPLE_LOTTIE_01)
**Verify:** All raw resource files present in `sample/src/main/res/raw/`

Tasks:
- [x] [Task 1.1 (`PL_SAMPLE_LOTTIE_T1_1`)]: Copy all 14 additional raw JSON animation files from `remotecompose/lottie/src/debug/res/raw/` to `sample/src/main/res/raw/` (`grandparent.json`, `m3_next.json`, `m3_play_pause.json`, `mute_to_unmute.json`, `next.json`, `parent_chain.json`, `play_pause.json`, `polystar.json`, `position_animated.json`, `position_static.json`, `rect_ellipse.json`, `unmute_to_mute.json`, `volume_down.json`, `volume_up.json`).

### Phase 2 — Self-Contained LottieScreen Showcase Implementation (`sample/.../lottie/LottieScreen.kt`) [DONE]  {#PL_SAMPLE_LOTTIE_P2}

**Depends on:** Phase 1
**Implements:** [SP_SAMPLE_LOTTIE_02](./sample_lottie_gallery.sp.md#SP_SAMPLE_LOTTIE_02)
**Verify:** [SP_SAMPLE_LOTTIE_05_03](./sample_lottie_gallery.sp.md#SP_SAMPLE_LOTTIE_05_03) — All 25+ animations accessible in gallery and detail player

Tasks:
- [x] [Task 2.1 (`PL_SAMPLE_LOTTIE_T2_1`)]: Author embedded `LottieDemoItem` catalog in `LottieScreen.kt` covering all 25+ animations (Media, Parametric Shapes, Gradients, Dashes/Miters, EvenOdd, TrimPaths, Inverted Alpha, Layer Masks, Repeaters, RoundedCorners, MergePaths, Precomps, Time Remapping, ImageLayers, Typography TextLayers, Skew).
- [x] [Task 2.2 (`PL_SAMPLE_LOTTIE_T2_2`)]: Implement `LottieGalleryList` and `LottieCard` nested composables inside `LottieScreen.kt` using `ScalingLazyColumn` with live animated previews, title badges, and category section headers.
- [x] [Task 2.3 (`PL_SAMPLE_LOTTIE_T2_3`)]: Implement `LottieDetailPlayer` nested composable inside `LottieScreen.kt` with full-screen playback, Play/Pause toggle, Next/Prev navigation, speed toggle, and metadata display.
- [x] [Task 2.4 (`PL_SAMPLE_LOTTIE_T2_4`)]: Connect dual-mode view in root `LottieScreen` composable with back-handling.

### Phase 3 — Code Formatting & Build Verification (`:sample`) [DONE]  {#PL_SAMPLE_LOTTIE_P3}

**Depends on:** Phase 2
**Implements:** [SP_SAMPLE_LOTTIE_05_01](./sample_lottie_gallery.sp.md#SP_SAMPLE_LOTTIE_05_01), [SP_SAMPLE_LOTTIE_05_02](./sample_lottie_gallery.sp.md#SP_SAMPLE_LOTTIE_05_02)
**Verify:** Full build passes cleanly with zero warnings or errors

Tasks:
- [x] [Task 3.1 (`PL_SAMPLE_LOTTIE_T3_1`)]: Format Kotlin source code (`./gradlew :sample:ktfmtFormat`).
- [x] [Task 3.2 (`PL_SAMPLE_LOTTIE_T3_2`)]: Compile Kotlin and assemble application (`./gradlew :sample:compileDebugKotlin`, `./gradlew :sample:assembleDebug`).
- [x] [Task 3.3 (`PL_SAMPLE_LOTTIE_T3_3`)]: Run all sample module checks (`./gradlew :sample:check`).

