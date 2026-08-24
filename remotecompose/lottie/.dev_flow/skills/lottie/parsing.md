# Lottie Parsing

Lottie files are serialized with `kotlinx.serialization`. Use `@SerialName` extensively because the JSON schema is based on 1-2 letter keys (e.g. `nm` for name, `ty` for type).

## Lottie 1.0.1 Specification Rules

1. **Animated vs Static Properties:** All properties define `a` (0 or 1) indicating if they are animated. If `a = 0`, `k` holds the direct primitive value (e.g., float, vector). If `a = 1`, `k` holds an array of Keyframes.
2. **Keyframes:** Contain `t` (time frame), `i` (in tangent for easing), `o` (out tangent), `s` (interpolated value). `i` and `o` define cubic bezier easing where `x` is time progression [0,1] and `y` is value interpolation (can be outside [0,1] for overshoots).
3. **Bezier Shapes:** Defined with `c` (closed boolean), `v` (vertices array), `i` (in tangents relative to `v`), `o` (out tangents relative to `v`).
4. **Split Position (`s: true`):** Position can exist split into independent `x` and `y` scalar animated values.
5. **Gradients:** Color stops and opacity stops are packed into a single flattened array: `[offset, r, g, b, ..., offset, alpha, ...]`.
