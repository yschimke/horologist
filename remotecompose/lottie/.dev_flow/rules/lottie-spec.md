# Lottie Parsing Constraints

- **must**: All `Property` data classes mapped from JSON must handle type unions for field `k` (either Primitive or Array of Keyframes). This often requires a custom serializer in `kotlinx.serialization` based on the value of `a` or the schema of `k` to prevent crash on deserialization.
- **should**: When parsing `Vector` colors (RGB), handle the alpha channel carefully as the spec states it is often ignored or defined disjointly from the standard 3-float array structure.
- **must**: Layers in a Lottie composition are ordered top-to-bottom in the JSON array, so canvas / painter rendering must traverse layers in reverse order (bottom-up) to ensure correct visual stacking. Track matte mask layers (`td: 1`) apply to the layer immediately following them in the JSON array (`i + 1`) and must not be rendered as standalone visual layers.
