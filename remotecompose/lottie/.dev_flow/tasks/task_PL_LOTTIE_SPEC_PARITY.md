# Task: Lottie 1.0.1 Specification Parity & Hardening

> **Task ID:** `task_PL_LOTTIE_SPEC_PARITY`
> **Created:** 2026-08-24 17:45
> **Last updated:** 2026-08-24 19:20
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
> Author: `dev-flow-orchestrator` — Created: 17:45 — Last updated: 19:20 — Status: `in-progress`

**Goal:** Implement Phase 1 critical bug fixes (AST model defaults, serial name annotations, hold flag parsing, transform singularities, PolyStar dynamic path, and hierarchy cycle guards).

**Progress:**
- [x] Create comprehensive audit document `docs/lottie_audit_and_gap_analysis.md`
- [x] Create formal implementation plan `docs/lottie_spec_parity.plan.md`
- [x] Initialize `.dev_flow/` task context and dashboard
- [x] [Task 1.1: AST Model Defaults & Fractional Framerate (`PL_LOTTIE_SPEC_PARITY_T1_1`)](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/docs/lottie_spec_parity.plan.md#PL_LOTTIE_SPEC_PARITY_T1_1)
- [x] [Task 1.2: GradientStroke Annotations & Keyframe Hold Flag Parsing (`PL_LOTTIE_SPEC_PARITY_T1_2`)](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/docs/lottie_spec_parity.plan.md#PL_LOTTIE_SPEC_PARITY_T1_2)
- [x] [Task 1.3: Transform Inversion Singularities (Scale = 0 Guard) (`PL_LOTTIE_SPEC_PARITY_T1_3`)](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/docs/lottie_spec_parity.plan.md#PL_LOTTIE_SPEC_PARITY_T1_3)
- [ ] **Next:** [Task 1.4: PolyStar Dynamic RemoteLottiePath Refactoring (`PL_LOTTIE_SPEC_PARITY_T1_4`)](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/docs/lottie_spec_parity.plan.md#PL_LOTTIE_SPEC_PARITY_T1_4)
- [ ] Task 1.5: Hierarchy Cycle Guard & Dynamic Track Matte Path Safety

**Activity:**
- 17:45 — Created task and initialized plan for Phase 1.
- 18:06 — Completed Task 1.1: AST Model Defaults & Fractional Framerate with tests and full verification.
- 18:55 — Completed Task 1.2: GradientStroke Annotations & Keyframe Hold Flag Parsing with clean pre-commit review.
- 19:27 — Completed Task 1.3: Transform Inversion Singularities (Scale = 0 Guard) with clean pre-commit review and Roborazzi verification.

## Coordination Notes

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

- 19:27 [dev-flow-orchestrator] — completed Task 1.3 Transform Inversion Singularities (Scale = 0 Guard)
- 18:55 [dev-flow-orchestrator] — completed Task 1.2 GradientStroke Annotations & Keyframe Hold Flag Parsing
- 18:06 [dev-flow-orchestrator] — completed Task 1.1 AST Model Defaults & Fractional Framerate
- 17:45 [dev-flow-orchestrator] — created task
