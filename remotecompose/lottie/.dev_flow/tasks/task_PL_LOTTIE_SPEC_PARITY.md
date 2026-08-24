# Task: Lottie 1.0.1 Specification Parity & Hardening

> **Task ID:** `task_PL_LOTTIE_SPEC_PARITY`
> **Created:** 2026-08-24 17:45
> **Last updated:** 2026-08-24 19:50
> **Status:** `in-progress`
> **Contributors:** dev-flow-orchestrator

## Current Work Item

| Field | Value |
|-------|-------|
| **Document** | `plan` — [`lottie_spec_parity.plan.md`](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/docs/lottie_spec_parity.plan.md) |
| **Pipeline phase** | `plan` |
| **Traceable ID** | `PL_LOTTIE_SPEC_PARITY` |
| **Ticket** | n/a |

## Intent

- **Goal (why):** Implement the fixes and specification parity roadmap derived from the 35-commit regression analysis and Lottie 1.0.1 specification audit.
- **Target state:** All 4 phases of `PL_LOTTIE_SPEC_PARITY` executed, verified via unit and Roborazzi screenshot diff tests, with 0 regressions.
- **Expected result:** Clean Gradle check passes (`./gradlew :remotecompose:lottie:check`), full test coverage, and complete parity with reference Lottie player.

## Description

Execute the 4-phase implementation plan defined in `docs/lottie_spec_parity.plan.md` step-by-step. Each phase and task is designed to be executed atomically with clean contexts to avoid LLM context drift or hallucinations. — dev-flow-orchestrator

## Subtasks

### Subtask: Execute Phase 1 (Critical Bug Fixes & Hardening)
> Author: `dev-flow-orchestrator` — Created: 17:45 — Last updated: 19:50 — Status: `completed`

**Goal:** Implement Phase 1 critical bug fixes (AST model defaults, serial name annotations, hold flag parsing, transform singularities, PolyStar dynamic path, and hierarchy cycle guards).

**Progress:**
- [x] Create comprehensive audit document `docs/lottie_audit_and_gap_analysis.md`
- [x] Create formal implementation plan `docs/lottie_spec_parity.plan.md`
- [x] Initialize `.dev_flow/` task context and dashboard
- [x] [Task 1.1: AST Model Defaults & Fractional Framerate (`PL_LOTTIE_SPEC_PARITY_T1_1`)](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/docs/lottie_spec_parity.plan.md#PL_LOTTIE_SPEC_PARITY_T1_1)
- [x] [Task 1.2: GradientStroke Annotations & Keyframe Hold Flag Parsing (`PL_LOTTIE_SPEC_PARITY_T1_2`)](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/docs/lottie_spec_parity.plan.md#PL_LOTTIE_SPEC_PARITY_T1_2)
- [x] [Task 1.3: Transform Inversion Singularities (Scale = 0 Guard) (`PL_LOTTIE_SPEC_PARITY_T1_3`)](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/docs/lottie_spec_parity.plan.md#PL_LOTTIE_SPEC_PARITY_T1_3)
- [x] [Task 1.4: PolyStar Dynamic RemoteLottiePath Refactoring (`PL_LOTTIE_SPEC_PARITY_T1_4`)](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/docs/lottie_spec_parity.plan.md#PL_LOTTIE_SPEC_PARITY_T1_4)
- [x] [Task 1.5: Hierarchy Cycle Guard & Dynamic Track Matte Path Safety (`PL_LOTTIE_SPEC_PARITY_T1_5`)](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/docs/lottie_spec_parity.plan.md#PL_LOTTIE_SPEC_PARITY_T1_5)

### Subtask: Execute Phase 2 (Core Rendering & Mathematical Parity)
> Author: `dev-flow-orchestrator` — Created: 19:50 — Last updated: 19:50 — Status: `in-progress`

**Goal:** Implement Phase 2 core rendering and mathematical parity (Spatial Bézier tangents `to`/`ti`, gradient shaders for fill/stroke, stroke dash patterns and miter limits, EvenOdd fill rule, and local layer timing scaling).

**Progress:**
- [ ] **Next:** [Task 2.1: Spatial Bézier Tangents (to, ti) in Position Keyframes (`PL_LOTTIE_SPEC_PARITY_T2_1`)](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/docs/lottie_spec_parity.plan.md#PL_LOTTIE_SPEC_PARITY_T2_1)
- [ ] [Task 2.2: Gradient Shaders for GradientFill and GradientStroke (`PL_LOTTIE_SPEC_PARITY_T2_2`)](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/docs/lottie_spec_parity.plan.md#PL_LOTTIE_SPEC_PARITY_T2_2)
- [ ] [Task 2.3: Stroke Dash Pattern (d) and Miter Limit (ml) (`PL_LOTTIE_SPEC_PARITY_T2_3`)](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/docs/lottie_spec_parity.plan.md#PL_LOTTIE_SPEC_PARITY_T2_3)
- [ ] [Task 2.4: Path FillRule (EvenOdd) & Primitive TrimPath Dispatch (`PL_LOTTIE_SPEC_PARITY_T2_4`)](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/docs/lottie_spec_parity.plan.md#PL_LOTTIE_SPEC_PARITY_T2_4)
- [ ] [Task 2.5: Local Layer Timing Scaling & Inverted Alpha Track Mattes (`PL_LOTTIE_SPEC_PARITY_T2_5`)](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/docs/lottie_spec_parity.plan.md#PL_LOTTIE_SPEC_PARITY_T2_5)

**Activity:**
- 17:45 — Created task and initialized plan for Phase 1.
- 18:06 — Completed Task 1.1: AST Model Defaults & Fractional Framerate with tests and full verification.
- 18:55 — Completed Task 1.2: GradientStroke Annotations & Keyframe Hold Flag Parsing with clean pre-commit review.
- 19:27 — Completed Task 1.3: Transform Inversion Singularities (Scale = 0 Guard) with clean pre-commit review and Roborazzi verification.
- 19:40 — Completed Task 1.4: PolyStar Dynamic RemoteLottiePath Refactoring with clean pre-commit review and Roborazzi verification.
- 19:50 — Completed Task 1.5: Hierarchy Cycle Guard & Dynamic Track Matte Path Safety with clean pre-commit review and full check suite pass. Phase 1 complete!

## Coordination Notes

- 19:50 [dev-flow-orchestrator] — Completed Task 1.5 and completed Phase 1. Added recursion cycle guard `visited: Set<Int>` in `buildAncestorTransforms` and path guards in `buildRemotePathFromBezier`, verified with unit tests and `./gradlew :remotecompose:lottie:check`.
- 19:40 [dev-flow-orchestrator] — Completed Task 1.4. Refactored `evaluatePolyStar` to return `RemoteLottiePath`, added `PolyStarTest`, recorded Roborazzi screenshots, and passed clean-context review.
- 19:27 [dev-flow-orchestrator] — Completed Task 1.3. Verified with unit tests (`TransformTest`), metalava, Roborazzi baseline updates & verification, and clean-context pre-commit review.
- 18:55 [dev-flow-orchestrator] — Completed Task 1.2. Verified with unit tests, metalava, Roborazzi screenshot verification, and clean-context review.
- 18:06 [dev-flow-orchestrator] — Completed Task 1.1. Verified with unit tests, metalava, and Roborazzi screenshot verification.
- 17:45 [dev-flow-orchestrator] — Plan authored and task initialized. Ready for Phase 1 Task 1.1 execution.

## Blocking Issues

[No blockers yet.]

## Relevant Context

| Type | Name / Path | Note (added by) |
|------|-------------|-----------------|
| Plan | [`docs/lottie_spec_parity.plan.md`](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/docs/lottie_spec_parity.plan.md) | Authoritative task breakdown — `dev-flow-orchestrator` |
| Audit | [`docs/lottie_audit_and_gap_analysis.md`](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/docs/lottie_audit_and_gap_analysis.md) | Audit analysis and root cause breakdown — `dev-flow-orchestrator` |

## Shared Activity Log

- 19:50 [dev-flow-orchestrator] — completed Task 1.5 Hierarchy Cycle Guard & Dynamic Track Matte Path Safety (Phase 1 Complete)
- 19:40 [dev-flow-orchestrator] — completed Task 1.4 PolyStar Dynamic RemoteLottiePath Refactoring
- 19:27 [dev-flow-orchestrator] — completed Task 1.3 Transform Inversion Singularities (Scale = 0 Guard)
- 18:55 [dev-flow-orchestrator] — completed Task 1.2 GradientStroke Annotations & Keyframe Hold Flag Parsing
- 18:06 [dev-flow-orchestrator] — completed Task 1.1 AST Model Defaults & Fractional Framerate
- 17:45 [dev-flow-orchestrator] — created task
