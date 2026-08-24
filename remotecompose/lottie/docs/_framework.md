# Framework Map

This project translates Lottie animations to Remote Compose instructions for Wear OS.

## Core Abstractions
- **Format**: `GraphicElement`s and properties representing a Lottie document.
  - Value Primitives: `format/values/` (`BezierValue`, `GradientValue`, `ColorStop`, `OpacityStop`).
  - Animatable Properties: `format/properties/` (`Scalar`, `Vector`, `Position`, `Color`, `Bezier`, `Gradient`).
  - Graphic Elements: `format/graphicelement/` (`GraphicElement`, `GeometryShape`, `Group`, `Transform`, `ShapeStyle`, `ShapeModifier`).
  - Layers & Composition: `format/layer/` (`Layer`, `ShapeLayer`, `NullLayer`, `SolidColorLayer`, `PrecompLayer`, `ImageLayer`, `TextLayer`, `UnknownLayer`), `format/Animation.kt`, `format/LottieDecoder.kt`.
- **Renderer**: Stateless geometry calculations and property interpolators.
  - Property Animators: `renderer/properties/` (`Scalar`, `Vector`, `Position`, `Color`, `Bezier`, `Gradient`).
  - Geometry Evaluators: `renderer/shapes/` (`Path`, `Rectangle`, `Ellipse`, `PolyStar`).
  - Layers Orchestration: `renderer/layers/` (`Layer`, `ShapeLayer`, `SolidColorLayer`).
  - Styles: `renderer/RemoteStyle.kt` (`RemoteFill`, `RemoteStroke`, `RemoteGradientFill`, `RemoteGradientStroke`).
  - Shape Orchestration: `renderer/Shape.kt` (`RenderShapes`, `gatherShapes`, `group`).
- **Compose API**: Public entry-points injecting `LottieSettings` context to render trees (`LottieAnimation.kt`).

## Layers
1. Leaves: `format` (pure Kotlin structs, deserialization)
2. Middle: `renderer` (Remote Compose primitives, math)
3. Root: `root_compose` (Composables, `CompositionLocal` environments, state)

## Conventions
- See `.dev_flow/rules/style.md` for coding style.
- See `.dev_flow/skills/` for domain specifics (Remote Compose, Lottie).
