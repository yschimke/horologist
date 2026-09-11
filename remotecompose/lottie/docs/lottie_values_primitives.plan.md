# Implementation Plan: Lottie Values and Primitives  {#PL_LOT_VAL}

> **Code:** PL_LOT_VAL
> **Status:** in-progress
> **Created:** 2026-09-02
> **Updated:** 2026-09-02
>
> **Specification:** [SP_LOT_VAL](./lottie_values_primitives.sp.md)
> **Depends on:** none
> **Used by:** [PL_LOT_SHAPES](./lottie_shapes.plan.md)
>
> Implementation plan for resolving reviewer comments on PR #2814 and aligning Bézier and Gradient models with the Lottie 1.0.1 specification.

## Goal

Resolve all 19 reviewer comments on PR #2814 by enforcing strict schema requirements (mandatory parameters without synthetic defaults), validating 2D point coordinates, aligning gradient stop parsing with the $4 \times N_c + 2 \times N_o$ formula, adopting RemoteColor, preventing floating-point deduplication bugs, and guaranteeing clean round-trip serialization.

## Technology Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Test-Driven Development (TDD) | Upfront test suite in `ParsingTest.kt` | Formulate regression reproducers and edge cases for Bézier and Gradient before modifying implementation |
| Coordinate Validation | Normalization ($\ge 2$ coordinates, extras discarded, $< 2$ throws) | Ensures strictly 2D points while tolerating 3D vector coordinates from After Effects |
| Offset Merging | Epsilon threshold ($10^{-4}$) | Avoids IEEE-754 `.distinct()` equality failures on float offsets |
| Stop Extraction | `drop(4 * Nc).chunked(2)` | Replaces error-prone manual index `while` loops with idiomatic Kotlin collections |
| Remote Types Adoption (Gradient) | `RemoteColor` in `ResolvedColorStop` | Fulfills reviewer feedback ("use Remote types") and decouples AST from AndroidX Compose UI |
| Point Model & Remote Types (Bezier) | `Point` in `BezierValue` and `RemoteBezierValue` | Strong type safety (`Point(x, y)` in AST, `Point(x: RemoteFloat, y: RemoteFloat)` in renderer), explicit named fields, eliminates nested array bounds errors |

## Required Knowledge

| Kind | Ref | Applies to | Note |
|---|---|---|---|
| rule | Testing Policy & TDD | Phase 1 | First step of every plan must write representative tests |
| rule | Module Execution Order | Phase 4 | Format -> Signatures -> Compile -> Unit Tests -> Check |
| rule | Spec-Driven Protocol | All Phases | Never execute entire plan in one pass; atomic verification per phase |

## Progress

- [ ] [Phase 1 — Write Unit Tests & Regression Reproducers (TDD)](#PL_LOT_VAL_P1)
- [ ] [Phase 2 — Refactor Bezier.kt Validation & Defaults](#PL_LOT_VAL_P2)
- [ ] [Phase 3 — Refactor Gradient.kt Spec Alignment & Serializer](#PL_LOT_VAL_P3)
- [ ] [Phase 4 — Local Verification & Commit](#PL_LOT_VAL_P4)

## Phases

### Phase 1 — Write Unit Tests & Regression Reproducers (TDD) [TODO]  {#PL_LOT_VAL_P1}

**Depends on:** none  
**Implements:** [SP_LOT_VAL_05_01](./lottie_values_primitives.sp.md#SP_LOT_VAL_05_01)  
**Verify:** New tests fail or capture missing functionality as expected before production code is refactored.

Tasks:
1. Add tests in `ParsingTest.kt` for `BezierValueSerializer` and `Point`:
   - Acceptance of $\ge 2$ coordinates (taking first 2, discarding 3D extras).
   - Rejection of $< 2$ coordinates (`[[10.0]]`) with `SerializationException`.
   - Rejection when required properties (`v`, `i`, `o`) are absent.
   - Acceptance and correct parsing of closed flags: support both boolean (`"c": true` / `"c": false`) and integer (`"c": 1` / `"c": 0`) without forcing only 0/1 or only boolean.
   - Round-trip serialization maintaining canonical boolean `"c"` and 2-element point arrays.
   - Conversion to Remote types: verify `BezierValue.toRemote()` converts coordinates to `RemoteBezierValue` with `List<Point>` of `RemoteFloat`.
2. Add tests in `ParsingTest.kt` for `GradientValue` & `GradientValueSerializer`:
   - Multi-stop layout with transparency ($4 \times N_c + 2 \times N_o$) creating corresponding color and opacity stops.
   - Epsilon-threshold deduplication in `resolveStops()` when color and opacity offsets differ by less than $10^{-4}$.
   - Migration to `RemoteColor` in `ResolvedColorStop`.
   - Round-trip serialization preserving `"p"` and `"k"` stops without data loss.

### Phase 2 — Refactor Bezier.kt & Point.kt to Remote Types (`remotecompose/lottie/.../values/`) [COMPLETED]  {#PL_LOT_VAL_P2}

**Depends on:** Phase 1  
**Implements:** [SP_LOT_VAL_01_01](./lottie_values_primitives.sp.md#SP_LOT_VAL_01_01), [SP_LOT_VAL_01_02](./lottie_values_primitives.sp.md#SP_LOT_VAL_01_02), [SP_LOT_VAL_02_01](./lottie_values_primitives.sp.md#SP_LOT_VAL_02_01), [SP_LOT_VAL_DEC_01](./lottie_values_primitives.sp.md#SP_LOT_VAL_DEC_01), [SP_LOT_VAL_DEC_04](./lottie_values_primitives.sp.md#SP_LOT_VAL_DEC_04)  
**Verify:** Unit tests pass with RemoteFloat coordinates and RemoteBoolean closed flag.

Tasks:
1. Refactor `format/values/Point.kt`: `Point(val x: RemoteFloat, val y: RemoteFloat)` with secondary constructor `Point(x: Float, y: Float) : this(x.rf, y.rf)`.
2. Update `PointSerializer`: deserialization reads $\ge 2$ coordinates into `Point(x.rf, y.rf)`, serialization writes `JsonPrimitive(value.x.constantValue)`.
3. Refactor `format/values/Bezier.kt`: `closed: RemoteBoolean = false.rb`.
4. Update `BezierValueSerializer`: parse closed flag into `RemoteBoolean` (`.rb`), serialization writes `put("c", value.closed.constantValue)`.
5. Remove duplicate `Point` definition in `renderer/properties/Position.kt` and use `format.values.Point`.
6. Remove duplicate `RemoteBezierValue` in `renderer/properties/Bezier.kt` and use `format.values.BezierValue` directly.
7. Remove redundant `BezierValue.toRemote()` passthrough; `animateBezier` returns `BezierValue`.
8. Update `renderer/shapes/Path.kt`: evaluate `path.closed.constantValueOrNull ?: false`.
9. Update unit tests in `ParsingTest.kt` to verify Remote types and round-trip fidelity.

### Phase 3 — Refactor Gradient.kt Spec Alignment & Serializer (`remotecompose/lottie/.../values/Gradient.kt`) [TODO]  {#PL_LOT_VAL_P3}

**Depends on:** Phase 2  
**Implements:** [SP_LOT_VAL_01_03](./lottie_values_primitives.sp.md#SP_LOT_VAL_01_03), [SP_LOT_VAL_01_04](./lottie_values_primitives.sp.md#SP_LOT_VAL_01_04), [SP_LOT_VAL_02_02](./lottie_values_primitives.sp.md#SP_LOT_VAL_02_02)  
**Verify:** Phase 1 Gradient tests pass.

Tasks:
1. Update `GradientValue` signature: remove `= 0` and `= emptyList()` defaults.
2. Add class-level and property-level documentation referencing Lottie Spec §2.5 and quoting the $4 \times N_c + 2 \times N_o$ stop layout formula.
3. Remove un-specced `values.size / 4` and `values.size % 4 != 0` heuristics; derive opacity stop count via $N_o = (\text{values.size} - 4 \times N_c) / 2$.
4. Refactor `opacityStops` using `values.drop(numberOfColors * 4).chunked(2).map { ... }`.
5. Update `resolveStops()` to use epsilon-based offset deduplication (`abs(a - b) < 1e-4f`) instead of `.distinct()`.
6. Fix `GradientValueSerializer` to ensure clean, symmetric round-trip serialization.
7. Migrate `ResolvedColorStop` from `androidx.compose.ui.graphics.Color` to `RemoteColor` (`Color(r, g, b, alpha).rc`), directly addressing reviewer comment ("use Remote types").
8. Update `ParsingTest.kt` assertions to check `RemoteColor` (e.g. `assertThat(stop.color).isEqualTo(Color.Red.rc)` or color integer).

### Phase 4 — Local Verification & Commit [TODO]  {#PL_LOT_VAL_P4}

**Depends on:** Phase 3  
**Verify:** All checks in `:remotecompose:lottie` pass.

Tasks:
1. Format code: `./gradlew :remotecompose:lottie:ktfmtFormat`
2. Update metalava signatures (if needed): `./gradlew :remotecompose:lottie:metalavaGenerateSignatureDebug`
3. Compile Kotlin: `./gradlew :remotecompose:lottie:compileDebugKotlin`
4. Run unit tests: `./gradlew :remotecompose:lottie:testDebugUnitTest`
5. Run all checks: `./gradlew :remotecompose:lottie:check`
6. Propose commit message following project commit guidelines.
