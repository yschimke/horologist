# Task: Lottie 1.0.1 Specification Parity Remediation & Hardening

> **Task ID:** `task_PL_LOTTIE_REMEDIATION`
> **Created:** 2026-08-25 17:45
> **Last updated:** 2026-08-25 17:45
> **Status:** `in-progress`
> **Contributors:** dev-flow-orchestrator

## Current Work Item

| Field | Value |
|---|---|
| **Document** | `plan` — [`lottie_remediation.plan.md`](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/docs/lottie_remediation.plan.md) |
| **Pipeline phase** | `plan` |
| **Traceable ID** | `PL_LOTTIE_REMED` |
| **Ticket** | n/a |

## Intent

- **Goal (why):** Implement the remediation plan derived from the 18-agent audit in `DOC_LOTTIE_AUDIT_2026_V3` to eliminate all critical rendering bugs, mathematical regressions, compositing defects, and visual screenshot diff discrepancies.
- **Target state:** All 3 phases of `PL_LOTTIE_REMED` executed step-by-step, verified via unit and Roborazzi screenshot diff tests with 0 regressions.
- **Expected result:** Clean Gradle check passes (`./gradlew :remotecompose:lottie:check`), full test coverage, and complete visual and mathematical parity with reference Lottie player.

## Description

Execute the 3-phase remediation plan defined in `docs/lottie_remediation.plan.md` step-by-step. Follow the AGENTS.md Spec-Driven Execution Protocol: TDD first, atomic commits per task, non-functional/functional commit separation, and full verification sequence. — dev-flow-orchestrator

## Subtasks

### Subtask: Execute Phase 1 (Critical Rendering & Data Loss Fixes)
> Author: `dev-flow-orchestrator` — Created: 17:45 — Last updated: 19:15 — Status: `completed`

**Goal:** Implement Phase 1 critical fixes: `RemoteFill` opacity compounding, `TextLayer` vector glyph harvesting, `FillRule.EvenOdd` canvas dispatch, sub-frame Look-Up Table easing linear interpolation, and inverted alpha/non-adjacent track mattes.

**Progress:**
- [x] Create concept document `docs/lottie_remediation.concept.md`
- [x] Create specification document `docs/lottie_remediation.sp.md`
- [x] Create implementation plan `docs/lottie_remediation.plan.md`
- [x] Initialize `.dev_flow/` task context and dashboard
- [x] [Task 1.1: Solid Fill Opacity Compounding in RemoteFill & Shape.kt (`PL_LOTTIE_REMED_T1_1`)](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/docs/lottie_remediation.plan.md#PL_LOTTIE_REMED_T1_1)
- [x] [Task 1.2: TextLayer Vector Glyph Harvesting in gatherShapes (`PL_LOTTIE_REMED_T1_2`)](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/docs/lottie_remediation.plan.md#PL_LOTTIE_REMED_T1_2)
- [x] [Task 1.3: EvenOdd Fill Rule Canvas Path Dispatch (`PL_LOTTIE_REMED_T1_3`)](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/docs/lottie_remediation.plan.md#PL_LOTTIE_REMED_T1_3)
- [x] [Task 1.4: Sub-Frame Look-Up Table Easing Linear Interpolation (`PL_LOTTIE_REMED_T1_4`)](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/docs/lottie_remediation.plan.md#PL_LOTTIE_REMED_T1_4)
- [x] [Task 1.5: Inverted Alpha Track Mattes & Non-Adjacent Matte Parent Routing (`PL_LOTTIE_REMED_T1_5`)](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/docs/lottie_remediation.plan.md#PL_LOTTIE_REMED_T1_5)

### Subtask: Execute Phase 2 (Core Algorithm, Math Safety & Modifier Compounding)
> Author: `dev-flow-orchestrator` — Created: 17:45 — Last updated: 19:30 — Status: `completed`

**Goal:** Implement Phase 2 core algorithm and math safety fixes: dynamic primitive keyframe evaluation in `Rectangle`/`Ellipse`/`PolyStar`, symmetric scale-zero transform clamping, native `ImageLayer` bitmap source rect scaling, multi-mask `Add` mode union merging, canvas skew axis rotation ordering, and `Repeater` group compounding.

**Progress:**
- [x] [Task 2.1: Animated Primitive Keyframe Evaluation in Rectangle, Ellipse, PolyStar (`PL_LOTTIE_REMED_T2_1`)](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/docs/lottie_remediation.plan.md#PL_LOTTIE_REMED_T2_1)
- [x] [Task 2.2: Scale-Zero Singularity Matrix Symmetry in Transform.kt (`PL_LOTTIE_REMED_T2_2`)](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/docs/lottie_remediation.plan.md#PL_LOTTIE_REMED_T2_2)
- [x] [Task 2.3: ImageLayer Native Bitmap Bounds Scaling (`PL_LOTTIE_REMED_T2_3`)](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/docs/lottie_remediation.plan.md#PL_LOTTIE_REMED_T2_3)
- [x] [Task 2.4: Multi-Mask Add Mode Composite Path Union (`PL_LOTTIE_REMED_T2_4`)](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/docs/lottie_remediation.plan.md#PL_LOTTIE_REMED_T2_4)
- [x] [Task 2.5: Canvas Skew Axis Rotation Ordering & Repeater Group Compounding (`PL_LOTTIE_REMED_T2_5`)](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/docs/lottie_remediation.plan.md#PL_LOTTIE_REMED_T2_5)

### Subtask: Execute Phase 3 (Extended Parity, Typography & Verification Hardening)
> Author: `dev-flow-orchestrator` — Created: 17:45 — Last updated: 19:30 — Status: `in-progress`

**Goal:** Implement Phase 3 extended parity fixes: multiline text and stroke-over-fill ordering, stroke miter limit application to `RemotePaint`, gradient stop array bounds protection, precomposition canvas boundary clipping, and full Roborazzi golden baseline verification.

**Progress:**
- [ ] [Task 3.1: Multiline Text, Line Height & Stroke-Over-Fill in TextLayer (`PL_LOTTIE_REMED_T3_1`)](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/docs/lottie_remediation.plan.md#PL_LOTTIE_REMED_T3_1)
- [ ] [Task 3.2: Stroke Miter Limit & Gradient Stop Boundary Guards (`PL_LOTTIE_REMED_T3_2`)](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/docs/lottie_remediation.plan.md#PL_LOTTIE_REMED_T3_2)
- [ ] [Task 3.3: Precomp Canvas Boundary Clipping & Roborazzi Baseline Verification (`PL_LOTTIE_REMED_T3_3`)](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/docs/lottie_remediation.plan.md#PL_LOTTIE_REMED_T3_3)
