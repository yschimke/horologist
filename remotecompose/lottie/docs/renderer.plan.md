# Plan: Renderer (`renderer`)
**Status**: completed

## Phase 1: Primitive Drawing [DONE]
- Map Rectangles, Ellipses, PolyStars to `RemotePath`.

## Phase 2: Frame Interpolation [DONE]
- Extract keyframe interpolation math.
- Bind `LottieSettings.currentFrame` to value retrievals.

## Backlog
- [TODO] Implement `RemoteStroke` with `PaintingStyle.Stroke`, `strokeWidth`, `strokeCap`, and `strokeJoin`, and integrate with `gatherShapes`.
