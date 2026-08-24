# Project Glossary

- **SlotMap**: A mapping structure linking generic slot string IDs to specific color values (`RemoteColor`).
- **GraphicElement**: The base building block of a Lottie shape layer representing geometry (`Path`, `Rectangle`, `Ellipse`, `PolyStar`), grouping (`Group`, `Transform`), styles (`Fill`, `Stroke`, `GradientFill`, `GradientStroke`), and modifiers (`TrimPath`, `Repeater`, `RoundedCorners`, `MergePaths`).
- **Layer**: The top-level composition building block representing spatial containers (`NullLayer`), vector graphics (`ShapeLayer`), solid backgrounds (`SolidColorLayer`), precompositions (`PrecompLayer`), images (`ImageLayer`), or text (`TextLayer`), with hierarchical transform parenting and timeline visibility bounds.
- **RemoteCompose**: An Android library used to serialize Compose-like trees over IPC (Inter-Process Communication) and render them on another process/device (commonly used for Wear OS widgets).
- **RemoteShape**: Evaluated geometry primitive (`RemoteCompiledPath`, `RemoteLottiePath`, `RemoteGroup`) ready for drawing to `RemoteCanvas`.
- **RemoteStyle**: Visual styling descriptor (`RemoteFill`, `RemoteStroke`, `RemoteGradientFill`, `RemoteGradientStroke`) producing configured `RemotePaint` instances.
