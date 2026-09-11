# Task: Lottie Values and Primitives Spec Alignment

> **Task ID:** `task_PL_LOT_VAL`
> **Created:** 2026-09-02 15:02
> **Last updated:** 2026-09-02 15:02
> **Status:** `in-progress`
> **Contributors:** `jetski`

## Current Work Item

| Field | Value |
|---|---|
| **Document** | `plan` — [Lottie Values and Primitives Plan](../../docs/lottie_values_primitives.plan.md) |
| **Pipeline phase** | `plan` |
| **Traceable ID** | `PL_LOT_VAL` |
| **Ticket** | `PR-2814` (https://github.com/google/horologist/pull/2814) |

## Intent

- **Goal (why):** Address all 19 reviewer comments left by `@yschimke` on PR #2814, resolve architectural questions, and strictly align Bézier and Gradient value models with the official Lottie 1.0.1 Specification and Schema.
- **Target state:** `BezierValue` requires `v`, `i`, and `o` with strict 2D coordinate validation; `GradientValue` requires `numberOfColors` and `values` with spec-compliant stop partitioning ($4 \times N_c + 2 \times N_o$); `resolveStops()` merges offsets using epsilon comparison; serializers round-trip cleanly without mutating the format.
- **Expected result:** All unit tests pass; `./gradlew :remotecompose:lottie:check` succeeds; PR #2814 review comments are completely addressed and verified ready for merge.

## Description

Fix all issues identified in code review on PR #2814 (`values/Bezier.kt` and `values/Gradient.kt`) following a strict TDD approach:
1. Write representative unit test cases covering edge cases (strict point validation, missing properties, stop layout with transparency, round-trip serialization, Remote types conversion).
2. Refactor `BezierValue`, `BezierValueSerializer`, and `RemoteBezierValue` (Remote types adoption via `List<List<RemoteFloat>>`).
3. Refactor `GradientValue` and `GradientValueSerializer` (`RemoteColor` adoption).
4. Run full local verification (`ktfmtFormat`, `compileDebugKotlin`, `testDebugUnitTest`, `check`).
— `jetski`

## Subtasks

### Subtask: PR #2814 Review Resolution
> Author: `jetski` — Created: 15:02 — Last updated: 16:02 — Status: `in-progress`

**Goal:** Execute Phase 1 through Phase 4 of `PL_LOT_VAL` following TDD.

**Progress:**
- [x] Analyze PR #2814 and PR #2813 comments against Lottie 1.0.1 spec & schema
- [x] Author Specification (`SP_LOT_VAL`) in `docs/lottie_values_primitives.sp.md`
- [x] Author Implementation Plan (`PL_LOT_VAL`) in `docs/lottie_values_primitives.plan.md`
- [x] Refactor `BezierValue` signature (remove synthetic defaults for `i`, `o`, `v`) & `BezierValueSerializer` descriptor
- [x] Refactor to `Point` model in `format.values` (`Point(x, y)`), normalize $\ge 2$ coordinates, discard 3D extras, throw on $< 2$, adopt `Point(x: RemoteFloat, y: RemoteFloat)` in `RemoteBezierValue`
- [x] Refactor `GradientValue` spec alignment & serializer (`RemoteColor` adoption)
- [x] Migrate `Point` to `RemoteFloat` (`Point(RemoteFloat, RemoteFloat)`) and `BezierValue` to `RemoteBoolean` (`closed: RemoteBoolean`) without custom equals, hashCode, or toString
- [x] Verify test suite & run module checks

**Activity:**
- 15:02 — Created task, specification (`SP_LOT_VAL`), and implementation plan (`PL_LOT_VAL`)
- 15:23 — Restored TDD test-first phase per user confirmation
- 15:35 — Configured Remote types for Bézier curves
- 15:46 — Documented dual boolean (`true`/`false`) and integer (`1`/`0`) closed flag support in deserialization based on spec text vs. JSON Schema specification analysis
- 16:02 — Completed `BezierValue` signature refactor, serializer descriptor update, and initial tests
- 17:25 — Updated design decision `DEC_04` to model 2D points using `Point` data classes in AST and renderer, normalizing coordinates ($\ge 2$ extracted, extras discarded, $< 2$ throws) per user direction
- 18:30 — Completed `Point` and `BezierValue` refactoring and unit tests
- 19:15 — Completed `GradientValue` refactor (`RemoteColor`, epsilon offset merging, required params, collection utils, strict coordinate validation) and full module verification passing
- 15:15 — User directed migration to `RemoteFloat` for `Point` and `RemoteBoolean` for `BezierValue`
- 15:42 — Removed custom `equals`, `hashCode`, and `toString` from `Point` and `BezierValue` per user direction; updated test assertions to evaluate constant values; all module checks and Roborazzi tests pass
- 15:51 — Eliminated duplicate `RemoteBezierValue` and `BezierValue.toRemote()`; `animateBezier` returns `BezierValue` directly; all checks pass

## Relevant Context

| Type | Name / Path | Note (added by) |
|---|---|---|
| Spec | `docs/lottie_values_primitives.sp.md` | Lottie 1.0.1 value primitives specification — `jetski` |
| Plan | `docs/lottie_values_primitives.plan.md` | Implementation plan and phase breakdown — `jetski` |
| Upstream PR | https://github.com/google/horologist/pull/2814 | Target PR under review — `jetski` |
| Prior PR | https://github.com/google/horologist/pull/2813 | Modularization PR review comments context — `jetski` |

## Shared Activity Log

- 15:02 [`jetski`] — Created task `task_PL_LOT_VAL` and authored `SP_LOT_VAL` and `PL_LOT_VAL`
