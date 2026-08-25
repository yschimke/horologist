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
- [04. State Transitions](#SP_SAMPLE_LOTTIE_04) — Gallery list vs detail player view state machine.
- [05. Verification Criteria](#SP_SAMPLE_LOTTIE_05) — Build and compile acceptance criteria.
- [06. Reversibility](#SP_SAMPLE_LOTTIE_06) — Rollback strategy.
- [07. Design Decisions](#SP_SAMPLE_LOTTIE_DEC) — Specification level design choices.

## 01. Data Structures  {#SP_SAMPLE_LOTTIE_01}

> Implements: [C_SAMPLE_LOTTIE_02](./sample_lottie_gallery.concept.md#C_SAMPLE_LOTTIE_02)

### 01_01. LottieDemoItem  {#SP_SAMPLE_LOTTIE_01_01}

```kotlin
internal data class LottieDemoItem(
  val title: String,
  val category: String,
  val rawRes: Int? = null,
  val json: String? = null,
  val description: String? = null,
)
```

Invariants:
- Exactly one of `rawRes` or `json` must be non-null.
- `title` must be unique and non-empty.

## 02. Contracts  {#SP_SAMPLE_LOTTIE_02}

### 02_01. LottieScreen Composable  {#SP_SAMPLE_LOTTIE_02_01}

Main entry point in `com.google.android.horologist.lottie.LottieScreen`:

```kotlin
@Composable
fun LottieScreen(modifier: Modifier = Modifier)
```

### 02_02. Nested Helper Composables  {#SP_SAMPLE_LOTTIE_02_02}

Private/internal composables inside `LottieScreen.kt`:
- `LottieGalleryList`: Displays all animations grouped with category headers and interactive cards using `ScalingLazyColumn`.
- `LottieDetailPlayer`: Full-screen player for a selected animation with Play/Pause toggle, Prev/Next navigation, and animated preview.
- `LottieCard`: Compact card component rendering a mini live animation preview, title, and category badge.

## 03. Validation Rules  {#SP_SAMPLE_LOTTIE_03}

- `VAL_SAMPLE_LOTTIE_01`: All animations must decode and render safely without throwing exceptions on physical hardware.
- `VAL_SAMPLE_LOTTIE_02`: All interactive touch elements on watch screens must be easily tappable (minimum 48dp height/width).

## 04. State Transitions  {#SP_SAMPLE_LOTTIE_04}

```
                  ┌────────────────────┐
                  │  GALLERY LIST VIEW │
                  └─────────┬──────────┘
                            │ Select Item
                            ▼
                  ┌────────────────────┐
                  │ DETAIL PLAYER VIEW ├────────┐
                  └─────────┬──────────┘        │
                            │ Back / Dismiss    │ Next / Prev
                            ▼                   ▼
                  ┌────────────────────┐ ┌──────────────┐
                  │  GALLERY LIST VIEW │ │ SWITCH ITEM  │
                  └────────────────────┘ └──────────────┘
```

## 05. Verification Criteria  {#SP_SAMPLE_LOTTIE_05}

- `SP_SAMPLE_LOTTIE_05_01`: `:sample` compiles cleanly (`./gradlew :sample:compileDebugKotlin` and `./gradlew :sample:assembleDebug`).
- `SP_SAMPLE_LOTTIE_05_02`: Code formatting passes (`./gradlew :sample:ktfmtFormat`).
- `SP_SAMPLE_LOTTIE_05_03`: All 25+ animations render visibly on screen and can be browsed and played.

## 06. Reversibility  {#SP_SAMPLE_LOTTIE_06}

Reverting `sample/src/main/java/com/google/android/horologist/lottie/LottieScreen.kt` and removing added raw resources returns `:sample` to its original single-animation state.

## 07. Design Decisions  {#SP_SAMPLE_LOTTIE_DEC}

- `SP_SAMPLE_LOTTIE_DEC_01`: Implement dual-mode view (Gallery list + Detail player) within `LottieScreen` using simple `var selectedIndex by remember { mutableStateOf<Int?>(null) }`. *Rationale:* Delivers maximum utility with zero external navigation boilerplate.
