# Concept: Layers Architecture & Lottie 1.0.1 Compliance {#C_LOTTIE_LAYERS}

> **Code:** C_LOTTIE_LAYERS
> **Status:** active
> **Created:** 2026-08-22
> **Updated:** 2026-08-22
>
> **Depends on:** [Format](format.concept.md), [Renderer](renderer.concept.md), [Shapes](shapes.concept.md)
> **Used by:** [Layers Spec](layers.sp.md), [Layers Plan](layers.plan.md)
>
> Comprehensive concept for modularizing Lottie Layers in `:remotecompose:lottie` under `format/layer/` and `renderer/layers/` with co-located serialization, float timing accuracy, layer parenting preservation, and solid color rendering, aligned with the [Lottie 1.0.1 Layers specification](https://lottie.github.io/lottie-spec/1.0.1/single-page/#specs-layers).

## 01 Philosophy / Overview {#C_LOTTIE_LAYERS_01_01}

Lottie Layers constitute the primary organizational and structural elements in an animation composition. Section 5 of the Lottie 1.0.1 specification defines layers as independent temporal, spatial, and visual nodes in a composition tree.
1. **Structural Containers & Hierarchy:** Layers define unique indices (`ind`) and parent references (`parent`) that establish a multi-level hierarchical transform chain across the composition.
2. **Temporal Windows:** Layers operate on floating-point frame intervals $[ip, op)$ (In Point, Out Point) modulated by start time offsets (`st`) and time stretch multipliers (`sr`).
3. **Visual Diversity:** Specific layer types (`ty`) define vector shapes (Shape Layer `4`), solid colored backgrounds (Solid Color Layer `1`), nested sub-compositions (Precomposition Layer `0`), bitmaps (Image Layer `2`), typography (Text Layer `5`), audio streams (Audio Layer `6`), or non-visual transform anchors (Null Layer `3`).
4. **Compositing Operations:** Visual layers support track mattes (`tt`, `tp`, `td`) and blend modes (`bm`) to control compositing with underlying canvas layers.

### Core Architecture & Guiding Priorities
- **Priority 1 (Refactor & Modularize):** Split the monolithic `format/Layers.kt` into dedicated per-file models under `com.google.android.horologist.remotecompose.lottie.format.layer` (`Layer.kt`, `ShapeLayer.kt`, `NullLayer.kt`, `SolidColorLayer.kt`, `PrecompLayer.kt`, `ImageLayer.kt`, `TextLayer.kt`, `UnknownLayer.kt`, `BlendMode.kt`, `MatteMode.kt`), with co-located serializers. Split monolithic `renderer/Layer.kt` into `com.google.android.horologist.remotecompose.lottie.renderer.layers` (`Layer.kt`, `ShapeLayer.kt`, `SolidColorLayer.kt`).
- **Priority 2 (Fix Existing Layer Deficiencies):** Align timing properties (`ip`, `op`, `st`, `sr`) to floating-point numbers (`Float?`), implement active frame visibility checks ($ip \le t < op$) in the layer rendering loop, support common layer metadata (`ao` Auto-Orient, `bm` BlendMode, `tt` MatteMode, `tp` MatteParent, `td` MatteTarget, `ddd` 3D), and preserve unknown layer nodes in the AST to maintain transform parenting integrity for descendant layers.
- **Priority 3 (Add Missing Layer Rendering):** Implement `SolidColorLayer` (`ty: 1`) rendering via `RemoteCanvas` solid colored rectangles and expand AST layer models (`PrecompLayer`, `ImageLayer`, `TextLayer`, `UnknownLayer`) for full specification completeness.

## 02 Domain Model {#C_LOTTIE_LAYERS_02_01}

Modular layout under `format/layer/` and `renderer/layers/`:

```
format/layer/ (package com.google.android.horologist.remotecompose.lottie.format.layer)
├── Layer.kt (sealed class Layer, LayerType enum, LayerSerializer, LayerTypeSerializer)
├── ShapeLayer.kt (data class ShapeLayer : Layer, "ty": 4, shapes: List<GraphicElement>)
├── NullLayer.kt (data class NullLayer : Layer, "ty": 3)
├── SolidColorLayer.kt (data class SolidColorLayer : Layer, "ty": 1, sc: String, sw: Float, sh: Float)
├── PrecompLayer.kt (data class PrecompLayer : Layer, "ty": 0, refId: String, w: Int, h: Int, tm: BaseScalarProperty?)
├── ImageLayer.kt (data class ImageLayer : Layer, "ty": 2, refId: String)
├── TextLayer.kt (data class TextLayer : Layer, "ty": 5)
├── UnknownLayer.kt (data class UnknownLayer : Layer, resilient AST fallback)
├── BlendMode.kt (enum class BlendMode, BlendModeSerializer)
└── MatteMode.kt (enum class MatteMode, MatteModeSerializer)

renderer/layers/ (package com.google.android.horologist.remotecompose.lottie.renderer.layers)
├── Layer.kt (Layer dispatch, timeline window [ip, op) checks, hidden checks, transform chaining)
├── ShapeLayer.kt (RenderShapeLayer: shapes evaluation orchestration)
└── SolidColorLayer.kt (RenderSolidColorLayer: solid quad canvas drawing with transform & opacity)
```

## 03 Mechanisms {#C_LOTTIE_LAYERS_03_01}

### 1. Polymorphic Layer Serialization & Resilient Tree Preservation
- `LayerSerializer` dispatches based on integer `"ty"` into corresponding serializers (`ShapeLayer`, `NullLayer`, `SolidColorLayer`, `PrecompLayer`, `ImageLayer`, `TextLayer`, `UnknownLayer`).
- Unknown or unsupported layer types deserialize into `UnknownLayer` retaining their `index` (`ind`), `parent`, `transform` (`ks`), and timing (`ip`, `op`, `st`, `sr`). This guarantees child layers referencing an unknown layer as a parent maintain their correct spatial transforms and positions.
- Serializers for enums (`LayerTypeSerializer`, `BlendModeSerializer`, `MatteModeSerializer`) are co-located in their respective enum files.

### 2. Timeline Frame Window & Visibility Invariant
- A layer is active and rendered at current composition frame $t$ if and only if:
  $$\text{hidden} \ne \text{true} \quad \land \quad ip \le t < op$$
- When $t < ip$ or $t \ge op$, the layer content is not drawn, but its spatial transform remains valid for active descendant child layers.
- In Remote Compose documents where $t$ is a dynamic expression (`RemoteFloat`), dynamic visibility is evaluated using `selectIfLt`:
  $$\text{layerVisibility} = \text{selectIfLt}(t, ip, 0, 1) \times \text{selectIfLt}(t, \text{effectiveEndFrame}, 1, 0)$$
  where $\text{effectiveEndFrame} = op + 0.01$ when $op \ge \text{composition.endFrame}$ (ensuring composition-terminal layers remain visible at progress $1.0$ / final frame, mirroring `lottie-android`), and $op$ otherwise. Static short-circuiting applies when $t$ resolves to a constant value.

### 3. Spatial Transform Parenting vs Opacity Isolation
- Spatial transformations accumulate recursively down the parent chain:
  $$\mathbf{M}_{\text{world}} = \mathbf{M}_{\text{root}} \times \dots \times \mathbf{M}_{\text{parent}} \times \mathbf{M}_{\text{child}}$$
- Layer opacity (`ks.o`) does not multiply down parent-child layers (opacity isolation per AE/Lottie spec). Child layers evaluate their own local opacity independently of parent layer opacity.

### 4. Solid Color Layer Rendering
- `SolidColorLayer` parses hex color string `sc` (e.g. `"#FFFFFF"` or `"#FF5722"`), resolves dimensions $(sw, sh)$, modulates color alpha by layer transform opacity `ks.o`, and draws a solid filled rectangle via `RemoteCanvas` with accumulated ancestor transforms applied.

## 04 Integration Points {#C_LOTTIE_LAYERS_04_01}

- **`format/Animation.kt`:** Root `Animation` holds `layers: List<Layer>`.
- **`format/LottieDecoder.kt`:** Root JSON decoder delegates layer deserialization to `format/layer/Layer.kt`.
- **`LottieAnimation.kt`:** Computes `ancestorTransforms` using `buildAncestorTransforms` and executes `Layer` rendering.
- **`renderer/shapes/`:** Shape layers delegate vector drawing to `RenderShapes`.
- **`MediaLottieDiffScreenshotTest` & `LottieFeatureDiffScreenshotTest`:** Verify parenting hierarchy, multi-level transforms, and visual layer accuracy against reference animations.

## 05 Design Decisions {#C_LOTTIE_LAYERS_DEC_01}

1. **`C_LOTTIE_LAYERS_DEC_01`: Modular Sub-Package `format/layer/` with Co-Located Serializers**
   - *Decision:* Split monolithic `format/Layers.kt` and `format/LottieDecoder.kt` layer serializers into dedicated files inside `com.google.android.horologist.remotecompose.lottie.format.layer`.
   - *Rationale:* Eliminates 300+ line monoliths, enforces single responsibility per layer model, and guarantees serializers evolve atomically with data structures.

2. **`C_LOTTIE_LAYERS_DEC_02`: Resilient Fallback Preserving Transform Chains**
   - *Decision:* Map unrecognized layer types to `UnknownLayer` capturing `ind`, `parent`, `ks`, `ip`, `op`, `st`, `sr` instead of dropping them.
   - *Rationale:* Prevents breaking parent transform chains when animations contain newer or unsupported layer types (such as Audio or Camera layers).

3. **`C_LOTTIE_LAYERS_DEC_03`: Floating-Point Timing Properties**
   - *Decision:* Define `startFrame` (`ip`), `endFrame` (`op`), `startTime` (`st`), and `timeStretch` (`sr`) as `Float?` rather than `Int?`.
   - *Rationale:* Lottie 1.0.1 specification defines timings as floating-point numbers, supporting sub-frame keyframes and fractional frame rates.

4. **`C_LOTTIE_LAYERS_DEC_04`: Modular Renderer Sub-Package `renderer/layers/`**
   - *Decision:* Modularize `renderer/Layer.kt` into `com.google.android.horologist.remotecompose.lottie.renderer.layers` with dedicated renderers for `ShapeLayer` and `SolidColorLayer`.
   - *Rationale:* Keeps render logic clean, testable, and isolated per visual layer type, while non-visual layers are handled as no-ops.

5. **`C_LOTTIE_LAYERS_DEC_05`: NullLayer Non-Visual Handling Without Dedicated Renderer**
   - *Decision:* Treat `NullLayer` (`ty: 3`) as a non-visual node handled as a direct no-op in `renderer/layers/Layer.kt` without a dedicated `NullLayer.kt` renderer file.
   - *Rationale:* Null layers serve purely as transform anchors. Their spatial transforms are pre-accumulated into the `ancestorTransforms` graph in `LottieAnimation.kt` during preprocessing, so no drawing operations are emitted.

## 06 Alternatives Considered {#C_LOTTIE_LAYERS_06_01}

### 1. Monolithic `format/Layers.kt` vs Modular `format/layer/` Package
- **Option A (Keep single monolithic `format/Layers.kt`)**:
  - *Pros:* Single file.
  - *Cons:* Violates single responsibility principle, grows exponentially as new layer types (Solid, Precomp, Image, Text) are added, couples unrelated serializers.
  - *Verdict:* Rejected.
- **Option B (Modular `format/layer/` package with 1 file per layer type) [Selected]**:
  - *Pros:* High cohesion, small focused files (~30-60 lines), strict symmetry with `format/graphicelement/`.
  - *Verdict:* Chosen.

### 2. Discard Unknown Layers vs AST `UnknownLayer` Retention
- **Option A (Discard unrecognized layers or treat as empty no-op without metadata)**:
  - *Pros:* Simpler serializer.
  - *Cons:* Drops `ind` and `ks`, breaking spatial transforms for all child layers chained to the unrecognized layer.
  - *Verdict:* Rejected.
- **Option B (Preserve in `UnknownLayer` with transform and parenting metadata) [Selected]**:
  - *Pros:* 100% resilient hierarchy retention, robust against forward extensions.
  - *Verdict:* Chosen.

### 3. Dedicated NullLayer Renderer vs Dispatcher No-Op
- **Option A (Create empty `renderer/layers/NullLayer.kt` composable)**:
  - *Pros:* 1:1 filename symmetry between `format/layer/` and `renderer/layers/`.
  - *Cons:* Redundant empty composable adding unnecessary stack frames and dead code.
  - *Verdict:* Rejected.
- **Option B (Direct no-op in `renderer/layers/Layer.kt` dispatcher) [Selected]**:
  - *Pros:* Zero overhead; transform chaining is fully handled via `ancestorTransforms`.
  - *Verdict:* Chosen.
