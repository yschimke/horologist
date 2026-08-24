# Implementation Plan: Graphic Elements Architecture & Lottie 1.0.1 Compliance {#PL_LOTTIE_SHAPES}

> **Code:** PL_LOTTIE_SHAPES
> **Status:** completed
> **Created:** 2026-08-22
> **Updated:** 2026-08-22
>
> **Concept:** [Shapes Concept](shapes.concept.md)
> **Specification:** [Shapes Specification](shapes.sp.md)
> **Specification Reference:** [Lottie 1.0.1 Shapes](https://lottie.github.io/lottie-spec/1.0.1/single-page/#specs-shapes)
> **Depends on:** none
> **Used by:** `format`, `renderer`, `MediaLottieDiffScreenshotTest`
>
> Implementation plan for Lottie Graphic Elements / Shapes specification compliance and modular architecture in `:remotecompose:lottie`: separating monolithic `format/Shapes.kt` and `renderer/Shape.kt` into category packages under `format/graphicelement/` (`geometry/`, `styles/`, `grouping/`, `modifiers/`) with non-sealed interfaces and co-located serializers, fixing and completing existing elements, implementing `GraphicElement.Stroke` and `RemoteStroke`, adding modifiers, and verifying via unit tests and Roborazzi screenshot tests.

## Goal

Align all Lottie Graphic Elements with the [Lottie 1.0.1 specification](https://lottie.github.io/lottie-spec/1.0.1/single-page/#specs-shapes) guided by three strict priorities:
1. **Priority 1 (Refactor Existing Codebase):**
   - Modularize `format/Shapes.kt` into `com.google.android.horologist.remotecompose.lottie.format.graphicelement`:
     - `GraphicElement.kt` (root interface, `ShapeType` enum, and polymorphic `GraphicElementSerializer`).
     - `geometry/` (`GeometryShape.kt`, `Path.kt`, `Rectangle.kt`, `Ellipse.kt`, `PolyStar.kt` with `PolyStarTypeSerializer`).
     - `styles/` (`ShapeStyle.kt`, `Fill.kt` with `FillRuleSerializer`, `Stroke.kt` with `LineCapSerializer`/`LineJoinSerializer`, `GradientFill.kt` with `GradientTypeSerializer`, `GradientStroke.kt`, `NoStyle.kt`).
     - `grouping/` (`Group.kt`, `Transform.kt`).
     - `modifiers/` (`ShapeModifier.kt`, `TrimPath.kt` with `TrimModeSerializer`, `Repeater.kt` with `CompositeModeSerializer`, `RoundedCorners.kt`, `MergePaths.kt` with `MergeModeSerializer`, `UnknownElement.kt`).
     - Co-locate each `KSerializer` in the exact file of the model/enum it serves.
   - Modularize `renderer/Shape.kt` into `com.google.android.horologist.remotecompose.lottie.renderer.shapes`:
     - 1 file per shape geometry evaluator (`Path.kt`, `Rectangle.kt`, `Ellipse.kt`, `PolyStar.kt`).
   - Refactor `renderer/Shape.kt` into a pure orchestration module (`RenderShapes`, `gatherShapes`, `group`).
2. **Priority 2 (Fix Problems & Complete Existing Elements):**
   - Add `fillRule: FillRule` (`r: 1` NonZero, `r: 2` EvenOdd) to `Fill` and `GradientFill`.
   - Add `skew: BaseScalarProperty?` and `skewAxis: BaseScalarProperty?` to `Transform`.
   - Add metadata fields `index: Int?` (`ix`), `matchName: String?` (`mn`), `propertyIndex: Int?` (`cix`) to base `GraphicElement`.
   - Align ellipse quadrant constant in `renderer/shapes/Ellipse.kt` to $E_t \approx 0.55191502449351057$.
   - Add resilient fallback in `GraphicElementSerializer` mapping unknown `"ty"` to `UnknownElement`.
3. **Priority 3 (Add Missing Elements):**
   - Add `Stroke` (`ty: "st"`) with `LineCap`, `LineJoin`, `miterLimit`, `strokeWidth`, `opacity`, `color`, `dashes`.
   - Add `RemoteStroke` in `renderer/RemoteStyle.kt` and wire into `gatherShapes`.
   - Add AST modifier models (`TrimPath`, `Repeater`, `RoundedCorners`, `MergePaths`, `NoStyle`, `UnknownElement`).
   - Enable `MediaLottieDiffScreenshotTest` tests (`m3Next`, `volumeUp`, `volumeDown`, `muteToUnmute`).

---

## Architectural Design & Package Structure

```
remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/
├── format/
│   ├── graphicelement/
│   │   ├── GraphicElement.kt       # Open root GraphicElement interface, ShapeType enum, GraphicElementSerializer
│   │   ├── geometry/
│   │   │   ├── GeometryShape.kt    # sealed interface GeometryShape : GraphicElement (direction: Int?)
│   │   │   ├── Path.kt             # data class Path : GeometryShape ("sh")
│   │   │   ├── Rectangle.kt        # data class Rectangle : GeometryShape ("rc")
│   │   │   ├── Ellipse.kt          # data class Ellipse : GeometryShape ("el")
│   │   │   └── PolyStar.kt         # data class PolyStar : GeometryShape ("sr"), PolyStarType, PolyStarTypeSerializer
│   │   ├── grouping/
│   │   │   ├── Group.kt            # data class Group : GraphicElement ("gr")
│   │   │   └── Transform.kt        # data class Transform : GraphicElement ("tr")
│   │   ├── styles/
│   │   │   ├── ShapeStyle.kt       # sealed interface ShapeStyle : GraphicElement (opacity: BaseScalarProperty)
│   │   │   ├── Fill.kt             # data class Fill : ShapeStyle ("fl"), FillRule, FillRuleSerializer
│   │   │   ├── Stroke.kt           # data class Stroke : ShapeStyle ("st"), LineCap, LineJoin, StrokeDash, serializers
│   │   │   ├── GradientFill.kt     # data class GradientFill : ShapeStyle ("gf"), GradientType, GradientTypeSerializer
│   │   │   ├── GradientStroke.kt   # data class GradientStroke : ShapeStyle ("gs")
│   │   │   └── NoStyle.kt          # data class NoStyle : ShapeStyle ("no")
│   │   └── modifiers/
│   │       ├── ShapeModifier.kt    # sealed interface ShapeModifier : GraphicElement
│   │       ├── TrimPath.kt         # data class TrimPath : ShapeModifier ("tm"), TrimMode, TrimModeSerializer
│   │       ├── Repeater.kt         # data class Repeater : ShapeModifier ("rp"), CompositeMode, CompositeModeSerializer
│   │       ├── RoundedCorners.kt   # data class RoundedCorners : ShapeModifier ("rd")
│   │       ├── MergePaths.kt       # data class MergePaths : ShapeModifier ("mm"), MergeMode, MergeModeSerializer
│   │       └── UnknownElement.kt   # data class UnknownElement : GraphicElement
│   └── LottieDecoder.kt            # Root decoder
└── renderer/
    ├── shapes/
    │   ├── Path.kt                 # evaluatePath(Path, LottieSettings): RemoteLottiePath
    │   ├── Rectangle.kt            # evaluateRectangle(Rectangle, LottieSettings): RemoteCompiledPath
    │   ├── Ellipse.kt              # evaluateEllipse(Ellipse, LottieSettings): RemoteCompiledPath
    │   └── PolyStar.kt             # evaluatePolyStar(PolyStar, LottieSettings): RemoteCompiledPath, createStarPath, createPolygonPath
    ├── RemoteShape.kt              # RemoteShape, RemoteCompiledPath, RemoteLottiePath, RemoteGroup
    ├── RemoteStyle.kt              # RemoteFill, RemoteStroke, RemoteGradientFill, RemoteGradientStroke, NoopStyle
    └── Shape.kt                    # RenderShapes, gatherShapes, group
```

---

## Phases

### Phase 1 — Test Harness & Resilience Assertions [DONE] {#PL_LOTTIE_SHAPES_P1}
- **Verify:** `SP_LOTTIE_SHAPES_05_01`, `SP_LOTTIE_SHAPES_05_02`
- Add unit tests in `ParsingTest.kt` for deserialization of:
  - `Stroke` (`st`) with color, opacity, stroke width, line caps (`lc: 1/2/3`), line joins (`lj: 1/2/3`), miter limits, and dashes.
  - `Fill` (`fl`) and `GradientFill` (`gf`) with `fillRule` (`r: 1` NonZero, `r: 2` EvenOdd).
  - `Transform` (`tr`) with `skew` (`sk`) and `skewAxis` (`sa`).
  - Parametric shapes (`Path`, `Rectangle`, `Ellipse`, `PolyStar`).
  - Shape modifiers (`TrimPath`, `Repeater`, `RoundedCorners`, `MergePaths`, `NoStyle`).
- Add unit tests in `LottieDecoderResilienceTest.kt` for:
  - Unknown shape type `"ty": "unsupported_extension"` safely deserializing into `UnknownElement`.
  - Missing optional fields and integer/string enum conversions.

### Phase 2 — Priority 1: Format Layer Category Package Modularization (`format/graphicelement/`) [DONE] {#PL_LOTTIE_SHAPES_P2}
- **Verify:** `SP_LOTTIE_SHAPES_05_01`
- Create `format/graphicelement/GraphicElement.kt` defining root `GraphicElement` open interface, `ShapeType`, and polymorphic `GraphicElementSerializer`.
- Create categorized subdirectories and element files with package-sealed domain category interfaces and co-located serializers:
  - `format/graphicelement/geometry/`: `sealed interface GeometryShape`, `Path.kt`, `Rectangle.kt`, `Ellipse.kt`, `PolyStar.kt`.
  - `format/graphicelement/grouping/`: `Group.kt`, `Transform.kt`.
  - `format/graphicelement/styles/`: `sealed interface ShapeStyle`, `Fill.kt`, `Stroke.kt`, `GradientFill.kt`, `GradientStroke.kt`, `NoStyle.kt`.
  - `format/graphicelement/modifiers/`: `sealed interface ShapeModifier`, `TrimPath.kt`, `Repeater.kt`, `RoundedCorners.kt`, `MergePaths.kt`, `UnknownElement.kt`.
- Update `format/Layers.kt` and `format/LottieDecoder.kt` to consume the modular package.

### Phase 3 — Priority 1: Renderer Geometry Decoupling (`renderer/shapes/`) [DONE] {#PL_LOTTIE_SHAPES_P3}
- **Verify:** `SP_LOTTIE_SHAPES_05_04`
- Extract `evaluatePath` to `renderer/shapes/Path.kt`.
- Extract `evaluateRectangle` to `renderer/shapes/Rectangle.kt`.
- Extract `evaluateEllipse` to `renderer/shapes/Ellipse.kt` with precision constant $E_t \approx 0.55191502449351057$.
- Extract `evaluatePolyStar`, `createStarPath`, `createPolygonPath` to `renderer/shapes/PolyStar.kt`.
- Refactor `renderer/Shape.kt` to focus exclusively on render loop orchestration (`RenderShapes`, `gatherShapes`, `group`).

### Phase 4 — Priority 2 & 3: Solid Stroke (`st`) & Style Integration [DONE] {#PL_LOTTIE_SHAPES_P4}
- **Verify:** `SP_LOTTIE_SHAPES_05_03`
- Create `format/graphicelement/styles/Stroke.kt` defining `Stroke`, `LineCap`, `LineJoin`, `StrokeDash`, and their serializers.
- Add `RemoteStroke` in `renderer/RemoteStyle.kt` supporting `RemotePaint { paintingStyle = PaintingStyle.Stroke }` with animated color, stroke width, opacity, stroke cap, stroke join, and miter limit.
- Wire `Stroke` handling into `gatherShapes` in `renderer/Shape.kt`.
- Update `MediaLottieDiffScreenshotTest.kt` to enable `m3Next`, `volumeUp`, `volumeDown`, `muteToUnmute` (`expectedFailure = false`).

### Phase 5 — Full Verification & Roborazzi Screenshot Suite [DONE] {#PL_LOTTIE_SHAPES_P5}
- **Verify:** `SP_LOTTIE_SHAPES_05_05`
- Run mandatory verification commands in order:
  1. `./gradlew :remotecompose:lottie:ktfmtFormat`
  2. `./gradlew :remotecompose:lottie:metalavaGenerateSignatureDebug`
  3. `./gradlew :remotecompose:lottie:compileDebugKotlin`
  4. `./gradlew :remotecompose:lottie:assembleDebug`
  5. `./gradlew :remotecompose:lottie:testDebugUnitTest --no-build-cache`
  6. `./gradlew :remotecompose:lottie:check`
  7. `./gradlew :remotecompose:lottie:verifyRoborazziDebug`

---

## Alternatives Considered

### 1. Monolithic Flat Sealed Class vs Package-Sealed Category Interfaces
- **Option A (Monolithic sealed class in single flat file)**:
  - *Pros:* Exhaustiveness checking by Kotlin compiler at root level.
  - *Cons:* Prevents sub-package organization into `styles/`, `geometry/`, etc.; results in huge files and tight coupling.
  - *Verdict:* Rejected.
- **Option B (Open root interface + Package-sealed category interfaces) [Selected]**:
  - *Pros:* Enables clean sub-package separation (`geometry/`, `styles/`, `modifiers/`); provides 100% compile-time exhaustiveness within each domain category (`GeometryShape`, `ShapeStyle`, `ShapeModifier`); accommodates unknown fallback elements gracefully.
  - *Verdict:* Chosen.

### 2. Co-Located Serializers vs Centralized `Serializers.kt`
- **Option A (Centralized `Serializers.kt`)**: Decouples models/enums from their serialization logic, requiring cross-file edits whenever fields change.
- **Option B (Co-located Serializers inside each element file) [Selected]**: Keeps model, enums, and serializers strictly co-located; high locality of reference; matches `format/properties/` architecture.
