# Specification: Graphic Elements Architecture & Lottie 1.0.1 Compliance {#SP_LOTTIE_SHAPES}

> **Code:** SP_LOTTIE_SHAPES
> **Status:** active
> **Created:** 2026-08-22
> **Updated:** 2026-08-22
>
> **Concept:** [Shapes Concept](shapes.concept.md)
> **Depends on:** [Format Concept](format.concept.md), [Renderer Concept](renderer.concept.md)
> **Used by:** [Shapes Plan](shapes.plan.md), `format/graphicelement`, `renderer/shapes`
>
> Formal specification for Lottie Graphic Elements non-sealed interfaces, per-file data structures under `format/graphicelement/`, co-located serialization contracts, parametric geometry evaluation, and Remote Compose rendering pipeline according to [Lottie 1.0.1 Shapes](https://lottie.github.io/lottie-spec/1.0.1/single-page/#specs-shapes).

## 01 Data Structures & Package Layout {#SP_LOTTIE_SHAPES_01_01}

### 1. Root Interface & Serializer (`format/graphicelement/GraphicElement.kt`)
```kotlin
package com.google.android.horologist.remotecompose.lottie.format.graphicelement

@Serializable(with = GraphicElementSerializer::class)
internal interface GraphicElement {
  val name: String?
  val hidden: Boolean?
  val type: ShapeType
  val index: Int?
  val matchName: String?
  val propertyIndex: Int?
}

@Serializable(with = ShapeTypeSerializer::class)
internal enum class ShapeType(val value: String) {
  Path("sh"),
  Rectangle("rc"),
  Ellipse("el"),
  PolyStar("sr"),
  Group("gr"),
  Transform("tr"),
  Fill("fl"),
  Stroke("st"),
  GradientFill("gf"),
  GradientStroke("gs"),
  NoStyle("no"),
  TrimPath("tm"),
  Repeater("rp"),
  RoundedCorners("rd"),
  MergePaths("mm"),
  OffsetPath("op"),
  PuckerBloat("pb"),
  Twist("tw"),
  ZigZag("zz"),
  Unknown("unknown");

  companion object {
    fun fromValueOrNull(value: String): ShapeType? = values().firstOrNull { it.value == value }
  }
}
```

### 2. Geometry Shapes (`format/graphicelement/geometry/`) {#SP_LOTTIE_SHAPES_01_02}

- **`GeometryShape.kt`:**
  ```kotlin
  package com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry

  import com.google.android.horologist.remotecompose.lottie.format.graphicelement.GraphicElement

  internal sealed interface GeometryShape : GraphicElement {
    val direction: Int?
  }
  ```
- **`Path.kt`:** `data class Path(...) : GeometryShape` (`ty: "sh"`, `ks: BaseBezierProperty`, `d: Int?`)
- **`Rectangle.kt`:** `data class Rectangle(...) : GeometryShape` (`ty: "rc"`, `p: BasePositionProperty`, `s: BaseVectorProperty`, `r: BaseScalarProperty`, `d: Int?`)
- **`Ellipse.kt`:** `data class Ellipse(...) : GeometryShape` (`ty: "el"`, `p: BasePositionProperty`, `s: BaseVectorProperty`, `d: Int?`)
- **`PolyStar.kt`:** `data class PolyStar(...) : GeometryShape` (`ty: "sr"`, `sy: PolyStarType`, `pt`, `p`, `r`, `or`, `os`, `ir`, `is`, `d`), `PolyStarType` enum, `PolyStarTypeSerializer`

### 3. Grouping & Hierarchy (`format/graphicelement/grouping/`) {#SP_LOTTIE_SHAPES_01_03}

- **`Group.kt`:** `data class Group(...) : GraphicElement` (`ty: "gr"`, `np: Int?`, `it: List<GraphicElement>`, `cix: Int?`, `ix: Int?`)
- **`Transform.kt`:** `data class Transform(...) : GraphicElement` (`ty: "tr"`, `a`, `p`, `r`, `s`, `o`, `sk: BaseScalarProperty?`, `sa: BaseScalarProperty?`)

### 4. Styles (`format/graphicelement/styles/`) {#SP_LOTTIE_SHAPES_01_04}

- **`ShapeStyle.kt`:**
  ```kotlin
  package com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles

  import com.google.android.horologist.remotecompose.lottie.format.graphicelement.GraphicElement
  import com.google.android.horologist.remotecompose.lottie.format.properties.BaseScalarProperty

  internal sealed interface ShapeStyle : GraphicElement {
    val opacity: BaseScalarProperty
  }
  ```
- **`Fill.kt`:** `data class Fill(...) : ShapeStyle` (`ty: "fl"`, `c: BaseColorProperty`, `o: BaseScalarProperty`, `r: FillRule`), `FillRule` enum, `FillRuleSerializer`
- **`Stroke.kt`:** `data class Stroke(...) : ShapeStyle` (`ty: "st"`, `c: BaseColorProperty`, `o: BaseScalarProperty`, `w: BaseScalarProperty`, `lc: LineCap`, `lj: LineJoin`, `ml: BaseScalarProperty?`, `d: List<StrokeDash>?`), `LineCap`, `LineJoin`, `StrokeDash`, `LineCapSerializer`, `LineJoinSerializer`
- **`GradientFill.kt`:** `data class GradientFill(...) : ShapeStyle` (`ty: "gf"`, `t: GradientType`, `s`, `e`, `g: BaseGradientProperty`, `o`, `r`, `h`, `fillRule: FillRule`), `GradientType` enum, `GradientTypeSerializer`
- **`GradientStroke.kt`:** `data class GradientStroke(...) : ShapeStyle` (`ty: "gs"`, `t: GradientType`, `s`, `e`, `g: BaseGradientProperty`, `o`, `w`, `lc: LineCap`, `lj: LineJoin`, `ml`, `d`, `r`, `h`)
- **`NoStyle.kt`:** `data class NoStyle(...) : ShapeStyle` (`ty: "no"`)

### 5. Modifiers (`format/graphicelement/modifiers/`) {#SP_LOTTIE_SHAPES_01_05}

- **`ShapeModifier.kt`:**
  ```kotlin
  package com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers

  import com.google.android.horologist.remotecompose.lottie.format.graphicelement.GraphicElement

  internal sealed interface ShapeModifier : GraphicElement
  ```
- **`TrimPath.kt`:** `data class TrimPath(...) : ShapeModifier` (`ty: "tm"`, `s`, `e`, `o`, `m: TrimMode`), `TrimMode` enum, `TrimModeSerializer`
- **`Repeater.kt`:** `data class Repeater(...) : ShapeModifier` (`ty: "rp"`, `c`, `o`, `m: CompositeMode`, `tr: Transform`), `CompositeMode` enum, `CompositeModeSerializer`
- **`RoundedCorners.kt`:** `data class RoundedCorners(...) : ShapeModifier` (`ty: "rd"`, `r: BaseScalarProperty`)
- **`MergePaths.kt`:** `data class MergePaths(...) : ShapeModifier` (`ty: "mm"`, `mm: MergeMode`), `MergeMode` enum, `MergeModeSerializer`
- **`OffsetPath.kt`:** `data class OffsetPath(...) : ShapeModifier` (`ty: "op"`, `a: BaseScalarProperty`, `lj: LineJoin`, `ml: BaseScalarProperty?`)
- **`PuckerBloat.kt`:** `data class PuckerBloat(...) : ShapeModifier` (`ty: "pb"`, `a: BaseScalarProperty`)
- **`Twist.kt`:** `data class Twist(...) : ShapeModifier` (`ty: "tw"`, `a: BaseScalarProperty`, `c: BasePositionProperty`)
- **`ZigZag.kt`:** `data class ZigZag(...) : ShapeModifier` (`ty: "zz"`, `s: BaseScalarProperty`, `r: BaseScalarProperty`, `pt: ZigZagType`), `ZigZagType` enum, `ZigZagTypeSerializer`
- **`UnknownElement.kt`:** `data class UnknownElement(...) : GraphicElement`

## 02 Contracts {#SP_LOTTIE_SHAPES_02_01}

### Contract 1: Polymorphic Shape Deserialization (`GraphicElementSerializer`)
- **Input:** JSON object with `"ty"` field.
- **Output:** Corresponding `GraphicElement` implementation instance.
- **Error Handling:** If `"ty"` is invalid, missing, or represents an unsupported element, returns `UnknownElement`.

### Contract 2: Shape Geometry Evaluation (`renderer/shapes/*`)
- **Input:** Specific `GeometryShape` node (`Path`, `Rectangle`, `Ellipse`, `PolyStar`) and `LottieSettings`.
- **Output:** `RemoteShape` (`RemoteLottiePath` or `RemoteCompiledPath`), or `null` if `hidden == true`.
- **Exhaustiveness:** Ensured at compile time via `sealed interface GeometryShape`.

### Contract 3: Shape Grouping & Rendering Orchestration (`renderer/Shape.kt`)
- **Input:** `List<GraphicElement>`, `LottieSettings`.
- **Output:** `List<StyledShapes>` evaluated with forward style binding and bottom-to-top (Z-order) rendering sequence.

## 03 Validation Rules & Mathematical Invariants {#SP_LOTTIE_SHAPES_03_01}

1. **Ellipse Quadrant Constant:** Approximated using $E_t \approx 0.55191502449351057$ (or circular constant $4(\sqrt{2}-1)/3 \approx 0.55228475$).
2. **PolyStar Tangent Constants:** $0.47829$ for Star inner/outer tangents; $0.25$ for Polygon vertices.
3. **Rectangle Corner Radius:** Coerced to $[0, \min(w/2, h/2)]$.
4. **Group Opacity Compounding:** Group transform opacity is applied atomically to the whole group result.

## 04 Integration Scenarios {#SP_LOTTIE_SHAPES_04_01}

1. **Scenario 1: Solid Stroke Rendering**
   - Layer with `Path` + `Stroke` (`st`) renders using `RemoteStroke` with `PaintingStyle.Stroke`.
2. **Scenario 2: Unknown Element Ingestion**
   - JSON with unknown `"ty": "foo"` deserializes cleanly into `UnknownElement` without throwing or failing parent layers.

## 05 Verification Criteria {#SP_LOTTIE_SHAPES_05_01}

- `SP_LOTTIE_SHAPES_05_01`: Unit tests in `ParsingTest.kt` verifying deserialization of all 15 shape types.
- `SP_LOTTIE_SHAPES_05_02`: Resilience tests verifying enum conversions, missing fields, and unknown type fallbacks.
- `SP_LOTTIE_SHAPES_05_03`: `MediaLottieDiffScreenshotTest` passes for stroke animations (`m3Next`, `volumeUp`, `volumeDown`, `muteToUnmute`).
- `SP_LOTTIE_SHAPES_05_04`: `LottieFeatureDiffScreenshotTest` passes for parametric shapes (`rectEllipse`, `polystar`).
- `SP_LOTTIE_SHAPES_05_05`: Full Gradle verification suite passes cleanly.

## 06 Rollback Strategy {#SP_LOTTIE_SHAPES_06_01}

Revert modular shape files to previous commit state without affecting public API contracts.

## 07 Alternatives Considered {#SP_LOTTIE_SHAPES_07_01}

| # | Approach | Pros | Cons | Verdict |
|---|----------|------|------|---------|
| 1 | Single monolithic file for all shapes (`format/Shapes.kt`) | Single file | 600+ lines, high coupling | Rejected |
| 2 | Open root interface + Package-sealed category interfaces | Clean sub-package isolation, small files (~30-80 lines), compile-time `when` exhaustiveness within domain categories | Root `GraphicElement` remains open | Chosen |

