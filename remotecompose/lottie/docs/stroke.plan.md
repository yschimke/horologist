# Implementation Plan: Lottie Stroke Support (`stroke`)  {#PL_LOTTIE_STROKE}

> **Code:** PL_LOTTIE_STROKE
> **Status:** in-progress
> **Created:** 2026-08-21
> **Updated:** 2026-08-21
>
> **Concept:** [Format](format.concept.md), [Renderer](renderer.concept.md)
> **Specification:** [Format](format.sp.md), [Renderer](renderer.sp.md)
> **Depends on:** none
> **Used by:** `MediaLottieDiffScreenshotTest`
>
> Implementation plan for supporting Lottie `Stroke` elements (`ty: "st"`) across deserialization and Remote Compose rendering to resolve failures in `MediaLottieDiffScreenshotTest_m3Next` (and related stroke-based animations).

## Goal

Add full support for Lottie stroke graphic elements (`ty: "st"`) in the `format` serialization layer and translate them into `RemoteStroke` with `PaintingStyle.Stroke` in the `renderer` drawing pipeline, enabling `MediaLottieDiffScreenshotTest_m3Next` to succeed.

## Phases

### Phase 1 — Format Deserialization (`format`) [TODO]  {#PL_LOTTIE_STROKE_P1}
- Define `ShapeType.Stroke("st")`.
- Define `StrokeCapType` and `StrokeJoinType` enums/serializers.
- Define `GraphicElement.Stroke` data class in `format/Shapes.kt`.
- Register `ShapeType.Stroke` in `GraphicElementSerializer` in `format/LottieDecoder.kt`.

### Phase 2 — Rendering & Drawing Pipeline (`renderer`) [TODO]  {#PL_LOTTIE_STROKE_P2}
- Add `RemoteStroke` implementing `RemoteStyle` in `renderer/RemoteStyle.kt`.
- Implement `stroke(...)` converter and handle `GraphicElement.Stroke` in `gatherShapes` inside `renderer/Shape.kt`.

### Phase 3 — Verification & Test Enablement (`test`) [TODO]  {#PL_LOTTIE_STROKE_P3}
- Update `MediaLottieDiffScreenshotTest.kt` to run `m3Next()` with `expectedFailure = false`.
- Run unit tests and Roborazzi screenshot verification.
