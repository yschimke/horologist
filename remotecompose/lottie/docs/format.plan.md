# Plan: Format (`format`)
**Status**: completed

## Phase 1: Serialization Mapping [DONE]
- Implement serialization constructs for shapes, styling and transforms.
- Add serialization constructs for `LottieAnimation` and `Layer`.

## Backlog
- [TODO] Support Stroke element (`ty: "st"`) deserialization (`GraphicElement.Stroke`, `ShapeType.Stroke`, `StrokeCapType`, `StrokeJoinType`).
- Potential optimization on serialization performance.
