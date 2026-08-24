# Concept: Format (`format`)

**Status**: active

## Philosophy
Provide a strictly-typed, declarative data model for Lottie JSON using `kotlinx.serialization`. The `format` module acts as the deserialization boundary separating the raw, optimized JSON (1-2 character keys) from the structured business logic used by the renderer.

## Domain Model
- `LottieAnimation`: The root container parsing frame ranges, dimensions, and composition layers.
- `GraphicElement`: A sealed hierarchy representing drawing vectors (e.g. `Path`), groups (`Group`), transforms (`Transform`), and visual styling (`Fill`).
- `Property` constructs: Classes representing animated variables (scalars, colors, vectors) traversing through keyframes and bezier curves.

## Mechanisms
- Standard `kotlinx.serialization` mapping. 
- Deeply nested structures are decoded without manual traversal algorithms.

## Integration Points
- Imported extensively by `renderer` and `root_compose`.
