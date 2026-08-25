# Task: Comprehensive Lottie Visual Screenshot Diff Test Suite

> **Task ID:** `task_PL_LOTTIE_SCREENSHOT_TESTS`
> **Created:** 2026-08-25 12:15
> **Last updated:** 2026-08-25 12:15
> **Status:** `in-progress`
> **Contributors:** dev-flow-orchestrator

## Current Work Item

| Field | Value |
|-------|-------|
| **Document** | `plan` — [`screenshot_test_suite.plan.md`](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/docs/screenshot_test_suite.plan.md) |
| **Pipeline phase** | `plan` |
| **Traceable ID** | `PL_LOTTIE_SCREENSHOT_TESTS` |
| **Ticket** | n/a |

## Intent

- **Goal (why):** Implement comprehensive Roborazzi visual screenshot diff tests comparing RemoteCompose Lottie rendering with reference `lottie-android` bitmaps across all 19 features implemented in Phases 1–4.
- **Target state:** All 4 phases of `PL_LOTTIE_SCREENSHOT_TESTS` executed, golden reference baselines recorded, and `./gradlew :remotecompose:lottie:verifyRoborazziDebug` passing with 0 visual regressions.
- **Expected result:** High visual test coverage across Gradients, Stroke Dashes/Miters, EvenOdd Fills, Trim Paths, Inverted Mattes, Layer Masks, Repeaters, RoundedCorners, MergePaths, Precomps, Time Remapping, ImageLayers, and Typography TextLayers.

## Description

Execute the 4-phase visual testing plan defined in `docs/screenshot_test_suite.plan.md` step-by-step. Each phase and task is designed to be executed atomically with clean contexts to avoid LLM context drift or hallucinations. — dev-flow-orchestrator

## Subtasks

### Subtask: Execute Phase 1 (Visual Styling & Shader Screenshot Tests)
> Author: `dev-flow-orchestrator` — Created: 12:15 — Last updated: 12:15 — Status: `in-progress`

**Goal:** Implement Phase 1 screenshot tests (Linear & Radial Gradients, Stroke Dashes & Miters, EvenOdd Fill Rules, and Primitive Trim Paths).

**Progress:**
- [x] Create comprehensive audit document `docs/lottie_audit_and_gap_analysis.md`
- [x] Create concept document `docs/screenshot_test_suite.concept.md`
- [x] Create specification document `docs/screenshot_test_suite.sp.md`
- [x] Create implementation plan `docs/screenshot_test_suite.plan.md`
- [x] Initialize `.dev_flow/` task context and dashboard
- [ ] **Next:** [Task 1.1: Linear & Radial Gradient Shaders Screenshot Test (`PL_LOTTIE_SCREENSHOT_TESTS_T1_1`)](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/docs/screenshot_test_suite.plan.md#PL_LOTTIE_SCREENSHOT_TESTS_T1_1)
- [ ] [Task 1.2: Stroke Dash Patterns, Miter Limits & EvenOdd Fill Rules Screenshot Test (`PL_LOTTIE_SCREENSHOT_TESTS_T1_2`)](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/docs/screenshot_test_suite.plan.md#PL_LOTTIE_SCREENSHOT_TESTS_T1_2)
- [ ] [Task 1.3: Primitive & Dynamic Shape Trim Paths Screenshot Test (`PL_LOTTIE_SCREENSHOT_TESTS_T1_3`)](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/docs/screenshot_test_suite.plan.md#PL_LOTTIE_SCREENSHOT_TESTS_T1_3)

### Subtask: Execute Phase 2 (Compositing, Track Mattes & Layer Masks)
> Author: `dev-flow-orchestrator` — Created: 12:15 — Last updated: 12:15 — Status: `pending`

**Goal:** Implement Phase 2 screenshot tests (Inverted Alpha Track Mattes, Non-Adjacent Mattes, and Layer Mask Clipping Pipeline).

**Progress:**
- [ ] [Task 2.1: Inverted Alpha & Non-Adjacent Track Mattes Screenshot Test (`PL_LOTTIE_SCREENSHOT_TESTS_T2_1`)](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/docs/screenshot_test_suite.plan.md#PL_LOTTIE_SCREENSHOT_TESTS_T2_1)
- [ ] [Task 2.2: Layer Mask Clipping Pipeline (`masksProperties`) Screenshot Test (`PL_LOTTIE_SCREENSHOT_TESTS_T2_2`)](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/docs/screenshot_test_suite.plan.md#PL_LOTTIE_SCREENSHOT_TESTS_T2_2)

### Subtask: Execute Phase 3 (Advanced Modifiers)
> Author: `dev-flow-orchestrator` — Created: 12:15 — Last updated: 12:15 — Status: `pending`

**Goal:** Implement Phase 3 screenshot tests (Repeater Modifiers, RoundedCorners, and MergePaths Boolean Operations).

**Progress:**
- [ ] [Task 3.1: Repeater Modifier Geometry & Opacity Progression Screenshot Test (`PL_LOTTIE_SCREENSHOT_TESTS_T3_1`)](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/docs/screenshot_test_suite.plan.md#PL_LOTTIE_SCREENSHOT_TESTS_T3_1)
- [ ] [Task 3.2: RoundedCorners & MergePaths Boolean Operations Screenshot Test (`PL_LOTTIE_SCREENSHOT_TESTS_T3_2`)](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/docs/screenshot_test_suite.plan.md#PL_LOTTIE_SCREENSHOT_TESTS_T3_2)

### Subtask: Execute Phase 4 (Compositions, Bitmap Images & Typography)
> Author: `dev-flow-orchestrator` — Created: 12:15 — Last updated: 12:15 — Status: `pending`

**Goal:** Implement Phase 4 screenshot tests (Deep Nested Precompositions, Time Remapping, Base64 Bitmap ImageLayers, and Vector Typography TextLayers).

**Progress:**
- [ ] [Task 4.1: Deep Nested Precompositions & Time Remapping Screenshot Test (`PL_LOTTIE_SCREENSHOT_TESTS_T4_1`)](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/docs/screenshot_test_suite.plan.md#PL_LOTTIE_SCREENSHOT_TESTS_T4_1)
- [ ] [Task 4.2: Bitmap ImageLayer & Vector Typography TextLayer Screenshot Test (`PL_LOTTIE_SCREENSHOT_TESTS_T4_2`)](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/docs/screenshot_test_suite.plan.md#PL_LOTTIE_SCREENSHOT_TESTS_T4_2)

## Activity

- 12:15 — Created audit document `docs/lottie_audit_and_gap_analysis.md`, concept `docs/screenshot_test_suite.concept.md`, specification `docs/screenshot_test_suite.sp.md`, plan `docs/screenshot_test_suite.plan.md`, and initialized task context.

## Coordination Notes

- 12:15 [dev-flow-orchestrator] — Initialized visual verification roadmap `PL_LOTTIE_SCREENSHOT_TESTS` across 4 phases to provide complete visual regression safety for all Lottie 1.0.1 capabilities. Ready to execute Task 1.1.
