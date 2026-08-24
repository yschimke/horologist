# Concept: Renderer (`renderer`)
**Status**: active
**Depends on**: [Format](format.concept.md)

## Philosophy
Translate the static `format` data classes into actionable `RemoteCompose` drawing nodes (`RemoteShape`, `RemoteCanvas` manipulations). It decouples parsing from drawing.

## Domain Model
- Translators matching `ShapeType` enum to geometric construction logic (e.g. `createStarPath`).
- Animators interpolating format `Property` types based on a provided `LottieSettings.currentFrame`.

## Mechanisms
- Bezier logic to convert Lottie points into `RemotePath.cubicTo` / `lineTo` / `moveTo` calls.
- Parent Transform application via `RemoteCanvas.translate/rotate/scale/translate` sequences.

## Integration Points
- Consumes `format` objects.
- Leverages Remote Compose `RemoteCanvas` for layout and `RemotePaint` for styling.
