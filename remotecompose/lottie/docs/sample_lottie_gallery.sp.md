# Wear OS Demo Sample App Lottie Showcase — Specification  {#SP_SAMPLE_LOTTIE}

> **Code:** SP_SAMPLE_LOTTIE
> **Status:** active
> **Created:** 2026-08-25
> **Updated:** 2026-08-25
>
> **Concept:** [C_SAMPLE_LOTTIE](./sample_lottie_gallery.concept.md)
> **Depends on:** [SP_FMT](./format.sp.md), [SP_RND](./renderer.sp.md), [SP_LOTTIE_SHAPES](./shapes.sp.md), [SP_LOTTIE_LAYERS](./layers.sp.md), [SP_LOTTIE_MATTE](./track_matte_and_styles.sp.md)
> **Used by:** —
> **Plan:** [sample_lottie_gallery.plan.md](./sample_lottie_gallery.plan.md)
>
> Technical specification defining the self-contained LottieScreen data structures, embedded catalog, nested composables, and interactive controls.

## Contents

- [01. Data Structures](#SP_SAMPLE_LOTTIE_01) — Demo item models, category enums, and in-screen state.
- [02. Contracts](#SP_SAMPLE_LOTTIE_02) — `LottieScreen` and nested helper composables.
- [03. Validation Rules](#SP_SAMPLE_LOTTIE_03) — Resource and render safety invariants.
- [01. Data Structures](#SP_SAMPLE_LOTTIE_01) — Demo item models, playback regimes, and view modes.
- [02. Contracts](#SP_SAMPLE_LOTTIE_02) — `LottieScreen` and nested helper composables.
- [03. Validation Rules](#SP_SAMPLE_LOTTIE_03) — Resource and render safety invariants.
- [04. State Transitions](#SP_SAMPLE_LOTTIE_04) — Gallery list vs Detail player vs Demo mode state machine.
- [05. Verification Criteria](#SP_SAMPLE_LOTTIE_05) — Build and compile acceptance criteria.
- [06. Reversibility](#SP_SAMPLE_LOTTIE_06) — Rollback strategy.
- [07. Design Decisions](#SP_SAMPLE_LOTTIE_DEC) — Specification level design choices.

## 01. Data Structures  {#SP_SAMPLE_LOTTIE_01}

> Implements: [C_SAMPLE_LOTTIE_02](./sample_lottie_gallery.concept.md#C_SAMPLE_LOTTIE_02)

### 01_01. LottieDemoItem & PlaybackRegime  {#SP_SAMPLE_LOTTIE_01_01}

```kotlin
internal data class LottieDemoItem(
  val title: String,
  val subtitle: String,
  val category: String,
  @param:RawRes val rawRes: Int,
)

internal enum class PlaybackRegime {
  TIME,
  CROWN,
}

internal sealed interface LottieViewMode {
  data object Gallery : LottieViewMode
  data class Detail(val index: Int) : LottieViewMode
  data class Demo(val index: Int) : LottieViewMode
}
```

## 02. Contracts  {#SP_SAMPLE_LOTTIE_02}

### 02_01. LottieScreen Composable  {#SP_SAMPLE_LOTTIE_02_01}

Main entry point in `com.google.android.horologist.lottie.LottieScreen`:

```kotlin
@Composable
fun LottieScreen(modifier: Modifier = Modifier)
```

### 02_02. Nested Helper Composables  {#SP_SAMPLE_LOTTIE_02_02}

Private/internal composables inside `LottieScreen.kt`:
- `LottieGalleryList`: Displays all 26 animations grouped with category headers, live preview thumbnail chips, and a top-level "Start Demo Mode" button.
- `LottieDetailPlayer`: Full-screen player supporting immediate Next/Prev switching via `key(item.rawRes)`, Play/Pause toggle, and a regime selector between Time continuous play and Rotary Crown progress scrubbing.
- `LottieDemoModePlayer`: Full-screen hands-free presentation automatically cycling animations every 3 seconds with animated preview and exit button.
- `LottieCard`: Chip component rendering a mini live animation preview, title, and subtitle.

## 03. Validation Rules  {#SP_SAMPLE_LOTTIE_03}

- `VAL_SAMPLE_LOTTIE_01`: All animations must decode and render safely without throwing exceptions on physical hardware.
- `VAL_SAMPLE_LOTTIE_02`: In Crown Scrubbing mode, rotary crown rotation must smoothly adjust normalized progress within `0.0f..1.0f` and update frames in real-time.
- `VAL_SAMPLE_LOTTIE_03`: Pressing Next or Previous in Detail Player must immediately update the active animation without requiring a Play/Pause toggle.
- `VAL_SAMPLE_LOTTIE_04`: In Demo Mode, animations must auto-advance sequentially every ~3 seconds.

## 04. State Transitions  {#SP_SAMPLE_LOTTIE_04}

```
                  ┌──────────────────────┐
                  │   GALLERY LIST VIEW  │
                  └──────┬────────┬──────┘
             Select Item │        │ Start Demo Mode
                         ▼        ▼
        ┌─────────────────────┐  ┌─────────────────────┐
        │ DETAIL PLAYER VIEW  │  │   DEMO MODE PLAYER  │
        │ - Time Regime       │  │ - Auto-cycles every │
        │ - Crown Scrub Regime│  │   3 seconds         │
        └────────┬────────────┘  └────────┬────────────┘
                 │ Back / Dismiss         │ Back / Tap Exit
                 ▼                        ▼
        ┌──────────────────────────────────────────────┐
        │              GALLERY LIST VIEW               │
        └──────────────────────────────────────────────┘
```

## 05. Verification Criteria  {#SP_SAMPLE_LOTTIE_05}

- `SP_SAMPLE_LOTTIE_05_01`: `:sample` compiles cleanly (`./gradlew :sample:compileDebugKotlin` and `./gradlew :sample:assembleDebug`).
- `SP_SAMPLE_LOTTIE_05_02`: Code formatting passes (`./gradlew :sample:ktfmtFormat`).
- `SP_SAMPLE_LOTTIE_05_03`: All 26 animations render visibly in Gallery, Detail Player, and Demo Mode.
- `SP_SAMPLE_LOTTIE_05_04`: Crown scrolling adjusts animation frame when in Crown Scrubbing mode.
- `SP_SAMPLE_LOTTIE_05_05`: Next/Prev buttons immediately switch animations on first click.

## 06. Reversibility  {#SP_SAMPLE_LOTTIE_06}

Reverting `sample/src/main/java/com/google/android/horologist/lottie/LottieScreen.kt` and removing added raw resources returns `:sample` to its original state.

## 07. Design Decisions  {#SP_SAMPLE_LOTTIE_DEC}

- `SP_SAMPLE_LOTTIE_DEC_01`: Keying RemoteDocument rendering with `key(item.rawRes)` ensures instantaneous recomposition when changing items.
- `SP_SAMPLE_LOTTIE_DEC_02`: Support Rotary Crown scrubbing using Compose `onRotaryScrollEvent` with `FocusRequester` requesting focus upon entering Crown Scrubbing mode.
- `SP_SAMPLE_LOTTIE_DEC_03`: Top-level Demo Mode button in `LottieGalleryList` allows frictionless kiosk presentation on watches.

