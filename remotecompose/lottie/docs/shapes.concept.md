# Concept: Graphic Elements Architecture & Lottie 1.0.1 Compliance {#C_LOTTIE_SHAPES}

> **Code:** C_LOTTIE_SHAPES
> **Status:** active
> **Created:** 2026-08-22
> **Updated:** 2026-08-22
>
> **Depends on:** [Format](format.concept.md), [Renderer](renderer.concept.md)
> **Used by:** [Shapes Spec](shapes.sp.md), [Shapes Plan](shapes.plan.md)
>
> Comprehensive concept for modularizing Lottie Graphic Elements (Geometry, Styles, Grouping, Modifiers) in `:remotecompose:lottie` using non-sealed category interfaces under `format/graphicelement/` with co-located serialization, aligned with the [Lottie 1.0.1 Shapes specification](https://lottie.github.io/lottie-spec/1.0.1/single-page/#specs-shapes).

## 01 Philosophy / Overview {#C_LOTTIE_SHAPES_01_01}

Lottie Graphic Elements encompass all vector drawing instructions in a shape layer (`Layer.ShapeLayer`). Section 6 of the Lottie 1.0.1 specification categorizes graphic elements into four distinct functional domains:
1. **Geometry Shapes:** Parametric curves and Bézier paths (`sh` Path, `rc` Rectangle, `el` Ellipse, `sr` PolyStar) defining spatial paths without styling.
2. **Grouping Elements:** Structural containers (`gr` Group, `tr` Transform) establishing coordinate hierarchies and scoping boundaries.
3. **Style Elements:** Visual appearance descriptors (`fl` Fill, `st` Stroke, `gf` GradientFill, `gs` GradientStroke, `no` NoStyle) defining paint configurations.
4. **Modifier Elements:** Geometric operations (`tm` TrimPath, `rp` Repeater, `rd` RoundedCorners, `mm` MergePaths, `op` OffsetPath, `pb` PuckerBloat, `tw` Twist, `zz` ZigZag) altering upstream paths.

### Core Architecture & Guiding Priorities
- **Priority 1 (Refactor & Modularize):** Use non-sealed root `GraphicElement` with package-sealed domain category interfaces (`GeometryShape`, `ShapeStyle`, `ShapeModifier`) under `format/graphicelement/` organized into subdirectories (`geometry/`, `styles/`, `grouping/`, `modifiers/`), with serializers co-located in the same files as their models and enums.
- **Priority 2 (Fix Existing Elements):** Align existing element properties with Lottie 1.0.1 (`fillRule` on `Fill`/`GradientFill`, `skew`/`skewAxis` on `Transform`, `ix`/`mn`/`cix` on base `GraphicElement`, and accurate $E_t \approx 0.551915$ on `Ellipse`).
- **Priority 3 (Add Missing Elements):** Implement `Stroke` (`st`), `NoStyle` (`no`), and modifier models (`TrimPath`, `Repeater`, `RoundedCorners`, `MergePaths`, `UnknownElement`) and wire `RemoteStroke` into the renderer.

## 02 Domain Model {#C_LOTTIE_SHAPES_02_01}

Root interface and specialized category interfaces located in `format/graphicelement/`:

```
GraphicElement (open root interface in format/graphicelement/GraphicElement.kt)
├── geometry/ (package ...format.graphicelement.geometry)
│   ├── GeometryShape (sealed interface : GraphicElement, val direction: Int?)
│   ├── Path.kt ("sh"): ks (BaseBezierProperty), d (direction)
│   ├── Rectangle.kt ("rc"): p (BasePositionProperty), s (BaseVectorProperty), r (BaseScalarProperty), d (direction)
│   ├── Ellipse.kt ("el"): p (BasePositionProperty), s (BaseVectorProperty), d (direction)
│   └── PolyStar.kt ("sr"): sy (PolyStarType), pt, p, r, or, os, ir, is, d, PolyStarTypeSerializer
├── styles/ (package ...format.graphicelement.styles)
│   ├── ShapeStyle (sealed interface : GraphicElement, val opacity: BaseScalarProperty)
│   ├── Fill.kt ("fl"): c (BaseColorProperty), o (BaseScalarProperty), r (FillRule), FillRuleSerializer
│   ├── Stroke.kt ("st"): c, o, w, lc (LineCap), lj (LineJoin), ml, d (List<StrokeDash>), LineCapSerializer, LineJoinSerializer
│   ├── GradientFill.kt ("gf"): t (GradientType), s, e, g (BaseGradientProperty), o, r, h, fillRule, GradientTypeSerializer
│   ├── GradientStroke.kt ("gs"): t, s, e, g, o, w, lc, lj, ml, d, r, h
│   └── NoStyle.kt ("no"): explicit no-op style placeholder
├── grouping/ (package ...format.graphicelement.grouping)
│   ├── Group.kt ("gr"): np (Int?), it (List<GraphicElement>), cix, ix
│   └── Transform.kt ("tr"): a, p, r, s, o, sk (skew), sa (skewAxis)
└── modifiers/ (package ...format.graphicelement.modifiers)
    ├── ShapeModifier (sealed interface : GraphicElement)
    ├── TrimPath.kt ("tm"): s, e, o, m (TrimMode), TrimModeSerializer
    ├── Repeater.kt ("rp"): c, o, m (CompositeMode), tr (Transform), CompositeModeSerializer
    ├── RoundedCorners.kt ("rd"): r (radius)
    ├── MergePaths.kt ("mm"): mm (MergeMode), MergeModeSerializer
    └── UnknownElement.kt: safe fallback for unrecognized types
```

## 03 Mechanisms {#C_LOTTIE_SHAPES_03_01}

### 1. Polymorphic Serialization with Co-Located Serializers
- `GraphicElementSerializer` in `GraphicElement.kt` dispatches based on `"ty"`, returning specific serializers from `geometry/`, `styles/`, `grouping/`, and `modifiers/`.
- Unknown shape types deserialize into `UnknownElement` to prevent crash or group corruption on unrecognized extensions.
- Every enum and model has its serializer in the exact same file (e.g. `FillRuleSerializer` in `Fill.kt`, `LineCapSerializer` in `Stroke.kt`).

### 2. Renderer Geometry Decoupling
- Geometry calculations are extracted into `com.google.android.horologist.remotecompose.lottie.renderer.shapes`:
  - `Path.kt`: `evaluatePath(Path, LottieSettings): RemoteLottiePath`
  - `Rectangle.kt`: `evaluateRectangle(Rectangle, LottieSettings): RemoteCompiledPath`
  - `Ellipse.kt`: `evaluateEllipse(Ellipse, LottieSettings): RemoteCompiledPath`
  - `PolyStar.kt`: `evaluatePolyStar(PolyStar, LottieSettings): RemoteCompiledPath`
- `renderer/Shape.kt` focuses exclusively on render loop orchestration (`RenderShapes`, `gatherShapes`, `group`).

### 3. Solid Stroke Rendering
- `RemoteStroke` implements `RemoteStyle` in `renderer/RemoteStyle.kt`, configuring `RemotePaint` with `PaintingStyle.Stroke`, animated color, stroke width, opacity, stroke cap, stroke join, and miter limit.

## 04 Integration Points {#C_LOTTIE_SHAPES_04_01}

- **`Layer.ShapeLayer` (`format/Layers.kt`):** Holds `shapes: List<GraphicElement>`.
- **`LottieDecoder` (`format/LottieDecoder.kt`):** Deserializes root `Animation`.
- **`RenderShapes` (`renderer/Shape.kt`):** RemoteCanvas composable evaluating shape trees.
- **`MediaLottieDiffScreenshotTest`:** Stroke-dependent screenshot tests (`m3_next`, `volume_up`, `volume_down`, `mute_to_unmute`).

## 05 Design Decisions {#C_LOTTIE_SHAPES_DEC_01}

1. **`C_LOTTIE_SHAPES_DEC_01`: Modular Category Sub-Packages in `format/graphicelement/`**
   - *Decision:* Define `GraphicElement` at the root and organize domain models into dedicated subdirectories (`geometry/`, `styles/`, `grouping/`, `modifiers/`).
   - *Rationale:* Decouples `ShapeStyle` from `Shape`, allows clean package-level modularization across subfolders, and enables strictly typed renderer APIs (`fun applyStyle(style: ShapeStyle)`).

2. **`C_LOTTIE_SHAPES_DEC_02`: Co-Located Serializers**
   - *Decision:* Place custom serializers in the same file as their target models and enums.
   - *Rationale:* Ensures high cohesion where data models and serialization rules evolve together.

3. **`C_LOTTIE_SHAPES_DEC_03`: Unknown Element Resilient Fallback**
   - *Decision:* Map unrecognized `"ty"` discriminators to `UnknownElement` instead of falling back to `Group` or failing serialization.
   - *Rationale:* Guarantees forward compatibility when encountering newer or unsupported Lottie extensions.

4. **`C_LOTTIE_SHAPES_DEC_04`: Package-Sealed Category Interfaces**
   - *Decision:* Declare `GeometryShape`, `ShapeStyle`, and `ShapeModifier` as Kotlin `sealed interface` within their respective sub-packages, while keeping root `GraphicElement` an open interface.
   - *Rationale:* Delivers compile-time exhaustiveness checking for `when` expressions in domain-specific renderer evaluators without requiring an unreachable `else` branch, while preserving open cross-package hierarchy and unknown fallback resilience at the root `GraphicElement` level.

## 06 Alternatives Considered {#C_LOTTIE_SHAPES_06_01}

### 1. Monolithic Flat Sealed Class vs Package-Sealed Category Interfaces
- **Option A (Monolithic sealed class in single flat file)**:
  - *Pros:* Exhaustiveness checking by Kotlin compiler at the root level.
  - *Cons:* Kotlin requires all sealed subclasses to be in the same package/file, preventing sub-package separation into `styles/`, `geometry/`, etc.; results in huge monolithic files and couples unrelated shape types.
  - *Verdict:* Rejected.
- **Option B (Open root interface + Package-sealed category interfaces) [Selected]**:
  - *Pros:* Enables clean sub-package separation (`geometry/`, `styles/`, `modifiers/`); gives 100% compile-time exhaustiveness within each domain category (`GeometryShape`, `ShapeStyle`, `ShapeModifier`); accommodates unknown fallback elements gracefully.
  - *Verdict:* Chosen.

