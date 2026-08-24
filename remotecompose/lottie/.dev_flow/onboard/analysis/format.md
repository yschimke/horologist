# Analysis: format

## Key Entities
- `LottieAnimation` (root of JSON)
- `Layer` (composition layers)
- `GraphicElement` (`Path`, `Rectangle`, `Ellipse`, `PolyStar`, `Group`, `Transform`, `Fill`)
- `Property` (Base properties, scalars, vectors, colors, bezier curves, keyframes)
- `LottieDecoder` (decodes stream to `LottieAnimation`)

## Public Contracts
- `LottieDecoder.decode(jsonString|inputStream)`: parses the raw JSON into the typed hierarchy.

## Validation / Invariants
- Uses `kotlinx.serialization` for strict declarative parsing based on the Lottie spec.

## Dependencies
- Does not depend on anything but `kotlinx.serialization`.

