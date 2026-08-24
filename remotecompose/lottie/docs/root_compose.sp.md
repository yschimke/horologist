# Specification: Root Compose API (`root_compose`)
**Status**: active
**Depends on**: [Format](format.sp.md), [Renderer](renderer.sp.md)

## 01 Data Structures
- `LottieSettings`: `currentFrame` (`RemoteFloat`), `slotMap` (`SlotMap`), `width`, `height`.
- `SlotMap`: Wrapper over a String -> Int (Color mapping) map.

## 02 Contracts
- **Contract 1: Rendering Pipeline**
  - Inputs: JSON string / Raw Resource ID, `SlotMap`, optional custom `progress`.
  - Output: Composed Remote Compose layout representing the animation.

## 03 Validation Rules
- `SlotMap` ignores lookup failures, safely returning `null` (keeps original color).
- Parent hierarchy map correctly assigns transformations in root-to-child order.
