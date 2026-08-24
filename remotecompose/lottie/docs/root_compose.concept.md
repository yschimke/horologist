# Concept: Root Compose API (`root_compose`)
**Status**: active
**Depends on**: [Format](format.concept.md), [Renderer](renderer.concept.md)

## Philosophy
Provide an idiomatic Compose interface for Remote Compose clients to embed Lottie animations. It abstracts the serialization, layout management, and frame progression.

## Domain Model
- `LottieAnimation` Composables.
- `LottieSettings` context passed down via `CompositionLocal`.
- `SlotMap` for dynamic theming (e.g. swapping colors at runtime).

## Mechanisms
- Uses `remember` to cache parsed animations to prevent redundant deserialization.
- Topologically sorts the Layer parent/child graph (`buildAncestorTransforms`) recursively to flatten matrices correctly.
- Calculates an automatic `currentFrame` derived dynamically from a base animation clock modded by the total animation frames, scaling the `ANIMATION_TIME`.

## Integration Points
- Provides the public API exported to users of the `lottie` module.
