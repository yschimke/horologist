# RC Lottie implementation guide for agents

This guide is for agents changing `:remotecompose:lottie`. The module translates
Lottie JSON into a Remote Compose (RC) document. AndroidX's RC player executes
that document; this module does not implement the AndroidX player itself.

The primary implementation requirement is correct playback from one recorded
document. A feature that works only when Kotlin records a new document at every
frame is not implemented for dynamic playback.

## Before changing code

1. Read the module's [workflow instructions](.agents/AGENTS.md), relevant rules
   under [.agents/rules](.agents/rules), and the
   [upstream issue backlog](docs/upstream-bugs.txt).
2. Check the working tree, branch, and dependency versions. Preserve unrelated
   changes. Do not assume this checkout is on a particular branch or that a
   previously tested AndroidX API is available in the current dependency.
3. Define a bounded behavior contract: supported inputs, expected pixels or
   values, time/coordinate spaces, boundaries, and exclusions. Distinguish
   decoding support, constant-frame rendering, and dynamic playback support.
4. Follow the required [isolated test-author workflow](.agents/agents/dev-test.md)
   before production changes. Give the test author the contract and public test
   interfaces, not the planned implementation. Establish a passing control and
   a failing reproduction. Documentation-only changes need documentation checks,
   not new runtime tests.

The workflow refers to a `dev-flow` skill. If it is unavailable, state that and
follow the checked-in workflow rules directly; do not invent an installed skill.
Use the `compose-preview` skill when rendering previews, if available.

## Architecture and source map

Production paths in this table are relative to
`src/main/java/com/google/android/horologist/remotecompose/lottie/`.

| Area | Entry points | Responsibility |
| --- | --- | --- |
| Loading/model | `format/Animation.kt`, `format/` | Decode JSON, preserve property variants and exporter fields. |
| Recording entry | `LottieAnimation.kt` | Map progress/clock to frames, create `LottieSettings`, fit to the canvas, traverse layers. |
| Layer evaluation | `renderer/layers/Layer.kt`, `PrecompLayer.kt` | Visibility, ancestry, precomp time and asset traversal. |
| Property evaluation | `renderer/properties/`, `renderer/Animation.kt` | Convert keyframes into live RC expressions and bounded lookup tables. |
| Shape traversal | `renderer/Shape.kt` | Associate geometry with styles, apply modifiers, masks and matte handling. |
| Geometry | `renderer/shapes/` | Construct and transform paths, evaluate modifiers and repeater instances. |
| Drawing | `renderer/RemoteShape.kt`, `renderer/RemoteStyle.kt` | Emit paths and paints, retain fill rules, trims and inherited opacity. |

Debug host adapters are in `src/debug/java/.../LottiePreview.kt`.
`rememberRemoteDocument` records the content; `RemoteDocumentPlayer` plays it.
Explicit progress uses a named remote float called `progress`, updated through
`setUserLocalFloat`. `LottieAnimatedPreview` drives that value with a Compose
transition. Host recomposition may update the value without rerecording content.

## RC implementation constraints

### Recording versus playback

- Kotlin loops, ordinary `if` statements, Android `Path` operations and ordinary
  floats execute while recording. They do not automatically rerun at playback.
- Preserve `RemoteFloat` expressions for changing positions, sizes, colors,
  opacity, trim endpoints and visibility. Use remote arithmetic and conditional
  expressions for decisions that depend on playback state.
- `constantValueOrNull == null` means the value is dynamic, not absent or zero.
  Do not use `constantValueOrNull ?: 0f` to make a dynamic feature compile.
  Reading `constantValue` requires a justified constant-input precondition.
- CPU preprocessing is appropriate for constant geometry or bounded samples of
  immutable keyframe data. Playback must still select/interpolate those samples
  using remote expressions. Document approximation error and table-size bounds.
- Dynamic topology requires an explicit design. A Kotlin loop over a recorded
  copy count cannot create a changing number of copies later. Do not silently
  advertise support based on a constant-count fallback.
- Confirm the selected player backend supports each operation, including its
  dynamic-input and caching behavior. Authoring API availability alone is not
  evidence of playback support. Keep backend limitations in the backlog.

### Time and interpolation

- Root explicit progress currently maps to `ip + progress * (op - ip)`. Clock
  playback uses the document animation clock and frame rate. Do not introduce a
  separate host clock into production property evaluation.
- Keep containing-composition time separate from child-precomp time. Bind
  precomp transforms and opacity to the containing timeline before switching
  settings. Child time uses `(frame - st) / sr`, or time remap in seconds
  multiplied by frame rate. Test nested, shifted and stretched compositions.
- Hold keyframes retain the starting value until the next boundary. Test the
  boundary itself, fractional frames, zero-duration segments and final values.
- Temporal easing and spatial interpolation are different operations. Preserve
  easing arrays and select handles per dimension; scalar/singleton fallback is
  covered by tests. Spatial `to` and `ti` belong to the starting keyframe and
  are relative to the start and end positions respectively.
- `SpatialPosition.kt` traverses a sampled cubic by eased arc length. Its current
  256-interval tables are an approximation. Preserve endpoint tangent behavior
  for over/undershoot; easing output is not necessarily confined to `[0, 1]`.
- Layer out-point padding and malformed root timelines remain known issues.
  Do not change endpoint policy incidentally while fixing interpolation.

### Geometry, paint and compositing

- Name the coordinate space at each boundary: shape-local, layer-local,
  containing composition, child composition, or fitted canvas. Apply transforms
  in the intended order and fit-to-canvas scaling only once at the root.
- Preserve path metadata when copying/transformation code creates new shapes:
  fill rule, live trim data, group transform and opacity multiplier matter.
- Keep colors and paint parameters dynamic. A path can move correctly while
  its fill, alpha or gradient remains frozen at recording time.
- Rectangle/ellipse trim and rounding preserve live geometry. The runtime trim
  path is scoped to a single parametric contour; do not extend it to combined
  contours without testing path measurement and ordering. Wrapped trims and
  later nonuniform geometry transforms have documented parity gaps.
- Shape-list ordering matters. Test exported `[geometry, fill, repeater]`
  ordering, not only handcrafted geometry-before-modifier arrangements.
- Repeater transforms have their own model: `so`/`eo` are independent opacity
  endpoints, not ordinary transform `o`. The current compatibility fallback
  uses legacy `o` only for missing endpoints. The pinned reference uses
  `copyIndex / copies` for the ramp, including a fractional count denominator.
- A clip path is not alpha/luminance compositing. Do not describe current
  outline-based matte clipping as full matte support. Mask opacity, expansion,
  mixed-operation ordering and precomp masks/bounds still need work.

## How to prove a fix

Use the smallest valid JSON fixture that exposes the behavior. Include required
property discriminators and transforms; a decoding failure does not establish a
rendering failure. Test malformed input separately as a resilience contract.

Use three distinct kinds of evidence as applicable:

1. Parser/property tests: retained fields, round trips, static values and
   boundaries. These cannot by themselves prove dynamic RC playback.
2. Native playback tests: record once, update named progress, and assert pixels
   at start/interior/end frames. Include positive visibility probes and negative
   background probes so blank or misplaced output cannot pass.
3. Reference/golden tests: compare against an independent expected result and
   separately verify stored goldens. A side-by-side screenshot is not a pixel
   parity assertion, and a golden can preserve an existing rendering defect.

Start from these tests in `src/test/java/.../lottie/`:

- `MotionPixelHarness.kt`: single-document progress and pixel assertions.
  Its fixtures use `ip=0`, `op=40`; `advance(frame)` divides by 40. Do not copy
  that mapping unchanged into a fixture with another root timeline.
- `MovingGeometryModifierTest.kt`: dynamic geometry against static fixtures.
- `PositionInterpolationTest.kt`: independent `PathMeasure` motion expectations,
  per-axis easing, serialization and overshoot controls.
- `RepeaterOpacityTest.kt`: exported ordering, copy count and opacity ramps.
- `PlaybackEffectsRegressionTest.kt`: masks, gradients and precomp timing.
- `DynamicColorPlaybackTest.kt`, `ReviewRegressionTest.kt`: color and keyframe
  boundary regressions.

The dynamic view must stay mounted across frames. A changing static reference
fixture needs a fresh recording (the harness keys that reference separately).
Probe the reference's expected position too: comparing against a stale or blank
reference can hide a test-harness bug. Static fixtures rendered by this module
are consistency checks, not independent lottie-android parity evidence.

Use the native `captureRoboImage` approach in the harness. `captureToImage`
hung with this native Robolectric setup during the playback work. Keep pixel
tolerances explicit and justified. Do not weaken assertions or add ignores to
make an implementation pass. `src/staleTest/` is excluded from the configured
test sources; its files are not evidence of executed coverage.

## Verification commands

Run from the repository root, using the module-specific tasks in this order.
Use `build-brief` for Gradle output when available. Do not run competing Gradle
builds or preview renders concurrently in this checkout.

```sh
build-brief ./gradlew \
  :remotecompose:lottie:ktfmtFormat \
  :remotecompose:lottie:metalavaGenerateSignatureDebug \
  :remotecompose:lottie:compileDebugKotlin \
  :remotecompose:lottie:assembleDebug \
  :remotecompose:lottie:testDebugUnitTest \
  :remotecompose:lottie:check
build-brief ./gradlew :remotecompose:lottie:verifyRoborazziDebug
```

Inspect generated API changes and screenshot differences before accepting them.
Do not rerecord goldens merely to remove failures. Run downstream checks if a
change affects consumers. Documentation-only changes should instead verify
paths, links, commands against configuration, and `git diff --check`.

### Still and animated previews

For sample or rendering changes, use the compose-preview workflow:

```sh
compose-preview show --module :remotecompose:lottie --json --timeout 1200
python remotecompose/lottie/tools/check_previews.py \
  --contact-sheet /tmp/lottie-previews.png
```

Read capture paths from `build/compose-previews/previews.json`; do not invent
hashed filenames. Inspect the contact sheet, including initial/middle/final GIF
frames. A successful render command does not prove visible artwork or motion.
Use a contrasting background, particularly for the black M3 artwork.

When adding a JSON sample under `src/debug/res/raw`, add both a still and an
animated preview in `LottiePreviews.kt`. Follow the existing direct `R.raw`
references: the checker resolves coverage from preview function bodies. Choose
a meaningful still progress and sufficient GIF duration to show the source
motion. Static source samples may legitimately produce non-moving GIFs.

Generated captures stay under `build/`; contact sheets are review artifacts,
not committed baselines. Do not force rerenders, clear caches or update tooling
without evidence that the ordinary render path is stale or broken.

## Remaining scope and handoff

Treat [upstream-bugs.txt](docs/upstream-bugs.txt) as the detailed issue ledger.
Open areas include matte compositing, mask semantics, precomp masks/bounds,
font/text playback, other dynamic geometry modifiers, broader repeater behavior,
animated dashes, ignored layer features, endpoint policy and malformed inputs.
A partial fix must leave its remaining scope explicit rather than closing the
whole finding.

For each fix, record the reproduction, cause, applied/suggested fix, tests and
remaining limitations in the ledger. Separate code-confirmed limitations from
reproduced defects. Do not file upstream issues without user authorization.
Keep commits scoped by behavior, and separate documentation/cleanup from runtime
changes. Report verification actually run, skipped tests, API/golden changes,
and whether commits are local or pushed. Push only to the authorized remote
and branch; do not infer those from this guide.

Last verified implementation snapshot: `311633835`, 2026-09-11. Module checks
and explicit golden verification passed with 399 passing tests and 17 existing
skips. Preview validation covered 15 samples, 33 previews (17 PNGs, 16 GIFs),
including 11 moving GIFs and five static-source GIFs. These are historical
results, not acceptance counts to hardcode or evidence for later changes.

## References and glossary

- [Lottie 1.0.1 specification](https://lottie.github.io/lottie-spec/1.0.1/single-page/)
- [Lottie 1.0.1 schema](https://lottie.github.io/lottie-spec/1.0.1/lottie.schema.json)
- [Property documentation](https://lottie.github.io/lottie-spec/dev/specs/properties/)
- [Pinned lottie-android reference](https://github.com/airbnb/lottie-android/tree/v6.7.1/lottie/src/main/java/com/airbnb/lottie)

Check `gradle/libs.versions.toml` before choosing a reference version. Published
specification coverage and exporter/player extensions differ; record which
contract a regression asserts instead of silently combining them.

RC: Remote Compose. Recording: constructing the document and its expressions.
Playback: evaluating that document in the player. Dynamic value: an expression
whose value can change without rerecording. Topology: number/connectivity of
paths, vertices or copies. Oracle: an independently justified expected result.
Parity: agreement with a specified reference renderer, not merely a passing
stored screenshot.

## Alternatives considered

- Rebuild the document per frame: simplifies CPU evaluation but masks frozen
  expressions and does not meet the single-document playback contract.
- Enable the entire stale suite at once: increases apparent coverage but mixes
  obsolete assumptions with feature work. Port tests by behavior instead.
- Treat previews/goldens as full parity tests: useful for visibility/regressions,
  but insufficient without independent expectations and controlled frame input.
