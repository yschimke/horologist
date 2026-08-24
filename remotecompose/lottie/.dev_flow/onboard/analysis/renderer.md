# Analysis: renderer

## Key Entities
- Shape rendering functions: `createPolygonPath`, `createStarPath`, `fill`, bezier conversions.
- Property animators: `animateScalar`, `animatePosition`, `animateVector`.

## Public Contracts
- Converts decoded `GraphicElement`s (`Path`, `Rectangle`, `Ellipse`, `PolyStar`) into `RemoteShape`s, `RemoteFill`s.
- `transform(...)` applies structural transforms using `RemoteCanvas` and `RemotePaint`.

## Dependencies
- Depends on `format` package (`GraphicElement`, properties).
- Depends on `LottieSettings` and interpolators.
- Heavily uses `androidx.compose.remote.creation.compose.*` APIs, like `RemoteCanvas`, `RemotePaint`.
