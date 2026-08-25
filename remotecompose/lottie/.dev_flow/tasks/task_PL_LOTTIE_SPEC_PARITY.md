# Task: Lottie 1.0.1 Specification Parity & Hardening

> **Task ID:** `task_PL_LOTTIE_SPEC_PARITY`
> **Created:** 2026-08-24 17:45
> **Last updated:** 2026-08-24 21:20
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
> Author: `dev-flow-orchestrator` — Created: 19:50 — Last updated: 22:45 — Status: `completed`

**Goal:** Implement Phase 2 core rendering and mathematical parity (Spatial Bézier tangents `to`/`ti`, gradient shaders for fill/stroke, stroke dash patterns and miter limits, EvenOdd fill rule, and local layer timing scaling).

**Progress:**
- [x] [Task 2.1: Spatial Bézier Tangents (to, ti) in Position Keyframes (`PL_LOTTIE_SPEC_PARITY_T2_1`)](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/docs/lottie_spec_parity.plan.md#PL_LOTTIE_SPEC_PARITY_T2_1)
- [x] [Task 2.2: Gradient Shaders for GradientFill and GradientStroke (`PL_LOTTIE_SPEC_PARITY_T2_2`)](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/docs/lottie_spec_parity.plan.md#PL_LOTTIE_SPEC_PARITY_T2_2)
- [x] [Task 2.3: Stroke Dash Pattern (d) and Miter Limit (ml) (`PL_LOTTIE_SPEC_PARITY_T2_3`)](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/docs/lottie_spec_parity.plan.md#PL_LOTTIE_SPEC_PARITY_T2_3)
- [x] [Task 2.4: Path FillRule (EvenOdd) & Primitive TrimPath Dispatch (`PL_LOTTIE_SPEC_PARITY_T2_4`)](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/docs/lottie_spec_parity.plan.md#PL_LOTTIE_SPEC_PARITY_T2_4)
- [x] [Task 2.5: Local Layer Timing Scaling & Inverted Alpha Track Mattes (`PL_LOTTIE_SPEC_PARITY_T2_5`)](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/docs/lottie_spec_parity.plan.md#PL_LOTTIE_SPEC_PARITY_T2_5)

### Subtask: Execute Phase 3 (Composition, Precomps & Layer Mask Pipeline)
> Author: `dev-flow-orchestrator` — Created: 23:00 — Last updated: 01:10 — Status: `completed`

**Goal:** Implement Phase 3 composition, precompositions, and layer mask pipeline (Root assets registry, recursive PrecompLayer engine, Layer masks clipping, and time remapping).

**Progress:**
- [x] [Task 3.1: Root assets[] Asset Registry Model (`PL_LOTTIE_SPEC_PARITY_T3_1`)](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/docs/lottie_spec_parity.plan.md#PL_LOTTIE_SPEC_PARITY_T3_1)
- [x] [Task 3.2: PrecompLayer Recursive Sub-Composition Rendering Engine (`PL_LOTTIE_SPEC_PARITY_T3_2`)](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/docs/lottie_spec_parity.plan.md#PL_LOTTIE_SPEC_PARITY_T3_2)
- [x] [Task 3.3: Layer Masks Pipeline (masksProperties) (`PL_LOTTIE_SPEC_PARITY_T3_3`)](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/docs/lottie_spec_parity.plan.md#PL_LOTTIE_SPEC_PARITY_T3_3)
- [x] [Task 3.4: Precomposition Time Remapping (tm) & Timeline Markers (`PL_LOTTIE_SPEC_PARITY_T3_4`)](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/docs/lottie_spec_parity.plan.md#PL_LOTTIE_SPEC_PARITY_T3_4)

### Subtask: Execute Phase 4 (Advanced Modifiers, Typography & Bitmap Assets)
> Author: `dev-flow-orchestrator` — Created: 01:10 — Last updated: 01:10 — Status: `in-progress`

**Goal:** Implement Phase 4 advanced modifiers, typography, and bitmap assets (Repeater geometry engine, RoundedCorners modifier, MergePaths boolean ops, Bitmap asset loading, and TextLayer AST/rendering).

**Progress:**
- [x] [Task 4.1: Repeater Modifier Geometry Engine (`PL_LOTTIE_SPEC_PARITY_T4_1`)](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/docs/lottie_spec_parity.plan.md#PL_LOTTIE_SPEC_PARITY_T4_1)
- [x] [Task 4.2: RoundedCorners Modifier on RemoteBezierValue (`PL_LOTTIE_SPEC_PARITY_T4_2`)](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/docs/lottie_spec_parity.plan.md#PL_LOTTIE_SPEC_PARITY_T4_2)
- [x] [Task 4.3: MergePaths Boolean Operations Engine (`PL_LOTTIE_SPEC_PARITY_T4_3`)](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/docs/lottie_spec_parity.plan.md#PL_LOTTIE_SPEC_PARITY_T4_3)
- [ ] **Next:** [Task 4.4: Bitmap Asset Loading & ImageLayer Rendering (`PL_LOTTIE_SPEC_PARITY_T4_4`)](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/docs/lottie_spec_parity.plan.md#PL_LOTTIE_SPEC_PARITY_T4_4)
- [ ] [Task 4.5: Vector Typography (TextLayer) Data Structures (`PL_LOTTIE_SPEC_PARITY_T4_5`)](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/docs/lottie_spec_parity.plan.md#PL_LOTTIE_SPEC_PARITY_T4_5)

**Activity:**
- 17:45 — Created task and initialized plan for Phase 1.
- 18:06 — Completed Task 1.1: AST Model Defaults & Fractional Framerate with tests and full verification.
- 18:55 — Completed Task 1.2: GradientStroke Annotations & Keyframe Hold Flag Parsing with clean pre-commit review.
- 19:27 — Completed Task 1.3: Transform Inversion Singularities (Scale = 0 Guard) with clean pre-commit review and Roborazzi verification.
- 19:40 — Completed Task 1.4: PolyStar Dynamic RemoteLottiePath Refactoring with clean pre-commit review and Roborazzi verification.
- 19:50 — Completed Task 1.5: Hierarchy Cycle Guard & Dynamic Track Matte Path Safety with clean pre-commit review and full check suite pass. Phase 1 complete!
- 21:20 — Completed Task 2.1: Spatial Bézier Tangents (to, ti) in Position Keyframes with dedicated `PositionTest` suite, Roborazzi baseline updates, and clean pre-commit review.
- 21:40 — Completed Task 2.2: Gradient Shaders for GradientFill and GradientStroke with dedicated `GradientTest` suite and clean pre-commit review.
- 22:00 — Completed Task 2.3: Stroke Dash Pattern (d) and Miter Limit (ml) with dedicated `StrokeTest` suite and clean pre-commit review.
- 22:30 — Completed Task 2.4: Path FillRule (EvenOdd) & Primitive TrimPath Dispatch with dedicated `PrimitiveTrimPathAndFillRuleTest` suite and clean pre-commit review.
- 22:45 — Completed Task 2.5: Local Layer Timing Scaling & Inverted Alpha Track Mattes with dedicated `LayerTimingAndTrackMatteTest` suite, clean pre-commit review, and full Roborazzi verification. Phase 2 complete!
- 23:30 — Completed Task 3.1: Root assets[] Asset Registry Model with polymorphic deserializer, `AssetTest` suite, and clean pre-commit review.
- 23:55 — Completed Task 3.2: PrecompLayer Recursive Sub-Composition Rendering Engine with dedicated `PrecompLayerTest` suite, Roborazzi screenshot verification, clean-context pre-commit review, and full check pass.
- 00:25 — Completed Task 3.3: Layer Masks Pipeline (masksProperties) with Mask AST models, canvas clipping in ShapeLayer and SolidColorLayer, MaskTest suite, and clean pre-commit review.
- 01:10 — Completed Task 3.4: Precomposition Time Remapping (tm) & Timeline Markers with Marker AST model, frameRate propagation, timeRemap evaluation in Layer.kt, PrecompLayerTest suite, and clean pre-commit review. Phase 3 complete!
- 01:45 — Completed Task 4.1: Repeater Modifier Geometry Engine with affine transform progression, start/end opacity compounding, `RepeaterTest` suite, clean pre-commit review, and full check suite pass.
- 02:15 — Completed Task 4.2: RoundedCorners Modifier on RemoteBezierValue with `roundBezierValue` geometry evaluator, `evaluatePathGeometry` timeline compositor, `RoundedCornersTest` suite, clean pre-commit review, and full check suite pass.
- 02:45 — Completed Task 4.3: MergePaths Boolean Operations Engine with `evaluateMergePaths`, `Path.Op` (Union, Difference, Intersect, XOR), `PathIterator`/`PathMeasure` contour extraction, `MergePathsTest` suite, clean pre-commit review, and full check suite pass.

## Coordination Notes

- 02:45 [dev-flow-orchestrator] — Completed Task 4.3. Implemented `evaluateMergePaths`, `shapeToAndroidPath`, and `androidPathToBezierValues` in `renderer/shapes/MergePaths.kt`, wired `MergePaths` modifier dispatch into `gatherShapes` and `evaluateGroupGeometries` in `Shape.kt`, verified with `MergePathsTest.kt` unit test suite and Roborazzi screenshot verification, passed clean pre-commit review, and passed full `:remotecompose:lottie:check` suite. Ready for Task 4.4.

- 01:45 [dev-flow-orchestrator] — Completed Task 4.1. Implemented `evaluateRepeater` geometry engine in `renderer/shapes/Repeater.kt`, added `startOpacity` and `endOpacity` to `Transform.kt`, supported `RemoteStyleWithOpacity` in `RemoteStyle.kt`, wired modifier dispatch in `Shape.kt`, verified with `RepeaterTest.kt` unit test suite and Roborazzi verification, passed clean pre-commit review, and passed `:remotecompose:lottie:check`. Ready for Task 4.2.

- 01:10 [dev-flow-orchestrator] — Completed Task 3.4 and completed Phase 3. Authored `Marker.kt` AST model, added `markers` to `Animation.kt`, added `frameRate` to `LottieSettings` in `LottieAnimation.kt`, evaluated `timeRemap` in `Layer.kt`, added test cases to `PrecompLayerTest.kt`, passed clean-context review, and passed full Gradle check suite. Phase 3 complete! Ready for Phase 4.

- 00:25 [dev-flow-orchestrator] — Completed Task 3.3. Implemented `Mask.kt` AST model and `MaskMode` deserializer, added `masksProperties` to `Layer` contract and all subclasses, implemented `applyLayerMasks` in `Shape.kt` with symmetric matrix save/restore, wired mask clipping into `ShapeLayer.kt` and `SolidColorLayer.kt`, verified with `MaskTest.kt`, and passed clean-context review and full Gradle checks.

- 23:55 [dev-flow-orchestrator] — Completed Task 3.2. Implemented `PrecompLayer.kt` sub-composition renderer, updated `LottieAnimation.kt` (`LottieSettings` and `buildAncestorTransforms`), wired `Layer.kt`, added `PrecompLayerTest.kt` and `precompSubcompositionRendering` Roborazzi test, passed clean-context review and all checks.

- 23:30 [dev-flow-orchestrator] — Completed Task 3.1. Authored `Asset.kt` polymorphic model hierarchy (`PrecompAsset`, `ImageAsset`, `AudioAsset`, `UnknownAsset`), updated `Animation.kt`, verified with `AssetTest.kt`, passed clean-context review, and passed full `:remotecompose:lottie:check` and Roborazzi verification.

- 22:45 [dev-flow-orchestrator] — Completed Task 2.5 and completed Phase 2. Supported `InvertedAlpha` and `InvertedLuma` track mattes via `ClipOp.Difference`, resolved non-adjacent `matteParent` references in `LottieAnimation.kt`, scoped `calculateLocalFrame` to precompositions, and authored `LayerTimingAndTrackMatteTest.kt`.
- 22:30 [dev-flow-orchestrator] — Completed Task 2.4. Extended `evaluateRectangle`, `evaluateEllipse`, and `evaluatePolyStar` to support `trimPath`, updated `Shape.kt` dispatch, added `fillRule` to `RemoteFill`, `RemoteGradientFill`, and `RemoteLottiePath`, authored `PrimitiveTrimPathAndFillRuleTest.kt`, and passed clean-context review.
- 22:00 [dev-flow-orchestrator] — Completed Task 2.3. Configured `RemotePaint.pathEffect` with animated dash patterns in `RemoteStyle.kt`, updated `Shape.kt`, authored `StrokeTest.kt`, and passed clean-context review.
- 21:40 [dev-flow-orchestrator] — Completed Task 2.2. Implemented `RemoteLinearShader` and `RemoteRadialShader` rendering in `RemoteStyle.kt`, updated `Shape.kt`, authored `GradientTest.kt`, and passed clean-context review.
- 21:20 [dev-flow-orchestrator] — Completed Task 2.1. Implemented 2D cubic Bézier spatial curve evaluation in `Position.kt`, added `PositionTest.kt`, updated Roborazzi screenshot baselines for `play_pause` and `m3_next`, and passed clean-context review.
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

- 22:30 [dev-flow-orchestrator] — completed Task 2.4 Path FillRule (EvenOdd) & Primitive TrimPath Dispatch
- 22:00 [dev-flow-orchestrator] — completed Task 2.3 Stroke Dash Pattern (d) and Miter Limit (ml)
- 21:40 [dev-flow-orchestrator] — completed Task 2.2 Gradient Shaders for GradientFill and GradientStroke
- 21:20 [dev-flow-orchestrator] — completed Task 2.1 Spatial Bézier Tangents (to, ti) in Position Keyframes
- 19:50 [dev-flow-orchestrator] — completed Task 1.5 Hierarchy Cycle Guard & Dynamic Track Matte Path Safety (Phase 1 Complete)
- 19:40 [dev-flow-orchestrator] — completed Task 1.4 PolyStar Dynamic RemoteLottiePath Refactoring
- 19:27 [dev-flow-orchestrator] — completed Task 1.3 Transform Inversion Singularities (Scale = 0 Guard)
- 18:55 [dev-flow-orchestrator] — completed Task 1.2 GradientStroke Annotations & Keyframe Hold Flag Parsing
- 18:06 [dev-flow-orchestrator] — completed Task 1.1 AST Model Defaults & Fractional Framerate
- 17:45 [dev-flow-orchestrator] — created task
