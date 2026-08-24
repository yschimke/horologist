# Specification: Format (`format`)
**Status**: active

## 01 Data Structures
- `GraphicElement`
  - Sealed class wrapping Lottie entities (type `ty`).
- `LottieAnimation`
  - Properties: `w` (width), `h` (height), `ip` (startFrame), `op` (endFrame), `fr` (frameRate).
- Keyframes / Tangents
  - Handled via `BezierKeyframe` describing interpolations and spatial bezier controls.

## 02 Contracts
- **Contract 1: Decoding**
  - Input: Raw JSON string or stream.
  - Output: Fully initialized `LottieAnimation` data class.
  - Errors: `SerializationException` from kotlinx when JSON is malformed or missing required non-nullable fields.

## 03 Validation Rules
- Enforces strict nullability matching the Lottie spec. Optional fields are `null`.
