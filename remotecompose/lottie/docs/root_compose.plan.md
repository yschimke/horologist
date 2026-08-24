# Plan: Root Compose API (`root_compose`)
**Status**: completed

## Phase 1: Composables [DONE]
- Create `LottieAnimation` overrides.
- Connect layout modifier with internal aspect scaling logic via `drawWithContent`.
- Implement frame progression algorithm bridging Remote Compose's `ANIMATION_TIME` and `LottieSettings`.

## Backlog
- Enable `RemoteRectangleShape` clipping once the native client fully supports it (see `TODO: 496943072`).
