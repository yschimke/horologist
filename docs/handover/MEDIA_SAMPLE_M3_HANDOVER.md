# media/sample → Material 3: handover notes

PR: https://github.com/yschimke/horologist/pull/5 · branch `media-sample-m3`
Preview images: orphan branch `preview_pr/5` on `yschimke/horologist`, served via `https://raw.githubusercontent.com/yschimke/horologist/preview_pr/5/pr5/<screen>/<file>.png`

## What's done (commits `1ed367696..ef13dfb58`)

| # | Commit | Screen / file | Preview comment |
|---|---|---|---|
| 1 | `1ed367696` | `UampSettingsScreen` stateful/stateless split + previews | — |
| 2 | `3c25dcea5` | `UampSettingsScreen` → M3 | https://github.com/yschimke/horologist/pull/5#issuecomment-4315943371 |
| 3 | `6b453a3a3` | `UampSettingsScreen` cleanup (TimeText, `minimumVerticalContentPadding`, M3-only imports) | (same comment, edited) |
| 4 | `b70a78e45` | `DeveloperOptionsScreen` → M3 + `@ScrollingPreview(LONG)` | https://github.com/yschimke/horologist/pull/5#issuecomment-4316483210 |
| 5 | `37d8aa896` | `SamplesScreen` → M3 + `MediaScreenScaffold` helper (suppresses `ScrollIndicator` while `LocalScrollCaptureInProgress.current` is true) | https://github.com/yschimke/horologist/pull/5#issuecomment-4316607663 |
| 6 | `9af9a0240` | `AudioDebugScreen` → M3, **delete `LegacySettingHelpers.kt`** | https://github.com/yschimke/horologist/pull/5#issuecomment-4316636947 |
| 7 | `9ef6648a9` | `MediaInfoTimeText` → M3 (M3 `TimeText` slot, `timeTextCurvedText` / `timeTextSeparator`) | https://github.com/yschimke/horologist/pull/5#issuecomment-4316656703 |
| 8 | `74bc8c2c8` | `GoogleSignInPromptScreen` + `GoogleSignOutScreen` + `UampSignInPromptViewModel` → M3 (auth.composables-material3 + auth.ui-material3 deps) | https://github.com/yschimke/horologist/pull/5#issuecomment-4316800564 |
| 9 | `0b1466f6d` | `UampPlaylistsScreen` → wraps `media.ui.material3.PlaylistsScreen`; M3 `AlertDialog`; `media.uiMaterial3` dep | https://github.com/yschimke/horologist/pull/5#issuecomment-4317006675 |
| 10 | `5acd011d8` | `UampMediaPlayerScreen` + `NewHotnessPlayerScreen` + `UampSettingsButtons` + `FavoriteButton` → M3 (`media.ui.material3.*`, `audio.ui.material3.*`); `media.audioUiMaterial3` dep | https://github.com/yschimke/horologist/pull/5#issuecomment-4318410269 |
| 11 | `ef13dfb58` | Delete `NewHotness*` (scratch screen from #2429) | — |

## Conventions established (apply for the rest)

1. **`MediaScreenScaffold`** (`media/sample/src/main/java/com/google/android/horologist/mediasample/ui/common/MediaScreenScaffold.kt`) — every screen with a `TransformingLazyColumn` uses this instead of M3's `ScreenScaffold` directly. It conditionally hides the `ScrollIndicator` while `LocalScrollCaptureInProgress.current` is true so `@ScrollingPreview(LONG)` captures stay deterministic.
2. **Stateful/stateless split per screen.** The stateless inner takes a single state data class + per-action callbacks; the stateful outer wires `hiltViewModel()`. Previews exercise the stateless variant.
3. **Previews wrap in `AppScaffold` + `TimeText(timeSource = FixedPreviewTimeSource)`** so the system UI is visible and stable. `FixedPreviewTimeSource` lives in `media/sample/src/debug/java/.../ui/settings/PreviewScaffold.kt` — import it from any preview file.
4. **Every TLC item carries `Modifier.minimumVerticalContentPadding(...)`** with the M3-recommended constants: `ListHeaderDefaults.minimumTopListContentPadding` for headers, `ButtonDefaults.minimumVerticalListContentPadding` for buttons / `CheckboxButton`s.
5. **For scrolling content add `@ScrollingPreview(modes = [ScrollMode.LONG])` previews**, in addition to the small/large round top frame. Default in 0.7.11 is `reduceMotion = true`, which is what we want.
6. **Per-screen GitHub PR comment**, not one mega comment. Stage rendered PNGs into `pr5/<screen>/`, push to `preview_pr/5` orphan branch, then `gh pr comment 5` on the canonical PR `yschimke/horologist#5`.
7. **No emojis in code, no docstrings beyond one-line WHY**.

## What's left

Remaining files importing M2 (`grep -rlE "^import androidx\.wear\.compose\.material[^3]" media/sample/src/main` and `grep -rlE "^import com\.google\.android\.horologist\.media\.ui\.(navigation|screens|components|state|snackbar|complication|model|tiles)" media/sample/src/main`):

### A. Browse + entity wrappers (5 files, all delegate to library composables — straight wrap-and-swap)

- `ui/browse/UampBrowseScreen.kt` → wrap `media.ui.material3.screens.browse.PlaylistDownloadBrowseScreen` (lib already exists)
- `ui/browse/UampStreamingBrowseScreen.kt` → wrap `media.ui.material3.screens.browse.BrowseScreen`
- `ui/entity/UampEntityScreen.kt` → wrap `media.ui.material3.screens.entity.PlaylistDownloadScreen` (also drops `compose.material.AlertDialog` for M3 `AlertDialog`)
- `ui/entity/UampStreamingPlaylistScreen.kt` → wrap `media.ui.material3.screens.entity.PlaylistStreamingScreen`
- View models that import `media.ui.state.*` or `media.ui.screens.*.PlaylistsScreenState` etc. — change package to `media.ui.material3.…` where the M3 module re-exports the state class. Some types may live in `media.ui.state` still (shared) — verify each import.

### B. Snackbar plumbing (2 files)

- `ui/app/SnackbarViewModel.kt` and `ui/app/MediaPlayerAppViewModel.kt` use `media.ui.snackbar.{SnackbarManager, SnackbarViewModel, UiMessage}`.
- **No `media.ui.material3.snackbar.*` exists.** Either keep importing from `media.ui.snackbar` (and keep `projects.media.ui` as a runtime dep just for snackbar) or copy the three classes into `mediasample.ui.snackbar` and drop `projects.media.ui` entirely. Recommendation: copy into the sample, since this is a Wear Snackbar implementation that the sample owns the lifecycle of, and it's only ~50 lines of code.

### C. App root + navigation (the big one — 3 files, all coupled)

- `ui/app/UampTheme.kt` — replace M2 `MaterialTheme` + `Colors` with M3 `MaterialTheme` + `ColorScheme`. The existing color values map cleanly: M2 `primary/secondary/background/surface/onPrimary/...` → M3 `colorScheme.{primary,secondary,background,surface,onPrimary,...}`. Reference `auth/composables-material3/src/main/.../theme/{Color,Theme}.kt` for the standard wiring.
- `ui/app/UampWearApp.kt` — swap to `media.ui.material3.navigation.MediaPlayerScaffold` (signature: `(volumeViewModel, playerScreen, libraryScreen, categoryEntityScreen: @Composable (id: String, name: String) -> Unit, mediaEntityScreen, playlistsScreen, settingsScreen, deepLinkPrefix, navController, …)`). Also drops `MediaInfoTimeText`'s wrapping `TimeText`-elsewhere case — pass `timeText = { MediaInfoTimeText(…) }` directly.
- `ui/navigation/UampNavigationScreen.kt` — **breaking shape change**. M3 `NavigationScreens` uses **string routes**, not the typed-serializable pattern this PR uses today. Every `navController.navigate(SomeScreen)` in the codebase becomes `navController.navigate(NavigationScreens.SomeScreen.destination())` or a sample-local extension. The `composable<X> { … }` wiring becomes `composable(route = "…") { … }`. Touch every screen that calls `navController.navigate(…)` — see `grep -rn "navController\.navigate\|popUpTo<" media/sample/src/main`.

  Concretely the affected callsites today:
    - `UampSettingsScreen` (`GoogleSignInScreen`, `GoogleSignOutScreen`, `DeveloperOptions`, `popUpTo<NavigationScreen.Player>`)
    - `DeveloperOptionsScreen` (`AudioDebug`, `Samples`)
    - `GoogleSignInPromptScreen` (`GoogleSignInScreen`)
    - `MediaPlayerAppViewModel` / startup nav (`navigateToLibrary`)
    - `UampWearApp` `additionalNavRoutes` block

  Suggested approach: keep `UampNavigationScreen` as **string-route holders** (`object DeveloperOptions { const val route = "developerOptions" }`) so callsites stay readable, and add navigate extensions if helpful.

### D. Player / browse view models

- `ui/player/MediaPlayerScreenViewModel.kt` extends `media.ui.state.PlayerViewModel` (M2). The M3 module re-exports `PlayerViewModel` (verify path — likely `media.ui.material3.state.PlayerViewModel` or just shared from `media.core`). Update the base class import.

### E. Tile + complication services (1+1 files)

- `data/service/tile/MediaCollectionsTileService.kt` uses `MediaCollectionsTileRenderer` from `media.ui.tiles`.
- `data/service/complication/MediaStatusComplicationService.kt` uses `MediaStatusTemplate` from `media.ui.complication`.

These are protolayout-based (not Wear Compose), so they don't need an "M3 component" port. They live in `media.ui` for now — leaving them M2 is fine. **If the goal is "drop `projects.media.ui`", these have to either move into `media.ui-material3` upstream (out of PR scope) or stay where they are with `projects.media.ui` kept as a `tiles`-only dependency.** Recommend the latter and call it out in the PR description.

### F. Final dep cleanup (after A–E)

In `media/sample/build.gradle.kts`:

- **Drop:** `libs.wearcompose.material`, `projects.composeMaterial`. (Keep `media.ui` only if E still needs it for tile/complication code; otherwise drop.)
- **Keep:** `libs.androidx.wear.compose.material3`, `projects.media.uiMaterial3`, `projects.media.audioUiMaterial3`, `projects.auth.composablesMaterial3`, `projects.auth.uiMaterial3`.

### G. Roborazzi baselines

- `UampPlayerScreenshotTest.kt` (only existing screenshot test in the module) currently references M2 `MaterialTheme` and `media.ui.*`. Update its imports + test fixture to M3 (mirroring the now-M3 production code), then run `./gradlew :media:sample:recordRoborazziDebug` and commit the regenerated PNGs under `media/sample/src/test/screenshots/`.
- Each commit on this branch so far has been roborazzi-clean (`verifyRoborazziDebug` passed at the time of first commit; should still pass for the un-affected device/font matrix). Re-record only after E lands and the player test compiles again.

## Useful paths / commands

- M3 source jars unpacked locally for grepping: `/tmp/m3srcs` (compose-material3-1.6.1) and `/tmp/srcsjar` (compose-foundation-1.6.1).
- WEAR_UI guidance: `/home/yuri/.claude/skills/compose-preview/design/WEAR_UI.md`.
- Render previews: `compose-preview render --module media:sample --filter <name>` (or `./gradlew :media:sample:renderAllPreviews` for everything).
- Verify roborazzi: `./gradlew :media:sample:verifyRoborazziDebug`.
- Compile only: `./gradlew :media:sample:compileDebugKotlin`.

## Gotchas hit (don't relearn these)

- `Modifier.minimumVerticalContentPadding(...)` is a member extension on `TransformingLazyColumnItemScope`, **not** a top-level Modifier function. Only callable inside `item { … }`.
- `SurfaceTransformation(spec)` is also a member of `TransformingLazyColumnItemScope` — same constraint.
- M3 has **no `FilledTonalButton`**. Use `Button(... colors = ButtonDefaults.filledTonalButtonColors())`.
- `wear.compose.foundation` `MinimumVerticalContentPadding{Element,Node}` classes exist in 1.6.1 but the **public extension only lives in `TransformingLazyColumnDsl.kt`** — not directly on `Modifier`.
- `compose-preview` CLI takes `--module media:sample` (no leading colon — it prepends one).
- Many `*.compose-preview-history/` and `*/bin/` directories appear as untracked; they're build outputs and ignored by `.gitignore`. Don't `git add -A`.
- Adding `preview-annotations` library: defined in `[libraries]` of `gradle/libs.versions.toml` (line 241) and `debugImplementation(libs.preview.annotations)` in `media/sample/build.gradle.kts`.

