# Analysis: root_compose

## Key Entities
- `LottieAnimation(rawRes|json|Animation)`: Main `@RemoteComposable` entry points.
- `LottieSettings`: State holder for `currentFrame`, `slotMap`, `width`, `height`. Passed via `CompositionLocal`.
- `SlotMap`: Provides mapping between slot IDs and actual `RemoteColor` values (for dynamic theming).

## Public Contracts
- `LottieAnimation` Composables provide configurable APIs to render a Lottie via Remote Compose.
- `progress` argument can drive the animation manually, otherwise it is clock-driven using `ANIMATION_TIME`.

## Algorithms / Invariants
- `buildAncestorTransforms`: Traverses the layer hierarchy by matching `parent` to `index` and computes a precalculated `Map<Int, List<Transform>>` to apply parent transforms correctly. This is topologically sorted and cached to prevent allocation churn.
- Uses `scaleModifier` combining `RemoteModifier.drawWithContent` to perform centering and aspect-ratio-preserving scaling (fit-center type).

## Dependencies
- Depends on `format` for models.
- Depends on `renderer` to invoke `Layer(...)` composables which draw the shapes.
